/**
* ============================== Header ============================== 
* file:          ProxyRequestStartJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       Dec 8, 2010 by brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author brinkers
 */
public class ProxyRequestStartJob extends ProxyRequest {
	
	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(ProxyRequestStartJob.class.getName());
	
	private static final long serialVersionUID = 1L;
	private String id_;
	private String[] nodes_;
	
	ProxyRequestStartJob(String id, String[] nodes){
		setId(id);
		log.debug("Starting job with id: " + id);
		setNodes(nodes);
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.Fzj.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponse execute() throws IOException {
		PbsConnection pbsConnection;
		DisRequest disRequest;

		pbsConnection = new PbsConnection(InetAddress.getLocalHost().getHostAddress());
		
		//log.debug("Sending DisRequestRunJob to Nodes: " + getNodes().toString());
		
		disRequest = DisRequestRunJob.make(getId(), getNodes());
		pbsConnection.send(disRequest);
		pbsConnection.close();

		return null;
	}

	/**
	 * @param id_ the id_ to set
	 */
	public void setId(String id_) {
		this.id_ = id_;
	}

	/**
	 * @return the id_
	 */
	public String getId() {
		return id_;
	}

	/**
	 * @param nodes_ the nodes_ to set
	 */
	public void setNodes(String[] nodes_) {
		this.nodes_ = nodes_;
	}

	/**
	 * @return the nodes_
	 */
	public String[] getNodes() {
		return nodes_;
	}

}
