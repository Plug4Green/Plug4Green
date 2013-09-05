/**
* ============================== Header ============================== 
* file:          ComEniThread.java
* project:       FIT4Green/CommunicatorEni
* created:       08/09/2011 by jclegea
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;
import f4g.schemas.java.metamodel.FrameworkStatusType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VirtualMachineType;


import f4g.communicatorEni.vmware.PerformanceInformation;
import f4g.communicatorEni.vmware.RetrieveHostInformation;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public class ComEniThread extends Thread{

	static Logger log = Logger.getLogger(ComEniThread.class.getName());
	private boolean running = true;
	private RetrieveHostInformation hostsInformation_;
	private static Properties configurationEni_ = null;	
	private boolean isConnected_ = false;
	IMonitor monitor_ = null;
	ComEni comEni_ = null;
	String[] actionArgs_;
	String retrieveAction_;
	String dataCenterName_;
	private String comEniStatus_ = null;	
	
	
	public ComEniThread(String dataCenterName,String[] actionArgs,String retrieveAction,RetrieveHostInformation hostsInformation, IMonitor monitor, ComEni comEni){
		actionArgs_ = actionArgs;
		retrieveAction_ = retrieveAction;
		comEni_ = comEni;
		monitor_ = monitor;
		
		// initialize comEni status
		dataCenterName_ = dataCenterName;
		comEniStatus_ = ComEniConstants.STOPPED;
		
		try {
			configurationEni_ = new Properties();
			InputStream configInputStream = this.getClass().getClassLoader().getResourceAsStream(ComEniConstants.CONFIGURATION_ENI);			
		
			configurationEni_.load(configInputStream);
		} catch (IOException exception) {
			log.error("Exception",exception);
		}			
		
	}
	
	/**
	 * 
	 * set the thread to running or to 
	 * 
	 * @param state
	 *
	 * @author jclegea
	 */
	public void setState(boolean state)
	{
		running = state;
	}

	/**
	 * 
	 * set the Framework status to be used by f4gGui
	 * 
	 * @param status
	 *
	 * @author jclegea
	 */
	public void setStatus(FrameworkStatusType status){				
		comEniStatus_ = status.value();
		log.debug("STATUS " + comEni_.comName + ": " + comEniStatus_);
		monitor_.setFrameworkStatus(comEni_.comName, status);		
	}
	
	/**
	 * 
	 * get if comEni is connected to vmware services
	 * 
	 * @return
	 *
	 * @author jclegea
	 */
	public boolean isConnected(){
		return isConnected_;
	}
	
	
	public void run(){
		
		boolean isRetrieved = false;
		int monitorInterval =  Integer.valueOf(configurationEni_.getProperty(ComEniConstants.INTERVAL));
		int retryCount = 0;
		int maxRetries = Integer.valueOf(configurationEni_.getProperty(ComEniConstants.RETRYCOUNT));
		
		hostsInformation_ = new RetrieveHostInformation(actionArgs_, retrieveAction_);
//		log.debug("hostinformation: " + hostsInformation_.toString());
		
		while(running){
			log.debug("Executing ComEniThread");			
			try {
				//log.debug("usr: " + )				
				log.debug("isConnected: " + isConnected_);
				isConnected_ = hostsInformation_.connect();
				if(isConnected_ == true){
					// if it is the first running put state to Starting					
					if(ComEniConstants.STOPPED.equals(comEniStatus_) == true && ComEniConstants.STARTING.equals(comEniStatus_) != true){						
						// update the status in the model
						setStatus(FrameworkStatusType.STARTING);												
					}
					else if(ComEniConstants.RUNNING.equals(comEniStatus_) != true){
						// state of comEni Running
						setStatus(FrameworkStatusType.RUNNING);
					}
					
					isRetrieved = retrieveInformation();
					if(isRetrieved){						 
						insertInformationDatamodel();						
					}
				
					Thread.sleep(monitorInterval);					
				}
				else{					
						log.debug("connection with vSphere Web Service not possible.");
						retryCount++;
						if(retryCount >= maxRetries && ComEniConstants.STOPPED.equals(comEniStatus_) != true){
							setStatus(FrameworkStatusType.STOPPED);						
						}
						Thread.sleep(monitorInterval);
				}
				
				if(isConnected_ == true){
					hostsInformation_.disconnect();
				}
				
			} catch (InterruptedException exception) {
				log.debug("Forced stop of " + comEni_.comName);
				log.error("Exception",exception);
			}
		}
	}

	
	/**
	 * 
	 * Retrieve hosts information from ENI's Servers
	 * 
	 * @return true if information is retrieved successfully
	 *
	 * @author jclegea
	 */
	private boolean retrieveInformation() {		
		boolean isRetrieved = false;
		
		try{
				isRetrieved = hostsInformation_.startRetrieveInformation();				
		}catch(Exception exception){
			log.error("Exception Retrieving information");			
		}
		
		return isRetrieved;
	}	
	
	
	

	
	/**
	 * 
	 * Creates a list of virtual machines.
	 * 
	 * @param actualHostList to store the hosts list.
	 *
	 * @author jclegea
	 */
	private void createHostVirtualMachineList(ArrayList actualHostList, int hostIndex) 
	throws Exception{
		String key;		
		HashMap serverList;
		ServerType serverType = new ServerType();
		VirtualMachineType serverVirtualMachine;
		List<VirtualMachineType> virtualMachineList;
		
		try{
			actualHostList.clear();		
			key = comEni_.comName + "_" + hostsInformation_.getHostName(hostIndex);
			serverList = monitor_.getMonitoredObjectsCopy(comEni_.comName);		
			serverType = (ServerType)serverList.get(key);
			if(serverType != null){
				virtualMachineList = serverType.getNativeHypervisor().getVirtualMachine();
				for (Iterator iterator = virtualMachineList.iterator(); iterator
					.hasNext();) {
					serverVirtualMachine = (VirtualMachineType) iterator.next();
					//log.debug("SERVERTYPE: " + serverVirtualMachine.getFrameworkID());														
					actualHostList.add(serverVirtualMachine.getFrameworkID());					
				}
			}
		
		}catch(Exception exception){
			log.error("Cannot create actual virtual machine list");			
		}		
	}

	/**
	 * 
	 * Retrieve information from hosts like virtual machines and
	 * insert into fit4gree model.
	 * 
	 * @return true if information is inserted, false otherwise.
	 *
	 * @author jclegea
	 */
	private boolean insertInformationDatamodel(){
		ComOperation operation;
		ComOperationCollector operations = new ComOperationCollector();
		ComOperationCollector operationSet = null;
		ArrayList actualHostList = new ArrayList();
		String key = null;
		String hostVirtualMachineName = null;
		boolean isInserted = false;
		ArrayList<PerformanceInformation> valueRetrieved = new ArrayList<PerformanceInformation>();
		ArrayList <String> type = new ArrayList<String>();
		ArrayList <Integer> counters = new ArrayList<Integer>();			
		ArrayList <int[]> pStates = new ArrayList<int[]>();		
		String powerState="";
		double measuredPower = 0.0;
		double cpuLoadHistory = -1;
		double cpuUsage = -1;
		ServerStatusType statusPower;
		String ip = null;
		int i=0, j=0 , k=0;
		log.debug("insertInformationDataModel");
		try{
			for(i=0;i<hostsInformation_.getHostListSize();i++){				
				key = comEni_.comName + "_" + hostsInformation_.getHostName(i);
				// Test if host is in the model
				if(((ConcurrentLinkedQueue<ComOperationCollector>)comEni_.getQueuesHashMap().get(key)) != null){
						createHostVirtualMachineList(actualHostList,i);
						hostsInformation_.getHostVirtualMachines(i);
						operationSet = new ComOperationCollector();
																
						
						log.debug("HostVirtualMachineSize: " + 
											hostsInformation_.getHostVirtualMachineSize());
						// ADD virtual machines for host in fit4green model
						for(j=0;j<hostsInformation_.getHostVirtualMachineSize();j++){
							hostVirtualMachineName = hostsInformation_.getHostVirtualMachineName(j);
							if(actualHostList.contains(hostVirtualMachineName) == false && hostVirtualMachineName!=null ){
								// ADD a virtual machine to the model
								operation = new ComOperation(ComOperation.TYPE_ADD, 
										"./nativeHypervisor/virtualMachine/frameworkID", 
										hostVirtualMachineName + " a a " + 
										hostsInformation_.getHostVirtualMachineNumCpus(j) + " " +
										hostVirtualMachineName);
								operations.add(operation);
								((ConcurrentLinkedQueue<ComOperationCollector>)comEni_.getQueuesHashMap().get(key)).add(operations);						
								monitor_.updateNode(key, comEni_);
								operations.remove(operation);						
								((ConcurrentLinkedQueue<ComOperationCollector>)comEni_.getQueuesHashMap().get(key)).poll();					
							}
							else{
								// Remove Virtual machine from actualHostList
								actualHostList.remove(hostVirtualMachineName);
							}
						}
						
						// DELETE virtual machines for host in fit4green model
						for(j=0;j<actualHostList.size();j++){
							log.debug("DELETING Virtual machine " + (String)actualHostList.get(j));
							operation = new ComOperation(ComOperation.TYPE_REMOVE,
									"./nativeHypervisor/virtualMachine/frameworkID",
									(String)actualHostList.get(j) + " a a "
											+ hostsInformation_.getHostVirtualMachineNumCpus(j));
							operations.add(operation);
							((ConcurrentLinkedQueue<ComOperationCollector>) comEni_.getQueuesHashMap().get(key)).add(operations);
							monitor_.updateNode(key, comEni_);
							operations.remove(operation);
							((ConcurrentLinkedQueue<ComOperationCollector>) comEni_.getQueuesHashMap().get(key)).poll();
						}
						
						// Update values for virtual machines in a host						
						UpdateThread[] ut = new UpdateThread[hostsInformation_.getHostVirtualMachineSize()];
						for(j=0;j<hostsInformation_.getHostVirtualMachineSize();j++){
							type.clear();
							counters.clear();							
							ut[j] = new UpdateThread(j, hostsInformation_, type, counters, valueRetrieved, operationSet, Integer.valueOf(configurationEni_.getProperty(ComEniConstants.HOST_PORT)));
							ut[j].start();
						}
						
						// Syncronize Threads
						for(j=0;j<hostsInformation_.getHostVirtualMachineSize();j++){
							ut[j].join();
							//log.debug("Virtual machine: " + hostsInformation_.getHostVirtualMachineName(j) + " powered on: " + ut[j].isPoweredOn());
							if(ut[j].isPoweredOn() == false){
								operation = new ComOperation(ComOperation.TYPE_REMOVE,
										"./nativeHypervisor/virtualMachine/frameworkID",
										hostsInformation_.getHostVirtualMachineName(j) + " a a "
												+ hostsInformation_.getHostVirtualMachineNumCpus(j));
								operations.add(operation);
								((ConcurrentLinkedQueue<ComOperationCollector>) comEni_.getQueuesHashMap().get(key)).add(operations);
								monitor_.updateNode(key, comEni_);
								operations.remove(operation);
								((ConcurrentLinkedQueue<ComOperationCollector>) comEni_.getQueuesHashMap().get(key)).poll();
							}
						}
						
						
						// Update power status for hosts						
						powerState = hostsInformation_.getHostPowerState(i);
						
						if("poweredOn".equals(powerState) == true){					
							operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.ON);
							operationSet.add(operation);
							//measuredPower = hostsInformation_.getHostMeasuredPower(i);

//							// Obtain power throught ILO
//							ip = configurationEni_.getProperty(hostsInformation_.getHostName(i));
//							hostsInformation_.setIp(ip);
//							hostsInformation_.setPort(port_);														
//							hostsInformation_.setUserName(userilo_);
//							hostsInformation_.setPassword(passilo_);
//							try{
//								measuredPower = Double.valueOf(hostsInformation_.getHostMeasuredPowerILO(i));
//							}catch(Exception exception){
//								log.error("Exception retrieving information through ILO");
//								exception.printStackTrace();
//							}							
//							log.debug("!!MEASURED POWER: " + measuredPower);
						} else{
							operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.OFF);
							operationSet.add(operation);
						}
						//log.debug("powerState: " + powerState);
						
						// Retrieving P-States from ILO
						try{
							ip = configurationEni_.getProperty(hostsInformation_.getHostName(i) + "_ILO");						
							hostsInformation_.setIp(ip);
							hostsInformation_.setPort(configurationEni_.getProperty(ComEniConstants.HOST_PORT));
							//log.debug("port: " + configurationEni_.getProperty(ComEniConstants.HOST_PORT));
							hostsInformation_.setUserName(DesencryptString.desencryptString(configurationEni_.getProperty("user_" + hostsInformation_.getHostName(i) + "_ILO")));
							//log.debug("user: " + DesencryptString.desencryptString(configurationEni_.getProperty("user_" + hostsInformation_.getHostName(i) + "_ILO")));
							hostsInformation_.setPassword(DesencryptString.desencryptString(configurationEni_.getProperty("pass_" + hostsInformation_.getHostName(i) + "_ILO")));
							//log.debug("pass: " + DesencryptString.desencryptString(configurationEni_.getProperty("pass_" + hostsInformation_.getHostName(i) + "_ILO")));
						
						
							// commented to speed up the testing phase
							hostsInformation_.getPStatesILOExec(pStates);
							
							int []states = new int[2];
							for(k=0;k<pStates.size();k=k+2){
								states = pStates.get(k);								
								// update p-states in the model
								operation = new ComOperation(ComOperation.TYPE_UPDATE,
										"/Mainboard/CPU/Core[frameworkID='" +
										hostsInformation_.getHostName(i) + "_CORE-" + k/2 + "']/lastPstate", 
										String.valueOf(Integer.valueOf(states[0])));
								operationSet.add(operation);
								operation = new ComOperation(ComOperation.TYPE_UPDATE,
										"/Mainboard/CPU/Core[frameworkID='" +
										hostsInformation_.getHostName(i) + "_CORE-" + k/2 + "']/totalPstates", 
										String.valueOf(Integer.valueOf(states[1])));
								operationSet.add(operation);
							}							
						}catch(Exception exception){
							log.error("Exception retrieving information through ILO",exception);
						}
						
						
						
						type.clear();
						counters.clear();						
//						type.add("cpu");
//						counters.add(ComEniConstants.CPU_USAGE);
						type.add("power");
						counters.add(ComEniConstants.POWER_USAGE);
						type.add("mem");
						counters.add(ComEniConstants.TOTAL_MEM);
						type.add("disk");
						counters.add(ComEniConstants.DISK_READ);						
						valueRetrieved.clear();
						valueRetrieved = hostsInformation_.getPerformance(hostsInformation_.getHostName(i), "HostSystem", type, counters,20);						
						
						
//						log.debug("ValuesRetrieved: " + valueRetrieved.size());
						k=0;
						for(Iterator<PerformanceInformation> it = valueRetrieved.iterator();it.hasNext();){
//							log.debug("ValueRetrieved: " + it.next().getValue());
							it.next();							
//							log.debug("k:" + k + " type: " + valueRetrieved.get(k).getType() + " instance: " + valueRetrieved.get(k).getInstance() + " value: " + valueRetrieved.get(k).getValue() + " length: " + valueRetrieved.get(k).getInstance().length());														
							
							// CPUUsage and coreLoad
//							if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && valueRetrieved.get(k).getInstance().isEmpty() && "CPU".equals(valueRetrieved.get(k).getType()) ){								
//								operation = new ComOperation(ComOperation.TYPE_UPDATE, 
//													"//cpuUsage[../frameworkID='" + 
//													hostsInformation_.getHostName(i) + "']", 
//													String.valueOf(valueRetrieved.get(k).getValue() / 100));
//								operationSet.add(operation);
//							}else if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "CPU".equals(valueRetrieved.get(k).getType()) ){								
//								operation = new ComOperation(ComOperation.TYPE_UPDATE, 
//										"//coreLoad[../frameworkID='" + 
//										hostsInformation_.getHostName(i) + "_CORE-" + valueRetrieved.get(k).getInstance() + "']", 
//										String.valueOf(valueRetrieved.get(k).getValue() / 100));
//								operationSet.add(operation);
//							} 
							
							// measuredPower
							if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && valueRetrieved.get(k).getInstance().isEmpty() && "Power".equals(valueRetrieved.get(k).getType()) ){				
								operation = new ComOperation(ComOperation.TYPE_UPDATE, 
										"./measuredPower",String.valueOf(valueRetrieved.get(k).getValue()));
								operationSet.add(operation);
							}
							
							// MemoryUsage
							if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && valueRetrieved.get(k).getInstance().isEmpty() && "Memory".equals(valueRetrieved.get(k).getType()) ){
//								log.debug("Memory: " + valueRetrieved.get(k).getValue());
								operation = new ComOperation(ComOperation.TYPE_UPDATE,
										".[frameworkID='" +
										hostsInformation_.getHostName(i) + "']/mainboard/memoryUsage", 
										String.valueOf(valueRetrieved.get(k).getValue()/ComEniConstants.KYLOBYTES_TO_GIGABYTES));
								operationSet.add(operation);
							}
							
							// readRate
							if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "Disk".equals(valueRetrieved.get(k).getType())){
//								log.debug("Disk: " + valueRetrieved.get(k).getValue());
								operation = new ComOperation(ComOperation.TYPE_UPDATE,
										"/mainboard/hardwareRAID/hardDisk[frameworkID='" +
										valueRetrieved.get(k).getInstance() + "']/readRate", 
										String.valueOf(valueRetrieved.get(k).getValue()));
								operationSet.add(operation);
							}							

							k++;
						}
						
						// obtain writeRate
						type.clear();
						counters.clear();
						type.add("disk");
						counters.add(ComEniConstants.DISK_WRITE);						
						valueRetrieved.clear();
						valueRetrieved = hostsInformation_.getPerformance(hostsInformation_.getHostName(i), "HostSystem", type, counters,20);
						
						k=0;
