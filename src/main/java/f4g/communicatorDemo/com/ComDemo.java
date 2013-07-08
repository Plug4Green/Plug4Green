/**
* ============================== Header ============================== 
* file:          ComDemo.java
* project:       FIT4Green/CommunicatorDemo
* created:       28/06/2012 by jclegea
* 
* $LastChangedDate: 2012-06-29 14:23:43 +0200 (vie, 29 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1509 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorDemo.com;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.StandByActionType;
import f4g.schemas.java.actions.StartJobActionType;
import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.FrameworkStatusType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VirtualMachineType;

/**
 * Communicator demo to show how a communicator works
 * This is a simplified version of a communicator without proxy between 
 * COM and the DC hypervisor (in this case is XML file) 
 * @author jclegea
 */
public class ComDemo  extends AbstractCom implements Runnable {

	static Logger log = Logger.getLogger(ComDemo.class.getName());
	
	private String comDemoStatus_ = null;
	RetrieveInformation datacenterInformation_ = null;
	
	
	/**
	 * initialize ComDemo to aware the monitor of the existence of ComDemo
	 *  
	 * @param conName name of the com
	 * @param monitor monitor of fit4green
	 */
	@Override	
	public boolean init(String comName, IMonitor monitor) {		
		
		comDemoStatus_ = "STOPPED";
		
		super.init(comName, monitor);
		return true;
	}
	
	
	/* 
	 * In the run method is located the monitoring loop of the Com, retrieving information 
	 * from data centres and updating meta model XML.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		 
		// Start ComDemo
		log.debug("Start:" + this.comName);
		
		// initialize retrieve information
		datacenterInformation_ = new RetrieveInformation();		
		datacenterInformation_.init();
		
		// temporal located here
		startRetrievingInformation();
		
		// while the state of fit4green plugin is not stopped retrieve data from DC
		// and update the meta model XML
		while (state != STATE_STOPPED) {
			//log.debug("state not stopped");
			try {
				// connect with the datacenter, in case of disconnection reconnect 
				if(datacenterInformation_.connect()){
					// set the status of comDemo
					if("Stopped".equals(comDemoStatus_) && !"Starting".equals(comDemoStatus_)){
						// update the status in the model to starting
						setStatus(FrameworkStatusType.STARTING);
					}
					else if(!"Running".equals(comDemoStatus_)){
						// update the status in the model to running
						setStatus(FrameworkStatusType.RUNNING);
					}
				}
				
				// start retrieving datacenter information
				//startRetrievingInformation();
				insertInformationDatamodel();
				
				
				Thread.sleep(10000);				
			} catch (InterruptedException e) {
				log.error("End of ComEni Loop",e);
				//e.printStackTrace();
			}
		}
		
		// stop all the threads and free all the connections
		datacenterInformation_.disconnect();
	}
	
	
	/**
	 * 
	 * set the Framework status to be used by f4gGui
	 * 
	 * @param status
	 *
	 * @author jclegea
	 */
	private void setStatus(FrameworkStatusType status){				
		comDemoStatus_ = status.value();
		log.debug("STATUS " + this.comName + ": " + comDemoStatus_);
		monitor.setFrameworkStatus(this.comName, status);		
	}
	
	
	/**
	 * 
	 * realize the actions needed to start retrieving information from the datacenter 
	 * 
	 * @param datacenterInformation
	 * @return
	 *
	 * @author jclegea
	 */
	private boolean startRetrievingInformation(){
		datacenterInformation_.startRetrieveInformation();
		return true;
	}
	
