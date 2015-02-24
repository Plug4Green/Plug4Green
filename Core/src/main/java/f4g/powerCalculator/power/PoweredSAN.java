package f4g.powerCalculator.power;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.Fan;
import f4g.schemas.java.metamodel.PSU;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.SAN;
import f4g.schemas.java.metamodel.Rack;
import f4g.schemas.java.metamodel.LogicalUnit;
import f4g.schemas.java.metamodel.NIC;
import f4g.schemas.java.metamodel.WaterCooler;
import f4g.schemas.java.metamodel.NetworkNode;

import java.util.Iterator;

public class PoweredSAN extends SAN implements PoweredComponent{

	private boolean simulationFlag;
	private JXPathContext sanContext;
	private SAN mySAN;
	static Logger log = Logger.getLogger(PoweredSAN.class.getName()); //
	
	public PoweredSAN(SAN san, boolean flag){
		this.mySAN = san;
		this.simulationFlag=flag;
		
	}//end of constructor
public void setContext(JXPathContext ctxt){
	this.sanContext=ctxt;
}
	
	/**
	 * This method computes the idle power of SAN
	 * @return
	 */
	public double computePowerIdle(){

		if (mySAN.getPowerIdle()==null || mySAN.getPowerIdle().getValue() <= 0.0){
			log.debug("Idle power of SAN mainboard is Null or zero or negative");
			return 0.0;
			
		}else if(!simulationFlag && mySAN.getMeasuredPower()!=null && mySAN.getMeasuredPower().getValue() > 0) return mySAN.getMeasuredPower().getValue();

		double powerSAN=0.0;
        boolean measuredPowerPSU = true;
        //boolean loadPSU = false;
        int countPSU = 0;
    	String psuQuery = "PSU";
		JXPathContext context = JXPathContext.newContext(this.sanContext);
    	Iterator psuPowerIterator = context.iterate(psuQuery);

    	/**
    	 * Iterate over the PSU to verify whether measuredPower information is 
    	 * provided by the monitoring system and to count the number of installed PSUs. 
    	 * 
    	 */
    	while(psuPowerIterator.hasNext())
    	{
    		PSU myPSU = (PSU)psuPowerIterator.next();
    	    if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
    	    	measuredPowerPSU = false;
    	    if(myPSU.getLoad().getValue() > 0.0)
    	    	//loadPSU = true;    	    	
    	    countPSU++;
    	}
    	
    	if(countPSU==0)	// No psu is loaded, no need to compute power of SAN.        		
    		powerSAN = 0.0;
    	else{

    		/**
    		 * compute the idle power of each Logical Unit
    		 */
    		String myQuery = "LogicalUnit";
    		Iterator iterator = context.iterate(myQuery);
    		// compute the power of all logical units
    		while(iterator.hasNext())
    		{	
    			LogicalUnit	lut = (LogicalUnit)iterator.next();
    			PoweredLogicalUnit plu = new PoweredLogicalUnit(lut);
    			plu.setContext(JXPathContext.newContext(lut));
    			double powerLUN = plu.computePowerIdle();
    			//Set the computed power to the hard disk class
				Power LUNPower=new Power();
				LUNPower.setValue(powerLUN);
				lut.setComputedPower(LUNPower);
				powerSAN=powerSAN+powerLUN;
    		} 
    		powerSAN = computeNICPowerIdle(this.sanContext) + powerSAN;
    		//compute the power of network part
    		powerSAN = computeCoolingSystemPower(this.sanContext) + powerSAN + this.powerIdle.getValue();	
    		//compute the power of PSU
    		powerSAN = computePSUPower(this.sanContext, powerSAN, countPSU, measuredPowerPSU) + powerSAN;	
    	}		
		return powerSAN;
	}//end of computePowerIdle function
	
	
	/**
	 * This method computes the total power consumption of SAN, which includes
	 * idle and dynamic.
	 */
	public double computePower(){

		if (mySAN.getPowerIdle()==null || mySAN.getPowerIdle().getValue() <= 0.0){
			log.debug("Idle power of SAN mainboard is Null or zero or negative");
			return 0.0;
			
		}else if(!simulationFlag && mySAN.getMeasuredPower()!=null && mySAN.getMeasuredPower().getValue() > 0) return mySAN.getMeasuredPower().getValue();

		double powerSAN=0.0;
        boolean measuredPowerPSU = true;
        //boolean loadPSU = false;
        int countPSU = 0;
    	String psuQuery = "PSU";
		JXPathContext context = JXPathContext.newContext(this.sanContext);
    	Iterator psuPowerIterator = context.iterate(psuQuery);

    	/**
    	 * Iterate over the PSU to verify whether measuredPower information is 
    	 * provided by the monitoring system and to count the number of installed PSUs. 
    	 * 
    	 */
    	while(psuPowerIterator.hasNext())
    	{
    		PSU myPSU = (PSU)psuPowerIterator.next();
    	    if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
    	    	measuredPowerPSU = false;
    	    if(myPSU.getLoad().getValue() > 0.0)
    	    	//loadPSU = true;    	    	
    	    countPSU++;
    	}
    	
    	if(countPSU==0)	// No PSU is loaded, no need to compute power of SAN.        		
    		powerSAN = 0.0;
    	else{
    		//compute the idle and dynamic power of network part within SAN.
    		powerSAN = computeNICPowerIdle(this.sanContext) + computeNICPower(context) + powerSAN;
    		// compute the power of logical units
    		powerSAN = computeLUNPower(this.sanContext) + powerSAN;	
    		//compute the power of cooling sytems e.g. fan and water cooler
    		powerSAN = computeCoolingSystemPower(this.sanContext) + this.powerIdle.getValue() + powerSAN;	
    		//compute the power of PSU
    		powerSAN = computePSUPower(this.sanContext, powerSAN, countPSU, measuredPowerPSU) + powerSAN;	
    	}
		
		return powerSAN;
	}//end of computePower function

