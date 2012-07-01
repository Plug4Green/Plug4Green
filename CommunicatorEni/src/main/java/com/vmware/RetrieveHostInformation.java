/**
* ============================== Header ============================== 
* file:          RetrieveHostInformation.java
* project:       FIT4Green/CommunicatorEni
* created:       09/12/2010 by jclegea
* 
* $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1497 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package com.vmware;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.TeePipedOutputStream;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.f4g.com.ComEniConstants;


//import com.vmware.apputils.AppUtil;
import com.vmware.apputils.version.ExtendedAppUtil;
import com.vmware.apputils.vim.ServiceConnection;
import com.vmware.apputils.vim25.ServiceUtil;
import com.vmware.vim.VMotionCompatibilityType;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.HostCapability;
import com.vmware.vim25.HostNumericSensorInfo;
import com.vmware.vim25.HostPowerOperationType;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HealthSystemRuntime;
import com.vmware.vim25.HostSystemPowerState;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineStorageInfo;
import com.vmware.vim25.VirtualMachineUsageOnDatastore;


	


public class RetrieveHostInformation {
	static Logger log = Logger.getLogger(RetrieveHostInformation.class.getName());
	
	//private HostHardwareInfo hardwareInfo_;
	//private HostRuntimeInfo runtimeInfo_;
	//private HealthSystemRuntime healthRuntime_;
	private ArrayList<ManagedObjectReference> hostList_ = new ArrayList<ManagedObjectReference>();
	private ManagedObjectReference[] virtualMachineList_;
	

	//private static AppUtil appUtil_;
	private ExtendedAppUtil extendedAppUtil_;
	private String[] actionArguments_ = null;
	private String retrieveAction_ = null;
	private String userilo_ = null;
	private String passilo_ = null;	
	private int port_ = 0;
	private String ipHost_ = null;

	/**
	 *  Constructor that initialize with arguments to connect ENI. 
	 * @param actionArguments of the action.
	 * @param retrieveAction name of the action.
	 */
	public RetrieveHostInformation(String[] actionArguments, String retrieveAction){
		try {
			//appUtil_ = AppUtil.initialize(retrieveAction, actionArguments);
			log.debug("actionarguments[1]: " + actionArguments[1]);
			actionArguments_ = actionArguments;
			retrieveAction_ = retrieveAction;
			extendedAppUtil_ = new ExtendedAppUtil(retrieveAction);
			extendedAppUtil_ = ExtendedAppUtil.initialize(retrieveAction,actionArguments);
			log.debug("extendedAppUtil_: " + extendedAppUtil_.toString());
//			log.debug("BEFORE CONNECT connection data - url: " + extendedAppUtil_.getServiceUrl() +
//					" user: " + extendedAppUtil_.getUsername() +
//					" password: " + extendedAppUtil_.getPassword());
		}
		catch (Exception exception) {
			log.error("Action arguments cannot be initialized",exception);
		}		
	}

		
	public void setPort(String port){
		port_ = Integer.valueOf(port);
	}
	
	public void setIp(String ip){
		ipHost_ = ip;
	}
	
	public void setUserName(String username){
		userilo_ = username;
	}
	
	public void setPassword(String password){
		passilo_ = password;
	}
	
	

	/**
	 * 
	 * get Managed Objects References, this references could be references 
	 * to hosts in ENI or Virtual Machines.
	 * 
	 * @param type of the references to get.
	 * @param root of the ManagedObjectReferences tree in vSphere inventory.
	 * @return an ArrayList with all references obtained.
	 *
	 * @author jclegea
	 */
	private ArrayList<ManagedObjectReference> getMors(String type,ManagedObjectReference root){
		String[][] filter;
		ArrayList<ManagedObjectReference> arrayMors = new ArrayList<ManagedObjectReference>();
		
		filter = new String[][] { new String[] { "guest.ipAddress", null, },
						 new String[] { "summary.config.guestId", null, } };
		
		try {
			arrayMors = extendedAppUtil_.getServiceUtil3().getDecendentMoRefs(root,type,filter);
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
		
		return arrayMors;
	}
	

	/**
	 * 
	 * Retrieve the hosts from ENI's Servers.
	 *  
	 * @return true if hosts are obtained, false otherwise.
	 *
	 * @author jclegea
	 */
	private boolean retrieveHosts(){
		boolean isRetrieved=false;		
				
		log.debug("Starting retrieving hosts");
		
		try {						
			hostList_ = getMors("HostSystem", null);
			log.debug("hostsystems: " + hostList_.size());
			isRetrieved = true;			
			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve hosts from the system",exception);
			isRetrieved = false;
		}			
		
		return isRetrieved;
	}
	
	/**
	 * 
	 * Get the Size of the Hosts List.
	 * 
	 * @return the size of the Host List.
	 *
	 * @author jclegea
	 */
	public int getHostListSize(){				
		return hostList_.size();
		
	}
	
	/**
	 * 
	 * get the name of the host by its index
	 * 
	 * @param index of the host 
	 *
	 * @return a String with the hostname.  
	 * 
	 * @author jclegea
	 * @throws Exception 
	 */
	public String getHostName(int hostIndex) throws Exception{
		String hostName = null;
		try {			
			hostName = (String)extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostList_.get(hostIndex),"name");			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve host name.",exception);
		}
		
		return hostName;
	}
	
		
	/**
	 * 
	 * get the power state of the host
	 * 
	 * @param index of the host
	 * 
	 * @return a String with the power state
	 * @throws Exception
	 *
	 * @author jclegea
	 */
	public String getHostPowerState(int hostIndex) throws Exception{
		String state = "PoweredOff";
		HostRuntimeInfo runtimeInfo;
		HostSystemPowerState powerState;
		
		try {			
			runtimeInfo = (HostRuntimeInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostList_.get(hostIndex), "runtime");
			
			powerState = runtimeInfo.getPowerState();
			state = powerState.getValue();			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve power state.",exception);
		}
		
		return state;
	}
	
		
	/**
	 * 
	 * get the HostCapabilities
	 * 
	 * @param hostIndex
	 * @return
	 * @throws Exception
	 *
	 * @author jclegea
	 */
	public HostCapability getHostCapabilities(int hostIndex) throws Exception{		
		HostCapability hostCapability = null;	
		
		try {			
			hostCapability = (HostCapability)extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostList_.get(hostIndex), "capability");
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve power state.",exception);
		}
		
		return hostCapability;
	}
	
	/**
	 * 
	 * get the Measured Power of the host by its index.
	 * 
	 * @param index of the host.
	 *  
	 * @return a souble with the power measured for the host
	 * passed by parameter.
	 *
	 * @author jclegea
	 */
	public double getHostMeasuredPower(int hostIndex) throws Exception{		
		HostNumericSensorInfo[] sensorsData;
		HealthSystemRuntime healthRuntime;
		double measuredPower = 0.0;		
		
		HostRuntimeInfo runtimeInfo;
		try {
			runtimeInfo = (HostRuntimeInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostList_.get(hostIndex), "runtime");		
	
		healthRuntime = runtimeInfo.getHealthSystemRuntime();		
		sensorsData = healthRuntime.getSystemHealthInfo().getNumericSensorInfo();
		measuredPower = sensorsData[0].getCurrentReading()/100;
		
		} 
		catch (Exception exception) {
			log.error("Cannot Retrieve measured power from host: " + getHostName(hostIndex));			
		}
		
		return measuredPower;
	}
	
	public String getHostMeasuredPowerILO(int hostIndex) throws Exception{
		SshClient sshClient = null;
		ClientSession clientSession;
		ChannelExec channelExec = null;
		ByteArrayOutputStream byteArrayOutputStream;
		TeePipedOutputStream teePipedOutputStream;
		PipedInputStream pipedInputStream;
		PipedOutputStream pipedOutputStream;
		BufferedReader bufferedReader;
		String s1;
		String power = "0.0";
		boolean isSshExecuted = false;		
		try {
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();
			clientSession = ((ConnectFuture) sshClient.connect(ipHost_, port_).await()).getSession();
			if (!((AuthFuture) clientSession.authPassword(userilo_,passilo_).await()).isSuccess()) {
				log.error("Authentication Failed");						
				sshClient.stop();	
				isSshExecuted = false;
				return "0.0";
			}
			
			//channelExec = clientSession.createExecChannel("show /system1/oemhp_power1");
			channelExec = clientSession.createExecChannel("show /system1");
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
			while(s1 != null){							
//				log.debug("S1: " + s1);				
				if(s1.contains("oemhp_PresentPower") == true ){
					power = s1.substring(s1.indexOf("=")+1,s1.indexOf("W")-1);
//					log.debug("power: " + s1.substring(s1.indexOf("=")+1,s1.indexOf("W")-1));
					break;
				}
				s1 = bufferedReader.readLine();
//				log.debug((new StringBuilder()).append("ssh return: ").append(s1).toString());
			}
			channelExec.waitFor(2, 0L);
			channelExec.close(false);			
			sshClient.stop();			
			isSshExecuted = true;
		} catch (InterruptedException interruptedexception) {
			channelExec.close(false);			
			sshClient.stop();	
//			interruptedexception.printStackTrace();
			log.error("Exception",interruptedexception);
			isSshExecuted = false;
		} catch (IOException ioexception) {
//			ioexception.printStackTrace();
			log.error("Exception",ioexception);
			channelExec.close(false);			
			sshClient.stop();
			isSshExecuted = false;
		} catch (Exception exception) {
//			exception.printStackTrace();
			log.error("Exception",exception);
			channelExec.close(false);		
			sshClient.stop();			
			isSshExecuted = false;
		}
		return power;
	}
	
	
	/**
	 * 
	 * Retrieve p-states of cores form ILO with SSHD exec connection
	 * 
	 * @param pStates is a list of array with 2 values
	 * lastPstate and total pStates available
	 * @return 
	 * @throws Exception
	 *
	 * @author jclegea
	 */
	public String getPStatesILOExec(ArrayList<int[]> pStates) throws Exception{
		//ArrayList arraylist;
		//log.info((new StringBuilder())
		//		.append("Creating a ssh client session to host: ")
		//		.append(host).append(" on port: ").append(22).toString());
		// log.info((new
		// StringBuilder()).append("Creating a ssh client session to host: ").append("albujon").append(" on port: ").append(22).toString());
		//arraylist = new ArrayList();
		SshClient sshClient = null;
		ClientSession clientSession;
		ChannelExec channelExec = null;
		ByteArrayOutputStream byteArrayOutputStream;
		TeePipedOutputStream teePipedOutputStream;
		PipedInputStream pipedInputStream;
		PipedOutputStream pipedOutputStream;
		BufferedReader bufferedReader;
		String s1;
		String power = "0.0";
		int numCpu=0;
		int numLogicalProcessors = 0;
		int numPstates= 0;
		int[] pStatesValues = new int[2];
		
		boolean isSshExecuted = false;
		try {
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();			
			//System.out.println("Connecting...");			
//			log.debug("ipHost: " + ipHost_ + " port: " + port_);
			clientSession = ((ConnectFuture) sshClient.connect(ipHost_, port_).await()).getSession();
			// clientSession = ((ConnectFuture)sshclient.connect("192.168.204.43",
			// 22).await()).getSession();
//			log.debug("clientSession: " + clientSession.toString());
//			log.debug("userilo: " + userilo_ + " passilo: " + passilo_);
			if (!((AuthFuture) clientSession.authPassword(userilo_,passilo_).await()).isSuccess()) {
				log.error("Authentication Failed");						
				sshClient.stop();	
				isSshExecuted = false;
				return "0.0";
			}
			
			// calculate CPUs      

			channelExec = clientSession.createExecChannel("show /system1/");
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
			numCpu=0;
			while(s1 != null){
//				log.debug("S1: " + s1);
				if(s1.contains("cpu") == true ){
					numCpu++;
				}
				s1 = bufferedReader.readLine();				
			}
			log.debug("numCPus: " + numCpu);			
			
			
			for(int i=1;i<=numCpu;i++){
				// Obtain logical processors
				numLogicalProcessors = 0;			
				channelExec = clientSession.createExecChannel("show /system1/cpu" + i);				
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
				while(s1 != null){
//					log.debug("S1: " + s1);
					if(s1.contains("logical_processor") == true ){
//						log.debug(s1.substring(s1.indexOf("l"),s1.length()));
						numLogicalProcessors ++;						
					}					
					s1 = bufferedReader.readLine();
				}
				
				for(int j=1;j<=numLogicalProcessors; j++){
					//Get the Pstates values
					channelExec = clientSession.createExecChannel("show /system1/cpu" + i + "/logical_processor" + j);				
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
					numPstates= 0;
					while(s1 != null){
//						log.debug("S1: " + s1);
						if(s1.contains("current") == true ){
//							log.debug("pstate: " + s1.substring(s1.indexOf("=")+1,s1.length()));
							//numLogicalProcessors ++;
							pStatesValues[0] = Integer.valueOf(s1.substring(s1.indexOf("=")+1,s1.length()));							
						}
						else if(s1.contains("pstate") == true){
							numPstates++;
						}					
						s1 = bufferedReader.readLine();
					}
//						log.debug("numPStates: " + numPstates);
						pStatesValues[1] = numPstates;
						pStates.add(pStatesValues);
				}				

			}
			channelExec.waitFor(2, 0L);
			channelExec.close(false);			
			sshClient.stop();			
			isSshExecuted = true;
			
      
		} catch (InterruptedException interruptedexception) {
			channelExec.close(false);			
			sshClient.stop();	
			log.error("Exception",interruptedexception);
			isSshExecuted = false;
		} catch (IOException ioexception) {
			log.error("Exception",ioexception);
			channelExec.close(false);			
			sshClient.stop();
			isSshExecuted = false;
		} catch (Exception exception) {
			log.error("Exception",exception);
			channelExec.close(false);		
			sshClient.stop();			
			isSshExecuted = false;
		}
		return power;
	}
	
	
	/**
	 * 
	 * Retrieve p-states of cores form ILO with SSHD shell connection
	 * 
	 * @param pStates is a list of array with 2 values
	 * lastPstate and total pStates available
	 * @return 
	 * @throws Exception
	 *
	 * @author jclegea
	 */	
	public void getPStatesILOShell(ArrayList<int[]> pStates) throws Exception{
		PipedInputStream pipedInputStream;
		PipedOutputStream pipedOutputStream;
		BufferedReader bufferedReader;
		String s1;
		String command = null;
		SshClient client = null;
		ClientChannel channel = null;
		boolean isStopped = false;
		boolean isSshExecuted = false;
		int numCpu = 0;
		int numLogicalProcessors = 0;
		int numPstates = 0;
		int[] pStatesValues = new int[2];

		try {
			// connect to ssh
			client = SshClient.setUpDefaultClient();
			client.start();
			ClientSession session = client.connect(ipHost_, port_).await()
					.getSession();
			
			// authenticate user/password
			if (!session.authPassword(userilo_, passilo_).await().isSuccess()) {
				log.error("Authentication Failed");
				client.stop();
				isSshExecuted = false;				
			}
			else{			
				// open the shell channel
				channel = session.createChannel(ClientChannel.CHANNEL_SHELL);
				ByteArrayOutputStream sent = new ByteArrayOutputStream();
				PipedOutputStream pipedIn = new TeePipedOutputStream(sent);
				channel.setIn(new PipedInputStream(pipedIn));
				pipedInputStream = new PipedInputStream();
				pipedOutputStream = new PipedOutputStream(pipedInputStream);
				channel.setOut(pipedOutputStream);
				channel.setErr(pipedOutputStream);
				channel.open();
				bufferedReader = new BufferedReader(new InputStreamReader(pipedInputStream));

				// calculate CPUs
				command = "show /system1/ \r\n";
				pipedIn.write(command.getBytes());
				pipedIn.flush();

				numCpu = 0;
				// s1 = bufferedReader.readLine();
				isStopped = false;
				while (isStopped == false) {
					s1 = bufferedReader.readLine();
					if (s1.contains("cpu") == true) {
						numCpu++;
					} else if (s1.contains("Verbs") == true) {
						isStopped = true;
					}
				}

				// log.debug("numCPus: " + numCpu);

				// Obtain logical processors
				for (int i = 1; i <= numCpu; i++) {
					numLogicalProcessors = 0;
					command = "show /system1/cpu" + i + "\r\n";
					pipedIn.write(command.getBytes());
					pipedIn.flush();
					isStopped = false;
					while (isStopped == false) {
						s1 = bufferedReader.readLine();
						// log.debug("S1: " + s1);
						if (s1.contains("logical_processor") == true) {
							numLogicalProcessors++;
						} else if (s1.contains("Verbs") == true) {
							isStopped = true;
						}
					}
					// log.debug("logical_processors: " + numLogicalProcessors);

					// Get the Pstates values
					for (int j = 1; j <= numLogicalProcessors; j++) {
						command = "show /system1/cpu" + i + "/logical_processor" + j
								+ "\r\n";
						pipedIn.write(command.getBytes());
						pipedIn.flush();
						isStopped = false;
						numPstates = 0;
						while (isStopped == false) {
							s1 = bufferedReader.readLine();
							if (s1.contains("current") == true) {
								// pStatesValues[0] =
								// Integer.valueOf(s1.substring(s1.indexOf("=")+1,s1.length()));
								// log.debug("current p-state: " + s1.substring(s1.indexOf("=")
								// +
								// 1, s1.length()));								
								pStatesValues[0] = Integer.valueOf(s1.substring(s1.indexOf("=") + 1, s1.length()));
//								log.debug("pstate: " + pStatesValues[0]);
							} else if (s1.contains("pstate") == true) {
								numPstates++;
							} else if (s1.contains("Verbs") == true) {
								isStopped = true;
							}
						}
//						log.debug("numPstates: " + numPstates);
						pStatesValues[1] = numPstates;
						pStates.add(pStatesValues);
					}
				}

				// exit from the ssh shell
				command = "exit \r\n";
				pipedIn.write(command.getBytes());
				pipedIn.flush();

				// s1 = bufferedReader.readLine();
				isStopped = false;
				while (isStopped == false) {
					s1 = bufferedReader.readLine();
					if (s1.contains("stopped"))
						isStopped = true;
				}

				// channel.waitFor(ClientChannel.CLOSED, 0);
				channel.close(true);
				session.close(true);
				client.stop();
				isSshExecuted = true;
			}
		} catch (InterruptedException interruptedexception) {
			channel.close(true);
			client.stop();
			log.error("Exception",interruptedexception);
			isSshExecuted = false;
		} catch (IOException ioexception) {
			log.error("Exception",ioexception);
			channel.close(true);
			client.stop();
			isSshExecuted = false;
		} catch (Exception exception) {
			log.error("Exception",exception);
			channel.close(true);
			client.stop();
			isSshExecuted = false;
		}
	}
	
	/**
	 * 
	 * get the virtual machine of the host by its index and create a 
	 * virtual machine list.
	 * 
	 * @param index of the host. 
	 *
	 * @author jclegea
	 */
	public void getHostVirtualMachines(int hostIndex) throws Exception{			
		ArrayList<ManagedObjectReference> VMOn = new ArrayList<ManagedObjectReference>();
		VirtualMachineRuntimeInfo runtimeInfo = null;		
		
		
		try{
			virtualMachineList_ = (ManagedObjectReference []) extendedAppUtil_.getServiceUtil3().getDynamicProperty(hostList_.get(hostIndex), "vm");
			
			// add only the virtual machines powered on
			for(int i=0; i<virtualMachineList_.length; i++){								
				runtimeInfo = (VirtualMachineRuntimeInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[i],"runtime");				
				log.debug("VM " + getHostVirtualMachineName(i) + " Status: " + runtimeInfo.getPowerState().getValue());
				if("poweredOn".equals(runtimeInfo.getPowerState().getValue()) == true){
					VMOn.add(virtualMachineList_[i]);
				}
			}
			
			virtualMachineList_ = VMOn.toArray(new ManagedObjectReference[VMOn.size()]);
			
			if(virtualMachineList_ != null){
				log.debug("Virtual Machines from host:" + virtualMachineList_.length);
			}
		}
		catch(Exception exception){
			log.error("Cannot Retrive Virtual Machines from " + getHostName(hostIndex) + " Host or there is no virtual machines in the Host.",exception);
		}
		
	}
	
	/**
	 * 
	 * Get the size of the virtual machine list.
	 * 
	 * @return the size of the virtual machine list.
	 *
	 * @author jclegea
	 */
	public int getHostVirtualMachineSize(){
		int size = 0;
		if(virtualMachineList_ != null){
			size = virtualMachineList_.length;
		}
		return size;
	}
	
	/**
	 * 
	 * get the name of a virtual machine by its index.
	 * 
	 * @param index of the virtual machine.
	 * 
	 * @return a String with the name of the virtual machine. 
	 *
	 * @author jclegea
	 */
	public String getHostVirtualMachineName(int virtualMachineIndex){
		String virtualMachineName = null;
		String temporalName = "";
		String[] values = null;
		int i=0;
		try {
			virtualMachineName = (String)extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[virtualMachineIndex],"name");
			values = virtualMachineName.split(" ");
			// transform complex name into a simple one
			if(values != null){
				for(i=0;i<values.length;i++){
					temporalName = temporalName.concat(values[i]);
				}				
				virtualMachineName = temporalName;				
			}
			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve virtual machine name");
		}
		
		return virtualMachineName;
	}
	
	/**
	 * 
	 * get the number of CPUs  of a virtual machine by its index.
	 * 
	 * @param index of the virtual machine. 
	 * 
	 * @return an int with the number of CPUs of the virtual machine. 
	 *
	 * @author jclegea
	 */
	public int getHostVirtualMachineNumCpus(int virtualMachineIndex){		
		VirtualMachineConfigInfo virtualMachineConfig = null;
		int numCpus = 0;
		
		try {			
			virtualMachineConfig = (VirtualMachineConfigInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[virtualMachineIndex],"config");
			numCpus = virtualMachineConfig.getHardware().getNumCPU();			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve virtual machine Number of CPUs");
		}
		
		return numCpus;
	}
	
	/**
	 * 
	 * Get virtual machine storage usage in a datastore.
	 * 
	 * @param index of the virtual machine.
	 * @param index that indicates bytes o percetange values.
	 * @return percentage or bytes, -1 otherwise.
	 *
	 * @author jclegea
	 */
	public double getVirtualMachineStorageUsage(int virtualMachineIndex,int counterIndex){		
		VirtualMachineStorageInfo virtualMachineStorageUsage = null;
		VirtualMachineUsageOnDatastore[] storage = null;
		ManagedObjectReference[] datastoreList = null;
		DatastoreSummary datastoreSummary = null;
		double datastoreCapacity = -1;
		double storageTotal = -1;
		
		try {			
			virtualMachineStorageUsage = (VirtualMachineStorageInfo)extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[virtualMachineIndex],"storage");
			
			storage = virtualMachineStorageUsage.getPerDatastoreUsage();
			
			storageTotal = 0;
			for(int i=0;i<storage.length;i++){
				storageTotal = storageTotal + storage[i].getCommitted();
			}
			
			
			// Storage Usage in Percentage
			if(counterIndex == ComEniConstants.STORAGE_USAGE_PERCENTAGE){
				// Obtain data store total capacity
				datastoreList = (ManagedObjectReference [])extendedAppUtil_.getServiceUtil3().getDynamicProperty(virtualMachineList_[virtualMachineIndex],"datastore");
				datastoreCapacity = 0;
				for(int j=0;j<datastoreList.length; j++){					  
					  datastoreSummary = (DatastoreSummary)extendedAppUtil_.getServiceUtil3().getDynamicProperty(datastoreList[j],"summary");
					  datastoreCapacity = datastoreCapacity +  datastoreSummary.getCapacity();
				}
				//log.debug("Storage Total: " + storageTotal + " - datastoreCapacity: " + datastoreCapacity);
				storageTotal = (storageTotal * 100) / datastoreCapacity;
				//storageTotal = storageTotal / datastoreCapacity;
			}
			else{
				storageTotal = storageTotal / ComEniConstants.BYTES_TO_GIGABYTES;
			}
			//log.debug("Storage Total2: " + storageTotal + " - datastoreCapacity: " + datastoreCapacity);
			
		} 
		catch (Exception exception) {
			log.error("Cannot retrieve storage usage.");
		}
		
		return storageTotal;
	}
	
	/**
	 * 
	 * get performance information from virtual machines.
	 * 
	 * @param vmname, name of the virtual machine.
	 * @param type of the performance to retrieve.
	 * @param counterNumber, number of the information to retrieve.
	 * 
	 * @return value retrieved from virtual machine, -1 otherwise.
	 *
	 * @author jclegea
	 */
	public ArrayList<PerformanceInformation> getPerformance(String name, String managedObject,ArrayList<String> type,ArrayList<Integer> counterNumber, int queryInterval){
		PerfCounterInfo pcInfo = null;
		ManagedObjectReference pmRef = null;
		List vmCounters = null;
		Map counters = null;
		PerfMetricId[] aMetrics;
		ArrayList mMetrics = null;
		PerformanceInformation information;
		ArrayList<PerformanceInformation> valueRetrieved = new ArrayList<PerformanceInformation>();
		
		try{
			ManagedObjectReference managerReference = extendedAppUtil_.getServiceUtil3().getDecendentMoRef(null,managedObject,name);
			if(managerReference!=null) {				
				
				pmRef = extendedAppUtil_.getServiceConnection3().getServiceContent().getPerfManager();
				PerfCounterInfo[] cInfo	= (PerfCounterInfo[])extendedAppUtil_.getServiceUtil3().getDynamicProperty(pmRef,"perfCounter");
				
				vmCounters = new ArrayList();
				counters = new HashMap();

//				for(int i=0;i<cInfo.length; ++i){
//					//log.debug("Counter " + i + ": " + cInfo[i].getGroupInfo().getKey() + " name: " + cInfo[i].getNameInfo().getKey() + " unit: " + cInfo[i].getUnitInfo().getKey() + " rollupType: " + cInfo[i].getRollupType().getValue());					
//					log.debug("Counter " + i + ": " + cInfo[i].getGroupInfo().getKey() + " - " + cInfo[i].getRollupType().getValue() + "." + cInfo[i].getUnitInfo().getKey() + "." + cInfo[i].getNameInfo().getKey() + " stats: " + cInfo[i].getStatsType().getValue() + " summary: " + cInfo[i].getNameInfo().getSummary());					
//				}
				
//				log.debug("cInfo length: " + cInfo.length);
				String typeNext = null;
				Iterator<String> iteratorType;
				Iterator<Integer> iteratorCounterNumber;
//				log.debug("Type length: " + type.size());
				for (iteratorType = type.iterator(), iteratorCounterNumber = counterNumber.iterator();iteratorType.hasNext();){					
					vmCounters.clear();
					typeNext = iteratorType.next();					
//					log.debug("Type: " + typeNext + " vmCounters length: " + vmCounters.size());
					for(int i=0; i<cInfo.length; ++i) {
						if(typeNext.equalsIgnoreCase(cInfo[i].getGroupInfo().getKey())) {
							vmCounters.add(cInfo[i]);
						}
					}
					
//				int index=0;
//				for(Iterator it = vmCounters.iterator(); it.hasNext();) {
//					pcInfo = (PerfCounterInfo)it.next();
//					//log.debug(++index + " - "+ pcInfo.getNameInfo().getSummary());
//					log.debug(++index + "- " +  pcInfo.getRollupType().getValue() + "." + pcInfo.getUnitInfo().getKey() + "." + pcInfo.getNameInfo().getKey() + " - " + pcInfo.getStatsType().getValue() + " - " + pcInfo.getNameInfo().getSummary());  
//					//		+ "." + cInfo[i].getUnitInfo().getKey() + "." + cInfo[i].getNameInfo().getKey() + " stats: " + cInfo[i].getStatsType().getValue() + " summary: " + cInfo[i].getNameInfo().getSummary());
//				}
				
					// Obtain the counters of the correct type
					//log.debug("counterNumber: " + iteratorCounterNumber.next());
					pcInfo = (PerfCounterInfo)vmCounters.get(iteratorCounterNumber.next());
//					log.debug("pcInfo: " + pcInfo.getKey());
					counters.put(new Integer(pcInfo.getKey()), pcInfo);
//					log.debug("[Bucle] Counters length: " + counters.size());
				}
				
//				log.debug("Counters length: " + counters.size());
				
        aMetrics = extendedAppUtil_.getServiceConnection3().getService().queryAvailablePerfMetric(pmRef,managerReference,null,null,new Integer(queryInterval));
        mMetrics = new ArrayList();
//        log.debug("aMetrics length: " + aMetrics.length);
        if(aMetrics != null) {
           for(int indexMetrics=0; indexMetrics<aMetrics.length; ++indexMetrics) {
//          	 log.debug("+" + aMetrics[indexMetrics].getCounterId());
              if(counters.containsKey(new Integer(aMetrics[indexMetrics].getCounterId()))) {
                 mMetrics.add(aMetrics[indexMetrics]);
//                 log.debug("mmetrics ADD");
              }
           }
        }
        
//        log.debug("mMetrics length: " + mMetrics.size());
        
        PerfMetricId[] metricIds = (PerfMetricId[])mMetrics.toArray(new PerfMetricId[0]);
//        log.debug("metricIDs length: " + metricIds.length);
		    PerfQuerySpec qSpec = new PerfQuerySpec();
		    qSpec.setEntity(managerReference);
		    qSpec.setMaxSample(new Integer(1));
//		    qSpec.setMetricId(metricIds);
		    qSpec.setIntervalId(new Integer(queryInterval));
//		    Calendar currentTime = Calendar.getInstance();
//		    currentTime.add(Calendar.SECOND, -20);
		    //log.debug("Current Time: " + currentTime);
		    //qSpec.setStartTime(currentTime);
//		    currentTime = Calendar.getInstance();
		    //log.debug("Current Time: " + currentTime);
		    //qSpec.setEndTime(currentTime);
		    
		    
		    PerfQuerySpec[] qSpecs = new PerfQuerySpec[] {qSpec};
		    PerfEntityMetricBase[] pValues = extendedAppUtil_.getServiceConnection3().getService().queryPerf(pmRef,qSpecs);
//		    log.debug("pValues.length: " + pValues.length);		
		    if(pValues != null){
			    for(int i=0;i<pValues.length; ++i) {
		         PerfMetricSeries[] vals = ((PerfEntityMetric)pValues[i]).getValue();
//		         PerfSampleInfo[]  infos = ((PerfEntityMetric)pValues[i]).
//		         														getSampleInfo();
	//	         log.debug("Sample time range: " +
	//	                           infos[0].getTimestamp().getTime().toString() + " - " +
	//	                           infos[infos.length-1].getTimestamp().getTime().
	//	                           toString());
	//	         log.debug("Vals.length: " + vals.length);
	//	         if(vals[vals.length-1] instanceof PerfMetricIntSeries) {
	//	        	 PerfMetricIntSeries val = (PerfMetricIntSeries)vals[vals.length-1];
	//	        	 PerfCounterInfo pci 
	//           = (PerfCounterInfo)counters.get(
	//                new Integer(vals[vals.length-1].getId().getCounterId())
	//              );
	//	        	 if(pci != null)
	//	        		 log.debug(pci.getNameInfo().getSummary() + "*");
	//        
	//	           long[] longs = val.getValue();
	//	           String linePrint = null;
	//	           for(int k=0; k<longs.length; ++k) {
	//	              linePrint = longs[k] + " <--> ";
	//	           }
	//	           //log.debug(longs[longs.length-1] + "^^");
	//	           log.debug(linePrint + "^^");
	//	         }
	//	         for(int vi=0; vi<vals.length && valueRetrieved == -1; ++vi){
		         for(int vi=0; vi<vals.length; ++vi){
		            PerfCounterInfo pci = (PerfCounterInfo)counters.get(new Integer(vals[vi].getId().getCounterId()));
		            if(pci != null){		            	
//		               log.debug(pci.getNameInfo().getKey() + " - " + pci.getGroupInfo().getLabel() +  "  - " + vals[vi].getId().getInstance() + " - " + pci.getNameInfo().getSummary() + "*");
		               information = new PerformanceInformation();
		               information.setType(pci.getGroupInfo().getLabel());
		               information.setInstance(vals[vi].getId().getInstance());		               
			            if(vals[vi] instanceof PerfMetricIntSeries) {
			               PerfMetricIntSeries val = (PerfMetricIntSeries)vals[vi];			               
			               long[] longs = val.getValue();
			               information.setValue(Double.valueOf(longs[0]));
			               valueRetrieved.add(information);
			               //valueRetrieved.add(Double.valueOf(longs[0]));
			               //String linePrint = null;
//			               log.debug("longs: " + longs.length);
	//		               for(int k=0; k<longs.length; ++k) {
			                  //linePrint = longs[k] + " <--> ";
	//		              	 log.debug("longs[K]: " + longs[k]);
	//		                  valueRetrieved = longs[k];		               
	//		               }
			               //log.debug(longs[longs.length-1] + "^^");
			               //log.debug(linePrint + "^^");		               
			            }
		            }
		         }
	           //log.debug(valueRetrieved + "^^");
		      }
		    }
		    
			}
		}catch(Exception exception){
			log.error("Exception",exception);
		} 
		
		return valueRetrieved;
	}
	
	
	/**
	 * 
	 * get performance information from virtual machines.
	 * 
	 * @param vmname, name of the virtual machine.
	 * @param type of the performance to retrieve.
	 * @param counterNumber, number of the information to retrieve.
	 * 
	 * @return value retrieved from virtual machine, -1 otherwise.
	 *
	 * @author jclegea
	 */
	public ArrayList<PerformanceInformation> getPerformanceHistory(String name, String managedObject,ArrayList<String> type,ArrayList<Integer> counterNumber, Calendar sTime, Calendar eTime, int queryInterval){
		PerfCounterInfo pcInfo = null;
		ManagedObjectReference pmRef = null;
		List vmCounters = null;
		Map counters = null;
		PerfMetricId[] aMetrics;
		ArrayList mMetrics = null;
		PerformanceInformation information;
		ArrayList<PerformanceInformation> valueRetrieved = new ArrayList<PerformanceInformation>();
		
		try{
			ManagedObjectReference managerReference = extendedAppUtil_.getServiceUtil3().getDecendentMoRef(null,managedObject,name);
			if(managerReference!=null) {
				
				
				pmRef = extendedAppUtil_.getServiceConnection3().getServiceContent().getPerfManager();
				PerfCounterInfo[] cInfo	= (PerfCounterInfo[])extendedAppUtil_.getServiceUtil3().getDynamicProperty(pmRef,"perfCounter");
				
				vmCounters = new ArrayList();
				counters = new HashMap();

//				for(int i=0;i<cInfo.length; ++i){
//					//log.debug("Counter " + i + ": " + cInfo[i].getGroupInfo().getKey() + " name: " + cInfo[i].getNameInfo().getKey() + " unit: " + cInfo[i].getUnitInfo().getKey() + " rollupType: " + cInfo[i].getRollupType().getValue());					
//					log.debug("Counter " + i + ": " + cInfo[i].getGroupInfo().getKey() + " - " + cInfo[i].getRollupType().getValue() + "." + cInfo[i].getUnitInfo().getKey() + "." + cInfo[i].getNameInfo().getKey() + " stats: " + cInfo[i].getStatsType().getValue() + " summary: " + cInfo[i].getNameInfo().getSummary());					
//				}
				
//				log.debug("cInfo length: " + cInfo.length);
				String typeNext = null;
				Iterator<String> iteratorType;
				Iterator<Integer> iteratorCounterNumber;
//				log.debug("Type length: " + type.size());
				for (iteratorType = type.iterator(), iteratorCounterNumber = counterNumber.iterator();iteratorType.hasNext();){					
					vmCounters.clear();
					typeNext = iteratorType.next();					
//					log.debug("Type: " + typeNext + " vmCounters length: " + vmCounters.size());
					for(int i=0; i<cInfo.length; ++i) {
						if(typeNext.equalsIgnoreCase(cInfo[i].getGroupInfo().getKey())) {
							vmCounters.add(cInfo[i]);
						}
					}
					
//				int index=0;
//				for(Iterator it = vmCounters.iterator(); it.hasNext();) {
//					pcInfo = (PerfCounterInfo)it.next();
//					//log.debug(++index + " - "+ pcInfo.getNameInfo().getSummary());
//					log.debug(++index + "- " +  pcInfo.getRollupType().getValue() + "." + pcInfo.getUnitInfo().getKey() + "." + pcInfo.getNameInfo().getKey() + " - " + pcInfo.getStatsType().getValue() + " - " + pcInfo.getNameInfo().getSummary());  
//					//		+ "." + cInfo[i].getUnitInfo().getKey() + "." + cInfo[i].getNameInfo().getKey() + " stats: " + cInfo[i].getStatsType().getValue() + " summary: " + cInfo[i].getNameInfo().getSummary());
//				}
				
					// Obtain the counters of the correct type
					//log.debug("counterNumber: " + iteratorCounterNumber.next());
					pcInfo = (PerfCounterInfo)vmCounters.get(iteratorCounterNumber.next());
//					log.debug("pcInfo: " + pcInfo.getKey());
					counters.put(new Integer(pcInfo.getKey()), pcInfo);
//					log.debug("[Bucle] Counters length: " + counters.size());
				}
				
//				log.debug("Counters length: " + counters.size());
				
        aMetrics = extendedAppUtil_.getServiceConnection3().getService().queryAvailablePerfMetric(pmRef,managerReference,sTime,eTime,new Integer(queryInterval));
        mMetrics = new ArrayList();
//        log.debug("aMetrics length: " + aMetrics.length);
        if(aMetrics != null) {
           for(int indexMetrics=0; indexMetrics<aMetrics.length; ++indexMetrics) {
//          	 log.debug("+" + aMetrics[indexMetrics].getCounterId());
              if(counters.containsKey(new Integer(aMetrics[indexMetrics].getCounterId()))) {
                 mMetrics.add(aMetrics[indexMetrics]);
//                 log.debug("mmetrics ADD");
              }
           }
        }
        
//        log.debug("mMetrics length: " + mMetrics.size());
        
        PerfMetricId[] metricIds = (PerfMetricId[])mMetrics.toArray(new PerfMetricId[0]);
//        log.debug("metricIDs length: " + metricIds.length);
		    PerfQuerySpec qSpec = new PerfQuerySpec();
		    qSpec.setEntity(managerReference);
		    qSpec.setMaxSample(new Integer(100));
		    qSpec.setStartTime(sTime);
		    qSpec.setEndTime(eTime);
//		    qSpec.setMetricId(metricIds);
		    qSpec.setIntervalId(new Integer(queryInterval));
		    
//		    Calendar currentTime = Calendar.getInstance();
//		    currentTime.add(Calendar.SECOND, -20);
		    //log.debug("Current Time: " + currentTime);		    
//		    currentTime = Calendar.getInstance();
		    //log.debug("Current Time: " + currentTime);
		    
		    
		    
		    PerfQuerySpec[] qSpecs = new PerfQuerySpec[] {qSpec};
		    PerfEntityMetricBase[] pValues = extendedAppUtil_.getServiceConnection3().getService().queryPerf(pmRef,qSpecs);
//		    log.debug("pValues.length: " + pValues.length);		
		    if(pValues != null){
			    for(int i=0;i<pValues.length; ++i) {
		         PerfMetricSeries[] vals = ((PerfEntityMetric)pValues[i]).getValue();
		         for(int vi=0; vi<vals.length; ++vi){
		            PerfCounterInfo pci = (PerfCounterInfo)counters.get(new Integer(vals[vi].getId().getCounterId()));
		            if(pci != null){		            	
//		               log.debug(pci.getNameInfo().getKey() + " - " + pci.getGroupInfo().getLabel() +  "  - " + vals[vi].getId().getInstance() + " - " + pci.getNameInfo().getSummary() + "*");
		               		               
			            if(vals[vi] instanceof PerfMetricIntSeries) {
			               PerfMetricIntSeries val = (PerfMetricIntSeries)vals[vi];			               
			               long[] longs = val.getValue();			               
			               
			               for(int k=0; k<longs.length; ++k) {
			              	 information = new PerformanceInformation();
				               information.setType(pci.getGroupInfo().getLabel());
				               information.setInstance(vals[vi].getId().getInstance());
				               information.setValue(Double.valueOf(longs[k]));
				               valueRetrieved.add(information);
			                  //linePrint = longs[k] + " <--> ";
//			              	 log.debug("longs[K]: " + longs[k]);
//			                  valueRetrieved = longs[k];		               
			               }
			               
			            }
		            }
		         }
	           //log.debug(valueRetrieved + "^^");
		      }
		    }
		    
			}
		}catch(Exception exception){
			log.error("Exception",exception);
		} 
		
		return valueRetrieved;
	}
	


	
	/**
	 * 
	 * connect to vSphere Web Service.
	 * 
	 * @return true if connection is ok, false otherwise.
	 *
	 * @author jclegea
	 */
	public boolean connect(){
		boolean isConnected = false;		
		try {		
				extendedAppUtil_ = ExtendedAppUtil.initialize(retrieveAction_, actionArguments_);
				extendedAppUtil_.connect();				
				isConnected = true;			
				log.debug("RetrieveAction: " + retrieveAction_ + "actionArguments_[1]: " + actionArguments_[1]);
//				log.debug("connection data - url: " + extendedAppUtil_.getServiceUrl() +
//						" user: " + extendedAppUtil_.getUsername() +
//						" password: " + extendedAppUtil_.getPassword());
		} 
		catch (Exception exception) {
				log.error("Cannot connect to vSphere Web Service");
				isConnected = false;
		}		
		return isConnected;
	}
	
	/**
	 * 
	 * disconnect to vSphere Web Service.
	 * 
	 * @return true if disconnection is ok, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean disconnect(){
		try {			
			extendedAppUtil_.disConnect();
			return true;
		} 
		catch (Exception exception) {
			log.error("Cannot disconnect to vSphere Web Service");
			return false;
		}
	}
	
	
	/**
	 * 
	 * entry point to retrieve hosts from ENI's Servers.
	 * 
	 * @return true if hosts are retrieved correctly, false otherwise.
	 *
	 * @author jclegea
	 */
	public boolean startRetrieveInformation() {
		boolean isRetrieved=false;		
		try {			  
				isRetrieved = retrieveHosts();								
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
		return isRetrieved;
	}

}
