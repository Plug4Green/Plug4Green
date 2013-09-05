package f4g.powerCalculator.power;
import f4g.powerCalculator.power.PoweredComponent;
import f4g.schemas.java.metamodel.HardDiskType;

import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.PowerType;
import org.apache.log4j.Logger;

public class PoweredHardDiskDrive extends HardDiskType implements PoweredComponent{	
	
	static Logger log = Logger.getLogger(PoweredHardDiskDrive.class.getName());
	private double operationProbability = 0.0; // This is used to distribute the probability of accessing the disk, being in idle or in startup modes. It is given by: x.P +0.9(1-x).P +0.1(1-x).P
	private final static double idlePower = 0.0;
	private boolean lunFlag;
	private double powerHardDiskDrive=0.0;
	
	public PoweredHardDiskDrive(IoRateType readRate, IoRateType maxReadRate, IoRateType writeRate, IoRateType maxWriteRate, PowerType powerIdle){		
		
		this.readRate = readRate;
		this.maxReadRate = maxReadRate;		
		this.writeRate =writeRate;		
		this.maxWriteRate =maxWriteRate;
		this.powerIdle = powerIdle;
		lunFlag = false;
	}
	
	public void setLUNFlag(boolean f){ lunFlag = f; }
	
	@Override
	public double computePower() {
		
		if (this.readRate == null || this.readRate.getValue() < 0 ||this.writeRate == null || this.writeRate.getValue() < 0){
			log.debug("Read and/or Write rate is NULL or Negative");
			return 0.0;
		}
		else if (this.maxReadRate == null || this.maxReadRate.getValue() <= 0.0 ||this.maxWriteRate == null || this.maxWriteRate.getValue() <= 0){
			log.debug("Maximum Read and/or Write rate is NULL, zero or Negative");
			return 0.0;
		}else if (this.powerIdle == null || this.powerIdle.getValue() <= 0.0){
			log.debug("Power idle of hard drives is Null or Negative or Zero");
			return 0.0;			
		}else{
			if(this.readRate.getValue()==0 && this.writeRate.getValue() == 0)
				operationProbability =0.0;	//The disk is in idle state. No probability to access the disk
			else if(this.readRate.getValue()==0)
				operationProbability = (this.writeRate.getValue())/(this.maxWriteRate.getValue()); //Compute the probability based on read and write rates	
			else if(this.writeRate.getValue() == 0)
				operationProbability = (this.readRate.getValue())/(this.maxReadRate.getValue()); //Compute the probability based on read and write rates	
			else
				operationProbability = (this.readRate.getValue()+this.writeRate.getValue())/(this.maxReadRate.getValue()+this.maxWriteRate.getValue()); //Compute the probability based on read and write rates	
		
			/**
			 * The power consumption of the hard disk is divided into three parts: R/W, Idle and Startup
			 * The R/W and startup power consumptions are computed based on the idle power given by the manufacturer		 * 
			 * (powerIdle+0.4*powerIdle) represents the R/W power whereas (powerIdle*3.7) denotes the startup power
			 */
			
			//Special case: if there are neither read nor write operations, then we consider that the Harddisk is always in the idle mode
			if(this.readRate.getValue() == 0 && this.writeRate.getValue() == 0){
				if (lunFlag) // This flag is used with NAS and SAN devices since they  barely go to sleep and standby states. Hence, no need to compute them. Just take idle power provided by the manufacturer.
					powerHardDiskDrive = powerIdle.getValue();
				else
					powerHardDiskDrive =computePowerIdle(1-operationProbability,powerIdle.getValue());	
			}else{
				if (lunFlag) // This flag is used with NAS and SAN devices since they  barely go to sleep and standby states. Hence, no need to compute them.Just take idle power provided by the manufacturer. Also, no need to compute the startup power.
					powerHardDiskDrive = operationProbability*(powerIdle.getValue()+0.4*powerIdle.getValue())+ (1-operationProbability)*(powerIdle.getValue());
				else				
					powerHardDiskDrive = operationProbability*(powerIdle.getValue()+0.4*powerIdle.getValue())+computePowerIdle((1-operationProbability)*0.9,powerIdle.getValue())+0.1*(1-operationProbability)*(powerIdle.getValue()*3.7);
			}
		     
			return powerHardDiskDrive;
		}//end of else
		
	}//end of computePower method

	/**
	 * A function to compute the total idle power of the hard disk based on the idle power provided by the manufacturer
	 * The total idle power includes the probability that the hard disk is idle and the probability it remains in sleep and standby modes
	 * @param probability
	 * @param powerIdle
	 * @return
	 */
	public double computePowerIdle(double probability, double powerIdle){
		//left part for idle mode and right part to go into
		// standby and sleep mode
		if (probability > 0 && probability <= 0.3) return (0.9*powerIdle + 0.1*0.1*powerIdle);	// 0.1= 0.2/2
		if (probability > 0.3 && probability <= 0.6) return (0.5*powerIdle + 0.5*0.1*powerIdle);// 0.1= 0.2/2
		if (probability > 0.6 && probability <= 1.0) return (0.1*powerIdle + 0.9*0.1*powerIdle);// 0.1= 0.2/2
		else return 0.0;
		
	}
}