	/**
	 * This function returns the power consumption of each logical units.
	 * @param context
	 * @return
	 */
	private double computeLUNPower(JXPathContext context){
		double power = 0.0;
		String myQuery = "LogicalUnit";
		Iterator iterator = context.iterate(myQuery);
		// compute the power of all attached hard drives
		while(iterator.hasNext())
		{	
			LogicalUnit	lut = (LogicalUnit)iterator.next();
			PoweredLogicalUnit plu = new PoweredLogicalUnit (lut);
			plu.setContext(JXPathContext.newContext(lut));
			double powerLUN = plu.computePower();
			//Set the computed power to the hard disk class
			Power LUNPower=new Power();
			LUNPower.setValue(powerLUN);
			lut.setComputedPower(LUNPower);
			power=power+powerLUN;
			
		}
		return power;
	}//end of computeHard drive power

	
	/**
	 * This function returns the idle power of network nodes e.g. NIC
	 * @param context
	 * @return
	 */
	private double computeNICPowerIdle(JXPathContext context){
		double power = 0.0;
		String myQuery = "EthernetNIC";
		Iterator iterator = context.iterate(myQuery);
		// compute the power of all attached hard drives
		while(iterator.hasNext())
		{	
			NetworkNode	nnt = (NetworkNode)iterator.next();
			PoweredNetworkNode nic = new PoweredNetworkNode (nnt);
			power = nic.getPowerIdle().getValue() + power;
		}

		myQuery = "FiberchannelNIC";
		iterator = context.iterate(myQuery);
		// compute the power of all attached hard drives
		while(iterator.hasNext())
		{				
			NetworkNode	nnt = (NetworkNode)iterator.next();
			PoweredNetworkNode nic = new PoweredNetworkNode (nnt);
			power = nic.getPowerIdle().getValue() + power;
		}

		return power;
	}//end of computeHard drive power
	