	/**
	 * 
	 * get the information from the datacenter and inserts them into FIT4Green metamodel 
	 * 
	 *
	 * @author jclegea
	 */
	private void insertInformationDatamodel(){
		ComOperation operation;
		ComOperationCollector operations = new ComOperationCollector();
		ComOperationCollector operationSet = null;
		ArrayList actualHostList = new ArrayList();	
		String key = null;
		String hostVirtualMachineName = null;
		String hostName = null;
		boolean isInserted = false;
		
		try{
			
		
			// See if there are new virtual machines with respect to the metamodel
			// iterate all the sites, datacenters, racks availables to find the servers
			for(int i=0;i<datacenterInformation_.getSiteListSize();i++){
				for(int j=0;j<datacenterInformation_.getDatacenterListSize(i);j++){
					for(int k=0;k<datacenterInformation_.getRackListSize(i, j);k++){
						for(int l=0;l<datacenterInformation_.getEnclosureListSize(i, j, k);l++){
							for(int m=0;m<datacenterInformation_.getHostListSize(i, j, k, l);m++){
								hostName = datacenterInformation_.getHostName(i, j, k, l, m);
								
								log.debug("host: " + hostName + " virtual machines: " + datacenterInformation_.getVmListSize(i, j, k, l, m));
								
								key = this.comName + "_" + hostName;
								log.debug("queueHashmap: " + this.getQueuesHashMap().size());
								
								// Test if host is in the model
								if(((ConcurrentLinkedQueue<ComOperationCollector>)this.getQueuesHashMap().get(key)) != null){
										createHostVirtualMachineList(actualHostList,i,j,k,l,m);
										//datacenterInformation_.getHostVirtualMachines(i);
										operationSet = new ComOperationCollector();																			
										log.debug("actualHostList size: " + actualHostList.size());
										
										log.debug("virtual machine list: " + 
															datacenterInformation_.getVmListSize(i, j, k, l, m));
										// ADD virtual machines for host in fit4green model
										for(int n=0;n<datacenterInformation_.getVmListSize(i, j, k, l, m);n++){
											hostVirtualMachineName = datacenterInformation_.getVmName(i,j,k,l,m,n);
											if(actualHostList.contains(hostVirtualMachineName) == false && hostVirtualMachineName!=null ){
												// ADD a virtual machine to the model
												operation = new ComOperation(ComOperation.TYPE_ADD, 
														"./nativeHypervisor/virtualMachine/frameworkID", 
														hostVirtualMachineName + " a a " + 
														datacenterInformation_.getVmNumCpus(i, j, k, l, m, n) + " " +
														hostVirtualMachineName);
												operations.add(operation);
												((ConcurrentLinkedQueue<ComOperationCollector>)this.getQueuesHashMap().get(key)).add(operations);						
												monitor.updateNode(key, this);
												operations.remove(operation);						
												((ConcurrentLinkedQueue<ComOperationCollector>)this.getQueuesHashMap().get(key)).poll();					
											}
											else{
												// Remove Virtual machine from actualHostList
												actualHostList.remove(hostVirtualMachineName);
											}
										}
										
										// DELETE virtual machines for host in fit4green model
										for(int n=0;n<actualHostList.size();n++){
											log.debug("DELETING Virtual machine " + (String)actualHostList.get(j));
											operation = new ComOperation(ComOperation.TYPE_REMOVE,
													"./nativeHypervisor/virtualMachine/frameworkID",
													(String)actualHostList.get(j) + " a a "
															+ datacenterInformation_.getVmNumCpus(i, j, k, l, m, n));
											operations.add(operation);
											((ConcurrentLinkedQueue<ComOperationCollector>) this.getQueuesHashMap().get(key)).add(operations);
											monitor.updateNode(key, this);
											operations.remove(operation);
											((ConcurrentLinkedQueue<ComOperationCollector>) this.getQueuesHashMap().get(key)).poll();
										}
										

										// Update values for virtual machines in a host
										for(int n=0;n<datacenterInformation_.getVmListSize(i, j, k, l, m);n++){
											updateVMDynamicValues(i, j, k, l, m, n, operationSet);
										}
										
										// Update values for the host
										updateHostDynamicValues(i, j, k, l, m, operationSet, hostName);
										
										log.debug("operation list size: " + operationSet.getOperations().size());
										if(operationSet != null){
											isInserted = monitor.simpleUpdateNode(key,operationSet);
										}		
								}
								
							} // m
						} // l
					} // k
				} // j
			} // i
		
			monitor.logModel();
		}catch(Exception exception){
			log.error("Exception inserting data on to the fit4green model",exception);
		}	
		
	}
	
