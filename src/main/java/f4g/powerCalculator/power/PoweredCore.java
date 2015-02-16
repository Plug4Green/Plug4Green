package f4g.powerCalculator.power;

import f4g.powerCalculator.power.PoweredComponent;
import f4g.schemas.java.metamodel.Frequency;
import f4g.schemas.java.metamodel.Voltage;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.metamodel.CPUArchitecture;
import f4g.schemas.java.metamodel.OperatingSystemType;
import f4g.schemas.java.metamodel.CoreLoad;
import f4g.schemas.java.metamodel.NrOfTransistors;
import f4g.schemas.java.metamodel.NrOfPstates;
import org.apache.log4j.Logger;

// Repository implementation
public class PoweredCore extends Core implements PoweredComponent{	
	
	CPUArchitecture CPUBrand;
	int numberOfCPUCores =0;
	private double numOfTrans=0.0;
	OperatingSystemType operatingSystem;
	double pFreq = 0.0;
	boolean dvfs;
	int loadedCoreCount; 
	
	static Logger log = Logger.getLogger(PoweredCore.class.getName());
	
	public PoweredCore(int numberOfCores, Frequency frequency, Frequency frequencyMin, Frequency frequencyMax, Voltage voltage,CPUArchitecture brand,OperatingSystemType operatingSystem, CoreLoad load, NrOfPstates totalPstates, NrOfTransistors transistorNumber, boolean dvfs, int loadedCores){
        this.totalPstates = totalPstates;
		this.numOfTrans = transistorNumber.getValue()/numberOfCores;
		this.frequencyMin = frequencyMin;
		this.frequencyMax = frequencyMax;
		this.coreLoad = load;		
		this.voltage = voltage;		
		this.frequency = frequency;		
		this.CPUBrand = brand;
		this.numberOfCPUCores=numberOfCores;
		this.operatingSystem = operatingSystem;
		this.dvfs = dvfs;
		this.loadedCoreCount = loadedCores;
	}
	
	private double selectPState(){
		
		double f=0.0;
		
		if (frequencyMin == null || frequencyMax == null || frequencyMax.getValue() <= 0.0 || frequencyMin.getValue() <= 0.0){
			
			if (this.frequency== null || this.frequency.getValue() <= 0.0){
				log.debug("The frequency of a CPU is NULL or Zero"); 
				return 0.0;
			}else
				f = this.frequency.getValue();
		}else{			
			if (this.getCoreLoad()!= null && this.getCoreLoad().getValue() >= 80.0) f = frequencyMax.getValue();
			else{
				//divide number of p-states uniformly
				double freqJump = (frequencyMax.getValue() - frequencyMin.getValue())/(totalPstates.getValue()-1);
				double loadJump = 80/(totalPstates.getValue()-1);
				if (this.getCoreLoad().getValue() <= loadJump ) f = frequencyMin.getValue();
				else f = frequencyMin.getValue() + ((this.getCoreLoad().getValue()/loadJump)* freqJump);
			}
		}
		if (f < 2.0 ) return 2.0; else return f;
	} //end of selectPState method

	@Override
	/**	 
	 * Computes the current power consumption of the cores based on the technology (number of cores) and the Operating System 
	 * 
	 * @param 
	 * @return double value containing information on power consumption of the cores
	 */
	public double computePower() {
		
		double corePower = 0.0, coreIdlePower = 0.0;
		
		if (this.voltage == null || this.voltage.getValue() <= 0.0){
			log.debug("Core Voltage is NULL or Negative or Zero");
			return 0.0;	
			
		}else if (this.numberOfCPUCores < 0){
    		log.debug("Number of CPU Cores is Negative");
    		return 0.0;
    		
    	}
		
		coreIdlePower = computeIdlePower();
	
		if (this.coreLoad == null || this.coreLoad.getValue() <= 0.0){
			log.debug("Core Load is NULL or Negative or Zero. Only idle power is computed.");
			return coreIdlePower;
    	}
		
		if (this.coreLoad.getValue() > 80.0 )			
			corePower = computeDynamicPower() * 80.0 * 0.01 + ((3.0/this.numberOfCPUCores)*((this.coreLoad.getValue()-80.0)*0.05));
		else
			corePower = this.coreLoad.getValue() * 0.01 * computeDynamicPower();

		if(this.dvfs && corePower > 0.0){
			corePower = corePower - (calEnergRedFact(this.numberOfCPUCores, this.frequency.getValue(), loadedCoreCount) * corePower);
			if (this.CPUBrand == CPUArchitecture.AMD) corePower = corePower * 1.02;
   		 }
		return (corePower+coreIdlePower);
	}//end of computePower method
	
