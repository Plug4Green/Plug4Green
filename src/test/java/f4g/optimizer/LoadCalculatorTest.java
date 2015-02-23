package f4g.optimizer;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.metamodel.CPU;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.metamodel.Mainboard;
import f4g.schemas.java.metamodel.PSU;
import f4g.schemas.java.metamodel.RackableServer;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.TowerServer;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.CoreLoad;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.PSULoad;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.metamodel.NrOfCpus;

import f4g.commons.util.LoadCalculator;



public class LoadCalculatorTest extends TestCase {
    
    LoadCalculator loadCalculator;
    
    public LoadCalculatorTest(String name) { 
        super(name);
    }
    
	protected void setUp() throws Exception {
		super.setUp();
        
        loadCalculator = new LoadCalculator();
        
	}
    
	protected void tearDown() throws Exception {
		super.tearDown();
	}
    
    
    public void testVMLoad() {
        
        
        CPU acpu = new CPU();  
        acpu.getCore().add(new Core());
        acpu.getCore().add(new Core());
        acpu.getCore().add(new Core());
        acpu.getCore().add(new Core());
        acpu.setCpuUsage( new CpuUsage(0.0) );
        for(Core core : acpu.getCore()) core.setCoreLoad(new CoreLoad(0.0) );
        
        Mainboard amainboard = new Mainboard(); 
        amainboard.getCPU().add( acpu );
        
        Server server = new Server();        
        server.getMainboard().add( amainboard );
        
        // Test VM with single CPU
        
        VirtualMachine vm = new VirtualMachine();
        vm.setNumberOfCPUs( new NrOfCpus(1) );
        
        // 1
        vm.setActualCPUUsage( new CpuUsage( 50. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(50.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(50. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

        // 2
        vm.setActualCPUUsage( new CpuUsage( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);        
        assertEquals(150.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals( 50. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());
        
         // 3
        vm.setActualCPUUsage( new CpuUsage( 75. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(225.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals( 25. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());
       
         // 4
        vm.setActualCPUUsage( new CpuUsage( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(325.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals( 25. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

         // 5
        vm.setActualCPUUsage( new CpuUsage( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        // assertEquals(100.0 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

       
        // Test VM with multiple CPU
        
	}
    
    
}


