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
import f4g.commons.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import f4g.commons.optimizer.CloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import f4g.commons.optimizer.CloudTraditional.SLAReader;
import f4g.commons.optimizer.utils.OptimizerWorkload;
import f4g.commons.optimizer.utils.OptimizerWorkload.CreationImpossible;
import f4g.commons.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.commons.power.PowerCalculator;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.ActionRequestType.ActionList;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.*;
import f4g.schemas.java.ObjectFactory;
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

        List<LoadType> load = new LinkedList<LoadType>();
        load.add(new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));


        PeriodType period = new PeriodType(
                begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

        PolicyType.Policy pol = new Policy();
        pol.getPeriodVMThreshold().add(period);

        List<Policy> polL = new LinkedList<Policy>();
        polL.add(pol);

        vmMargins = new PolicyType(polL);
        vmMargins.getPolicy().add(pol);
        optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(),
                SLAGenerator.createVirtualMachineType(), vmMargins, makeSimpleFed(vmMargins, null));

    }


    protected void tearDown() throws Exception {
        super.tearDown();
        optimizer = null;

    }


    public void testaddVM() {

        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        modelGenerator.setCPU(2);
        modelGenerator.setCORE(6);

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
        load.add(new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

        PeriodType period = new PeriodType(
                begin, end, null, null, new LoadType(new SpareCPUs(3, UnitType.ABSOLUTE), null));

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
        JAXBElement<CloudVmAllocationType> operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
        AllocationRequestType allocationRequest = new AllocationRequestType();
        allocationRequest.setRequest(operationType);

        //TEST 1

        //Create a new optimizer with the special power calculator
        OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(),
                vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));

        AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);

        //server xxx consumes less than the others.
        assertEquals("id100000", ((CloudVmAllocationResponseType) response.getResponse().getValue()).getNodeId());

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
        assertEquals("id100000", ((CloudVmAllocationResponseType) response2.getResponse().getValue()).getNodeId());


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

      
        FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType();

        //TEST 1
        optimizer.setPowerCalculator(new PowerCalculator());
        optimizer.setCostEstimator(new NetworkCost());
        optimizer.runGlobalOptimization(model);

        assertEquals(2, getMoves().size());


        //TEST 2
        List<ServerType> servers = Utils.getAllServers(model);

        //server 0 has less power usage
        //servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().get(0).setFrequencyMin(new FrequencyType(0.5));
       // servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().get(0).setFrequency(new FrequencyType(0.5));
        servers.get(0).getMainboard().get(0).setPowerIdle(new PowerType(10));//.getCPU().get(0).getCore().get(0).setVoltage(new VoltageType(1));

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

        FIT4GreenType model = modelGenerator.createPopulatedFIT4GreenType2Sites();


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
        cluster.add(new Cluster("c1", new NodeControllerType(nodeName), bSlas, bPolicies, "idc1"));
        nodeName = new ArrayList<String>();
        nodeName.add("id1000000");
        nodeName.add("id1100000");
        nodeName.add("id1200000");
        nodeName.add("id1300000");
        cluster.add(new Cluster("c2", new NodeControllerType(nodeName), bSlas, bPolicies, "idc2"));
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

        AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.small");
        ((CloudVmAllocationType) allocationRequest.getRequest().getValue()).getClusterId().clear();
        ((CloudVmAllocationType) allocationRequest.getRequest().getValue()).getClusterId().add("c1");

        //clearing VMs
        List<ServerType> servers = Utils.getAllServers(model);
        for (int i = 1; i < 8; i++) {
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
        FIT4GreenType model = modelGenerator.getModel("resources" + sep + "unittest_f4gmodel_instance_ComHP_federated.xml");

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


        AllocationRequestType allocationRequest = createAllocationRequestCloud("m1.xlarge");
        ((CloudVmAllocationType) allocationRequest.getRequest().getValue()).getClusterId().clear();
        ((CloudVmAllocationType) allocationRequest.getRequest().getValue()).getClusterId().add("c1");


        //TEST 1
        AllocationResponseType response = optimizer.allocateResource(allocationRequest, model);

        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().getValue() instanceof CloudVmAllocationResponseType);

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

        List<ServerType> servers = Utils.getAllServers(model);
        int maxVM = 9;
        for (int i = 0; i < maxVM; i++) {
            VirtualMachineType VM = modelGenerator.createVirtualMachineType(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);
            servers.get(0).getNativeHypervisor().getVirtualMachine().add(VM);
            VM.setCloudVmType("m1.small");
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
