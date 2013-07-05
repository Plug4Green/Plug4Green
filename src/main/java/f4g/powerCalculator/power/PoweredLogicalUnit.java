package org.f4g.power;

import java.util.Iterator;

import org.apache.commons.jxpath.JXPathContext;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.SANType;
import org.f4g.schema.metamodel.HardDiskType;
import org.f4g.schema.metamodel.LogicalUnitType;
import org.apache.log4j.Logger;

public class PoweredLogicalUnit extends LogicalUnitType implements PoweredComponent{
	
	private JXPathContext context;
	private int totalHdd = 0;
	private double avgPower;
	private LogicalUnitType lut;
	static Logger log = Logger.getLogger(PoweredLogicalUnit.class.getName()); 
	
	public PoweredLogicalUnit(LogicalUnitType lobj){
		this.lut = lobj;
	}
	
	public void setContext(JXPathContext ctxt){
		this.context = ctxt;
	}
	

	/**
	 * This method counts the total number of hard disk within a logical unit.
	 * @param iterator
	 * @return
	 */
	private int countHardDrive(){
		int n=0;
		String myQuery = "hardDisk";
		Iterator iterator = context.iterate(myQuery);
		while(iterator.hasNext())
		{	
			HardDiskType hdd = (HardDiskType)iterator.next();
			avgPower = avgPower + hdd.getPowerIdle().getValue();
			n = n + 1;
		}
		
		if ( n > 1) this.avgPower = avgPower / n;
		return n;
	}//end of countHDD method

	/**
	 * This method compute the power consumption of Logical Unit
	 */
	public double computePower(){
		double lunPower=0.0;
		
		//it returns the total number of hard drives within a logical unit
		this.totalHdd = countHardDrive();

		if (lut.getMaxReadRate() == null || lut.getMaxWriteRate() == null || lut.getReadRate() == null || lut.getWriteRate() == null || lut.getMaxReadRate().getValue() < 0 || lut.getMaxWriteRate().getValue() < 0.0 || lut.getReadRate().getValue() < 0.0 || lut.getWriteRate().getValue() < 0.0){
			log.debug("Maximum and Average Read and Write rate of Logical Unit cannot be NULL or Negative.");
			return 0.0;

		}else if (lut.getNumOfReadOP() == null || lut.getNumOfWriteOP() == null || lut.getNumOfReadOP().getValue() < 0 || lut.getNumOfWriteOP().getValue() < 0.0){
				log.debug("Number of read and write operations of Logical Unit cannot be NULL or Negative.");
				return 0.0;

		}else if (lut.getStripSize() == null || lut.getStripSize().getValue() <= 0.0){
			log.debug("Strip Size is Negative or Zero");
			return 0.0;

		}else if (lut.getRAID() == null || lut.getRAID().getLevel() == null || lut.getRAID().getLevel().getValue() <0){
				log.debug("Raid level is invalid");
				return 0.0;
				
		}else if (lut.getMaxReadRate().getValue() < lut.getReadRate().getValue()){
			log.debug("Maximum Read rate cannot be smaller than the actual read rate");
			return 0.0;	
			
		}else if (lut.getMaxWriteRate().getValue() < lut.getWriteRate().getValue()){
			log.debug("Maximum write rate cannot be smaller than the actual write rate");
			return 0.0;
			
		}else{

			int hddReadCount=0, hddWriteCount=0;
			IoRateType maxReadRate = new IoRateType();
			IoRateType maxWriteRate = new IoRateType();
			IoRateType readSize = new IoRateType();
			readSize.setValue(lut.getReadRate().getValue()/lut.getNumOfReadOP().getValue());		
			IoRateType writeSize = new IoRateType();
			writeSize.setValue(lut.getWriteRate().getValue()/lut.getNumOfWriteOP().getValue());
			
			if (readSize.getValue() == 0.0 && writeSize.getValue() == 0.0){
				log.debug("Read and Write rate per operation is zero. Thus idle power of Logical Unit is returned.");
				
				String myQuery = "hardDisk";
				Iterator iterator = context.iterate(myQuery);				
				while(iterator.hasNext())
				{	
					double powerHDD=0.0;
					HardDiskType hdd = (HardDiskType)iterator.next();
					maxReadRate.setValue(hdd.getMaxReadRate().getValue());
					maxWriteRate.setValue(hdd.getMaxWriteRate().getValue());
					PoweredHardDiskDrive poweredHDD = new PoweredHardDiskDrive(readSize,maxReadRate, writeSize, maxWriteRate, hdd.getPowerIdle());
					poweredHDD.setLUNFlag(true);
					powerHDD=poweredHDD.computePower();
					//Set the computed power to the hard disk class
					PowerType HDDPower=new PowerType();
					HDDPower.setValue(powerHDD);
					hdd.setComputedPower(HDDPower);
					
					//increment the total power of LUN
					lunPower = lunPower + powerHDD;
				}
			}else{
				double powerHDD=0.0;
				hddReadCount =  (int)getHDDWithRaidLevels(readSize.getValue(), 1);
				hddWriteCount = (int)getHDDWithRaidLevels(writeSize.getValue(), 0);
	
				if (hddReadCount > totalHdd ) hddReadCount = totalHdd;
				if (hddWriteCount > totalHdd ) hddWriteCount = totalHdd;

				IoRateType curReadWriteRate = new IoRateType();
				curReadWriteRate.setValue(0.0);
				PowerType idlePow = new PowerType();
				idlePow.setValue(avgPower);
		
				//read rate is zero, only the idle power is enough
				if (hddReadCount == 0 && hddWriteCount == 0){
					PoweredHardDiskDrive poweredHDD = new PoweredHardDiskDrive(curReadWriteRate, readSize, curReadWriteRate, writeSize, idlePow);
					poweredHDD.setLUNFlag(true);
					powerHDD=poweredHDD.computePower();
										
					//increment the total power of LUN
					lunPower = lunPower + powerHDD;
					
				}else{
					curReadWriteRate.setValue(lut.getStripSize().getValue());
					idlePow.setValue(avgPower);
					PoweredHardDiskDrive poweredHDD = new PoweredHardDiskDrive(curReadWriteRate, readSize, curReadWriteRate, writeSize, idlePow);
					poweredHDD.setLUNFlag(true);
					//compute the power of read operation
					if (hddReadCount > 0)	lunPower = (lut.getNumOfReadOP().getValue()/(lut.getNumOfReadOP().getValue()+lut.getNumOfWriteOP().getValue()))*(hddReadCount*poweredHDD.computePower()) + lunPower;				
					//compute the power of write operation
					if (hddWriteCount > 0)	lunPower = (lut.getNumOfWriteOP().getValue()/(lut.getNumOfReadOP().getValue()+lut.getNumOfWriteOP().getValue()))*(hddWriteCount*poweredHDD.computePower()) + lunPower;
				}
			}
		}
		return lunPower;
	}//end of computePower function		
	
