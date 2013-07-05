/**
* ============================== Header ============================== 
* file:          ProxyRequest.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parent class for all requests send from F4g plug-in to the Proxy
*   Also involves a RMC like structure for a function, which should be executed on the Proxy, when receiving one of these messages
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.io.Serializable;

/**
 * Parent class for all requests send from F4g plug-in to the Proxy
 *   Also involves a RMC like structure for a function, which should be executed on the Proxy, when receiving one of these messages
 * @see Proxy
 * @see ProxyResponse
 * @author Daniel Brinkers
 */
public abstract class ProxyRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The method to be executed on the Proxy, when received one of these messages (to be implemented by child classes)
	 * @return the response which should be send back to the F4G plug-in
	 *
	 */
	public abstract ProxyResponse execute() throws IOException;
}
