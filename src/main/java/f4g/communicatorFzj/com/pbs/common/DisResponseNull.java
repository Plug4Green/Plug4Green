/**
* ============================== Header ============================== 
* file:          DisResponseNull.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Class for a empty DIS encoded response 
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.InputStream;

/**
 * Class for a empty DIS encoded response
 * 
 * @author Daniel Brinkers
 */
public class DisResponseNull extends DisResponse {

	/**
	 * Constructor of empty response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 */
	public DisResponseNull(InputStream inputStream, Header header) {
		super(header);
	}

}
