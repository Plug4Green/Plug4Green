package f4g.optimizer;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.cloud.OptimizerEngineCloud;
import f4g.optimizer.cloud.SLAReader;
import f4g.optimizer.utils.OptimizerWorkload;
import f4g.optimizer.utils.OptimizerWorkload.CreationImpossible;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.powerCalculator.power.PowerCalculator;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.*;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.metamodel.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.File;
import java.io.IOException;
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

    OptimizerEngineCloud optimizer = null;
    PolicyType vmMargins;
    
    @Before
    public void setUp() throws Exception {
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
        optimizer = new OptimizerEngineCloud(new MockController(), new MockPowerCalculator(), new NetworkCost(),
                SLAGenerator.createVirtualMachine(), vmMargins, makeSimpleFed(vmMargins, null));

    }

    @Test
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

            Utils.addVM(VM, S0);

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
    @Test
    public void testAllocation() {

        modelGenerator.setNB_SERVERS(5);
        modelGenerator.setNB_VIRTUAL_MACHINES(1);
        //servers settings
        modelGenerator.setCPU(1);
        modelGenerator.setCORE(4); //4 cores
        modelGenerator.setVM_TYPE("small");

        FIT4Green model = modelGenerator.createPopulatedFIT4Green();

        VMFlavorType vms = new VMFlavorType();

        VMFlavorType.VMFlavor type1 = new VMFlavorType.VMFlavor();
        type1.setName("small");
        type1.setCapacity(new CapacityType(new NrOfCpus(1), new RAMSize(1), new StorageCapacity(0)));
        type1.setExpectedLoad(new ExpectedLoad(new CpuUsage(100), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
        vms.getVMFlavor().add(type1);

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
        OptimizerEngineCloud MyOptimizer = new OptimizerEngineCloud(new MockController(), new PowerCalculator(), new NetworkCost(),
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
    @Test
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
        servers.get(0).getMainboard().get(0).setPowerMax(new Power(10));

        optimizer.runGlobalOptimization(model);
      
        // going to the low power server
        assertEquals("id100000", getMoves().get(0).getDestNodeController());
    }


    /**
     * Test allocation & global with HP SLA
     */
    @Test
    public void testHPSLA() {

        String sep = System.getProperty("file.separator");
        File modelFile = new File("src" + sep + "main" + sep + "resources" + sep + "optimizer" + sep + "unittest_f4gmodel_instance_ComHP_federated.xml");
        assertTrue(modelFile.exists());
        FIT4Green model = null;
	try {
	    model = modelGenerator.getModel(modelFile.getCanonicalPath());
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
       
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


        SLAReader sla = new SLAReader("src" + sep + "main" + sep + "resources" + sep + "optimizer" + sep + "unittest_SLA_instance_ComHP.xml");
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
      
        assertEquals(8, getPowerOffs().size());

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
