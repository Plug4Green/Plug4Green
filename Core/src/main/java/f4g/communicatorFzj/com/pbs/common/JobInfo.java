/**
* ============================== Header ============================== 
* file:          JobInfo.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-12-19 13:36:43 +0100 (Mo, 19 Dez 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1149 $
* 
* short description:
*   Structure to hold Informations about a job
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * Structure to hold Informations about a job
 *
 * @author Daniel Brinkers, Andre Giesler
 */
public class JobInfo implements Serializable, Cloneable{

	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(JobInfo.class.getName());
	
	private static final long serialVersionUID = 1L;
	
	private String id_;
	private NodeInfo frameworkRef_;
	private int priority_;
	private int nNodes_;
	private int nCores_;
	private double memory_;
	//Set?
	private String architecture_;
	private long wallTime_;
	private long submitTime_;
	private long startTime_;
	private long runtime_;
	private long queuedtime_;
	//Later use
	private Set<String> properties_;
	private Set<String> nodes_;
	private State state_;
	
	/**
	 * The status of a job in the RM system
	 */
	public enum State{
		QUEUED,
		RUNNING,
		EXITING,
		COMPLETED,
		HELD,
		MOVED,
		WAITING
	}
	
	/**
	 * Constructs the class based on the parsed information of the RMS response 
	 * @param status the RMS response
	 */
	JobInfo(DisResponseStatus.Status status){
		String str;

		setId(status.name);
		log.debug("Job Id: " + status.name);
		//later Priority? + config + queue + ?
		setPriority(0);
		Map<String, String> res = status.resources.get("Resource_List");
		setnNodes(1);
		setnCores(1);
		setMemory(0);
		setArchitecture("x86_64");
		setWallTime(0);
		setSubmitTime(-1);
		setStartTime(-1);
		
		setProperties(null);
		setNodes(null);
		setState(State.QUEUED);
		
		if(res != null){
			str = res.get("neednodes");
			log.debug("Needed nodes and cores: " + str);
			if(str != null){
				str = str.replace("+",",");
				log.trace("After re-format: " + str);
				if(str.contains(",")){
					//neednodes were already altered by plugin
					Set<String> nodesSet = new HashSet<String>();
					Scanner topsc = new Scanner(str).useDelimiter(",");
					int i = 0;
					while (topsc.hasNext()) {
				          String top_str = topsc.next();
				          Scanner sc = new Scanner(top_str).useDelimiter(":ppn=");
				          i++;
				          nodesSet.add(sc.next());
				          setnCores(sc.nextInt());
				      }
					setnNodes(i);
					//Just for Fit4Green tests
					if (i>=8){
						//setPriority(1);
					}
					setNodes(nodesSet);
				}
				else {
					Scanner sc = new Scanner(str).useDelimiter(":ppn=");
					
					Set<String> nodesSet = new HashSet<String>();
					String s = sc.next();
					//log.debug("Got from Scanner: " + s);
					try{
						int n = Integer.parseInt(s);
						setnNodes(n);
						log.debug("Set nr of node of Job to : " + n);
						//Just for Fit4Green tests
						if (n>=8){
							//setPriority(1);
						}
					}
					catch(Exception ex){
						setnNodes(1);
						nodesSet.add(s);
						setNodes(nodesSet);
						log.debug("Could not parse other nodes: Set node of Job to : " + s);
					}
					s = sc.next();
					log.trace("Got from Scanner: " + s);
					try{
						int n = Integer.parseInt(s);
						setnCores(n);
					}
					catch(Exception ex){
						log.error("Cannot get cores");
					}
				}
			}
					
			str = res.get("mem");
			if(str != null){
				setMemory(Memory.parse(str));
			}
			str = res.get("walltime");
			if(str != null){
				Scanner sc = new Scanner(str).useDelimiter(":");
				int wallTime = 0;
				wallTime += sc.nextInt() * 3600;
				wallTime+= sc.nextInt() * 60;
				wallTime += sc.nextInt();
				//wallTime *= 1000;
				setWallTime(wallTime);
				log.debug("Set walltime[seconds]: " + wallTime);
			}
		}
		if((str = status.attributes.get("qtime")) != null){
			setSubmitTime(Long.decode(str));
			log.debug("Set Submit time: " + Long.decode(str));
		}
		if((str = status.attributes.get("start_time")) != null){
			setStartTime(Long.decode(str));
			log.debug("Set Start time: " + Long.decode(str));
		}
		if((str = status.attributes.get("exec_host")) != null){
			setNodes(new TreeSet<String>());
			String all[] = str.split("\\+");
			for(int i=0; i<all.length; ++i){
				String pair[] = all[i].split("/");
				if(getNodes().add(pair[0]))
					log.debug("Add exec host: " + pair[0]);
			}
		}
		if((str = status.attributes.get("job_state")) != null){
			log.debug("Job state: " + str);
			switch(str.charAt(0)){
			case 'Q':
				setState(State.QUEUED);
				break;
			case 'E':
			case 'C':
			case 'R':
				setState(State.RUNNING);
				break;			
			default:
				//no other states implemented
				setState(State.QUEUED);
			}
		}
	}
	
