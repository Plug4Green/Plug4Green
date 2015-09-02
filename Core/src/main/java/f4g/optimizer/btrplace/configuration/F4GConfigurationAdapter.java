package f4g.optimizer.btrplace.configuration;

import java.util.NoSuchElementException;

import f4g.schemas.java.metamodel.Federation;
import f4g.schemas.java.sla.VMFlavor;
import f4g.schemas.java.sla.VMFlavors;
import org.apache.log4j.Logger;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ShareableResource;

import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.btrplace.NamingService;
import f4g.optimizer.btrplace.plan.objective.PowerView;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;
import org.jscience.physics.amount.Amount;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import static f4g.optimizer.utils.Utils.MEGABYTE;
import static javax.measure.unit.NonSI.PERCENT;
import static javax.measure.unit.SI.WATT;

/**
 * Adapter to translate FIT4Green configurations to Entropy
 *
 * @author Corentin Dupont
 */
public class F4GConfigurationAdapter
{
	public static final String VIEW_POWER_IDLES = "PowerIdles";
	public static final String VIEW_POWER_PER_VM = "PowerPerVMs";
	public static final String VM_NAMING_SERVICE = "VMNames";
	public static final String NODE_NAMING_SERVICE = "NodeNames";
	public static final String SHAREABLE_RESOURCE_CPU = "ShareableResourceCPU";
	public static final String SHAREABLE_RESOURCE_RAM = "ShareableResourceRAM";

	Federation currentFederation;
	VMFlavors currentVMFlavors;
		
	private Logger log;
	private OptimizationObjective optiObjective;

	public F4GConfigurationAdapter(Federation fed, VMFlavors flavors, IPowerCalculator powerCalculator, OptimizationObjective optiObjective) {
		currentFederation = fed;
		currentVMFlavors = flavors;
		this.optiObjective = optiObjective;
		log = Logger.getLogger(F4GConfigurationAdapter.class.getName());
	}
	
	
	/* 
	 * extracts the configuration for the metamodel
	 */
	public void addConfiguration(Model model) {
				
		NamingService<VM> VMNS = new NamingService<VM>(VM_NAMING_SERVICE);
		NamingService<Node> NodeNS = new NamingService<Node>(NODE_NAMING_SERVICE);
		ShareableResource cpus = new ShareableResource(SHAREABLE_RESOURCE_CPU);
		ShareableResource memories = new ShareableResource(SHAREABLE_RESOURCE_RAM); //TODO set names as static constants
		PowerView powersIdles = new PowerView(VIEW_POWER_IDLES);
		PowerView powersPerVMs = new PowerView(VIEW_POWER_PER_VM);
		
		
		for(Server server : Utils.getAllServers(currentFederation)) {
			
			//Add server related resources
			Node node = model.newNode();
			NodeNS.putElementName(node, server.getServerName().toString());
			putServerCPUResource(node, server, cpus);
			putServerMemoryResource(node, server, memories);
			putServerPowerIdleResource(node, server, powersIdles);
			putServerPowerPerVMResource(node, server, powersPerVMs);
					
			if(server.getStatus() == ServerStatus.ON ||
			   server.getStatus() == ServerStatus.POWERING_ON) { //POWERING_ON nodes are seen as ON by BtrPlace as they will be soon on. This avoids ping-pong effect on the state.
				
				model.getMapping().addOnlineNode(node);
			
				for(VirtualMachine VM : server.getVMs()) {
					
					//Add VM related resources
					VM vm = model.newVM();	
					model.getMapping().addRunningVM(vm, node);
					
					VMNS.putElementName(vm, VM.getName().toString());
					putVMCPUConsumption(vm, VM, cpus);
					putVMMemoryConsumption(vm, VM, memories);
					
				}	
			} else { //OFF, POWERING_OFF
				model.getMapping().addOfflineNode(node);
			}
		}
		model.attach(NodeNS);
		model.attach(VMNS);
		model.attach(cpus);
		model.attach(memories);
		model.attach(powersIdles);
		model.attach(powersPerVMs);
		
	}
	
