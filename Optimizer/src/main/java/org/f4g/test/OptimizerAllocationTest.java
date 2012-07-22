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
package org.f4g.test;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import org.f4g.com.util.PowerData;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.Optimizer.CloudTradCS;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.allocation.TraditionalVmAllocationResponseType;
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
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;



/**
 * Test singe allocation with Entropy
 *
 */
public class OptimizerAllocationTest extends OptimizerTest {
	
	SLAGenerator slaGenerator = new SLAGenerator();
	PolicyType vmMargins;
	/**
	 * Construction of the test suite
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		List<LoadType> load = new LinkedList<LoadType>();
		load.add(new LoadType("m1.small", 300, 6));


		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		vmMargins = new PolicyType(polL);
		vmMargins.getPolicy().add(pol);
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, null));
	    
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
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(12);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),"id100000");
	}
	
	/**
	 * Test allocation with void parameters 
	 *
	 * @author cdupont
	 */
	public void testAllocationVoid() {

		AllocationRequestType allocationRequestVoid = new AllocationRequestType();
		FIT4GreenType modelVoid = new FIT4GreenType();
		
		assertNull(optimizer.allocateResource(null, modelVoid));
		assertNull(optimizer.allocateResource(allocationRequestVoid, null));
		assertNull(optimizer.allocateResource(allocationRequestVoid, modelVoid));
		
	}
	
