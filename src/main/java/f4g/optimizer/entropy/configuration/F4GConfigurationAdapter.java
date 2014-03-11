package f4g.optimizer.entropy.configuration;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.RequestType;
import f4g.schemas.java.allocation.TraditionalVmAllocationType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType.VMType;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import f4g.commons.util.StaticPowerCalculation;
import f4g.commons.util.Util;


/**
 * Adapter to translate FIT4Green configurations to Entropy
 *
 * @author Corentin Dupont
 */
public class F4GConfigurationAdapter
{
	FIT4GreenType currentFit4Green;
	VMTypeType currentVMType;
	
	private Logger log;
	private List<ServerType> allServers;
	private OptimizationObjective optiObjective;
	private IPowerCalculator powerCalculator;
	


	private StaticPowerCalculation powerCalculation;
	
    public FIT4GreenType getCurrentFIT4Green() {
		return currentFit4Green;
	}

	public void setCurrentFIT4Green(FIT4GreenType currentFIT4Green) {
		this.currentFit4Green = currentFIT4Green;
	}

	public F4GConfigurationAdapter(FIT4GreenType f4g, VMTypeType vmType, IPowerCalculator powerCalculator, OptimizationObjective optiObjective) {
		currentFit4Green = f4g;
		currentVMType = vmType;
		log = Logger.getLogger(F4GConfigurationAdapter.class.getName());
		allServers = Utils.getAllServers(f4g);
		this.powerCalculator = powerCalculator;
		powerCalculation = new StaticPowerCalculation(null);
		this.optiObjective = optiObjective;
	}
	
	
	/* 
	 * extracts the configuration for the metamodel
	 */
	public void putConfiguration(Model mo) {
				
		for(ServerType server : Utils.getAllServers(currentFit4Green)) {
			
			Node node = mo.newNode();
			
			if(server.getStatus() == ServerStatusType.ON ||
			   server.getStatus() == ServerStatusType.POWERING_ON) { //POWERING_ON nodes are seen as ON by entropy as they will be soon on. This avoids ping-pong effect on the state.
				
				mo.getMapping().addOnlineNode(node);
			
				for(VirtualMachineType VM : Utils.getVMs(server)) {
					VM vm = mo.newVM();	
					mo.getMapping().addRunningVM(vm, node);
				}	
			} else { //OFF, POWERING_OFF
				mo.getMapping().addOfflineNode(node);
			}
		}
	}
	
	public void insertVMtoAllocate(Configuration config, RequestType request) {
		VirtualMachine VM = getVM(request);	
		config.addWaiting(VM);
	}
	
	
	private VM getVM(Node node, VirtualMachineType VM) {
		
		//VM type should be set OR some measurements should be present
		if(VM.getCloudVmType() == null &&
		   (VM.getNumberOfCPUs() == null ||
			VM.getActualCPUUsage() == null ||
			VM.getActualMemoryUsage() == null)) {
			log.error("VM type not set for " + VM.getFrameworkID() + " and no VM measures found");
			System.exit(-1);
		}
		
		VMTypeType.VMType SLA_VM = null;
		if(VM.getCloudVmType() != null) {
			SLA_VM = Util.findVMByName(VM.getCloudVmType(), currentVMType);	
		}
		
				
		//If the measured values are present in the VM, we take these.
		//otherwise, we take the specification values from the SLA.
		int nbCPUs;
		if(VM.getNumberOfCPUs() != null) {
			nbCPUs = VM.getNumberOfCPUs().getValue();
		} else {
			nbCPUs = SLA_VM.getCapacity().getVCpus().getValue();
		}
		
		double CPUUsage;
		if(VM.getActualCPUUsage() != null) {
			CPUUsage = VM.getActualCPUUsage().getValue();
		} else {
			CPUUsage = SLA_VM.getExpectedLoad().getVCpuLoad().getValue();
		}
		
		int memory;
		if(VM.getActualMemoryUsage() != null) {
			memory = (int) (VM.getActualMemoryUsage().getValue() * 1024);
		} else {
			memory = (int) (SLA_VM.getCapacity().getVRam().getValue() * 1024);
		}
		
		int consumption = (int) (CPUUsage * nbCPUs);
				
		log.debug("creation of an Entropy VM with name " + VM.getFrameworkID());
		log.debug("nbCPUs " + nbCPUs);
		log.debug("consumption " + consumption + " %");
		log.debug("memory " + memory + " MB");
		VirtualMachine vm = new SimpleVirtualMachine(VM.getFrameworkID(), nbCPUs, consumption, memory);                                       
		return vm;
	}
	
	public VM getVM(RequestType request) {
		if(request instanceof CloudVmAllocationType) {
			return getVM((CloudVmAllocationType)request);	
		} else {
			return getVM((TraditionalVmAllocationType)request);
		}
	}
	