	/**
	 * 
	 * This function updates the dynamic values for VMs from tha DataCenter
	 * 
	 * @param siteIndex
	 * @param datacenterIndex
	 * @param rackIndex
	 * @param enclosureIndex
	 * @param hostIndex
	 * @param vmIndex
	 * @param operationSet
	 *
	 * @author jclegea
	 */
	private void updateVMDynamicValues(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex, ComOperationCollector operationSet){
		ComOperation operation;
		double cpuUsage;
		double diskUsage;
		double memoryUsage;
		double storageUsage;
		String hostVirtualMachineName=null;
		
		
		// get all virtual machines dynamic values from the datacenter
		// and add an update operation for each value
		
		// get the name of the virtual machine
		hostVirtualMachineName = datacenterInformation_.getVmName(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, vmIndex);
		
		// get the CPU usage
		try{
			cpuUsage = datacenterInformation_.getActualVMCPUUsage(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, vmIndex);
			operation = new ComOperation(
					ComOperation.TYPE_UPDATE,
					"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName + "']/actualCPUUsage",
					String.valueOf(cpuUsage));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve CPU information from the DC");
		}
		
		// get the Disk usage
		try{
			diskUsage = datacenterInformation_.getActualVMDiskIORate(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, vmIndex);
			operation = new ComOperation(ComOperation.TYPE_UPDATE,
					"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName + "']/actualDiskIORate",
					String.valueOf(diskUsage));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve Disk information from the DC");
		}
		
		// get the Memory usage
		try{
			memoryUsage = datacenterInformation_.getActualVMMemoryUsage(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, vmIndex);				
			operation = new ComOperation(ComOperation.TYPE_UPDATE,
					"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName
					+ "']/actualMemoryUsage", String.valueOf(memoryUsage));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve Memory information from the DC");
		}
				
		// get the storage usage
		try{
			storageUsage = datacenterInformation_.getActualVMStorageUsage(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, vmIndex);
			operation = new ComOperation(ComOperation.TYPE_UPDATE,
					"/nativeHypervisor/virtualMachine[frameworkID='" + hostVirtualMachineName
							+ "']/actualStorageUsage", String.valueOf(storageUsage));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve storage information from the DC");
		}
		
	}
	
