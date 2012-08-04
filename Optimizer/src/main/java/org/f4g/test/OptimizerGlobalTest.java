package org.f4g.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.f4g.com.util.PowerData;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedPoliciesType;
import org.f4g.schema.constraints.optimizerconstraints.FederationType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.SpareCPUs;
import org.f4g.schema.constraints.optimizerconstraints.UnitType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.junit.Test;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author cdupont
 */
public class OptimizerGlobalTest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 *
	 * @author cdupont
	 */
	protected void setUp() throws Exception {
		super.setUp();

		PeriodType period = new PeriodType(begin, end, null, null, new LoadType(null, null));
		
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
		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(),
				SLAGenerator.createVirtualMachineType(), policies, fed);
	    
		optimizer.setSla(SLAGenerator.createDefaultSLA());
		optimizer.setOptiObjective(OptimizationObjective.Power);
		//ChocoLogging.setVerbosity(Verbosity.SEARCH);
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

		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		optimizer.runGlobalOptimization(model);
			
		assertEquals(getMoves().size(), 1);
		assertEquals(getMoves().get(0).getFrameworkName(), "DC1");

		optimizer.getVmTypes().getVMType().get(0).getExpectedLoad().setVCpuLoad(new CpuUsageType(100));
		optimizer.runGlobalOptimization(model);
		
		assertEquals(getMoves().size(), 0);
		
	}
	
	
	/**
	 * Test global optimization with one VM per servers and no load
	 *
	 */
	public void testGlobalOneVMperServerVMNoLoad() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);				
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
	
		optimizer.getVmTypes().getVMType().get(0).getExpectedLoad().setVCpuLoad(new CpuUsageType(0));
		optimizer.runGlobalOptimization(model);
				
		assertEquals(getMoves().size(), 9);
		assertEquals(getPowerOffs().size(), 9);
	
		//no duplicate moves or power offs should be found
		Set<MoveVMActionType> moveSet = new HashSet<MoveVMActionType>(getMoves());
		Set<PowerOffActionType> powerOffSet = new HashSet<PowerOffActionType>(getPowerOffs()); 
		assertTrue(moveSet.size()==9);
		assertTrue(powerOffSet.size()==9);
	}



	/**
	 * Test global optimization with one VM per servers, constraint is on Memory
	 * 
	 * @author cdupont
	 */
	public void testGlobalConstraintOnMemory() {

		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); 
		modelGenerator.setRAM_SIZE(2);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
	
		optimizer.getVmTypes().getVMType().get(0).getExpectedLoad().setVCpuLoad(new CpuUsageType(0));
		optimizer.getVmTypes().getVMType().get(0).getCapacity().getVRam().setValue(1);
		optimizer.runGlobalOptimization(model);
        		
		assertEquals(getMoves().size(), 5);
	
	}


		
	/**
	 * Test the "expected saved power" field.
	 * 
	 * @author cdupont
	 */
	public void testExpectedSavedPower(){
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(20);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores

		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		optimizer.runGlobalOptimization(model);
			  
		log.debug("ExpectedPowerSaving = " + (actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue()));
		//17 servers off, should result in 17 * 10.0 units of power saved
		assertTrue(actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue() == - 17 * 10.0);

	}


	/**
	 * Test global optimization with no VMs
	 *
	 * @author cdupont
	 */
	public void testGlobalNoVM() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
			
		optimizer.runGlobalOptimization(model);
		
		assertEquals(getMoves().size(), 0);
		assertEquals(getPowerOffs().size(), 10);
	}



	
	/**
	 * server with high power idle is switched off
	 */ 
	public void testGlobalServerWithLowPowerIdle(){

		//Create a Power Calculator that computes a more feeble power a server.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
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
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		//TEST 1 switching off the server that consumes more
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.runGlobalOptimization(model);
		
		//switching off the server that consumes more
		assertEquals(getPowerOffs().get(0).getNodeName(), "id100000");

		//TEST 2 same with different server
	
		//Create a Power Calculator that computes a more feeble power for a server.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
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
		assertEquals(getPowerOffs().get(0).getNodeName(), "id200000");
		
	}

	/**
	 * There is too few ON servers, issuing powers ON on the most energy efficient server
	 */
	public void testGlobalTooFewServers() {
		
		//Create a Power Calculator that computes a more feeble power a server.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
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
								
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		List<ServerType> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
		servers.get(0).setStatus(ServerStatusType.OFF);
		servers.get(1).setStatus(ServerStatusType.OFF);
		
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(3, UnitType.ABSOLUTE));
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.runGlobalOptimization(model);
		
		//turning On only one machine, id1 is more efficient than id0
		assertEquals(getPowerOns().size(), 1);
		assertEquals(getPowerOns().get(0).getNodeName(), "id200000");
		
	}
	
	
	/**
	 *
	 */
	public void testGlobalConstraintOnNbCoresCharged(){
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		modelGenerator.setCORE(4);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		//emptying VM1
		RackableServerType S1 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1);
		VirtualMachineType VM1 = S1.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().get(0);
		
		//nulify non used values (should work without)
		VM1.setNumberOfCPUs(null);
		VM1.setActualCPUUsage(null);
		VM1.setActualDiskIORate(null);
		VM1.setActualMemoryUsage(null);
		VM1.setActualNetworkUsage(null);
		VM1.setActualStorageUsage(null);
						
		optimizer.runGlobalOptimization(model);
	
		//VM are packed 4 by servers, following number of cores
		assertEquals(getMoves().size(), 16);
		//one machine is kept alive
		assertEquals(getPowerOffs().size(), 4);

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
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();

		optimizer.runGlobalOptimization(model);	
		
		assertTrue(getMoves().size() != 0);
		
	}


	/**
	 * Test global with constraints not satisfied
	 */
	public void testGlobalTooMuchVMs() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		modelGenerator.setCORE(4); 
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
			
		optimizer.setClusters(createDefaultCluster(8, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		optimizer.getSla().getSLA().get(0).getCommonQoSRelatedMetrics().setMaxVirtualCPUPerCore(new MaxVirtualCPUPerCore((float)1.0, 1));
		
		//TEST 1
		//8 VMS -> full servers
				
		//transferring VMs
		List<VirtualMachineType> VMs0 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs1 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		VMs0.addAll(VMs1);
		VMs1.clear();
				
		optimizer.runGlobalOptimization(model);
		
		assertEquals(getMoves().size(),4);
			
	}
}



