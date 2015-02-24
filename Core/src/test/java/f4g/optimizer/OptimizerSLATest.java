package f4g.optimizer;

import f4g.commons.com.util.PowerData;
import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.constraints.optimizerconstraints.*;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType.MaxPowerServer;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.HDDCapacity;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerCPULoad;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVMperServer;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVRAMperPhyRAM;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.schemas.java.metamodel.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.*;

import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.measure.quantity.*;

import static javax.measure.unit.SI.*;
import static org.junit.Assert.*;


public class OptimizerSLATest extends OptimizerTest {

	/**
	 * Construction of the optimizer
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Period period = new Period(begin, end, null, null, new Load(null, null));
		
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
				SLAGenerator.createVirtualMachine(), policies, fed);
	    
		optimizer.setSla(SLAGenerator.createDefaultSLA());
		optimizer.setOptiObjective(OptimizationObjective.Power);
		optimizer.setClusters(createDefaultCluster(4, optimizer.getSla().getSLA(), optimizer.getPolicies().getPolicy()));
		
		BoundedClustersType.Cluster bclu = new BoundedClustersType.Cluster(optimizer.getClusters().getCluster().get(0));
		BoundedClustersType bclus = new BoundedClustersType();
		bclus.getCluster().add(bclu);	
		optimizer.getFederation().setBoundedCluster(bclus);
	}

	/**
	 * Test global optimization with one VM per servers and no load
	 */
	@Test
	public void testHDDGlobal() {
		
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setSTORAGE_SIZE(100);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(0));
		optimizer.getSla().getSLA().get(0).getHardwareConstraints().setHDDCapacity(new HDDCapacity(100, 1));

		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());

	}

	/**
	 * Test global optimization with one VM per servers and no load
	 */
	@Test
	public void testSpareCPUs() {

		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCORE(2);
	
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		//TEST 1: no Spare CPU
		optimizer.runGlobalOptimization(model);

		assertEquals(1, getPowerOffs().size());

		//TEST 2: absolute CPU
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(1, UnitType.ABSOLUTE));
		optimizer.runGlobalOptimization(model);

		assertEquals(0, getPowerOffs().size());

		//TEST 3: relative CPU
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(50, UnitType.RELATIVE));
		optimizer.runGlobalOptimization(model);

		assertEquals(0, getPowerOffs().size());
		
		//TEST 4: relative CPU
		optimizer.getPolicies().getPolicy().get(0).getPeriodVMThreshold().get(0).getLoad().setSpareCPUs(new SpareCPUs(0, UnitType.RELATIVE));
		optimizer.runGlobalOptimization(model);

		assertEquals(1, getPowerOffs().size());
	}

	/**
	 * Test global optimization for Max Server CPU load
	 */
	@Test  
	public void testCPUGlobal() {
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setCORE(1);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(50));
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxServerCPULoad(new MaxServerCPULoad(50, 1));
		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());
		assertEquals(0, getPowerOffs().size());

	}

	/**
	 * Test global optimization with one VM per servers and no load
	 */
	@Test
	public void testOverbookingGlobal() {

		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(4); // 16 VMs total
		modelGenerator.setCORE(4);
		modelGenerator.setRAM_SIZE(560);
		modelGenerator.setSTORAGE_SIZE(10000000);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		// TEST 1 low VM consumption
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(1));
		optimizer.runGlobalOptimization(model);

		assertEquals(12, getMoves().size()); // everyone on the same server

		// TEST 2 with overbooking setting = 1
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(100));
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxVirtualCPUPerCore(new MaxVirtualCPUPerCore((float)1.0, 1));
		
		optimizer.runGlobalOptimization(model);


		assertEquals(0, getMoves().size()); //no Overbooking (==1) -> servers already full

		// TEST 3 with overbooking setting = 2
		optimizer.getSla().getSLA().get(0).getQoSConstraints().getMaxVirtualCPUPerCore().setValue((float) 2.0);

		optimizer.runGlobalOptimization(model);

		assertEquals(8, getMoves().size()); 
		
		// TEST 4 with overbooking setting = 1.5
		optimizer.getSla().getSLA().get(0).getQoSConstraints().getMaxVirtualCPUPerCore().setValue((float) 1.5);

		optimizer.runGlobalOptimization(model);

		assertEquals(4, getMoves().size()); 

		// TEST 5 with overbooking setting = 2, mixed VMs
		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(4); // 16 VMs total
		model = modelGenerator.createPopulatedFIT4Green();

		List<VirtualMachine> vms = Utils.getAllVMs(model);
		vms.get(0).setCloudVm("m1.xlarge");
		vms.get(1).setCloudVm("m1.xlarge");
		vms.get(2).setCloudVm("m1.xlarge");
		vms.get(3).setCloudVm("m1.xlarge");

		optimizer.getSla().getSLA().get(0).getQoSConstraints().getMaxVirtualCPUPerCore().setValue((float) 2.0);

		optimizer.runGlobalOptimization(model);
		
		assertEquals(4, getMoves().size()); 
	}

	@Test
	public void testMaxVMperServerGlobal() {
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(0));
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxVMperServer(new MaxVMperServer(1, 1));

		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());
	}
	
	@Test
	public void testMemoryOverbookingGlobal() {
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setRAM_SIZE(100);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(0));
		optimizer.getVmTypes().getVMFlavor().get(0).getCapacity().setVRam(new RAMSize(50));
		optimizer.getSla().getSLA().get(0).getQoSConstraints().setMaxVRAMperPhyRAM(new MaxVRAMperPhyRAM((float)0.9, 1));

		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());
	}


	/**
	 * Test VM Payback Time
	 */
	@Test
	public void testVMPaybackTimeGlobal() {

		class MyNetworkCost implements ICostEstimator {

			@Override
			public boolean dispose() {return false;}

			@Override
			public Amount<Duration> moveDownTimeCost(NetworkNode fromServer,
					NetworkNode toServer, VirtualMachine VM,
					FIT4Green model) {
				return null;
			}

			@Override
			public Amount<Energy> moveEnergyCost(NetworkNode fromServer,
					NetworkNode toServer, VirtualMachine VM,
					FIT4Green model) {
				
				return Amount.valueOf(1000, JOULE);
			}

			@Override
			public Amount<Money> moveFinancialCost(NetworkNode fromServer,
					NetworkNode toServer, VirtualMachine VM,
					FIT4Green model) {
				return null;
			}
		}

		modelGenerator.setNB_SERVERS(4);
		modelGenerator.setNB_VIRTUAL_MACHINES(4); // 16 VMs total

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		optimizer.setCostEstimator(new MyNetworkCost());
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(0));
		
		// TEST 1: with payback time = 1 min		
		optimizer.getPolicies().getPolicy().get(0).setVMMigrationPaybacktime(1);
		
		optimizer.runGlobalOptimization(model);
		
		//payback time = 60s, move = 100J, power saved = 2.5W (A server is 10W, we have to move 4 VMs to switch is off, 
		// all servers energy profile are equivalent).
		// 2.5*60 < 1000, move disallowed
		assertEquals(0, getMoves().size());
		
		// TEST 2: with payback time = 10 min		
		optimizer.getPolicies().getPolicy().get(0).setVMMigrationPaybacktime(10);
		
		optimizer.runGlobalOptimization(model);

		assertEquals(12, getMoves().size());
		
	}
	
		
	@Test	
	public void testDelayBetweenMoveGlobal() {
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		try {			
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar VMLastTimeMove = factory.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT")));
			VMLastTimeMove.add(factory.newDuration(false, 0, 0, 0, 0, 4, 0)); // 4 Minutes negative Duration
			List<VirtualMachine> vms = Utils.getAllVMs(model);
			for (VirtualMachine vmt : vms){
				vmt.setLastMigrationTimestamp(VMLastTimeMove);
			}	

		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		optimizer.getPolicies().getPolicy().get(0).setDelayBetweenMove(5);
		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());
		assertEquals(0, getPowerOffs().size());

	}
	
	@Test
	public void testDelayBetweenOnOffGlobal() {
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

		FIT4Green model = modelGenerator.createPopulatedFIT4Green();

		try {			
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar LastTimeOnOff = factory.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT")));
			LastTimeOnOff.add(factory.newDuration(false, 0, 0, 0, 0, 4, 0)); // 4 Minutes negative Duration
			List<Server> ns = Utils.getAllServers(model);
			for (Server n : ns){
				n.setLastOnOffTimestamp(LastTimeOnOff);
			}	

		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		optimizer.getPolicies().getPolicy().get(0).setDelayBetweenOnOff(5);
		optimizer.runGlobalOptimization(model);

		assertEquals(0, getPowerOffs().size());

	}

    /**
     * Unit tests to check F4GCPUOverbooking2.
     * @author Fabien Hermenier
     */
//    public void testCPUOverbooking2() {
//        Configuration src = new SimpleConfiguration();
//        Node n1 = new SimpleNode("N1", 5, 30, 30);
//        Node n2 = new SimpleNode("N2", 5, 30, 30);
//        Node n3 = new SimpleNode("N3", 5, 30, 30);
//        src.addOnline(n1);
//        src.addOnline(n2);
//        src.addOnline(n3);
//        for (int i = 0; i < 15; i++) {
//            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
//            src.setRunOn(vm, src.getOnlines().get(0)); //Only on the first node
//        }
//        ChocoCustomRP rp = new ChocoCustomRP(new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8 , 9));
//        rp.setRepairMode(false);
//        rp.setTimeLimit(0);
//        rp.doOptimize(false);
//        VJob v = new DefaultVJob("v");
//
//        List<VJob> vjobs = new ArrayList<VJob>();
//        vjobs.add(v);
//
//        try {
//        TimedReconfigurationPlan res = rp.compute(src, src.getRunnings(),
//                        src.getWaitings(),
//                        src.getSleepings(),
//                        new SimpleManagedElementSet<VirtualMachine>(),
//                        new SimpleManagedElementSet<Node>(),
//                        new SimpleManagedElementSet<Node>(),
//                        vjobs);
//            Assert.assertEquals(0, res.size());
//        } catch (PlanException e) {
//            Assert.fail(e.getMessage());
//        }
//        //x.reset();
//
//        v.addConstraint(new F4GCPUOverbookingConstraint2(src.getOnlines(), 2D));
//        v.addConstraint(new Ban(src.getAllVirtualMachines(), new SimpleManagedElementSet<Node>(n3))); //Prevent the use of node3
//        //Overbooking factor of 2, so 2 nodes will be used. Ideally, 10 VMs on n1, 4 VMs on n2
//        try {
//         //   x  = new DefaultVcpuPcpuMapping(rp.getModel());
//            TimedReconfigurationPlan res = rp.compute(src, src.getRunnings(),
//                    src.getWaitings(),
//                    src.getSleepings(),
//                    new SimpleManagedElementSet<VirtualMachine>(),
//                    new SimpleManagedElementSet<Node>(),
//                    new SimpleManagedElementSet<Node>(),
//                    vjobs);
//            Assert.assertEquals(5, res.size());
//            Configuration dst = res.getDestination();
//            Assert.assertEquals(10, dst.getRunnings(n1).size());
//            Assert.assertEquals(5, dst.getRunnings(n2).size());
//            //Control of the pCPU usage
//            VcpuPcpuMapping mapping = DefaultVcpuPcpuMapping.getInstances();
//            Assert.assertEquals(5, mapping.getPcpuUsage(n1).getVal());
//            Assert.assertEquals(3, mapping.getPcpuUsage(n2).getVal()); //3 but the last is "half full"
//            //x.reset();
//        } catch (PlanException e) {
//            Assert.fail(e.getMessage());
//        }
//        DefaultVcpuPcpuMapping.getInstances().reset();
//        vjobs.clear();
//        v = new DefaultVJob("v");
//        v.addConstraint(new F4GCPUOverbookingConstraint2(src.getOnlines(), 1D)); //No cpu overbooking. 5 VMs per node
//        vjobs.add(v);
//        try {
//            TimedReconfigurationPlan res = rp.compute(src, src.getRunnings(),
//                    src.getWaitings(),
//                    src.getSleepings(),
//                    new SimpleManagedElementSet<VirtualMachine>(),
//                    new SimpleManagedElementSet<Node>(),
//                    new SimpleManagedElementSet<Node>(),
//                    vjobs);
//            Assert.assertEquals(10, res.size());
//            Configuration dst = res.getDestination();
//            Assert.assertEquals(5, dst.getRunnings(n1).size());
//            Assert.assertEquals(10, dst.getRunnings(n1).size() + dst.getRunnings(n2).size());
//            Assert.assertTrue(dst.getRunnings(n3).size() <= 5 && dst.getRunnings(n2).size() <= 5);
//        } catch (PlanException e) {
//            Assert.fail(e.getMessage());
//        }
//    }
//
//    /**
//     * Unit tests to check SpareCPU2.
//     * @author Fabien Hermenier
//     */
//    public void testSpareCPU2() {
//        Configuration src = new SimpleConfiguration();
//        Node n1 = new SimpleNode("N1", 4, 30, 30);
//        Node n2 = new SimpleNode("N2", 4, 30, 30);
//        Node n3 = new SimpleNode("N3", 4, 30, 30);
//        src.addOnline(n1);
//        src.addOnline(n2);
//        src.addOnline(n3);
//        for (int i = 0; i < 20; i++) {
//            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
//            src.setRunOn(vm, src.getOnlines().get(0)); //Only on the first node
//        }
//        ChocoCustomRP rp = new ChocoCustomRP(new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8 , 9));
//        rp.setRepairMode(false);
//        rp.setTimeLimit(0);
//        rp.doOptimize(false);
//        VJob v = new DefaultVJob("v");
//
//        List<VJob> vjobs = new ArrayList<VJob>();
//        vjobs.add(v);
//
//        v.addConstraint(new F4GCPUOverbookingConstraint2(src.getOnlines(), 2D)); //at most 2 vCPU per CPU on each node
//        ManagedElementSet<Node> s = new SimpleManagedElementSet<Node>(n1);
//        s.add(n2);
//        v.addConstraint(new SpareCPUs2(s, 2)); //Let 2 free pCPU on n1 + n2. So at most 12 vCPU on the 2 nodes (cumulated)
//        //n3 will then support the last VMs.
//        try {
//            TimedReconfigurationPlan res = rp.compute(src, src.getRunnings(),
//                    src.getWaitings(),
//                    src.getSleepings(),
//                    new SimpleManagedElementSet<VirtualMachine>(),
//                    new SimpleManagedElementSet<Node>(),
//                    new SimpleManagedElementSet<Node>(),
//                    vjobs);
//            Configuration dst = res.getDestination();
//            Assert.assertEquals(12, dst.getRunnings(n1).size() + dst.getRunnings(n2).size());
//            Assert.assertEquals(8, dst.getRunnings(n3).size());
//        } catch (PlanException e) {
//            Assert.fail(e.getMessage());
//        }
//        DefaultVcpuPcpuMapping.getInstances().reset();
//    }
    
	/**
	 * Test Max Power per Server constraint
	 */
	@Test
	public void testMaxPowerServerGlobal() {
	
		modelGenerator.setNB_SERVERS(2);
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setSTORAGE_SIZE(100);
		FIT4Green model = modelGenerator.createPopulatedFIT4Green();
		
		class MyPowerCalculator extends MockPowerCalculator {
			public PowerData computePowerServer(Server server) {
				PowerData power = new PowerData();
				power.setActualConsumption(1000.0  + 1.0 * server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
								
				log.debug("computePowerServer:" + power.getActualConsumption());
				return power;
			}								
		}
		
		optimizer.setPowerCalculator(new MyPowerCalculator());
		optimizer.getVmTypes().getVMFlavor().get(0).getExpectedLoad().setVCpuLoad(new CpuUsage(100));
		optimizer.getSla().getSLA().get(0).getEnergyConstraints().setMaxPowerServer(new MaxPowerServer(1100, 1));

		optimizer.runGlobalOptimization(model);

		assertEquals(0, getMoves().size());

	}
}
