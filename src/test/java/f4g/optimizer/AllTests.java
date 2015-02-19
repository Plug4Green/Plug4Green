package f4g.optimizer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	OptimizerAllocationTest.class,
	OptimizerGlobalTest.class,
	OptimizerModelConstraintTest.class,
	OptimizerMultiClusterTest.class,
	OptimizerSLATest.class,
	IntegrationTest.class,

	// network
    LoadCalculatorTest.class,
    NetworkCostTest.class,
    OptimizerNetworkTest.class,
    OptimizerNetworkTestBasic.class
    
})
public class AllTests {}
