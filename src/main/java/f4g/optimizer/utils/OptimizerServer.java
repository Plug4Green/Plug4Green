/**
* ============================== Header ============================== 
* file:          Server.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-04-05 17:37:28 +0200 (jue, 05 abr 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1310 $
* 
* short description:
*   optimizer's type to represent a server. 
*   
* ============================= /Header ==============================
*/

package f4g.optimizer.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


import org.apache.log4j.Logger;
import f4g.commons.com.util.PowerData;
import f4g.commons.optimizer.CloudTraditional.SLAReader;
import f4g.commons.optimizer.CloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.metamodel.*;
import f4g.commons.util.Util;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;


/** 
 *  This class is the internal representation of a server used by the optimizer.
 */	
public class OptimizerServer {
		
	public Logger log;  
		
	/** 
	 * Exception raised when the creation of a new server is impossible
	 */
	public class CreationImpossible extends Exception {}
	
	/** This enumerate allows to know the state of the optimizer during optimization.
	 *  FREE = empty server ready for switch off
	 *  SOURCE = the optimizer will try to empty this server
	 *  TARGET = the optimizer will use this server to put the VMs
	 *  NOT_ASSIGNED = no role is yet decided.
	 */	
	public static enum CandidateState { FREE, SOURCE, TARGET, NOT_ASSIGNED};
	
	CandidateState candidateState;		

	/**
	 * This is the working list of workloads, used during optimization.
	 */
	protected List<OptimizerWorkload> workloads;
	
	public List<OptimizerWorkload> getWorkloads() {
		return workloads;
	}

	public void setWorkloads(List<OptimizerWorkload> workloads) {
		this.workloads = workloads;
	}
 
	public void InitOptimizerServer(final ServerType modelServer, ServerType toServer) throws CreationImpossible{
		
		log = Logger.getLogger(this.getClass().getName());
		
		candidateState = CandidateState.NOT_ASSIGNED;
		
	    
	    List<MainboardType> mainboards = modelServer.getMainboard();
	    MainboardType firstMainboard = null;
	    CPUType firstCPU = null;
	    	    
	    //log.debug("mainboards.size():" + mainboards.size());
	    //check validity
	    if(mainboards.size()!=0){
	    	firstMainboard = modelServer.getMainboard().get(0);
    	    if(firstMainboard.getCPU().size()!=0){
    	    	firstCPU = firstMainboard.getCPU().get(0);
    	    	if(firstCPU.getCore().size()!=0){
    	    	} else {
    	    		log.warn("CPU has no Core!");
    	    		throw new CreationImpossible();
    	    	}    	    		
    	    } else {
    	    	log.warn("Mainboard has no CPU!");
    	    	throw new CreationImpossible();
    	    }
	    } else {
	    	log.warn("Server has no mainboard!");
	    	throw new CreationImpossible();
	    }
	  	    
	    toServer.setName(         modelServer.getName());
	    toServer.setStatus(       modelServer.getStatus());
	    toServer.setFrameworkID(  modelServer.getFrameworkID());
        toServer.setComputedPower(modelServer.getComputedPower());
	    toServer.setMeasuredPower(modelServer.getMeasuredPower());
	            
	    toServer.getMainboard().addAll(   modelServer.getMainboard());
	    toServer.setNativeOperatingSystem(modelServer.getNativeOperatingSystem());
	    toServer.setNativeHypervisor(     modelServer.getNativeHypervisor());
	    toServer.setFrameworkRef(         modelServer.getFrameworkRef());
	    	
	}
	
