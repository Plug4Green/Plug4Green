package f4g.optimizer;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.metamodel.CPUType;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.MainboardType;
import f4g.schemas.java.metamodel.PSUType;
import f4g.schemas.java.metamodel.RackableServerType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.TowerServerType;
import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.CoreLoadType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.PSULoadType;
import f4g.schemas.java.metamodel.MemoryUsageType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import f4g.schemas.java.metamodel.NrOfCpusType;

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
        
        
        CPUType acpu = new CPUType();  
        acpu.getCore().add(new CoreType());
        acpu.getCore().add(new CoreType());
        acpu.getCore().add(new CoreType());
        acpu.getCore().add(new CoreType());
        acpu.setCpuUsage( new CpuUsageType(0.0) );
        for(CoreType core : acpu.getCore()) core.setCoreLoad(new CoreLoadType(0.0) );
        
        MainboardType amainboard = new MainboardType(); 
        amainboard.getCPU().add( acpu );
        
        ServerType server = new ServerType();        
        server.getMainboard().add( amainboard );
        
        // Test VM with single CPU
        
        VirtualMachineType vm = new VirtualMachineType();
        vm.setNumberOfCPUs( new NrOfCpusType(1) );
        
        // 1
        vm.setActualCPUUsage( new CpuUsageType( 50. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(50.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(50. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals( 0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

        // 2
        vm.setActualCPUUsage( new CpuUsageType( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);        
        assertEquals(150.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals( 50. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());
        
         // 3
        vm.setActualCPUUsage( new CpuUsageType( 75. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(225.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals( 25. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(  0. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());
       
         // 4
        vm.setActualCPUUsage( new CpuUsageType( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        assertEquals(325.0/4 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals( 25. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

         // 5
        vm.setActualCPUUsage( new CpuUsageType( 100. ) );
        server = loadCalculator.addVMLoadOnServer(server, vm);
        // assertEquals(100.0 ,  server.getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(2).getCoreLoad().getValue());
        assertEquals(100. ,  server.getMainboard().get(0).getCPU().get(0).getCore().get(3).getCoreLoad().getValue());

       
        // Test VM with multiple CPU
        
	}
    
    
}


