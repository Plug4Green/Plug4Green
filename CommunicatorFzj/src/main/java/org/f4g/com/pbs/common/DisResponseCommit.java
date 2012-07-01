/**
* ============================== Header ============================== 
* file:          DisResponseCommit.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parse DIS encoded response of a commit request
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parse DIS encoded response of a commit request
 * 
 * @author Daniel Brinkers
 */
public class DisResponseCommit extends DisResponse {
	private String id_;
	/**
	 * Constructor of the response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 * @throws IOException 
	 */
	public DisResponseCommit(InputStream inputStream, Header header) throws IOException {
		super(header);
		setId(readString(inputStream));
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id_ = id;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id_;
	}
}
