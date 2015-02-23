/**
* ============================== Header ============================== 
* file:          OptimizerGlobalCloudTest.java
* project:       FIT4Green/Optimizer
* created:       10 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-05-01 00:59:19 +0200 (mar, 01 may 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1406 $
* 
* short description:
*   Optimizer cloud allocation algorithm tests
* ============================= /Header ==============================
*/
package f4g.optimizer;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import f4g.commons.com.util.PowerData;
import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;
import f4g.optimizer.Optimizer.CloudTradCS;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.allocation.TraditionalVmAllocationResponse;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.Load;
import f4g.schemas.java.constraints.optimizerconstraints.NodeController;
import f4g.schemas.java.constraints.optimizerconstraints.Period;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;

/**
 * Test singe allocation with Entropy
 *
 */
public class OptimizerAllocationTest extends OptimizerTest {
	
	PolicyType vmMargins;
	/**
	 * Construction of the test suite
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		Period period = new Period(
				begin, end, null, null, new Load(new SpareCPUs(3, UnitType.ABSOLUTE), null));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);
		

		vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        SLAGenerator.createVirtualMachine(), vmMargins, makeSimpleFed(vmMargins, null));
		optimizer.setSla(SLAGenerator.createDefaultSLA());
	}


	/**
	 * Destruction
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;
	}

	/**
	 * Test allocation success
	 */
	public void testAllocationSuccess() {
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponse);
				
		CloudVmAllocationResponse VMAllocResponse = (CloudVmAllocationResponse) response.getResponse().getValue();
		