	/**
	 * Server constructor for Cloud
	 */
	public OptimizerServer(final ServerType modelServer, final VMTypeType myVMTypes, ServerType toServer) throws CreationImpossible{

		InitOptimizerServer(modelServer, toServer);
		
	    workloads = new ArrayList<OptimizerWorkload>();
	    
	    int i=0;
		for(VirtualMachineType VM : getVMs(toServer)) { //getAllOptimizerWorkload
			//TODO: for now, the VM ids in the server 1000 are of the form 11001,11002...
						
			try {
				
				VMTypeType.VMType SLA_VM = Util.findVMByName(VM.getCloudVmType(), myVMTypes);
				OptimizerWorkload optimizerWorkload = new OptimizerWorkload(SLA_VM, VM.getFrameworkID());
				workloads.add(optimizerWorkload); 
				i++;
				
			} catch (NoSuchElementException e) {
				log.warn("VM type \"" + VM.getCloudVmType() + "\" not found in SLA, not taken into account");
			}
			
		}
		 
	}
	
	/**
	 * Server constructor for traditional
	 * if the first parameter is not null, it take these workloads. Otherwise it gets them from the model server.
	 */
	public OptimizerServer(final List<OptimizerWorkload> WLs, final ServerType modelServer, ServerType toServer) throws CreationImpossible{

		InitOptimizerServer(modelServer, toServer);
		
		if(WLs == null)
			workloads = getAllOptimizerWorkload(toServer);
		else
			workloads = WLs;
	}
	

	/**
	 * Default constructor
	 */
	public OptimizerServer() {
	}

//	public double getNICMaxPower(ServerType toServer) {
//		double nic_maxPower = 0;
//		for(MainboardType myMainboard : toServer.getMainboard())
//	    	for(NICType nic : myMainboard.getNIC())
//	    		nic_maxPower = nic.getPowerMax().getValue();
//		return nic_maxPower;
//	}
//
//
//	public double getNICIdlePower(ServerType toServer) {
//		double nic_idlePower = 0;
//		for(MainboardType myMainboard : toServer.getMainboard())
//	    	for(NICType nic : myMainboard.getNIC())
//	    		nic_idlePower = nic.getPowerIdle().getValue();
//		return nic_idlePower;
//	}
//
//
//	public double getIdlePower(ServerType toServer) {
//		double mp_idlePower = 0; 
//    	for(MainboardType myMainboard : toServer.getMainboard())
//    		mp_idlePower += myMainboard.getPowerIdle().getValue();
//    	return mp_idlePower;
//	}
//
//
//	public int getMaxPower(ServerType toServer) {
//		int mp_maxPower = 0;
//    	for(MainboardType myMainboard : toServer.getMainboard())
//    		mp_maxPower += myMainboard.getPowerMax().getValue();
//    	return mp_maxPower;
//	}


	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizerServer#getNbCores()
	 */
	public int getNbCores(ServerType toServer) {
		int nr_cores = 0;
	    for(MainboardType myMainboard : toServer.getMainboard())	
	    	for(CPUType CPU : myMainboard.getCPU())
	    		nr_cores += CPU.getCore().size();
	    return nr_cores;
	}


	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizerServer#getNbCPU()
	 */
	public int getNbCPU(ServerType toServer) {
		int nr_cpu = 0;
	    for(MainboardType myMainboard : toServer.getMainboard())
	    	nr_cpu += myMainboard.getCPU().size();
	    return nr_cpu;
	}	
	
	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizerServer#getMemory()
	 */
	public long getMemory(ServerType toServer) {
		int memory=0;
		for(MainboardType myMainboard : toServer.getMainboard())
	    	for(RAMStickType RAMStick : myMainboard.getRAMStick())
	    		memory += RAMStick.getSize().getValue();
		return memory;
	}
	
	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizerServer#getStorage()
	 */
	public double getStorage(ServerType server) {
		double storage = 0;
		for(StorageUnitType storageUnit : Utils.getAllStorages(server))
	   		storage += storageUnit.getStorageCapacity().getValue();
		return storage;
	}
	
	
	public double getNICBandwidth(ServerType server) {
		double bandwidth = 0;
		for(NICType nic : Utils.getAllNIC(server))
			bandwidth += nic.getProcessingBandwidth().getValue();
		return bandwidth;
	}
	
		
	public List<OptimizerWorkload> getAllOptimizerWorkload(ServerType server){
		
		List<OptimizerWorkload> optimizerWorkloads = new ArrayList<OptimizerWorkload>();
		

		if(server.getNativeHypervisor() != null){
			for(VirtualMachineType VM : server.getNativeHypervisor().getVirtualMachine()) {
				try {
					optimizerWorkloads.add(new OptimizerWorkload(VM));
				} catch (OptimizerWorkload.CreationImpossible e) {
					log.warn("getAllOptimizerWorkload: CreationImpossible");
				}
			}			
		}
			
		if(server.getNativeOperatingSystem() != null) {
			for(HostedHypervisorType hostedHypervisor : server.getNativeOperatingSystem().getHostedHypervisor()) {
				for(VirtualMachineType VM : hostedHypervisor.getVirtualMachine()) { 
					try {
						optimizerWorkloads.add(new OptimizerWorkload(VM));
					} catch (OptimizerWorkload.CreationImpossible e) {
						log.warn("getAllOptimizerWorkload: CreationImpossible");
					}
				}
			}			
		}
			
		return optimizerWorkloads;
	}
	
