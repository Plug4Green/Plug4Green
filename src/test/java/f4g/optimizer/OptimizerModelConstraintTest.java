package f4g.optimizer;

import java.util.LinkedList;
import java.util.List;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.DatacenterType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerRoleType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.constraints.optimizerconstraints.LoadType;
import f4g.schemas.java.constraints.optimizerconstraints.PeriodType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author cdupont
 */
public class OptimizerModelConstraintTest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 *
	 * @author cdupont
	 */
	protected void setUp() throws Exception {
		super.setUp();

		
		SLAGenerator slaGenerator = new SLAGenerator();
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);

		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, null));
	    
				
	}


	/**
	 * Destruction
	 * 
	 * @author cdupont
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;
	}
	

	/**
	 * cloud controllers doesn't host VMs
	 */
	public void testNoMovesForController() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
		
		DatacenterType DC = Utils.getFirstDatacenter(model);
		
		//test 1: without cloud controllers
		optimizer.runGlobalOptimization(model);
		
		assertEquals(2, getMoves().size());
		assertEquals(2, getPowerOffs().size());
		
	
		//test 2: with cloud controllers"		
		model = modelGenerator.createPopulatedFIT4GreenType();	
		DC = Utils.getFirstDatacenter(model);
		
		DC.getRack().get(0).getRackableServer().get(0).setName(ServerRoleType.CLOUD_CONTROLLER);
		DC.getRack().get(0).getRackableServer().get(1).setName(ServerRoleType.CLOUD_CONTROLLER);
		
		optimizer.runGlobalOptimization(model);
		
		//No VMs on could controller and no power off
		assertEquals(2, getMoves().size());
		assertEquals(0, getPowerOffs().size());
	}
	
	
	
	/**
	 * Test if the framework cabability ispoweron/ispoweroff  is working
	 */
	
	public void testOnOffCapability() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);

		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();
							
		//TEST 1 - power off capability
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);

		optimizer.runGlobalOptimization(model);
			
		assertEquals(0, getPowerOffs().size());
				
		//TEST 2 - power on capability
		for(ServerType s : Utils.getAllServers(model)) {
			s.setStatus(ServerStatusType.OFF);
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
	public void testGlobalPoweringOnPoweringOff() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2); 
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		List<ServerType> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
			
		servers.get(0).setStatus(ServerStatusType.POWERING_OFF);
				
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());
		
		servers.get(0).setStatus(ServerStatusType.POWERING_ON);
		
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());

		
	}
}



