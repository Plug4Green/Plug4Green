/**
* ============================== Header ============================== 
* file:          DisRequestAuth.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-05 17:17:43 +0200 (Di, 05 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 640 $
* 
* short description:
*   DIS encoded authorization request
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;

/**
 * A DIS encoded request to authorize a certain connection to belong to the user root
 * (for PBS internal rights management)
 * 
 * @author Daniel Brinkers
 */
public class DisRequestAuth extends DisRequest {

	/**
	 * Creates a DIS encoded request for authorization a connection using a specific port as root 
	 * 
	 * @param port the outgoing port, which should be authorized as being used by root (PBS user)
	 * @return DisRequest for this authorization request
	 *
	 * @author Daniel Brinkers
	 */
	
	static Logger log = Logger.getLogger(DisRequestAuth.class.getName());
	
	public static DisRequest make(int port) {
		DisRequest disRequest = new DisRequestAuth();
		disRequest.addHeader(Request.AUTHENUSER);
		disRequest.addInt(port);
		disRequest.addExtension();
		log.trace("Making DisRequestAuth: " +  disRequest.get());
		return disRequest;
	}
}
