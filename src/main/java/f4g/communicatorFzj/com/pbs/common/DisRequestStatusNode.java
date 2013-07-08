/**
* ============================== Header ============================== 
* file:          DisRequestStatusNode.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-05 17:17:43 +0200 (Di, 05 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 640 $
* 
* short description:
*   DIS encoded request for the status of all nodes
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;

/**
 *  DIS encoded request for the status of all nodes
 *
 * @author Daniel Brinkers
 */
public class DisRequestStatusNode extends DisRequest {

	static Logger log = Logger.getLogger(DisRequestStatusNode.class.getName());
	
	/**
	 * Creates a request
	 * 
	 * @return DisRequest for this status request
	 *
	 * @author Daniel Brinkers
	 */
	public static DisRequest make() {
		DisRequest req = new DisRequestStatusNode();
		req.addHeader(RequestType.STATUSNODE);
		req.addInt(0);
		req.addInt(0);
		req.addExtension();
		log.trace("Making DisRequestStatusNode: " + req.get());
		return req;
	}

}