	public JobInfo() {
		setId("");
		setPriority(0);
		setnNodes(1);
		setnCores(1);
		setMemory(0);
		setArchitecture("x86_64");
		setWallTime(0);
		setSubmitTime(-1);
		setStartTime(-1);
		setProperties(null);
		setNodes(null);
		setState(State.QUEUED);
	}

	/**
	 * Returns the object as String
	 * @return all information in a readable format as String.
	 */
	public String toString(){
		String ret = "";
		ret += getId() + "\n";
		ret += getPriority() + "\n";
		ret += getnNodes() + "\n";
		ret += getnCores() + "\n";
		ret += getMemory() + "\n";
		ret += getArchitecture() + "\n";
		ret += getWallTime() + "\n";
		ret += getSubmitTime() + "\n";
		ret += getStartTime() + "\n";
		ret += getProperties() + "\n";
		if(getNodes() != null){
			Iterator<String> iter = getNodes().iterator();
			while(iter.hasNext()){
				String elem = iter.next();
				ret += elem + " ";
			}
			ret += "\n";
		}else
			ret += "null\n";
		return ret;
	}
	/**
	 * Returns whether to instances have the same values.
	 * @param obj the object to compare.
	 * @return If the instances hold the same values.
	 */
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof JobInfo))
			return false;
		JobInfo o = (JobInfo)obj;

		if(!getId().equals(o.getId()))
			return false;
		if(getPriority() != o.getPriority())
			return false;
		if(getnNodes() != o.getnNodes())
			return false;
		if(getnCores() != o.getnCores())
			return false;
		if(getMemory() != o.getMemory())
			return false;
		if(!getArchitecture().equals(o.getArchitecture()))
			return false;
		if(getWallTime() != o.getWallTime())
			return false;
		if(getSubmitTime() != o.getSubmitTime())
			return false;
		if(getStartTime() != o.getStartTime())
			return false;
		if(!getProperties().equals(o.getProperties()))
			return false;
		if(!getNodes().equals(o.getNodes()))
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
		code ^= getPriority();
		code ^= getnNodes();
		code ^= getnCores();
		//code ^= (int)(getMemory()>>32);
		if(getArchitecture() != null)
			code ^= getArchitecture().hashCode();
		code ^= (int)(getWallTime()>>32);
		code ^= (int)(getSubmitTime()>>32);
		code ^= (int)(getStartTime()>>32);
		if(getProperties() != null)
			code ^= getProperties().hashCode();
		if(getNodes() != null)
			code ^= getNodes().hashCode();
		return code;
	}
	
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
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
	 * @return the frameworkRef_
	 */
	public NodeInfo getFrameworkRef_() {
		return frameworkRef_;
	}

	/**
	 * @param frameworkRef_ the frameworkRef_ to set
	 */
	public void setFrameworkRef_(NodeInfo frameworkRef_) {
		this.frameworkRef_ = frameworkRef_;
	}

	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority) {
		this.priority_ = priority;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority_;
	}

	/**
	 * @param nNodes the nNodes to set
	 */
	public void setnNodes(int nNodes) {
		this.nNodes_ = nNodes;
	}

	/**
	 * @return the nNodes
	 */
	public int getnNodes() {
		return nNodes_;
	}

	/**
	 * @param memory the memory to set
	 */
	public void setMemory(long memory) {
		long size=memory;
		long s=0;
		if(size>(1024*1024)){
			s=size/(1024*1024);
			log.debug("Set Memory= [" + s + " Mb]");				
			this.memory_ = s;
		}	
		else{
			this.memory_ = memory;
		}
	}

	/**
	 * @return the memory
	 */
	public double getMemory() {
		return memory_;
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
	 * @param wallTime the wallTime to set
	 */
	public void setWallTime(long wallTime) {
		this.wallTime_ = wallTime;
	}

	/**
	 * @return the wallTime
	 */
	public long getWallTime() {
		return wallTime_;
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
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime_ = startTime;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime_;
	}

	/**
	 * @param submitTime the submitTime to set
	 */
	public void setSubmitTime(long submitTime) {
		this.submitTime_ = submitTime;
	}

	/**
	 * @return the submitTime
	 */
	public long getSubmitTime() {
		return submitTime_;
	}
	

	/**
	 * @return the queuedtime_
	 */
	public long getQueuedtime_() {
		return queuedtime_;
	}

	/**
	 * @param queuedtime_ the queuedtime_ to set
	 */
	public void setQueuedtime_(long queuedtime_) {
		this.queuedtime_ = queuedtime_;
	}

	/**
	 * @return the runtime_
	 */
	public long getRuntime_() {
		return runtime_;
	}

	/**
	 * @param runtime_ the runtime_ to set
	 */
	public void setRuntime_(long runtime_) {
		this.runtime_ = runtime_;
	}

	/**
	 * @param nodes the nodes to set
	 */
	public void setNodes(Set<String> nodes) {
		this.nodes_ = nodes;
	}

	/**
	 * @return the nodes
	 */
	public Set<String> getNodes() {
		return nodes_;
	}

	/**
	 * @param state_ the state_ to set
	 */
	public void setState(State state) {
		this.state_ = state;
	}

	/**
	 * @return the state_
	 */
	public State getState() {
		return state_;
	}
}
