package f4g.optimizer.btrplace.configuration;


import java.util.ArrayList;
import java.util.NoSuchElementException;

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
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType.VMFlavor;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.commons.util.StaticPowerCalculation;
import f4g.commons.util.Util;


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
	
	FIT4Green currentFit4Green;
	VMFlavorType currentVMFlavor;
		
	private Logger log;
	private OptimizationObjective optiObjective;
	private IPowerCalculator powerCalculator;
	
	private StaticPowerCalculation powerCalculation;
	
    public FIT4Green getCurrentFIT4Green() {
		return currentFit4Green;
	}

	public void setCurrentFIT4Green(FIT4Green currentFIT4Green) {
		this.currentFit4Green = currentFIT4Green;
	}

	public F4GConfigurationAdapter(FIT4Green f4g, VMFlavorType vm, IPowerCalculator powerCalculator, OptimizationObjective optiObjective) {
		currentFit4Green = f4g;
		currentVMFlavor = vm;
		this.powerCalculator = powerCalculator;
		powerCalculation = new StaticPowerCalculation(null);
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
		
		
		for(Server server : Utils.getAllServers(currentFit4Green)) {
			
			//Add server related resources
			Node node = model.newNode();
			NodeNS.putElementName(node, server.getFrameworkID());
			putServerCPUResource(node, server, cpus);
			putServerMemoryResource(node, server, memories);
			putServerPowerIdleResource(node, server, powersIdles);
			putServerPowerPerVMResource(node, server, powersPerVMs);
					
			if(server.getStatus() == ServerStatus.ON ||
			   server.getStatus() == ServerStatus.POWERING_ON) { //POWERING_ON nodes are seen as ON by BtrPlace as they will be soon on. This avoids ping-pong effect on the state.
				
				model.getMapping().addOnlineNode(node);
			
				for(VirtualMachine VM : Utils.getVMs(server)) {
					
					//Add VM related resources
					VM vm = model.newVM();	
					model.getMapping().addRunningVM(vm, node);
					
					VMNS.putElementName(vm, VM.getFrameworkID());
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
			VMFlavorType.VMFlavor SLA_VM = Util.findVMByName(request.getVm(), currentVMFlavor);
			s.setConsumption(vm, SLA_VM.getCapacity().getVCpus().getValue() * 100);
		} catch (NoSuchElementException e1) {
			log.error("VM name " + request.getVm() + " could not be found in SLA");
		}		
	}

	private void putVMMemoryConsumption(VM vm, CloudVmAllocation request, ShareableResource s) {
		
		try {
			VMFlavorType.VMFlavor SLA_VM = Util.findVMByName(request.getVm(), currentVMFlavor);
			s.setConsumption(vm, (int) SLA_VM.getCapacity().getVRam().getValue() * 1024);
		} catch (NoSuchElementException e1) {
			log.error("VM name " + request.getVm() + " could not be found in SLA");
		}
	}
	
	private void putVMCPUConsumption(VM vm, VirtualMachine F4GVM, ShareableResource s) {
	
		s.setConsumption(vm, getVMCPUConsumption(vm, F4GVM, currentVMFlavor));
	}

	private void putVMMemoryConsumption(final VM vm, final VirtualMachine F4GVM, ShareableResource s) {
		
		VMFlavorType.VMFlavor SLA_VM = null;
		if(F4GVM.getCloudVm() != null) {
			SLA_VM = Util.findVMByName(F4GVM.getCloudVm(), currentVMFlavor);	
		}
		
		if(F4GVM.getActualMemoryUsage() != null) {
			s.setConsumption(vm, (int) (F4GVM.getActualMemoryUsage().getValue() * 1024));
		} else {
			s.setConsumption(vm, (int) (SLA_VM.getCapacity().getVRam().getValue() * 1024));
		}
		
	}
		
	private void putServerCPUResource(final Node n, final Server server, ShareableResource s) {
		
		ArrayList<Core> cores = Utils.getAllCores(server.getMainboard().get(0));
		//CPU capacity is a percentage of one core. 4 cores = 400% CPU capacity
		s.setCapacity(n, cores.size() * 100);		
	
	}

	private void putServerMemoryResource(final Node n, final Server server, ShareableResource s) {
	     s.setCapacity(n, (int) Utils.getMemory(server) * 1024);		
	}
	
	private void putServerPowerIdleResource(final Node n, final Server server, PowerView s) {
		
	    s.setPowers(n, (int) getPIdle(server));		
	}
	
	private void putServerPowerPerVMResource(final Node n, final Server server, PowerView s) {
		
		s.setPowers(n, (int) getPperVM(server));		
	}
		 
	public float getPIdle(Server server) {
      
		double ue = getUsageEffectiveness(server);		
		
    	ServerStatus status = server.getStatus();
    	server.setStatus(ServerStatus.ON); //set the server status to ON to avoid a null power
        float powerIdle = (float) (powerCalculation.computePowerIdle(server, powerCalculator) * ue);
        server.setStatus(status);
                    
        return powerIdle;        
    }
    

    public float getPperVM(Server server) {
                
    	double ue = getUsageEffectiveness(server);	
    	
    	VMFlavor vm = currentVMFlavor.getVMFlavor().get(0);
    	ServerStatus status = server.getStatus();
    	server.setStatus(ServerStatus.ON); //set the server status to ON to avoid a null power
    	float PperVM = (float) (powerCalculation.computePowerForVM(server, vm, powerCalculator) * ue);
        server.setStatus(status);
            
        return PperVM;        
    }

	private double getUsageEffectiveness(Server server) {
    	if(optiObjective == OptimizationObjective.Power) {
    		return Utils.getServerSite(server, currentFit4Green).getPUE().getValue();
    	} else {
    		return Utils.getServerSite(server, currentFit4Green).getCUE().getValue();
    	}
	}
	
	public static int getVMCPUConsumption(VM vm, VirtualMachine F4GVM, VMFlavorType VMFlavor) {
		
		VMFlavorType.VMFlavor SLA_VM = null;
		if(F4GVM.getCloudVm() != null) {
			SLA_VM = Util.findVMByName(F4GVM.getCloudVm(), VMFlavor);	
		}
					
		//If the measured values are present in the VM, we take these.
		//otherwise, we take the specification values from the SLA.
        int nbCPUs;
        if(F4GVM.getNumberOfCPUs() != null) {
                nbCPUs = F4GVM.getNumberOfCPUs().getValue();
        } else {
                nbCPUs = SLA_VM.getCapacity().getVCpus().getValue();
        }

        double CPUUsage;
        if(F4GVM.getActualCPUUsage() != null) {
                CPUUsage = F4GVM.getActualCPUUsage().getValue();
        } else {
                CPUUsage = SLA_VM.getExpectedLoad().getVCpuLoad().getValue();
        }
		
		return (int) (CPUUsage * nbCPUs);
	}

	
}
