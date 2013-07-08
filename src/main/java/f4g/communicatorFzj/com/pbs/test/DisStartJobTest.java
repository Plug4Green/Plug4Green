/**
* ============================== Header ============================== 
* file:          DisStartJobTest.java
* project:       FIT4Green/CommunicatorFzj
* created:       21.02.2011 by agiesler
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import f4g.commons.com.pbs.common.DisRequest;
import f4g.commons.com.pbs.common.DisRequestRunJob;
import f4g.commons.com.pbs.common.PbsConnection;
import org.junit.Before;
import org.junit.Test;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author agiesler
 */
public class DisStartJobTest {

	/**
	 * {To be completed; use html notation if necessary}
	 * 
	 * @throws java.lang.Exception
	 *
	 * @author agiesler
	 */
	
	String id = "2153.juggle21.zam.kfa-juelich.de";
	String[] nodes = new String[]{"juggle22"};
	
	@Before
	public void setUp() throws Exception {
		
	}

	/**
	 * Test method for {@link org.f4g.com.fzj.pbs.common.ProxyRequestStartJob#execute()}.
	 */
	@Test
	public void testExecute() {
		PbsConnection pbsConnection;
		DisRequest disRequest;

		try {
			pbsConnection = new PbsConnection(InetAddress.getLocalHost().getHostAddress());
			
			disRequest = DisRequestRunJob.make(id, nodes);
			pbsConnection.send(disRequest);
			pbsConnection.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		fail("Not yet implemented");
	}
	
	public void main(){
		id = "2153.juggle21.zam.kfa-juelich.de";
		nodes = new String[]{"juggle22"};
		
		testExecute();
	}

}
