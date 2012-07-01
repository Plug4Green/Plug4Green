/**
* ============================== Header ============================== 
* file:          NodeInfo.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1244 $
* 
* short description:
*   Structure for the information about a node
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Structure for the information about a node
 * @author Daniel Brinkers, Andre Giesler
 */
public class NodeInfo implements Serializable, Cloneable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(NodeInfo.class.getName());
	
	private String id_ = null;
	private String architecture_;
	private int nCores_;
	private int numOfUsedCores;
	private double loadAve;
	private long idletime;
	private Set<String> jobRefs;
	private long memory_;
	private String[] jobs_;
	private Set<String> properties_;
	//add loads
	private State state_;
	private Governor governor;
	private double cpuUsage;
	private double[] coreUsages;
	private double[] coreFrequencies;
	private double[] coreVoltage;
	private double memoryUsage;
	private int[] fanActualRPMs;
	private double storageUnitReadRate;
	private double storageUnitWriteRate;
	private long comstarttime = 0;
	private long stateSwitchTime = 0;
	private long downTime = 0;
	private long upTime = 0;
	private long powersaveTime = 0;
	private long performanceTime = 0;
	
	/**
	 * status of the node in the RMS system (+some F4g stati)
	 */
	public enum State{
		DOWN,
		IDLE,
		RESERVED,
		BUSY,
		RUNNING,
		STANDBY,
		HYBERNATED,
		OFF
	}
	
	public enum Governor{
		POWERSAVE,
		ONDEMAND,
		USERSPACE,
		PERFORMANCE
	}
	
	boolean jobAlreadyInList(String jobId){
		String[] jobs = getJobs();
		for(int i = 0; i< jobs.length; i++){
			if(jobId.equals(jobs[i])){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Create the structure based on a parsed RMS response
	 * @param status the parsed response from the RMS
	 */
	/*
	 * Similar to the job version of this function
	 */
	NodeInfo(DisResponseStatus.Status status){
		String str;

		
		setId(status.name);
		setArchitecture("x86_64");
		setnCores(1);
		setNumOfUsedCores(0);
		setMemory(0);
		setJobs(new String[getnCores()]);
		setProperties(null);
		setJobRefs(new HashSet<String>());
		setDownTime(0);
		setUpTime(0);
		setPowersaveTime(0);
		setPerformanceTime(0);
		setComstarttime(0);
		setStateSwitchTime(0);

		str = status.attributes.get("state");
		str = str.trim();
		log.debug("State of node " + getId() +" is: " + str);
		if(str != null){
			if(str.contains("free")){
				setState(State.IDLE);
			}
			else if(str.contains("job-exclusive")){
				setState(State.RUNNING);
			}else if(str.contains("busy")){
				setState(State.RUNNING);
			}else if(str.contains("down")||str.contains("offline")){
				setState(State.STANDBY);
			}else if(str.equals("reserved")){
				setState(State.RESERVED);
			}else{
				setState(State.IDLE);
			}			
		}
		
		str = status.attributes.get("np");
		if(str != null){
			setnCores(Integer.decode(str));			
			setJobs(new String[getnCores()]);
			log.debug("Number of available cores " + getnCores());
		}
		
		str = status.attributes.get("jobs");
		if(str != null){
			log.debug("Identified job id " + str);
			
			//Set state to running since job was detected on node
			setState(State.RUNNING);
			
			String[] j = str.split(",");
			log.debug("Number of used cores " + j.length);
			int free_cores = getnCores() - j.length;
			log.debug("Number of free cores: " + free_cores);
			setNumOfUsedCores(j.length);
			
			int skip = 0;
			//String jobRef = "";
			for(int i=0; i<j.length; i++){
				if(j[i].length() == 0)
					++skip;
				else{
					String[] m = j[i].split("/");
					String jobId = m[1];
					jobId = jobId.trim();
					if(!jobAlreadyInList(jobId)){
						getJobs()[i-skip] = jobId;
						//log.debug("Added identified job id " + jobId);
						jobId = "_" + jobId;
						getJobRefs().add(jobId);
						log.debug("Added to JobRefs: " + jobId);
					}
				}
			}
		}
		else{
			log.debug("No jobs detected on node");
		}
		
		str = status.attributes.get("status");
		if(str != null){
			log.trace("status of node " + str);
			String t;
			Map<String, String> map = new HashMap<String, String>();
			String[] all = str.split(",");
			for(int i=0; i<all.length; ++i){
				String[] pair = all[i].split("=");
				if(pair.length == 1)
					map.put(pair[0], "");
				else
					map.put(pair[0], pair[1]);
			}
			t = map.get("physmem");
			if(t != null){
				setMemory(Memory.parse(t));
			}
			
			t = map.get("loadave");
			if(t != null){
				log.trace("load average " + t);
				setLoadAve(Double.parseDouble(t));
			}
			
			t = map.get("idletime");
			if(t != null){
				setIdletime(Integer.parseInt(t));
			}

//			t = map.get("jobs");
//			//log.debug("Job unsplitted " + t);
//			if(t != null){
//				String[] j = t.split(" ");
//				//log.debug("Job String " + t);
//				int skip = 0;
//				//for(int i=0; i<j.length; ++i){
//					for(int i=0; i<j.length; i++){
//					if(j[i].length() == 0)
//						++skip;
//					else
//						getJobs()[i-skip] = j[i];
//				}
//			}
		}
	}
	
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
	
	
	/**
	 * Builds a String of the Object.
	 * @return A String containing all relevant values in a readable format.
	 */
	public String toString(){
		String ret = "";
		ret += getId() + "\n";
		ret += getArchitecture() + "\n";
		ret += getnCores() + "\n";
		ret += getMemory() + "\n";
		for(int i=0; i<getJobs().length; ++i)
			ret += getJobs()[i] + " ";
		ret += "\n";
		ret += getProperties() + "\n";
		return ret;
	}

	/**
	 * Returns whether to instances have the same values.
	 * @param obj the object to compare.
	 * @return If the instances hold the same values.
	 */
	@Override
	public boolean equals(Object ob){
		if(!(ob instanceof NodeInfo))
			return false;
		NodeInfo o = (NodeInfo)ob;

		if(!getId().equals(o.getId()))
			return false;
		if(!getArchitecture().equals(o.getArchitecture()))
			return false;
		if(getnCores() != o.getnCores())
			return false;
		if(getMemory() != o.getMemory())
			return false;
		if(!getJobs().equals(o.getJobs()))
			return false;
		if(!getProperties().equals(o.getProperties()))
			return false;
		return true;
	}
	
	/**
	 * Returns a hash code.
	 * @return hash code of the data values.
	 */
	@Override
	public int hashCode(){
		int code = 0;
		if(getId() != null)
			code ^= getId().hashCode();
		if(getArchitecture() != null)
			code ^= getArchitecture().hashCode();
		code ^= getnCores();
		code ^= getMemory();
		if(getJobs() != null)
			code ^= getJobs().hashCode();
		if(getProperties() != null)
			code ^= getProperties().hashCode();
		return code;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id_ = id;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id_;
	}

	/**
	 * @param architecture the architecture to set
	 */
	public void setArchitecture(String architecture) {
		this.architecture_ = architecture;
	}

	/**
	 * @return the architecture
	 */
	public String getArchitecture() {
		return architecture_;
	}

	/**
	 * @param nCores the nCores to set
	 */
	public void setnCores(int nCores) {
		this.nCores_ = nCores;
	}

	/**
	 * @return the nCores
	 */
	public int getnCores() {
		return nCores_;
	}

	/**
	 * @param memory the memory to set
	 */
	public void setMemory(long memory) {
		this.memory_ = memory;
	}

	/**
	 * @return the memory
	 */
	public long getMemory() {
		return memory_;
	}
	
	/**
	 * @return the idletime
	 */
	public long getIdletime() {
		return idletime;
	}

	/**
	 * @param idletime the idletime to set
	 */
	public void setIdletime(long idletime) {
		this.idletime = idletime;
	}

	/**
	 * @return the loadAve
	 */
	public double getLoadAve() {
		return loadAve;
	}

	/**
	 * @param loadAve the loadAve to set
	 */
	public void setLoadAve(double loadAve) {
		this.loadAve = loadAve;
	}

	/**
	 * @param jobs the jobs to set
	 */
	public void setJobs(String[] jobs) {
		this.jobs_ = jobs;
	}

	/**
	 * @return the jobs
	 */
	public String[] getJobs() {
		return jobs_;
	}
	
	/**
	 * @return the jobRefs
	 */
	public Set<String> getJobRefs() {
		return jobRefs;
	}

	/**
	 * @param jobRefs the jobRefs to set
	 */
	public void setJobRefs(Set<String> jobRefs) {
		this.jobRefs = jobRefs;
	}


	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Set<String> properties) {
		this.properties_ = properties;
	}

	/**
	 * @return the properties
	 */
	public Set<String> getProperties() {
		return properties_;
	}

	/**
	 * @param state_ the state_ to set
	 */
	public void setState(State state_) {
		this.state_ = state_;
	}

	/**
	 * @return the state_
	 */
	public State getState() {
		return state_;
	}
	
	/**
	 * @param state_ the state_ to set
	 */
	public void setGovernor(Governor governor_) {
		this.governor = governor_;
	}

	/**
	 * @return the state_
	 */
	public Governor getGovernor() {
		return governor;
	}
	
	/**
	 * @return the numOfUsedCores
	 */
	public int getNumOfUsedCores() {
		return numOfUsedCores;
	}
	
	/**
	 * @param numOfUsedCores the numOfUsedCores to set
	 */
	public void setNumOfUsedCores(int numOfUsedCores) {
		this.numOfUsedCores = numOfUsedCores;
	}

	/**
	 * @return the cpuUsage
	 */
	public double getCpuUsage() {
		return cpuUsage;
	}

	/**
	 * @param cpuUsage the cpuUsage to set
	 */
	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	/**
	 * @return the coreUsages
	 */
	public double[] getCoreUsages() {
		return coreUsages;
	}

	/**
	 * @param coreUsages the coreUsages to set
	 */
	public void setCoreUsages(double[] coreUsages) {
		this.coreUsages = coreUsages;
	}

	/**
	 * @return the coreFrequencies
	 */
	public double[] getCoreFrequencies() {
		return coreFrequencies;
	}

	/**
	 * @param coreFrequencies the coreFrequencies to set
	 */
	public void setCoreFrequencies(double[] coreFrequencies) {
		this.coreFrequencies = coreFrequencies;
	}

	/**
	 * @return the coreVoltage
	 */
	public double[] getCoreVoltage() {
		return coreVoltage;
	}

	/**
	 * @param coreVoltage the coreVoltage to set
	 */
	public void setCoreVoltage(double[] coreVoltage) {
		this.coreVoltage = coreVoltage;
	}

	/**
	 * @return the memoryUsage
	 */
	public double getMemoryUsage() {
		return memoryUsage;
	}

	/**
	 * @param memoryUsage the memoryUsage to set
	 */
	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	/**
	 * @return the fanActualRPMs
	 */
	public int[] getFanActualRPMs() {
		return fanActualRPMs;
	}

	/**
	 * @param fanActualRPMs the fanActualRPMs to set
	 */
	public void setFanActualRPMs(int[] fanActualRPMs) {
		this.fanActualRPMs = fanActualRPMs;
	}

	/**
	 * @return the storageUnitReadRate
	 */
	public double getStorageUnitReadRate() {
		return storageUnitReadRate;
	}

	/**
	 * @param storageUnitReadRate the storageUnitReadRate to set
	 */
	public void setStorageUnitReadRate(double storageUnitReadRate) {
		this.storageUnitReadRate = storageUnitReadRate;
	}

	/**
	 * @return the storageUnitWriteRate
	 */
	public double getStorageUnitWriteRate() {
		return storageUnitWriteRate;
	}

	/**
	 * @param storageUnitWriteRate the storageUnitWriteRate to set
	 */
	public void setStorageUnitWriteRate(double storageUnitWriteRate) {
		this.storageUnitWriteRate = storageUnitWriteRate;
	}

	/**
	 * @return the comstarttime
	 */
	public long getComstarttime() {
		return comstarttime;
	}

	/**
	 * @param comstarttime the comstarttime to set
	 */
	public void setComstarttime(long comstarttime) {
		this.comstarttime = comstarttime;
	}
	

	/**
	 * @return the downTime
	 */
	public long getDownTime() {
		return downTime;
	}

	/**
	 * @param downTime the downTime to set
	 */
	public void setDownTime(long downTime) {
		this.downTime = downTime;
	}

	/**
	 * @return the runningTime
	 */
	public long getUpTime() {
		return upTime;
	}

	/**
	 * @param runningTime the runningTime to set
	 */
	public void setUpTime(long runningTime) {
		this.upTime = runningTime;
	}

	/**
	 * @return the powersaveTime
	 */
	public long getPowersaveTime() {
		return powersaveTime;
	}

	/**
	 * @param powersaveTime the powersaveTime to set
	 */
	public void setPowersaveTime(long powersaveTime) {
		this.powersaveTime = powersaveTime;
	}

	/**
	 * @return the performanceTime
	 */
	public long getPerformanceTime() {
		return performanceTime;
	}

	/**
	 * @param performanceTime the performanceTime to set
	 */
	public void setPerformanceTime(long performanceTime) {
		this.performanceTime = performanceTime;
	}

	/**
	 * @return the stateSwitchTime
	 */
	public long getStateSwitchTime() {
		return stateSwitchTime;
	}

	/**
	 * @param stateSwitchTime the stateSwitchTime to set
	 */
	public void setStateSwitchTime(long stateSwitchTime) {
		this.stateSwitchTime = stateSwitchTime;
	}

	
	
	
}
