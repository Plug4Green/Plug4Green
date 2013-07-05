package org.f4g.power;

import org.f4g.schema.metamodel.PSUType;
import org.apache.log4j.Logger;

public class PoweredPSU extends PSUType implements PoweredComponent{

	double measuredPower=0.0;
	double efficiency=0.0;
	static Logger log = Logger.getLogger(PoweredPSU.class.getName());
	
	public PoweredPSU(double measuredPower, double efficiency){
		this.measuredPower = measuredPower;
		this.efficiency = efficiency;
	}//end of constructor
	
	/**	 
	 * Computes the current power consumption of the power supply units
	 * The efficiency of the PSU should be given in percentage form with respect to the corresponding load
	 * @param obj:input object of type SAN, NetworkNode, Enclosure, RackableServer and TowerServer 
	 * @return double value containing information on power consumption of the power supply units
	 */	
	public double computePower(){		
			
			double PSUPower =0.0;
			
			if (this.efficiency < 0 || this.measuredPower < 0){
				log.debug("Efficiency or MeasurePower is negative.");
				return 0.0;
			}
			
			PSUPower=(this.measuredPower*(100-this.efficiency))/100;							
			return PSUPower;			
	}//end of computePower method
}//end of class