	/**
	 * Test allocation with many servers full load
	 * There is no space left to allocate a new VM
	 */
	public void testAllocationManyServersFullLoad() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(6);

		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); //2 cores
		modelGenerator.setRAM_SIZE(100);
		
		//VM settings
		modelGenerator.setCPU_USAGE(0.0);
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(0);
		modelGenerator.setVM_TYPE("oneCPU");
    	FIT4GreenType modelManyServersFullLoad = modelGenerator.createPopulatedFIT4GreenType();
    	
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("oneCPU");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
	
    	
    	optimizer.setVmTypes(vmTypes);
    	    	
		AllocationRequestType request = createAllocationRequestCloud("oneCPU");
		
		AllocationResponseType response = optimizer.allocateResource(request, modelManyServersFullLoad);
		
		//No space for new VM		
		assertNull(response.getResponse());
	}

	/**
	 * Test allocation with no servers
	 */
	public void testAllocationNoServer() {
		ModelGenerator modelGenerator = new ModelGenerator();
		FIT4GreenType modelNoServers = modelGenerator.createFIT4GreenType();
				
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, modelNoServers);
		assertNotNull(response);
		//Null response -> no space for VM
		assertNull(response.getResponse());
	}


	/**
	 * Test allocation with one server and no VM
	 *
	 */
	public void testAllocationOneServerNoVM() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
	
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),"id100000");
	}
	

	/**
	 * Test allocation failure and success with respect to the constraint on CPU
	 */
	public void testAllocationCPUConstraint() {	
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();

		modelGenerator.setNB_SERVERS(1); 
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1);
		modelGenerator.setRAM_SIZE(100);

		modelGenerator.setVM_TYPE("CPU_constraint");
		final FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();	
		
		VMTypeType VMs = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(12), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		
		optimizer.setVmTypes(VMs);
			
		AllocationRequestType allocationRequest = createAllocationRequestCloud("CPU_constraint");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		//enough CPU: New VM should be allocated on first server		
		assertEquals(((CloudVmAllocationResponseType) response.getResponse().getValue()).getNodeId(),"id100000");
		
		optimizer.getVmTypes().getVMType().get(0).getCapacity().getVCpus().setValue(2);

		response = optimizer.allocateResource(allocationRequest, model);
		
		//not enough CPU: VMs now need 2 CPUs
		assertNull(response.getResponse());
		
	}

	/**
	 * Test allocation failure and success with respect to the constraint on RAM
	 */
	public void testAllocationRAMConstraint() {	
		
		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();

		modelGenerator.setNB_SERVERS(1); 
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1);
		modelGenerator.setRAM_SIZE(10);

		modelGenerator.setVM_TYPE("CPU_constraint");
		final FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();	
		
		VMTypeType VMs = new VMTypeType();
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		
		optimizer.setVmTypes(VMs);
			
		AllocationRequestType allocationRequest = createAllocationRequestCloud("CPU_constraint");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		//enough RAM: New VM should be allocated on first server		
		assertEquals(((CloudVmAllocationResponseType) response.getResponse().getValue()).getNodeId(),"id100000");
		
		optimizer.getVmTypes().getVMType().get(0).getCapacity().getVRam().setValue(2000);

		response = optimizer.allocateResource(allocationRequest, model);
		
		//not enough RAM: 
		assertNull(response.getResponse());
		
	}
	
	/**
	 * Test the filling of all response fields
	 */
	public void testResponseFields() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(1);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
	
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
				
		AllocationRequestType allocationRequest = new AllocationRequestType();
		
		CloudVmAllocationType cloudAlloc = new CloudVmAllocationType();
		cloudAlloc.setVmType("m1.small");
		cloudAlloc.getClusterId().add("c1");
		cloudAlloc.setImageId("i1");
		cloudAlloc.setUserId("u1");
		
		//Simulates a CloudVmAllocationType operation
		JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id100000");
		allocationRequest.setRequest(operationType);
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , null, null, "idc1"));
		ClusterType clusterType = new ClusterType(cluster);
		optimizer.setClusterType(clusterType);
		
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),   "id100000");
		assertEquals(VMAllocResponse.getVmType(),   "m1.small");
		assertEquals(VMAllocResponse.getClusterId(),"c1");
		assertEquals(VMAllocResponse.getImageId(),  "i1");
		assertEquals(VMAllocResponse.getUserId(),   "u1");
	}

	
	/**
	 * Test allocation: VM should be allocated on the server with lowest energy profile
	 */
	public void testAllocationPowerIdle() {
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id300000"))
					power.setActualConsumption(8.0 + traverser.calculatePower(server).getActualConsumption());
				else
					power.setActualConsumption(10.0  + traverser.calculatePower(server).getActualConsumption());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}						
		}
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		ArrayList<String> clusterId = new ArrayList<String>();
		clusterId.add("c1");
		CloudVmAllocationType cloudAlloc = new CloudVmAllocationType("i1", clusterId, "m1.small", "u1", 0); 
		
		//Simulates a CloudVmAllocationType operation
		JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		AllocationRequestType allocationRequest = new AllocationRequestType();
		allocationRequest.setRequest(operationType);
		
		//TEST 1
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator(), new NetworkCost(), 
		        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, model));
		
		AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		//assertEquals(((CloudVmAllocationResponseType)response.getResponse().getValue()).getNodeId(), "id200000");
		
		//TEST 2
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
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
		        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, model));
		
		AllocationResponseType response2 = MyOptimizer2.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		//assertEquals(((CloudVmAllocationResponseType)response2.getResponse().getValue()).getNodeId(), "id100000");
		
		
	}
	
	/**
	 * Test allocation: VM should be allocated on the server with lowest energy profile
	 */
	public void testAllocationPowerPerVM() {
		
		//Create a Power Calculator that computes a more feeble power for server #1.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id300000"))
					power.setActualConsumption(10.0 + 0.8 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
				else
					power.setActualConsumption(10.0  + 1.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		ArrayList<String> clusterId = new ArrayList<String>();
		clusterId.add("c1");
		CloudVmAllocationType cloudAlloc = new CloudVmAllocationType("i1", clusterId, "m1.small", "u1", 0); 
		
		//Simulates a CloudVmAllocationType operation
		JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		AllocationRequestType allocationRequest = new AllocationRequestType();
		allocationRequest.setRequest(operationType);
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator(), new NetworkCost(), 
		        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, model));
		
		AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);
	            
		//server id100000 consumes less than the others.
		assertEquals(((CloudVmAllocationResponseType)response.getResponse().getValue()).getNodeId(), "id300000");
		
	}
	
	
	/**
	 * Test allocation: VM should be allocated on the server with lowest energy profile
	 */
	public void testAllocationPowerPerVMCharged() {
		
		//Create a Power Calculator that computes a more feeble power for server #1.
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id300000"))
					power.setActualConsumption(1000.0 + 4.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
				else
					power.setActualConsumption(1000.0  + 5.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(5);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(4); //4 cores
		modelGenerator.setVM_TYPE("small");
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
		
		List<LoadType> load = new LinkedList<LoadType>();
		load.add(new LoadType("m1.small", 300, 6));

		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("m1.small", 300, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVmMargins = new PolicyType(polL);
		myVmMargins.getPolicy().add(pol);
		
		ArrayList<String> clusterId = new ArrayList<String>();
		clusterId.add("c1");
		CloudVmAllocationType cloudAlloc = new CloudVmAllocationType("i1", clusterId, "small", "u1", 0); 
		
		//Simulates a CloudVmAllocationType operation
		JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
		AllocationRequestType allocationRequest = new AllocationRequestType();
		allocationRequest.setRequest(operationType);
		
		//TEST 1
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);
				
		//server xxx consumes less than the others.
		assertEquals(((CloudVmAllocationResponseType)response.getResponse().getValue()).getNodeId(), "id300000");
		
		//TEST 2
		
		//Create a Power Calculator that computes a more feeble power for server #2.
		class MyPowerCalculator2 extends MockPowerCalculator {
			public PowerData computePowerServer(ServerType server) {
				PowerData power = new PowerData();
				if(server.getFrameworkID().equals("id200000"))
					power.setActualConsumption(10.0 + 0.8 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
				else
					power.setActualConsumption(10.0  + 1.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}
				
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer2 = new OptimizerEngineCloudTraditional(new MockController(), new MyPowerCalculator2(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		AllocationResponseType response2 = MyOptimizer2.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		assertEquals(((CloudVmAllocationResponseType)response2.getResponse().getValue()).getNodeId(), "id200000");
		
		
	}

		

	/**
	 * Test allocation success
	 */
	public void testAllocationSuccessTraditional() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(3);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(100);
		modelGenerator.IS_CLOUD = false;
		
		modelGenerator.setNB_CPU(1);
		modelGenerator.setCPU_USAGE(100);
		modelGenerator.setMEMORY_USAGE(200);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
	
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		optimizer.setComputingStyle(CloudTradCS.TRADITIONAL);
		AllocationRequestType allocationRequest = createAllocationRequestTrad();
		List<String> nodeName = new ArrayList<String>();
		nodeName.add("id0");
		nodeName.add("id100000");
		nodeName.add("id200000");
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();
		cluster.add(new Cluster("c1", new NodeControllerType(nodeName) , null, null, "idc1"));
		ClusterType clusterType = new ClusterType(cluster);
		optimizer.setClusterType(clusterType);
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof TraditionalVmAllocationResponseType);
				
		TraditionalVmAllocationResponseType VMAllocResponse = (TraditionalVmAllocationResponseType) response.getResponse().getValue();
		
		//New VM should be allocated on first server		
		assertEquals(VMAllocResponse.getNodeId(),"id100000");
		assertEquals(VMAllocResponse.getClusterId(),"c1");
	}
	
	/**
	 * Test allocation success
	 */
	public void testAllocationAllOff() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
	
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(100);
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
		
		for(ServerType s : Utils.getAllServers(model)){
			s.setStatus(ServerStatusType.OFF);
		}
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNull(response.getResponse());
		
	}
	
	/**
	 * Test allocation with constraints not satisfied
	 */
	public void testAllocationTooMuchVMs() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
		modelGenerator.setRAM_SIZE(100);//24
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(1), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
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
		
		//TEST 1 
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
	
		
		//8 VMS -> full serverss		
		model = modelGenerator.createPopulatedFIT4GreenType();
		
		//clearing space on c2
		List<VirtualMachineType> VMs4 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(4).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs5 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(5).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs6 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(6).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs7 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(7).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		
		//seriously overloaded server
		VMs4.addAll(VMs6);
		VMs6.clear();
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
	}
	
	/**
	 * Test allocation with constraints not satisfied
	 */
	public void testAllocationFull() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(8);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(1); 
		modelGenerator.setRAM_SIZE(100);//24
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
				
		optimizer.setVmTypes(VMs);
		
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
		
		//TEST 1 
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c2");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
	
		
		//8 VMS -> full serverss		
		model = modelGenerator.createPopulatedFIT4GreenType();
		
		//clearing space on c2
		List<VirtualMachineType> VMs4 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(4).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs5 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(5).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs6 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(6).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		List<VirtualMachineType> VMs7 = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(7).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
		
		//seriously overloaded server
		VMs4.addAll(VMs6);
		VMs6.clear();
		
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
	}
	
	
	/**
	 *
	 */
	public void testAllocationPolicyNotSatisfied() {
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(10);
		modelGenerator.setNB_VIRTUAL_MACHINES(6);

		modelGenerator.setCPU(1);
		modelGenerator.setCORE(6); //2 cores
		modelGenerator.setRAM_SIZE(100);
		
		//VM settings
		modelGenerator.setCPU_USAGE(0.0);
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(0);
		modelGenerator.setSTORAGE_USAGE(0);
		modelGenerator.setMEMORY_USAGE(0);
		modelGenerator.setVM_TYPE("oneCPU");
    	FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();
    	
		VMTypeType vmTypes = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("oneCPU");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(0)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(100), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		vmTypes.getVMType().add(type1);
	
		//clearing one VM
		model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().get(0).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
    	
    	    	
		AllocationRequestType request = createAllocationRequestCloud("oneCPU");
		
		//This policy is not met by the configuration
		PeriodType period = new PeriodType(
				begin, end, null, null, new LoadType("small", 1000, 6));

		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType myVmMargins = new PolicyType(polL);
		myVmMargins.getPolicy().add(pol);
				
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		
		AllocationResponseType response = MyOptimizer.allocateResource(request, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
				
		CloudVmAllocationResponseType VMAllocResponse = (CloudVmAllocationResponseType) response.getResponse().getValue();
		
		//The policies are not taken into account in allocation, so allocation is possible.		
		assertEquals(VMAllocResponse.getNodeId(),"id100000");

	}


}
