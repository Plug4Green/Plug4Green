package f4g.powerCalculator.power;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Map;

import f4g.commons.power.PoweredComponent;
import f4g.schemas.java.metamodel.CPUArchitectureType;
import f4g.schemas.java.metamodel.CPUType;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.CoreLoadType;
import f4g.schemas.java.metamodel.FrequencyType;
import f4g.schemas.java.metamodel.NrOfTransistorsType;
import f4g.schemas.java.metamodel.OperatingSystemTypeType;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;

// Repository Implmentation

public class PoweredCPU extends CPUType implements PoweredComponent{	
	
	static Logger log; 
	double totalCPUPower;			
	int i;		
	boolean coreLoadInformation;
	int numberOfCores;
	double cpuPower;
	double maximumFrequency; //Represents the maximum frequency with which any core of the same CPU is running

	JXPathContext context2;
	String coreQuery = null;
	CPUType myCPU;
	OperatingSystemTypeType operatingSystem;
	
	public PoweredCPU(CPUType cpu,OperatingSystemTypeType operatingSystem){
	
		this.transistorNumber = cpu.getTransistorNumber();
		/**
		 * Initialization of the variables
		 */
	
		totalCPUPower = 0.0;
		i=1;
		coreLoadInformation = false;
		numberOfCores =0;
		cpuPower = 0.0;
		maximumFrequency = 2.0;
		
		log = Logger.getLogger(PowerCalculator.class.getName());
		
		this.myCPU = cpu;
		this.operatingSystem = operatingSystem;
	}

	public int getNumOfCores(){
		
		JXPathContext ctxt = JXPathContext.newContext(this.myCPU);
	    coreQuery = "core";
	    Iterator coreIterator = ctxt.iterate(coreQuery);
		
		while(coreIterator.hasNext()){
			
			CoreType myCore = (CoreType)coreIterator.next();					  
				
			//Monitoring System provides information on the load
			if(myCore.getCoreLoad() != null && myCore.getCoreLoad().getValue()>0)
					coreLoadInformation = true;
			
			if(myCore.getFrequency().getValue()> maximumFrequency) maximumFrequency = myCore.getFrequency().getValue();
					  
			//Count the number of Cores
			numberOfCores++;
		}
		return this.numberOfCores;
	}
	

	@Override
	public double computePower() {
	
		this.numberOfCores = getNumOfCores();	// calculate the physical number of cores inside a CPU
		
		/**
		 * If CPU has no core objects
		 */
		if(numberOfCores == 0)
		{
			totalCPUPower = totalCPUPower + cpuPower;			
			log.error("CPU should consist of at least one core ");			
		}
		else
		{
			    /**
			      * Iterate over the cores in order to compute the power consumption 
			    */
			    context2 = JXPathContext.newContext(this.myCPU);
			    coreQuery = "core";
			    Iterator coreIterator = context2.iterate(coreQuery);
			    double corePower=0.0;
			    		    
			    // this loop iterates all cores within a processor
			    while (coreIterator.hasNext()){
			    	CoreType myCore = (CoreType)coreIterator.next();
			    	PoweredCore myPoweredCore = new PoweredCore(numberOfCores, myCore.getFrequency(), myCore.getFrequencyMin(), myCore.getFrequencyMax(), myCore.getVoltage(),myCPU.getArchitecture(),operatingSystem, myCore.getCoreLoad(), myCore.getTotalPstates(), transistorNumber, myCPU.isDVFS(), this.computeLoadedCore(myCPU));
			    	
			    	/**
			    	 * The code below compute the dynamic power consumption of cores.
			    	 */			    	
			    	 //  When the monitoring system cannot provide information about individual cores
			    	if(myCPU.getCpuUsage() != null){ 
			    		
			    		if (!coreLoadInformation){			    			
			    			//consider the cpu load as core load
			    			CoreLoadType clt = new CoreLoadType();
			    			clt.setValue(this.myCPU.getCpuUsage().getValue());
			    			myPoweredCore.setCoreLoad(clt);
			    		}
			    		cpuPower = cpuPower + myPoweredCore.computePower();
			    		
			    	}else{ 	
			    			cpuPower = cpuPower + myPoweredCore.computePower();
			    	}
			}//end of while loop
				    			
			totalCPUPower = totalCPUPower + cpuPower; 					
			//log.debug("The power consumption of the CPU is "+cpuPower + " Watts");
		
		}
		return totalCPUPower;	
	
	}
	
	/**
	 * Compute the number of active cores inside a CPU
	 * @param myCPU
	 * @return
	 */
	public int computeLoadedCore(CPUType myCPU){
		JXPathContext ctxt = JXPathContext.newContext(myCPU);
	    coreQuery = "core";
	    int c=0;
	    Iterator localIterator = ctxt.iterate(coreQuery);
	    
	    while (localIterator.hasNext()){
	    	CoreType myCore = (CoreType)localIterator.next();
	    	if (myCore.getCoreLoad()!= null && myCore.getCoreLoad().getValue() > 0.0) 
	    		c = c + 1;
	    }
	    return c;
	}//end of comuteLoadedCore method

}//end of class



