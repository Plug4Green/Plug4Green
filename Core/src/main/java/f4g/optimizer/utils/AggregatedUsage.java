/**
* ============================== Header ============================== 
* file:          AggregatedUsageData.java
* project:       FIT4Green/Optimizer
* created:       12 janv. 2011 by cdupont
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   this class is a tool to gather resources and make operations on them.
* ============================= /Header ==============================
*/
package f4g.optimizer.utils;

import java.util.List;

import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.VirtualMachine;

/**
 * this class is a tool to gather resources and make operations on them.
 * @author  cdupont
 */
public class AggregatedUsage {

	public static Logger log = Logger.getLogger("AggregatedUsage");
	
	//total number of cores
	public double aggregatedNbCores;
	//mean CPU usage (between all CPUs)
	public double aggregatedCPUUsage;
	//total storage usage
	public double aggregatedStorageUsage;
	//total disk IO rate
	public double aggregatedDiskIORate;
	//total memory usage
	public double aggregatedMemoryUsage;
	//total network bandwidth usage
	public double aggregatedNetworkUsage;
	
	
	/**
	 * Empty constructor
	 */
	public AggregatedUsage() {
		aggregatedCPUUsage     = 0.0;
		aggregatedStorageUsage = 0.0;
		aggregatedDiskIORate   = 0.0;
		aggregatedMemoryUsage  = 0.0;
		aggregatedNetworkUsage = 0.0;
		aggregatedNbCores      = 0.0;
		
		log = Logger.getLogger(this.getClass().getName());		
	}
	
	/**
	 * Copy constructor
	 */
	public AggregatedUsage(AggregatedUsage usage) {
		aggregatedCPUUsage     = usage.aggregatedCPUUsage    ;
		aggregatedStorageUsage = usage.aggregatedStorageUsage;
		aggregatedDiskIORate   = usage.aggregatedDiskIORate  ;
		aggregatedMemoryUsage  = usage.aggregatedMemoryUsage ;
		aggregatedNetworkUsage = usage.aggregatedNetworkUsage;
		aggregatedNbCores      = usage.aggregatedNbCores     ;
		
		log = Logger.getLogger(this.getClass().getName());		
	}
	
	/**
	 * construct a AggregatedUsage from a VirtualMachine
	 */
	public static <T extends VirtualMachine> AggregatedUsage getAggregatedUsage(T VM){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();
	
		//Aggregated CPU usage is needed only in Traditional.
		aggregatedUsage.aggregatedCPUUsage     = VM.getActualCPUUsage().getValue();
		aggregatedUsage.aggregatedStorageUsage = VM.getActualStorageUsage().getValue(); 
		aggregatedUsage.aggregatedDiskIORate   = VM.getActualDiskIORate().getValue(); 
		aggregatedUsage.aggregatedMemoryUsage  = VM.getActualMemoryUsage().getValue(); 
		aggregatedUsage.aggregatedNetworkUsage = VM.getActualNetworkUsage().getValue(); 
		aggregatedUsage.aggregatedNbCores      = VM.getNumberOfCPUs().getValue();
		return aggregatedUsage;
	}
	
	/**
	 * construct a AggregatedUsage from a IOptimizerServer
	 */
	public static AggregatedUsage getAggregatedUsage(IOptimizerServer server){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();
	
		//Aggregated CPU usage is needed only in Traditional.
		aggregatedUsage.aggregatedCPUUsage     = 100.0;
		aggregatedUsage.aggregatedStorageUsage = server.getStorage();
		aggregatedUsage.aggregatedDiskIORate   = 0.0; //TODO
		aggregatedUsage.aggregatedMemoryUsage  = server.getMemory();
		aggregatedUsage.aggregatedNetworkUsage = server.getNICBandwidth();
		aggregatedUsage.aggregatedNbCores      = server.getNbCores();
		return aggregatedUsage;
	}
	
	/**
	 * construct a AggregatedUsage from a list of VirtualMachine
	 */
	public static AggregatedUsage getAggregatedUsage(List<? extends VirtualMachine> workloads){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();

		double CPUUsageSum = 0;
		for (VirtualMachine VM : workloads) {
						
			AggregatedUsage VMU = getAggregatedUsage(VM);
			
			//log.debug("VM usage " + VM.getFrameworkID() + ":\n" + VMU.show());
			aggregatedUsage = add(aggregatedUsage, VMU);

			CPUUsageSum += VMU.aggregatedCPUUsage * VMU.aggregatedNbCores / 100.0;
		}
		
		//log.debug("all VMs CPUUsageSum " + CPUUsageSum);
		
		//CPU Usage is the mean load between all CPUs.
		if(aggregatedUsage.aggregatedNbCores != 0){
			aggregatedUsage.aggregatedCPUUsage = CPUUsageSum * 100 / aggregatedUsage.aggregatedNbCores;
			
		} else {
			aggregatedUsage.aggregatedCPUUsage = 0.0;
		}
			
		
		
		return aggregatedUsage;
	}
	
