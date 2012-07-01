/**
* ============================== Header ============================== 
* file:          DisResponseText.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parses a DIS encoded text response
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses a DIS encoded text response
 * @author Daniel Brinkers
 */
public class DisResponseText extends DisResponse {
	private String text_;
	/**
	 * Constructor of the response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 * @throws IOException 
	 */
	public DisResponseText(InputStream inputStream, Header header) throws IOException {
		super(header);
		setText(readString(inputStream));
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text_ = text;
	}
	/**
	 * @return the text_
	 */
	public String getText() {
		return text_;
	}

}
