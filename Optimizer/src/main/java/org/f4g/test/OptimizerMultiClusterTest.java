/**
* ============================== Header ============================== 
* file:          OptimizerGlobalCloudTest.java
* project:       FIT4Green/Optimizer
* created:       10 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-05-02 00:47:35 +0200 (mié, 02 may 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1411 $
* 
* short description:
*   Optimizer cloud allocation algorithm tests
* ============================= /Header ==============================
*/
package org.f4g.test;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.SiteType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedClustersType;
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
import org.f4g.schema.constraints.optimizerconstraints.SpareCPUs;
import org.f4g.schema.constraints.optimizerconstraints.UnitType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;


/**
 * Test singe allocation with Entropy
 *
 */
public class OptimizerMultiClusterTest extends OptimizerTest {
	
	SLAGenerator slaGenerator = new SLAGenerator();
	PolicyType vmMargins;
	
	/**
	 * Construction of the test suite
	 */
	protected void setUp() throws Exception {
		super.setUp();
		

		List<LoadType> load = new LinkedList<LoadType>();
		load.add(new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType(new SpareCPUs(0, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
		
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(pol);
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);
		
		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachineType(), vmMargins, fed);
	    
	}


	/**
	 * Destruction
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;
	}

	/**
	 * Test allocation with multiple clusters
	 */
	public void testAllocationWithClusters() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(100);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	
		
		
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id200000");
		nodeName.add("id300000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		ClusterType clusters = new ClusterType(cluster);
		SLAType slas = new SLAType();

		optimizer.setClusterType(clusters);
		
		//TEST 1
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),"id200000");
		assertEquals(VMAllocResponse.getClusterId(),"c2");
		
		//TEST 2
		
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		

