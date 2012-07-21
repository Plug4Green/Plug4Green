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


import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.apache.log4j.Logger;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import org.f4g.optimizer.CloudTraditional.SLAReader;
import org.f4g.optimizer.utils.OptimizerWorkload;
import org.f4g.optimizer.utils.OptimizerWorkload.CreationImpossible;
import org.f4g.optimizer.utils.Recorder;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.IPowerCalculator;
import org.f4g.power.PowerCalculator;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType.ActionList;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.allocation.*;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.constraints.optimizerconstraints.*;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType.VMType;
import org.f4g.schema.metamodel.*;
import org.f4g.util.Util;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.*;


/**
 * Integration with the power calculator tests
 *
 * @author cdupont
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
        JAXBElement<CloudVmAllocationType> operationType = (new ObjectFactory()).createCloudVmAllocation(cloudAlloc);
        AllocationRequestType allocationRequest = new AllocationRequestType();
        allocationRequest.setRequest(operationType);

        //TEST 1

        //Create a new optimizer with the special power calculator
        OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(),
                vmTypes, myVmMargins, makeSimpleFed(myVmMargins, model));

        AllocationResponseType response = MyOptimizer.allocateResource(allocationRequest, model);

        //server xxx consumes less than the others.
        assertEquals(((CloudVmAllocationResponseType) response.getResponse().getValue()).getNodeId(), "id100000");

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
        assertEquals(((CloudVmAllocationResponseType) response2.getResponse().getValue()).getNodeId(), "id100000");


    }

    /**
     * test global optimization with real power calculator
     */
    public void testGlobal() {
        //generate one VM per server
        //VMs ressource usage is 0
        ModelGenerator modelGenerator = new ModelGenerator();

        modelGenerator.setNB_SERVERS(3);
        modelGenerator.setNB_VIRTUAL_MACHINES(1);

        //servers settings
        modelGenerator.setCPU(1);
        modelGenerator.setCORE(4);
        modelGenerator.setRAM_SIZE(100);
        modelGenerator.CPU_VOLTAGE = 2;

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

        //Create a new optimizer with the power calculator
        OptimizerEngineCloudTraditional MyOptimizer = new OptimizerEngineCloudTraditional(new MockController(), new PowerCalculator(), new NetworkCost(),
                vmTypes, vmMargins, makeSimpleFed(vmMargins, model));


        MyOptimizer.runGlobalOptimization(model);
        try {
            actionRequestAvailable.acquire();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
        List<PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();

        if (actionRequest != null) {
            for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()) {
                if (action.getValue() instanceof MoveVMActionType)
                    moves.add((MoveVMActionType) action.getValue());
                if (action.getValue() instanceof PowerOffActionType)
                    powerOffs.add((PowerOffActionType) action.getValue());
            }
        }


        log.debug("moves=" + moves.size());
        log.debug("powerOffs=" + powerOffs.size());
        //one VM is moving to switch off a server
        assertEquals(moves.get(0).getSourceNodeController(), "id200000");


        //TEST 2

        List<ServerType> servers = Utils.getAllServers(model);

        //server 0 has less power usage
        servers.get(0).getMainboard().get(0).getCPU().get(0).getCore().get(0).setFrequency(new FrequencyType(0.5));

        MyOptimizer.runGlobalOptimization(model);
        try {
            actionRequestAvailable.acquire();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        moves.clear();
        powerOffs.clear();

        if (actionRequest != null) {
            for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()) {
                if (action.getValue() instanceof MoveVMActionType)
                    moves.add((MoveVMActionType) action.getValue());
                if (action.getValue() instanceof PowerOffActionType)
                    powerOffs.add((PowerOffActionType) action.getValue());
            }
        }

        log.debug("moves=" + moves.size());
        log.debug("powerOffs=" + powerOffs.size());

        // going to the low power server
        assertEquals(moves.get(0).getSourceNodeController(), "id100000");

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

        MyOptimizer.setClusterType(clusters);

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
        ModelGenerator modelGenerator = new ModelGenerator();
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
        optimizer.setClusterType(sla.getCluster());
        optimizer.setSla(sla.getSLAs());
        optimizer.setFederation(sla.getFeds());
        optimizer.setClusterType(sla.getCluster());
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
        try {
            actionRequestAvailable.acquire();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ActionList response2 = actionRequest.getActionList();

        List<MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
        List<PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();

        for (JAXBElement<? extends AbstractBaseActionType> action : response2.getAction()) {
            if (action.getValue() instanceof MoveVMActionType)
                moves.add((MoveVMActionType) action.getValue());
            if (action.getValue() instanceof PowerOffActionType)
                powerOffs.add((PowerOffActionType) action.getValue());
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
        for (int i = 0; i < maxVM; i++) {
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

        for (JAXBElement<? extends AbstractBaseActionType> action : response3.getAction()) {
            if (action.getValue() instanceof MoveVMActionType)
                moves.add((MoveVMActionType) action.getValue());
            if (action.getValue() instanceof PowerOffActionType)
                powerOffs.add((PowerOffActionType) action.getValue());
        }

        log.debug("moves=" + moves.size());
        log.debug("powerOffs=" + powerOffs.size());

        assertTrue(response3.getAction().size() > 0);

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
