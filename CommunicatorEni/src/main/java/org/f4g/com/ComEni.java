/**
 * ============================== Header ============================== 
 * file:             ComEni.java
 * project:       FIT4Green/Manager
 * created:       18 nov. 2010 by jclegea
 * 
 * $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
 * $LastChangedBy: jclegea $
 * $LastChangedRevision: 1497 $
 * 
 * short description:
 *   {Connector to execute Actions on ENI System}
 * ============================= /Header ==============================
 */

package org.f4g.com;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.f4g.com.simulation.ComEniUpdateSimulationThread;
import org.f4g.com.util.ComOperation;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.actions.StartJobActionType;
import org.f4g.schema.actions.StandByActionType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.com.ComEniConstants;

import com.vmware.MigrateRelocateVirtualMachine;
import com.vmware.PowerOps;
import com.vmware.RetrieveHostInformation;


public class ComEni extends AbstractCom implements Runnable {

	static Logger log = Logger.getLogger(ComEni.class.getName()); 
	private MigrateRelocateVirtualMachine migrateRelocateVirtualMachine_;
	private PowerOps powerOps;	
	private String userName_;
	private String url_;
	private String password_;
	private String port_;
	private int numberDatacenters_;
	private RetrieveHostInformation[] hostsInformation_;
	private ComEniThread[] eniThread_;
	private static Properties configurationEni_ = null;
	private ProxyServerComEni proxyServerComEni_ = null;	

	
	public ComEni() {
	}
	

