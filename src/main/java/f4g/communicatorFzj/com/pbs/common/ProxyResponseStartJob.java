/**
* ============================== Header ============================== 
* file:          ProxyResponseStartJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       05.10.2011 by agiesler
* 
* $LastChangedDate: 2011-10-07 11:14:56 +0200 (Fr, 07 Okt 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 864 $
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
public class ProxyResponseStartJob extends ProxyResponse {
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
