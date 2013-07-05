package org.f4g.util;

import org.apache.log4j.Logger;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.metamodel.CPUType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.HardDiskType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.TowerServerType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.CoreLoadType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;


/**
 * This class computes the increase of load on a server due to the VMs addition.
 * 
 */
public class LoadCalculator {
    
    private static Logger log = Logger.getLogger(LoadCalculator.class.getName());
    private static VMTypeType currentVMType;
	
    public LoadCalculator() { 
        currentVMType = null;
    }
    
    public LoadCalculator(VMTypeType vmType) { 
        currentVMType = vmType;
    }
    
    /**
     * get the server in idle state (suppress every loads)
     */
    public static ServerType getServerIdle(ServerType server, IPowerCalculator powerCalculator) {
        
    	ServerType myServer = (ServerType) server.clone();
    	
    	// zeroing server's measured power
    	if (myServer.getMeasuredPower() != null) {
    		myServer.setMeasuredPower( new PowerType(0.0) );
    	}
        
    	//zeroing the CPU loads
    	MainboardType mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
    		
    		for(CPUType cpu : mainboard.getCPU()) {
    			cpu.setCpuUsage(new CpuUsageType(0.0) );
    			
    			for(CoreType core : cpu.getCore()) {
    				core.setCoreLoad(new CoreLoadType(0.0) );
    			}    			
    		}    			
    	}
    	
    	//PB: zeroing hard disk read and write rate
    	if(mainboard != null) {
    		for(HardDiskType hardDisk : mainboard.getHardDisk()){
    			hardDisk.setReadRate(new IoRateType(0.0));
    			hardDisk.setWriteRate(new IoRateType(0.0));
    		}
    	}
    	
    	//PB: zeroing memory
    	if(mainboard != null) {
    		mainboard.setMemoryUsage(new MemoryUsageType(0.0));
    	}
    	
    	
    	if(mainboard != null) {  		
    		for(CPUType cpu : mainboard.getCPU()) {
    			cpu.setCpuUsage(new CpuUsageType(0.0) );
    			
    			for(CoreType core : cpu.getCore()) {
    				core.setCoreLoad(new CoreLoadType(0.0) );
    			}    			
    		}    			
    	}
    	    	
    	
    	//zeroing the PSU loads
    	if(myServer instanceof RackableServerType) {
    		RackableServerType myRackableServer = (RackableServerType) myServer;
    		
    		for(PSUType psu : myRackableServer.getPSU()) {
    			//psu.setLoad(new PSULoadType(0.0) );
    			psu.setMeasuredPower( new PowerType(0.0) );
    		}
    	}
    	
    	if(myServer instanceof TowerServerType) {
    		TowerServerType myTowerServer = (TowerServerType) myServer;
    		
    		for(PSUType psu : myTowerServer.getPSU()) {
    			//psu.setLoad(new PSULoadType(0.0) );
    			psu.setMeasuredPower( new PowerType(0.0) );
    		}
    	}
    	
    	//suppressing any VM.
    	if(myServer.getNativeHypervisor() != null)
    		myServer.getNativeHypervisor().getVirtualMachine().clear();
    	
    	if(myServer.getNativeOperatingSystem() != null)
    		myServer.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().clear();
    	
    	return myServer; 
    }
    
    
    /**
     * compute the power overhead induced by one VM on a server
     */
    public static ServerType addVMLoadOnServer(final ServerType server, final VMTypeType.VMType vm) {
        
    	ServerType myServer = (ServerType) server.clone();
    	
    	//setting the CPU loads
    	MainboardType mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
            
            // Find lowest loaded CPU
            
            CPUType cpu0 = new CPUType();
            cpu0.setCpuUsage( new CpuUsageType( 100 ) );
    		for(CPUType cpu : mainboard.getCPU()) 
                if( cpu.getCpuUsage().getValue() < cpu0.getCpuUsage().getValue() )
                    cpu0 = cpu;
            
            // check for the right CPU load normalization assumption
            
            double vmLoad  = vm.getExpectedLoad().getVCpuLoad().getValue();        // 0 -- 100
            double effLoad = vmLoad * vm.getCapacity().getVCpus().getValue();      // 0 -- 100 * vmCpus 
            
            for(CoreType core : cpu0.getCore()) {
                double availCap = 100 - core.getCoreLoad().getValue();
                if( availCap > vmLoad ) {
                    core.setCoreLoad( new CoreLoadType( core.getCoreLoad().getValue() + vmLoad ) );
                    break;
                }
                else {
                    vmLoad = vmLoad - availCap;
                    core.setCoreLoad( new CoreLoadType( 100.0 ) );
                }
            }
            
            double cpuLoad = 0.0;
            for(CoreType core : cpu0.getCore()) 
                    cpuLoad += core.getCoreLoad().getValue();
            cpu0.setCpuUsage( new CpuUsageType( cpuLoad / cpu0.getCore().size() ) ); 
        }
        return myServer;
    }
    
    
    
    /**
     * compute the power overhead induced by one VM on a server
     */
    public static ServerType addVMLoadOnServer(final ServerType server, final VirtualMachineType vm) {
        
    	ServerType myServer = (ServerType) server.clone();
    	
    	//setting the CPU loads
    	MainboardType mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
            
            // Find lowest loaded CPU
            
            CPUType cpu0 = new CPUType();
            cpu0.setCpuUsage( new CpuUsageType( 100 ) );
    		for(CPUType cpu : mainboard.getCPU()) 
                if( cpu.getCpuUsage().getValue() < cpu0.getCpuUsage().getValue() )
                    cpu0 = cpu;
            
            // check for the right CPU load normalization assumption
            
           double vmLoad = 0.;                                              // 0 -- 100
            
            if(vm.getActualCPUUsage() != null) {
                vmLoad = vm.getActualCPUUsage().getValue();
            } else {
                VMTypeType.VMType SLA_VM = null;
                
                if(vm.getCloudVmType() != null) {
                    SLA_VM = Util.findVMByName(vm.getCloudVmType(), currentVMType);	
                    vmLoad = SLA_VM.getExpectedLoad().getVCpuLoad().getValue();
                }
                
            }

            double effLoad = vmLoad * vm.getNumberOfCPUs().getValue();      // 0 -- 100 * vmCpus 
            
            for(CoreType core : cpu0.getCore()) {
                double availCap = 100.0 - core.getCoreLoad().getValue();
                if( availCap > vmLoad ) {
                    core.setCoreLoad( new CoreLoadType( core.getCoreLoad().getValue() + vmLoad ) );
                    break;
                }
                else {
                    vmLoad = vmLoad - availCap;
                    core.setCoreLoad( new CoreLoadType( 100.0 ) );
                }
            }
            
            double cpuLoad = 0.0;
            for(CoreType core : cpu0.getCore()) 
                    cpuLoad += core.getCoreLoad().getValue();
            cpu0.setCpuUsage( new CpuUsageType( cpuLoad / cpu0.getCore().size() ) ); 
            
           /*
             //increment the memory usage on the mainboard
             if(mainboard.getMemoryUsage() != null)
             mainboard.setMemoryUsage( new MemoryUsageType(vm.getActualMemoryUsage().getValue()) );      // we only add the VM's usage given that we want 
             */
            
        }
        return myServer;
    }
    
    
}