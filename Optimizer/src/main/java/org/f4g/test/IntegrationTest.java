/**
* ============================== Header ============================== 
* file:          OptimizerTest.java
* project:       FIT4Green/Optimizer
* created:       10 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-07-05 16:23:09 +0200 (jeu. 05 juil. 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1512 $
* 
* short description:
*   Integration with the power calculator tests
* ============================= /Header ==============================
*/
package org.f4g.test;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.CloudTraditional.SLAReader;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import org.f4g.optimizer.utils.OptimizerWorkload;
import org.f4g.optimizer.utils.Recorder;
import org.f4g.optimizer.utils.Utils;
import org.f4g.optimizer.utils.OptimizerWorkload.CreationImpossible;
import org.f4g.power.IPowerCalculator;
import org.f4g.power.PowerCalculator;

import org.f4g.schema.metamodel.CoreLoadType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.FrequencyType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.NrOfPstatesType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageCapacityType;
import org.f4g.schema.metamodel.StorageUsageType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.metamodel.VoltageType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.ActionRequestType.ActionList;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.ObjectFactory;
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
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType.VMType;
import org.f4g.util.Util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import entropy.configuration.Node;


/**
 * Integration with the power calculator tests
 * @author  cdupont
 */
public class IntegrationTest extends OptimizerTest {

	public Logger log;  
		
	OptimizerEngineCloudTraditional optimizer = null;
	SLAGenerator slaGenerator = new SLAGenerator();
	PolicyType vmMargins;

	
	protected void setUp() throws Exception {
		super.setUp();
		
		log = Logger.getLogger(this.getClass().getName()); 
		
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



	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;

	}
	
	
	public void testaddVM() {
		
		//Creating a new model generator
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		//servers settings
		modelGenerator.setCPU(2);
		modelGenerator.setCORE(6); 
		//VM settings
		modelGenerator.setCPU_USAGE(0.0); 
		modelGenerator.setNB_CPU(1);
		modelGenerator.setNETWORK_USAGE(1.0);
		modelGenerator.setSTORAGE_USAGE(1.0);
		modelGenerator.setMEMORY_USAGE(1.0);
					
		FrameworkCapabilitiesType frameworkCapabilitie = new FrameworkCapabilitiesType();
		frameworkCapabilitie.setFrameworkName("FM");
		
		RackableServerType S0 = modelGenerator.createRandomServer(frameworkCapabilitie, 0);
		S0.setStatus(ServerStatusType.OFF);
		
		//Creating a virtual machine
		VirtualMachineType virtualMachine = new VirtualMachineType();
		virtualMachine.setNumberOfCPUs(new NrOfCpusType(1));
		virtualMachine.setActualCPUUsage(new CpuUsageType(0.2));
		virtualMachine.setActualNetworkUsage(new NetworkUsageType(0.0)); 
		virtualMachine.setActualStorageUsage(new StorageUsageType(0.0));
		virtualMachine.setActualMemoryUsage(new MemoryUsageType(0.0));
		virtualMachine.setActualDiskIORate(new IoRateType(0.0)); 
		virtualMachine.setFrameworkID("newVM");
			
		
		IPowerCalculator powerCalculator = new PowerCalculator();
		
		OptimizerWorkload VM;
		try {
			VM = new OptimizerWorkload(virtualMachine);
			
			double powerBefore = powerCalculator.computePowerServer(S0).getActualConsumption();
			
			Utils.addVM(VM, S0, AlgoType.CLOUD);
		
			double powerAfter = powerCalculator.computePowerServer(S0).getActualConsumption();
			
			log.debug("power before: " + powerBefore);
			log.debug("power after: " + powerAfter);
			
			assertTrue(powerBefore != 0);
			assertTrue(powerAfter != 0);
			assertTrue(powerBefore < powerAfter);
			
		} catch (CreationImpossible e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Test allocation with real power calculator
	 */
	public void testAllocation() {
		
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
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(), 
				vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));
		
		AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);
				
		//server xxx consumes less than the others.
		assertEquals(((CloudVmAllocationResponseType)response.getResponse().getValue()).getNodeId(), "id100000");
		
		//TEST 2
		
		List<ServerType> servers = Utils.getAllServers(model);
		
		//add a supplementary core to S0
		CoreType core = new CoreType();
		core.setFrequency(new FrequencyType(1));
		core.setCoreLoad(new CoreLoadType(0.1));
		core.setVoltage(new VoltageType(1.0));
		core.setLastPstate(new NrOfPstatesType(0));
		core.setTotalPstates(new NrOfPstatesType(0));
		servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().add(core);

		
		AllocationResponseType response2 = MyOptimizer.allocateResource(allocationRequest, model);
	            
