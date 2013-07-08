/**
* ============================== Header ============================== 
* file:          DisResponseQueue.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parses DIS encoded queue response
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses DIS encoded queue response
 *
 * @author Daniel Brinkers
 */
public class DisResponseQueue extends DisResponse {
	private String id_;
	/**
	 * Constructor of the response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 * @throws IOException 
	 */
	public DisResponseQueue(InputStream inputStream, Header header) throws IOException {
		super(header);
		setId(readString(inputStream));
	}
	/**
	 * @param id_ the id_ to set
	 */
	public void setId(String id) {
		this.id_ = id;
	}
	/**
	 * @return the id_
	 */
	public String getId() {
		return id_;
	}

}