	public VM getVM(CloudVmAllocationType request) {
		
		VMTypeType.VMType SLA_VM;
		
		try {
			SLA_VM = Util.findVMByName(request.getVmType(), currentVMType);
		} catch (NoSuchElementException e1) {
			log.error("VM name " + request.getVmType() + " could not be found in SLA");
			return null;
		}

		int nbCPUs = SLA_VM.getCapacity().getVCpus().getValue();
		//the consumption of a VM if set as a percentage * number of CPU.
		//it should be demanding a certain CPU power instead.
		int consumption = (int) (nbCPUs * SLA_VM.getExpectedLoad().getVCpuLoad().getValue()); //(int) (SLA_VM.getExpectedLoad().getVCpuLoad().getValue() * REFERENCE_CPU_SPEED);
		int memory = (int) (SLA_VM.getCapacity().getVRam().getValue() * 1024);
		
		log.debug("creation of an Entropy VM for allocation request");
		log.debug("nbCPUs " + nbCPUs);
		log.debug("consumption " + consumption + " %");
		log.debug("memory " + memory + " MB");
		VirtualMachine vm = new SimpleVirtualMachine("new VM", nbCPUs, consumption, memory); 
		
		return vm;
	}
	
	public VM getVM(TraditionalVmAllocationType request) {
		
		int nbCPUs;
		if(request.getNumberOfCPUs() != null) nbCPUs = request.getNumberOfCPUs();
		else nbCPUs = 1;
		//the consumption of a VM if set as a percentage * number of CPU.
		//it should be demanding a certain CPU power instead.
		
		double CPUUsage;
		if(request.getCPUUsage() != null) CPUUsage = request.getCPUUsage();
		else CPUUsage = 100;
		
		int consumption = (int) (nbCPUs * CPUUsage);
		
		int memory;
		if(request.getMemoryUsage() != null) memory = (int) (request.getMemoryUsage() * 1.0 * 1024);
		else memory = 1;

		log.debug("creation of an Entropy VM for allocation request");
		log.debug("nbCPUs " + nbCPUs);
		log.debug("consumption " + consumption + " %");
		log.debug("memory " + memory + " MB");
		VM vm = new VM("new VM", nbCPUs, consumption, memory);                                        
		return vm;
	}

	private Node getNode(ServerType server) {
		ArrayList<CoreType> cores = Utils.getAllCores(server.getMainboard().get(0));
		
		//freq in Hz
		double freq = cores.get(0).getFrequency().getValue();
		int nbCPUs = cores.size();
		int cpuCapacity = nbCPUs * 100;  // getCPUCapacity ((int) (freq / 1000000), nbCPUs);
		int memoryTotal = (int) Utils.getMemory(server) * 1024;
		int powerIdle = (int) getPIdle(server);
		int powerPerVM = (int) getPperVM(server);
		
		log.debug("creation of an Entropy Node with name " + server.getFrameworkID());
		log.debug("server is " + server.getStatus().toString());
		log.debug("nbCPUs " + nbCPUs);
		log.debug("freq " + freq + " GHz");
		log.debug("cpuCapacity " + cpuCapacity +" %");
		log.debug("memoryTotal " + memoryTotal + " MB");
		log.debug("powerIdle " + powerIdle + " W");
		log.debug("powerPerVM " + powerPerVM + " W");
		
		Node node = new Node(server.getFrameworkID(), nbCPUs, cpuCapacity, memoryTotal, powerIdle, powerPerVM);
		return node;
	}
	
	 
	public float getPIdle(ServerType server) {
      
		double ue = getUsageEffectiveness(server);		
		
    	ServerStatusType status = server.getStatus();
    	server.setStatus(ServerStatusType.ON); //set the server status to ON to avoid a null power
        float powerIdle = (float) (powerCalculation.computePowerIdle(server, powerCalculator) * ue);
        server.setStatus(status);
                    
        return powerIdle;        
    }
    

    public float getPperVM(ServerType server) {
                
    	double ue = getUsageEffectiveness(server);	
    	
        VMType vm = currentVMType.getVMType().get(0);
    	ServerStatusType status = server.getStatus();
    	server.setStatus(ServerStatusType.ON); //set the server status to ON to avoid a null power
    	float PperVM = (float) (powerCalculation.computePowerForVM(server, vm, powerCalculator) * ue);
        server.setStatus(status);
            
        return PperVM;        
    }

	private double getUsageEffectiveness(ServerType server) {
    	if(optiObjective == OptimizationObjective.Power) {
    		return Utils.getServerSite(server, currentFit4Green).getPUE().getValue();
    	} else {
    		return Utils.getServerSite(server, currentFit4Green).getCUE().getValue();
    	}
	}

	
}