		//server xxx consumes less than the others.
		assertEquals(((CloudVmAllocationResponseType)response2.getResponse().getValue()).getNodeId(), "id100000");
		
		
	}

	/**
	 * test global optimization with real power calculator
	 */
	public void testGlobal(){
		
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
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(), 
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
		//one VM is moving to switch off a server
		assertEquals(moves.get(0).getSourceNodeController(), "id200000");


		//TEST 2
	
		List<ServerType> servers = Utils.getAllServers(model);
		
		//add a supplementary core to S0
		CoreType core = new CoreType();
		core.setFrequency(new FrequencyType(1));
		core.setCoreLoad(new CoreLoadType(0.1));
		core.setVoltage(new VoltageType(1.0));
		core.setLastPstate(new NrOfPstatesType(0));
		core.setTotalPstates(new NrOfPstatesType(0));
		servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().add(core);
				
		MyOptimizer.runGlobalOptimization(model);
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
		
		// going to the low power server
		assertEquals(moves.get(0).getSourceNodeController(), "id0");
		//assertEquals(moves.get(1).getDestNodeController(), "id100000");
		
	}
	
	
	/**
	 * Test allocation with constraints not satisfied
	 */
	public void testconstraintnotsatisfied() {
		
		
		ModelGenerator modelGenerator = new ModelGenerator();
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		modelGenerator.setCPU(8);
		modelGenerator.setCORE(1); 
		modelGenerator.setRAM_SIZE(24);//24
		
		FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();
				
		modelGenerator.setVM_TYPE("m1.small");
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		VMTypeType.VMType type2 = new VMTypeType.VMType();
		type2.setName("m1.medium");
		type2.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(1), new StorageCapacityType(1)));
		type2.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type2);
				
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
		nodeName.add("id1000000");
		nodeName.add("id1100000");
		nodeName.add("id1200000");
		nodeName.add("id1300000");
		cluster.add(new Cluster("c2", new NodeControllerType(nodeName) , bSlas, bPolicies, "idc2"));
		ClusterType clusters = new ClusterType(cluster);
		
		
		FederationType fed = new FederationType();

		BoundedClustersType bcls = new BoundedClustersType();
		for(Cluster cl: clusters.getCluster()) {
			BoundedClustersType.Cluster bcl = new BoundedClustersType.Cluster();
			bcl.setIdref(cl);
			bcls.getCluster().add(bcl);
		}
		
		fed.setBoundedCluster(bcls);
		
		//Create a new optimizer with the special power calculator
		OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(), 
		        VMs, vmMargins, fed);
		
		MyOptimizer.setClusterType(clusters);
		
		//TEST 1 
		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
			
		//clearing VMs
		List<ServerType> servers = Utils.getAllServers(model);
		for(int i=1; i<8; i++) {
			servers.get(i).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
		}
		 		
		AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
		
	}
	
	
	/**
	 * Test allocation & global with HP SLA
	 */
	public void testHPSLA() {
		
		String sep = System.getProperty("file.separator");
		ModelGenerator modelGenerator = new ModelGenerator();
		FIT4GreenType model = modelGenerator.getModel("resources" + sep	+ "unittest_f4gmodel_instance_ComHP_federated.xml");
			
		try {
			Date date = new Date();
			GregorianCalendar gCalendar = new GregorianCalendar();
			gCalendar.setTime(date);
			XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
			model.setDatetime(xmlCalendar);
		} catch (DatatypeConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		SLAReader sla = new SLAReader("resources" + sep	+ "unittest_SLA_instance_ComHP.xml");
		optimizer.setClusterType(sla.getCluster());
		optimizer.setSla(sla.getSLAs());
		optimizer.setFederation(sla.getFeds());
		optimizer.setClusterType(sla.getCluster());
		optimizer.setPolicies(sla.getPolicies());
		optimizer.setVmTypes(sla.getVMtypes());

		
		AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.xlarge");
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().clear();
		((CloudVmAllocationType)allocationRequest.getRequest().getValue()).getClusterId().add("c1");
	
		 
		//TEST 1
		AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);
		
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);
		
		//TEST 2	
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionList response2 = actionRequest.getActionList();
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response2.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
	
		//assertEquals(powerOffs.size(), 6);
	
		//TEST 3
		Date date = new Date();
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTime(date);
		XMLGregorianCalendar now = null;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
		} catch (DatatypeConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		List<ServerType> servers = Utils.getAllServers(model);
		int maxVM = 9;
		for(int i=0; i<maxVM; i++) {
			VirtualMachineType VM = modelGenerator.createVirtualMachineType(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);		
			servers.get(0).getNativeHypervisor().getVirtualMachine().add(VM);
			VM.setCloudVmType("m1.small");
			VM.setLastMigrationTimestamp(now);
			VM.setFrameworkID("VMa" + i);
		}
//		maxVM = 16;
//		for(int i=0; i<maxVM; i++) {
//			VirtualMachineType VM = modelGenerator.createVirtualMachineType(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);		
//			servers.get(1).getNativeHypervisor().getVirtualMachine().add(VM);
//			VM.setCloudVmType("m1.xlarge");
//			VM.setLastMigrationTimestamp(now);
//			VM.setFrameworkID("VMb" + i);
//		}
		
		//servers.get(2).setStatus(ServerStatusType.OFF);		
		//servers.get(3).setStatus(ServerStatusType.OFF);	
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionList response3 = actionRequest.getActionList();
		
		moves.clear();
		powerOffs.clear();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response3.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
	
		assertTrue(response3.getAction().size() > 0);
		
	}
	
	
	//generation nbConf random configurations with nbServers in the directory path given
	public void generateConfigurations(int nbServers, int nbConf, String path){
		
		for(int i = 0; i < nbConf; i++) {
			generateConfiguration(nbServers, path);
		}
	}
		
	//generation a random configuration with nbServers in the directory path given
	public void generateConfiguration(int nbServers, String path){
		
		int NbVMsperServer = 6;
		int NBVMsTotal = nbServers * NbVMsperServer;
		int nbServers1 = nbServers / 2;
		int nbServers2 = (nbServers % 2 == 0 ? nbServers / 2: nbServers / 2 + 1);
				
		ModelGenerator modelGenerator1 = new ModelGenerator();
		//Server1:
		//CPU Dual CPU, quad-core, Intel® Xeon® E5520 2.27 GHz
		//Memory    24 GB (6 x 4 GB DIMMs)
		//Hard disk  2 x 300 GB
		modelGenerator1.setCPU(2);
		modelGenerator1.setCORE(4);
		modelGenerator1.setFREQUENCY(2.27);
		modelGenerator1.setRAM_SIZE(24);
		modelGenerator1.setSTORAGE_SIZE(600);
		modelGenerator1.setNB_VIRTUAL_MACHINES(0);
		modelGenerator1.setNB_SERVERS(nbServers1);
		modelGenerator1.setNB_ROUTERS(0);
		modelGenerator1.setNB_SWITCHES(0);
		modelGenerator1.NUMBER_OF_TRANSISTORS = 731;
		modelGenerator1.SERVER_FRAMEWORK_ID = 100000;
		
		
		ModelGenerator modelGenerator2 = new ModelGenerator();
		//Server2:
		//CPU Dual CPU, quad-core, Intel® Xeon® E5540 2.53 GHz
		//Memory    24 GB (6 x 4 GB DIMMs)
		//Hard disk  2 x 300 GB
		modelGenerator2.setCPU(2);
		modelGenerator2.setCORE(4);
		modelGenerator2.setFREQUENCY(2.53);
		modelGenerator2.setRAM_SIZE(24);
		modelGenerator2.setSTORAGE_SIZE(600);
		modelGenerator2.setNB_VIRTUAL_MACHINES(0);
		modelGenerator2.setNB_SERVERS(nbServers2);
		modelGenerator2.SERVER_FRAMEWORK_ID = 200000;
		modelGenerator2.NIC_FRAMEWORK_ID = 300000;
		modelGenerator2.NUMBER_OF_TRANSISTORS = 731;
		FIT4GreenType model = modelGenerator1.createPopulatedFIT4GreenType();
		FIT4GreenType model2 = modelGenerator2.createPopulatedFIT4GreenType();
			
			
		//all the servers of model2 are added in model 1. model2 will not be used anymore.
		List<RackableServerType> rackServers = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer();
		
		for(ServerType server2 : Utils.getAllServers(model2)) {
			rackServers.add((RackableServerType) server2);
		}		
		List<ServerType> servers = Utils.getAllServers(model);
		
		String sep = System.getProperty("file.separator");
		final SLAReader sla = new SLAReader("resources" + sep	+ "unittest_SLA_instance_ComHP.xml");

				
		//predicate to determine is a server is full according to our known constraints
		Predicate<ServerType> isFull = new Predicate<ServerType>() { 
		    @Override public boolean apply(ServerType server) { 
		    	
		    	List<VirtualMachineType> vms = Utils.getVMs(server);
			
		    	int sumCPUs = 0;
		    	int sumCPUDemands = 0;
		    	for(VirtualMachineType vm : vms){
		    		VMType SLAVM = Util.findVMByName(vm.getCloudVmType(), sla.getVMtypes());
		    		sumCPUs += SLAVM.getCapacity().getVCpus().getValue();
		    		sumCPUDemands += SLAVM.getExpectedLoad().getVCpuLoad().getValue();
		    	}
			
		    	//constraint MaxVMperServer=15
		    	if(vms.size() >= 15)
		    		return true;
		    	//constraint MaxVirtualCPUPerCore=2
		    	if(sumCPUs >= Utils.getNbCores(server) * 2)
		    		return true;
		    	//regular CPU consumption constraint
		    	if(sumCPUDemands + 100 >= Utils.getNbCores(server) * 100)
		    		return true;
			
		    	return false;
			}
		};
			
		
		Date date = new Date();
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTime(date);
		XMLGregorianCalendar now = null;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
		} catch (DatatypeConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		List<VirtualMachineType> vms = new ArrayList<VirtualMachineType>();
		Random rand = new Random(System.currentTimeMillis());
				
		for(int i=0; i<NBVMsTotal; i++) {
			VirtualMachineType VM = modelGenerator1.createVirtualMachineType(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);		
			VM.setCloudVmType("m1.small");
			VM.setLastMigrationTimestamp(now);
			VM.setFrameworkID("VMa" + i);
			vms.add(VM);
			
			Collection<ServerType> nonFullServers = Collections2.filter(servers, Predicates.not(isFull));
			int size = nonFullServers.size();
			if(size == 0) 
				break;
			
			int item = rand.nextInt(size);
			List<ServerType> myList = new ArrayList<ServerType>();
			myList.addAll(nonFullServers);
			ServerType s = myList.get(item);
			s.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().add(VM);
					
		}
		
		Recorder recorder = new Recorder(true, path);
		recorder.recordModel(model);
		
		
	}
	
	//run all configurations in a directory
	void runConfigurations(String pathName) {
		
		
		String fileName;
		File folder = new File(pathName);
		File[] listOfFiles = folder.listFiles(); 
		 
		for (int i = 0; i < listOfFiles.length; i++) 
		{		 
			if (listOfFiles[i].isFile()) 
			{
				fileName = listOfFiles[i].getName();
				if (fileName.endsWith(".xml"))
				{
					runConfiguration(pathName + File.separator + fileName);
			    }
		    }
		}
	}
	
	//run a configuration file
	void runConfiguration(String pathName) {
		
		ModelGenerator modelGenerator = new ModelGenerator();	
		FIT4GreenType model = modelGenerator.getModel(pathName);
		
		String sep = File.separator;
		final SLAReader sla = new SLAReader("resources" + sep	+ "unittest_SLA_instance_ComHP.xml");
		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(), 
		        slaGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, null));
		
		optimizer.setClusterType(sla.getCluster());
		optimizer.setSla(sla.getSLAs());
		optimizer.setFederation(sla.getFeds());
		optimizer.setClusterType(sla.getCluster());
		optimizer.setPolicies(sla.getPolicies());
		optimizer.setVmTypes(sla.getVMtypes());
		
		optimizer.runGlobalOptimization(model);
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ActionList response3 = actionRequest.getActionList();
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		
		for (JAXBElement<? extends AbstractBaseActionType> action : response3.getAction()){
			if (action.getValue() instanceof MoveVMActionType) 
				moves.add((MoveVMActionType)action.getValue());
			if (action.getValue() instanceof PowerOffActionType) 
				powerOffs.add((PowerOffActionType)action.getValue());
		}
	         	
		log.debug("moves=" + moves.size());
		log.debug("powerOffs=" + powerOffs.size());
		
	}
	
	//generate a sample configurations into XML files in a directory
	public void testGenerateConfigurations(){
		//generateConfigurations(int nbServers, int nbConf, String path)
		generateConfigurations(2, 1, "F4Gmodels");
	}
	
	//run all configurations found in a directory
	public void testRunConfigurations(){
		runConfigurations("F4Gmodels");
	}
	
	//generate sample configurations & run all
	public void testGenerateRunConfigurations(){
		
		deleteDir(new File("F4Gmodels"));
		generateConfigurations(10, 1, "F4Gmodels");
		runConfigurations("F4Gmodels");
	}
	
	public static boolean deleteDir(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }

	    // The directory is now empty so delete it
	    return dir.delete();
	}
	
	   
}
