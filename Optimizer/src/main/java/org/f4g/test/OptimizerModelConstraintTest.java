package org.f4g.test;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;

import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.DatacenterType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.ServerRoleType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.constraints.optimizerconstraints.BoundedPoliciesType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedSLAsType;
import org.f4g.schema.constraints.optimizerconstraints.CapacityType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.ExpectedLoadType;
import org.f4g.schema.constraints.optimizerconstraints.FederationType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.test.OptimizerTest.MockController;
import org.f4g.test.OptimizerTest.MockPowerCalculator;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;


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
				begin, end, null, null, new LoadType("m1.small", 300, 6));

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
	 * 
	 * 
	 * @author cdupont
	 */
	public void testNoMovesForController() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); 
		modelGenerator.setRAM_SIZE(50);
	
		//VM settings
		modelGenerator.VM_TYPE = "RAM_constraint";

		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("RAM_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(0), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(0.1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		
		
		optimizer.getVmTypes().getVMType().add(type1);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
	
		FederationType federation = makeSimpleFed(optimizer.getPolicies(), model);
		optimizer.setFederation(federation);
		ClusterType clusters = new ClusterType();
		clusters.getCluster().add(federation.getBoundedCluster().getCluster().get(0).getIdref());
		optimizer.setClusterType(clusters);
		
		
		DatacenterType DC = Utils.getFirstDatacenter(model);
		
		log.debug("test 1: without cloud controllers");
	
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionRequestType.ActionList response = actionRequest.getActionList();
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());

		assertEquals(moves.size(), 2);
		assertEquals(powerOffs.size(), 2);
		
	
		log.debug("test 2: with cloud controllers");
		
		model = modelGenerator.createPopulatedFIT4GreenType();	
		DC = Utils.getFirstDatacenter(model);
		
		DC.getRack().get(0).getRackableServer().get(0).setName(ServerRoleType.CLOUD_CONTROLLER);
		DC.getRack().get(0).getRackableServer().get(1).setName(ServerRoleType.CLOUD_CONTROLLER);
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		response = actionRequest.getActionList();
		
		List <MoveVMActionType> moves2 = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs2 = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves2.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs2.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("moves=" + moves2.size());
		log.debug("powerOffs=" + powerOffs2.size());
	
		//No VMs on could controller and no power off
		assertEquals(moves2.size(), 2);
		assertEquals(powerOffs2.size(), 0);
	}
	
	
	
	/**
	 * Test if the framework cabability ispoweron/ispoweroff  is working
	 * @author cdupont
	 */
	
	public void testOnOffCapability() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(8); 
		modelGenerator.setRAM_SIZE(24);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	

		SLAType slas = new SLAType();
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", -1, -1));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
			
		//TEST 1 - power off capability
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOff(false);

		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				VMs, vmMargins, makeSimpleFed(vmMargins, model));
		
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ActionRequestType.ActionList response = actionRequest.getActionList();
		
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("powerOffs=" + powerOffs.size());
	
		assertTrue(powerOffs.size()==0);
		
		
		//TEST 2 - power on capability
		//TODO: this is not working
//		for(ServerType s : Utils.getAllServers(model)) {
//			s.setStatus(ServerStatusType.OFF);
//			s.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
//		}
//		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOn(false);
//		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getNode().setPowerOn(false);
//	
//		MyOptimizer.runGlobalOptimization(model);
//		try {
//			actionRequestAvailable.acquire();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		response = actionRequest.getActionList();
//		
//		powerOffs.clear();
//		
//		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
//			if (action.getValue() instanceof PowerOffActionType) 
//				powerOffs.add((PowerOffActionType)action.getValue());
//		}
//	         	
//		log.debug("powerOffs=" + powerOffs.size());
//	
//		assertTrue(powerOffs.size()==0);
	
		
	}
	
	
	/**
	 * There is too few ON servers, issuing powers ON
	 */
	public void testGlobalPoweringOnPoweringOff() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4); //8
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		
		//VM settings
		modelGenerator.setCPU_USAGE(0.0);
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(0);

		FrameworkCapabilitiesType frameworkCapabilitie = new FrameworkCapabilitiesType();
		frameworkCapabilitie.setFrameworkName("FM");
						
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		List<ServerType> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
			
		servers.get(0).setStatus(ServerStatusType.POWERING_OFF);
		servers.get(1).setStatus(ServerStatusType.POWERING_ON);
		
		//add a supplementary core to S1 -> should be turned on
		servers.get(1).getMainboard().get(0).getCPU().get(0).getCore().add(new CoreType());
		
		//add a supplementary core to S2 -> should get allocated
		servers.get(2).getMainboard().get(0).getCPU().get(0).getCore().add(new CoreType());
				
		CloudVmAllocationType cloudAlloc = new CloudVmAllocationType();
		cloudAlloc.setVmType("m1.small");
		cloudAlloc.getClusterId().add("c1");
		cloudAlloc.setImageId("i1");
		cloudAlloc.setUserId("u1");
		
		//Simulates a CloudVmAllocationType operation
		JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
	
		AllocationRequestType allocationRequest = new AllocationRequestType();
		allocationRequest.setRequest(operationType);
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", -1, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVmMargins = new PolicyType(polL);
		myVmMargins.getPolicy().add(pol);
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
		OptimizerEngineCloudTraditional myOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		myOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <PowerOnActionType>  powerOns = new ArrayList<PowerOnActionType>();
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof PowerOnActionType) 
				powerOns.add((PowerOnActionType)action.getValue());
		}		
		
		//turning On only one machine, id1 is more efficient than id0
		assertEquals(actionRequest.getActionList().getAction().size(), 0);
		assertEquals(powerOns.size(), 0);

		
	}
}



