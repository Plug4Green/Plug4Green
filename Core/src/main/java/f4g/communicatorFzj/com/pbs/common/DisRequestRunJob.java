/**
* ============================== Header ============================== 
* file:          DisRequestRunJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   DIS encoded request to run a job
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;


/**
 * DIS encoded request to run a certain job on certain nodes
 *
 * @author Daniel Brinkers
 */
public class DisRequestRunJob extends DisRequest {
	
	/**
	 * 
	 *  Creates the request.
	 * 
	 * @param jobId
	 * @param hosts
	 * @return DisRequest for this authorization request
	 *
	 * @author Daniel Brinkers
	 */
	
	static Logger log = Logger.getLogger(DisRequestRunJob.class.getName());
	
	public static DisRequest make(String jobId, String[] hosts){
		DisRequest disRequest = new DisRequestRunJob();
		String hostlist = "";
		if(hosts.length > 0){
			hostlist = hosts[0];
			for(int i=1; i<hosts.length; ++i)
				hostlist += "+" + hosts[i];
			log.debug("hostlist: " +  hostlist);
		}
		disRequest.addHeader(Request.RUNJOB);
		disRequest.addString(jobId);
		disRequest.addString(hostlist);
		log.debug("Added destination: " +  hostlist);
		disRequest.addInt(0);
		disRequest.addExtension();
		log.debug("Making DisRequestRunJob: " +  disRequest.get());
		return disRequest;
	}
}
