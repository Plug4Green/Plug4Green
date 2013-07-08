package f4g.powerCalculator.power;
import org.apache.log4j.Logger;

import f4g.commons.power.PoweredComponent;
import f4g.schemas.java.metamodel.FanType;
import f4g.schemas.java.metamodel.RPMType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.DimensionType;


// Repository Implementation

public class PoweredFan extends FanType implements PoweredComponent{	
	
	static Logger log = Logger.getLogger(PoweredFan.class.getName()); 
	private boolean simulationFlag;
	
	public PoweredFan(RPMType actualRPM, DimensionType depth, RPMType maxRPM, PowerType maxPower, PowerType measuredPower, boolean flag){
		this.maxRPM = maxRPM;
		this.actualRPM = actualRPM;		
		this.depth = depth;
		this.powerMax = maxPower;
		this.measuredPower = measuredPower;
		this.simulationFlag = flag;
	}

	/**
	 * this method returns the constant factor, which will be used to compute the power
	 * consumption of fan for any RPM.
	 * @param maxPower
	 * @param maxRPM
	 * @param depth
	 * @return
	 */
	private double computeConstant(double maxPow, int maxRPM, double depth){
		return (maxPow*3600)/(maxRPM*maxRPM*depth);
	}//end of computeConstant function
	
	@Override
	public double computePower() {

		if (this.measuredPower != null && this.measuredPower.getValue() >= 0.0) return this.measuredPower.getValue();
		else{
			if (this.maxRPM == null || this.maxRPM.getValue() <= 0){
				log.debug("Max RPM cannot be NULL or Zero or Negative");
				return 0.0;
			}else if (this.actualRPM == null || this.actualRPM.getValue() < 0.0){
				log.debug("Actual RPM cannot be NULL or Negative");
				return 0.0;
			}else if (this.depth == null || this.depth.getValue() <= 0.0){ 
				log.debug("Width or/and Depth cannot NULL or Zero");
				return 0.0;
			}else if (this.actualRPM.getValue()  > this.maxRPM.getValue()){
				log.debug("Actual RPM cannot be greater than maxRPM");
				return 0.0;				
			}else if (this.powerMax == null || this.powerMax.getValue() <=0.0 ){
				log.debug("Power Max is NULL or Zero.");
				return 0.0;
			}else{
				return (computeConstant(powerMax.getValue(), maxRPM.getValue(), (depth.getValue()*0.001))* (actualRPM.getValue()*actualRPM.getValue()*(depth.getValue()*0.001)))/3600;
			}
		}
	}//end of computePower method
	
}//end of class
