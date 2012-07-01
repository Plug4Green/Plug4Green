package org.f4g.power;

import org.apache.commons.jxpath.JXPathContext;

import org.apache.log4j.Logger;
import org.f4g.schema.metamodel.CacheType;
import org.f4g.schema.metamodel.DimensionType;
import org.f4g.schema.metamodel.FanType;
import org.f4g.schema.metamodel.OperatingSystemTypeType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.NASType;
import org.f4g.schema.metamodel.RPMType;
import org.f4g.schema.metamodel.RackType;
import org.f4g.schema.metamodel.LogicalUnitType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.NICType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.WaterCoolerType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.ControllerType;
import org.f4g.schema.metamodel.HardDiskType;
import org.f4g.schema.metamodel.SolidStateDiskType;
import org.f4g.schema.metamodel.RAIDDiskType;
import org.f4g.com.util.PowerData;
import org.f4g.power.PoweredFan;

import java.util.Iterator;
import java.util.List;

public class PoweredNAS extends NASType implements PoweredComponent{

	private boolean simulationFlag;
	//private JXPathContext nasContext;
	private NASType myNAS;
	static Logger log = Logger.getLogger(PoweredNAS.class.getName()); //
	
	public PoweredNAS(NASType nas, boolean flag){
		this.myNAS = nas;
		this.simulationFlag=flag;
		
	}//end of constructor



/**
 * This method computes the idle power of NAS: if the powerIdle attribute of NAS is configured, then this value is returned.
 * Otherwise, it goes every single component and computes its idle power. In the end, a constant factor is added
 * which represents the power consumption of the components not captured by the NAS model. If every single 
 * component of NAS device is captured correctly, then this constant factor has a value of zero.
 * @return
 */
public double computePowerIdle(){

			JXPathContext context = JXPathContext.newContext(this.myNAS);
		    boolean measuredPowerPSU = true;
		    double nasPower=0.0;
		    double psuPower=0.0;
		    int countPSU=0;
	    	String psuQuery = "PSU";
	    	Iterator psuPowerIterator = context.iterate(psuQuery);	    	    	
				

	    	/** Iterate over the PSU to verify whether measuredPower information is provided by the monitoring system and to count the number 
	    	 * of  PSUs having a load >0. 
	    	 * 
	    	 */
	    	while(psuPowerIterator.hasNext())
	    	{
	    		PSUType myPSU = (PSUType)psuPowerIterator.next();
	    		if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
	   	    	measuredPowerPSU = false;
	    		if(myPSU.getLoad().getValue() > 0.0)	   	    	
	    		countPSU++;	    		
	    	}
	   	
	    	if(countPSU==0)          		
	   		  nasPower=0.0;
	    	else
	    	{
	    		
	    		/** Test whether the idle power or the measured power of the NAS device are given. 
		    	 * 
		    	 */
	    		if (!simulationFlag && myNAS.getMeasuredPower()!=null && myNAS.getMeasuredPower().getValue() > 0) return myNAS.getMeasuredPower().getValue();
	    		else if(myNAS.getPowerIdle()!=null && myNAS.getPowerIdle().getValue() >  0.0)
	    			return myNAS.getPowerIdle().getValue();
	    		 

	    		
	    		double nicPower =0.0;
	    		double controllerPower=0.0;
	    		double hddPower=0.0;
	    		double ssdPower=0.0;
	    		double fanPower=0.0;	    		
	    		double raidDiskPower=0.0;	
	    			/** compute power of network interface cards. 
			    	 * 
			    	 */	
	    			nicPower=NICIdlePower(this.myNAS);
	    			log.debug("The idle power consumption of the NAS NICs is "+nicPower+ " Watts");               
	    			nasPower=nasPower+nicPower;
	    			
	    			/** compute power of controller which is nothing but a CPU 
			    	 * 
			    	 */	
	    			String controllerQuery = "Controller";
	    	    	Iterator controllerIterator = context.iterate(controllerQuery); 	    	
	    				

	    	    	/** Iterate over the controllers 
	    	    	 * 
	    	    	 */
	    	    	while(controllerIterator.hasNext())
	    	    	{
	    	    		double contCurrentPower=0.0;
	    	    		ControllerType myController = (ControllerType)controllerIterator.next();
	    	    		if(myController.getPowerIdle()!=null && myController.getPowerIdle().getValue()>0.0)
	    	    		contCurrentPower=myController.getPowerIdle().getValue();
	    	    		
	    	    		log.debug("The idle power consumption of the NAS controller is "+contCurrentPower+ " Watts");  
		    	    	
	    	    		PowerType contCurrentPow = new PowerType();
	    	    		contCurrentPow.setValue(contCurrentPower);
	    	    		myController.setComputedPower(contCurrentPow);
	    	    		controllerPower=controllerPower+contCurrentPower;	    	    			    	    		             
	    			    
	    	    	}
	    	    	
	    	    	log.debug("The idle power consumption of the NAS controllers is "+controllerPower+ " Watts");  
	    	    	nasPower=nasPower+controllerPower;
	    		
	    	    	/** Iterate over the spare storage units which remain always idle 
	    	    	 * 
	    	    	 */
	    	    	String storageQuery = "hardDisk";
	    		    Iterator storagePowerIterator = context.iterate(storageQuery);
	    		    
	    	        while(storagePowerIterator.hasNext())
	    	        {
	    	        	double storageCurrentPower=0.0;
	    	        	HardDiskType myHardDisk = (HardDiskType)storagePowerIterator.next();
	    	        	if(myHardDisk.getPowerIdle()!=null && myHardDisk.getPowerIdle().getValue()>0.0)		    	    		
	    	        	storageCurrentPower = myHardDisk.getPowerIdle().getValue();	    	      	    
	    	            
	    	      	    PowerType storageCurrentPow = new PowerType();
	    	      	    storageCurrentPow.setValue(storageCurrentPower);
	    	            myHardDisk.setComputedPower(storageCurrentPow);
	    	            hddPower = hddPower + storageCurrentPower; 
	    	            log.debug("The idle power consumption of the NAS spare Hard Disk Unit is "+storageCurrentPower+ " Watts");               
	    	         }	

	    	        log.debug("The idle power consumption of the NAS spare Hard Disk Units "+hddPower+ " Watts");  
	    	    	nasPower=nasPower+hddPower;
	    	        
	    	        storageQuery = "solidStateDisk";
	    		    storagePowerIterator = context.iterate(storageQuery);	      		  
	    	        while(storagePowerIterator.hasNext())
	    	        {
	    	        	double storageCurrentPower=0.0;
	    	        	SolidStateDiskType mySolidStateDisk = (SolidStateDiskType)storagePowerIterator.next();	    	      	    
	    	        	if(mySolidStateDisk.getPowerIdle()!=null && mySolidStateDisk.getPowerIdle().getValue()>0.0)    		
		    	        storageCurrentPower = mySolidStateDisk.getPowerIdle().getValue();
	    	            
	    	            PowerType storageCurrentPow = new PowerType();
	    	      	    storageCurrentPow.setValue(storageCurrentPower);
	    	            mySolidStateDisk.setComputedPower(storageCurrentPow);
	    	            ssdPower = ssdPower + storageCurrentPower; 
	    	            log.debug("The idle power consumption of the NAS spare Solid State Disk is "+storageCurrentPower+ " Watts");               
	    	         }
	    	    	
	    	    	log.debug("The idle power consumption of the NAS spare Solid State Disk Units is "+ssdPower+ " Watts");  
	    	    	nasPower=nasPower+ssdPower;
	    		
	    	    	/** compute power of fans 
			    	 * 
			    	 */	
	    			String fanQuery = "Fan";
	    	    	Iterator fanIterator = context.iterate(fanQuery); 	    	
	    				

	    	    	/** Iterate over the fans 
	    	    	 * 
	    	    	 */
	    	    	while(fanIterator.hasNext())
	    	    	{
	    	    		double fanCurrentPower=0.0;
	    	    		FanType myFan = (FanType)fanIterator.next();
	    	    		PoweredFan myPoweredFan = new PoweredFan(myFan.getActualRPM(),myFan.getDepth(),myFan.getMaxRPM(),myFan.getPowerMax(),myFan.getMeasuredPower(),this.simulationFlag);
	    	    		    	    					
	    	    		fanCurrentPower=myPoweredFan.computePower();
	    	    		
	    	    		log.debug("The power consumption of the NAS fan is "+fanCurrentPower+ " Watts");  
		    	    	
	    	    		PowerType fanCurrentPow = new PowerType();
	    	    		fanCurrentPow.setValue(fanCurrentPower);
	    	    		myFan.setComputedPower(fanCurrentPow);
	    	    		fanPower=fanPower+fanCurrentPower;	    	    			    	    		             
	    			    
	    	    	}
	    	    	
	    	    	log.debug("The power consumption of the NAS fans is "+fanPower+ " Watts");  
	    	    	nasPower=nasPower+fanPower;
	    	    	
	    	    	//TODO: In the future add an idle power consumption model for Cache 
	    	    	
	    	    	/** compute power of Raided Disks 
			    	 * 
			    	 */	
	    			String raidDiskQuery = "RAIDDisk";
	    	    	Iterator raidDiskIterator = context.iterate(raidDiskQuery);
	    	    	
	    	    	/** Iterate over the RAID Disk 
	    	    	 * 
	    	    	 */
	    	    	while(raidDiskIterator.hasNext())
	    	    	{
	    	    		double raidDiskCurrentPower=0.0;
	    	    		RAIDDiskType myRAIDDisk= (RAIDDiskType)raidDiskIterator.next();
	    	    		PoweredRAIDDisk myPoweredRAIDDisk= new PoweredRAIDDisk(myRAIDDisk);
	    	    		    	    					
	    	    		raidDiskCurrentPower=myPoweredRAIDDisk.computePowerIdle();
	    	    		
	    	    		log.debug("The power consumption of the NAS RAIDED Disk Unit is "+raidDiskCurrentPower+ " Watts");  
		    	    	
	    	    		PowerType raidDiskCurrentPow = new PowerType();
	    	    		raidDiskCurrentPow.setValue(raidDiskCurrentPower);
	    	    		myRAIDDisk.setComputedPower(raidDiskCurrentPow);
	    	    		raidDiskPower=raidDiskPower+raidDiskCurrentPower;	    	    			    	    		             
	    			    
	    	    	}
	    	    	
	    	    	log.debug("The power consumption of the NAS RAIDED Disk Units is "+raidDiskPower+ " Watts");  
	    	    	nasPower=nasPower+raidDiskPower;
	    		
	    	    	/** Add the constant factor 
			    	 * 
			    	 */	
	    	    	nasPower=nasPower+this.myNAS.getConstantFactor().getValue();
	    	    	
	    	    	/** compute power of PSUs. 
			    	 * 
			    	 */	
	    	    	psuPower=computePSUPower(nasPower,countPSU,measuredPowerPSU);
	    			log.debug("The power consumption of the NAS PSU is "+psuPower+ " Watts");               
	    			nasPower=nasPower+psuPower;
	    	    	
	    	    	
	    	}
	    	
	    	log.debug("The idle power consumption of the NAS device is "+nasPower+ " Watts");
	    	return nasPower;
}
/**
 * This method computes the overall power consumption of NAS
 * @return
 */
public double computePower(){

	JXPathContext context = JXPathContext.newContext(this.myNAS);
    boolean measuredPowerPSU = true;
    double nasPower=0.0;
    double psuPower=0.0;
    int countPSU=0;
	String psuQuery = "PSU";
	Iterator psuPowerIterator = context.iterate(psuQuery);	    	    	
		

	/** Iterate over the PSU to verify whether measuredPower information is provided by the monitoring system and to count the number 
	 * of  PSUs having a load >0. 
	 * 
	 */
	while(psuPowerIterator.hasNext())
	{
		PSUType myPSU = (PSUType)psuPowerIterator.next();
		if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
	    	measuredPowerPSU = false;
		if(myPSU.getLoad().getValue() > 0.0)	   	    	
		countPSU++;	    		
	}
	
	if(countPSU==0)          		
		 nasPower=0.0;
	else
	{
		
		/** Test whether the measured power of the NAS device is given. 
    	 * 
    	 */
		if(!simulationFlag && myNAS.getMeasuredPower()!=null && myNAS.getMeasuredPower().getValue() > 0) return myNAS.getMeasuredPower().getValue();
		
		double nicPower =0.0;
		double controllerPower=0.0;
		double hddPower=0.0;
		double ssdPower=0.0;
		double fanPower=0.0;	    		
		double raidDiskPower=0.0;	
		
			/** compute idle and dynamic power of network interface cards. 
	    	 * 
	    	 */	
			nicPower=NICIdlePower(this.myNAS)+NICPower(this.myNAS);
			log.debug("The overall power consumption of the NAS NICs is "+nicPower+ " Watts");               
			nasPower=nasPower+nicPower;
			
			/** compute power of controller which is nothing but a CPU 
	    	 * 
	    	 */	
			String controllerQuery = "Controller";
	    	Iterator controllerIterator = context.iterate(controllerQuery); 	    	
				

	    	/** Iterate over the controllers 
	    	 * 
	    	 */
	    	while(controllerIterator.hasNext())
	    	{
	    		double contCurrentPower=0.0;
	    		ControllerType myController = (ControllerType)controllerIterator.next();
	    		PoweredCPU myPoweredCPU = new PoweredCPU(myController,getOperatingSystem(myController));		
	    			    		
	    		if (myPoweredCPU.getCpuUsage().getValue() > 0.0)	contCurrentPower = myPoweredCPU.computePower();
	    		
	    		PowerType contCurrentPow = new PowerType();
	    		contCurrentPow.setValue(contCurrentPower);
	    		myController.setComputedPower(contCurrentPow);	   		
	    		 		
	    		log.debug("The power consumption of the NAS controller is "+contCurrentPower+ " Watts");    	    	
	    		
	    		controllerPower=controllerPower+contCurrentPower;	    	    			    	    		             
			    
	    	}
	    	
	    	log.debug("The overall power consumption of the NAS controllers is "+controllerPower+ " Watts");  
	    	nasPower=nasPower+controllerPower;
		
	    	/** Iterate over the spare storage units which remain always idle 
	    	 * 
	    	 */
	    	String storageQuery = "hardDisk";
		    Iterator storagePowerIterator = context.iterate(storageQuery);
		    
	        while(storagePowerIterator.hasNext())
	        {
	        	double storageCurrentPower=0.0;
	        	HardDiskType myHardDisk = (HardDiskType)storagePowerIterator.next();
	        	if(myHardDisk.getPowerIdle()!=null && myHardDisk.getPowerIdle().getValue()>0.0)		    	    		
	        	storageCurrentPower = myHardDisk.getPowerIdle().getValue();	    	      	    
	            
	      	    PowerType storageCurrentPow = new PowerType();
	      	    storageCurrentPow.setValue(storageCurrentPower);
	            myHardDisk.setComputedPower(storageCurrentPow);
	            hddPower = hddPower + storageCurrentPower; 
	            log.debug("The idle power consumption of the NAS spare Hard Disk Unit is "+storageCurrentPower+ " Watts");               
	         }	

	        log.debug("The idle power consumption of the NAS spare Hard Disk Units "+hddPower+ " Watts");  
	    	nasPower=nasPower+hddPower;
	        
	        storageQuery = "solidStateDisk";
		    storagePowerIterator = context.iterate(storageQuery);	      		  
	        while(storagePowerIterator.hasNext())
	        {
	        	double storageCurrentPower=0.0;
	        	SolidStateDiskType mySolidStateDisk = (SolidStateDiskType)storagePowerIterator.next();	    	      	    
	        	if(mySolidStateDisk.getPowerIdle()!=null && mySolidStateDisk.getPowerIdle().getValue()>0.0)    		
    	        storageCurrentPower = mySolidStateDisk.getPowerIdle().getValue();
	            
	            PowerType storageCurrentPow = new PowerType();
	      	    storageCurrentPow.setValue(storageCurrentPower);
	            mySolidStateDisk.setComputedPower(storageCurrentPow);
	            ssdPower = ssdPower + storageCurrentPower; 
	            log.debug("The idle power consumption of the NAS spare Solid State Disk is "+storageCurrentPower+ " Watts");               
	         }
	    	
	    	log.debug("The idle power consumption of the NAS spare Solid State Disk Units is "+ssdPower+ " Watts");  
	    	nasPower=nasPower+ssdPower;
		
	    	/** compute power of fans 
	    	 * 
	    	 */	
			String fanQuery = "Fan";
	    	Iterator fanIterator = context.iterate(fanQuery); 	    	
				

	    	/** Iterate over the fans 
	    	 * 
	    	 */
	    	while(fanIterator.hasNext())
	    	{
	    		double fanCurrentPower=0.0;
	    		FanType myFan = (FanType)fanIterator.next();
	    		PoweredFan myPoweredFan = new PoweredFan(myFan.getActualRPM(),myFan.getDepth(),myFan.getMaxRPM(),myFan.getPowerMax(),myFan.getMeasuredPower(),this.simulationFlag);
	    		    	    					
	    		fanCurrentPower=myPoweredFan.computePower();
	    		
	    		log.debug("The power consumption of the NAS fan is "+fanCurrentPower+ " Watts");  
    	    	
	    		PowerType fanCurrentPow = new PowerType();
	    		fanCurrentPow.setValue(fanCurrentPower);
	    		myFan.setComputedPower(fanCurrentPow);
	    		fanPower=fanPower+fanCurrentPower;	    	    			    	    		             
			    
	    	}
	    	
	    	log.debug("The power consumption of the NAS fans is "+fanPower+ " Watts");  
	    	nasPower=nasPower+fanPower;
	    	
	    	/** compute power of Cache 
	    	 * 
	    	 */	 
	    	double totalCachePower=0.0;	
	    	double hitRatio=0.0;
	    	int countCache=0;
			String cacheQuery = "cache";
			Iterator cachePowerIterator = context.iterate(cacheQuery);
			
	        while(cachePowerIterator.hasNext())
	        {
	      	  CacheType myCache = (CacheType)cachePowerIterator.next();
	      	  //TODO: In the future, implement the power consumption function for the Cache
	          double cacheCurrentPower = 5.5;
	          
	          PowerType cacheCurrentPow = new PowerType();
	          cacheCurrentPow.setValue(cacheCurrentPower);
	          myCache.setComputedPower(cacheCurrentPow);
	          totalCachePower = totalCachePower + cacheCurrentPower;
	          hitRatio=hitRatio+myCache.getHitRatio().getValue();
	          countCache++;
	          log.debug("The power consumption of the Cache is "+cacheCurrentPower+ " Watts");               
	         }
	        /** compute average hit ratio 
	    	 * 
	    	 */	
	        hitRatio=hitRatio/countCache;
	        
	        
	        nasPower=nasPower+(hitRatio)*totalCachePower;
	    	/** compute power of Raided Disks 
	    	 * 
	    	 */	
			String raidDiskQuery = "RAIDDisk";
	    	Iterator raidDiskIterator = context.iterate(raidDiskQuery);
	    	
	    	/** Iterate over the RAID Disk 
	    	 * 
	    	 */
	    	while(raidDiskIterator.hasNext())
	    	{
	    		double raidDiskCurrentPower=0.0;
	    		RAIDDiskType myRAIDDisk= (RAIDDiskType)raidDiskIterator.next();
	    		PoweredRAIDDisk myPoweredRAIDDisk= new PoweredRAIDDisk(myRAIDDisk);
	    		    	    					
	    		raidDiskCurrentPower=myPoweredRAIDDisk.computePower();
	    		
	    		log.debug("The power consumption of the NAS RAIDED Disk Unit is "+raidDiskCurrentPower+ " Watts");  
    	    	
	    		PowerType raidDiskCurrentPow = new PowerType();
	    		raidDiskCurrentPow.setValue(raidDiskCurrentPower);
	    		myRAIDDisk.setComputedPower(raidDiskCurrentPow);
	    		raidDiskPower=raidDiskPower+raidDiskCurrentPower;	    	    			    	    		             
			    
	    	}
	    	
	    	log.debug("The power consumption of the NAS RAIDED Disk Units is "+raidDiskPower+ " Watts");  
	    	
	    	/** add the overall power to the NAS device based on the cache's hit ratio 
	    	 * 
	    	 */	
	    	
	    	nasPower=nasPower+(1-hitRatio)*raidDiskPower;
		
	    	/** compute power of PSUs. 
	    	 * 
	    	 */	
	    	psuPower=computePSUPower(nasPower,countPSU,measuredPowerPSU);
			log.debug("The power consumption of the NAS PSU is "+psuPower+ " Watts");               
			nasPower=nasPower+psuPower;
	    	
	    	
	}
	
	log.debug("The overall power consumption of the NAS device is "+nasPower+ " Watts");
	return nasPower;

	
}
/**
 * This method returns the power consumption of PSUs
 * @param context
 * @param sanPower
 * @param countPSU
 * @param measuredPowerPSU
 * @return
 */
private double computePSUPower(double nasPower, int countPSU, boolean measuredPowerPSU){
	
	double totalPSUPower = 0.0;
	JXPathContext context = JXPathContext.newContext(this.myNAS);
	String myQuery = "PSU";
	Iterator iterator = context.iterate(myQuery);
	
	while(iterator.hasNext())
	{
		PowerType psuCurrentPower = new PowerType();
		psuCurrentPower.setValue(0.0);
		
		PSUType myPSU = (PSUType)iterator.next();
		
		if(myPSU.getLoad().getValue()>0)
    	{		
    		if(!this.simulationFlag && measuredPowerPSU)
    		{
    			PoweredPSU myPoweredPSU = new PoweredPSU(myPSU.getMeasuredPower().getValue(), myPSU.getEfficiency().getValue());
    			psuCurrentPower.setValue(myPoweredPSU.computePower());
    			    			
    		}
    		/**
    		 * If the measuredPower at the server level is provided by the monitoring system, then this measuredPower is evenly distrbiuted 
    		 * among all the PSUs inside the same server 
    		 * 
    		 */
    		else if(!simulationFlag && this.myNAS.getMeasuredPower() != null && this.myNAS.getMeasuredPower().getValue()>0){
    			PoweredPSU myPoweredPSU = new PoweredPSU(this.myNAS.getMeasuredPower().getValue()/countPSU,myPSU.getEfficiency().getValue());
     			psuCurrentPower.setValue(myPoweredPSU.computePower());    			
    			
    		}else{
    			double measuredPower=nasPower/countPSU;    			
    			if(myPSU.getEfficiency().getValue()>0)
    				psuCurrentPower.setValue(Math.round((measuredPower/myPSU.getEfficiency().getValue())*100)-Math.round((nasPower/countPSU)));	    			
    			    			
    		}	    			    			    			
    	}//end of if ( PSU has a load >0)
		
		log.debug("The power consumption of the PSU is "+psuCurrentPower.getValue()+ " Watts");
		myPSU.setComputedPower(psuCurrentPower);
		totalPSUPower = totalPSUPower+psuCurrentPower.getValue();	
		  	
	}//end of while
	return totalPSUPower;
}//end of computeHard drive power

/**	 
 * Computes the current power consumption of the network interface cards
 * 
 * @param obj:input object of type Mainboard, SAN, and Enclosure
 * @return double value containing information on power consumption of the network interface cards
 */	
private double NICPower(Object obj){
  
  double totalNICPower=0.0;  
  JXPathContext context = JXPathContext.newContext(obj);
  String nicQuery = "NIC";
  
  Iterator nicPowerIterator = context.iterate(nicQuery);		 
  while(nicPowerIterator.hasNext())
  	{
	  double nicCurrentPower = 0.0;
  		NICType myNIC = (NICType)nicPowerIterator.next();
  		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
  		nicCurrentPower = myNetworkNode.computePower(); 
  		
  		PowerType nicCurrentPow = new PowerType();
  		nicCurrentPow.setValue(nicCurrentPower);
  		myNIC.setComputedPower(nicCurrentPow);
  		totalNICPower = totalNICPower + nicCurrentPower; 
  		log.debug("The power consumption of the Network Interface Card is "+nicCurrentPower+ " Watts");               
  	}
//Ethernet NIC power
  nicQuery = "EthernetNIC";
 
 nicPowerIterator = context.iterate(nicQuery);		 
 while(nicPowerIterator.hasNext())
 	{
	    	double nicCurrentPower = 0.0;
 		NICType myNIC = (NICType)nicPowerIterator.next();
 		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
  		nicCurrentPower = myNetworkNode.computePower(); 
 		      		
 		PowerType nicCurrentPow = new PowerType();
 		nicCurrentPow.setValue(nicCurrentPower);
 		myNIC.setComputedPower(nicCurrentPow);
 		totalNICPower = totalNICPower + nicCurrentPower; 
 		log.debug("The power consumption of the Ethernet Network Interface Card is "+nicCurrentPower+ " Watts");               
 	}
 
//Fiber channel NIC power
 nicQuery = "FiberchannelNIC";

nicPowerIterator = context.iterate(nicQuery);		 
while(nicPowerIterator.hasNext())
	{
	    	double nicCurrentPower = 0.0;
		NICType myNIC = (NICType)nicPowerIterator.next();
		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
  		nicCurrentPower = myNetworkNode.computePower(); 
		      		
		PowerType nicCurrentPow = new PowerType();
		nicCurrentPow.setValue(nicCurrentPower);
		myNIC.setComputedPower(nicCurrentPow);
		totalNICPower = totalNICPower + nicCurrentPower; 
		log.debug("The power consumption of the Fiber Channel Network Interface Card is "+nicCurrentPower+ " Watts");               
	}
  return totalNICPower;
}
/**	 
 * Computes the current power consumption of the network interface cards
 * 
 * @param obj:input object of type Mainboard, SAN, and Enclosure
 * @return double value containing information on power consumption of the network interface cards
 */	
private double NICIdlePower(Object obj){
  
  double totalNICPower=0.0;  
  JXPathContext context = JXPathContext.newContext(obj);
  String nicQuery = "NIC";
  
  Iterator nicPowerIterator = context.iterate(nicQuery);		 
  while(nicPowerIterator.hasNext())
  	{
	    double nicCurrentPower = 0.0;
  		NICType myNIC = (NICType)nicPowerIterator.next();
  		nicCurrentPower=myNIC.getPowerIdle().getValue();
  		      		
  		PowerType nicCurrentPow = new PowerType();
  		nicCurrentPow.setValue(nicCurrentPower);
  		myNIC.setComputedPower(nicCurrentPow);
  		totalNICPower = totalNICPower + nicCurrentPower; 
  		log.debug("The idle power consumption of the Network Interface Card is "+nicCurrentPower+ " Watts");               
  	}
  
  //Ethernet NIC idle power
   nicQuery = "EthernetNIC";
  
  nicPowerIterator = context.iterate(nicQuery);		 
  while(nicPowerIterator.hasNext())
  	{
	    double nicCurrentPower = 0.0;
  		NICType myNIC = (NICType)nicPowerIterator.next();
  		nicCurrentPower=myNIC.getPowerIdle().getValue();
  		      		
  		PowerType nicCurrentPow = new PowerType();
  		nicCurrentPow.setValue(nicCurrentPower);
  		myNIC.setComputedPower(nicCurrentPow);
  		totalNICPower = totalNICPower + nicCurrentPower; 
  		log.debug("The idle power consumption of the Ethernet Network Interface Card is "+nicCurrentPower+ " Watts");               
  	}
  
//Fiber channel NIC idle power
  nicQuery = "FiberchannelNIC";
 
 nicPowerIterator = context.iterate(nicQuery);		 
 while(nicPowerIterator.hasNext())
 	{
	    double nicCurrentPower = 0.0;
 		NICType myNIC = (NICType)nicPowerIterator.next();
 		nicCurrentPower=myNIC.getPowerIdle().getValue();
 		      		
 		PowerType nicCurrentPow = new PowerType();
 		nicCurrentPow.setValue(nicCurrentPower);
 		myNIC.setComputedPower(nicCurrentPow);
 		totalNICPower = totalNICPower + nicCurrentPower; 
 		log.debug("The idle power consumption of the Fiber Channel Network Interface Card is "+nicCurrentPower+ " Watts");               
 	}
  return totalNICPower;
}


/**	 
 * A function to return the operating system running on the server
 * 
 * @param obj:input object of type Server
 * @return OperatingSystemTypeType value containing information on the operating system
 */
private OperatingSystemTypeType getOperatingSystem(ControllerType obj){

	if(obj.getNativeHypervisor() != null){
		if(obj.getNativeHypervisor().getName() == null){
			return OperatingSystemTypeType.LINUX;
		}
		return obj.getNativeHypervisor().getName();
	} else {
		if(obj.getNativeOperatingSystem().getName() == null){
			return OperatingSystemTypeType.LINUX;
		}
		return obj.getNativeOperatingSystem().getName();
	}

	
}




}
