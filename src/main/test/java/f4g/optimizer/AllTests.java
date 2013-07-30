/**
* ============================== Header ============================== 
* file:          AllTests.java
* project:       FIT4Green/Optimizer
* created:       10 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-05-01 00:59:19 +0200 (mar, 01 may 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1406 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.optimizer;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author cdupont
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.f4g.test");
		//$JUnit-BEGIN$
		suite.addTestSuite(OptimizerAllocationTest.class);
		suite.addTestSuite(OptimizerGlobalTest.class);
		suite.addTestSuite(OptimizerModelConstraintTest.class);
		suite.addTestSuite(OptimizerMultiClusterTest.class);
		suite.addTestSuite(OptimizerSLATest.class);
		suite.addTestSuite(IntegrationTest.class);

		// rlent
        suite.addTestSuite(LoadCalculatorTest.class);
        suite.addTestSuite(NetworkCostTest.class);
        suite.addTestSuite(OptimizerNetworkTest.class);
        suite.addTestSuite(OptimizerNetworkTestBasic.class);
        
		
		//$JUnit-END$
		return suite;
	}

}
