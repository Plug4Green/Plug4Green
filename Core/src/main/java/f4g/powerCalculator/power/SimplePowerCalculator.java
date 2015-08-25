package f4g.powerCalculator.power;

import f4g.commons.com.util.PowerData;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.*;
import org.apache.log4j.Logger;

public class SimplePowerCalculator implements IPowerCalculator {

	static Logger log;

	public SimplePowerCalculator() {

		log = Logger.getLogger(SimplePowerCalculator.class.getName());
	}

	/**
	 * 
	 * Computes the current power consumption of a server
	 * 
	 * @param server
	 * @return a data structure containing the power consumption in Watts of a server
	 */
	@Override
	public PowerData computePowerServer(Server server){

		PowerData pd = new PowerData();
		pd.setActualConsumption(100.0);
		log.debug("Power consumption of a server is: " + pd.getActualConsumption() + " Watt/hour");
		return pd;
	}

	
}
