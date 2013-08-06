/**
 * ============================== Header ============================== 
 * file:          ComFzj.java
 * project:       FIT4Green/CommunicatorFzj
 * created:       Nov 25, 2010 by Daniel Brinkers
 * 
 * $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
 * $LastChangedBy: f4g.julichde $
 * $LastChangedRevision: 1244 $
 * 
 * short description:
 *   Implementation of the ICom interface for the HPC scenario at the FZJ
 * 
 * @author Daniel Brinkers
 * @see f4gcom.ICom
 * ============================= /Header ==============================
 */
package f4g.communicatorFzj.com.pbs;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import f4g.commons.com.ICom;
import f4g.communicatorFzj.com.pbs.common.JobInfo;
import f4g.communicatorFzj.com.pbs.common.JobInfo.State;
import f4g.communicatorFzj.com.pbs.common.NodeInfo;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestAlterJob;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestNodeToStandby;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestNodeWakeUp;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestRunJob;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestSimpleNodeCmd;
import f4g.communicatorFzj.com.pbs.common.ProxyRequestUpdate;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseAlterJob;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseNodeToStandby;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseNodeWakeUp;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseSimpleNodeCmd;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseStartJob;
import f4g.communicatorFzj.com.pbs.common.ProxyResponseUpdate;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;
import f4g.commons.power.PowerCalculator;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.StandByActionType;
import f4g.schemas.java.actions.StartJobActionType;
import f4g.schemas.java.metamodel.CPUType;
import f4g.schemas.java.metamodel.CoreLoadType;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkStatusType;
import f4g.schemas.java.metamodel.FrequencyType;
import f4g.schemas.java.metamodel.IDREFS;
import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.JobPriorityType;
import f4g.schemas.java.metamodel.JobPropOfNodesType;
import f4g.schemas.java.metamodel.JobStatusType;
import f4g.schemas.java.metamodel.JobTimeType;
import f4g.schemas.java.metamodel.JobType;
import f4g.schemas.java.metamodel.MemoryUsageType;
import f4g.schemas.java.metamodel.NodeStatusType;
import f4g.schemas.java.metamodel.NrOfCoresType;
import f4g.schemas.java.metamodel.NrOfNodesType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.RPMType;
import f4g.schemas.java.metamodel.RackableServerType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VoltageType;

/**
 * Implementation of the ICom interface for the Torque /PBS adaption scenario at 
 * FZJ FIT4Green supercomputing testbed
 * 
 * @see f4gcom.ICom
 * 
 * @author Daniel Brinkers, Andre Giesler
 */
public class ComPBS implements ICom, Runnable {

	static Logger log = Logger.getLogger(ComPBS.class.getName());

	int error_counter = 0;
	int update_counter = 0;
	int finished_job_counter = 0;
	double total_power = 0.0;
	double min_power = 0.0;
	double max_power = 0.0;
	long total_queued_time = 0;
	long total_runtime = 0;
	long benchmark_starttime = 0;

	static boolean block_update = false;

	public static final String COM_PROPERTIES_DIR = "config/";
	public static final String COM_PROPERTIES_SUFFIX = ".properties";
	String CLIENT_KEYSTORE_PATH;
	String CLIENT_KEYSTORE_PASS;
	String PROXY_HOST;
	int PROXY_PORT;
	String RMS_HEAD_NODE;	
	String POWER_CONSUMPTION_CMD;
	String[] CPU_USAGE_CMD;
	String[] CORE_FREQS_CMD;
	String [] CORE_VOLTAGE_CMD;
	String[] MEM_USAGE_CMD;
	String[] FAN_USAGE_CMD;
	String[] IO_STATS_CMD;
	String RUN_JOB_CMD;
	String ALTER_JOB_CMD;
	String WAKEUP_NODE_CMD;
	String SLEEP_NODE_CMD;
	String DEEP_SLEEP_NODE_CMD;
	String CMD_DIR;
	String IPMI_CORE_VOLTAGE_ID;
	String MPSTAT_IDLE_COL;
	boolean HYPERTHREADING;
	int HYPERTHREADED_CORES;
	int UPDATE_INTERVAL;
	boolean RECORDING = false;
	boolean ACTIONS_OFF;
	boolean SYSTEM_MON;
	boolean BENCHMARK_MODE = false;
	boolean STOP_COM_AFTER_BENCHMARK = false;
	boolean IS_PROXY_CONNECTED = false;
	boolean USE_ACPI_STANDBY = false;
	boolean USE_POWERSAVE_GOV = false;
	boolean USE_RMS_SCHEDULER = false;
	String START_RMS_SCHEDULER_CMD;
	String STOP_RMS_SCHEDULER_CMD;
	String POWERSAVE_GOV_CMD;
	String PERFORMANCE_GOV_CMD;
	boolean MAIL_REPORT_ON;
	String MAIL_REPORT_SMTP;
	String MAIL_REPORT_RECEPIENTS;


	private Map<String, String> macMap_ = new HashMap<String, String>();
	private Map<String, JobInfo> jobInfoMap_ = new HashMap<String, JobInfo>();
	private Map<String, NodeInfo> nodeInfoMap_ = new HashMap<String, NodeInfo>();
	private Map<String, Double> powerPerNodeMap = new HashMap<String, Double>();
	private Map<String, double[]> coreUsagePerNodeMap = new HashMap<String, double[]>();
	private Map<String, double[]> coreFrequencyPerNodeMap = new HashMap<String, double[]>();
	private Map<String, double[]> coreVoltagePerNodeMap = new HashMap<String, double[]>();
	private Map<String, Double> memoryUsagePerNodeMap = new HashMap<String, Double>();
	private Map<String, int[]> fanActualRPMPerNodeMap = new HashMap<String, int[]>();
	private Map<String, Double> storageUnitReadRatePerNodeMap = new HashMap<String, Double>();
	private Map<String, Double> storageUnitWriteRatePerNodeMap = new HashMap<String, Double>();

	private Set<String> usedNodes = new HashSet<String>();

	private Map<String, ICom> monitoredObjects_;
	private Map<String, ConcurrentLinkedQueue<ComOperationCollector>> queuesHashMap_;
	private Thread thread_ = new Thread(this);
	private IMonitor monitor_ = null;
	private String name_ = null;
	private Map<String, JobInfo> changedJobs_;
	private Map<String, JobInfo> deletedJobs_;
	private Map<String, JobInfo> newJobs_;
	private String RmKey_;

