package f4g.optimizer;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.Datacenter;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.ServerRole;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.constraints.optimizerconstraints.Load;
import f4g.schemas.java.constraints.optimizerconstraints.Period;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.optimizer.cloud.OptimizerEngineCloud;


public class OptimizerModelConstraintTest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		SLAGenerator slaGenerator = new SLAGenerator();
		
		Period period = new Period(
				begin, end, null, null, new Load(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);

		
		optimizer = new OptimizerEngineCloud(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachine(), vmMargins, makeSimpleFed(vmMargins, null));
				
	}


	/**
	 * cloud controllers doesn't host VMs
	 */
	@Test
	public void testNoMovesForController() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);		
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();				
		
		Datacenter DC = Utils.getFirstDatacenter(model);
		
		//test 1: without cloud controllers
		optimizer.runGlobalOptimization(model);
		
		assertEquals(2, getMoves().size());
		assertEquals(2, getPowerOffs().size());
		
	
		//test 2: with cloud controllers"		
		model = modelGenerator.createPopulatedFIT4Green();	
		DC = Utils.getFirstDatacenter(model);
		
		DC.getRack().get(0).getRackableServer().get(0).setName(ServerRole.CLOUD_CONTROLLER);
		DC.getRack().get(0).getRackableServer().get(1).setName(ServerRole.CLOUD_CONTROLLER);
		
		optimizer.runGlobalOptimization(model);
		
		//No VMs on could controller and no power off
		assertEquals(2, getMoves().size());
		assertEquals(0, getPowerOffs().size());
	}
	
	
	
	/**
	 * Test if the framework capability ispoweron/ispoweroff  is working
	 */
	@Test
	public void testOnOffCapability() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);

		FIT4Green model = modelGenerator.createPopulatedFIT4Green2Sites();
							
		//TEST 1 - power off capability
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);

		optimizer.runGlobalOptimization(model);
			
		assertEquals(0, getPowerOffs().size());
				
		//TEST 2 - power on capability
		for(Server s : Utils.getAllServers(model)) {
			s.setStatus(ServerStatus.OFF);
			s.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
		}
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOn(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOn(false);
	
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(3, UnitType.ABSOLUTE));
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getPowerOns().size());
	}
	
	
	/**
	 * There is too few ON servers, issuing powers ON
	 */
	@Test
	public void testGlobalPoweringOnPoweringOff() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2); 
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		List<Server> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
			
		servers.get(0).setStatus(ServerStatus.POWERING_OFF);
				
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());
		
		servers.get(0).setStatus(ServerStatus.POWERING_ON);
		
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());

		
	}
}