		AllocationResponseType response2 = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse2 = (CloudVmAllocationResponseType) response2.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse2.getNodeId(),"id0");
		assertEquals(VMAllocResponse2.getClusterId(),"c1");
	}
	

	/**
	 * Test with 2 DC and 2 clusters in each DC
	 *
	 * @author cdupont
	 */
	public void testPowerOnOffClusters() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		
		//VM settings
		modelGenerator.VM_TYPE = "Ridiculous";
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("Ridiculous");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(pol));	
				
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id200000");
		nodeName.add("id300000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		nodeName = new ArrayList<String>();
		nodeName.add("id1000000");
		nodeName.add("id1100000");
		cluster.add(new Cluster("c3", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc3"));
		nodeName = new ArrayList<String>();
		nodeName.add("id1200000");
		nodeName.add("id1300000");
		cluster.add(new Cluster("c4", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc4"));
		ClusterType clusters = new ClusterType(cluster);
		SLAType slas = new SLAType();

		
		PolicyType vmMargins = new PolicyType();
		vmMargins.getPolicy().add(pol);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();			
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, vmMargins, makeSimpleFed(vmMargins, model));

		MyOptimizer.setClusterType(clusters);
		MyOptimizer.setVmTypes(vmTypes);
		MyOptimizer.setSla(slas);
		
			
	
		MyOptimizer.runGlobalOptimization(model);
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
	
		assertEquals(moves.size(), 0);
		assertEquals(powerOffs.size(), 4);

	}

	
	/**
	 * Test with different policies on cluster & federation
	 *
	 * @author cdupont
	 */
	public void testPowerOnOffClustersAndFederation() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		
		//VM settings
		modelGenerator.VM_TYPE = "Ridiculous";
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("Ridiculous");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(pol));	
				
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id200000");
		nodeName.add("id300000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		nodeName = new ArrayList<String>();
		nodeName.add("id1000000");
		nodeName.add("id1100000");
		cluster.add(new Cluster("c3", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc3"));
		nodeName = new ArrayList<String>();
		nodeName.add("id1200000");
		nodeName.add("id1300000");
		cluster.add(new Cluster("c4", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc4"));
		ClusterType clusters = new ClusterType(cluster);
		SLAType slas = new SLAType();
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();			

		PolicyType.Policy polFed = new Policy();
		
		PeriodType periodFed = new PeriodType(
				begin, end, null, null, new LoadType(new SpareCPUs(17, UnitType.ABSOLUTE), null));

		polFed.getPeriodVMThreshold().add(periodFed);
				
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(polFed);
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);
		
		BoundedClustersType bcls = new BoundedClustersType();
		for(Cluster cl: clusters.getCluster()) {
			BoundedClustersType.Cluster bcl = new BoundedClustersType.Cluster();
			bcl.setIdref(cl);
			bcls.getCluster().add(bcl);
		}
		
		fed.setBoundedCluster(bcls);
			
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, vmMargins, fed);

		MyOptimizer.setClusterType(clusters);
		MyOptimizer.setVmTypes(vmTypes);
		MyOptimizer.setSla(slas);
		
			
	
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionRequestType.ActionList response = actionRequest.getActionList();

		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}

		log.debug("powerOffs=" + powerOffs.size());

		//with 17 cores needed, the federation policy enforces to have 5 servers on
		//whereas the cluster per cluster policy needed only 4.
		assertEquals(powerOffs.size(), 3);

	}
	
	/**
	 * Test global optimization with 2 DC and migrations between allowed
	 *
	 * @author cdupont
	 */
	public void test2DCMigrationInter() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		optimizer.runGlobalOptimization(model);
		     		
		assertEquals(getMoves().size(), 1);
		assertEquals(getPowerOffs().size(), 1);		
	}
		


	
	
	/**
	 * Test global optimization with 2 DC and migrations between allowed
	 *
	 * @author cdupont
	 */
	public void test2DCMigrationIntra() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(2);
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
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();				
	
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
	
		assertTrue(moves.size()==2);
		assertTrue(powerOffs.size()==2);
		//Check that VMs moves only inside a DC
		assertEquals(moves.get(0).getDestNodeController(), "id100000");
		assertEquals(moves.get(1).getDestNodeController(), "id1000000");
		

	}
	
	/**
	 * Test global optimization based on PUE
	 * @author cdupont
	 */
	public void test2SitesMigrationInterPUE() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
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
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		
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
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id0");
		
		//TEST 2
		
		//set a different PUE
		model.getSite().get(0).getPUE().setValue(3.0);
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response = actionRequest.getActionList();
		
		moves.clear();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id1000000");
			
	}
	
	/**
	 * Test global optimization based on CUE
	 * @author cdupont
	 */
	public void test2SitesMigrationInterCUE() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
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
		
		//optimizer according to CO2
		optimizer.setOptiObjective(OptimizationObjective.CO2);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		
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
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id1000000");
		
		//TEST 2
		
		//set a different PUE
		model.getSite().get(0).getCUE().setValue(10.0);
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response = actionRequest.getActionList();
		
		moves.clear();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id0");
		
	}
	
	/**
	 * Test global optimization based on CUE
	 * @author cdupont
	 */
	public void test2SitesMigrationInterPUEorCUE() {
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
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
		
		//optimizer according to CO2
		optimizer.setOptiObjective(OptimizationObjective.CO2);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		
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
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id1000000");
		
		//TEST 2
		
		optimizer.setOptiObjective(OptimizationObjective.Power);
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response = actionRequest.getActionList();
		
		moves.clear();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
	
		assertTrue(moves.size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals(moves.get(0).getDestNodeController(),"id0");
		
	}
	

	/**
	 * 
	 */
	public void testAllocationAllOffSecondCluster() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(100);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(2).setStatus(ServerStatusType.OFF);
		//model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(3).setStatus(ServerStatusType.OFF);
		
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	
		
		
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id200000");
		nodeName.add("id300000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		ClusterType clusters = new ClusterType(cluster);
		SLAType slas = new SLAType();
			
		optimizer.setClusterType(clusters);
		
		//TEST 1
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),"id100000");
		assertEquals(VMAllocResponse.getClusterId(),"c1");
		
	}
	
	/**
	 * Test allocation with multiple clusters
	 */
	public void testAllocationOneClusterFull() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(8); 
		modelGenerator.setRAM_SIZE(24);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
	
		VMTypeType vmTypes = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(1), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		optimizer.setVmTypes(vmTypes);
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	
		
		
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

		
		//TEST 1 two clusters, free space
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		

		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse2 = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first cluster		
		assertEquals(VMAllocResponse2.getNodeId(),"id100000");
		assertEquals(VMAllocResponse2.getClusterId(),"c1");
		
		//TEST 2 two clusters, no more space on c1
		
		//8 VMS -> full servers
		modelGenerator.setNB_VIRTUAL_MACHINES(8);
		model = modelGenerator.createPopulatedFIT4GreenType();
		
		//clearing space on c2
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(4).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(5).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
		
		response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		VMAllocResponse2 = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on second cluster		
		assertEquals(VMAllocResponse2.getNodeId(),"id500000");
		assertEquals(VMAllocResponse2.getClusterId(),"c2");
			
	}
	
	

	/**
	 * Test global with one cluster non repairable
	 */
	public void testGlobalOneClusterBroken() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
		modelGenerator.setRAM_SIZE(24);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
	
		VMTypeType vmTypes = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(10), new MemoryUsageType(1), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		optimizer.setVmTypes(vmTypes);
		
		SLAType slas = createDefaultSLA();
		SLAType.SLA sla = new SLAType.SLA();
		SLAType.SLA sla2 = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		BoundedSLAsType bSlas2 = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));
		bSlas2.getSLA().add(new BoundedSLAsType.SLA(sla2));	
		
		//adding a constraint in sla2
		QoSDescriptionType qos = new QoSDescriptionType();
		MaxVirtualCPUPerCore mvCPU = new MaxVirtualCPUPerCore();
		qos.setMaxVirtualCPUPerCore(mvCPU);
		qos.getMaxVirtualCPUPerCore().setValue((float) 1.0);
		sla.setCommonQoSRelatedMetrics(qos);
		
		
		optimizer.setSla(slas);
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	
		
		
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id100000");
		nodeName.add("id200000");
		nodeName.add("id300000");
		nodeName.add("id400000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc1"));
		nodeName = new ArrayList<String>();
		nodeName.add("id500000");
		nodeName.add("id600000");
		nodeName.add("id700000");
		nodeName.add("id800000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas2, bPolicies, "idc2"));
		ClusterType clusters = new ClusterType(cluster);
			
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(policy);
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);

		
		optimizer.setFederation(fed);
		optimizer.setClusterType(clusters);

		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType(null, null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVMMargins = new PolicyType(polL);
		myVMMargins.getPolicy().add(pol);
		
		optimizer.setPolicies(myVMMargins);
		
		//transferring VMs
		List<VirtualMachineType> VMs0 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs4 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(4).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		VMs0.addAll(VMs4);
		VMs4.clear();
		

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
	
		//no moves should be issued on cluster one because it is broken
		assertEquals(moves.size(), 2);
				
	}
	
	/**
	 * Test if the framework cabability Move and Live Migrate is working
	 * @author cdupont
	 */
	
	public void testMoveVSLiveMigrate() {
		
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
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		
		
		SLAType.SLA sla = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));	
		
		PolicyType.Policy policy = new PolicyType.Policy();
		
		BoundedPoliciesType bPolicies = new BoundedPoliciesType();
		bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy));	

		SLAType slas = new SLAType();
			
		//TEST 1 - with Move capability
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);

		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				VMs, vmMargins, makeSimpleFed(vmMargins, model));
		
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ActionRequestType.ActionList response = actionRequest.getActionList();
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
	
		assertTrue(moves.size()==3);
		
		
		//TEST 2 - with Live Migrate
		
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
	
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response = actionRequest.getActionList();
		
		List <LiveMigrateVMActionType> liveMigrate = new ArrayList<LiveMigrateVMActionType>();
		
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof LiveMigrateVMActionType) 
				liveMigrate.add((LiveMigrateVMActionType)action.getValue());
		}
	         	
		log.debug("liveMigrate=" + liveMigrate.size());
	
		assertTrue(liveMigrate.size()==3);
	
		
		//TEST 3 - with a mix
		
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraMoveVM(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraMoveVM(false);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(false);
	
		MyOptimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response = actionRequest.getActionList();
		
		List <LiveMigrateVMActionType> liveMigrate2 = new ArrayList<LiveMigrateVMActionType>();
		List <MoveVMActionType> move2 = new ArrayList<MoveVMActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof LiveMigrateVMActionType) 
				liveMigrate2.add((LiveMigrateVMActionType)action.getValue());
		}
		for (JAXBElement<? extends AbstractBaseActionType> action : response.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				move2.add((MoveVMActionType)action.getValue());
		}

	    log.debug("moves=" + move2.size());
		log.debug("liveMigrates=" + liveMigrate2.size());
	
		assertTrue(liveMigrate2.size()==1);
		assertTrue(move2.size()==2);
	}
	
	
}
