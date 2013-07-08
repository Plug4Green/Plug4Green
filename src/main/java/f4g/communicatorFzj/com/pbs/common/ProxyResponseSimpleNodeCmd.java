/**
* ============================== Header ============================== 
* file:          ProxyResponseSimpleNodeCommand.java
* project:       FIT4Green/CommunicatorFzj
* created:       27.02.2012 by agiesler
* 
* $LastChangedDate:$ 
* $LastChangedBy:$
* $LastChangedRevision:$
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author agiesler
 */
public class ProxyResponseSimpleNodeCmd extends ProxyResponse {
	/**
	 * 
	 */
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
