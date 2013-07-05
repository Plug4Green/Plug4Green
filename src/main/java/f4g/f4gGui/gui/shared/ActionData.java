/**
* ============================== Header ============================== 
* file:          ActionData.java
* project:       FIT4Green/F4gGui
* created:       14 dec 2010 by jos@almende.org
* 
* $LastChangedDate: 2011-09-07 16:08:44 +0200 (wo, 07 sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 716 $
* 
* short description:
*   Wrapper class for action related data
* ============================= /Header ==============================
*/
package org.f4g.gui.shared;

import java.io.Serializable;
import java.util.Date;

/**
 * Wrapper class for action related data
 * 
 * @author Jos de Jong, Vasiliki Georgiadou
 */
public class ActionData implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private boolean automatic;
	
	private String operatorName;
	
	private String comName = "";
	
	private String action = "";
	private String nodeName = "";
	
	private String srcNodeName = "";
	private String dstNodeName = "";
	private String virtualMachine = "";
	
	private String jobID = "";
	
	private Date lastUpdateTimestamp = null;
	
	private boolean obsolete;
	
	/**
	 * @return the automatic
	 */
	public boolean isAutomatic() {
		return automatic;
	}
	/**
	 * @param automatic the automatic to set
	 */
	public void setAutomatic(boolean automatic) {
		this.automatic = automatic;
	}
	/**
	 * @return the operatorName
	 */
	public String getOperatorName() {
		return operatorName;
	}
	/**
	 * @param operatorName the operatorName to set
	 */
	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}
	
	public String getComName() {
		return comName;
	}
	public void setComName(String comName) {
		this.comName = comName;
	}
	
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getNodeName() {
		return nodeName;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}	
	
	public Date getLastUpdateTimestamp() {
		return lastUpdateTimestamp;
	}
	public void setLastUpdateTimestamp(Date lastUpdateTimestamp) {
		this.lastUpdateTimestamp = lastUpdateTimestamp;
	}
	public boolean isObsolete() {
		return obsolete;
	}
	public void setObsolete(boolean obsolete) {
		this.obsolete = obsolete;
	}
	public String getSrcNodeName() {
		return srcNodeName;
	}
	public void setSrcNodeName(String srcNodeName) {
		this.srcNodeName = srcNodeName;
	}
	public String getDstNodeName() {
		return dstNodeName;
	}
	public void setDstNodeName(String dstNodeName) {
		this.dstNodeName = dstNodeName;
	}
	public String getVirtualMachine() {
		return virtualMachine;
	}
	public void setVirtualMachine(String virtualMachine) {
		this.virtualMachine = virtualMachine;
	}
	public String getJobID() {
		return jobID;
	}
	public void setJobID(String jobID) {
		this.jobID = jobID;
	}
	
}
