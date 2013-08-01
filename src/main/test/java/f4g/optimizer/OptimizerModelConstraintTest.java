package f4g.optimizer;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.DatacenterType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.MemoryUsageType;
import f4g.schemas.java.metamodel.NetworkUsageType;
import f4g.schemas.java.metamodel.NrOfCpusType;
import f4g.schemas.java.metamodel.RAMSizeType;
import f4g.schemas.java.metamodel.ServerRoleType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.StorageCapacityType;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.AllocationRequestType;
import f4g.schemas.java.CloudVmAllocationType;
import f4g.schemas.java.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType;
import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoadType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.LoadType;
import f4g.schemas.java.constraints.optimizerconstraints.PeriodType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.optimizer.Benchmark;
import f4g.optimizer.IntegrationTest;
import f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;


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



