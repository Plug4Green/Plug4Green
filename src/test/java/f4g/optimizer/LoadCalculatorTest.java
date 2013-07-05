package org.f4g.test;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.metamodel.CPUType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.TowerServerType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.CoreLoadType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.PSULoadType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.metamodel.NrOfCpusType;

import org.f4g.util.LoadCalculator;



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


