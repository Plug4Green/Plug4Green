package org.f4g.test;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import org.f4g.com.util.PowerData;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.OptimizerEngine;
import org.f4g.optimizer.utils.Recorder;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.PUEType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.actions.ActionRequestType.ActionList;
import org.f4g.schema.constraints.optimizerconstraints.BoundedPoliciesType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedSLAsType;
import org.f4g.schema.constraints.optimizerconstraints.CapacityType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.ExpectedLoadType;
import org.f4g.schema.constraints.optimizerconstraints.FederationType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.NodeControllerType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.junit.Test;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;

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

		SLAGenerator slaGenerator = new SLAGenerator();
		
		PeriodType period = new PeriodType(begin, end, null, null, new LoadType("m1.small", -1, -1));
		
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
				        slaGenerator.createVirtualMachineType(), policies, fed);
	    
		optimizer.setOptiObjective(OptimizationObjective.Power);
		//algo = new AlgoGlobal(new MockPowerCalculator(), new NetworkCost(), AlgoType.CLOUD);
		ChocoLogging.setVerbosity(Verbosity.SEARCH);
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
	 * Specific to cloud: in cloud, only one VM is allowed per Core
	 * @author cdupont
	 */
    @Test
	public void testGlobalConstraintOnCPUUsage(){
		//generate one VM per server
		//VMs ressource usage is 0
        
		ModelGenerator modelGenerator = new ModelGenerator();

		modelGenerator.setNB_SERVERS(8); //8
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(100);


		modelGenerator.setVM_TYPE("CPU_constraint");
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
				
		optimizer.getVmTypes().getVMType().add(type1);
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();

		
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		if(actionRequest!=null) {
			for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
				if (action.getValue() instanceof MoveVMActionType) 
					moves.add((MoveVMActionType)action.getValue());
				if (action.getValue() instanceof PowerOffActionType) 
					powerOffs.add((PowerOffActionType)action.getValue());
			}	
		}
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
		//no overbooking is made
		assertEquals(moves.size(), 6);
		//only 3 powers off since we keep a margin of max 6 slots = 3 machines
		assertEquals(powerOffs.size(), 6);
		assertEquals(moves.get(0).getFrameworkName(), "DC1");


	}
	
	
	/**
	 * Test global optimization with one VM per servers, constraint is on nb cores
	 * 
	 * @author cdupont
	 */
	public void testGlobalConstraintOnNbCores(){
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
	
		modelGenerator.setVM_TYPE("CPU_constraint");
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();
		
		//emptying VM1
		RackableServerType S1 = modelManyServersNoLoad.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1);
		VirtualMachineType VM1 = S1.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().get(0);
		
		//nulify non used values (should work without)
		VM1.setNumberOfCPUs(null);
		VM1.setActualCPUUsage(null);
		VM1.setActualDiskIORate(null);
		VM1.setActualMemoryUsage(null);
		VM1.setActualNetworkUsage(null);
		VM1.setActualStorageUsage(null);
			
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
						
		optimizer.getVmTypes().getVMType().add(type1);
		
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
		
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
		
              	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());

		//VM are packed 4 by servers, following number of cores
		assertEquals(moves.size(), 7);
		//one machine is kept alive
		assertEquals(powerOffs.size(), 7);

	}


	/**
	 * Test global optimization with two servers and two VMS
	 *
	 * @author cdupont
	 */
	public void testGlobalTwoServersTwoVMs() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setVM_TYPE("small");
		
		FIT4GreenType modelTwoServersTwoVMs = modelGenerator.createPopulatedFIT4GreenType();				
	
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(12), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
				
		optimizer.getVmTypes().getVMType().add(type1);
		
		optimizer.runGlobalOptimization(modelTwoServersTwoVMs);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionRequestType.ActionList response = actionRequest.getActionList();
		
		//one move, one switch off
		assertTrue(response.getAction().size() == 2);
		assertTrue(response.getAction().get(0).getValue() instanceof MoveVMActionType);
		assertTrue(response.getAction().get(1).getValue() instanceof PowerOffActionType);
		
		MoveVMActionType move = (MoveVMActionType)response.getAction().get(0).getValue();
		PowerOffActionType powerOff = (PowerOffActionType)response.getAction().get(1).getValue();
		
		assertNotNull(move.getVirtualMachine());
		assertNotNull(move.getDestNodeController());
		assertNotNull(move.getSourceNodeController());
		assertNotNull(powerOff.getNodeName());
		
		//two possible solutions
		boolean sol1 = move.getVirtualMachine()      .equals("id" + (modelGenerator.SERVER_FRAMEWORK_ID * 1 + modelGenerator.VM_FRAMEWORK_ID)) 
					&& move.getDestNodeController()  .equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 2)
					&& move.getSourceNodeController().equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 1)
					&& powerOff.getNodeName()        .equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 1);
		
		boolean sol2 = move.getVirtualMachine()      .equals("id" + (modelGenerator.SERVER_FRAMEWORK_ID * 2 + modelGenerator.VM_FRAMEWORK_ID)) 
		            && move.getDestNodeController()  .equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 1)
		            && move.getSourceNodeController().equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 2)
					&& powerOff.getNodeName()        .equals("id" + modelGenerator.SERVER_FRAMEWORK_ID * 2);
		assertTrue(sol1 || sol2);
		
	}


	/**
	 * Test global optimization with one VM per servers and no load
	 *
	 * @author cdupont
	 */
	public void testGlobalOneVMperServerVMNoLoad() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);

		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		
		//VM settings
		modelGenerator.VM_TYPE = "Ridiculous";
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("Ridiculous");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));

		optimizer.getVmTypes().getVMType().add(type1);
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();				
	
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
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
	
		assertTrue(moves.size()==9);
		assertTrue(powerOffs.size()==9);
	
		//no duplicate moves or power offs should be found
		Set<MoveVMActionType> moveSet = new HashSet<MoveVMActionType>(moves);
		Set<PowerOffActionType> powerOffSet = new HashSet<PowerOffActionType>(powerOffs); 
		assertTrue(moveSet.size()==9);
		assertTrue(powerOffSet.size()==9);
	}



	/**
	 * Test global optimization with one VM per servers, constraint is on Network usage
	 * 
	 * @author cdupont
	 */
	public void testGlobalConstraintOnMemory() {
        //ChocoLogging.setVerbosity(Verbosity.SEARCH);
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); 
		modelGenerator.setRAM_SIZE(2);
	
		//VM settings
		modelGenerator.setCPU_USAGE(2.0);

		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(1);
		modelGenerator.VM_TYPE = "RAM_constraint";
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("RAM_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
			
		optimizer.getVmTypes().getVMType().add(type1);
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();				
	
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
        optimizer.setSearchTimeLimit(180);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
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
	
		assertEquals(moves.size(), 5);
		//one machine kept alive
		assertEquals(powerOffs.size(), 5);
	
	}


	/**
	 * Test global optimization with one VM per servers, constraint is on Storage
	 * 
	 * @author cdupont
	 */
	public void testGlobalConstraintOnStorage() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); 
		modelGenerator.setSTORAGE_SIZE(10000);
	
		//VM settings
		modelGenerator.setCPU_USAGE(0.0); 
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(modelGenerator.MAX_STORAGE_SIZE / 2);
		modelGenerator.setMEMORY_USAGE(0); 
		modelGenerator.VM_TYPE = "Storage_constraint";
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("Storage_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(0), new RAMSizeType(1), new StorageCapacityType(modelGenerator.MAX_STORAGE_SIZE / 2)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(0.1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		
		optimizer.getVmTypes().getVMType().add(type1);
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();				
		RackableServerType S1 = modelManyServersNoLoad.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0);
		S1.setFrameworkRef(null);
		
		(new Recorder()).recordModel(modelManyServersNoLoad);
		
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
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
		//log.debug("saving=" + response.getAction(
		
		assertTrue(moves.size() == 9);
		assertTrue(powerOffs.size() == 9);
	
	}
		
	/**
	 * Test the "expected saved power" field.
	 * 
	 * @author cdupont
	 */
	public void testExpectedSavedPower(){
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(20);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		
		//VM settings
		modelGenerator.VM_TYPE = "small";
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(modelGenerator.MAX_STORAGE_SIZE / 2)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));

		optimizer.getVmTypes().getVMType().add(type1);

		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();
				
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
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
	              	
		log.debug("ExpectedPowerSaving = " + (actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue()));

		//74 servers off, should result in 15 * 10.0 units of power saved
		assertTrue(actionRequest.getComputedPowerAfter().getValue() - actionRequest.getComputedPowerBefore().getValue() == - 15 * 10.0);

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
	
		//VM settings
		modelGenerator.VM_TYPE = "small";
		
		VMTypeType vmTypes = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(0), new RAMSizeType(modelGenerator.MAX_RAM_SIZE / 2), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(0.1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
					
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVmMargins = new PolicyType(polL);
		myVmMargins.getPolicy().add(pol);
				
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		
		MyOptimizer.runGlobalOptimization(model);
		
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
		assertEquals(moves.size(), 0);
		assertEquals(powerOffs.size(), 9);
	}



	
	/**
	 * Test global optimization with one VM per servers, constraint is on CPU usage
	 * Specific to cloud: in cloud, only one VM is allowed per Core
	 * @author cdupont
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
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();

		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); 
		modelGenerator.setRAM_SIZE(100);

		modelGenerator.setVM_TYPE("small");
		
		VMTypeType vmTypes = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(12), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		model.getSite().get(0).setPUE(new PUEType(1.8));
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
		
		//TEST 1
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator(), new NetworkCost(), 
				vmTypes, vmMargins, makeSimpleFed(vmMargins, model));

		
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		if(actionRequest!=null) {
			for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
				if (action.getValue() instanceof MoveVMActionType) 
					moves.add((MoveVMActionType)action.getValue());
				if (action.getValue() instanceof PowerOffActionType) 
					powerOffs.add((PowerOffActionType)action.getValue());
			}	
		}
				
              	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
		//swtiching off the server that consumes more
		assertEquals(powerOffs.get(0).getNodeName(), "id100000");


		//TEST 2
		
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
		
				
		OptimizerEngineCloudTraditional MyOptimizer2 = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator2(), new NetworkCost(), 
				vmTypes, vmMargins, makeSimpleFed(vmMargins, model));
		
		MyOptimizer2.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		moves.clear();
		powerOffs.clear();
		
		if(actionRequest!=null) {
			for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
				if (action.getValue() instanceof MoveVMActionType) 
					moves.add((MoveVMActionType)action.getValue());
				if (action.getValue() instanceof PowerOffActionType) 
					powerOffs.add((PowerOffActionType)action.getValue());
			}	
		}
		
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
		
		//switching off the server that consumes more
		assertEquals(powerOffs.get(0).getNodeName(), "id200000");
		
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
		modelGenerator.setNB_SERVERS(4); //8
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
								
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		List<ServerType> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
		servers.get(0).setStatus(ServerStatusType.OFF);
		servers.get(1).setStatus(ServerStatusType.OFF);
					
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVmMargins = new PolicyType(polL);
		myVmMargins.getPolicy().add(pol);


		optimizer.setPolicies(myVmMargins);
		optimizer.setFederation(makeSimpleFed(myVmMargins, model));
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.runGlobalOptimization(model);
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
		assertEquals(actionRequest.getActionList().getAction().size(), 1);
		assertEquals(powerOns.size(), 1);
		assertEquals(powerOns.get(0).getNodeName(), "id200000");
		
	}
	
	/**
	 * There is too much ON servers, issuing powers OFF
	 */
	public void testAllocationTooMuchServers() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores

		modelGenerator.setVM_TYPE("m1.small");
						
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraMoveVM(true);
		
		List<ServerType> servers = Utils.getAllServers(model.getSite().get(0).getDatacenter().get(0));
		
		//emptying 8 first
		for(int i=0; i<8; i++)
			servers.get(i).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
				
		//add a supplementary core to S1
		servers.get(1).getMainboard().get(0).getCPU().get(0).getCore().add(new CoreType());
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
    	optimizer.setVmTypes(vmTypes);
    	  		
    	
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", 800, 12));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
		
		OptimizerEngine optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, vmMargins, makeSimpleFed(vmMargins, model));
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		List <PowerOffActionType>  powerOffs = new ArrayList<PowerOffActionType>();
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
		
		//turning Off 4 machines: leaving 4 machines alive, that represents 16 cores: within the bounds 8 -> 16 VM slots.
		assertEquals(actionRequest.getActionList().getAction().size() < 12, true);
		assertEquals(powerOffs.size(), 6);
		assertNotSame(powerOffs.get(0).getNodeName(), "id800000");
		
	}
	

	/**
	 * There is too much ON servers, issuing powers OFF
	 */
	public void testAllocationNoVMs() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(8); 
		modelGenerator.setRAM_SIZE(24);
		
		modelGenerator.setVM_TYPE("m1.small");
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();	
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(0.5), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
    	optimizer.setVmTypes(vmTypes);
    	  		
    	PeriodType period = new PeriodType(
    			begin, end, null, null, new LoadType("m1.small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);

		
		OptimizerEngine optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, vmMargins, makeSimpleFed(vmMargins, model));
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		List <PowerOffActionType>  powerOffs = new ArrayList<PowerOffActionType>();
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
		
		//turning Off 4 machines: leaving 4 machines alive, that represents 16 cores: within the bounds 8 -> 16 VM slots.
		assertEquals(actionRequest.getActionList().getAction().size(), 7);
		assertEquals(powerOffs.size(), 7);

		
	}
	
	/**
	 *
	 */
	public void testGlobalConstraintOnNbCoresCharged(){
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		modelGenerator.setCPU_USAGE(0);
		
		modelGenerator.setVM_TYPE("CPU_constraint");
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();
		
		//emptying VM1
		RackableServerType S1 = modelManyServersNoLoad.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1);
		VirtualMachineType VM1 = S1.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().get(0);
		
		//nulify non used values (should work without)
		VM1.setNumberOfCPUs(null);
		VM1.setActualCPUUsage(null);
		VM1.setActualDiskIORate(null);
		VM1.setActualMemoryUsage(null);
		VM1.setActualNetworkUsage(null);
		VM1.setActualStorageUsage(null);

				
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(0.1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
				
		optimizer.getVmTypes().getVMType().add(type1);
		
		optimizer.runGlobalOptimization(modelManyServersNoLoad);
		
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
		
              	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());

		//VM are packed 4 by servers, following number of cores
		assertTrue(moves.size()==36);
		//one machine is kept alive
		assertEquals(powerOffs.size(), 9);

	}
	
	/**
	 * Test with a very loaded configuration
	 */
    @Test
	public void testGlobalChargedTraditional(){
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(50); //Was 50
		modelGenerator.setNB_VIRTUAL_MACHINES(1);//Was 1
		modelGenerator.setRAM_SIZE(800); //was 8000
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		 
		
		modelGenerator.IS_CLOUD = false;
		modelGenerator.setCPU_USAGE(70);
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(100);
		modelGenerator.setVM_TYPE("small");
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		//FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);

		
		PeriodType period = new PeriodType(
    			begin, end, null, null, new LoadType("m1.small", 300, 7));
		

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType vmMargins = new PolicyType(polL);
		

		SLAType slas = new SLAType();
		
		QoSDescriptionType qos = new QoSDescriptionType();
		qos.setMaxVirtualCPUPerCore(new QoSDescriptionType.MaxVirtualCPUPerCore((float)2.0, (Integer)1));
				
		SLAType.SLA sla = new SLAType.SLA();
		slas.getSLA().add(sla);
		sla.setCommonQoSRelatedMetrics(qos);
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	
		
		ClusterType clusters = createDefaultCluster(modelGenerator.MAX_NB_SERVERS, slas.getSLA().get(0), vmMargins); 
		
		OptimizerEngineCloudTraditional myOptimizer2 = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				VMs, vmMargins, makeSimpleFed(vmMargins, model));

		myOptimizer2.setClusterType(clusters);
		
		//TEST 1
		
		myOptimizer2.runGlobalOptimization(model);
		
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}

		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());

		assertTrue(moves.size() != 0);
		
	}


	/**
	 * Test global with constraints not satisfied
	 */
	public void testGlobalTooMuchVMs() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);//8
		modelGenerator.setNB_VIRTUAL_MACHINES(4);
		
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); 
		modelGenerator.setRAM_SIZE(24);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(10), new MemoryUsageType(1), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		
		SLAType slas = new SLAType();
		
		QoSDescriptionType qos = new QoSDescriptionType();
		MaxVirtualCPUPerCore mvCPU = new MaxVirtualCPUPerCore();
		qos.setMaxVirtualCPUPerCore(mvCPU);
		qos.getMaxVirtualCPUPerCore().setValue((float) 1.0);
				
		SLAType.SLA sla = new SLAType.SLA();
		slas.getSLA().add(sla);
		sla.setCommonQoSRelatedMetrics(qos);
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PeriodType period = new PeriodType(
    			begin, end, null, null, new LoadType("m1.small", -1, -1));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(pol));	
		
		
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		nodeName.add("id200000");
		nodeName.add("id300000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id400000");
		nodeName.add("id500000");
		nodeName.add("id600000");
		nodeName.add("id700000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		ClusterType clusters = new ClusterType(cluster);

		optimizer.setClusterType(clusters);
		optimizer.setSla(slas);
		optimizer.setVmTypes(VMs);
		
		//TEST 1
		//8 VMS -> full servers
				
		//transferring VMs
		List<VirtualMachineType> VMs0 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs1 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		VMs0.addAll(VMs1);
		VMs1.clear();
				
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionList response = actionRequest.getActionList();
		
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
	
		assertEquals(moves.size(),4);
			
	}
	

}



