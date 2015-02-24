/**
 * ============================== Header ============================== 
 * file:          PowerOps.java
 * project:       FIT4Green/CommunicatorEni
 * created:       25/11/2010 by jclegea
 * 
 * $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
 * $LastChangedBy: jclegea $
 * $LastChangedRevision: 1497 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package f4g.communicatorEni.vmware;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.TeePipedOutputStream;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;


import com.vmware.apputils.AppUtil;
import com.vmware.apputils.OptionSpec;
import com.vmware.apputils.version.ExtendedAppUtil;
import com.vmware.apputils.version.VersionUtil;
import com.vmware.apputils.vim.VMUtils;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemPowerState;
import com.vmware.vim25.InvalidPowerState;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.Timedout;
import com.vmware.vim25.ToolsUnavailable;


import f4g.commons.com.ComEniConstants; 
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;



public class PowerOps {
	static Logger log = Logger.getLogger(PowerOps.class.getName());

	//private static AppUtil appUtil_;	
	private static VMUtils vmUtils_ = null;
	private static String vmName_ = null;
	private static String userilo_ = null;
	private static String passilo_ = null;
	private static String host_ = null;
	private static String folder_ = null;
	private static String datacenter_ = null;
	private static String pool_ = null;
	private static String guestId_ = null;
	private static String ipAddress_ = null;
	private static String[][] filter_ = null;
	private static ExtendedAppUtil extendedAppUtil_;
	private ManagedObjectReference[] virtualMachineList_;
	private static String ipHost_ = null;
	private static int port_ = 0;
	private String[] actionArguments_ = null;
	private String powerAction_ = null;
	private OptionSpec[] optionalParameters_ = null;
	
	private PowerOffAction powerOffAction_;
	private PowerOnAction powerOnAction_;

	/**
	 * Constructor that initializes the action with the corresponding arguments.
	 *  
	 * @param actionArguments
	 * @param powerAction
	 */
	public PowerOps(String[] actionArguments, String powerAction, PowerOffAction powerOffAction, PowerOnAction powerOnAction) {
		try {
			optionalParameters_ = new OptionSpec[8];
			optionalParameters_ = constructOptions();
			log.debug("Power: " + powerAction + " arguments[" + 
								actionArguments.length + "]");
			
			actionArguments_ = actionArguments;
			powerAction_ = powerAction;
			extendedAppUtil_ = ExtendedAppUtil.initialize(powerAction,optionalParameters_,actionArguments);
			powerOffAction_ = powerOffAction;
			powerOnAction_ = powerOnAction;
			log.debug("get_option: " + extendedAppUtil_.get_option("username"));
		} 
		catch (Exception exception) {
			log.error("Action arguments cannot be initializated");
//			exception.printStackTrace();
			log.error("Exception",exception);
		}
	}
	
	
	public void setIp(String ip){
		ipHost_ = ip;
	}
	
	public void setPort(String port){
		port_ = Integer.valueOf(port);
	}
	
	public void setUserName(String username){
		userilo_ = username;
	}
	
	public void setPassword(String password){
		passilo_ = password;
	}
	
	/**
	 * 
	 * get the managed object references in the server, virtualmachines, host, 
	 * datastores...
	 * 
	 * @param name of the object to get.
	 * @param type of the object.
	 * @param root in the hierarchy of objects.
	 * 
	 * @return the managed object reference with name and type from the arguments. 
	 * 
	 * @author jclegea
	 */
	private ManagedObjectReference getMor(String name, String type,
			ManagedObjectReference root){

		ManagedObjectReference nameMor=null;
		try {
			nameMor = (ManagedObjectReference) extendedAppUtil_.getServiceUtil3().getDecendentMoRef(root, type, name);

		if (nameMor == null) {
			log.error("Error:: " + name + " not found");
			//return null;
		} else {
			//return nameMor;
		}
		}
		catch (Exception exception) {
//			exception.printStackTrace();
			log.error("Exception",exception);
		}
		
		return nameMor;
	}

	/**
	 * 
	 * get a list of the virtual machines in the server, 
	 * uses the parameters initialized to perform the search of virtual machines. 
	 * 
	 * @return the ArrayList of virtual machines obtained in the search.
	 *
	 * @author jclegea
	 */
	private ArrayList getVirtualMachines(){
		ArrayList vmList = new ArrayList();
		if (extendedAppUtil_.option_is_set(ComEniConstants.HOST)) {
			host_ = extendedAppUtil_.get_option(ComEniConstants.HOST);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.FOLDER)) {
			folder_ = extendedAppUtil_.get_option(ComEniConstants.FOLDER);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.DATA_CENTER)) {
			datacenter_ = extendedAppUtil_.get_option(ComEniConstants.DATA_CENTER);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.VIRTUAL_MACHINE_NAME)) {
			vmName_ = extendedAppUtil_.get_option(ComEniConstants.VIRTUAL_MACHINE_NAME);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.POOL)) {
			pool_ = extendedAppUtil_.get_option(ComEniConstants.POOL);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.IP_ADRESS)) {
			ipAddress_ = extendedAppUtil_.get_option(ComEniConstants.IP_ADRESS);
		}
		if (extendedAppUtil_.option_is_set(ComEniConstants.GUEST_ID)) {
			guestId_ = extendedAppUtil_.get_option(ComEniConstants.GUEST_ID);
		}
		// filter = new String[][] { new String[] { "summary.config.guestId",
		// "winXPProGuest",},};
		filter_ = new String[][] { new String[] { "guest.ipAddress", ipAddress_, },
				new String[] { "summary.config.guestId", guestId_, } };
		try {
			log.debug("VMachine Name: " + vmName_ + " - " + datacenter_ + " - " + 
								folder_ + " - " + pool_ + " - " + vmName_ + " - " + host_ + 
								" - " + guestId_ + " - " + ipAddress_ + " Filter: " + filter_);
			virtualMachineList_ = (ManagedObjectReference [])extendedAppUtil_.getServiceUtil3().getDynamicProperty(extendedAppUtil_.getServiceUtil3().getDecendentMoRef(null, "HostSystem", host_), "vm");
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
		
		return vmList;
	}

	/**
	 * 
	 * get if the task performed finished ok.
	 * 
	 * @param task managed object reference to get the info.
	 * 
	 * @return true if the task finished ok, false otherwise.
	 *
	 * @author jclegea
	 */
	private boolean getTaskInfo(ManagedObjectReference taskMor){
		boolean isValid = false;

		String res;
		try {
			res = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
			
			if (res.equalsIgnoreCase("sucess")) {
				isValid = true;
			} else {
				isValid = false;
			}
		}
		catch (Exception exception) {
			log.debug("Cannot get the Task Info");
			log.error("Exception",exception);
		}

		return isValid;
	}

	/**
	 * 
	 * Power On virtual machines. 
	 * 
	 * @param vmList, the array of virtual machines to power on.
	 * 
	 * @return true if the virtual machines are powered ok.
	 *
	 * @author jclegea
	 */
	private boolean powerOnVM(ArrayList vmList){
		boolean isTaskInfo = false; 
		try{
			for (int i = 0; i < vmList.size(); i++) {
				String vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty((ManagedObjectReference) vmList.get(i), "name");
				ManagedObjectReference vmMor = (ManagedObjectReference) vmList.get(i);
				ManagedObjectReference taskMor = null;
				try {
					log.debug("Powering on virtualmachine '" + vmName + "'");
					taskMor = extendedAppUtil_.getServiceConnection3().getService()
					.powerOnVM_Task(vmMor, null);
					
					isTaskInfo = getTaskInfo(taskMor);
					if (isTaskInfo) {
						log.debug("" + vmName + " powered on successfuly");
					}
				} 
				catch (InvalidPowerState invalidPowerStateException) {
					log.error("Virtual Machine is already powered on");
				} 
				catch (Exception exception) {
					log.error("Exception",exception);
				}
			}
		}
		catch(Exception exception){
			log.error("Exception",exception);
		}
		return isTaskInfo;
	}

	/**
	 * 
	 * power off virtual machines.
	 * 
	 * @param vmList, the array of virtual machines to power off.
	 * 
	 * @return true if the virtual machines are powered off.
	 *
	 * @author jclegea
	 */
	private boolean powerOffVM(ArrayList vmList){		
		boolean isTaskInfo = false;
		
		try{
			for (int i = 0; i < virtualMachineList_.length; i++) {
				String vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
				ManagedObjectReference taskMor = null;
				try {
					log.debug("Powering off virtualmachine '" + vmName + "'");
					taskMor = extendedAppUtil_.getServiceConnection3().getService().powerOffVM_Task(virtualMachineList_[i]);
					isTaskInfo = getTaskInfo(taskMor);
					if (isTaskInfo) {
						log.debug("Virtual Machine " + vmName + " powered off successfuly");
					}
				} 
				catch (Exception exception) {
					log.error("Error");
					log.error("Exception",exception);
				}
			}
		}
		catch(Exception exception){
			log.error("Exception",exception);
		}
		
		return isTaskInfo;
	}

	/**
	 * 
	 * reset virtual machines.
	 * 
	 * @param vmList, the list of virtual machines to reset.
	 * 
	 * @return true if the virtual machines are reseted.
	 *
	 * @author jclegea
	 */
	private boolean resetVM(ArrayList vmList){
		boolean isTaskInfo = false;
		
		try{
			for (int i = 0; i < virtualMachineList_.length; i++) {
				String vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
				ManagedObjectReference taskMor = null;
				try {
					log.debug("Reseting virtualmachine '" + vmName + "'");
					taskMor = extendedAppUtil_.getServiceConnection3().getService().resetVM_Task(virtualMachineList_[i]);
					isTaskInfo = getTaskInfo(taskMor);
					if (isTaskInfo) {
						log.debug("Virtual Machine " + vmName + " reset successfuly");
					}
				} 
				catch (Exception exception) {
					log.error("Error");
					log.error("Exception",exception);
				}
			}
		}
		catch(Exception exception){
			log.error("Exception",exception);
		}
		
		return isTaskInfo;
	}

	/**
	 * 
	 * suspend virtual machines.
	 * 
	 * @param vmList, the list of virtual machines to suspend.
	 * 
	 * @return true if the virtual machines are suspended ok, false otherwise.
	 *
	 * @author jclegea
	 */
	private boolean suspendVM(ArrayList vmList){
		boolean isTaskInfo = false;
		
		try{					
				for (int i = 0; i < virtualMachineList_.length; i++) {
					String vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
				ManagedObjectReference taskMor = null;
				try {
					log.debug("Suspending virtualmachine '" + vmName + "'");
					taskMor = extendedAppUtil_.getServiceConnection3().getService().suspendVM_Task(virtualMachineList_[i]);					
					isTaskInfo = getTaskInfo(taskMor);
					if (isTaskInfo) {
						log.debug("Virtual Machine " + vmName + " suspended successfuly");
						return isTaskInfo;
					}
				} 
				catch (Exception exception) {					
					log.error("Exception",exception);
				}
			}
		}
		catch(Exception exception){
			log.error("Exception",exception);
		}
		
		return isTaskInfo;
	}

	/**
	 * 
	 * reboot virtual machines.
	 * 
	 * @param vmList, the list of virtual machines to reboot.
	 * 
	 * @author jclegea
	 */
	private void rebootVM(ArrayList vmList){
		String vmName = ""; 
		
		for (int i = 0; i < virtualMachineList_.length; i++) {
			try{
				vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
			}
			catch(Exception exception){
				log.error("Exception",exception);
			}
			
		
			try {
				log.debug("Rebooting virtualmachine '" + vmName + "'");
				extendedAppUtil_.getServiceConnection3().getService().rebootGuest(virtualMachineList_[i]);
				log.debug("Guest os in vm '" + vmName + "' rebooted");				
			} 
			catch (InvalidPowerState invalidPowerState) {
				log.debug("Error : Operation cannot be performed in the current power state");
			} 
			catch (InvalidState invalidEstate) {
				log.error(" Error : Operation cannot be performed because of the virtual machine's current state");
			} 
			catch (ToolsUnavailable toolsUnavaliable) {
				log.error("Error :VMware Tools are not running.");
			} 
			catch (Exception exception) {
				log.error("Exception",exception);
			}
		}		
	}

	/**
	 * 
	 * shutdown virtual machines.
	 * 
	 * @param vmList, the list of virtual machines to shutdown.	 
	 *
	 * @author jclegea
	 */
	private void shutdownVM(ArrayList vmList){
		String vmName = "";
		
		 for (int i = 0; i < virtualMachineList_.length; i++) {		                    
			try{
				vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
			}
			catch (Exception exception){
				log.error("Exception",exception);
			}
		
		
			try {
				log.debug("Shutting down virtualmachine '" + vmName + "'");
				extendedAppUtil_.getServiceConnection3().getService().shutdownGuest(virtualMachineList_[i]);
				log.debug("Guest os in vm '" + vmName + "' shutdown");
			} 
			catch (InvalidPowerState invalidPowerState) {
				log.error("Error : Operation cannot be performed in the current power state");
			} 
			catch (InvalidState e) {
				log.error("Error :  Operation cannot be performed because of the virtual machine's current state");
			} 
			catch (ToolsUnavailable e) {
				log.error("Error :  VMware Tools are not running.");
			} 
			catch (Exception exception) {
				log.error("Exception",exception);
			}
		}		
	}
	
	
	/**
	 * 
	 * Standby virtual machines.
	 * 
	 * @param vmList, the list of virtual machines to stand by.
	 *
	 * @author jclegea
	 */
	private void standbyVM(ArrayList vmList){
		String vmName = ""; 
			
		for (int i = 0; i < virtualMachineList_.length; i++) {
			try {
				vmName = (String) extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i], "name");
			}
			catch(Exception exception){
				log.error("Exception",exception);
			}
			
			try {
				log.debug("Putting the guestOs of vm '" + vmName + "' in standby mode");
				extendedAppUtil_.getServiceConnection3().getService().standbyGuest(virtualMachineList_[i]);				
				log.debug("GuestOs of vm '" + vmName + "' put in standby mode");
			} 
			catch (InvalidPowerState invalidPowerState) {
				log.error("Error :  Operation cannot be performed in the current power state");
			} 
			catch (InvalidState invalidState) {
				log.error("Error : Operation cannot be performed because of the virtual machine's current state");
			} 
			catch (ToolsUnavailable toolsUnavailable) {
				log.error("Error : VMware Tools are not running.");
			} 
			catch (Exception exception) {
				log.error("Exception",exception);
			}
		}
	}
	
	/**
	 * 
	 * Execute a command through ssh
	 * 
	 * @param command to execute
	 * @param host to connect
	 * @param port to connect
	 * @return true if command is executed, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean sshExecuteCommand(String command,String host,int port)
  {
		//ArrayList arraylist;
//		log.info((new StringBuilder())
//				.append("Creating a ssh client session to host: ")
//				.append(host).append(" on port: ").append(22).toString());
		// log.info((new
		// StringBuilder()).append("Creating a ssh client session to host: ").append("albujon").append(" on port: ").append(22).toString());
		//arraylist = new ArrayList();
		SshClient sshClient;
		ClientSession clientSession;
		ChannelExec channelExec;
		ByteArrayOutputStream byteArrayOutputStream;
		TeePipedOutputStream teePipedOutputStream;
		PipedInputStream pipedInputStream;
		PipedOutputStream pipedOutputStream;
		BufferedReader bufferedReader;
		String s1;
		boolean isSshExecuted = false;
		try {
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();
			System.out.println("Connecting...");
			clientSession = ((ConnectFuture) sshClient.connect(host, port).await()).getSession();
			// clientSession = ((ConnectFuture)sshclient.connect("192.168.204.43",
			// 22).await()).getSession();			
			if (!((AuthFuture) clientSession.authPassword(userilo_,
					passilo_).await()).isSuccess()) {
				log.error("Authentication Failed");
				return isSshExecuted;
			}

			channelExec = clientSession.createExecChannel(command);
			byteArrayOutputStream = new ByteArrayOutputStream();
			teePipedOutputStream = new TeePipedOutputStream(byteArrayOutputStream);
			channelExec.setIn(new PipedInputStream(teePipedOutputStream));
			pipedInputStream = new PipedInputStream();
			pipedOutputStream = new PipedOutputStream(pipedInputStream);
			channelExec.setOut(pipedOutputStream);
			channelExec.setErr(pipedOutputStream);
			channelExec.open();
			bufferedReader = new BufferedReader(new InputStreamReader(pipedInputStream));
			s1 = bufferedReader.readLine();
			do {
				s1 = bufferedReader.readLine();
				if (s1 == null)
					break;
				log.debug((new StringBuilder()).append("ssh return: ").append(s1).toString());
			} while (true);
			channelExec.waitFor(2, 0L);
			channelExec.close(false);			
			sshClient.stop();			
			isSshExecuted = true;
		} catch (InterruptedException interruptedexception) {
			log.error("Exception",interruptedexception);
			isSshExecuted = false;
		} catch (IOException ioexception) {
			log.error("Exception",ioexception);
			isSshExecuted = false;
		} catch (Exception exception) {
			log.error("Exception",exception);
			isSshExecuted = false;
		}
		
      return isSshExecuted;
  }
	
	
	/*
	 * Power off a host.
	 * 
	 * @param hostSystem to power off to stand by.
	 */
	private void powerOffHost(String hostSystem){		
		log.debug("about to shutdown hostsystem: " + hostSystem);
		ManagedObjectReference hostSystemMOR = getMor(hostSystem,ComEniConstants.HOST_SYSTEM, null);
		ManagedObjectReference taskMor = null;
		ArrayList arrList = null;
		String operationResult = null; 
		
		//log.debug("POWER OFF");			
//		log.debug("Power off host: " +sshExecuteCommand("stop /system1",ipHost_,port_));
		
	
		try {
			// shutdown virtual machines of the host
//			arrList = getVirtualMachines();
//			log.debug("shutdown guest of " + arrList.size() + " virtual machines");
//			shutdownVM(arrList);
			
			// Enter maintenance mode			
			taskMor = extendedAppUtil_.getServiceConnection3().getService().enterMaintenanceMode_Task(hostSystemMOR, 3600, null);
			
			// set fowarded boolean
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			powerOffAction_.setForwarded(true);
			powerOffAction_.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
			
			operationResult = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);

			log.debug("Entered maintenance mode: " + operationResult);

			log.debug("host " + hostSystem + " entered in maintenance mode");
			// Power off host
			taskMor = extendedAppUtil_.getServiceConnection3().getService().shutdownHost_Task(hostSystemMOR, false);
			log.debug("host powered off");
			//operationResult = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
			
		// power on virtual machines of the host again			
//			log.debug("power on guest of " + arrList.size() + " virtual machines");
//			powerOnVM(arrList);
			
			
		} catch (InvalidState e) {
			log.error("Host is in an invalid state");			
		} catch (Timedout e) {
			log.error("Timeout to shutdown the Host");			
		} catch (RuntimeFault e) {			
			log.error("Exception",e);
		} catch (RemoteException e) {
			log.error("Exception",e);
		} catch (Exception e) {
			log.error("Exception",e);
		} 
	}
	
	/**
	 * 
	 * Power on a host from stand by mode for now.
	 * 
	 * @param hostSystem to power on from stand by.
	 *
	 * @author jclegea
	 */
	private void powerOnHost(String hostSystem){
		log.debug("POWER ON");
		// log.debug("Power on host: "
		// +sshExecuteCommand("show /system1",ipHost_,port_));
		log.debug("Power on host: "	+ sshExecuteCommand("start /system1", ipHost_, port_));

		// set fowarded boolean
		try {
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			powerOnAction_.setForwarded(true);		
			powerOnAction_.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
		} catch (DatatypeConfigurationException exception) {			
			log.error("Exception",exception);
		}

		boolean isInvalidState = true;
		log.debug("about to power on hostsystem: " + hostSystem);
		ManagedObjectReference hostSystemMOR = getMor(hostSystem,
				ComEniConstants.HOST_SYSTEM, null);
		ManagedObjectReference taskMor = null;
		HostRuntimeInfo runtimeInfo;
		HostSystemPowerState powerState;


		// wait to the host to start
		try {
		Thread.sleep(180000);
		
		// reconnect host
//		while (isInvalidState) {
//			
//				taskMor = extendedAppUtil_.getServiceConnection3().getService()
//				.reconnectHost_Task(hostSystemMOR, null);		
//				String result = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
//				log.debug("Result: " + result);
//				if("success".equals(result))
//					isInvalidState = false;			
//			
//			// wait host to power on
//			
//				Thread.sleep(60000);
//			
//		}
		
		// exit maintenance mode
		while (isInvalidState) {
			runtimeInfo = (HostRuntimeInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostSystemMOR, "runtime");
			
			log.debug("POWERING ON... Is in maintenance mode?? " + runtimeInfo.isInMaintenanceMode() + " powerState: " + runtimeInfo.getPowerState().getValue());
			
			taskMor = extendedAppUtil_.getServiceConnection3().getService()
					.exitMaintenanceMode_Task(hostSystemMOR, 3600);
			String result = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
			log.debug("Result: " + result);
			if("success".equals(result))
				isInvalidState = false;	
			
			// Test if host is already on
			
			powerState = runtimeInfo.getPowerState();
			if("poweredOn".equals(powerState.getValue()) == true){
				isInvalidState = false;
			}
		
		// wait host to power on
		
			Thread.sleep(60000);
		
	}
		
		} catch (InvalidState e) {
			log.error("Host is on invalid state");
			isInvalidState = true;
			log.error("Exception",e);
		} catch (Timedout e) {
			log.error("Exception",e);
			isInvalidState = false;
		} catch (RuntimeFault e) {
			log.error("Exception",e);
			isInvalidState = false;
		} catch (RemoteException e) {
			log.error("Exception",e);
			isInvalidState = false;
		} catch (InterruptedException e) {
			log.error("Exception",e);
			isInvalidState = false;
		} catch (Exception e) {
			log.error("Exception",e);
			isInvalidState = false;
		}

	}
	
	private void suspendHost(String hostSystem){
		try {
			boolean isTaskInfo = false;
			log.debug("about to shutdown hostsystem: " + hostSystem);
			ManagedObjectReference hostSystemMOR = getMor(hostSystem, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference taskMor = null;


			// power off to standby mode
			// extendedAppUtil_.connect();
			taskMor = extendedAppUtil_.getServiceConnection3().getService().powerDownHostToStandBy_Task(hostSystemMOR, 3600, false);
			String result = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
			log.debug("Result: " + result);
			// extendedAppUtil_.disConnect();

			// Enter maintenance mode
			 //taskMor =
			 //appUtil_.getConnection().getService().enterMaintenanceMode_Task(hostSystemMOR,
			 //3600);
			// while(isTaskInfo == false){
			// isTaskInfo = getTaskInfo(taskMor);
			// log.debug("isTaskInfo: " + isTaskInfo);
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// log.debug("Entered manteinance mode");
			// if (isTaskInfo) {
			// log.debug("host " + hostSystem + " entered in maintenance mode");
			// Power off host
			// taskMor =
			// appUtil_.getConnection().getService().shutdownHost_Task(hostSystemMOR,false);

			// }

		} catch (Exception exception) {
			log.error("Cannot power off the host into standby mode",exception);
		}
		
	}
	
	private void bootHost(String hostSystem){
		try {
			boolean isTaskInfo = false;
			log.debug("about to bootHost hostsystem: " + hostSystem);
			ManagedObjectReference hostSystemMOR = getMor(hostSystem, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference taskMor = null;


			// power off to standby mode
			// extendedAppUtil_.connect();
			taskMor = extendedAppUtil_.getServiceConnection3().getService().powerUpHostFromStandBy_Task(hostSystemMOR, 3600);
			String result = extendedAppUtil_.getServiceUtil3().waitForTask(taskMor);
			log.debug("Result: " + result);
			// extendedAppUtil_.disConnect();

			// Enter maintenance mode
			// taskMor =
			// appUtil_.getConnection().getService().enterMaintenanceMode_Task(hostSystemMOR,
			// 3600);
			// while(isTaskInfo == false){
			// isTaskInfo = getTaskInfo(taskMor);
			// log.debug("isTaskInfo: " + isTaskInfo);
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// log.debug("Entered manteinance mode");
			// if (isTaskInfo) {
			// log.debug("host " + hostSystem + " entered in maintenance mode");
			// Power off host
			// taskMor =
			// appUtil_.getConnection().getService().shutdownHost_Task(hostSystemMOR,false);

			// }

		} catch (Exception exception) {
			log.error("Cannot power on the host into standby mode",exception);
		}		
	}

	/**
	 * 
	 * Performs the power operation requested.
	 * 
	 * @return true if operation runs ok, false otherwise.
	 *
	 * @author jclegea
	 */
	private boolean runOperation(){
		
		String operation = extendedAppUtil_.get_option("operation");
		ArrayList arrList = getVirtualMachines();
		String hostSystem = extendedAppUtil_.get_option(ComEniConstants.HOST);
		boolean isCorrectOperation = false;

		if (arrList == null) {
			log.error("No virtual machine found");
			return isCorrectOperation;
		} else {
			if (operation.equalsIgnoreCase(ComEniConstants.POWER_ON)) {
				isCorrectOperation = powerOnVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.POWER_OFF)) {
				isCorrectOperation = powerOffVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.RESET)) {
				isCorrectOperation = resetVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.SUSPEND)) {
				isCorrectOperation = suspendVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.REBOOT)) {
				rebootVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.SHUTDOWN)) {
				shutdownVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.STANDBY)) {
				standbyVM(arrList);
			} else if (operation.equalsIgnoreCase(ComEniConstants.POWER_OFF_HOST)) {
				powerOffHost(hostSystem);
			} else if (operation.equalsIgnoreCase(ComEniConstants.POWER_ON_HOST)) {
				powerOnHost(hostSystem);
			} else if(operation.equalsIgnoreCase(ComEniConstants.SUSPEND_HOST)){
				suspendHost(hostSystem);
			} else if(operation.equalsIgnoreCase(ComEniConstants.BOOT_HOST)){
				bootHost(hostSystem);
			}
			
			
		}
		return isCorrectOperation;
	}
	
	/**
	 * 
	 * initialize optional arguments to the actions.
	 * 
	 * @return an Array of optional arguments.
	 */
	private static OptionSpec[] constructOptions() {
    OptionSpec [] useroptions = new OptionSpec[8];
    useroptions[0] = new OptionSpec("vmname","String",0,"Name of the virtual machine",null);
    useroptions[1] = new OptionSpec("host","String",0,"Name of the host",null);
    useroptions[2] = new OptionSpec("datacenter","String",0,"Name of the datacenter",null);
    useroptions[3] = new OptionSpec("folder","String",0,"Name of the folder",null);
    useroptions[4] = new OptionSpec("pool","String",0,"Name of the resource pool",null);
    useroptions[5] = new OptionSpec("guestid","String",0,"Id of the guestOS",null);
    useroptions[6] = new OptionSpec("ipaddress","String",0,"IpAddress of the guestOS",null);
    useroptions[7] = new OptionSpec("operation","String",1,"operation to be performed",null);
    return useroptions;
 }      

	/**
	 * 
	 * entry point to do any power operation in virtual machines.
	 * 
	 * @return true if the operation is executed ok, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean startPowerAction() {

		boolean isExecutedAction = false;

		try {
			//appUtil_.connect();
			extendedAppUtil_ = ExtendedAppUtil.initialize(powerAction_,optionalParameters_,actionArguments_);
			extendedAppUtil_.connect();
			vmUtils_ = new VMUtils(extendedAppUtil_);
			isExecutedAction = runOperation();
			extendedAppUtil_.disConnect();
			//appUtil_.disConnect();
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}

		return isExecutedAction;
	}

}