	/**
	 * This function compute the idle power of Logical Unit (having all hard drives)
	 * @return
	 */
	public double computePowerIdle(){
		double powerLUN = 0.0;
		double powerHDD= 0.0;
		String myQuery = "hardDisk";
		Iterator itr = context.iterate(myQuery);

		while(itr.hasNext())
		{
			HardDiskType hdd = (HardDiskType)itr.next();
			powerHDD = hdd.getPowerIdle().getValue();
			
			//Set the computed power to the hard disk class
			PowerType HDDPower=new PowerType();
			HDDPower.setValue(powerHDD);
			hdd.setComputedPower(HDDPower);
			
			//increment the total power of LUN
			powerLUN = powerLUN + powerHDD;
		} //end of while loop
				
		return powerLUN;
	}//end of computeWritePower method
	
	
	/**
	 * This method computes the number of disks involved with respect to
	 * different number of raid-levels.
	 * @param raid
	 * @return
	 */	
	private double getHDDWithRaidLevels(double opSize, int readFlag){
		double hddCount = 0;
		int level = lut.getRAID().getLevel().getValue();
		
		switch (level){
			case 1:
				if (readFlag == 1) return 1;
				else return totalHdd;			// the whole file is mirrored to all other disks, all disks will be involved.
				
			case 2:
				return (opSize*1024 * 1024 * 8)/lut.getStripSize().getValue();	//counts into bits since strip size is in bits.
				
			case 3:
				return (((int)opSize)*1024 * 1024)/lut.getStripSize().getValue();	//counts into bits since strip size is in bits.
				
			case 10:
				if (readFlag == 1)	return (opSize*1024)/lut.getStripSize().getValue();	// Due to strip, more than one drives can be involved.
				else return totalHdd;	// Due to mirroring, all hard drives will be involved.
				
			default:
				hddCount = (opSize*1024)/lut.getStripSize().getValue();	// convert IORate into kilo bytes.
		}//end of switch condition
			
		/**
		 *  This block of code determines the number of involved disks for parity with
		 *  respect to the different raid-levels 
		 */
		if (level == 4 || level == 5 || level == 6){
			// one hard disk is dedicated as parity hard disk
			if (level == 4) hddCount = hddCount + 1;	
			// one parity disk is involved after every 4 blocks
			else if (level == 5){
				if (hddCount % 4 != 0)	hddCount = hddCount/4 + hddCount + 1;	
				else hddCount = hddCount/4 + hddCount;
			// two parity disks are involved after every 4 blocks
			}else if (level == 6){
				if (hddCount % 4 != 0)	hddCount = 2 * (hddCount/4) + hddCount + 2;	
				else hddCount = 2 * (hddCount/4) + hddCount;
			}
		}
		return hddCount;
	}//end of hddWithRaidLevels method
}//end of class
