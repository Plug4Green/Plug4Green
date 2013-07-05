/**
* ============================== Header ============================== 
* file:          JobAllocationRequest.java
* project:       FIT4Green/CommunicatorFzj
* created:       25.08.2011 by agiesler
* 
* $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1244 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com.unicore;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author agiesler
 */
@XmlRootElement
public class JobAllocationRequest {
	String id;
	int num_of_nodes; 
	int needed_cores;
	int mem; 
	long walltime; 
	boolean energy_aware; 
	long latest_finish; 
	String suitable_clusters;
	String clusterID;
	String benchmark_id;	
	
	/**
	 * @return the clusterID
	 */
	public String getClusterID() {
		return clusterID;
	}
	/**
	 * @param clusterID the clusterID to set
	 */
	public void setClusterID(String clusterID) {
		this.clusterID = clusterID;
	}
	/**
	 * @return the num_of_nodes
	 */
	public int getNum_of_nodes() {
		return num_of_nodes;
	}
	/**
	 * @param num_of_nodes the num_of_nodes to set
	 */
	public void setNum_of_nodes(int num_of_nodes) {
		this.num_of_nodes = num_of_nodes;
	}
	/**
	 * @return the needed_cores
	 */
	public int getNeeded_cores() {
		return needed_cores;
	}
	/**
	 * @param needed_cores the needed_cores to set
	 */
	public void setNeeded_cores(int needed_cores) {
		this.needed_cores = needed_cores;
	}
	/**
	 * @return the mem
	 */
	public int getMem() {
		return mem;
	}
	/**
	 * @param mem the mem to set
	 */
	public void setMem(int mem) {
		this.mem = mem;
	}
	/**
	 * @return the walltime
	 */
	public long getWalltime() {
		return walltime;
	}
	/**
	 * @param walltime the walltime to set
	 */
	public void setWalltime(long walltime) {
		this.walltime = walltime;
	}
	/**
	 * @return the energy_aware
	 */
	public boolean isEnergy_aware() {
		return energy_aware;
	}
	/**
	 * @param energy_aware the energy_aware to set
	 */
	public void setEnergy_aware(boolean energy_aware) {
		this.energy_aware = energy_aware;
	}
	/**
	 * @return the latest_finish
	 */
	public long getLatest_finish() {
		return latest_finish;
	}
	/**
	 * @param latest_finish the latest_finish to set
	 */
	public void setLatest_finish(long latest_finish) {
		this.latest_finish = latest_finish;
	}
	/**
	 * @return the suitable_clusters
	 */
	public String getSuitable_clusters() {
		return suitable_clusters;
	}
	/**
	 * @param suitable_clusters the suitable_clusters to set
	 */
	public void setSuitable_clusters(String suitable_clusters) {
		this.suitable_clusters = suitable_clusters;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the benchmark_linpack_id
	 */
	public String getBenchmark_id() {
		return benchmark_id;
	}
	/**
	 * @param benchmark_linpack_id the benchmark_linpack_id to set
	 */
	public void setBenchmark_id(String benchmark_id) {
		this.benchmark_id = benchmark_id;
	}
}
