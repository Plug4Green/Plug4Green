package f4g.optimizer;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import f4g.commons.com.util.PowerData;
import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.RackableServer;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.Load;
import f4g.schemas.java.constraints.optimizerconstraints.Period;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SpareNodes;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.optimizer.cloud.OptimizerEngineCloud;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class OptimizerGlobalTest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();

		Period period = new Period(begin, end, null, null, new Load(null, null));
		
		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType policies = new PolicyType(polL);
		policies.getPolicy().add(pol);
		
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(pol);
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);
		
		optimizer = new OptimizerEngineCloud(new MockController(), new MockPowerCalculator(), new NetworkCost(),
				SLAGenerator.createVirtualMachine(), policies, fed);
	    
		optimizer.setSla(SLAGenerator.createDefaultSLA());
		optimizer.setOptiObjective(OptimizationObjective.Power);
		//ChocoLogging.setVerbosity(Verbosity.SEARCH);
	}

	/**
	 * Test global optimization with one VM per servers, constraint is on CPU usage
	 */
    @Test
	public void testGlobalConstraintOnCPUUsage(){
        
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1);
		modelGenerator.setRAM_SIZE(100);

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		optimizer.runGlobalOptimization(model);
			
		assertEquals(1, getMoves().size());
		assertEquals("DC1", getMoves().get(0).getFrameworkName());

		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(100));
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());
		
	}
	
	
	/**
	 * Test global optimization with one VM per servers and no load
	 */
    @Test
	public void testGlobalOneVMperServerVMNoLoad() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(5);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);				
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();				
	
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(1));
		optimizer.runGlobalOptimization(model);
				
		assertEquals(4, getMoves().size());
		assertEquals(4, getPowerOffs().size());

		//no duplicate moves or power offs should be found
		Set<MoveVMAction> moveSet = new HashSet<MoveVMAction>(getMoves());
		Set<PowerOffAction> powerOffSet = new HashSet<PowerOffAction>(getPowerOffs()); 
		assertTrue(moveSet.size()==4);
		assertTrue(powerOffSet.size()==4);
	}

	/**
	 * Test global optimization with one VM per servers, constraint is on Memory
	 */
    @Test
	public void testGlobalConstraintOnMemory() {

		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6);
		modelGenerator.setRAM_SIZE(2);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();				
	
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(10));
		optimizer.getVmTypes().getVMFlavor().get(0).getCapacity().getVRam().setValue(1);
		optimizer.runGlobalOptimization(model);

		//Servers offers 2 RAM units, VMs consumes 1
		assertEquals(2, getMoves().size());
	}

	/**
	 * Test the "expected saved power" field.
	 */
    @Test
	public void testExpectedSavedPower(){
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
				
		optimizer.runGlobalOptimization(model);
			  
		log.debug("ExpectedPowerSaving = " + (actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue()));
		//8 servers off, should result in 8 * 10.0 units of power saved
		assertEquals(-8 * 10.0, actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue(), 0.1);

	}


	/**
	 * Test global optimization with no VMs
	 */
    @Test
	public void testGlobalNoVM() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();				
			
		optimizer.runGlobalOptimization(model);
		
		assertEquals(0, getMoves().size());
		assertEquals(10, getPowerOffs().size());
	}
	
	/**
	 * server with high power idle is switched off
	 */
    @Test
	public void testGlobalServerWithLowPowerIdle(){

		//Create a Power Calculator that computes a more feeble power a server.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id100000"))
					power.setActualConsumption(15.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}						
		}
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		//TEST 1 switching off the server that consumes more
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.runGlobalOptimization(model);
		
		//switching off the server that consumes more
		assertEquals("id100000", getPowerOffs().get(0).getNodeName());

		//TEST 2 same with different server
	
		//Create a Power Calculator that computes a more feeble power for a server.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id200000"))
					power.setActualConsumption(15.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}						
		}		
		optimizer.setPowerCalculator(new MyPowerCalculator2());
		optimizer.runGlobalOptimization(model);
	
		//switching off the server that consumes more
		assertEquals("id200000", getPowerOffs().get(0).getNodeName());
		
	}

	/**
	 * There is too few ON servers, issuing powers ON on the most energy efficient server
	 */
    @Test
	public void testGlobalTooFewServers() {
		
		//Create a Power Calculator that computes a more feeble power a server.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id200000"))
					power.setActualConsumption(8.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());

				return power;
			}						
		}
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2); 
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
								
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		List<Server> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
		servers.get(0).setStatus(ServerStatus.OFF);
		servers.get(1).setStatus(ServerStatus.OFF);
		
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareNodes(new SpareNodes(1, UnitType.ABSOLUTE));
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.runGlobalOptimization(model);
		
		//turning On only one machine
		assertEquals(1, getPowerOns().size());
				
	}
	
	
	/**
	 *
	 */
    @Test
	public void testGlobalConstraintOnNbCoresCharged(){
		
		//generate one VM per server
		//VMs resource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		modelGenerator.setCORE(4);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		//emptying VM1
		RackableServer S1 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1);
		VirtualMachine VM1 = S1.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().get(0);
		
		//Nullify non used values (should work without)
		VM1.setNumberOfCPUs(null);
		VM1.setActualCPUUsage(null);
		VM1.setActualDiskIORate(null);
		VM1.setActualMemoryUsage(null);
		VM1.setActualNetworkUsage(null);
		VM1.setActualStorageUsage(null);
						
		optimizer.runGlobalOptimization(model);
	
		//VM are packed 4 by servers, following number of cores
		assertEquals(16, getMoves().size());
		//one machine is kept alive
		assertEquals(4, getPowerOffs().size());

	}
	
	/**
	 * Test with a very loaded configuration
	 */
    @Test
	public void testGlobalChargedTraditional(){

		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(50); 
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setRAM_SIZE(800);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4);
		modelGenerator.IS_CLOUD = false;
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		optimizer.runGlobalOptimization(model);	
		
		assertTrue(getMoves().size() != 0);
		
	}


	/**
	 * Test global with CPU constraints not satisfied (model need to be repaired)
	 */
    @Ignore @Test //The new BtrPlace does not support anymore the "wrong" situations, where VMs are consuming more than 100% CPUs and must be moved.
    public void testGlobalTooMuchVMs() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		modelGenerator.setCORE(4); 
		
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxVirtualCPUPerCore(new MaxVirtualCPUPerCore((float)1.0, 1));
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(100));
		
		//TEST 1		
		//transferring VMs: server id100000 has 8 VMs, id200000 has zero 
		List<VirtualMachine> VMs0 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachine> VMs1 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();

		VMs0.addAll(VMs1);
		VMs1.clear();
				
		optimizer.runGlobalOptimization(model);

		//id100000 is too full, 4 VMs should move out
		assertEquals(4, getMoves().size());
			
	}
}