	/**
	 * This method computes the dynamic power of network nodes e.g. NIC
	 * @param context
	 * @return
	 */
	private double computeNICPower(JXPathContext context){
		double power = 0.0;
		String myQuery = "EthernetNIC";
		Iterator iterator = context.iterate(myQuery);
		// compute the power of all attached hard drives
		while(iterator.hasNext())
		{				
			NetworkNode	nnt = (NetworkNode)iterator.next();
			PoweredNetworkNode nic = new PoweredNetworkNode (nnt);	
			power = nic.computePower() + power;
		}

		myQuery = "FiberchannelNIC";
		iterator = context.iterate(myQuery);
		// compute the power of all attached hard drives
		while(iterator.hasNext())
		{				
			NetworkNode	nnt = (NetworkNode)iterator.next();
			PoweredNetworkNode nic = new PoweredNetworkNode (nnt);
			power = nic.computePower() + power;
		}

		return power;
	}//end of computeHard drive power
	
	
	/**
	 * This method returns the power consumption of cooling systems e.g. fan and water cooler.
	 * @param ctx
	 * @return
	 */
	private double computeCoolingSystemPower(JXPathContext ctx){
		
		double totalCoolingPower = 0.0;
		double fanPower=0.0;
		double waterCoolerPower=0.0;
		JXPathContext context = JXPathContext.newContext(ctx);
		
		/**
		 * Fan power consumption
		 */
		String fanQuery = "fan";
		Iterator fanPowerIterator = context.iterate(fanQuery);
		while(fanPowerIterator.hasNext())
        {		   	
           Fan myFan = (Fan)fanPowerIterator.next();
           PoweredFan myPoweredFan = new PoweredFan(myFan.getActualRPM(),myFan.getDepth(),myFan.getMaxRPM(),myFan.getPowerMax(),myFan.getMeasuredPower(),this.simulationFlag);
           
           double fanCurrentPower = myPoweredFan.computePower();   		   		           
           fanPower = fanPower + fanCurrentPower; 
           log.debug("The power consumption of the Fan is "+fanCurrentPower+ " Watts");
           
        }
		log.debug("The power consumption of the Fans is "+fanPower+ " Watts");
		totalCoolingPower = totalCoolingPower+fanPower;
		
		/**
		 * Water cooler power consumption
		 */
		String waterCoolerQuery = "waterCooler";
		Iterator waterCoolerPowerIterator = context.iterate(waterCoolerQuery);
		
		while(waterCoolerPowerIterator.hasNext())
        {		   	
		   WaterCooler myWaterCooler = (WaterCooler)waterCoolerPowerIterator.next();	
           double waterCoolerCurrentPower = 0.0;           
           waterCoolerPower = waterCoolerPower + waterCoolerCurrentPower; 
           log.debug("The power consumption of the Water Cooler is "+waterCoolerCurrentPower+ " Watts");           
        }
		log.debug("The power consumption of the Water Coolers is "+waterCoolerPower+ " Watts");
		totalCoolingPower = totalCoolingPower+waterCoolerPower;
		
		return totalCoolingPower;
	}//end of computeCoolingSystemPower method
	
	/**
	 * This method returns the power consumption of PSUs
	 * @param context
	 * @param sanPower
	 * @param countPSU
	 * @param measuredPowerPSU
	 * @return
	 */
	private double computePSUPower(JXPathContext context, double sanPower, int countPSU, boolean measuredPowerPSU){
		double power = 0.0;
		String myQuery = "PSU";
		Iterator iterator = context.iterate(myQuery);
		
    	while(iterator.hasNext())
    	{
    		PSU psuType = (PSU)iterator.next();
    		PoweredPSU myPSU = new PoweredPSU(psuType.getMeasuredPower().getValue(), psuType.getEfficiency().getValue());

			if(!simulationFlag && measuredPowerPSU) power = myPSU.computePower() + power;
			else if(sanPower > 0.0 ) power = myPSU.computePower() + power;  			
			else{
				double measuredPower = sanPower/countPSU;
				if(myPSU.getEfficiency().getValue()>0)
					power = Math.round((measuredPower/myPSU.getEfficiency().getValue())*100)-Math.round(sanPower/countPSU);				
				power = power + power;
			
			}   	
    	}//end of while
		return power;
	}//end of computeHard drive power

}//end of PowerSAN class
