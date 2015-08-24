package f4g.commons.util;

import org.apache.log4j.Logger;

import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;



//computes power idle and power per VM.
public class StaticPowerCalculation {

   private static Logger log = Logger.getLogger(StaticPowerCalculation.class.getName());
   private VMFlavorType currentVMFlavor;
   
   public StaticPowerCalculation(VMFlavorType vm) { 
       currentVMFlavor = vm;
   }
   
   
    
    //compute the idle power of a server
    public double computePowerIdle(Server server, IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
    	
    	Server serverIdle = (new LoadCalculator()).getServerIdle(server, powerCalculator);
    	double powerIdle = powerCalculator.computePowerServer(serverIdle).getActualConsumption();
    	
    	log.debug("computePowerIdle for server " + server.getFrameworkID() + " = " + powerIdle);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return powerIdle;
    }
    
    
    //compute the power overhead induced by one VM on a server
    public double computePowerForVM(Server server, VirtualMachine vm, IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
               
        LoadCalculator loadCalculator = new LoadCalculator(currentVMFlavor);   	

    	Server serverIdle   = loadCalculator.getServerIdle(server, powerCalculator);
    	Server serverWithVM = loadCalculator.addVMLoadOnServer(serverIdle, vm);
    	    	
    	double idlePower    = computePowerIdle(serverIdle, powerCalculator);
    	double chargedPower = powerCalculator.computePowerServer(serverWithVM).getActualConsumption();
    	log.debug("chargedPower for server " + server.getFrameworkID() + " and VM " + vm.getName() + " = " + chargedPower);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return chargedPower - idlePower;
    }
    
    //compute the power overhead induced by one VM on a server
    public double computePowerForVM(Server server, VMFlavorType.VMFlavor vm, 
    		IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
               
        LoadCalculator loadCalculator = new LoadCalculator(currentVMFlavor);   	

    	Server serverIdle   = loadCalculator.getServerIdle(server, powerCalculator);
    	Server serverWithVM = loadCalculator.addVMLoadOnServer(serverIdle, vm);
    	    	
    	double idlePower    = computePowerIdle(serverIdle, powerCalculator);
    	double chargedPower = powerCalculator.computePowerServer(serverWithVM).getActualConsumption();
    	log.debug("chargedPower for server " + server.getFrameworkID() + " and VM " + vm.getName() + " = " + chargedPower);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return chargedPower - idlePower;
    }

}
