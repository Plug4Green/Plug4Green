/**
* ============================== Header ============================== 
* file:          DisRequestStatusJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-05 17:17:43 +0200 (Di, 05 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 640 $
* 
* short description:
*   DIS encoded request for the status of all jobs
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;

/**
 * class to create a DIS encoded request for the status of all jobs
 * @author Daniel Brinkers
 */
public class DisRequestStatusJob extends DisRequest {
	
	static Logger log = Logger.getLogger(DisRequestStatusJob.class.getName());
	
	/**
	 * Creates a request
	 * 
	 * @return DisRequest for this status request
	 *
	 * @author Daniel Brinkers
	 */
	static DisRequest make(){
		DisRequest disRequest = new DisRequestStatusJob();
		disRequest.addHeader(RequestType.STATUSJOB);
		disRequest.addInt(0);
		disRequest.addInt(0);
		disRequest.addExtension();
		log.trace("Making DisRequestStatusJob: " +  disRequest.get());
		return disRequest;
	}
}
