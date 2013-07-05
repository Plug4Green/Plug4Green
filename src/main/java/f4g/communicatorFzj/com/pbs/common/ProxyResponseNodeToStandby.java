/**
* ============================== Header ============================== 
* file:          ProxyResponsePowerOff.java
* project:       FIT4Green/CommunicatorFzj
* created:       05.10.2011 by agiesler
* 
* $LastChangedDate: 2012-02-21 19:21:17 +0100 (Tue, 21 Feb 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1167 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author agiesler
 */
public class ProxyResponseNodeToStandby extends ProxyResponse {
	private static final long serialVersionUID = 1L;
	private boolean success;
	private String message;
	
	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}
	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	} 
}
