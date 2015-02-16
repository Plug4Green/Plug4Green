package f4g.commons.util;

import org.apache.log4j.Logger;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.CPU;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.metamodel.HardDisk;
import f4g.schemas.java.metamodel.IoRate;
import f4g.schemas.java.metamodel.Mainboard;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.PSU;
import f4g.schemas.java.metamodel.RackableServer;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.TowerServer;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.CoreLoad;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;


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
    
    public LoadCalculator(VMTypeType vm) { 
        currentVMType = vm;
    }
    
    /**
     * get the server in idle state (suppress every loads)
     */
    public static Server getServerIdle(Server server, IPowerCalculator powerCalculator) {
        
    	Server myServer = (Server) server.clone();
    	
    	// zeroing server's measured power
    	if (myServer.getMeasuredPower() != null) {
    		myServer.setMeasuredPower( new Power(0.0) );
    	}
        
    	//zeroing the CPU loads
    	Mainboard mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
    		
    		for(CPU cpu : mainboard.getCPU()) {
    			cpu.setCpuUsage(new CpuUsage(0.0) );
    			
    			for(Core core : cpu.getCore()) {
    				core.setCoreLoad(new CoreLoad(0.0) );
    			}    			
    		}    			
    	}
    	
    	//PB: zeroing hard disk read and write rate
    	if(mainboard != null) {
    		for(HardDisk hardDisk : mainboard.getHardDisk()){
    			hardDisk.setReadRate(new IoRate(0.0));
    			hardDisk.setWriteRate(new IoRate(0.0));
    		}
    	}
    	
    	//PB: zeroing memory
    	if(mainboard != null) {
    		mainboard.setMemoryUsage(new MemoryUsage(0.0));
    	}
    	
    	
    	if(mainboard != null) {  		
    		for(CPU cpu : mainboard.getCPU()) {
    			cpu.setCpuUsage(new CpuUsage(0.0) );
    			
    			for(Core core : cpu.getCore()) {
    				core.setCoreLoad(new CoreLoad(0.0) );
    			}    			
    		}    			
    	}
    	    	
    	
    	//zeroing the PSU loads
    	if(myServer instanceof RackableServer) {
    		RackableServer myRackableServer = (RackableServer) myServer;
    		
    		for(PSU psu : myRackableServer.getPSU()) {
    			//psu.setLoad(new PSULoad(0.0) );
    			psu.setMeasuredPower( new Power(0.0) );
    		}
    	}
    	
    	if(myServer instanceof TowerServer) {
    		TowerServer myTowerServer = (TowerServer) myServer;
    		
    		for(PSU psu : myTowerServer.getPSU()) {
    			//psu.setLoad(new PSULoad(0.0) );
    			psu.setMeasuredPower( new Power(0.0) );
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
    public static Server addVMLoadOnServer(final Server server, final VMTypeType.VMType vm) {
        
    	Server myServer = (Server) server.clone();
    	
    	//setting the CPU loads
    	Mainboard mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
            
            // Find lowest loaded CPU
            
            CPU cpu0 = new CPU();
            cpu0.setCpuUsage( new CpuUsage( 100 ) );
    		for(CPU cpu : mainboard.getCPU()) 
                if( cpu.getCpuUsage().getValue() < cpu0.getCpuUsage().getValue() )
                    cpu0 = cpu;
            
            // check for the right CPU load normalization assumption
            
            double vmLoad  = vm.getExpectedLoad().getVCpuLoad().getValue();        // 0 -- 100
            double effLoad = vmLoad * vm.getCapacity().getVCpus().getValue();      // 0 -- 100 * vmCpus 
            
            for(Core core : cpu0.getCore()) {
                double availCap = 100 - core.getCoreLoad().getValue();
                if( availCap > vmLoad ) {
                    core.setCoreLoad( new CoreLoad( core.getCoreLoad().getValue() + vmLoad ) );
                    break;
                }
                else {
                    vmLoad = vmLoad - availCap;
                    core.setCoreLoad( new CoreLoad( 100.0 ) );
                }
            }
            
            double cpuLoad = 0.0;
            for(Core core : cpu0.getCore()) 
                    cpuLoad += core.getCoreLoad().getValue();
            cpu0.setCpuUsage( new CpuUsage( cpuLoad / cpu0.getCore().size() ) ); 
        }
        return myServer;
    }
    
    
    
    /**
     * compute the power overhead induced by one VM on a server
     */
    public static Server addVMLoadOnServer(final Server server, final VirtualMachine vm) {
        
    	Server myServer = (Server) server.clone();
    	
    	//setting the CPU loads
    	Mainboard mainboard = myServer.getMainboard().get(0);
    	if(mainboard != null) {
            
            // Find lowest loaded CPU
            
            CPU cpu0 = new CPU();
            cpu0.setCpuUsage( new CpuUsage( 100 ) );
    		for(CPU cpu : mainboard.getCPU()) 
                if( cpu.getCpuUsage().getValue() < cpu0.getCpuUsage().getValue() )
                    cpu0 = cpu;
            
            // check for the right CPU load normalization assumption
            
           double vmLoad = 0.;                                              // 0 -- 100
            
            if(vm.getActualCPUUsage() != null) {
                vmLoad = vm.getActualCPUUsage().getValue();
            } else {
                VMTypeType.VMType SLA_VM = null;
                
                if(vm.getCloudVm() != null) {
                    SLA_VM = Util.findVMByName(vm.getCloudVm(), currentVMType);	
                    vmLoad = SLA_VM.getExpectedLoad().getVCpuLoad().getValue();
                }
                
            }

            double effLoad = vmLoad * vm.getNumberOfCPUs().getValue();      // 0 -- 100 * vmCpus 
            
            for(Core core : cpu0.getCore()) {
                double availCap = 100.0 - core.getCoreLoad().getValue();
                if( availCap > vmLoad ) {
                    core.setCoreLoad( new CoreLoad( core.getCoreLoad().getValue() + vmLoad ) );
                    break;
                }
                else {
                    vmLoad = vmLoad - availCap;
                    core.setCoreLoad( new CoreLoad( 100.0 ) );
                }
            }
            
            double cpuLoad = 0.0;
            for(Core core : cpu0.getCore()) 
                    cpuLoad += core.getCoreLoad().getValue();
            cpu0.setCpuUsage( new CpuUsage( cpuLoad / cpu0.getCore().size() ) ); 
            
           /*
             //increment the memory usage on the mainboard
             if(mainboard.getMemoryUsage() != null)
             mainboard.setMemoryUsage( new MemoryUsage(vm.getActualMemoryUsage().getValue()) );      // we only add the VM's usage given that we want 
             */
            
        }
        return myServer;
    }
    
    
}