		//New VM should be allocated on a server		
		assertNotNull(VMAllocResponse);
	}
	
	/**
	 * Test allocation with void parameters 
	 *
	 * @author cdupont
	 */
	public void testAllocationVoid() {

		AllocationRequest allocationRequestVoid = new AllocationRequest();
		FIT4Green modelVoid = new FIT4Green();
		
		assertNull(optimizer.allocateResource(null, modelVoid));
		assertNull(optimizer.allocateResource(allocationRequestVoid, null));
		assertNull(optimizer.allocateResource(allocationRequestVoid, modelVoid));
		
	}
	
	/**
	 * Test allocation with many servers full load
	 * There is no space left to allocate a new VM
	 */
	public void testAllocationManyServersFullLoad() {
		
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(6);
		modelGenerator.setCORE(6);
		
		//VMs takes 100% CPU
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().getVCpuLoad().setValue(100);
		
    	FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		AllocationRequest request = createAllocationRequestCloud("m1.small");
		AllocationResponse response = optimizer.allocateResource(request, model);
		
		//No space for new VM		
		assertNull(response.getResponse());
	}

	/**
	 * Test allocation with no servers
	 */
	public void testAllocationNoServer() {
		FIT4Green model = modelGenerator.createFIT4Green();
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");
		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		assertNotNull(response);
		//Null response -> no space for VM
		assertNull(response.getResponse());
	}


	/**
	 * Test allocation with one server and no VM
	 *
	 */
	public void testAllocationOneServerNoVM() {
		
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
	
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
				
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponse);
				
		CloudVmAllocationResponse VMAllocResponse = (CloudVmAllocationResponse) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals("id100000", VMAllocResponse.getNodeId());
	}
	

	/**
	 * Test allocation failure and success with respect to the constraint on CPU
	 */
	public void testAllocationCPUConstraint() {	
		modelGenerator.setNB_SERVERS(1); 
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1);

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();	
					
		//VMs takes 100% CPU
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().getVCpuLoad().setValue(100);
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		//enough CPU: New VM should be allocated on first server		
		assertEquals("id100000", ((CloudVmAllocationResponse) response.getResponse().getValue()).getNodeId());
		//assertEquals(0, getPowerOns().size());
		
		optimizer.getVmTypes().getVMFlavor().get(0).getCapacity().getVCpus().setValue(2);

		response = optimizer.allocateResource(allocationRequest, model);
		
		//not enough CPU: VMs now need 2 CPUs
		assertNull(response.getResponse());
				
	}

	/**
	 * Test allocation failure and success with respect to the constraint on RAM
	 */
	public void testAllocationRAMConstraint() {	

		modelGenerator.setNB_SERVERS(1); 
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		modelGenerator.setCORE(1);
		modelGenerator.setRAM_SIZE(10);

		final FIT4Green model = modelGenerator.createPopulatedFIT4Green();	
	
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		//enough RAM: New VM should be allocated on first server		
		assertEquals("id100000", ((CloudVmAllocationResponse) response.getResponse().getValue()).getNodeId());
		
		optimizer.getVmTypes().getVMFlavor().get(0).getCapacity().getVRam().setValue(2000);

		response = optimizer.allocateResource(allocationRequest, model);
		
		//not enough RAM: 
		assertNull(response.getResponse());
		
	}
	
	/**
	 * Test the filling of all response fields
	 */
	public void testResponseFields() {
		
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
	
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
								
		AllocationRequest allocationRequest = new AllocationRequest();
		
		CloudVmAllocation cloudAlloc = new CloudVmAllocation();
		cloudAlloc.setVm("m1.small");
		cloudAlloc.getClusterId().add("c1");
		cloudAlloc.setImageId("i1");
		cloudAlloc.setUserId("u1");
		
		//Simulates a CloudVmAllocation operation
		JAXBElement<CloudVmAllocation>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id100000");
		allocationRequest.setRequest(operationType);
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeController(nodeName) , null, null, "idc1"));
		ClusterType clusterType = new ClusterType(cluster);
		optimizer.setClusters(clusterType);
		
		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponse);
				
		CloudVmAllocationResponse VMAllocResponse = (CloudVmAllocationResponse) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(  "id100000", VMAllocResponse.getNodeId());
		assertEquals(  "m1.small", VMAllocResponse.getVm());
		assertEquals("c1", VMAllocResponse.getClusterId());
		assertEquals( "i1", VMAllocResponse.getImageId());
		assertEquals(  "u1", VMAllocResponse.getUserId());
	}

	
	/**
	 * Test allocation: VM should be allocated on the server with lowest energy profile
	 * TODO: power idle doesn't have any impact on VM allocation now
	 */
	public void testAllocationPowerIdle() {
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id300000"))
					power.setActualConsumption(8.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}						
		}

		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
				
		ArrayList<String> clusterId = new ArrayList<String>();
		clusterId.add("c1");
		CloudVmAllocation cloudAlloc = new CloudVmAllocation(null, "i1", clusterId, "m1.small", "u1", 0); 
		//public CloudVmAllocation(final QName jaxbElementName, final String imageId, final List<String> clusterId, final String vm, final String userId, final Integer minPriority)
		//Simulates a CloudVmAllocation operation
		JAXBElement<CloudVmAllocation>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		AllocationRequest allocationRequest = new AllocationRequest();
		allocationRequest.setRequest(operationType);
		
		//TEST 1
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator(), new NetworkCost(), 
		        SLAGenerator.createVirtualMachine(), vmMargins, makeSimpleFed(vmMargins, model));
		
		AllocationResponse response = MyOptimizer.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		//assertEquals("id200000", ((CloudVmAllocationResponse)response.getResponse().getValue()).getNodeId());
		
		//TEST 2
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id200000"))
					power.setActualConsumption(8.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}						
		}
				
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer2 = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator2(), new NetworkCost(), 
				SLAGenerator.createVirtualMachine(), vmMargins, makeSimpleFed(vmMargins, model));
		
		AllocationResponse response2 = MyOptimizer2.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		//assertEquals("id100000", ((CloudVmAllocationResponse)response2.getResponse().getValue()).getNodeId());
		
		
	}
	
	/**
	 * Test allocation: VM should be allocated on the server with lowest energy profile
	 */
	public void testAllocationPowerPerVM() {
		
		//Create a Power Calculator that computes a more feeble power for server #1.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id100000"))
					power.setActualConsumption(10.0 + 0.8 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
				else
					power.setActualConsumption(10.0  + 1.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}

		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);		
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
				
		optimizer.setPowerCalculator(new MyPowerCalculator());		
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");	
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		assertEquals("id100000", ((CloudVmAllocationResponse)response.getResponse().getValue()).getNodeId());
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id200000"))
					//TODO check this
					power.setActualConsumption(10.0 + 0.8 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
				else
					power.setActualConsumption(10.0  + 1.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}
		
		optimizer.setPowerCalculator(new MyPowerCalculator2());		
		AllocationResponse response2 = optimizer.allocateResource(allocationRequest, model);
		
		assertEquals("id200000", ((CloudVmAllocationResponse)response2.getResponse().getValue()).getNodeId());
	}
	
	
		

	/**
	 * Test allocation success
	 */
//	public void testAllocationSuccessTraditional() {
//		
//		modelGenerator.setNB_SERVERS(3);
//		modelGenerator.setNB_VIRTUAL_MACHINES(1);
//		modelGenerator.IS_CLOUD = false;
//		
//		modelGenerator.setNB_CPU(1);
//		modelGenerator.setCPU_USAGE(1);
//		modelGenerator.setMEMORY_USAGE(2);
//		
//		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
//		
//		optimizer.setComputingStyle(CloudTradCS.TRADITIONAL);
//		AllocationRequest allocationRequest = createAllocationRequestTrad();
//		optimizer.setClusters(createDefaultCluster(3, null, null));
//		
//		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
//		
//		assertNotNull(response);
//		assertNotNull(response.getResponse());
//		assertTrue(response.getResponse().getValue() instanceof TraditionalVmAllocationResponse);
//				
//		TraditionalVmAllocationResponse VMAllocResponse = (TraditionalVmAllocationResponse) response.getResponse().getValue();
//		
//		//New VM should be allocated on first server		
//		assertEquals("id100000", VMAllocResponse.getNodeId());
//		assertEquals("c1", VMAllocResponse.getClusterId());
//	}
	
	/**
	 * 
	 */
	public void testAllocationAllOff() {
		
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		for(Server s : Utils.getAllServers(model)){
			s.setStatus(ServerStatus.OFF);
		}
				
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");		
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		//BtrPlace will try to boot a server
		assertNotNull(response);
		assertNotNull(response.getResponse());
		
	}
	
	/**
	 * Test allocation with constraints not satisfied
	 * MaxVirtualCPUPerCore is not satisfied on a node -> suppress this node and hosted VMs and allocate anyway
	 */
	public void testAllocationTooMuchVMs() {
		
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
		
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
					
		optimizer.setClusters(createDefaultCluster(8, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxVirtualCPUPerCore(new MaxVirtualCPUPerCore((float)1.0, 1));
		//TEST 1 
		
		AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocation)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocation)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		((CloudVmAllocation)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
	
		
		//8 VMS -> full serverss		
		model = modelGenerator.createPopulatedFIT4Green();
		
		//clearing space on c2
		List<VirtualMachine> VMs4 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(4).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachine> VMs6 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(6).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		
		//overloaded server
		VMs4.addAll(VMs6);
		VMs6.clear();
	
		AllocationResponse response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNull(response.getResponse());

	}
	
	
}