	/**
	 * 
	 * Update the dynamic values from a host
	 * 
	 * @param siteIndex
	 * @param datacenterIndex
	 * @param rackIndex
	 * @param enclosureIndex
	 * @param hostIndex
	 * @param operationSet
	 * @param hostName
	 *
	 * @author jclegea
	 */
	private void updateHostDynamicValues(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, ComOperationCollector operationSet, String hostName){
		ComOperation operation;
		double measuredPower;		
		double memoryUsage;
		double HDReadRate;
		double HDWriteRate;
		double cpuUsage;
		double coreLoad;
		double totalPSate;
		double lastPstate;
		int HDListSize;
		int CPUListSize;
		int coresListSize;		
		String powerState = null;		
		
		// get all virtual machines dynamic values from the datacenter
		// and add an update operation for each value
		
		// get the power status
		try{
			powerState = datacenterInformation_.getPowerState(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex);
			if("ON".equals(powerState) == true){					
				operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.ON);
				operationSet.add(operation);
			} else{
				operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.OFF);
				operationSet.add(operation);
			}
			
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve CPU information from the DC");
		}
		
		// get the measured power for the host
		try{
			measuredPower = datacenterInformation_.getMeasuredPower(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex);
			operation = new ComOperation(ComOperation.TYPE_UPDATE, 
					"./measuredPower",String.valueOf(measuredPower));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve CPU information from the DC");
		}
		
		// get the memory usage
		try{
			memoryUsage = datacenterInformation_.getHostMemoryUsage(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex);
			operation = new ComOperation(ComOperation.TYPE_UPDATE,
					".[frameworkID='" +	hostName + "']/mainboard/memoryUsage", 
					String.valueOf(memoryUsage));
			operationSet.add(operation);
		}catch (NullPointerException exception){
			// in case that is impossible to get the information, whatever the reason 
			// simply don't update the model
			log.debug("Cannot retrieve CPU information from the DC");
		}
				
		// get the hard diks usages
		
		HDListSize = datacenterInformation_.getHDListSize(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex);		
		for(int HDIndex=0;HDIndex< HDListSize;HDIndex++){
			try{
				// Read Rate for HD
				HDReadRate = datacenterInformation_.getHDReadRate(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, HDIndex);
				operation = new ComOperation(ComOperation.TYPE_UPDATE,
						"/mainboard/hardwareRAID/hardDisk[frameworkID='" +
						datacenterInformation_.getHDFramework(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, HDIndex) + "']/readRate", 
						String.valueOf(HDReadRate));
				operationSet.add(operation);
			}catch (NullPointerException exception){
				// in case that is impossible to get the information, whatever the reason 
				// simply don't update the model
				log.debug("Cannot retrieve read rate information from the HD");
			}
				
			try{
				// Read Rate for HD
				HDWriteRate = datacenterInformation_.getHDWriteRate(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, HDIndex);
				operation = new ComOperation(ComOperation.TYPE_UPDATE,
						"/mainboard/hardwareRAID/hardDisk[frameworkID='" +
						datacenterInformation_.getHDFramework(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, HDIndex) + "']/writeRate", 
						String.valueOf(HDWriteRate));
				operationSet.add(operation);
			}catch (NullPointerException exception){
				// in case that is impossible to get the information, whatever the reason 
				// simply don't update the model
				log.debug("Cannot retrieve write rate information from the HD");
			}				
		}
		
		// get the CPU usages
		CPUListSize = datacenterInformation_.getHostCPUListSize(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex);		
		for(int CPUIndex=0;CPUIndex< HDListSize;CPUIndex++){
			try{
				// CPU Load
				cpuUsage = datacenterInformation_.getHostCPUUsage(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, CPUIndex);
				operation = new ComOperation(ComOperation.TYPE_UPDATE, 
						"/mainboard/CPU[frameworkID='" + hostName + "']/cpuUsage",
						String.valueOf(cpuUsage));
				operationSet.add(operation);					
			}catch (NullPointerException exception){
				// in case that is impossible to get the information, whatever the reason 
				// simply don't update the model
				log.debug("Cannot retrieve read rate information from the HD");
			}
			
			try{
				// Core Load
				coresListSize = datacenterInformation_.getHostCoreListSize(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, CPUIndex);				
				for(int COREIndex=0;COREIndex<coresListSize;COREIndex++){
					coreLoad = datacenterInformation_.getHostCoreLoad(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, CPUIndex, COREIndex);
					operation = new ComOperation(ComOperation.TYPE_UPDATE,
							"/mainboard/CPU/core[frameworkID='"
									+ hostName + "_CORE-" + COREIndex + "']/coreLoad",
							String.valueOf(coreLoad));
					operationSet.add(operation);
					
					lastPstate = datacenterInformation_.getHostCoreLastPSate(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, CPUIndex, COREIndex);
					operation = new ComOperation(ComOperation.TYPE_UPDATE,
							"/mainboard/CPU/core[frameworkID='" +
							hostName + "_CORE-" + COREIndex + "']/lastPstate", 
							lastPstate);
					operationSet.add(operation);
					
					totalPSate = datacenterInformation_.getHostCoreTotalPSates(siteIndex, datacenterIndex, rackIndex, enclosureIndex, hostIndex, CPUIndex, COREIndex);
					operation = new ComOperation(ComOperation.TYPE_UPDATE,
							"/mainboard/CPU/core[frameworkID='" +
							hostName + "_CORE-" + COREIndex + "']/totalPstates", 
							lastPstate);
					operationSet.add(operation);
				}
									
			}catch (NullPointerException exception){
				// in case that is impossible to get the information, whatever the reason 
				// simply don't update the model
				log.debug("Cannot retrieve read rate information from the HD");
			}
				
		}
			
			
		
		
		
	}
	
	/**
	 * 
	 * Creates a list of virtual machines.
	 * 
	 * @param actualHostList to store the hosts list.
	 *
	 * @author jclegea
	 */
	private void createHostVirtualMachineList(ArrayList actualHostList, int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex, int hostIndex) 
	throws Exception{
		String key;		
		HashMap serverList;
		ServerType serverType = new ServerType();
		VirtualMachineType serverVirtualMachine;
		List<VirtualMachineType> virtualMachineList;
		
		try{
			actualHostList.clear();		
			key = this.comName + "_" + datacenterInformation_.getHostName(siteIndex,datacenterIndex, rackIndex, enclosureIndex,hostIndex);
			serverList = monitor.getMonitoredObjectsCopy(this.comName);		
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
	 * Execute a list of actions on behalf of the Controller
	 * 
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */	
	@Override
	public boolean executeActionList(ArrayList actionList) {
		
		PowerOnActionType powerOnAction;
		PowerOffActionType powerOffAction;
		LiveMigrateVMActionType migrateAction;
		MoveVMActionType moveAction;
		int i=0;

		String key = null;
		ComOperation operation;
		ComOperationCollector operationSet = null;
		
		// initialize operationSet
		operationSet = new ComOperationCollector();
		
		// First 
		log.debug(this.comName + ": executing action list...");
		JAXBElement<? extends AbstractBaseActionType> elem;
		Iterator iter = actionList.iterator();
		while (iter.hasNext()) {
			elem = (JAXBElement<? extends AbstractBaseActionType>) iter.next();

			Object action = elem.getValue();
			action = elem.getValue().getClass().cast(action);
			try {
				if (action.getClass().equals(PowerOffActionType.class)) {					
					// perform power off action										
					powerOffAction = (PowerOffActionType) action;
					
					// set the status of powering on to the servers
					key = comName + "_" + powerOffAction.getNodeName();
					operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.POWERING_OFF);
					operationSet.add(operation);
					if(operationSet != null){
						monitor.simpleUpdateNode(key,operationSet);				
					}
					
					this.powerOff(powerOffAction);
				} else if (action.getClass().equals(PowerOnActionType.class)) {
					// perform power on action
					
					powerOnAction = (PowerOnActionType) action;
					
					// set the status of powering on to the servers
					key = comName + "_" + powerOnAction.getNodeName();
					operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.POWERING_ON);
					operationSet.add(operation);
					if(operationSet != null){
						monitor.simpleUpdateNode(key,operationSet);				
					}
					// call the method to power on
					this.powerOn(powerOnAction);
				} else if (action.getClass().equals(LiveMigrateVMActionType.class)) {
					// perform migrate vm action
					migrateAction = (LiveMigrateVMActionType) action;
					this.liveMigrate(migrateAction);
				} else if (action.getClass().equals(MoveVMActionType.class)) {
					// perform move vm action
					moveAction = (MoveVMActionType) action;
					this.moveVm(moveAction);
				}
			} catch (SecurityException e) {
				log.error("Exception",e);
			} catch (IllegalArgumentException e) {
				log.error("Exception",e);
			}
		}		
			
		return true;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#powerOn(org.f4g.schema.actions.PowerOnActionType)
	 */	
	@Override
	public boolean powerOn(PowerOnActionType action) {
		// Here include the code to power on a host
		log.debug("POWERING ON: " + action.getNodeName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#powerOff(org.f4g.schema.actions.PowerOffActionType)
	 */
	@Override
	public boolean powerOff(PowerOffActionType action) {
		// Here include the code to power off a host
		log.debug("POWERING OFF: " + action.getNodeName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#liveMigrate(org.f4g.schema.actions.LiveMigrateVMActionType)
	 */
	@Override
	public boolean liveMigrate(LiveMigrateVMActionType action) {
		// Here include the code to live migrate a virtual machine
		log.debug("Live migrate: " + action.getFrameworkName() + " from " + action.getSourceNodeController() + " to " + action.getDestNodeController());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#moveVm(org.f4g.schema.actions.MoveVMActionType)
	 */
	@Override
	public boolean moveVm(MoveVMActionType action) {
		// Here include the code to move a virtual machine
		log.debug("Move: " + action.getFrameworkName() + " from " + action.getSourceNodeController() + " to " + action.getDestNodeController());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#startJob(org.f4g.schema.actions.StartJobActionType)
	 */
	@Override
	public boolean startJob(StartJobActionType action) {
		log.error("Operation not supported on this Com component");
		return false;
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.IComOperationSet#standBy(org.f4g.schema.actions.StandByActionType)
	 */
	@Override
	public boolean standBy(StandByActionType action) {
		log.error("Operation not supported on this Com component");
		return false;
	}		
		
	/**
	 * dispose method performs a clean stop of CommunicatorDemo.                              
	 */	
	@Override
	public boolean dispose() {
		// free the connections with the data centre  
		// and stop all the possible threads running 
		// then stop the plug-in itself
		
		log.debug("ComDemo Stop");
		super.dispose();
		
		return true;
	}

}