	/**
	 * init communicatorEni
	 *  
	 * @param conName name of the com
	 * @param monitor monitor of fit4green
	 */
	@Override	
	public boolean init(String comName, IMonitor monitor) {
		try {
			configurationEni_ = new Properties();
			InputStream configInputStream = this.getClass().getClassLoader().getResourceAsStream(ComEniConstants.CONFIGURATION_ENI);			
			configurationEni_.load(configInputStream);			
		
			port_ = configurationEni_.getProperty(ComEniConstants.HOST_PORT);
			numberDatacenters_ = Integer.valueOf(configurationEni_.getProperty(ComEniConstants.DATA_CENTER));
			proxyServerComEni_ = new ProxyServerComEni(monitor);
			
		} catch (IOException exception) {
			log.error("Exception",exception);
		}
		
		super.init(comName, monitor);
		return true;
	}

	
	/**
	 * In the run method, a set of mock threads are started, simulating a set of
	 * possible operations performed by a Com component
	 */
	@Override
	public void run() {		
		String[][] actionArguments = new String[numberDatacenters_][6];
		String dataCenterName = null;
		int actualDataCenter = -1;
		
		
		
		log.debug("!!!!!COMNAME:" + this.comName);
		//log.debug("number of datacenters: " + numberDatacenters_);
		for(int i=0;i<numberDatacenters_;i++){
			dataCenterName = configurationEni_.getProperty(ComEniConstants.DATA_CENTER + "_" + i);
			log.debug("dataCenterName: " + dataCenterName);
			if(this.comName.equals(dataCenterName) == true){
				actualDataCenter = i;				
			}
			//log.debug("username_" + dataCenterName + ": " + DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName)));
			try{
				actionArguments[i][0] = "--" + ComEniConstants.URL; 
				actionArguments[i][1] = configurationEni_.getProperty(ComEniConstants.URL + "_" + dataCenterName);
				actionArguments[i][2] = "--" + ComEniConstants.USER_NAME;
				actionArguments[i][3] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName));
				actionArguments[i][4] = "--" + ComEniConstants.PASSWORD;
				actionArguments[i][5] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.PASSWORD + "_" + dataCenterName));
			}catch(Exception exception){
				log.error("Error in configuration file, stop the plug-in and check user and password" , exception);
			}
		}
		
		

		hostsInformation_ = new RetrieveHostInformation[numberDatacenters_];
		
		if("true".equals(configurationEni_.get(ComEniConstants.IS_SIMULATED)) == true) {
			// Simulated ENI Activities
			ComEniUpdateSimulationThread updateThread = null;
			
			updateThread = new ComEniUpdateSimulationThread(this, 5000, monitor);					
		}
		else {
			eniThread_ = new ComEniThread[numberDatacenters_];

			// 1st method to manage 2 or more datacenters, 2 threads with same comName			
//			for(int i=0;i<numberDatacenters_;i++){
//				//log.debug("HERE: " + actionArguments[i][1] + " comname: " + this.comName);
//				//if(actionArguments[i][1].contains(this.comName)){
//					//log.debug("HERE!!!!!!!!!!!");
//					eniThread[i] = new ComEniThread(dataCenterName,actionArguments[i],"retrieveInformation"+i,hostsInformation_[i],monitor, this);
//					eniThread[i].start();
//				//}				
//			}
			
			// 2nd method to manage, 1 thread each comName, only 1 actionArguments.
			log.debug("actualDatacenter: " + actualDataCenter + " url: " + actionArguments[actualDataCenter][1]);
			eniThread_[0] = new ComEniThread(dataCenterName,actionArguments[actualDataCenter],"retrieveInformation1",hostsInformation_[actualDataCenter],monitor, this);			
			eniThread_[0].start();
						
			while (state != STATE_STOPPED) {
				//log.debug("state not stopped");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.error("End of ComEni Loop",e);
					//e.printStackTrace();
				}
			}

			for(int i=0;i<numberDatacenters_;i++){
				eniThread_[i].setState(false);
				eniThread_[i].dispose();
				
			}

			log.debug("End of Monitor Loop");		
		}
	}

	/**
	 * dispose method performs a clean stop of CommunicatorEni.                              
	 */
	@Override
	public boolean dispose() {
		// stop connection to ENI vCenter
		
		for(int i=0;i<numberDatacenters_;i++){
			log.debug("Disposing... Datacenter: " + i);			
			eniThread_[i].dispose();
		}
		proxyServerComEni_.stop();
		super.dispose();
		return true;
	}
	


	/**
	 * Execute a list of actions on behalf of the Controller
	 * 
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean executeActionList(ArrayList actionList) {
		ArrayList<PowerOnActionType> powerOnOperations = new ArrayList<PowerOnActionType>();
		ArrayList<PowerOffActionType> powerOffOperations = new ArrayList<PowerOffActionType>();
		ArrayList<MoveVMActionType> moveVmOperations = new ArrayList<MoveVMActionType>();
		ArrayList<LiveMigrateVMActionType> migrateVmOperations = new ArrayList<LiveMigrateVMActionType>();
		int i=0, j=0;
		String urlMoveVm = null;
		String userNameMoveVm = null;
		String passwordMoveVm = null;
		String dataCenterName = null;
		String key = null;
		ComOperation operation;
		ComOperationCollector operations = new ComOperationCollector();
		ComOperationCollector operationSet = null;
		
		// initialize operationSet
		operationSet = new ComOperationCollector();
		
		log.debug(this.comName + ": executing action list...");
		JAXBElement<? extends AbstractBaseActionType> elem;
		Iterator iter = actionList.iterator();
		while (iter.hasNext()) {
			elem = (JAXBElement<? extends AbstractBaseActionType>) iter.next();

			Object action = elem.getValue();
			action = elem.getValue().getClass().cast(action);
			try {
				if (action.getClass().equals(PowerOffActionType.class)) {
					powerOffOperations.add((PowerOffActionType) action);
				} else if (action.getClass().equals(PowerOnActionType.class)) {
					powerOnOperations.add((PowerOnActionType) action);
				} else if (action.getClass().equals(LiveMigrateVMActionType.class)) {					
					migrateVmOperations.add((LiveMigrateVMActionType) action);
				} else if (action.getClass().equals(MoveVMActionType.class)) {
					moveVmOperations.add((MoveVMActionType) action);
				}
			} catch (SecurityException e) {
				log.error("Exception",e);
			} catch (IllegalArgumentException e) {
				log.error("Exception",e);
			}
		}
		
		// Performing first power on operations 
		iter = powerOnOperations.iterator();
		PowerOnActionType powerOnAction;
		while (iter.hasNext()) {
			try{
				powerOnAction = (PowerOnActionType) iter.next();
				
				// set the status of powering on to the servers
				key = comName + "_" + powerOnAction.getNodeName();
				operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.POWERING_ON);
				operationSet.add(operation);
				if(operationSet != null){
					monitor.simpleUpdateNode(key,operationSet);				
				}
				
				this.powerOn(powerOnAction);
			
				

				
			// Update as the state in the model as fast as possible to avoid possible errors when Optimizing
//			key = comName + "_" + ((PowerOnActionType) iter.next()).getNodeName();
//			log.debug("update status " + key + " ON");
//			operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.ON);
//			operationSet.add(operation);
//			if(operationSet != null){
//				monitor.simpleUpdateNode(key,operationSet);				
//			}
			}catch(Exception e){
				log.error("Exception",e);
			}
			
		}
		
		
		
		
		// Performing now move vm operations
		iter = moveVmOperations.iterator();		
		
		while (iter.hasNext()) {
			i=0;
			try{
				//log.debug("vm: " + moveVmOperations.get(i).getVirtualMachine());
				MoveVMActionType moveAction = (MoveVMActionType)iter.next();
				this.moveVm(moveAction);

			
			// Deleting the VM on the model and creating the vm on the target host
//			log.debug("Moving Virtual Machine " + ((MoveVMActionType) moveAction).getVirtualMachine() + " to " + ((MoveVMActionType) moveAction).getDestNodeController());
//			// Deleting from source host
//			key = comName + "_" + ((MoveVMActionType) moveAction).getSourceNodeController();
//			operation = new ComOperation(ComOperation.TYPE_REMOVE,"./nativeHypervisor/virtualMachine/frameworkID",((MoveVMActionType) moveAction).getVirtualMachine() + " a a 0");
//			operations.add(operation);
//			((ConcurrentLinkedQueue<ComOperationCollector>) this.getQueuesHashMap().get(key)).add(operations);
//			monitor.updateNode(key, this);
//			operations.remove(operation);
//			((ConcurrentLinkedQueue<ComOperationCollector>) this.getQueuesHashMap().get(key)).poll();
//			// Adding to target host
//			key = comName + "_" + ((MoveVMActionType) moveAction).getDestNodeController(); 
//			operation = new ComOperation(ComOperation.TYPE_ADD, 
//					"./nativeHypervisor/virtualMachine/frameworkID",((MoveVMActionType) moveAction).getVirtualMachine() + " a a 1 " +
//					((MoveVMActionType) moveAction).getVirtualMachine());
//			operations.add(operation);
//			((ConcurrentLinkedQueue<ComOperationCollector>)this.getQueuesHashMap().get(key)).add(operations);						
//			monitor.updateNode(key, this);
//			operations.remove(operation);						
//			((ConcurrentLinkedQueue<ComOperationCollector>)this.getQueuesHashMap().get(key)).poll();
			}catch(Exception e){
				log.error("Exception",e);
			}
			i++;
		}
		
		// Syncronize Threads
//		for(j=0;j<i;j++){
//			try {
//				mv[j].join();
//				// updating number of CPUs
////				log.debug("with numCpus: " + mv[j].getNumCpus());
////				key = comName + "_" + mv[j].getTargetHostName();
////				operation = new ComOperation(ComOperation.TYPE_UPDATE, 
////						"//numberOfCPUs[../frameworkID='" + mv[j].getVmName() + "']", String.valueOf(mv[j].getNumCpus()));
////				operationSet.add(operation);				
////				if(operationSet != null){
////					monitor.simpleUpdateNode(key,operationSet);				
////				}
//			} catch (InterruptedException e) {				
//				e.printStackTrace();
//			}							
//		}
		
		// Performing now migrate vm operations
		iter = migrateVmOperations.iterator();
	
		while (iter.hasNext()) {
			i=0;
			try{
				//log.debug("vm: " + moveVmOperations.get(i).getVirtualMachine());				
				LiveMigrateVMActionType migrateAction = (LiveMigrateVMActionType) iter.next();
				this.liveMigrate(migrateAction);
			}catch(Exception e){
				log.error("Exception",e);
			}
			i++;
		}
		
		
		// Performing at last power off operations
		iter = powerOffOperations.iterator();
		PowerOffActionType powerOffAction;
		while (iter.hasNext()) {
			try{
			powerOffAction = (PowerOffActionType) iter.next();
			this.powerOff(powerOffAction);
			
			// set the status of powering on to the servers
			key = comName + "_" + powerOffAction.getNodeName();
			operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.POWERING_OFF);
			operationSet.add(operation);
			if(operationSet != null){
				monitor.simpleUpdateNode(key,operationSet);				
			}
			}catch(Exception e){
				log.error("Exception",e);
			}

			
			// Update as the state in the model as fast as possible to avoid possible errors when Optimizing			
//			key = comName + "_" + ((PowerOffActionType) iter.next()).getNodeName();
//			log.debug("update status " + key + " OFF");
//			operation = new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatusType.OFF);
//			operationSet.add(operation);
//			if(operationSet != null){
//				monitor.simpleUpdateNode(key,operationSet);				
//			}
		}
		
		// log model when performing action to see whether is working new powering on/off status
		monitor.logModel();
		
		

		return false;
	}
	
	
	/**
	 * 
	 * migrate a Virtual Machine from source node to destination node.
	 * 
	 * @param action type for liveMigrate action.
	 * @return true if migrate is successful, false if otherwise
	 * 
	 * @author jclegea
	 */
	@Override
	public boolean liveMigrate(LiveMigrateVMActionType action) {
		String[] actionArguments = new String[16];
		boolean isMigrated = false;
		String dataCenterName = configurationEni_.getProperty(action.getDestNodeController() + "_DC");
		
		try {		
			actionArguments[0] = "--" + ComEniConstants.URL;
			actionArguments[1] = configurationEni_.getProperty(ComEniConstants.URL + "_" + dataCenterName);
			//actionArguments[1] = url_;
			actionArguments[2] = "--" + ComEniConstants.USER_NAME;
			actionArguments[3] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName));
			//actionArguments[3] = userName_;		
			actionArguments[4] = "--" + ComEniConstants.PASSWORD;
			actionArguments[5] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.PASSWORD + "_" + dataCenterName));
			//actionArguments[5] = password_;
	
			actionArguments[6] = "--" + ComEniConstants.VIRTUAL_MACHINE_NAME;
			actionArguments[7] = action.getVirtualMachine();
			actionArguments[8] = "--" + ComEniConstants.TARGET_HOST;
			actionArguments[9] = action.getDestNodeController();
			actionArguments[10] = "--" + ComEniConstants.SOURCE_HOST;
			actionArguments[11] = action.getSourceNodeController();
			actionArguments[12] = "--" + ComEniConstants.TARGET_POOL;
			actionArguments[13] = "";
			actionArguments[14] = "--" + ComEniConstants.TARGET_DATA_STORE;
			actionArguments[15] = "";


			migrateRelocateVirtualMachine_ = new MigrateRelocateVirtualMachine(actionArguments, ComEniConstants.MIGRATE, null, action);
			log.debug("About to relocate element with id="
					+ actionArguments[7] + " to "
					+ actionArguments[9] + " of framework "
					+ action.getFrameworkName());
			isMigrated = migrateRelocateVirtualMachine_
					.startMigrateOrRelocate(ComEniConstants.MIGRATE);
		} catch (Exception exception) {
			log.error("Action arguments cannot be initialized",exception);
		}

		return isMigrated;
	}

	/**
	 * 
	 * move a Virtual Machine from source node to destination node.
	 * 
	 * @param action type for moveVM action.
	 * @return true if migrate is successful, false if otherwise
	 * 
	 * @author jclegea
	 */
	@Override
	public boolean moveVm(MoveVMActionType action) {
		String[] actionArguments = new String[16];
		boolean isMoved = false;
		String dataCenterName = configurationEni_.getProperty(action.getDestNodeController() + "_DC");
		
		try {
			actionArguments[0] = "--" + ComEniConstants.URL;
			actionArguments[1] = configurationEni_.getProperty(ComEniConstants.URL + "_" + dataCenterName);
			//actionArguments[1] = url_;
			actionArguments[2] = "--" + ComEniConstants.USER_NAME;
			actionArguments[3] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName));
			//actionArguments[3] = userName_;		
			actionArguments[4] = "--" + ComEniConstants.PASSWORD;
			actionArguments[5] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.PASSWORD + "_" + dataCenterName));
			//actionArguments[5] = password_;
	
			actionArguments[6] = "--" + ComEniConstants.VIRTUAL_MACHINE_NAME;
			actionArguments[7] = action.getVirtualMachine();
			actionArguments[8] = "--" + ComEniConstants.TARGET_HOST;
			actionArguments[9] = action.getDestNodeController();
			actionArguments[10] = "--" + ComEniConstants.SOURCE_HOST;
			actionArguments[11] = action.getSourceNodeController();
			actionArguments[12] = "--" + ComEniConstants.TARGET_POOL;
			actionArguments[13] = "";
			actionArguments[14] = "--" + ComEniConstants.TARGET_DATA_STORE;
			actionArguments[15] = "";

		
			migrateRelocateVirtualMachine_ = new MigrateRelocateVirtualMachine(actionArguments, ComEniConstants.MIGRATE, action, null);
			log.debug("About to relocate element with id="
					+ actionArguments[7] + " to "
					+ actionArguments[9] + " of framework "
					+ action.getFrameworkName());
			isMoved = migrateRelocateVirtualMachine_
					.startMigrateOrRelocate(ComEniConstants.MIGRATE);
		} catch (Exception exception) {
			log.error("Action arguments cannot be initialized",exception);
		}

		return isMoved;
	}

	

	/**
	 *  Power on Host
	 *  Return error when operation is not supported.
	 */
	@Override
	public boolean powerOn(PowerOnActionType action) {
		String ip = null;
		String userilo = null;
		String passilo = null;
		String[] actionArguments = new String[10];
		boolean isExecutedOperation = false;
		String dataCenterName = configurationEni_.getProperty(action.getNodeName() + "_DC");
		
		try {		
			actionArguments[0] = "--" + ComEniConstants.URL;
			actionArguments[1] = configurationEni_.getProperty(ComEniConstants.URL + "_" + dataCenterName);
			//actionArguments[1] = url_;
			actionArguments[2] = "--" + ComEniConstants.USER_NAME;
			actionArguments[3] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName));
			//actionArguments[3] = userName_;		
			actionArguments[4] = "--" + ComEniConstants.PASSWORD;
			actionArguments[5] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.PASSWORD + "_" + dataCenterName));
			//actionArguments[5] = password_;
			actionArguments[6] = "--" + ComEniConstants.HOST;		
			actionArguments[7] = action.getNodeName();
			actionArguments[8] = "--" + "operation";
			actionArguments[9] = ComEniConstants.POWER_ON_HOST;


			powerOps = new PowerOps(actionArguments, ComEniConstants.POWER_ON_HOST,null,action);
			log.debug("About to power on  host: "
					+ action.getNodeName() + " of framework "
					+ action.getFrameworkName());
			configurationEni_.getProperty(ComEniConstants.PASSWORD);
			ip = configurationEni_.getProperty(action.getNodeName() + "_ILO");
			userilo = DesencryptString.desencryptString(configurationEni_.getProperty("user_" + action.getNodeName() + "_ILO"));
			passilo = DesencryptString.desencryptString(configurationEni_.getProperty("pass_" + action.getNodeName() + "_ILO"));
			log.debug("port: " + port_ + " IP: " + ip);
			powerOps.setPort(port_);
			powerOps.setIp(ip);
			powerOps.setUserName(userilo);
			powerOps.setPassword(passilo);
			isExecutedOperation = powerOps.startPowerAction();
		} catch (Exception exception) {
			log.error("Action arguments cannot be initialized",exception);
		}
		return isExecutedOperation;		
	}

	/**
	 *  Power off Hosts
	 *  Return error when operation is not supported.
	 */
	@Override
	public boolean powerOff(PowerOffActionType action) {
		//log.error("Operation not supported on this Com component");
		String port = null;
		String ip = null;
		String[] actionArguments = new String[10];
		boolean isExecutedOperation = false;
		String dataCenterName = configurationEni_.getProperty(action.getNodeName() + "_DC");
		
		try {		
			actionArguments[0] = "--" + ComEniConstants.URL;
			actionArguments[1] = configurationEni_.getProperty(ComEniConstants.URL + "_" + dataCenterName);
			//actionArguments[1] = url_;
			actionArguments[2] = "--" + ComEniConstants.USER_NAME;
			actionArguments[3] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.USER_NAME + "_" + dataCenterName));
			//actionArguments[3] = userName_;		
			actionArguments[4] = "--" + ComEniConstants.PASSWORD;
			actionArguments[5] = DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.PASSWORD + "_" + dataCenterName));
			//actionArguments[5] = password_;
			actionArguments[6] = "--" + ComEniConstants.HOST;		
			actionArguments[7] = action.getNodeName();
			actionArguments[8] = "--" + "operation";
			actionArguments[9] = ComEniConstants.POWER_OFF_HOST;
		

			powerOps = new PowerOps(actionArguments, ComEniConstants.POWER_OFF_HOST, action, null);			
			log.debug("About to power off  host: " + action.getNodeName());
			configurationEni_.getProperty(ComEniConstants.PASSWORD);
			port = configurationEni_.getProperty(ComEniConstants.HOST_PORT);
			ip = configurationEni_.getProperty(action.getNodeName() + "_ILO");
			log.debug("port: " + port + " IP: " + ip);
			powerOps.setPort(port);
			powerOps.setIp(ip);
			isExecutedOperation = powerOps.startPowerAction();
		} catch (Exception exception) {
			log.error("Action arguments cannot be initialied",exception);
		}
		return isExecutedOperation;
	}

	/**
	 *  Return error when operation is not supported.
	 */
	@Override
	public boolean startJob(StartJobActionType action) {
		log.error("Operation not supported on this Com component");
		return false;
	}


	/**
	 *  Return error when operation is not supported.
	 */
	@Override
	public boolean standBy(StandByActionType action) {
		log.error("Operation not supported on this Com component");
		return false;
	}

}

