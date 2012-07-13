package org.f4g.test;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.f4g.cost_estimator.NetworkCost;
import org.f4g.entropy.plan.constraint.PlacementConstraintFactory;
import org.f4g.optimizer.CloudTraditional.ConstraintReader;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.CloudTraditional.SLAReader;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.constraints.optimizerconstraints.CapacityType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.ExpectedLoadType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.placement.Capacity;

import entropy.configuration.VirtualMachine;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author TS
 */
public class OptimizerPlacementContraintTest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 *
	 * @author TS
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
	 * @author TS
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;
	}
	
	
	
		
	/**
	 * Test global optimization with one VM per servers and no load
	 *
	 * @author Ts
	 */
	public void testCapacityConstraint() {
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);

		//VM settings
		modelGenerator.setCPU_USAGE(0.0);
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(0);
		modelGenerator.VM_TYPE = "m1.small";
		
		//servers settings
		modelGenerator.setCPU(8);
		modelGenerator.setCORE(16); 
		modelGenerator.setRAM_SIZE(560);
		modelGenerator.setSTORAGE_SIZE(10000000);
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(0.2), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);		
		
		((OptimizerEngineCloudTraditional)optimizer).setVmTypes(vmTypes);
		
		
		//TODO: Insert the following command to optimizer with correct src and model
		PlacementConstraintFactory cf = new PlacementConstraintFactory(null, null, null);
		
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();				
		SLAReader sla = new SLAReader("resources\\SLAClusterConstraints2.xml");
		optimizer.setClusterType(sla.getCluster());
		optimizer.setConstraintType(sla.getPCs());
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
		
		Set<MoveVMActionType> moveSet = new HashSet<MoveVMActionType>(moves);
		Set<PowerOffActionType> powerOffSet = new HashSet<PowerOffActionType>(powerOffs); 
		assertTrue(moveSet.size()==1);
			
	}
	
//	public void testtest(){
//		
//		VirtualMachine vm = new DefaultVirtualMachine("name", 2, 70, 3);
//		vm.setCPUDemand(30);
//		System.out.println(vm.getCPUConsumption());
//		System.out.println(vm.getCPUDemand());
//		System.out.println(vm.getMemoryConsumption());
//		System.out.println(vm.getMemoryDemand());
//		
//	}
}