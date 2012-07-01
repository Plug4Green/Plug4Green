package org.f4g.test;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.util.LoadCalculator;
import org.f4g.cost_estimator.NetworkCost;
import static javax.measure.units.NonSI.*;
import static javax.measure.units.SI.*;
import org.jscience.physics.measures.Measure;
import org.jscience.economics.money.*;
import org.jscience.economics.money.Currency;
import javax.measure.quantities.*;

import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.NetworkPortType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.metamodel.SiteType;
import org.f4g.optimizer.ICostEstimator;
import org.f4g.power.IPowerCalculator;
import org.f4g.power.PoweredNetworkNode;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.NetworkPortBufferSizeType;
import org.f4g.schema.metamodel.BitErrorRateType;
import org.f4g.schema.metamodel.PropagationDelayType;
import org.f4g.schema.metamodel.NetworkTrafficType;
import org.f4g.schema.metamodel.LinkType;
import org.f4g.schema.metamodel.FlowType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.StorageUsageType;



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

        assertEquals(srcNetNode.getNetworkPort().size(), 1); 
        assertEquals(dstNetNode.getNetworkPort().size(), 1);
        assertEquals(allNetdevs.get(0).getNetworkPort().size(), 2);
        assertEquals(allNetdevs.get(1).getNetworkPort().size(), 2);
        assertEquals(allNetdevs.get(2).getNetworkPort().size(), 2);
        assertEquals(allNetdevs.get(3).getNetworkPort().size(), 2);
        assertEquals(allNetdevs.get(4).getNetworkPort().size(), 2);

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
        assertEquals( energycost, 0.0 );

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