//						log.debug("ValuesRetrieved: " + valueRetrieved.size());
						for(Iterator<PerformanceInformation> it = valueRetrieved.iterator();it.hasNext();){
							it.next();
							
							if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "Disk".equals(valueRetrieved.get(k).getType())){
//								log.debug("Disk: " + valueRetrieved.get(k).getValue());
								operation = new ComOperation(ComOperation.TYPE_UPDATE, 
										"/mainboard/hardwareRAID/hardDisk[frameworkID='" +
										valueRetrieved.get(k).getInstance() + "']/writeRate", 
										String.valueOf(valueRetrieved.get(k).getValue()));
								operationSet.add(operation);
							}
							
							k++;
						}
						
						// Get CPULoad average			
						ArrayList <String> typeCPULoad = new ArrayList<String>();
						ArrayList <Integer> countersCPULoad = new ArrayList<Integer>();			
						Calendar endTime = Calendar.getInstance();
						Calendar startTime = Calendar.getInstance();
						int cores = -1;
						double []coresHistory = null;
						int []coresHistoryOccurrences = null;
						int cpuLoadHistoryOccurrences = 0;						
						
						
						startTime.add(Calendar.MINUTE,-Integer.valueOf(configurationEni_.getProperty(ComEniConstants.HOST_PORT)));
						valueRetrieved.clear();
						typeCPULoad.add("cpu");			
						countersCPULoad.add(ComEniConstants.CPU_USAGE);
						valueRetrieved = hostsInformation_.getPerformanceHistory(hostsInformation_.getHostName(i), "HostSystem", typeCPULoad, countersCPULoad, startTime, endTime, 20);
						
						if(valueRetrieved.size() > 0){
							// Find the number of cores
							for(k=0;k<valueRetrieved.size();k++){
								if(!valueRetrieved.get(k).getInstance().isEmpty() && Integer.valueOf(valueRetrieved.get(k).getInstance()) > cores){
									cores = Integer.valueOf(valueRetrieved.get(k).getInstance());
								}
							}

							coresHistory = new double[cores + 1];
							coresHistoryOccurrences = new int[cores + 1];
							// Initialize coreHistory
							for(k=0;k<cores + 1;k++){
								coresHistory[k]=0;
								coresHistoryOccurrences[k]=0;
							}
							cpuLoadHistory = 0;							
							for(k=0;k<valueRetrieved.size();k++){								
								if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && valueRetrieved.get(k).getInstance().isEmpty() && "CPU".equals(valueRetrieved.get(k).getType()) ){					
									cpuLoadHistory += valueRetrieved.get(k).getValue();
									cpuLoadHistoryOccurrences++;
								}
								else if(valueRetrieved!= null && valueRetrieved.size()>0 && valueRetrieved.get(k).getValue() != -1 && "CPU".equals(valueRetrieved.get(k).getType()) ){							
									coresHistory[Integer.valueOf(valueRetrieved.get(k).getInstance())] += valueRetrieved.get(k).getValue();
									coresHistoryOccurrences[Integer.valueOf(valueRetrieved.get(k).getInstance())]++;
								}
							}
							
							// Update the model
							for (k = 0; k < cores + 1; k = k + 2) {
								if(coresHistoryOccurrences[k]>0){
									operation = new ComOperation(ComOperation.TYPE_UPDATE,
											"/mainboard/CPU/core[frameworkID='"
													+ hostsInformation_.getHostName(i) + "_CORE-"
													+ k/2 + "']/coreLoad",
											String.valueOf(((coresHistory[k/2] + coresHistory[k+1/2]) / (coresHistoryOccurrences[k/2] + coresHistoryOccurrences[k+1/2])) / 100));
									operationSet.add(operation);
								}
							}
							
							if(cpuLoadHistoryOccurrences>0){
								cpuLoadHistory = cpuLoadHistory / cpuLoadHistoryOccurrences;
							}
							cpuUsage = cpuLoadHistory / 100;
							operation = new ComOperation(ComOperation.TYPE_UPDATE, 
									"/mainboard/CPU[frameworkID='" + 
									hostsInformation_.getHostName(i) + "']/cpuUsage",
									String.valueOf(cpuUsage));
							operationSet.add(operation);
							
						}
						
						
						
						
						//log.debug("Queue Size After: " + 
						log.debug("operation list size: " + operationSet.getOperations().size());
						if(operationSet != null){
							isInserted = monitor_.simpleUpdateNode(key,operationSet);
						}						
						
				} // del if	
			}
			
			monitor_.logModel();
		}catch(Exception exception){
			log.error("Exception inserting data on to the fit4green model",exception);
		}		
		
		return isInserted;
	}	
	
	@Override
	public void finalize(){
		try {
			super.finalize();
		} catch (Throwable e) {
			log.error(e);
		}
	}
	
	// dispose
	public void dispose(){
		if(isConnected_ == true){
			hostsInformation_.disconnect();
		}
	}
}
