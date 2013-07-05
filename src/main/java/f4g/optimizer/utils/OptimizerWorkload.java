/**
* ============================== Header ============================== 
* file:          Workload.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-02-28 17:59:36 +0100 (mar, 28 feb 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1177 $
* 
* short description:
*   optimizer's type to represent a workload 
*   
* ============================= /Header ==============================
*/

package org.f4g.optimizer.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.StorageUsageType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.allocation.*;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;


/**
 * Optimizer internal representation for a workload
 */
public class OptimizerWorkload extends VirtualMachineType implements Cloneable, CopyTo{
	
	public Logger log;  
	
	/**
	 * Exception raised when the creation of a new Workload is impossible
	 */
	public class CreationImpossible extends Exception {}
	
	/**
	 * Making a Workload from a VirtualMachineType from model (Traditional)
	 */
	public OptimizerWorkload(VirtualMachineType VM) throws CreationImpossible{
		
		log = Logger.getLogger(OptimizerWorkload.class.getName()); 
		
		//checkings
		if( VM.getFrameworkID() == null ){
			log.warn("Missing FrameworkID in VM");
			throw new CreationImpossible();
		}
		if( VM.getNumberOfCPUs() == null ){
			log.warn("Missing NumberOfCPUs in VM");
			throw new CreationImpossible();
		}
		if( VM.getActualMemoryUsage() == null ){
			log.warn("Missing ActualMemoryUsage in VM");
			throw new CreationImpossible();
		}
		if( VM.getActualStorageUsage() == null ){
			log.warn("Missing ActualStorageUsage in VM");
			throw new CreationImpossible();
		}
		if( VM.getActualNetworkUsage() == null ){
			log.warn("Missing ActualNetworkUsage in VM");
			throw new CreationImpossible();
		}
		if( VM.getActualCPUUsage() == null ){
			log.warn("Missing ActualCPUUsage in VM");
			throw new CreationImpossible();
		}	
		if( VM.getActualDiskIORate() == null ){
			log.warn("Missing ActualDiskIORate in VM");
			throw new CreationImpossible();
		}		
				    
		//copy constructor.
		name = VM.getName();
	    frameworkID = VM.getFrameworkID();
	    frameworkRef = VM.getFrameworkRef();
	    numberOfCPUs = VM.getNumberOfCPUs();
	    actualCPUUsage = VM.getActualCPUUsage();
	    actualStorageUsage = VM.getActualStorageUsage();
	    actualDiskIORate = VM.getActualDiskIORate();
	    actualMemoryUsage = VM.getActualMemoryUsage();
	    actualNetworkUsage = VM.getActualNetworkUsage();
	    hostedOperatingSystem = VM.getHostedOperatingSystem();
	    cloudVmImage = VM.getCloudVmImage();
	    cloudVmType = VM.getCloudVmType();
	   
	}
	
	/**
	 * Making a Workload from a VirtualMachineType from model (Traditional)
	 */
	public OptimizerWorkload(TraditionalVmAllocationType allocation, String frameWorkID) {

		setFrameworkID(frameWorkID);
		if(allocation.getNumberOfCPUs() != null ) setNumberOfCPUs(new NrOfCpusType(allocation.getNumberOfCPUs())); //in a VM, the number of CPU is in fact the number of cores
		else setNumberOfCPUs(new NrOfCpusType(0)); 
		if( allocation.getMemoryUsage() != null)  setActualMemoryUsage(new MemoryUsageType(allocation.getMemoryUsage())); 
		else setActualMemoryUsage(new MemoryUsageType(0));			
		if( allocation.getStorageUsage() != null) setActualStorageUsage(new StorageUsageType(allocation.getStorageUsage()));
		else setActualStorageUsage(new StorageUsageType(0));
		if( allocation.getCPUUsage() != null)     setActualCPUUsage(new CpuUsageType(allocation.getCPUUsage()));	
		else setActualCPUUsage(new CpuUsageType(0));	
		if( allocation.getNetworkUsage() != null) setActualNetworkUsage(new NetworkUsageType(allocation.getNetworkUsage()));	
		else setActualNetworkUsage(new NetworkUsageType(0));			
		if( allocation.getDiskIORate() != null)   setActualDiskIORate(new IoRateType(allocation.getDiskIORate()));
		else setActualDiskIORate(new IoRateType(0));
	}
	
	/**
	 * making a Workload from a VM from the SLA (Cloud)
	 */
	public OptimizerWorkload(VMTypeType.VMType VM, String frameWorkID) {

		setFrameworkID(frameWorkID);
		setNumberOfCPUs(VM.getCapacity().getVCpus());
		setActualMemoryUsage(new MemoryUsageType(VM.getCapacity().getVRam().getValue())); //TODO fix
		setActualStorageUsage(new StorageUsageType((double)VM.getCapacity().getVHardDisk().getValue())); //TODO fix or delete
		setActualCPUUsage(VM.getExpectedLoad().getVCpuLoad()); 
        setActualNetworkUsage(new NetworkUsageType(0.0)); 
        setActualDiskIORate(new IoRateType(0.0));
	}
			

    /**
	 * 
	 */
	public OptimizerWorkload() {
		// TODO Auto-generated constructor stub
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
        super.copyTo(locator, draftCopy, strategy);
        if (draftCopy instanceof OptimizerWorkload) {
        	final OptimizerWorkload copy = ((OptimizerWorkload) draftCopy);
            
            copy.log = this.log;
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new OptimizerWorkload();
    }
    
}

