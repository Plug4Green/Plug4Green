/**
* ============================== Header ============================== 
* file:          Server.java
* project:       FIT4Green/CommunicatorFzj
* created:       24.08.2011 by agiesler
* 
* $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1244 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com.unicore;

import org.apache.log4j.Logger;

/**
 * Simple InterServlet communication Service based on Cajo to reveive job allocation
 * request from Unicore Servlet and forward the request to the F4G monitorer 
 * 
 *
 * @author agiesler
 */


public class InterServletComServer {
	
	static Logger log = Logger.getLogger(InterServletComServer.class.getName());
	
	ComUnicore comUnicore;
	
	public InterServletComServer(ComUnicore comUnicore) {
		this.comUnicore = comUnicore;
	}
	
	public String prepareAllocationRequest(String id, String _nodes, 
			String _cores, String _mem, String _walltime, String _energy, 
			String _latest, String _suitable_clusters, String _benchmark_id) {
		log.info("Job Request for id " +  id);
		int nodes = Integer.parseInt(_nodes);
		int cores = Integer.parseInt(_cores);
		int mem = Integer.parseInt(_mem);
		long walltime = Long.parseLong(_walltime);
		boolean isenergy = Boolean.parseBoolean(_energy);
		long latest = Long.parseLong(_latest);
		String suitableClusters = _suitable_clusters;
		String clusterID = comUnicore.generateHPCAllocRequest(nodes, cores, mem,
				walltime, isenergy, latest, suitableClusters, _benchmark_id);
		
        return clusterID;
    }
}