	public PowerData getPower(IPowerCalculator powerCalculator, ServerType server){
		return powerCalculator.computePowerServer(server);
	}
	
	public void setCandidateState(CandidateState candidateState) {
		this.candidateState = candidateState;
	}
	
	public String getCandidateState() {
		return candidateState.toString();
	}
	

	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizerServer#getLoadRate(org.f4g.optimizer.AggregatedUsage, java.lang.Boolean)
	 */
	public double getLoadRate(AggregatedUsage reference, AlgoType algoType)
	{
		AggregatedUsage serverUsage = AggregatedUsage.getAggregatedUsage(workloads);
		
		return AggregatedUsage.loadRate(serverUsage, reference, algoType);
	}
	
	//returns the VMs of the server (no copying)
	List<VirtualMachineType> getVMs(ServerType server){
		
		if(server.getNativeHypervisor() != null)
			return server.getNativeHypervisor().getVirtualMachine(); 
		else if (server.getNativeOperatingSystem() != null && server.getNativeOperatingSystem().getHostedHypervisor().size() != 0)
			return server.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		else 
			return new ArrayList<VirtualMachineType>();
	}
	
	
	/* 
	 * adds a VM to a server, simulating the increase of every loads
	 * WARNING: workloads field is NOT updated
	 */
	public void addVM(final OptimizerWorkload WL, final AlgoType algoType, ServerType toServer){

		Utils.addVM(WL, toServer, algoType);
		
	}
		
    public ServerStatusType getStatus(ServerType server){
    	return server.getStatus();
    }

    public void setStatus(ServerStatusType value, ServerType server) {
    	server.setStatus(value);
    }
    

    public Object clone() {
        return copyTo(createNewInstance());
    }

    public Object copyTo(Object target) {
        final CopyStrategy strategy = JAXBCopyStrategy.INSTANCE;
        return copyTo(null, target, strategy);
    }

    public Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy) {
        final Object draftCopy = ((target == null)?createNewInstance():target);

        if (draftCopy instanceof OptimizerServer) {
        	final OptimizerServer copy = ((OptimizerServer) draftCopy);
        	copy.candidateState = this.candidateState;
            
            if (this.workloads!= null) {
                List<OptimizerWorkload> sourceWorkloads;
                sourceWorkloads = this.getWorkloads();
                List<OptimizerWorkload> copyWorkloads = new ArrayList<OptimizerWorkload>();
                for(OptimizerWorkload WL : sourceWorkloads)
                	copyWorkloads.add((OptimizerWorkload) WL.clone());
                copy.workloads = copyWorkloads;
            } else {
                copy.workloads = null;
            }
            
            copy.log = this.log;
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new OptimizerServer();
    }


}
