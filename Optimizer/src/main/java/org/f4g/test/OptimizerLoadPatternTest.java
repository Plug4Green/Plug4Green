package org.f4g.test;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;


import org.f4g.cost_estimator.NetworkCost;
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
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.actions.ActionRequestType.ActionList;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.constraints.optimizerconstraints.BoundedPoliciesType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedSLAsType;
import org.f4g.schema.constraints.optimizerconstraints.CapacityType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.ExpectedLoadType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.NodeControllerType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType;
import org.f4g.schema.constraints.optimizerconstraints.RepeatsType;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.test.OptimizerTest.MockController;
import org.f4g.test.OptimizerTest.MockPowerCalculator;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.Optimizer.CloudTradCS;
import org.junit.Ignore;
import org.junit.Test;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author cdupont
 */
public class OptimizerLoadPatternTest extends OptimizerTest {

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

		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachineType(), policies, makeSimpleFed(policies, null));
	    
		//algo = new AlgoGlobal(new MockPowerCalculator(), new NetworkCost(), AlgoType.CLOUD);
		
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
	 * Test load patterns.
	 * There is 20 servers and no VMs. The servers contains one VM slot each.
	 *
	 * @author cdupont
	 */
	public void testBasics() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(20);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
	
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
	
		//VM settings
		modelGenerator.VM_TYPE = "small";
			
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();				
			
		VMTypeType vmTypes = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(modelGenerator.MAX_RAM_SIZE / 2), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(1), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		optimizer.setVmTypes(vmTypes);	
		
		
		// TEST 0
		// no policies -> no on/off
		
		optimizer.setPolicies(new PolicyType());
		optimizer.runGlobalOptimization(model);
		
		ActionRequestType.ActionList response = actionRequest.getActionList();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("powerOffs=" + powerOffs.size());
		assertEquals(powerOffs.size(), 0);
		
		optimizer.getPolicies().getPolicy().add(new Policy());
		optimizer.runGlobalOptimization(model);
		
		response = actionRequest.getActionList();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("powerOffs=" + powerOffs.size());
		assertEquals(powerOffs.size(), 0);
		
		//TEST 1
		//thresholds with no begin and end
		
		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(0, new PeriodType(null, null, null, null, new LoadType("small", 300, 6)));
		pol.getPeriodVMThreshold().add(1, new PeriodType(null, null, null, null, new LoadType("small", 700, 10)));
		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);
		PolicyType myVmMargins = new PolicyType(polL);
		optimizer.setPolicies(myVmMargins);
		
		optimizer.runGlobalOptimization(model);
		
		response = actionRequest.getActionList();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("powerOffs=" + powerOffs.size());
		assertEquals(powerOffs.size(), 14);
		
		//TEST 2
		//test with two non overlapping periods.
		myVmMargins.getPolicy().get(0).getPeriodVMThreshold().add(new PeriodType(null, null, null, null, new LoadType("small", 700, 10)));		
		
		try {
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setStarts(begin);
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setEnds(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2010, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 00 ));
			//second period should be taken
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(1).setStarts(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2010, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 00 ));
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(1).setEnds(end);
			
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		
		optimizer.setPolicies(myVmMargins);
		
		optimizer.runGlobalOptimization(model);
		
		response = actionRequest.getActionList();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("powerOffs=" + powerOffs.size());
		//second period should be taken.
		assertEquals(powerOffs.size(), 10);
		
		//TEST 3
		//test with a duration (1 day) and a repetition (1 week)	
		try {
			//first period starts now and lasts 1 day
			log.debug(model.getDatetime());
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setStarts(model.getDatetime());
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setEnds(end);
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setDuration(DatatypeFactory.newInstance().newDurationDayTime(/*isPositive*/ true, /*day*/ 1, /*hour*/ 0, /*minute*/ 0, /*second*/ 0));
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(0).setRepeats(RepeatsType.WEEKLY);
			//second period last all time
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(1).setStarts(begin);
			myVmMargins.getPolicy().get(0).getPeriodVMThreshold().get(1).setEnds(begin);			
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		
		optimizer.setPolicies(myVmMargins);
		
		optimizer.runGlobalOptimization(model);
		
		response = actionRequest.getActionList();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}		
	          	
		log.debug("powerOffs=" + powerOffs.size());
		//first period should be taken
		assertEquals(powerOffs.size(), 14);
		
		
	}
	
	
}