	/**
	 * This function computes the idle power consumption of a core
	 * {To be completed; use html notation if necessary}
	 * 
	 * @return
	 *
	 * @author nasirali
	 */
	public double computeIdlePower(){	
		double p = 0.0;
		
	
		if (this.numOfTrans <= 0.0){
			log.debug("Number of Transistors is Negative or Zero");
			return 0.0;
		}			
			// P is the power of a processor
			p = (0.114312*(this.voltage.getValue() * this.voltage.getValue()) + (-0.22835 * this.voltage.getValue()) + 0.139204)* this.voltage.getValue() * numOfTrans;

		if(this.dvfs){
		    if (this.CPUBrand != null && this.CPUBrand == CPUArchitecture.AMD){
	
		    	double minFreq = 0.0;
		    	double f=0.0;
		    	double maxFreq = 0.0;
		    	
		    	minFreq = (this.frequencyMin == null || this.frequencyMin.getValue() <= 0.0) ? 2.0 : this.frequencyMin.getValue();
	
		    	if (this.frequencyMax == null || this.frequencyMax.getValue() < 0.0){
		    	
		    		if (this.frequency == null || this.frequency.getValue() <= 0.0){
		    			log.debug("Frequency is null or zero or negtaive");
		    			return 0.0;
		    		} else if (this.frequency.getValue() < minFreq){
		    			log.debug("Minimum Frequency is greater than the current frequency");
		    			return 0.0;	    			
		    		}
		    		maxFreq = this.frequency.getValue();
		    	}
		    	maxFreq = (this.frequencyMax == null || this.frequencyMax.getValue() < 0.0) ? this.frequency.getValue() : this.frequencyMax.getValue();
		    	
		    	double fRatio = this.frequency.getValue()/maxFreq;
		    		
		    	if (fRatio <= 0.25) f = minFreq;
		    	else if (fRatio > 0.25 && fRatio < 0.6563 ) f = maxFreq*0.625;
		    	else if (fRatio > 0.6563 && fRatio < 0.7813 ) f = maxFreq*0.6875;
		    	else	f = maxFreq;
	
		    	double rho11 = -3.01181, rho12= 18.7443, rho13= -35.0267, rho14=18.0333;
		    	double rho21 = 6.86864, rho22= -42.6639, rho23= 79.8181, rho24 = -41.6034;
		    	double rho31 = -4.20735, rho32= 26.0495, rho33= -48.4299, rho34=25.8339;
		    		
		    	double beta1 = betaCalculate(rho11, rho12, rho13, rho14, f);
		    	double beta2 = betaCalculate(rho21, rho22, rho23, rho24, f); 
		    	double beta3 = betaCalculate(rho31, rho32, rho33, rho34, f);
		    		
		    	double delta = beta1*Math.pow(this.voltage.getValue(), 2) + beta2*this.voltage.getValue() + beta3;
		    	
		    	if (this.numberOfCPUCores == 2) delta = 1.8*delta;
				
				//The reduction factor should be between 0 and 1
				if(delta>1)delta=1;
	    		
		    	return delta*p;
		    
		    }else{ 
					if (this.CPUBrand != null && this.CPUBrand == CPUArchitecture.INTEL && numberOfCPUCores == 2) return 0.942*p;
					else if (this.CPUBrand != null && this.CPUBrand == CPUArchitecture.INTEL && numberOfCPUCores == 4) return 0.728*p;
					 else 
						 	 return 0.316*p;
		    }
		}
		return p;
	}//end of computePowerIdle method

	/**
	 * This function computes the dynamic power consumption of a core
	 * {To be completed; use html notation if necessary}
	 * 
	 * @return
	 *
	 * @author nasirali
	 */

