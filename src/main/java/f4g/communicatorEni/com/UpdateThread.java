/**
* ============================== Header ============================== 
* file:          updateThread.java
* project:       FIT4Green/CommunicatorEni
* created:       11/07/2011 by jclegea
* 
* $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1497 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorEni.com;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.log4j.Logger;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;

import f4g.communicatorEni.vmware.PerformanceInformation;
import f4g.communicatorEni.vmware.RetrieveHostInformation;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public class UpdateThread extends Thread{
	static Logger log = Logger.getLogger(UpdateThread.class.getName());
	int j;
	int cpuInterval_;
	private RetrieveHostInformation hostsInformation_;
	ArrayList <String> type = null;
	ArrayList <Integer> counters = null;
	ArrayList <Double> cpuLoadAverage_ = null;
	ArrayList<PerformanceInformation> valueRetrieved = null;
	ComOperationCollector operationSet = null;
	private boolean isPoweredOn = true;
	
	public UpdateThread(int jArg,RetrieveHostInformation hostsInformation, ArrayList <String> typeArg, ArrayList <Integer> countersArg,ArrayList<PerformanceInformation> valueRetrievedArg, ComOperationCollector operationSetArg, int cpuInterval){
		j = jArg;		
		hostsInformation_ = hostsInformation;
		type = typeArg;
		counters = countersArg;
		valueRetrieved = valueRetrievedArg;
		operationSet = operationSetArg;
		cpuInterval_ = cpuInterval;
	}
	
	public boolean isPoweredOn(){					
		return isPoweredOn;
	}

	public void run() {		
		
		String hostVirtualMachineName = null;
		ComOperation operation;
		long numberReads = -1;
		long numberWrites = -1;
		long packetsTransmited = -1;
		long packetsRecieved = -1;
		double cpuLoadHistory = -1;
		double cpuUsage = -1;
		int k=0;
		
		hostVirtualMachineName = hostsInformation_.getHostVirtualMachineName(j);

		// UPDATE node		
		if (hostVirtualMachineName != null) {
			
			// Get CPULoad average				
			ArrayList <String> typeCPULoad = new ArrayList<String>();
			ArrayList <Integer> countersCPULoad = new ArrayList<Integer>();			
			Calendar endTime = Calendar.getInstance();
			Calendar startTime = Calendar.getInstance();
			startTime.add(Calendar.MINUTE,-cpuInterval_);
			valueRetrieved.clear();
			typeCPULoad.add("cpu");			
			countersCPULoad.add(ComEniConstants.CPU_USAGE);
			valueRetrieved = hostsInformation_.getPerformanceHistory(hostVirtualMachineName, "VirtualMachine", typeCPULoad, countersCPULoad, startTime, endTime, 20);
			
			// Retrieve History values for CPULoad
			if(valueRetrieved.size() > 0){
				cpuLoadHistory = 0;
				for(int i=0;i<valueRetrieved.size();i++){
					cpuLoadHistory += valueRetrieved.get(i).getValue();
				}
				
				cpuLoadHistory = cpuLoadHistory / valueRetrieved.size();				
				cpuUsage = cpuLoadHistory / 100;
				operation = new ComOperation(
						ComOperation.TYPE_UPDATE,
						"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName + "']/actualCPUUsage",
						String.valueOf(cpuUsage));
				operationSet.add(operation);				
			}
			else{
				log.debug("virtual machine " + hostVirtualMachineName + " is powered off");
				isPoweredOn = false;				
			}

			
			

			// Add Types to retrieve information
//			type.add("cpu");
			type.add("mem");			
			// type.add("disk");
			type.add("net");
			type.add("net");
			type.add("disk");
			type.add("disk");
//			counters.add(ComEniConstants.CPU_USAGE);
			counters.add(ComEniConstants.TOTAL_MEM);			
			// counters.add(ComEniConstants.STORAGE_USAGE);
			counters.add(ComEniConstants.PACKETS_RECIEVED);
			counters.add(ComEniConstants.PACKETS_TRANSMITED);
			counters.add(ComEniConstants.DISK_READ);
			counters.add(ComEniConstants.DISK_WRITE);

			valueRetrieved.clear();
			valueRetrieved = hostsInformation_.getPerformance(hostVirtualMachineName, "VirtualMachine", type, counters,20);

			//log.debug("valueRetrieved UpdateThread: " + valueRetrieved.size());
			// for(Iterator<Double> it = valueRetrieved.iterator();it.hasNext();){
			// log.debug("ValueRetrieved: " + it.next());
			// }

			// CPU Performance			
//			if (valueRetrieved != null && valueRetrieved.size() > 0 && valueRetrieved.get(0).getValue() != -1) {				
//				cpuUsage = valueRetrieved.get(0).getValue() / 100;
//				operation = new ComOperation(
//						ComOperation.TYPE_UPDATE,
//						"//actualCPUUsage[../frameworkID='" + hostVirtualMachineName + "']",
//						String.valueOf(cpuUsage));
//				operationSet.add(operation);
//			}

			
			

			k=0;
			packetsTransmited = 0;
			numberReads = 0;
			for(Iterator<PerformanceInformation> it = valueRetrieved.iterator();it.hasNext();){
				it.next();
				
				// Memory usage in Gigas
				if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "Memory".equals(valueRetrieved.get(k).getType()) && isPoweredOn() == true){					
					operation = new ComOperation(ComOperation.TYPE_UPDATE,
					"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName
					+ "']/actualMemoryUsage", String.valueOf(valueRetrieved.get(k).getValue()/ComEniConstants.KYLOBYTES_TO_GIGABYTES));
					operationSet.add(operation);
				}
	
				// Network Usage
				if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "Net".equals(valueRetrieved.get(k).getType()) && isPoweredOn() == true){				
					packetsTransmited += valueRetrieved.get(1).getValue().longValue();
				}				
				
				
				// Disk Usage
				if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "Disk".equals(valueRetrieved.get(k).getType())  && isPoweredOn() == true){			
					numberReads += valueRetrieved.get(k).getValue().longValue();										
				}			
			
				k++;
			}
			
			// update networkUsage 
			if (packetsTransmited != 0  && isPoweredOn() == true) {
				operation = new ComOperation(ComOperation.TYPE_UPDATE,
						"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName
								+ "']/actualNetworkUsage",
						String.valueOf(packetsTransmited));
				operationSet.add(operation);
			}
			
			// update diskUsage
			if (numberReads != 0  && isPoweredOn() == true) {
				// IN Optimizer Diskiorate is in TODO state
				// I put a 0 until optimizer is ready to read the value
				operation = new ComOperation(ComOperation.TYPE_UPDATE,
				"/nativeHypervisor/virtualMachine[frameworkID='"
				+ hostVirtualMachineName + "']/actualDiskIORate",
				String.valueOf(numberReads));
				operationSet.add(operation);
			}
			
			// update storageUsage
			double storageUsage = -1;
			storageUsage = hostsInformation_.getVirtualMachineStorageUsage(j,
					ComEniConstants.STORAGE_USAGE);
			// log.debug("Storage Usage: " + storageUsage);
			if (storageUsage != -1  && isPoweredOn() == true) {
				operation = new ComOperation(ComOperation.TYPE_UPDATE,
						"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName
								+ "']/actualStorageUsage", String.valueOf(storageUsage));
				operationSet.add(operation);
			}
				
		}		
	} // run
	
	/**
	 * dispose method performs a clean stop of CommunicatorEni.                              
	 */
	@Override
	public void finalize() {		
		try {
			super.finalize();
		} catch (Throwable e) {
			log.error("Exception",e);
		}
	}
	

}

