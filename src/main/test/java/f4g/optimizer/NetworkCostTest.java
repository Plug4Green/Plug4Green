package f4g.optimizer;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.commons.util.LoadCalculator;
import f4g.optimizer.cost_estimator.NetworkCost;
import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;
import javax.measure.quantity.*;
import static javax.measure.unit.SI.*;

import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.NetworkNodeType;
import f4g.schemas.java.metamodel.NetworkPortType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import f4g.schemas.java.metamodel.SiteType;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.power.IPowerCalculator;
import f4g.commons.power.PoweredNetworkNode;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.NetworkPortBufferSizeType;
import f4g.schemas.java.metamodel.BitErrorRateType;
import f4g.schemas.java.metamodel.PropagationDelayType;
import f4g.schemas.java.metamodel.NetworkTrafficType;
import f4g.schemas.java.metamodel.LinkType;
import f4g.schemas.java.metamodel.FlowType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.MemoryUsageType;
import f4g.schemas.java.metamodel.StorageUsageType;



public class NetworkCostTest extends TestCase {
    
    NetworkCost networkCost;
    FIT4GreenType model;
    List<ServerType> allServers;
    List<NetworkNodeType> allNetdevs;
    
    public NetworkCostTest(String name) { 
        super(name);
    }
    
	protected void setUp() throws Exception {
		super.setUp();
        networkCost = new NetworkCost();

		ModelGenerator modelGenerator = new ModelGenerator();

		// Servers' settings
		modelGenerator.setNB_SERVERS(2); 
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(1);
    
        // VMS' settings
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		modelGenerator.setVM_TYPE("m1.small");
		
        // Network settings
        modelGenerator.setNB_SWITCHES(2);
        modelGenerator.setNB_ROUTERS(3);
        ModelGenerator.defaultSwitchPowerIdle = new PowerType( 100.0 );
        ModelGenerator.defaultSwitchPowerMax = new PowerType( 100.0 );
        ModelGenerator.defaultRouterPowerIdle = new PowerType( 100.0 );
        ModelGenerator.defaultRouterPowerMax = new PowerType( 100.0 );
       
        // Populate model
        model = modelGenerator.createPopulatedFIT4GreenType();

        allServers = Utils.getAllServers(model);
        allNetdevs = Utils.getAllNetworkDeviceNodes(model);

        // Create Topology:
        //
        //            server0 ---\
        //                         switch0 ---\
        //                                       router1
        //                                          |
        //                                       router2
        //                                          |
        //                                       router3
        //                         switch4 ---/
        //            server1 ---/             

        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(0), allNetdevs.get(0));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(1), allNetdevs.get(4));
        ModelGenerator.connectNetDevsFullDuplex(allNetdevs.get(0), allNetdevs.get(1));
        ModelGenerator.connectNetDevsFullDuplex(allNetdevs.get(1), allNetdevs.get(2));
        ModelGenerator.connectNetDevsFullDuplex(allNetdevs.get(2), allNetdevs.get(3));
        ModelGenerator.connectNetDevsFullDuplex(allNetdevs.get(3), allNetdevs.get(4));
        
	}
    
	protected void tearDown() throws Exception {
		super.tearDown();
	}
    
    public void testSwitchConnections() {

        NetworkNodeType srcNetNode = allServers.get(0).getMainboard().get(0).getEthernetNIC().get(0);
        NetworkNodeType dstNetNode = allServers.get(1).getMainboard().get(0).getEthernetNIC().get(0);

        assertEquals(1, srcNetNode.getNetworkPort().size()); 
        assertEquals(1, dstNetNode.getNetworkPort().size());
        assertEquals(2, allNetdevs.get(0).getNetworkPort().size());
        assertEquals(2, allNetdevs.get(1).getNetworkPort().size());
        assertEquals(2, allNetdevs.get(2).getNetworkPort().size());
        assertEquals(2, allNetdevs.get(3).getNetworkPort().size());
        assertEquals(2, allNetdevs.get(4).getNetworkPort().size());

        // NICs match servers' IDs
        assertEquals( allServers.get(0).getFrameworkID(), srcNetNode.getFrameworkID() );
        assertEquals( allServers.get(1).getFrameworkID(), dstNetNode.getFrameworkID() );
    }

    
    
    
    public void testMoveCost() {

        NetworkNodeType srcServer = allServers.get(0).getMainboard().get(0).getEthernetNIC().get(0);
        NetworkNodeType dstServer = allServers.get(1).getMainboard().get(0).getEthernetNIC().get(0);
        double xfertime=0.0, energycost=0.0;
        
        VirtualMachineType vm = new VirtualMachineType();

        // 1: VM size 0 -> no transfer, no energy 
        vm.setActualStorageUsage( new StorageUsageType(0.0) );
        vm.setActualMemoryUsage( new MemoryUsageType(0.0) );
        energycost = networkCost.moveEnergyCost(srcServer, dstServer, vm, model).doubleValue(JOULE);
        assertEquals(0.0 ,  energycost);

        // 2
        vm.setActualStorageUsage( new StorageUsageType(45.0) );
        vm.setActualMemoryUsage( new MemoryUsageType(45.0) );

        xfertime = networkCost.moveDownTimeCost(srcServer, dstServer, vm, null).doubleValue(SECOND);
        assertTrue( xfertime > 134.0 );  
        assertTrue( xfertime < 135.0 );  

        energycost = networkCost.moveEnergyCost(srcServer, dstServer, vm, model).doubleValue(JOULE);
        assertTrue( energycost < 0.1 );
         
        // 3
        vm.setActualStorageUsage( new StorageUsageType(1000000.0) );
        vm.setActualMemoryUsage( new MemoryUsageType(1000000.0) );

        energycost = networkCost.moveEnergyCost(srcServer, dstServer, vm, model).doubleValue(JOULE);
        assertTrue( energycost > 0.3 );
        
	}
    
    
}


