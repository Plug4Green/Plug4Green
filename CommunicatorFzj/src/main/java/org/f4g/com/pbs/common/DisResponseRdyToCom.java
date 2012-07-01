/**
* ============================== Header ============================== 
* file:          DisResponseRdyToCom.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parses DIS encoded Ready to communicate response
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses DIS encoded Ready to communicate response
 * 
 * @author Daniel Brinkers
 */
public class DisResponseRdyToCom extends DisResponse {
	private String id_;
	/**
	 * Constructor of the response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 * @throws IOException 
	 */

	public DisResponseRdyToCom(InputStream inputStream, Header header) throws IOException {
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
