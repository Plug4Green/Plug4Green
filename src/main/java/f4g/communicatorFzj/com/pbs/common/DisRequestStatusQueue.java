/**
* ============================== Header ============================== 
* file:          DisRequestStatusQueue.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   DIS encoded request for the status of all queues
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;

/**
 * DIS encoded request for the status of all queues
 * 
 *
 * @author Daniel Brinkers
 */
public class DisRequestStatusQueue extends DisRequest {
	/**
	 * Creates a request
	 * 
	 * @return DisRequest for this status request
	 *
	 * @author Daniel Brinkers
	 */
	
	static Logger log = Logger.getLogger(DisRequestStatusQueue.class.getName());
	
	static DisRequest make(){
		DisRequest disRequest = new DisRequestStatusQueue();
		disRequest.addHeader(RequestType.STATUSQUEUE);
		disRequest.addInt(0);
		disRequest.addInt(0);
		disRequest.addExtension();
		log.debug("Making DisRequestStatusQueue: " +  disRequest.get());
		return disRequest;
	}
}