	/**
	 * Connects to proxy on cluster to get the latest job and node updates. Maintains node and jobs maps
	 * to cache the latest updates. Decides if an optimization task needs to be performed.
	 * 
	 *
	 * @author Daniel Brinkers, Andre Giesler
	 */
	private void getUpdate() {		
		try {
			int i;
			boolean isNewSituation = false;
			Map<String, JobInfo> jobInfoMapNew;
			Map<String, NodeInfo> nodeInfoMapNew;

			ProxyRequestUpdate proxyRequestUpdate;
			proxyRequestUpdate = new ProxyRequestUpdate(CMD_DIR, POWER_CONSUMPTION_CMD, 
					CPU_USAGE_CMD, CORE_FREQS_CMD, CORE_VOLTAGE_CMD, MEM_USAGE_CMD, 
					FAN_USAGE_CMD, IO_STATS_CMD, SYSTEM_MON);
			proxyRequestUpdate.setIpmi_core_voltage_id(IPMI_CORE_VOLTAGE_ID);
			proxyRequestUpdate.setMpstat_idle_col(MPSTAT_IDLE_COL);
			ProxyResponseUpdate proxyResponseUpdate;

			if(IS_PROXY_CONNECTED){
				log.info("Creating connection to proxy...");
				ProxyConnection proxyConnection;
				proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
				log.debug("Connected to proxy");
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());			
				objectOutputStream.writeObject(proxyRequestUpdate);
				objectOutputStream.flush();
				proxyResponseUpdate = (ProxyResponseUpdate) objectInputStream.readObject();
				proxyConnection.close();
				log.debug("Closed connection to proxy");
			}
			else{
				proxyResponseUpdate = (ProxyResponseUpdate) proxyRequestUpdate.execute();
			}		

			nodeInfoMapNew = new HashMap<String, NodeInfo>();
			jobInfoMapNew = new HashMap<String, JobInfo>();

			for(i = 0; i < proxyResponseUpdate.getNodeInfo().length; ++i){
				nodeInfoMapNew.put(proxyResponseUpdate.getNodeInfo()[i].getId(), proxyResponseUpdate.getNodeInfo()[i]);
				log.debug("Detected node " + proxyResponseUpdate.getNodeInfo()[i].getId() + " from current update");
				getCoreUsagePerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getCoreUsages());
				getCoreFrequencyPerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getCoreFrequencies());
				getCoreVoltagePerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getCoreVoltage());
				getMemoryUsagePerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getMemoryUsage());
				getFanActualRPMPerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getFanActualRPMs());
				getStorageUnitReadRatePerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getStorageUnitReadRate());
				getStorageUnitWriteRatePerNodeMap().put(proxyResponseUpdate.getNodeInfo()[i].getId(),proxyResponseUpdate.getNodeInfo()[i].getStorageUnitWriteRate());
			}

			for(i = 0; i < proxyResponseUpdate.getJobInfo().length; ++i){
				String id = proxyResponseUpdate.getJobInfo()[i].getId();
				proxyResponseUpdate.getJobInfo()[i].setId("_" + id);				
				jobInfoMapNew.put(proxyResponseUpdate.getJobInfo()[i].getId(), proxyResponseUpdate.getJobInfo()[i]);
				log.debug("Detected job " + proxyResponseUpdate.getJobInfo()[i].getId() + " from current update");				
			}

			//Removing deleted jobs from JobInfoMap
			if(getJobInfoMap()!=null || !getJobInfoMap().isEmpty()){				
				Iterator<String> jobInfoSetItr = getJobInfoMap().keySet().iterator(); 
				while( jobInfoSetItr.hasNext()) {
					String jobInfoKey = jobInfoSetItr.next();
					Iterator<String> deletedJobsSetItr = getDeletedJobs().keySet().iterator(); 
					while( deletedJobsSetItr.hasNext()) {
						String delJobKey = deletedJobsSetItr.next();
						if(jobInfoKey.equals(delJobKey)){
							jobInfoSetItr.remove();
							deletedJobsSetItr.remove();
							isNewSituation = true;
							log.info("Removed deleted job " + jobInfoKey + " from JobInfoMap");
							break;
						}
					}
				}
			}

			//Clearing temporary maps
			setChangedJobs(new HashMap<String, JobInfo>());
			setNewJobs(new HashMap<String, JobInfo>());
			setDeletedJobs(new HashMap<String, JobInfo>());

			//Checking status of known jobs
			log.info("Checking status of jobs");
			Iterator<Entry<String, JobInfo> > jobInfoEntryIterator = getJobInfoMap().entrySet().iterator();
			while(jobInfoEntryIterator.hasNext()){
				Entry<String, JobInfo> jobInfoEntry = jobInfoEntryIterator.next();
				String key = jobInfoEntry.getKey();
				JobInfo jobInfo = jobInfoEntry.getValue();
				if(jobInfoMapNew.containsKey(key)){
					isNewSituation = checkJobUpdated(jobInfo, jobInfoMapNew.get(key)) || isNewSituation;
				}else{
					log.info("Job id " + key + " seems to be deleted. Put in DeletedJobsMap.");
					getDeletedJobs().put(jobInfo.getId(), jobInfo);
					isNewSituation = true;
					if(BENCHMARK_MODE){
						total_queued_time += jobInfo.getQueuedtime_();
						total_runtime += jobInfo.getRuntime_();
						finished_job_counter++;
					}
				}
			}

			//Adding new jobs
			Iterator<Entry<String, JobInfo> > jobInfoNewEntryIterator = jobInfoMapNew.entrySet().iterator();
			while(jobInfoNewEntryIterator.hasNext()){
				Entry<String, JobInfo> jobInfoEntry = jobInfoNewEntryIterator.next();
				String key = jobInfoEntry.getKey();
				JobInfo jobInfo = jobInfoEntry.getValue();
				if(!getJobInfoMap().containsKey(key)){
					log.debug("Adding new job id " + key + " to NewJobsMap");
					getNewJobs().put(jobInfo.getId(), jobInfo);
					isNewSituation = true;
				}
			}

			if(isNewSituation)
				getMonitor().updateNode(getRmKey(), this);

			adjustNodeState(nodeInfoMapNew);

			//cache power comsumption
			double total_update_power = 0.0;
			String power_consumption_str = proxyResponseUpdate.getPower_Consumption();	
			try{
				Scanner sc = new Scanner(power_consumption_str).useDelimiter(";");
				while (sc.hasNext()) {
					String nodePower = sc.next();
					Scanner sc_node = new Scanner(nodePower).useDelimiter("=");
					String s = sc_node.next();
					String power_watt = sc_node.next().trim();	
					log.info("Power consumption of node key " + s + " is: " + power_watt);	

					Double power = Double.valueOf(power_watt);
					total_update_power += power.doubleValue();
					getPowerPerNodeMap().put(s, power);
				}
			}
			catch(Exception ex){
				log.error(ex.getMessage());
				log.error("Couldn't get current power consumption");
			}
			total_power += total_update_power;

			//retrieveStatesFromModel();

			Iterator<Entry<String, NodeInfo> > nodeInfoEntryIterator = getNodeInfoMap().entrySet().iterator();
			while(nodeInfoEntryIterator.hasNext()){
				Entry<String, NodeInfo> nodeInfoEntry = nodeInfoEntryIterator.next();
				String key = nodeInfoEntry.getKey();
				NodeInfo nodeInfo = nodeInfoEntry.getValue();

				//Update cycling dynamic parameters
				getMonitor().updateNode(getName() + "_" + nodeInfo.getId(), this);

				//Update Node's state
				isNewSituation = updateNode(host2Key(key), nodeInfo, nodeInfoMapNew.get(key)) || isNewSituation;		
			}	

			if(BENCHMARK_MODE){
				long currentTime = System.currentTimeMillis();
				if(getJobInfoMap()!=null || ! getJobInfoMap().entrySet().isEmpty()){	
					Iterator<Entry<String, JobInfo> > jobInfoQueuedRunIterator = getJobInfoMap().entrySet().iterator();
					while( jobInfoQueuedRunIterator.hasNext()) {
						Entry<String, JobInfo> jobInfoEntry = jobInfoQueuedRunIterator.next();						
						String jobInfoKey = jobInfoEntry.getKey();
						JobInfo jobInfo = jobInfoEntry.getValue();						
						if(jobInfo.getState().equals(JobInfo.State.QUEUED)){
							jobInfo.setQueuedtime_((currentTime - (jobInfo.getSubmitTime()*1000))/1000);
						}
						else{
							jobInfo.setRuntime_((currentTime - (jobInfo.getStartTime()*1000))/1000);							
						}
						log.info("Job " +  jobInfoKey + ": queuedtime[s]: " 
								+ jobInfo.getQueuedtime_()+  " and runtime[s]: " + jobInfo.getRuntime_());
					}
				}

				Iterator<Entry<String, NodeInfo> > nodeInfoEntryDownUpIterator = getNodeInfoMap().entrySet().iterator();
				long uptime = 0;
				long downtime = 0;
				long powersaveGovTime = 0;
				long performanceGovTime = 0;
				while(nodeInfoEntryDownUpIterator.hasNext()){
					Entry<String, NodeInfo> nodeInfoEntry = nodeInfoEntryDownUpIterator.next();
					String key = nodeInfoEntry.getKey();
					NodeInfo nodeInfo = nodeInfoEntry.getValue();
					if(nodeInfo.getState().equals(NodeInfo.State.STANDBY)){
						nodeInfo.setDownTime(nodeInfo.getDownTime() + (currentTime - nodeInfo.getStateSwitchTime()));
					}
					else{
						nodeInfo.setUpTime(nodeInfo.getUpTime() + (currentTime - nodeInfo.getStateSwitchTime()));
					}
					uptime += nodeInfo.getUpTime();
					downtime += nodeInfo.getDownTime();
					log.info("Node " +  key + ": downtime[s]: " + nodeInfo.getDownTime()/1000+  " and uptime[s]: " + nodeInfo.getUpTime()/1000);

					if(nodeInfo.getGovernor().equals(NodeInfo.Governor.POWERSAVE)){
						nodeInfo.setPowersaveTime(nodeInfo.getPowersaveTime() + (currentTime - nodeInfo.getStateSwitchTime()));
					}
					else{
						nodeInfo.setPerformanceTime(nodeInfo.getPerformanceTime() + (currentTime - nodeInfo.getStateSwitchTime()));
					}
					performanceGovTime += nodeInfo.getPerformanceTime();
					powersaveGovTime += nodeInfo.getPowersaveTime();
					log.info("Node " +  key + ": powersaveGov[s]: " + nodeInfo.getPowersaveTime()/1000+  " and performanceGov[s]: " + nodeInfo.getPerformanceTime()/1000);
					nodeInfo.setStateSwitchTime(currentTime);
				}
				StringBuilder benchmarks = new StringBuilder(); 
				benchmarks.append("Benchmarks:\n");
				benchmarks.append("After " +  finished_job_counter + " finished jobs of framework " + getName() + ": queued time[s]: " + total_queued_time +  " and runtime[s]: " + total_runtime + "\n");
				long total_jobs_time = total_queued_time + total_runtime;
				double total_jobs_time_db = total_jobs_time;
				double queued_time_db = total_queued_time;
				double percent_queued = 0.0;
				if(total_jobs_time_db>0){
					percent_queued = 100 * queued_time_db / total_jobs_time_db;
				}
				StringBuilder sb = new StringBuilder();
				Formatter formatter = new Formatter( sb );
				formatter.format( "%.2f", percent_queued );
				benchmarks.append("Jobs queued time in percent[%]: " + formatter + "\n");
				benchmarks.append("All nodes of framework " + getName() + ": downtime[s]: " + downtime/1000 +  " and uptime[s]: " + uptime/1000 + "\n");
				benchmarks.append("All nodes of framework " + getName() + ": powersaveGov[s]: " + powersaveGovTime/1000 +  " and performanceGov[s]: " + performanceGovTime/1000 + "\n");
				long total = uptime + downtime;
				double total_db = total;
				double downtime_db = downtime;
				double percent_down = 100 * downtime_db / total_db;
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", percent_down );
				benchmarks.append("Downtime in percent[%]: " + formatter + "\n");
				total = powersaveGovTime + performanceGovTime;
				total_db = total;
				downtime_db = powersaveGovTime;
				percent_down = 100 * downtime_db / total_db;
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", percent_down );
				benchmarks.append("Time in powersave governor in percent[%]: " + formatter + "\n");				
				update_counter++;								
				benchmarks.append("Update counter: " + update_counter + "\n");
				long elapsed_time = (currentTime - benchmark_starttime)/1000;
				double currentTime_db = currentTime;
				double benchmark_starttime_db = benchmark_starttime;
				double elapsed_time_db = (currentTime_db - benchmark_starttime_db)/1000;
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", elapsed_time_db );
				benchmarks.append("Elapsed time[s]: " + formatter + "\n");
				benchmarks.append("Current total power[Watt]: " + total_update_power + "\n");
				double avg_power = total_power / update_counter;
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", avg_power );
				benchmarks.append("Average Power[Watt]: " + formatter + "\n");
				if(min_power==0.0){
					min_power = total_update_power;
				}
				if(max_power==0){
					max_power = total_update_power;
				}
				if(total_update_power<min_power){
					min_power = total_update_power;
				}
				if(total_update_power>max_power){
					max_power = total_update_power;
				}
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", min_power );			
				benchmarks.append("Min Power[Watt]: " + formatter + "\n");
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", max_power );
				benchmarks.append("Max Power[Watt]: " + formatter + "\n");

				double work = avg_power * elapsed_time / 1000;
				sb = new StringBuilder();
				formatter = new Formatter( sb );
				formatter.format( "%.2f", work );
				benchmarks.append("Consumed energy[kJ]: " + formatter + "\n");
				log.info(benchmarks.toString());

				if(jobInfoMap_.isEmpty()){
					log.info("No jobs in queue ...");
					if(STOP_COM_AFTER_BENCHMARK)
						error_counter+=3;
				}
			}

			log.info("Update completed");	

			if (isNewSituation && !ACTIONS_OFF) {
				boolean runGloablOpt = true;
				if(USE_RMS_SCHEDULER){
					boolean stopRMSScheduler = stopRMSScheduling();
					if(!stopRMSScheduler)
						runGloablOpt = false;
				}
				if(runGloablOpt){
					log.info("Call Global Optimization...");
					getMonitor().requestGlobalOptimization();
				}
				else{
					log.info("Cannot call Global Optimization, since default RMS scheduler cannot be stopped");
				}

				//Some seconds time for optimization task
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
			}

			if(RECORDING){				
				JuggleRecorder recorder = new JuggleRecorder(getName());
				getRmKey();
				FIT4GreenType modelCopyUpdated = getMonitor().getModelCopy();
				PowerCalculator powerCalculator = new PowerCalculator();
				powerCalculator.computePowerFIT4Green(modelCopyUpdated);
				recorder.recordModel(modelCopyUpdated);				
			}


		} catch (IOException e) {
			e.printStackTrace();
			error_counter++;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			error_counter++;
		}

		if(error_counter > 10){
			log.error("Got more than 10 failed update cycles. Stopping com... ");	
			stopUpdate();
		}

	}

	//	private String key2Host(String key){
	//		return key.substring(getName().length() + 1);
	//	}

	private String host2Key(String host){
		if(host.startsWith(getName())){
			return host;
		}
		else{
			return getName() + "_" + host;
		}

	}

	/**
	 * Gets the node states from the model and puts them in a map
	 * 
	 */
	@SuppressWarnings("unused")
	private void retrieveStatesFromModel(){
		//Avoid Reserved state
		String myQuery = "//rackableServer";
		JXPathContext context = JXPathContext.newContext(getMonitor().getModelCopy());
		Iterator<?> elements = context.iterate(myQuery);

		// Iteration over the "rackableServer" items		
		while (elements.hasNext())
		{       	
			Object element = elements.next();

			// The node is a worker node
			if(((ServerType)element).getName().toString().matches("HPC_COMPUTE_NODE"))
			{		
				String id = ((ServerType)element).getFrameworkID();
				NodeStatusType nodeStatusType = ((ServerType)element).getNativeOperatingSystem().getNode().get(0).getStatus();
				NodeInfo.State state = NodeInfo.State.RESERVED;
				switch(nodeStatusType){
				case BUSY:
					state = NodeInfo.State.BUSY;
					break;
				case DOWN:
					state = NodeInfo.State.STANDBY;
					break;
				case IDLE:
					state = NodeInfo.State.IDLE;
					break;
				case OFF:
					state = NodeInfo.State.OFF;
					break;
				case RUNNING:
					state = NodeInfo.State.RUNNING;
					break;
				case STANDBY:
					state = NodeInfo.State.STANDBY;
					break;
				}	
				//getStatesFromModel().put(id, state);
			}
		}
	}


	/**
	 * Updates node status of local cache maps and of monitorer.
	 * 
	 * @param key - The name of the node 
	 * @param nodeInfo - nodeInfo object of the actual map
	 * @param nodeInfoNew - nodeInfo object of the new map
	 * @return true - If a new situation is given by a node update which 
	 * requires an optimzation task.
	 *
	 * @author Daniel Brinkers, Andre Giesler
	 */
	private boolean updateNode(String key, NodeInfo nodeInfo, NodeInfo nodeInfoNew) {

		boolean changed = false;
		ComOperationCollector operation = new ComOperationCollector();

		//Status has changed
		log.debug("Status of node " + key + " before node update " + nodeInfo.getState());
		log.debug("Status of node " + key + " got from node update " + nodeInfoNew.getState());
		if(nodeInfo.getState() != nodeInfoNew.getState()){
			changed = true;	
			NodeInfo.State current_state = nodeInfo.getState();
			nodeInfo.setState(nodeInfoNew.getState());				
			NodeStatusType type = NodeStatusType.RESERVED;
			ServerStatusType stype = ServerStatusType.ON;
			switch(nodeInfo.getState()){
			case BUSY:
				type = NodeStatusType.BUSY;
				stype = ServerStatusType.ON;
				break;
			case DOWN:
				if(!current_state.equals(NodeInfo.State.OFF)){
					type = NodeStatusType.STANDBY;
					stype = ServerStatusType.STANDBY;
				}									
				break;
			case IDLE:
				type = NodeStatusType.IDLE;
				stype = ServerStatusType.ON;
				break;
			case OFF:
				type = NodeStatusType.OFF;
				stype = ServerStatusType.OFF;
				break;
			case RESERVED:
				log.debug("Set node to RESERVED");
				stype = ServerStatusType.ON;
				break;
			case RUNNING:
				type = NodeStatusType.RUNNING;
				stype = ServerStatusType.ON;
				break;
			case STANDBY:
				type = NodeStatusType.STANDBY;
				stype = ServerStatusType.STANDBY;
				break;
			case HYBERNATED:
				type = NodeStatusType.HYBERNATED;
				stype = ServerStatusType.STANDBY;
				break;
			}			
			operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", type));
			operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", stype));		
		}
		else{			
			if(getNodeInfoMap().get(nodeInfoNew.getId()).getState().equals(NodeInfo.State.IDLE)
					&& !getUsedNodes().contains(nodeInfoNew.getId())){				
				operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", NodeStatusType.IDLE));
				operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", ServerStatusType.ON));
				operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/coresInUse", String.valueOf(0)));
				if(USE_POWERSAVE_GOV && !USE_ACPI_STANDBY && nodeInfo.getGovernor().equals(NodeInfo.Governor.POWERSAVE) ){
					changed = false;
				}
				else{
					changed = true;
				}
			}
			//			if(getNodeInfoMap().get(nodeInfoNew.getId()).getState().equals(NodeInfo.State.STANDBY)){
			//				operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", NodeStatusType.STANDBY));
			//				operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", ServerStatusType.STANDBY));
			//			}

		}

		//force change if nodes are down and job queue is not empty
		//		if(getNodeInfoMap().get(nodeInfoNew.getId()).getState().equals(NodeInfo.State.STANDBY)
		//				&& !getJobInfoMap().isEmpty()){
		//			changed = true;				
		//		}


		//Number of used cores changed
		if(nodeInfo.getNumOfUsedCores()!=nodeInfoNew.getNumOfUsedCores()){
			nodeInfo.setNumOfUsedCores(nodeInfoNew.getNumOfUsedCores());
			operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/coresInUse", String.valueOf(nodeInfo.getNumOfUsedCores())));
		}

		//JobRef changed
		if(nodeInfo.getJobRefs()!=null && nodeInfoNew.getJobRefs()!=null && !nodeInfo.getJobRefs().equals(nodeInfoNew.getJobRefs())){
			nodeInfo.getJobRefs().clear();
			Iterator<String> newJobs = nodeInfoNew.getJobRefs().iterator();
			while(newJobs.hasNext()){
				String jobRef = newJobs.next();
				nodeInfo.getJobRefs().add(jobRef);
				log.debug("Add jobRef " + jobRef + " to Node " + nodeInfo.getId());
			}
			//nodeInfo.setJobRefs(nodeInfoNew.getJobRefs());		
			getMonitor().updateNode(getName() + "_" + nodeInfo.getId(), this);
			log.debug("Current JobRef of " + key + " is: " + nodeInfo.getJobRefs().toString());
		}

		if(operation.getOperations().size()>0)
			getMonitor().simpleUpdateNode(host2Key(key), operation);
		log.info("Status of node " + key + " after node update: " + nodeInfo.getState() + " : Nr of used cores: " + nodeInfo.getNumOfUsedCores());
		return changed;
	}

	/**
	 * 
	 * Checks if the state of a job has been changed
	 * 
	 * @param jobInfo - Job of the current map
	 * @param jobInfoNew - Job of the new map
	 * @return true - If a new situation is given by a job update which 
	 * requires an optimization task.
	 */
	private boolean checkJobUpdated(JobInfo jobInfo, JobInfo jobInfoNew){
		boolean changed = false;

		changed = (jobInfo.getState() != jobInfoNew.getState()) || changed;
		if(changed){
			log.info("job status of " + jobInfo.getId() + " changed from " + 
					jobInfo.getState() + " to " + jobInfoNew.getState());
		}		

		//should be changed just once
		changed = (jobInfo.getnNodes() != jobInfoNew.getnNodes()) || changed;
		changed = (jobInfo.getStartTime() != jobInfoNew.getStartTime()) || changed;

		if(changed){
			getChangedJobs().put(jobInfoNew.getId(), jobInfoNew);
		}			
		return changed;
	}

	@Override
	public void run() {	
		while (true) {
			getUpdate();
			if(error_counter > 10){
				log.error("Got more than 10 failed update cycles. Stopping com... ");	
				stopUpdate();
				break;
			}
			try {
				Thread.sleep(UPDATE_INTERVAL * 1000);
				while (block_update) {
					log.info("Com " + getName() + " still busy with actions. Wait some seconds");
					Thread.sleep(2 * 1000);					
				}
			} catch (InterruptedException e) {
				getMonitor().setFrameworkStatus(getName(), FrameworkStatusType.STOPPED);
				error_counter++;
				e.printStackTrace();				
			}
		}
	}

	/**
	 * @param macMap
	 *            the macMap to set
	 */
	public void setMacMap(Map<String, String> macMap) {
		this.macMap_ = macMap;
	}

	/**
	 * @return the macMap
	 */
	public Map<String, String> getMacMap() {
		return macMap_;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean executeActionList(@SuppressWarnings("rawtypes") ArrayList arg0) {
		@SuppressWarnings("rawtypes")
		Iterator iterator = arg0.iterator();
		block_update = true;
		log.info("Com " + getName() + " block updates");
		while (iterator.hasNext()) {
			JAXBElement<? extends AbstractBaseActionType> elem;
			elem = (JAXBElement<? extends AbstractBaseActionType>) iterator.next();
			Object action = elem.getValue();
			action = elem.getValue().getClass().cast(action);

			if (StandByActionType.class.isInstance(action)) {
				String node = ((StandByActionType) action).getNodeName();
				if(USE_POWERSAVE_GOV){
					if(POWERSAVE_GOV_CMD.equals("")|| PERFORMANCE_GOV_CMD.equals("")){
						log.info("Powersave or Performance Governor command not set in properties file");
					}
					else{
						if(!set_Powersave_Gov(node)){
							log.info("Set to powersave governor on " + (node + " was not successful"));
						}
						else{
							log.info("Set to powersave governor on " + (node + " successfully executed"));
						}
					}					
				}
				if(USE_ACPI_STANDBY){
					if(!standBy((StandByActionType) action)){
						log.info("Standby action on " + ((StandByActionType) action).getNodeName() + " was not successful");
					}
					else{
						log.info("Standby action on " + ((StandByActionType) action).getNodeName() + " successfully executed");
					}
				}				
			} else if (PowerOnActionType.class.isInstance(action)) {
				if(!powerOn((PowerOnActionType) action)){
					log.info("Wake up action on " + ((PowerOnActionType) action).getNodeName() + " was not successful");
				}
				else{
					log.info("Wake up action on " + ((PowerOnActionType) action).getNodeName() + " successfully executed");
				}	
			} else if (StartJobActionType.class.isInstance(action)) {
				if(!startJob((StartJobActionType) action)){
					log.info("Start job action of " + ((StartJobActionType) action).getJobID() + " was not successful");
				}
				else{
					log.info("Start job action of " + ((StartJobActionType) action).getJobID() + " successfully executed");
				}	
			} 
		}
		if(startRMSScheduling()){
			log.info("Restarted/Resumed successfully the RMS Scheduling");
		}
		else{
			log.info("Failed to restart RMS Scheduling");
		}

		block_update = false;
		log.info("Com " + getName() + " block updates released");		
		return true;
	}

	/**
	 * 
	 * Starts a new job by sending an appropriate request to the proxy
	 * 
	 * @param action - The StartJobActionType object holds information about 
	 * the job id and the nodes to be used
	 * 
	 * @author Andre Giesler
	 */
	private boolean startJob(StartJobActionType action) {
		if(ACTIONS_OFF){
			action.setForwarded(false);
			return false;
		}

		boolean success = true;

		//At first wake up nodes if needed
		Iterator<String> iterator = action.getNodeName().iterator();	
		HashMap<String, Boolean> wakeupSuccessMap = new HashMap<String, Boolean>();
		ThreadGroup group = new ThreadGroup("WakeUp");
		while(iterator.hasNext()){
			String node = iterator.next();
			if(getNodeInfoMap().get(node) != null && getNodeInfoMap().get(node).getState() == NodeInfo.State.STANDBY
					|| getNodeInfoMap().get(node).getState() == NodeInfo.State.OFF){
				wakeupSuccessMap.put(node, false);				
				PowerOnActionType act = new PowerOnActionType();
				act.setFrameworkName(node);
				act.setNodeName(node);
				log.info("Start wake up thread on node " + node + " for job " + action.getJobID());				
				WakeUpWorker wakeupWorker = new WakeUpWorker(group, node, act, wakeupSuccessMap);
				wakeupWorker.start();
			}
			if(USE_POWERSAVE_GOV){
				if(!set_Performance_Gov(node)){
					log.info("Set to performance governor on " + node + " was not successful");
				}
				else{
					log.info("Set to performance governor on " + node + " successfully executed");
				}
			}
		}		

		Thread[]  threadArray = new Thread[ group.activeCount() ];
		group.enumerate( threadArray );

		// print array
		for ( Thread t : threadArray )
			log.trace( t );

		synchronized( group )
		{
			while ( group.activeCount() > 0 ){
				log.debug("Active Threads: " + group.activeCount());
				try {
					group.wait( 100 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			log.info("Gathering Wake Up results");
			Iterator<Entry<String, Boolean>> it = wakeupSuccessMap.entrySet().iterator();
			while( it.hasNext() )
			{
				Entry<String, Boolean> entry = it.next();
				String node = entry.getKey();
				Boolean wakenup = entry.getValue();
				log.info("Node " +  node + " has been waken up: " + wakenup.booleanValue());
				if(!wakenup.booleanValue()){
					return false;
				}
			}
			log.info("All requested nodes have been started. Proceed to start job " + action.getJobID());
		}

		String id = action.getJobID();
		log.info("Starting job " + id);
		List<String> nodes = action.getNodeName();
		String[] param_nodes = new String[nodes.size()];
		int[] param_cores = new int[nodes.size()];
		int i = 0;
		for(String s : nodes){		
			param_nodes[i] = s;
			param_cores[i] = getJobInfoMap().get(id).getnCores();
			log.info("Using node " + s + " with " + param_cores[i] + " cores for job " + id);			
			i++;
		}
		if(id.startsWith("_"))
			id = id.substring(1);

		try {
			ProxyResponseAlterJob proxyResponseAlterJob;
			if(IS_PROXY_CONNECTED){
				ProxyConnection proxyConnection;
				proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
				objectOutputStream.writeObject(new ProxyRequestAlterJob(id, param_nodes, param_cores, ALTER_JOB_CMD, CMD_DIR));
				objectOutputStream.flush();
				proxyResponseAlterJob = (ProxyResponseAlterJob)objectInputStream.readObject();			
				proxyConnection.close();
			}
			else{
				proxyResponseAlterJob = 
					new ProxyRequestAlterJob(id, param_nodes, param_cores, ALTER_JOB_CMD, CMD_DIR).execute();
			}		
			if(proxyResponseAlterJob.isSuccess()){
				ProxyResponseStartJob proxyResponseStartJob;
				if(IS_PROXY_CONNECTED){
					ProxyConnection proxyConnection;
					proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);			
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
					ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
					objectOutputStream.writeObject(new ProxyRequestRunJob(id, RUN_JOB_CMD, CMD_DIR));
					objectOutputStream.flush();	
					proxyResponseStartJob = (ProxyResponseStartJob) objectInputStream.readObject();
					proxyConnection.close();
				}
				else{
					proxyResponseStartJob = new ProxyRequestRunJob(id, RUN_JOB_CMD, CMD_DIR).execute();
				}
				if(proxyResponseStartJob.isSuccess()){
					action.setForwarded(true);
					try {
						TimeZone gmt = TimeZone.getTimeZone("GMT");
						GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
						action.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
					} catch (DatatypeConfigurationException e) {
						log.error("Error in date");
						return false;
					}
					int j = 0;			
					for(String s : nodes){	
						getNodeInfoMap().get(s).setState(NodeInfo.State.RUNNING);
						getNodeInfoMap().get(s).setNumOfUsedCores(param_cores[j]);
						getNodeInfoMap().get(s).getJobRefs().add(id);
						ComOperationCollector operation = new ComOperationCollector();
						operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", NodeStatusType.RUNNING));
						operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", ServerStatusType.ON));					
						operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/coresInUse", String.valueOf(param_cores[j])));
						getMonitor().simpleUpdateNode(host2Key(s), operation);
						getMonitor().updateNode(getName() + "_" + s, this);
						j++;
					}
				}	
				else{
					action.setForwarded(false);
					success = false;
					log.info("Could not start job " + id + ".");
					log.debug(proxyResponseStartJob.getMessage());
				}	
			}
			else{
				action.setForwarded(false);
				success = false;
				log.info("Could not alter job requirements of job " + id + ".");
				log.debug(proxyResponseAlterJob.getMessage());
			}			

			return success;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	//	private String getJobRefString (List<String> jobRefs){
	//		String jobRefStr = "";
	//		Iterator<String> jobRefIter = jobRefs.iterator();
	//		while(jobRefIter.hasNext())
	//		{
	//			String entry =  (String)jobRefIter.next();
	//			jobRefStr = jobRefStr + " " + entry;
	//		}				
	//		return jobRefStr.trim();
	//	}

	/**
	 * 
	 * Adjusts the node status
	 * 
	 * @param nodeInfoMapNew - The new node map
	 * 
	 * @author Andre Giesler
	 */
	private synchronized void adjustNodeState(Map<String, NodeInfo> nodeInfoMapNew){

		//Detecting which nodes are currently used by jobs
		usedNodes.clear();
		Iterator<Entry<String, JobInfo>> jobEntryIterator = getJobInfoMap().entrySet().iterator();
		while(jobEntryIterator.hasNext()){
			JobInfo jobInfo = jobEntryIterator.next().getValue();
			if(!jobInfo.getState().equals(State.QUEUED) && !getDeletedJobs().containsKey(jobInfo.getId())){
				Iterator<String> iterator = jobInfo.getNodes().iterator();
				while(iterator.hasNext()){
					String node = iterator.next();
					if(usedNodes.add(node))
						log.debug("Node " + node + " added to usedNodes by job " + jobInfo.getId() );
				}
			}
		}

		//Add nodes to current local map and monitor initially 
		Iterator<Entry<String, NodeInfo>> nodeEntryIterator = nodeInfoMapNew.entrySet().iterator();
		while(nodeEntryIterator.hasNext()){
			NodeInfo nodeInfoNew = nodeEntryIterator.next().getValue();
			log.info("Reported state of node " + nodeInfoNew.getId() + " is " + nodeInfoNew.getState().toString());
			NodeInfo nodeInfo = getNodeInfoMap().get(nodeInfoNew.getId());
			if(nodeInfo==null){
				try {
					nodeInfoNew.setComstarttime(System.currentTimeMillis());
					nodeInfoNew.setStateSwitchTime(System.currentTimeMillis());
					NodeInfo nodInfoClone = (NodeInfo)nodeInfoNew.clone();
					nodInfoClone.setState(NodeInfo.State.IDLE);
					nodInfoClone.setGovernor(NodeInfo.Governor.PERFORMANCE);
					getNodeInfoMap().put(nodeInfoNew.getId(), nodInfoClone);
					log.info("Added node " + nodeInfoNew.getId() + " initially to current NodeInfoMap");			
				} catch (CloneNotSupportedException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}

			//Adjust a detected idle node to running if it is already used by a job.
			if(usedNodes.contains(nodeInfoNew.getId()) 
					&& (nodeInfoNew.getState() == NodeInfo.State.BUSY 
							|| nodeInfoNew.getState() == NodeInfo.State.IDLE)){
				nodeInfoNew.setState(NodeInfo.State.RUNNING);
				log.info("Adjust correct status of node " + nodeInfoNew.getId() + " to Running");
				getMonitor().updateNode(getName() + "_" + nodeInfoNew.getId(), this);
			}
		}
	}

	/**
	 * 
	 * Activates a node which was set to sleep status before
	 * 
	 * @param action - The PowerOnActionType object holds information about 
	 * the node that should be powered on.
	 * 
	 * @author Andre Giesler
	 */
	private boolean powerOn(PowerOnActionType action) {
		if(ACTIONS_OFF){
			action.setForwarded(false);
			return false;
		}
		boolean success = true;
		if(getNodeInfoMap().get(action.getNodeName()).getState() != NodeInfo.State.IDLE 
				&& getNodeInfoMap().get(action.getNodeName()).getState() != NodeInfo.State.RUNNING){

			String node = action.getNodeName();

			log.info("Wakening up node " + node);

			try {
				ProxyResponseNodeWakeUp proxyResponsePowerOn;
				if(IS_PROXY_CONNECTED){
					ProxyConnection proxyConnection;
					proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
					ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
					objectOutputStream.writeObject(new ProxyRequestNodeWakeUp(node, WAKEUP_NODE_CMD, CMD_DIR));
					objectOutputStream.flush();
					proxyResponsePowerOn = (ProxyResponseNodeWakeUp) objectInputStream.readObject();
					proxyConnection.close();
				}
				else{
					proxyResponsePowerOn = (ProxyResponseNodeWakeUp) new ProxyRequestNodeWakeUp(node, WAKEUP_NODE_CMD, CMD_DIR).execute();
				}				
				if(proxyResponsePowerOn.isSuccess()){
					action.setForwarded(true);
					try {
						TimeZone gmt = TimeZone.getTimeZone("GMT");
						GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
						action.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
					} catch (DatatypeConfigurationException e) {
						log.error("Error in date");
						action.setForwardedAt(null);
						return false;
					}
					getNodeInfoMap().get(action.getNodeName()).setState(NodeInfo.State.IDLE);
					ComOperationCollector operation = new ComOperationCollector();
					operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", NodeStatusType.IDLE));
					operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", ServerStatusType.ON));
					getMonitor().simpleUpdateNode(host2Key(node), operation);
				}	
				else{
					action.setForwarded(false);
					success = false;
					log.info("Could not wake up node " + node + ".");
					log.debug(proxyResponsePowerOn.getMessage());
				}
				if(USE_POWERSAVE_GOV){
					if(!set_Performance_Gov(node)){
						log.info("Set to performance governor on " + node + " was not successful");
					}
					else{
						log.info("Set to performance governor on " + node + " successfully executed");
					}
				}

				return success;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return false;
		}	
		return true;
	}

	/**
	 * 
	 * Sets a node to powersave governor
	 * 
	 */
	private boolean set_Powersave_Gov(String node) {
		if(ACTIONS_OFF){
			return false;
		}

		boolean success = true;

		if(getNodeInfoMap().get(node).getState() != NodeInfo.State.STANDBY 
				&& getNodeInfoMap().get(node).getState() != NodeInfo.State.RUNNING){
			if(getNodeInfoMap().get(node).getGovernor().equals(NodeInfo.Governor.POWERSAVE)){
				log.info("Powersave Governor on node " + node + " already set.");
				return true;
			}
			log.debug("Set to Powersave Governor: node " + node);			
			try {
				ProxyResponseSimpleNodeCmd proxyResponseSimpleNodeCmd;
				if(IS_PROXY_CONNECTED){
					ProxyConnection proxyConnection;
					proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
					ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
					objectOutputStream.writeObject(new ProxyRequestSimpleNodeCmd(node, POWERSAVE_GOV_CMD, CMD_DIR));
					objectOutputStream.flush();
					proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) objectInputStream.readObject();
					proxyConnection.close();
				}
				else{
					proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) 
					new ProxyRequestSimpleNodeCmd(node, POWERSAVE_GOV_CMD, CMD_DIR).execute();
				}

				if(proxyResponseSimpleNodeCmd.isSuccess()){
					getNodeInfoMap().get(node).setGovernor(NodeInfo.Governor.POWERSAVE);
				}	
				else{
					success = false;
					log.info("Could not set node " + node + " to powersave governor.");
					log.debug(proxyResponseSimpleNodeCmd.getMessage());
				}

				return success;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}	

		return false;
	}

	/**
	 * 
	 * Sets a node to powersave governor
	 * 
	 */
	private boolean set_Performance_Gov(String node) {
		if(ACTIONS_OFF){
			return false;
		}

		boolean success = true;

		log.info("Set to Performance Governor: node " + node);			
		try {
			ProxyResponseSimpleNodeCmd proxyResponseSimpleNodeCmd;
			if(IS_PROXY_CONNECTED){
				ProxyConnection proxyConnection;
				proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
				objectOutputStream.writeObject(new ProxyRequestSimpleNodeCmd(node, PERFORMANCE_GOV_CMD, CMD_DIR));
				objectOutputStream.flush();
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) objectInputStream.readObject();
				proxyConnection.close();
			}
			else{
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) 
				new ProxyRequestSimpleNodeCmd(node, PERFORMANCE_GOV_CMD, CMD_DIR).execute();
			}

			if(proxyResponseSimpleNodeCmd.isSuccess()){
				getNodeInfoMap().get(node).setGovernor(NodeInfo.Governor.PERFORMANCE);
			}	
			else{
				success = false;
				log.info("Could not set node " + node + " to performance governor.");
				log.debug(proxyResponseSimpleNodeCmd.getMessage());
			}

			return success;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}


		return false;
	}

	/**
	 * 
	 * Sets a node to sleep modus
	 * 
	 * @param action - The PowerOffActionType object holds information about 
	 * the node that should be powered off.
	 * 
	 * @author Andre Giesler
	 */
	private boolean standBy(StandByActionType action) {
		if(ACTIONS_OFF){
			action.setForwarded(false);
			return false;
		}

		boolean success = true;

		if(getNodeInfoMap().get(action.getNodeName()).getState() != NodeInfo.State.STANDBY 
				&& getNodeInfoMap().get(action.getNodeName()).getState() != NodeInfo.State.RUNNING){						
			String node = action.getNodeName();		
			log.info("Set to standby: node " + node);			
			try {
				ProxyResponseNodeToStandby proxyResponseToStandby;
				if(IS_PROXY_CONNECTED){
					ProxyConnection proxyConnection;
					proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
					ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
					objectOutputStream.writeObject(new ProxyRequestNodeToStandby(node, SLEEP_NODE_CMD, CMD_DIR));
					objectOutputStream.flush();
					proxyResponseToStandby = (ProxyResponseNodeToStandby) objectInputStream.readObject();
					proxyConnection.close();
				}
				else{
					proxyResponseToStandby = (ProxyResponseNodeToStandby) 
					new ProxyRequestNodeToStandby(node, SLEEP_NODE_CMD, CMD_DIR).execute();
				}

				if(proxyResponseToStandby.isSuccess()){
					action.setForwarded(true);
					try {
						TimeZone gmt = TimeZone.getTimeZone("GMT");
						GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
						action.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
					} catch (DatatypeConfigurationException e) {
						log.error("Error in date");
						return false;
					}
					getNodeInfoMap().get(action.getNodeName()).setState(NodeInfo.State.STANDBY);
					ComOperationCollector operation = new ComOperationCollector();
					operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/nativeOperatingSystem/node[1]/status", NodeStatusType.STANDBY));
					operation.add(new ComOperation(ComOperation.TYPE_UPDATE, "/status", ServerStatusType.STANDBY));
					getMonitor().simpleUpdateNode(host2Key(node), operation);
				}	
				else{
					action.setForwarded(false);
					success = false;
					log.info("Could not set node " + node + " to standby.");
					log.debug(proxyResponseToStandby.getMessage());
				}

				return success;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}	

		return false;
	}

	/**
	 * 
	 * Sets a node to sleep modus
	 * 
	 * @param action - The PowerOffActionType object holds information about 
	 * the node that should be powered off.
	 * 
	 * @author Andre Giesler
	 */
	private boolean stopRMSScheduling() {
		if(ACTIONS_OFF){
			return false;
		}

		boolean success = true;				

		log.debug("Trying to stop/pause scheduling of default RMS scheduler");			
		try {
			ProxyResponseSimpleNodeCmd proxyResponseSimpleNodeCmd;
			if(IS_PROXY_CONNECTED){
				ProxyConnection proxyConnection;
				proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
				objectOutputStream.writeObject(new ProxyRequestSimpleNodeCmd("", STOP_RMS_SCHEDULER_CMD, CMD_DIR));
				objectOutputStream.flush();
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) objectInputStream.readObject();
				proxyConnection.close();
			}
			else{
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) 
				new ProxyRequestSimpleNodeCmd("", STOP_RMS_SCHEDULER_CMD, CMD_DIR).execute();
			}

			if(proxyResponseSimpleNodeCmd.isSuccess()){
				log.info("Stopped/Paused RMS scheduling successfully");
			}	
			else{
				success = false;
				log.info("Could not stop/pause RMS scheduling");
				log.debug(proxyResponseSimpleNodeCmd.getMessage());
			}

			return success;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}


		return false;
	}

	/**
	 * 
	 * Sets a node to sleep modus
	 * 
	 * @param action - The PowerOffActionType object holds information about 
	 * the node that should be powered off.
	 * 
	 * @author Andre Giesler
	 */
	private boolean startRMSScheduling() {
		if(ACTIONS_OFF){
			return false;
		}

		boolean success = true;				

		log.debug("Trying to start/resume scheduling of default RMS scheduler");			
		try {
			ProxyResponseSimpleNodeCmd proxyResponseSimpleNodeCmd;
			if(IS_PROXY_CONNECTED){
				ProxyConnection proxyConnection;
				proxyConnection = new ProxyConnection(PROXY_HOST, PROXY_PORT, CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASS);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(proxyConnection.getOutputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(proxyConnection.getInputStream());
				objectOutputStream.writeObject(new ProxyRequestSimpleNodeCmd("", START_RMS_SCHEDULER_CMD, CMD_DIR));
				objectOutputStream.flush();
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) objectInputStream.readObject();
				proxyConnection.close();
			}
			else{
				proxyResponseSimpleNodeCmd = (ProxyResponseSimpleNodeCmd) 
				new ProxyRequestSimpleNodeCmd("", START_RMS_SCHEDULER_CMD, CMD_DIR).execute();
			}

			if(proxyResponseSimpleNodeCmd.isSuccess()){
				log.info("Started/Resumed RMS scheduling successfully");
			}	
			else{
				success = false;
				log.info("Could not start/resume RMS scheduling");
				log.debug(proxyResponseSimpleNodeCmd.getMessage());
			}

			return success;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}


		return false;
	}



	@Override
	public boolean executeUpdate(String arg0, Object arg1) {
		if(arg0 == getRmKey()){
			executeJobUpdate((ServerType)arg1);
		}
		else{
			executeNodeUpdate((ServerType)arg1);
		}
		return false;
	}

	/**
	 /**
	 * Updates a node in model on behalf of this COM object.
	 * 
	 * @param serverType
	 */
	private synchronized void executeNodeUpdate(ServerType serverType) {
		NodeInfo nodeInfo = getNodeInfoMap().get(serverType.getFrameworkID());

		RackableServerType rServer = (RackableServerType)serverType;

		if(rServer.getStatus().equals(ServerStatusType.OFF)){
			rServer.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatusType.OFF);
			nodeInfo.setState(NodeInfo.State.OFF);
		}

		PowerType pt = new PowerType(getPowerPerNodeMap().get(nodeInfo.getId()));
		rServer.setMeasuredPower(pt);

		if(nodeInfo.getJobRefs() != null && nodeInfo.getJobRefs().size()>0){		
			if(rServer.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
				try {
					rServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().clear();
					rServer.getNativeOperatingSystem().getNode().get(0).setCoresInUse(new NrOfCoresType(0));
				} catch (UnsupportedOperationException e) {
					log.error(e.getMessage());
				}
			}
			//Ensure that jobRefs are not null (because of minoccurs=0 parameter in schema)
			else{
				IDREFS idrefs = new IDREFS();
				rServer.getNativeOperatingSystem().getNode().get(0).setJobRef(idrefs);
				rServer.getNativeOperatingSystem().getNode().get(0).setCoresInUse(new NrOfCoresType(0));
			}

			int nr_of_used_cores = 0;
			Iterator<String> jobIter = nodeInfo.getJobRefs().iterator();
			while(jobIter.hasNext())
			{				
				String id = jobIter.next();
				if(!id.startsWith("_")){
					id = "_" + id;
				}
				RackableServerType rmKeyServer = (RackableServerType)getMonitor().getMonitoredObjectsCopy(getName()).get(getRmKey());			
				Iterator<JobType> jobTypeIter = rmKeyServer.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();				
				while(jobTypeIter.hasNext())
				{
					JobType tempJob =  (JobType) jobTypeIter.next();
					if(rServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().contains(tempJob)){
						continue;
					}
					if(tempJob.getId().equals(id)){
						rServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(tempJob);//.add(tempJob);
						log.info("Added jobRef " + tempJob.getId() + " to server " + rServer.getFrameworkID());
						nr_of_used_cores += tempJob.getNeededCoresPerNode().getValue();
						continue;
					}
				}
				rServer.getNativeOperatingSystem().getNode().get(0).setCoresInUse(new NrOfCoresType(nr_of_used_cores));				
			}
			log.info("Set Nr of used cores at node " + nodeInfo.getId() + " to: " + nr_of_used_cores);
			if(nr_of_used_cores==0){
				rServer.getNativeOperatingSystem().getNode().get(0).setJobRef(null);
				log.debug("Nulled IDREFS at node " + nodeInfo.getId());
			}
		}
		else{
			if(rServer.getNativeOperatingSystem().getNode().get(0).getJobRef()!=null){
				rServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().clear();	
				rServer.getNativeOperatingSystem().getNode().get(0).setJobRef(null);
				//rServer.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatusType.IDLE);
				rServer.getNativeOperatingSystem().getNode().get(0).setCoresInUse(new NrOfCoresType(0));
				log.debug("Nulled IDREFS at node " + nodeInfo.getId());
			}
		}

		if(SYSTEM_MON){				
			log.info("Updating dynamic parameters of node " + nodeInfo.getId());

			//update cpu/core loads
			double[] usage_values = getCoreUsagePerNodeMap().get(nodeInfo.getId());	
			double[] frequency_values = getCoreFrequencyPerNodeMap().get(nodeInfo.getId());

			if(frequency_values!=null && usage_values!=null){
				int nr_of_cpus = rServer.getMainboard().get(0).getCPU().size();
				int index = 0, index_freq = 0;
				for (int i = 0; i < nr_of_cpus; i++) {
					int nr_of_cores = rServer.getMainboard().get(0).getCPU().get(0).getCore().size();
					double cpu = 0.0;
					if(HYPERTHREADING){
						nr_of_cores = nr_of_cores / HYPERTHREADED_CORES;
						int threading_indexer = usage_values.length / HYPERTHREADED_CORES;
						for (int j = 0; j < nr_of_cores; j++) {					
							double core_logical_1 = usage_values[index];
							double core_logical_2 = usage_values[index+threading_indexer];
							index++;
							double core = (core_logical_1 + core_logical_2)/2;
							log.debug("CPU" + i +  " Hyperthreaded CORE" + j + " Usage: " + core);
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j).setCoreLoad(new CoreLoadType(core));
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j + threading_indexer/HYPERTHREADED_CORES).setCoreLoad(new CoreLoadType(0.0));
							cpu += core;
							double core_freq = frequency_values[index_freq++];
							log.debug("CPU" + i +  " Hyperthreaded CORE" + j + " frequency: " + core_freq);
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j).setFrequency(new FrequencyType(core_freq));	
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j + threading_indexer/HYPERTHREADED_CORES).setFrequency(new FrequencyType(0.0));
						}
					}
					else{
						for (int j = 0; j < nr_of_cores; j++) {					
							double core = usage_values[index++];
							log.debug("CPU" + i +  "CORE" + j + " Usage: " + core);
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j).setCoreLoad(new CoreLoadType(core));
							cpu += core;
							double core_freq = frequency_values[index_freq++];
							log.debug("CPU" + i +  "CORE" + j + " frequency: " + core_freq);
							rServer.getMainboard().get(0).getCPU().get(i).getCore().get(j).setFrequency(new FrequencyType(core_freq));
						}
					}

					cpu = cpu / nr_of_cores;
					log.debug("CPU" + i + " Usage: " + cpu);
					rServer.getMainboard().get(0).getCPU().get(i).setCpuUsage(new CpuUsageType(cpu));
				}
				log.info("All CPU Usage: " + nodeInfo.getCpuUsage());
				rServer.getNativeOperatingSystem().getNode().get(0).setActualCPUUsage(new CpuUsageType(nodeInfo.getCpuUsage()));
			}			

			//update core voltage
			double[] voltage_val = getCoreVoltagePerNodeMap().get(nodeInfo.getId());
			if(voltage_val!=null){			
				ListIterator<CPUType> cpu_it = rServer.getMainboard().get(0).getCPU().listIterator();
				while(cpu_it.hasNext()){
					int i = 0;
					CPUType cpuType = cpu_it.next();
					ListIterator<CoreType> core_it = cpuType.getCore().listIterator();
					while(core_it.hasNext()){
						CoreType coreType = core_it.next();
						coreType.setVoltage(new VoltageType(voltage_val[i]));
					}
					i++;
				}
			}		

			//update memory usage
			double mem_free_usage_val = getMemoryUsagePerNodeMap().get(nodeInfo.getId());
			log.info("Free Memory usage: " + mem_free_usage_val);
			int sticks = rServer.getMainboard().get(0).getRAMStick().size();
			double ramStickSum = 0.0;
			for(int i=0;i<sticks;i++){
				double ramStickSize = rServer.getMainboard().get(0).getRAMStick().get(i).getSize().getValue();			
				ramStickSum += ramStickSize;			
			}

			rServer.getMainboard().get(0).setMemoryUsage(new MemoryUsageType(ramStickSum - mem_free_usage_val));
			rServer.getNativeOperatingSystem().setSystemRAMBaseUsage(new MemoryUsageType(ramStickSum - mem_free_usage_val));

			//update fan's actual RPMs
			int[] actualRPM_values = getFanActualRPMPerNodeMap().get(nodeInfo.getId());
			int rpm_list_length = rServer.getFan().size();
			if(actualRPM_values!=null){
				for(int i=0;i<rpm_list_length;i++){
					int fan = actualRPM_values[i];
					log.info("Fan Nr." + i + " actual RPM: " + fan);
					rServer.getFan().get(i).setActualRPM(new RPMType(fan));
				}
			}

			//update storage unit's read/write rate
			double readrate_val = getStorageUnitReadRatePerNodeMap().get(nodeInfo.getId());
			log.info("Read rate: " + readrate_val);
			double writerate_val = getStorageUnitWriteRatePerNodeMap().get(nodeInfo.getId());
			log.info("Write rate: " + writerate_val);
			rServer.getMainboard().get(0).getHardDisk().get(0).setReadRate(new IoRateType(readrate_val));
			rServer.getMainboard().get(0).getHardDisk().get(0).setWriteRate(new IoRateType(writerate_val));
		}
	}

	/**
	 * Updates a job on behalf of this COM object.
	 * 
	 * @param serverType
	 *
	 * @author Daniel Brinkers
	 */
	private synchronized void executeJobUpdate(ServerType serverType) {
		//Getting running jobs from queue
		Iterator<JobType> iterator = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
		List<JobType> jobTypeList = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs();
		while(iterator.hasNext()){
			JobType jobType = iterator.next();
			String key = jobType.getFrameworkID();

			//Updating changed jobs
			if(getChangedJobs().containsKey(key)){
				JobInfo jobInfoNew = getChangedJobs().get(key);
				JobInfo jobInfo = getJobInfoMap().get(jobInfoNew.getId());
				JobInfo.State old_state = jobInfo.getState();
				jobInfo.setState(jobInfoNew.getState());
				JobStatusType jobStatusType = JobStatusType.QUEUED;
				switch(jobInfo.getState()){
				case QUEUED:
					jobStatusType = JobStatusType.QUEUED;
					jobInfo.setState(JobInfo.State.QUEUED);
					break;
				case RUNNING:
					jobStatusType = JobStatusType.RUNNING;
					jobInfo.setState(JobInfo.State.RUNNING);
					break;
				}
				jobType.setStatus(jobStatusType);

				jobInfo.setNodes(new HashSet<String>());
				jobInfo.getNodes().addAll(jobInfoNew.getNodes());

				//mm update? - not included in mm?

				jobInfo.setStartTime(jobInfoNew.getStartTime());
				jobType.setTimeOfStart(new JobTimeType(jobInfoNew.getStartTime()));
				log.info("Job state of " + key + " changed from " + old_state + " to " + jobInfo.getState() );
			}
			else if(getJobInfoMap().containsKey(key)){
				//remove finished jobs from the model
				if(getDeletedJobs().containsKey(key)){
					jobTypeList.remove(jobType);
					iterator = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
					jobType = null;
				}
			}
			else {
				//should be deleted
				//getJobInfoMap().remove(key);
				jobTypeList.remove(jobType);
				iterator = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
				jobType = null;
			}
		}

		//Adding new jobs
		Iterator<Entry<String, JobInfo>> entryIterator = getNewJobs().entrySet().iterator();
		while(entryIterator.hasNext()){
			Entry<String, JobInfo> entry = entryIterator.next();
			String key = entry.getKey();
			JobInfo jobInfo = entry.getValue();
			try {
				getJobInfoMap().put(key, (JobInfo) jobInfo.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			JobType jobType = new JobType();
			jobType.setFrameworkID(jobInfo.getId());
			log.info("Adding new job " + jobInfo.getId() + " to jobList");
			jobType.setFrameworkRef(serverType.getFrameworkRef());
			jobType.setId(jobInfo.getId());
			jobType.setNeededCoresPerNode(new NrOfCoresType(jobInfo.getnCores()));
			jobType.setNeededCPUSpeed(new FrequencyType(0));
			jobType.setNeededMemory(new MemoryUsageType(jobInfo.getMemory()/1024));
			jobType.setNumberOfNodes(new NrOfNodesType(jobInfo.getnNodes()));
			jobType.setPriority(new JobPriorityType(jobInfo.getPriority()));
			log.info("Priority " + jobInfo.getPriority());
			jobType.getPropertiesOfNodes().clear();
			jobType.getPropertiesOfNodes().add(JobPropOfNodesType.TO_BE_REVISED);

			switch(jobInfo.getState()){
			case QUEUED:
				jobType.setStatus(JobStatusType.QUEUED);
				jobInfo.setState(JobInfo.State.QUEUED);
				break;
			case RUNNING:
				jobType.setStatus(JobStatusType.RUNNING);
				jobInfo.setState(JobInfo.State.RUNNING);
				break;
			}

			if(jobInfo.getStartTime()<0){
				jobInfo.setStartTime(0);
			}
			log.info("Start time " + jobInfo.getStartTime());
			jobType.setTimeOfStart(new JobTimeType(jobInfo.getStartTime()));

			if(jobInfo.getSubmitTime()<0){
				jobInfo.setSubmitTime(0);
			}
			log.info("Submission time " + jobInfo.getSubmitTime());
			jobType.setTimeOfSubmission(new JobTimeType(jobInfo.getSubmitTime()));

			if(jobInfo.getWallTime()<0){
				jobInfo.setWallTime(0);
			}
			log.info("Wall time " + jobInfo.getWallTime());
			jobType.setWallTime(new JobTimeType(jobInfo.getWallTime()));

			jobTypeList.add(jobType);
		}

		iterator = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
		jobTypeList = serverType.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs();
		while(iterator.hasNext()){
			JobType jobType = iterator.next();
			String key = jobType.getFrameworkID();
			log.debug(key + " is monitored");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public HashMap getMonitoredObjects() {
		return (HashMap) getMonitoredObjects_();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public HashMap getQueuesHashMap() {
		return (HashMap) getQueuesHashMap_();
	}

	@Override
	public boolean init(String name, IMonitor monitor) {
		setMonitor(monitor);
		setName(name);

		Properties prop = new Properties();
		String current_key = "";
		try
		{
			String prop_path = COM_PROPERTIES_DIR + name + COM_PROPERTIES_SUFFIX;
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(prop_path);
			log.info("Reading properties from " + prop_path);
			prop.load(is);

			current_key = "clientKeystorePath";    		
			CLIENT_KEYSTORE_PATH = prop.getProperty(current_key).trim();

			current_key = "clientKeystorePass";
			CLIENT_KEYSTORE_PASS = prop.getProperty(current_key).trim();

			current_key = "proxyHost";
			PROXY_HOST = prop.getProperty(current_key).trim();

			current_key = "proxyPort";
			PROXY_PORT = Integer.parseInt(prop.getProperty("proxyPort").trim());

			current_key = "rms_Head_Node";
			RMS_HEAD_NODE = prop.getProperty("rms_Head_Node").trim();

			current_key = "power_Consumption_Cmd";
			POWER_CONSUMPTION_CMD = prop.getProperty("power_Consumption_Cmd").trim();

			current_key = "getCpuUsageCmd";
			String cpu_usage_cmd = prop.getProperty("getCpuUsageCmd","").trim();
			CPU_USAGE_CMD = cpu_usage_cmd.split(" ");

			current_key = "getCoreFreqsCmd";
			String core_freqs_cmd = prop.getProperty("getCoreFreqsCmd","").trim();
			CORE_FREQS_CMD = core_freqs_cmd.split(" ");

			current_key = "getCoreVoltageCmd";
			String core_voltage_cmd = prop.getProperty("getCoreVoltageCmd").trim();
			CORE_VOLTAGE_CMD = core_voltage_cmd.split(" ");

			current_key = "getAvailMemoryCmd";
			String mem_usage_cmd = prop.getProperty("getAvailMemoryCmd").trim();
			MEM_USAGE_CMD = mem_usage_cmd.split(" ");

			current_key = "getFanRPMsCmd";
			String fan_usage_cmd = prop.getProperty("getFanRPMsCmd").trim();
			FAN_USAGE_CMD = fan_usage_cmd.split(" ");

			current_key = "getIOStatsCmd";
			String io_stats_cmd = prop.getProperty("getIOStatsCmd").trim();
			IO_STATS_CMD = io_stats_cmd.split(" ");

			current_key = "run_Job_Cmd";
			RUN_JOB_CMD = prop.getProperty("run_Job_Cmd").trim();

			current_key = "alter_Job_Cmd";
			ALTER_JOB_CMD = prop.getProperty("alter_Job_Cmd").trim();

			current_key = "wakeup_Node_Cmd";
			WAKEUP_NODE_CMD = prop.getProperty("wakeup_Node_Cmd").trim();

			current_key = "sleep_Node_Cmd";
			SLEEP_NODE_CMD = prop.getProperty("sleep_Node_Cmd").trim();

			current_key = "deep_sleep_Node_Cmd";
			DEEP_SLEEP_NODE_CMD = prop.getProperty("deep_sleep_Node_Cmd").trim();

			current_key = "cmd_Dir";
			CMD_DIR = prop.getProperty("cmd_Dir").trim();

			current_key = "ipmi_core_voltage_id";
			IPMI_CORE_VOLTAGE_ID = prop.getProperty("ipmi_core_voltage_id").trim();

			current_key = "mpstat_idle_col";
			MPSTAT_IDLE_COL = prop.getProperty("mpstat_idle_col").trim();

			current_key = "hyperthreading";
			HYPERTHREADING = Boolean.parseBoolean(prop.getProperty("hyperthreading", "false"));

			current_key = "hyperthread_cores";
			HYPERTHREADED_CORES = Integer.parseInt(prop.getProperty("hyperthread_cores", "2"));

			current_key = "updateInterval";
			UPDATE_INTERVAL = Integer.parseInt(prop.getProperty("updateInterval").trim());

			current_key = "stresstest_record";
			RECORDING = Boolean.parseBoolean(prop.getProperty("stresstest_record").trim());

			current_key = "switch_off_actions";
			String switch_off = prop.getProperty("switch_off_actions", "false").trim();	    	
			ACTIONS_OFF = Boolean.parseBoolean(switch_off);

			current_key = "dynamic_system_monitoring";
			String dynamic_system_monitoring = prop.getProperty("dynamic_system_monitoring", "false");
			SYSTEM_MON = Boolean.parseBoolean(dynamic_system_monitoring.trim());

			current_key = "benchmark_mode";
			String benchmark_mode = prop.getProperty("benchmark_mode", "false");
			BENCHMARK_MODE = Boolean.parseBoolean(benchmark_mode.trim());

			current_key = "stop_after_benchmark_mode";
			String stop_after_benchmark_mode = prop.getProperty("stop_after_benchmark_mode", "false");
			STOP_COM_AFTER_BENCHMARK = Boolean.parseBoolean(stop_after_benchmark_mode.trim());

			current_key = "is_proxy_connected";
			String is_proxy_connected = prop.getProperty("is_proxy_connected", "false");
			IS_PROXY_CONNECTED = Boolean.parseBoolean(is_proxy_connected.trim());

			current_key = "set_to_standby";
			String use_acpi_standby = prop.getProperty("set_to_standby", "false");
			USE_ACPI_STANDBY = Boolean.parseBoolean(use_acpi_standby.trim());

			current_key = "use_powersave_gov";
			String use_powersave_gov = prop.getProperty("use_powersave_gov", "false");
			USE_POWERSAVE_GOV = Boolean.parseBoolean(use_powersave_gov.trim());

			current_key = "powersave_gov_cmd";
			POWERSAVE_GOV_CMD = prop.getProperty("powersave_gov_cmd","").trim();

			current_key = "performance_gov_cmd";
			PERFORMANCE_GOV_CMD = prop.getProperty("performance_gov_cmd","").trim();

			current_key = "useRMSScheduler";
			String usedefaultScheduler = prop.getProperty(current_key, "false");
			USE_RMS_SCHEDULER = Boolean.parseBoolean(usedefaultScheduler.trim());

			current_key = "startSchedulerCmd";
			START_RMS_SCHEDULER_CMD = prop.getProperty("current_key","").trim();

			current_key = "stopSchedulerCmd";
			STOP_RMS_SCHEDULER_CMD = prop.getProperty("current_key","").trim();

			current_key = "mailReportActivated";
			String mailReportActivated = prop.getProperty(current_key, "false");
			MAIL_REPORT_ON = Boolean.parseBoolean(mailReportActivated.trim());

			current_key = "mailReportSMTP";
			MAIL_REPORT_SMTP = prop.getProperty("current_key","").trim();

			current_key = "mailReportRecipients";
			MAIL_REPORT_RECEPIENTS = prop.getProperty("current_key","").trim();

			log.info("Client keystore path: " + CLIENT_KEYSTORE_PATH);
			log.info("Proxy host: " + PROXY_HOST);
			log.info("Proxy port: " + PROXY_PORT);
			log.info("RMS head node : " + RMS_HEAD_NODE);
			log.info("USE_RMS_SCHEDULER : " + USE_RMS_SCHEDULER);
			log.info("START_RMS_SCHEDULER_CMD : " + START_RMS_SCHEDULER_CMD);
			log.info("STOP_RMS_SCHEDULER_CMD : " + STOP_RMS_SCHEDULER_CMD);
			log.info("POWER_CONSUMPTION_CMD : " + POWER_CONSUMPTION_CMD);
			log.info("CPU_USAGE_CMD : " + CPU_USAGE_CMD.toString());
			log.info("CORE_FREQS_CMD : " + CORE_FREQS_CMD.toString());
			log.info("MEM_USAGE_CMD : " + MEM_USAGE_CMD.toString());
			log.info("FAN_USAGE_CMD : " + FAN_USAGE_CMD.toString());
			log.info("IO_STATS_CMD : " + IO_STATS_CMD.toString());
			log.info("RUN_JOB_CMD : " + RUN_JOB_CMD.toString());
			log.info("ALTER_JOB_CMD : " + ALTER_JOB_CMD);
			log.info("WAKEUP_NODE_CMD : " + WAKEUP_NODE_CMD);
			log.info("SLEEP_NODE_CMD : " + SLEEP_NODE_CMD);
			log.info("DEEP_SLEEP_NODE_CMD : " + DEEP_SLEEP_NODE_CMD);
			log.info("CMD_DIR : " + CMD_DIR);
			log.info("IPMI_CORE_VOLTAGE_ID : " + IPMI_CORE_VOLTAGE_ID);
			log.info("MPSTAT_IDLE_COL : " + MPSTAT_IDLE_COL);
			log.info("HYPERTHREADING : " + HYPERTHREADING);
			log.info("HYPERTHREADED_CORES : " + HYPERTHREADED_CORES);
			log.info("UPDATE_INTERVAL : " + UPDATE_INTERVAL);
			log.info("RECORDING : " + RECORDING);
			log.info("ACTIONS_OFF : " + ACTIONS_OFF);
			log.info("SYSTEM_MON : " + SYSTEM_MON);
			log.info("BENCHMARK_MODE : " + BENCHMARK_MODE);
			log.info("STOP_COM_AFTER_BENCHMARK : " + STOP_COM_AFTER_BENCHMARK);
			log.info("IS_PROXY_CONNECTED : " + IS_PROXY_CONNECTED);
			log.info("USE_ACPI_STANDBY : " + USE_ACPI_STANDBY);
			log.info("USE_POWERSAVE_GOV : " + USE_POWERSAVE_GOV);
			log.info("POWERSAVE_GOV_CMD : " + POWERSAVE_GOV_CMD);
			log.info("PERFORMANCE_GOV_CMD : " + PERFORMANCE_GOV_CMD);
			log.info("MAIL_REPORT_ON : " + MAIL_REPORT_ON);
			log.info("MAIL_REPORT_SMTP : " + MAIL_REPORT_SMTP);
			log.info("MAIL_REPORT_RECEPIENTS : " + MAIL_REPORT_RECEPIENTS);

			Map<String, ICom> map = getMonitor().getMonitoredObjectsMap(getName());

			setQueuesHashMap_(new HashMap<String, ConcurrentLinkedQueue<ComOperationCollector>>());

			Iterator<String> iterator = map.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				getQueuesHashMap_().put(key, new ConcurrentLinkedQueue<ComOperationCollector>());
				String[] key_trim = key.split("_");
				if(key_trim[1].equals(RMS_HEAD_NODE))
					setRmKey(key);
			}

			getMonitor().setFrameworkStatus(getName(), FrameworkStatusType.STARTING);

			startUpdate();

			getMonitor().setFrameworkStatus(getName(), FrameworkStatusType.RUNNING);

			return true;

		}
		catch (IOException ioe){
			log.error("Couldn't read properties from relative path 'config/ComFzj.properties'.");
			return false;
		}
		catch (Exception e)
		{
			log.error("Couldn't read key " + current_key + " from properties. Please check logs. Stopping com update...");
			stopUpdate();
			return false;
		}    		
	}

	@Override
	public boolean startUpdate() {	
		if(BENCHMARK_MODE){
			benchmark_starttime = System.currentTimeMillis();
		}	
		getThread().start();
		return false;
	}

	@Override
	public boolean stopUpdate() {
		log.info("Trying to interrupt update thread...");
		getThread().interrupt();
		getMonitor().setFrameworkStatus(getName(), FrameworkStatusType.STOPPED);
		log.info("Update thread successfully stopped");
		return false;
	}

	public void setJobInfoMap(Map<String, JobInfo> jobInfoMap) {
		this.jobInfoMap_ = jobInfoMap;
	}

	public Map<String, JobInfo> getJobInfoMap() {
		return jobInfoMap_;
	}

	/**
	 * @return the powerPerNodeMap
	 */
	public Map<String, Double> getPowerPerNodeMap() {
		return powerPerNodeMap;
	}

	/**
	 * @param powerPerNodeMap the powerPerNodeMap to set
	 */
	public void setPowerPerNodeMap(Map<String, Double> powerPerNodeMap) {
		this.powerPerNodeMap = powerPerNodeMap;
	}

	public void setNodeInfoMap(Map<String, NodeInfo> nodeInfoMap) {
		this.nodeInfoMap_ = nodeInfoMap;
	}

	public Map<String, NodeInfo> getNodeInfoMap() {
		return nodeInfoMap_;
	}

	public void setMonitor(IMonitor monitor) {
		this.monitor_ = monitor;
	}

	public IMonitor getMonitor() {
		return monitor_;
	}

	public void setName(String name) {
		this.name_ = name;
	}

	public String getName() {
		return name_;
	}

	public void setMonitoredObjects_(Map<String, ICom> monitoredObjects_) {
		this.monitoredObjects_ = monitoredObjects_;
	}

	public Map<String, ICom> getMonitoredObjects_() {
		return monitoredObjects_;
	}

	public void setQueuesHashMap_(
			Map<String, ConcurrentLinkedQueue<ComOperationCollector>> queuesHashMap_) {
		this.queuesHashMap_ = queuesHashMap_;
	}

	public Map<String, ConcurrentLinkedQueue<ComOperationCollector>> getQueuesHashMap_() {
		return queuesHashMap_;
	}

	public void setThread(Thread thread_) {
		this.thread_ = thread_;
	}

	public Thread getThread() {
		return thread_;
	}

	/**
	 * @return the usedNodes
	 */
	public Set<String> getUsedNodes() {
		return usedNodes;
	}

	/**
	 * @param usedNodes the usedNodes to set
	 */
	public void setUsedNodes(Set<String> usedNodes) {
		this.usedNodes = usedNodes;
	}

	/**
	 * @param changedJobs_ the changedJobs_ to set
	 */
	public void setChangedJobs(Map<String, JobInfo> changedJobs) {
		this.changedJobs_ = changedJobs;
	}

	/**
	 * @return the changedJobs_
	 */
	public Map<String, JobInfo> getChangedJobs() {
		return changedJobs_;
	}

	/**
	 * @param rmKey_ the rmKey_ to set
	 */
	public void setRmKey(String rmKey_) {
		RmKey_ = rmKey_;
	}

	/**
	 * @return the rmKey_
	 */
	public String getRmKey() {
		return RmKey_;
	}

	/**
	 * @param newJobs_ the newJobs_ to set
	 */
	public void setNewJobs(Map<String, JobInfo> newJobs) {
		this.newJobs_ = newJobs;
	}

	/**
	 * @return the newJobs_
	 */
	public Map<String, JobInfo> getNewJobs() {
		return newJobs_;
	}

	/**
	 * @param deletedJobs_ the deletedJobs_ to set
	 */
	public void setDeletedJobs(Map<String, JobInfo> deletedJobs) {
		this.deletedJobs_ = deletedJobs;
	}

	/**
	 * @return the deletedJobs_
	 */
	public Map<String, JobInfo> getDeletedJobs() {
		return deletedJobs_;
	}

	/* (non-Javadoc)
	 * @see f4gcom.ICom#dispose()
	 */
	@Override
	public boolean dispose() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return the coreUsagePerNodeMap
	 */
	public Map<String, double[]> getCoreUsagePerNodeMap() {
		return coreUsagePerNodeMap;
	}

	/**
	 * @return the coreFrequencyPerNodeMap
	 */
	public Map<String, double[]> getCoreFrequencyPerNodeMap() {
		return coreFrequencyPerNodeMap;
	}

	/**
	 * @return the coreVoltagePerNodeMap
	 */
	public Map<String, double[]> getCoreVoltagePerNodeMap() {
		return coreVoltagePerNodeMap;
	}

	/**
	 * @return the memoryUsagePerNodeMap
	 */
	public Map<String, Double> getMemoryUsagePerNodeMap() {
		return memoryUsagePerNodeMap;
	}

	/**
	 * @return the fanActualRPMPerNodeMap
	 */
	public Map<String, int[]> getFanActualRPMPerNodeMap() {
		return fanActualRPMPerNodeMap;
	}

	/**
	 * @return the storageUnitReadRatePerNodeMap
	 */
	public Map<String, Double> getStorageUnitReadRatePerNodeMap() {
		return storageUnitReadRatePerNodeMap;
	}

	/**
	 * @return the storageUnitWriteRatePerNodeMap
	 */
	public Map<String, Double> getStorageUnitWriteRatePerNodeMap() {
		return storageUnitWriteRatePerNodeMap;
	}

//	public void postMail( String recipients[ ], String subject, String message , String from) throws MessagingException
//	{
//		boolean debug = false;
//
//		//Set the host smtp address
//		Properties props = new Properties();
//		props.put("mail.smtp.host", "smtp.jcom.net");
//
//		// create some properties and get the default Session
//		Session session = Session.getDefaultInstance(props, null);
//		session.setDebug(debug);
//
//		// create a message
//		Message msg = new MimeMessage(session);
//
//		// set the from and to address
//		InternetAddress addressFrom = new InternetAddress(from);
//		msg.setFrom(addressFrom);
//
//		InternetAddress[] addressTo = new InternetAddress[recipients.length]; 
//		for (int i = 0; i < recipients.length; i++)
//		{
//			addressTo[i] = new InternetAddress(recipients[i]);
//		}
//		msg.setRecipients(Message.RecipientType.TO, addressTo);
//
//
//		// Optional : You can also set your custom headers in the Email if you Want
//		msg.addHeader("MyHeaderName", "myHeaderValue");
//
//		// Setting the Subject and Content Type
//		msg.setSubject(subject);
//		msg.setContent(message, "text/plain");
//		Transport.send(msg);
//	}

	class WakeUpWorker extends Thread{
		PowerOnActionType action;
		String name;
		HashMap<String, Boolean> wakeupSuccessMap;

		public WakeUpWorker(ThreadGroup group, String name, PowerOnActionType action, HashMap<String, Boolean> wakeupSuccessMap)
		{
			super(group, name);
			this.action = action;
			this.name = name;
			this.wakeupSuccessMap = wakeupSuccessMap;
		}		 

		public void run()
		{
			if(!powerOn(action)){
				log.info("Could not wake up node " + name);
				action.setForwarded(false);
				wakeupSuccessMap.put(name, false);				
			}
			else{
				wakeupSuccessMap.put(name, true);	
			}
		}
	}

	class SetStandbyWorker extends Thread{
		StandByActionType action;
		String name;
		HashMap<String, Boolean> standbySuccessMap;

		public SetStandbyWorker(ThreadGroup group, String name, StandByActionType action, HashMap<String, Boolean> standbySuccessMap)
		{
			super(group, name);
			this.action = action;
			this.name = name;
			this.standbySuccessMap = standbySuccessMap;
		}		 

		public void run()
		{
			if(!standBy(action)){
				log.info("Could not set node " + name + " to standby");
				action.setForwarded(false);
				standbySuccessMap.put(name, false);				
			}
			else{
				standbySuccessMap.put(name, true);	
			}
		}
	}
}