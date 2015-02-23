package f4g.optimizer.entropy.configuration;


import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ShareableResource;

import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.entropy.plan.objective.PowerView;
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
					
			Node node = model.newNode();
			NodeNS.putElementName(node, server.getFrameworkID());
			putServerCPUResource(node, server, cpus);
			putServerMemoryResource(node, server, memories);
			putServerPowerIdleResource(node, server, powersIdles);
			putServerPowerPerVMResource(node, server, powersPerVMs);
					
			if(server.getStatus() == ServerStatus.ON ||
			   server.getStatus() == ServerStatus.POWERING_ON) { //POWERING_ON nodes are seen as ON by entropy as they will be soon on. This avoids ping-pong effect on the state.
				
				model.getMapping().addOnlineNode(node);
			
				for(VirtualMachine VM : Utils.getVMs(server)) {
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
			s.setConsumption(vm, SLA_VM.getCapacity().getVCpus().getValue());
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
		
		VMFlavorType.VMFlavor SLA_VM = null;
		if(F4GVM.getCloudVm() != null) {
			SLA_VM = Util.findVMByName(F4GVM.getCloudVm(), currentVMFlavor);	
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
		
		s.setConsumption(vm, (int) (CPUUsage * nbCPUs));
		
	}

	private void putVMMemoryConsumption(VM vm, VirtualMachine F4GVM, ShareableResource s) {
		
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
		
	private void putServerCPUResource(Node n, Server server, ShareableResource s) {
		
		ArrayList<Core> cores = Utils.getAllCores(server.getMainboard().get(0));
		//CPU capacity is a percentage of one core. 4 cores = 400% CPU capacity
		s.setCapacity(n, cores.size() * 100);		
	
	}

	private void putServerMemoryResource(Node n, Server server, ShareableResource s) {
		
	     s.setCapacity(n, (int) Utils.getMemory(server) * 1024);		
	
	}
	
	private void putServerPowerIdleResource(Node n, Server server, PowerView s) {
		
	    s.setPowers(n, (int) getPIdle(server));		
	
	}
	
	private void putServerPowerPerVMResource(Node n, Server server, PowerView s) {
		
		s.setPowers(n, (int) getPperVM(server));		
	
	}

	
//	private VM getVM(Node node, VirtualMachine VM) {
//		
//		//VM type should be set OR some measurements should be present
//		if(VM.getCloudVmType() == null &&
//		   (VM.getNumberOfCPUs() == null ||
//			VM.getActualCPUUsage() == null ||
//			VM.getActualMemoryUsage() == null)) {
//			log.error("VM type not set for " + VM.getFrameworkID() + " and no VM measures found");
//			System.exit(-1);
//		}
//		
//		VMFlavorType.VMFlavor SLA_VM = null;
//		if(VM.getCloudVmType() != null) {
//			SLA_VM = Util.findVMByName(VM.getCloudVmType(), currentVMFlavor);	
//		}
//		
//				
//		//If the measured values are present in the VM, we take these.
//		//otherwise, we take the specification values from the SLA.
//		int nbCPUs;
//		if(VM.getNumberOfCPUs() != null) {
//			nbCPUs = VM.getNumberOfCPUs().getValue();
//		} else {
//			nbCPUs = SLA_VM.getCapacity().getVCpus().getValue();
//		}
//		
//		double CPUUsage;
//		if(VM.getActualCPUUsage() != null) {
//			CPUUsage = VM.getActualCPUUsage().getValue();
//		} else {
//			CPUUsage = SLA_VM.getExpectedLoad().getVCpuLoad().getValue();
//		}
//		
//		int memory;
//		if(VM.getActualMemoryUsage() != null) {
//			memory = (int) (VM.getActualMemoryUsage().getValue() * 1024);
//		} else {
//			memory = (int) (SLA_VM.getCapacity().getVRam().getValue() * 1024);
//		}
//		
//		int consumption = (int) (CPUUsage * nbCPUs);
//				
//		log.debug("creation of an Entropy VM with name " + VM.getFrameworkID());
//		log.debug("nbCPUs " + nbCPUs);
//		log.debug("consumption " + consumption + " %");
//		log.debug("memory " + memory + " MB");
//		VirtualMachine vm = new SimpleVirtualMachine(VM.getFrameworkID(), nbCPUs, consumption, memory);                                       
//		return vm;
//	}
//	
//	public VM getVM(Request request) {
//		if(request instanceof CloudVmAllocation) {
//			return getVM((CloudVmAllocation)request);	
//		} else {
//			return getVM((TraditionalVmAllocation)request);
//		}
//	}
	
//	public VM getVM(CloudVmAllocation request) {
//		
//		VMFlavorType.VMFlavor SLA_VM;
//		
//		try {
//			SLA_VM = Util.findVMByName(request.getVm(), currentVMFlavor);
//		} catch (NoSuchElementException e1) {
//			log.error("VM name " + request.getVm() + " could not be found in SLA");
//			return null;
//		}
//
//		int nbCPUs = SLA_VM.getCapacity().getVCpus().getValue();
//		//the consumption of a VM if set as a percentage * number of CPU.
//		//it should be demanding a certain CPU power instead.
//		int consumption = (int) (nbCPUs * SLA_VM.getExpectedLoad().getVCpuLoad().getValue()); //(int) (SLA_VM.getExpectedLoad().getVCpuLoad().getValue() * REFERENCE_CPU_SPEED);
//		int memory = (int) (SLA_VM.getCapacity().getVRam().getValue() * 1024);
//		
//		log.debug("creation of an Entropy VM for allocation request");
//		log.debug("nbCPUs " + nbCPUs);
//		log.debug("consumption " + consumption + " %");
//		log.debug("memory " + memory + " MB");
//		VirtualMachine vm = new SimpleVirtualMachine("new VM", nbCPUs, consumption, memory); 
//		
//		return vm;
//	}
	
//	public VM getVM(TraditionalVmAllocation request) {
//		
//		int nbCPUs;
//		if(request.getNumberOfCPUs() != null) nbCPUs = request.getNumberOfCPUs();
//		else nbCPUs = 1;
//		//the consumption of a VM if set as a percentage * number of CPU.
//		//it should be demanding a certain CPU power instead.
//		
//		double CPUUsage;
//		if(request.getCPUUsage() != null) CPUUsage = request.getCPUUsage();
//		else CPUUsage = 100;
//		
//		int consumption = (int) (nbCPUs * CPUUsage);
//		
//		int memory;
//		if(request.getMemoryUsage() != null) memory = (int) (request.getMemoryUsage() * 1.0 * 1024);
//		else memory = 1;
//
//		log.debug("creation of an Entropy VM for allocation request");
//		log.debug("nbCPUs " + nbCPUs);
//		log.debug("consumption " + consumption + " %");
//		log.debug("memory " + memory + " MB");
//		VM vm = new VM("new VM", nbCPUs, consumption, memory);                                        
//		return vm;
//	}


	
//	private Node getNode(Server server) {
//		
//		
//		//freq in Hz
//		double freq = cores.get(0).getFrequency().getValue();
//		int nbCPUs = cores.size();
//		int cpuCapacity = nbCPUs * 100;  // getCPUCapacity ((int) (freq / 1000000), nbCPUs);
//		int memoryTotal = (int) Utils.getMemory(server) * 1024;
//		int powerIdle = (int) getPIdle(server);
//		int powerPerVM = (int) getPperVM(server);
//		
//		log.debug("creation of an Entropy Node with name " + server.getFrameworkID());
//		log.debug("server is " + server.getStatus().toString());
//		log.debug("nbCPUs " + nbCPUs);
//		log.debug("freq " + freq + " GHz");
//		log.debug("cpuCapacity " + cpuCapacity +" %");
//		log.debug("memoryTotal " + memoryTotal + " MB");
//		log.debug("powerIdle " + powerIdle + " W");
//		log.debug("powerPerVM " + powerPerVM + " W");
//		
//		Node node = new Node(server.getFrameworkID(), nbCPUs, cpuCapacity, memoryTotal, powerIdle, powerPerVM);
//		return node;
//	}
	
	 
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

	
}
