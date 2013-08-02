package f4g.commons.util;

import org.apache.log4j.Logger;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VirtualMachineType;



//computes power idle and power per VM.
public class StaticPowerCalculation {

   private static Logger log = Logger.getLogger(StaticPowerCalculation.class.getName());
   private VMTypeType currentVMType;
   
   public StaticPowerCalculation(VMTypeType vmType) { 
       currentVMType = vmType;
   }
   
   
    
    //compute the idle power of a server
    public double computePowerIdle(ServerType server, IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
    	
    	ServerType serverIdle = (new LoadCalculator()).getServerIdle(server, powerCalculator);
    	double powerIdle = powerCalculator.computePowerServer(serverIdle).getActualConsumption();
    	
    	log.debug("computePowerIdle for server " + server.getFrameworkID() + " = " + powerIdle);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return powerIdle;
    }
    
    
    //compute the power overhead induced by one VM on a server
    // public static double computePowerForVM(ServerType server, VMTypeType.VMType vm, IPowerCalculator powerCalculator) 
    public double computePowerForVM(ServerType server, VirtualMachineType vm, 
    		IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
               
        LoadCalculator loadCalculator = new LoadCalculator(currentVMType);   	

    	ServerType serverIdle   = loadCalculator.getServerIdle(server, powerCalculator);
    	ServerType serverWithVM = loadCalculator.addVMLoadOnServer(serverIdle, vm);
    	    	
    	double idlePower    = computePowerIdle(serverIdle, powerCalculator);
    	double chargedPower = powerCalculator.computePowerServer(serverWithVM).getActualConsumption();
    	log.debug("chargedPower for server " + server.getFrameworkID() + " and VM " + vm.getName() + " = " + chargedPower);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return chargedPower - idlePower;
    }
    
    //compute the power overhead induced by one VM on a server
    public double computePowerForVM(ServerType server, VMTypeType.VMType vm, 
    		IPowerCalculator powerCalculator) {
    	
    	//boolean previousFlag = powerCalculator.getSimulationFlag();
    	//powerCalculator.setSimulationFlag(true);
               
        LoadCalculator loadCalculator = new LoadCalculator(currentVMType);   	

    	ServerType serverIdle   = loadCalculator.getServerIdle(server, powerCalculator);
    	ServerType serverWithVM = loadCalculator.addVMLoadOnServer(serverIdle, vm);
    	    	
    	double idlePower    = computePowerIdle(serverIdle, powerCalculator);
    	double chargedPower = powerCalculator.computePowerServer(serverWithVM).getActualConsumption();
    	log.debug("chargedPower for server " + server.getFrameworkID() + " and VM " + vm.getName() + " = " + chargedPower);
    	
    	//powerCalculator.setSimulationFlag(previousFlag);
    	
    	return chargedPower - idlePower;
    }

}