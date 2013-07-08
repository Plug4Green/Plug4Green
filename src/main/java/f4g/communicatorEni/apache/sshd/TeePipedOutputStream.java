/**
 * ============================== Header ============================== 
 * file:          TeePipedOutputStream.java
 * project:       FIT4Green/CommunicatorEni
 * created:       21/02/2011 by jclegea
 * 
 * $LastChangedDate: 2011-02-25 12:59:34 +0100 (vie, 25 feb 2011) $ 
 * $LastChangedBy: jclegea $
 * $LastChangedRevision: 599 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package f4g.communicatorEni.apache.sshd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author jclegea
 */
public class TeePipedOutputStream extends PipedOutputStream {
	private OutputStream tee;

	public TeePipedOutputStream(OutputStream outputstream) {
		tee = outputstream;
	}

	public void write(int i) throws IOException {
		super.write(i);
		tee.write(i);
	}

	public void write(byte abyte0[], int i, int j) throws IOException {
		super.write(abyte0, i, j);
		tee.write(abyte0, i, j);
	}

}