	public void addVMViews(VM vm, CloudVmAllocation request, Model mo) {
		ShareableResource memories = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + SHAREABLE_RESOURCE_RAM);
		putVMMemoryConsumption(vm, request, memories);
		ShareableResource cpus = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + SHAREABLE_RESOURCE_CPU);
		putVMCPUConsumption(vm, request, cpus);
	}
	
	
	private void putVMCPUConsumption(VM vm, CloudVmAllocation request, ShareableResource s) {
		
		try {
			VMFlavor vmFlavor = Utils.findVMByName(request.getVm(), currentVMFlavors);
			s.setConsumption(vm, vmFlavor.getCapacity().getNbOfVCPUs() * 100);
		} catch (NoSuchElementException e1) {
			log.error("VM name " + request.getVm() + " could not be found in SLA");
		}		
	}

	private void putVMMemoryConsumption(VM vm, CloudVmAllocation request, ShareableResource s) {
		
		try {
			VMFlavor vmFlavor = Utils.findVMByName(request.getVm(), currentVMFlavors);
			s.setConsumption(vm, (int) vmFlavor.getCapacity().getVRamSize().doubleValue(MEGABYTE));
		} catch (NoSuchElementException e1) {
			log.error("VM name " + request.getVm() + " could not be found in SLA");
		}
	}
	
	private void putVMCPUConsumption(VM vm, VirtualMachine F4GVM, ShareableResource s) {
	
		s.setConsumption(vm, (int) getVMCPUConsumption(F4GVM, currentVMFlavors).doubleValue(PERCENT));
	}

	private void putVMMemoryConsumption(final VM vm, final VirtualMachine P4GVM, ShareableResource s) {
		
		VMFlavor SLA_VM = null;
		if(P4GVM.getFlavorName() != null) {
			SLA_VM = Utils.findVMByName(P4GVM.getFlavorName().toString(), currentVMFlavors);
		}
		
		if(P4GVM.getActualMemoryUsage() != null) {
			s.setConsumption(vm, (int) (P4GVM.getActualMemoryUsage().doubleValue(MEGABYTE)));
		} else {
			s.setConsumption(vm, (int) (SLA_VM.getCapacity().getVRamSize().doubleValue(MEGABYTE)));
		}
		
	}
		
	private void putServerCPUResource(final Node n, final Server server, ShareableResource s) {

		//CPU capacity is a percentage of one core. 4 cores = 400% CPU capacity
		s.setCapacity(n, server.getCores().getCoreNumber() * 100);
	
	}

	private void putServerMemoryResource(final Node n, final Server server, ShareableResource s) {
	     s.setCapacity(n, (int) server.getRamSize().doubleValue(MEGABYTE));
	}
	
	private void putServerPowerIdleResource(final Node n, final Server server, PowerView s) {
		
	    s.setPowers(n, (int) getPIdle(server).doubleValue(WATT));
	}
	
	private void putServerPowerPerVMResource(final Node n, final Server server, PowerView s) {
		
		s.setPowers(n, (int) getPperVM(server).doubleValue(WATT));
	}
		 
	public Amount<Power> getPIdle(Server server) {

        return (Amount<Power>) server.getIdlePower().times(getUsageEffectiveness(server));
    }
    

    public Amount<Power> getPperVM(Server server) {

		Amount<Dimensionless> ue = getUsageEffectiveness(server);	
    	
    	VMFlavor vmFlavor = currentVMFlavors.getVmFlavors().get(0); //TODO check
		Amount<Power> dynPower = server.getMaxPower().minus(server.getIdlePower());

		Amount<Dimensionless> VMCPUConsumption = vmFlavor.getExpectedLoad().getvCPULoad().times(vmFlavor.getCapacity().getNbOfVCPUs());

        return (Amount<Power>) VMCPUConsumption.times(dynPower).divide(server.getCores().getCoreNumber());
    }

	private Amount<Dimensionless> getUsageEffectiveness(Server server) {
    	if(optiObjective == OptimizationObjective.Power) {
    		return Utils.getServerDatacenter(server, currentFederation).getPue().getValue();
    	} else {
    		return Utils.getServerDatacenter(server, currentFederation).getCue().getValue();
    	}
	}
	
	public static Amount<Dimensionless> getVMCPUConsumption(VirtualMachine F4GVM, VMFlavors VMFlavors) {
		
		VMFlavor SLA_VM = null;
		if(F4GVM.getFlavorName() != null) {
			SLA_VM = Utils.findVMByName(F4GVM.getFlavorName().toString(), VMFlavors);
		}
					
		//We take the specification values from the SLA.
        int nbCPUs = SLA_VM.getCapacity().getNbOfVCPUs();

        Amount<Dimensionless> CPUUsage;
        if(F4GVM.getActualCPUUsage() != null) {
                CPUUsage = F4GVM.getActualCPUUsage();
        } else {
                CPUUsage = SLA_VM.getExpectedLoad().getvCPULoad();
        }
		
		return CPUUsage.times(nbCPUs);
	}

	public Federation getCurrentFederation() {
		return currentFederation;
	}

	public void getCurrentFederation(Federation currentFederation) {
		this.currentFederation = currentFederation;
	}


}
