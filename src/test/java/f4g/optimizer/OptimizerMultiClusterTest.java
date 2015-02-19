package f4g.optimizer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.MemoryUsageType;
import f4g.schemas.java.metamodel.NetworkUsageType;
import f4g.schemas.java.metamodel.NrOfCpusType;
import f4g.schemas.java.metamodel.RAMSizeType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.StorageCapacityType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import f4g.schemas.java.allocation.CloudVmAllocationResponseType;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.AllocationResponseType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType;
import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoadType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.LoadType;
import f4g.schemas.java.constraints.optimizerconstraints.NodeControllerType;
import f4g.schemas.java.constraints.optimizerconstraints.PeriodType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType.SLA;


public class OptimizerMultiClusterTest extends OptimizerTest {
	
	SLAGenerator slaGenerator = new SLAGenerator();
	PolicyType vmMargins;
	
	/**
	 * Construction of the test suite
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		PeriodType period = new PeriodType(begin, end, null, null, new LoadType(null, null));

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
				        SLAGenerator.createVirtualMachineType(), vmMargins, fed);
		
		optimizer.setSla(SLAGenerator.createDefaultSLA());
		optimizer.setClusters(createDefaultCluster(2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
    	
		
	}

	protected ClusterType createMultiCluster(int NumberOfNodes, int NumberOfClusters, List<SLA> sla, List<Policy> policy) {
		
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		for(int c=0; c<NumberOfClusters; c++){
						
			List<String> nodeName = new ArrayList<String>();
			for(int i=0; i<NumberOfNodes; i++){
				nodeName.add("id" + (c*1000000 + i*100000 ));	
			}
			
			BoundedSLAsType bSlas = null;
			if(sla != null) {
				bSlas = new BoundedSLAsType();
				bSlas.getSLA().add(new BoundedSLAsType.SLA(sla.get(0)));	
			} 		
			
			BoundedPoliciesType bPolicies = null;
			if(policy != null) {
				bPolicies = new BoundedPoliciesType();
				bPolicies.getPolicy().add(new BoundedPoliciesType.Policy(policy.get(0)));	
			}
			cluster.add(new Cluster("c" + c, new NodeControllerType(nodeName) , bSlas, bPolicies, "c" + c));
		}
		return new ClusterType(cluster);
	}
	
	
	/**
	 * Test allocation with multiple clusters
	 */
	@Test
	public void testAllocationWithClusters() {
		
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();
					
		optimizer.setClusters(createMultiCluster(1, 2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		//TEST 1
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");	
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals("id1000000", VMAllocResponse.getNodeId());
		assertEquals("c1", VMAllocResponse.getClusterId());
		
		//TEST 2
		
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c0");
		
		AllocationResponseType response2 = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse2 = (CloudVmAllocationResponseType) response2.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals("id0", VMAllocResponse2.getNodeId());
		assertEquals("c0", VMAllocResponse2.getClusterId());
	}
	

	/**
	 * Test with 2 DC and 2 clusters in each DC
	 */
	@Test
	public void testPowerOnOffClusters() {
		
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();			
	
		optimizer.setClusters(createMultiCluster(2, 2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		optimizer.runGlobalOptimization(model);
			
		assertEquals(4, getPowerOffs().size());

	}

	
	/**
	 * Test with different policies on cluster & federation
	 */
	@Test
	public void testPowerOnOffClustersAndFederation() {
		
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();

		optimizer.setClusters(createMultiCluster(2, 2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		BoundedClustersType bcls = new BoundedClustersType();
		for(Cluster cl: optimizer.getClusters().getCluster()) {
			BoundedClustersType.Cluster bcl = new BoundedClustersType.Cluster();
			bcl.setIdref(cl);
			bcls.getCluster().add(bcl);
			cl.getBoundedPolicies().getPolicy().get(0).getIdref().getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(1, UnitType.ABSOLUTE));
		}
		
		PeriodType period = new PeriodType(begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(new BoundedPoliciesType.Policy(pol));		
		
		optimizer.getFederation().setBoundedPolicies(bpols);
		optimizer.runGlobalOptimization(model);

		assertEquals(1, getPowerOffs().size());

	}
	
	/**
	 * Test global optimization with 2 DC and migrations between allowed
	 */
	@Test
	public void test2DCMigrationInter() {
		
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2DC();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(0).getDatacenter().get(1).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		optimizer.runGlobalOptimization(model);
		     		
		assertEquals(1, getMoves().size());
		
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		model.getSite().get(0).getDatacenter().get(1).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		
		
		optimizer.runGlobalOptimization(model);
		     		
		assertEquals(0, getMoves().size());

	}
		
	
	/**
	 * Test global optimization based on PUE
	 */
	@Test
	public void test2SitesMigrationInterPUE() {
		
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		optimizer.runGlobalOptimization(model);
	
		assertEquals(1, getMoves().size());
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id0", getMoves().get(0).getDestNodeController());
		
		//TEST 2		
		//set a different PUE
		model.getSite().get(0).getPUE().setValue(3.0);
		
		optimizer.runGlobalOptimization(model);
			
		assertEquals(1, getMoves().size());
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id1000000", getMoves().get(0).getDestNodeController());
			
	}
	
	/**
	 * Test global optimization based on CUE
	 */
	@Test
	public void test2SitesMigrationInterCUE() {

		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
				
		//optimizer according to CO2
		optimizer.setOptiObjective(OptimizationObjective.CO2);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		optimizer.runGlobalOptimization(model);
		
		assertTrue(getMoves().size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id1000000", getMoves().get(0).getDestNodeController());
		
		//TEST 2
		
		//set a different PUE
		model.getSite().get(0).getCUE().setValue(10.0);
		
		optimizer.runGlobalOptimization(model);

		assertTrue(getMoves().size()==1);
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id0", getMoves().get(0).getDestNodeController());
		
	}
	
	/**
	 * Test global optimization based on CUE
	 */
	@Test
	public void test2SitesMigrationInterPUEorCUE() {
		
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();				
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		
		//TEST 1
		//optimizer according to CO2
		
		optimizer.setOptiObjective(OptimizationObjective.CO2);
		optimizer.runGlobalOptimization(model);
	
	
		assertEquals(1, getMoves().size());
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id1000000", getMoves().get(0).getDestNodeController());
		
		//TEST 2
		
		optimizer.setOptiObjective(OptimizationObjective.Power);		
		optimizer.runGlobalOptimization(model);
	
		assertEquals(1, getMoves().size());
		//everyone goes on the same server because inter DC migration is allowed
		assertEquals("id0", getMoves().get(0).getDestNodeController());
		
	}

	@Test
	public void testAllocationAllOffSecondCluster() {

		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

		optimizer.setClusters(createMultiCluster(1, 2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		optimizer.getClusters().getCluster().get(0).getNodeController().getNodeName().clear();
		optimizer.getClusters().getCluster().get(0).getNodeController().getNodeName().add("id100000");
		optimizer.getClusters().getCluster().get(1).getNodeController().getNodeName().clear();
		optimizer.getClusters().getCluster().get(1).getNodeController().getNodeName().add("id200000");
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1).setStatus(ServerStatusType.OFF);

		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c0");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		
		//TEST 1
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals("id100000", VMAllocResponse.getNodeId());
		assertEquals("c0", VMAllocResponse.getClusterId());
		
	}
	
	/**
	 * Test allocation with multiple clusters
	 */
	@Test
	public void testAllocationOneClusterFull() {

		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);	
		modelGenerator.setCORE(6);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		optimizer.getVmTypes().getVMType().get(0).getExpectedLoad().setVCpuLoad(new CpuUsageType(100));	
		optimizer.setClusters(createMultiCluster(1, 2, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		optimizer.getClusters().getCluster().get(0).getNodeController().getNodeName().clear();
		optimizer.getClusters().getCluster().get(0).getNodeController().getNodeName().add("id100000");
		optimizer.getClusters().getCluster().get(1).getNodeController().getNodeName().clear();
		optimizer.getClusters().getCluster().get(1).getNodeController().getNodeName().add("id200000");
		
		//TEST 1 two clusters, free space		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c0");

		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse2 = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first cluster		
		assertEquals("id100000", VMAllocResponse2.getNodeId());
		assertEquals("c0", VMAllocResponse2.getClusterId());
		
		//TEST 2 two clusters, no more space on c1
		//clearing c0
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(1).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
		//c1 full
		optimizer.getVmTypes().getVMType().get(0).getCapacity().setVCpus(new NrOfCpusType(6));
		response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		VMAllocResponse2 = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on second cluster		
		assertEquals("id200000", VMAllocResponse2.getNodeId());
		assertEquals("c1", VMAllocResponse2.getClusterId());
			
	}
	
	/**
	 * Test global with one cluster non reparable
	 */
	@Test
	public void testGlobalOneClusterBroken() {

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
		
		SLAType slas = SLAGenerator.createDefaultSLA();
		SLAType.SLA sla = new SLAType.SLA();
		SLAType.SLA sla2 = new SLAType.SLA();
		BoundedSLAsType bSlas = new BoundedSLAsType();
		BoundedSLAsType bSlas2 = new BoundedSLAsType();
		bSlas.getSLA().add(new BoundedSLAsType.SLA(sla));
		bSlas2.getSLA().add(new BoundedSLAsType.SLA(sla2));	
		
		//adding a constraint in sla2
		QoSConstraintsType qos = new QoSConstraintsType();
		MaxVirtualCPUPerCore mvCPU = new MaxVirtualCPUPerCore();
		qos.setMaxVirtualCPUPerCore(mvCPU);
		qos.getMaxVirtualCPUPerCore().setValue((float) 1.0);
		sla.setQoSConstraints(qos);
		
		
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
		optimizer.setClusters(clusters);

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
		
		//no moves should be issued on cluster one because it is broken
		assertEquals(2, getMoves().size());
				
	}
	
	/**
	 * Test if the framework capability Move and Live Migrate is working
	 */
	@Test
	public void testMoveVSLiveMigrate() {

		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
			
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();
					
		//TEST 1 - with Move capability
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);

		optimizer.runGlobalOptimization(model);
		assertTrue(getMoves().size()==3);
		
		
		//TEST 2 - with Live Migrate
		
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(false);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
	
		optimizer.runGlobalOptimization(model);
		
		assertTrue(getLiveMigrate().size()==3);
			
		//TEST 3 - with a mix
		
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraMoveVM(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraMoveVM(false);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterMoveVM(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setIntraLiveMigrate(true);
		model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(false);
		model.getSite().get(1).getDatacenter().get(0).getFrameworkCapabilities().get(0).getVm().setInterLiveMigrate(false);
	
		optimizer.runGlobalOptimization(model);
		
		assertTrue(getLiveMigrate().size()==1);
		assertTrue(getMoves().size()==2);
	}
}
