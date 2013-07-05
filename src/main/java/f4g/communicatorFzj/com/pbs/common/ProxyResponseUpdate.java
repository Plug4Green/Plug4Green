/**
* ============================== Header ============================== 
* file:          ProxyResponseUpdate.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   The response for a update request
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

/**
 * The response for a update request
 * @see Proxy
 * @see ProxyRequestUpdate
 * 
 * @author Daniel Brinkers, Andre Giesler
 */
public class ProxyResponseUpdate extends ProxyResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private NodeInfo[] nodeInfo_;
	private JobInfo[] jobInfo_;
	private String power_Consumption;
	
	/**
	 * @return the power_Consumption
	 */
	public String getPower_Consumption() {
		return power_Consumption;
	}
	/**
	 * @param power_Consumption the power_Consumption to set
	 */
	public void setPower_Consumption(String power_Consumption) {
		this.power_Consumption = power_Consumption;
	}
	/**
	 * @param nodeInfo the nodeInfo to set
	 */
	public void setNodeInfo(NodeInfo[] nodeInfo) {
		this.nodeInfo_ = nodeInfo;
	}
	/**
	 * @return the nodeInfo
	 */
	public NodeInfo[] getNodeInfo() {
		return nodeInfo_;
	}
	/**
	 * @param jobInfo the jobInfo to set
	 */
	public void setJobInfo(JobInfo[] jobInfo) {
		this.jobInfo_ = jobInfo;
	}
	/**
	 * @return the jobInfo
	 */
	public JobInfo[] getJobInfo() {
		return jobInfo_;
	}
}