	/**
	 * substraction of two AggregatedUsage
	 * usually we substract a server capacity with all its WM usages, to get the remaining capacity of the server
	 */
	public static AggregatedUsage substract(AggregatedUsage usage1, AggregatedUsage usage2){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();
	
		aggregatedUsage.aggregatedStorageUsage = usage1.aggregatedStorageUsage - usage2.aggregatedStorageUsage;
		aggregatedUsage.aggregatedDiskIORate   = usage1.aggregatedDiskIORate   - usage2.aggregatedDiskIORate  ;
		aggregatedUsage.aggregatedMemoryUsage  = usage1.aggregatedMemoryUsage  - usage2.aggregatedMemoryUsage ;
		aggregatedUsage.aggregatedNetworkUsage = usage1.aggregatedNetworkUsage - usage2.aggregatedNetworkUsage;
		
		aggregatedUsage.aggregatedCPUUsage     = 0.0;
		aggregatedUsage.aggregatedNbCores      = usage1.aggregatedNbCores      - usage2.aggregatedNbCores     ;
		
		return aggregatedUsage;
	}
	
	/**
	 * addition of two AggregatedUsage
	 * the case of the CPU usage, which is a mean, must be treated separatly
	 */
	public static AggregatedUsage add(AggregatedUsage usage1, AggregatedUsage usage2){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();
	
		aggregatedUsage.aggregatedStorageUsage = usage1.aggregatedStorageUsage + usage2.aggregatedStorageUsage;
		aggregatedUsage.aggregatedDiskIORate   = usage1.aggregatedDiskIORate   + usage2.aggregatedDiskIORate  ;
		aggregatedUsage.aggregatedMemoryUsage  = usage1.aggregatedMemoryUsage  + usage2.aggregatedMemoryUsage ;
		aggregatedUsage.aggregatedNetworkUsage = usage1.aggregatedNetworkUsage + usage2.aggregatedNetworkUsage;
		aggregatedUsage.aggregatedNbCores      = usage1.aggregatedNbCores      + usage2.aggregatedNbCores     ;
		
		return aggregatedUsage;
	}
	
	/**
	 * division of two AggregatedUsage
	 */
	public static AggregatedUsage divide(AggregatedUsage usage1, AggregatedUsage usage2){
		
		AggregatedUsage aggregatedUsage = new AggregatedUsage();
	
		aggregatedUsage.aggregatedCPUUsage     = (usage2.aggregatedCPUUsage     != 0 ? usage1.aggregatedCPUUsage     / usage2.aggregatedCPUUsage     : 0.0);
		aggregatedUsage.aggregatedStorageUsage = (usage2.aggregatedStorageUsage != 0 ? usage1.aggregatedStorageUsage / usage2.aggregatedStorageUsage : 0.0);
		aggregatedUsage.aggregatedDiskIORate   = (usage2.aggregatedDiskIORate   != 0 ? usage1.aggregatedDiskIORate   / usage2.aggregatedDiskIORate   : 0.0);
		aggregatedUsage.aggregatedMemoryUsage  = (usage2.aggregatedMemoryUsage  != 0 ? usage1.aggregatedMemoryUsage  / usage2.aggregatedMemoryUsage  : 0.0);
		aggregatedUsage.aggregatedNetworkUsage = (usage2.aggregatedNetworkUsage != 0 ? usage1.aggregatedNetworkUsage / usage2.aggregatedNetworkUsage : 0.0);
		aggregatedUsage.aggregatedNbCores      = (usage2.aggregatedNbCores      != 0 ? usage1.aggregatedNbCores      / usage2.aggregatedNbCores      : 0.0);
				
		return aggregatedUsage;
	}
	
	/**
	 * load rate is defined as max(nCPU/maxCPU, nMem/maxMem, nStor/MaxStore))
	 */
	public static double loadRate(AggregatedUsage server, AggregatedUsage reference){
		
		AggregatedUsage aggregatedRates = divide(server, reference);
		return max3(aggregatedRates.aggregatedNbCores, aggregatedRates.aggregatedMemoryUsage, aggregatedRates.aggregatedStorageUsage);	
	
	}
	

	public static double max3 (double a, double b, double c) {
		return Math.max(Math.max(a, b), c);
	}
	
	/**
	 * tests if an AggregatedUsage (typicaly from a VM) fits in another AggregatedUsage (typicaly from a Server)
	 */
	public static Boolean fitsIn(AggregatedUsage VMUsage, AggregatedUsage ServerUsage) {
		
		Boolean CPUfits;
		Boolean NetworkFits;
		
		//In Traditional, several VMs can be packed in one CPU.
		//In cloud, number of cores requirements must be strictly followed.
		CPUfits = VMUsage.aggregatedNbCores <= ServerUsage.aggregatedNbCores;
		NetworkFits = true; //No network in Cloud now
			
		return CPUfits
		    && (VMUsage.aggregatedStorageUsage <= ServerUsage.aggregatedStorageUsage)
		    //&& (VMUsage.aggregatedDiskIORate   <= ServerUsage.aggregatedDiskIORate)
		    && (VMUsage.aggregatedMemoryUsage  <= ServerUsage.aggregatedMemoryUsage)
		    && NetworkFits; 
		  
	}
	
	public String show() {

		return 	"aggregatedCPUUsage = "     + aggregatedCPUUsage     + "\n" +
				"aggregatedStorageUsage = " + aggregatedStorageUsage + "\n" +
				"aggregatedDiskIORate = "   + aggregatedDiskIORate   + "\n" +
				"aggregatedMemoryUsage = "  + aggregatedMemoryUsage  + "\n" +
				"aggregatedNetworkUsage = " + aggregatedNetworkUsage + "\n" +
				"aggregatedNbCores = "      + aggregatedNbCores;      
	}
}
