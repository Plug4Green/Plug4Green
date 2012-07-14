package org.f4g.entropy.configuration;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.RequestType;
import org.f4g.schema.allocation.TraditionalVmAllocationType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType.VMType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.util.StaticPowerCalculation;
import org.f4g.util.Util;

import choco.Choco;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.variables.integer.IntDomainVar;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.SimpleNode;
import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import entropy.monitoring.ConfigurationAdapter;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;


public class F4GConfigurationAdapter extends ConfigurationAdapter
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

	public F4GConfigurationAdapter(FIT4GreenType f4g, VMTypeType vmType, IPowerCalculator powerCalculator) {
		currentFit4Green = f4g;
		currentVMType = vmType;
		log = Logger.getLogger(F4GConfigurationAdapter.class.getName());
		allServers = Utils.getAllServers(f4g);
		this.powerCalculator = powerCalculator;
		powerCalculation = new StaticPowerCalculation(null);
	}
	
	
	/* (non-Javadoc)
	 * @see entropy.monitoring.ConfigurationAdapter#extractConfiguration()
	 */
	@Override
	public Configuration extractConfiguration() {
		
		Configuration config = new SimpleConfiguration();
		
		for(ServerType server : Utils.getAllServers(currentFit4Green)) {
			Node node = getNode(server);
			
			if(server.getStatus() == ServerStatusType.ON ||
			   server.getStatus() == ServerStatusType.POWERING_ON) {
				config.addOnline(node);
			
				for(VirtualMachineType VM : Utils.getVMs(server)) {
					VirtualMachine vm = getVM(node, VM);	
					config.setRunOn(vm, node);
				}	
			} else {
				config.addOffline(node);
			}
		}
		return config;
	}
	
	public void insertVMtoAllocate(Configuration config, RequestType request) {
		VirtualMachine VM = getVM(request);	
		config.addWaiting(VM);
	}
	
	
	private VirtualMachine getVM(Node node, VirtualMachineType VM) {
		
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
	
	public VirtualMachine getVM(RequestType request) {
		if(request instanceof CloudVmAllocationType) {
			return getVM((CloudVmAllocationType)request);	
		} else {
			return getVM((TraditionalVmAllocationType)request);
		}
	}
	
	public VirtualMachine getVM(CloudVmAllocationType request) {
		
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
	
	public VirtualMachine getVM(TraditionalVmAllocationType request) {
		
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
		VirtualMachine vm = new SimpleVirtualMachine("new VM", nbCPUs, consumption, memory); //CDU TODO check reference ID                                       
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
		
		Node node = new F4GNode(server.getFrameworkID(), nbCPUs, cpuCapacity, memoryTotal, powerIdle, powerPerVM);
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