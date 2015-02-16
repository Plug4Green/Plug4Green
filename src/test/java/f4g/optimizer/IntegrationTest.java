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
package f4g.optimizer;



import org.apache.log4j.Logger;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import f4g.optimizer.cloudTraditional.SLAReader;
import f4g.optimizer.utils.OptimizerWorkload;
import f4g.optimizer.utils.OptimizerWorkload.CreationImpossible;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.powerCalculator.power.PowerCalculator;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.ActionRequest.ActionList;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.*;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.*;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.schemas.java.metamodel.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.File;
import java.util.*;

import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.UnitType;

/**
 * Integration with the power calculator tests
 *
 * @author cdupont
 */
public class IntegrationTest extends OptimizerTest {

    public Logger log;

    OptimizerEngineCloudTraditional optimizer = null;
    PolicyType vmMargins;


    protected void setUp() throws Exception {
        super.setUp();

        log = Logger.getLogger(this.getClass().getName());

        List<Load> load = new LinkedList<Load>();
        load.add(new Load(new SpareCPUs(3, UnitType.ABSOLUTE), null));


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

    }


    protected void tearDown() throws Exception {
        super.tearDown();
        optimizer = null;

    }


    public void testaddVM() {

        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        modelGenerator.setCPU(2);
        modelGenerator.setCORE(6);

        FrameworkCapabilities frameworkCapabilitie = new FrameworkCapabilities();
        frameworkCapabilitie.setFrameworkName("FM");

        RackableServer S0 = modelGenerator.createRandomServer(frameworkCapabilitie, 0);
        S0.setStatus(ServerStatus.OFF);

        //Creating a virtual machine
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setNumberOfCPUs(new NrOfCpus(1));
        virtualMachine.setActualCPUUsage(new CpuUsage(0.2));
        virtualMachine.setActualNetworkUsage(new NetworkUsage(0.0));
        virtualMachine.setActualStorageUsage(new StorageUsage(0.0));
        virtualMachine.setActualMemoryUsage(new MemoryUsage(0.0));
        virtualMachine.setActualDiskIORate(new IoRate(0.0));
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

        modelGenerator.setNB_SERVERS(5);
        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        //servers settings
        modelGenerator.setCPU(1);
        modelGenerator.setCORE(4); //4 cores
        modelGenerator.setVM_TYPE("small");

        FIT4Green model = modelGenerator.createPopulatedFIT4Green();

        VMTypeType vms = new VMTypeType();

        VMTypeType.VMType type1 = new VMTypeType.VMType();
        type1.setName("small");
        type1.setCapacity(new CapacityType(new NrOfCpus(1), new RAMSize(1), new StorageCapacity(0)));
        type1.setExpectedLoad(new ExpectedLoad(new CpuUsage(100), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
        vms.getVMType().add(type1);

        List<Load> load = new LinkedList<Load>();
        load.add(new Load(new SpareCPUs(3, UnitType.ABSOLUTE), null));

        Period period = new Period(
                begin, end, null, null, new Load(new SpareCPUs(3, UnitType.ABSOLUTE), null));

        PolicyType.Policy pol = new Policy();
        pol.getPeriodVMThreshold().add(period);

        List<Policy> polL = new LinkedList<Policy>();
        polL.add(pol);

        PolicyType myVmMargins = new PolicyType(polL);
        myVmMargins.getPolicy().add(pol);

        ArrayList<String> clusterId = new ArrayList<String>();
        clusterId.add("c1");
        CloudVmAllocation cloudAlloc = new CloudVmAllocation(null, "i1", clusterId, "small", "u1", 0);

        //Simulates a CloudVmAllocation operation
        JAXBElement<CloudVmAllocation> operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
        AllocationRequest allocationRequest = new AllocationRequest();
        allocationRequest.setRequest(operationType);

        //TEST 1

        //Create a new optimizer with the special power calculator
        OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(),
                vms, myVmMargins, makeSimpleFed(myVmMargins, model));

        AllocationResponse response = MyOptimizer.allocateResource(allocationRequest, model);

        //server xxx consumes less than the others.
        assertEquals("id100000", ((CloudVmAllocationResponse) response.getResponse().getValue()).getNodeId());

        //TEST 2

        List<Server> servers = Utils.getAllServers(model);

        //add a supplementary core to S0
        Core core = new Core();
        core.setFrequency(new Frequency(1));
        core.setCoreLoad(new CoreLoad(0.1));
        core.setVoltage(new Voltage(1.0));
        core.setLastPstate(new NrOfPstates(0));
        core.setTotalPstates(new NrOfPstates(0));
        servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().add(core);


        AllocationResponse response2 = MyOptimizer.allocateResource(allocationRequest, model);

        //server xxx consumes less than the others.
        assertEquals("id100000", ((CloudVmAllocationResponse) response2.getResponse().getValue()).getNodeId());


    }

    /**
     * test global optimization with real power calculator
     */
    public void testGlobal() {

        modelGenerator.setNB_SERVERS(3);
        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        modelGenerator.setCPU(1);
        modelGenerator.setCORE(4);
        modelGenerator.setRAM_SIZE(100);
        modelGenerator.CPU_VOLTAGE = 2;

      
        FIT4Green model = modelGenerator.createPopulatedFIT4Green();

        //TEST 1
        optimizer.setPowerCalculator(new PowerCalculator());
        optimizer.setCostEstimator(new NetworkCost());
        optimizer.runGlobalOptimization(model);

        assertEquals(2, getMoves().size());


        //TEST 2
        List<Server> servers = Utils.getAllServers(model);

        //server 0 has less power usage
        //servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().get(0).setFrequencyMin(new Frequency(0.5));
       // servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().get(0).setFrequency(new Frequency(0.5));
        servers.get(0).getMainboard().get(0).setPowerIdle(new Power(10));//.getCPU().get(0).getCore().get(0).setVoltage(new Voltage(1));

        optimizer.runGlobalOptimization(model);
      
        // going to the low power server
        assertEquals("id100000", getMoves().get(0).getSourceNodeController());

    }


    /**
     * Test allocation with constraints not satisfied
     */
    public void testconstraintnotsatisfied() {
        modelGenerator.setNB_SERVERS(4);
        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        modelGenerator.setCPU(8);
        modelGenerator.setCORE(1);
        modelGenerator.setRAM_SIZE(24);

        FIT4Green model = modelGenerator.createPopulatedFIT4Green2Sites();


        VMTypeType VMs = new VMTypeType();

        VMTypeType.VMType type1 = new VMTypeType.VMType();
        type1.setName("m1.small");
        type1.setCapacity(new CapacityType(new NrOfCpus(1), new RAMSize(1), new StorageCapacity(1)));
        type1.setExpectedLoad(new ExpectedLoad(new CpuUsage(50), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
        VMs.getVMType().add(type1);
        VMTypeType.VMType type2 = new VMTypeType.VMType();
        type2.setName("m1.medium");
        type2.setCapacity(new CapacityType(new NrOfCpus(1), new RAMSize(1), new StorageCapacity(1)));
        type2.setExpectedLoad(new ExpectedLoad(new CpuUsage(50), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
        VMs.getVMType().add(type2);

        SLAType slas = new SLAType();

        QoSConstraintsType qos = new QoSConstraintsType();
        MaxVirtualCPUPerCore mvCPU = new MaxVirtualCPUPerCore();
        qos.setMaxVirtualCPUPerCore(mvCPU);
        qos.getMaxVirtualCPUPerCore().setValue((float) 1.0);

        SLAType.SLA sla = new SLAType.SLA();
        slas.getSLA().add(sla);
        sla.setQoSConstraints(qos);
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
        cluster.add(new Cluster("c1", new NodeController(nodeName), bSlas, bPolicies, "idc1"));
        nodeName = new ArrayList<String>();
        nodeName.add("id1000000");
        nodeName.add("id1100000");
        nodeName.add("id1200000");
        nodeName.add("id1300000");
        cluster.add(new Cluster("c2", new NodeController(nodeName), bSlas, bPolicies, "idc2"));
        ClusterType clusters = new ClusterType(cluster);


        FederationType fed = new FederationType();

        BoundedClustersType bcls = new BoundedClustersType();
        for (Cluster cl : clusters.getCluster()) {
            BoundedClustersType.Cluster bcl = new BoundedClustersType.Cluster();
            bcl.setIdref(cl);
            bcls.getCluster().add(bcl);
        }

        fed.setBoundedCluster(bcls);

        //Create a new optimizer with the special power calculator
        OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(),
                VMs, vmMargins, fed);

        MyOptimizer.setClusters(clusters);

        //TEST 1

        AllocationRequest allocationRequest = createAllocationRequestCloud("m1.small");
        ((CloudVmAllocation) allocationRequest.getRequest().getValue()).getClusterId().clear();
        ((CloudVmAllocation) allocationRequest.getRequest().getValue()).getClusterId().add("c1");

        //clearing VMs
        List<Server> servers = Utils.getAllServers(model);
        for (int i = 1; i < 8; i++) {
            servers.get(i).getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
        }

        AllocationResponse response = MyOptimizer.allocateResource(allocationRequest, model);

        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponse);

    }


    /**
     * Test allocation & global with HP SLA
     */
    public void testHPSLA() {

        String sep = System.getProperty("file.separator");
        FIT4Green model = modelGenerator.getModel("resources" + sep + "unittest_f4gmodel_instance_ComHP_federated.xml");

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


        SLAReader sla = new SLAReader("resources" + sep + "unittest_SLA_instance_ComHP.xml");
        optimizer.setClusters(sla.getCluster());
        optimizer.setSla(sla.getSLAs());
        optimizer.setFederation(sla.getFeds());
        optimizer.setClusters(sla.getCluster());
        optimizer.setPolicies(sla.getPolicies());
        optimizer.setVmTypes(sla.getVMtypes());


        AllocationRequest allocationRequest = createAllocationRequestCloud("m1.xlarge");
        ((CloudVmAllocation) allocationRequest.getRequest().getValue()).getClusterId().clear();
        ((CloudVmAllocation) allocationRequest.getRequest().getValue()).getClusterId().add("c1");


        //TEST 1
        AllocationResponse response = optimizer.allocateResource(allocationRequest, model);

        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponse);

        //TEST 2

        optimizer.runGlobalOptimization(model);
      
        assertEquals(6, getPowerOffs().size());

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

        List<Server> servers = Utils.getAllServers(model);
        int maxVM = 9;
        for (int i = 0; i < maxVM; i++) {
            VirtualMachine VM = modelGenerator.createVirtualMachine(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);
            servers.get(0).getNativeHypervisor().getVirtualMachine().add(VM);
            VM.setCloudVm("m1.small");
            VM.setLastMigrationTimestamp(now);
            VM.setFrameworkID("VMa" + i);
        }

        optimizer.runGlobalOptimization(model);
   
        assertEquals(getMoves().size(), 0);

    }



    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
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