	public double computeDynamicPower(){
		double corePower=0;
		double tempFreq = 0.0;
		
		if (this.totalPstates != null && this.totalPstates.getValue() > 1)
			tempFreq = selectPState();
		else{
			if (this.frequency == null || this.frequency.getValue() <= 0.0){
				log.debug("Frequency is Null or negative or zero.");
				return 0.0;
			}
			if (this.frequency.getValue() > 0.0 && this.frequency.getValue() < 2.0 ) tempFreq = 2.0;
			else tempFreq = this.frequency.getValue();
		}
		
		//For AMD Quad core Processors
		if (this.CPUBrand != null && this.CPUBrand == CPUArchitecture.AMD && this.numberOfCPUCores == 4)
			corePower = (this.voltage.getValue()*3.052+1.5*(tempFreq-2.0))*tempFreq*this.voltage.getValue()*this.voltage.getValue();
		
		else corePower = computeCapacitance(tempFreq, this.voltage.getValue(),this.numberOfCPUCores)*tempFreq*this.voltage.getValue()*this.voltage.getValue();
		
		//For Windows operating system on Intel CPUs
		if( this.CPUBrand != null && this.CPUBrand == CPUArchitecture.INTEL && this.operatingSystem != null && this.operatingSystem == OperatingSystemType.WINDOWS)
			corePower = corePower*0.95;
		else if(this.CPUBrand != null && this.CPUBrand == CPUArchitecture.AMD && this.operatingSystem != null && this.operatingSystem == OperatingSystemType.WINDOWS)
			corePower = corePower*0.93;
		
		return corePower;
	}//end of computeDynamicPower method
	
	
	/**	 
	 * Computes the capacitance of the power consumption function for cores
	 * 
	 * @param frequency, voltage and numberOfCores
	 * @return double value containing information on power consumption of the capacitance of the cores
	 */
	private double computeCapacitance(double frequency, double voltage, int numberOfCores){
		
	double 	factor1 = voltage*2.43; // Indicates the reference value for 2.0 GHz CPUs
	double factor2 = 0.0; // Indicates the rate of change in capacitance for processors with frequency higher than 2.0 GHz
	
	if (numberOfCores == 1 )factor2 = 2.16;
	else
	{
	
		if(voltage == 1.325)factor2 = 1.5;
		else if(voltage == 1.1)factor2 = 7.05;
		else
		factor2 = 7.05;
	}
	
	return factor1+factor2*(frequency -2.0);
		
   }//end of computeCapacitance method
	

	 /**
	    * This method is a polynomial for AMD processor in order to determine the idle power consumption of it.
	    * The frequency and voltage ranges are limited as specified in the deliverable.
	    * @param x_a
	    * @param y_a
	    * @param x_b
	    * @param y_b
	    * @param x
	    * @return
	    */
	   private double betaCalculate(double r1, double r2, double r3, double r4, double f){
	    	return r1*Math.pow(f, 3) + r2*Math.pow(f, 2)+r3 *f + r4;
	    }

	   /**
	     * This function is specific for Intel machines because
	     * the power consumption of n cores is less
	     * than the sum of power consumptions of these n cores.
	     * This method calculates the error factor, which  then
	     * will be subtracted from the summation to obtain the actual
	     * power consumption.
	     */
	    private double calEnergRedFact(int numberOfCores, double frequency, int loadedCoreCount){
	    	
//	    	System.out.println("erf .."+numberOfCores+"Freq..."+frequency+"Loade..."+loadedCoreCount);
	    	
	    	double alpha = 0.0;		// base value for 2,0 frequency
	    	double beta = 0.0;		// reflect the error due to change in frequency
	    	double gamma = 0.0;		//shows the rate of change with number of cores.
	    	
	    	if (numberOfCores == 2) return 0.01;	//dual core
	    	else{
	    		alpha = 0.04;			
	    		beta = 0.42;			
	    		gamma = 0.015;			
	    	}//end if 
	    	return alpha + beta *(frequency-2.0) + gamma * (loadedCoreCount-2);
	    }//end of callEngRedFact method
}//end of class
