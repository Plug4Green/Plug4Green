package f4g.powerCalculator.power;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.HardDiskType;
import f4g.schemas.java.metamodel.RAIDDiskType;

public class PoweredRAIDDisk extends RAIDDiskType implements PoweredComponent{
		
	private int totalHdd = 0;
	private double avgPower;
	private RAIDDiskType myRAIDDisk;
	static Logger log = Logger.getLogger(PoweredRAIDDisk.class.getName()); 
	
	public PoweredRAIDDisk(RAIDDiskType lobj){
		this.myRAIDDisk = lobj;
	}
	


	/**
	 * This method counts the total number of hard disk within a RAIDED Disk unit.
	 * @param iterator
	 * @return
	 */
	private int countHardDrive(){
		int n=0;
		JXPathContext context = JXPathContext.newContext(this.myRAIDDisk);
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
	 * This method computes the power consumption of RAIDED Disk Unit
	 */
	public double computePower(){
		double raidDiskPower=0.0;
		JXPathContext context = JXPathContext.newContext(this.myRAIDDisk);
		//it returns the total number of hard drives within a raided disk unit
		this.totalHdd = countHardDrive();

		if (myRAIDDisk.getMaxReadRate() == null || myRAIDDisk.getMaxWriteRate() == null || myRAIDDisk.getReadRate() == null || myRAIDDisk.getWriteRate() == null || myRAIDDisk.getMaxReadRate().getValue() < 0 || myRAIDDisk.getMaxWriteRate().getValue() < 0.0 || myRAIDDisk.getReadRate().getValue() < 0.0 || myRAIDDisk.getWriteRate().getValue() < 0.0){
			log.debug("Maximum and Average Read and Write rate of RAIDED Disk Unit cannot be NULL or Negative.");
			return 0.0;

		}else if (myRAIDDisk.getNumberOfReadOps() == null || myRAIDDisk.getNumberOfWriteOps() == null || myRAIDDisk.getNumberOfReadOps().getValue() < 0 || myRAIDDisk.getNumberOfWriteOps().getValue() < 0.0){
				log.debug("Number of read and write operations of RAIDED Disk Unit cannot be NULL or Negative.");
				return 0.0;

		}else if (myRAIDDisk.getBlockSize() == null || myRAIDDisk.getBlockSize().getValue() <= 0.0){
			log.debug("Block Size of RAIDED Disk Unit is Negative or Zero");
			return 0.0;

		}else if (myRAIDDisk.getLevel()== null ||  myRAIDDisk.getLevel().getValue() <0){
				log.debug("Raid level of RAIDED Disk Unit is invalid");
				return 0.0;
				
		}else if (myRAIDDisk.getMaxReadRate().getValue() < myRAIDDisk.getReadRate().getValue()){
			log.debug("Maximum Read rate of RAIDED Disk Unit cannot be smaller than the actual read rate");
			return 0.0;	
			
		}else if (myRAIDDisk.getMaxWriteRate().getValue() < myRAIDDisk.getWriteRate().getValue()){
			log.debug("Maximum write rate of RAIDED Disk Unit cannot be smaller than the actual write rate");
			return 0.0;
			
		}else{

			int hddReadCount=0, hddWriteCount=0;
			IoRateType maxReadRate = new IoRateType();
			IoRateType maxWriteRate = new IoRateType();
			IoRateType readSize = new IoRateType();
			readSize.setValue(myRAIDDisk.getReadRate().getValue()/myRAIDDisk.getNumberOfReadOps().getValue());		
			IoRateType writeSize = new IoRateType();
			writeSize.setValue(myRAIDDisk.getWriteRate().getValue()/myRAIDDisk.getNumberOfWriteOps().getValue());
			
			if (readSize.getValue() == 0.0 && writeSize.getValue() == 0.0){
				log.debug("Read and Write rate per operation is zero. Thus idle power of RAIDED Disk Unit is returned.");
				
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
					raidDiskPower = raidDiskPower + powerHDD;
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
		
				// The idle power is computed in case the number of read and write hdd is zero
				if (hddReadCount == 0 && hddWriteCount == 0){
					PoweredHardDiskDrive poweredHDD = new PoweredHardDiskDrive(curReadWriteRate, readSize, curReadWriteRate, writeSize, idlePow);
					poweredHDD.setLUNFlag(true);
					powerHDD=poweredHDD.computePower();
										
					//increment the total power of LUN
					raidDiskPower = raidDiskPower + powerHDD;
					
				}else{
					curReadWriteRate.setValue(myRAIDDisk.getBlockSize().getValue());
					idlePow.setValue(avgPower);
					PoweredHardDiskDrive poweredHDD = new PoweredHardDiskDrive(curReadWriteRate, readSize, curReadWriteRate, writeSize, idlePow);
					poweredHDD.setLUNFlag(true);
					//compute the power of read operation
					if (hddReadCount > 0)	raidDiskPower = (myRAIDDisk.getNumberOfReadOps().getValue()/(myRAIDDisk.getNumberOfReadOps().getValue()+myRAIDDisk.getNumberOfWriteOps().getValue()))*(hddReadCount*poweredHDD.computePower()) + raidDiskPower;				
					//compute the power of write operation
					if (hddWriteCount > 0)	raidDiskPower = (myRAIDDisk.getNumberOfWriteOps().getValue()/(myRAIDDisk.getNumberOfReadOps().getValue()+myRAIDDisk.getNumberOfWriteOps().getValue()))*(hddWriteCount*poweredHDD.computePower()) + raidDiskPower;
				}
			}
		}
		return raidDiskPower;
	}//end of computePower function		
	
	/**
	 * This function computes the idle power of RAIDED Disk Unit (having all hard drives)
	 * @return
	 */
	public double computePowerIdle(){
		double raidDiskPower = 0.0;
		double hddPower= 0.0;
		JXPathContext context = JXPathContext.newContext(this.myRAIDDisk);
		String myQuery = "hardDisk";
		Iterator itr = context.iterate(myQuery);

		while(itr.hasNext())
		{
			HardDiskType hdd = (HardDiskType)itr.next();
			hddPower = hdd.getPowerIdle().getValue();
			
			//Set the computed power to the hard disk class
			PowerType HDDPower=new PowerType();
			HDDPower.setValue(hddPower);
			hdd.setComputedPower(HDDPower);
			
			//increment the total power of RAIDED Disk Unit
			raidDiskPower = raidDiskPower + hddPower;
		} //end of while loop
				
		return raidDiskPower;
	}//end of computeWritePower method
	
	
	/**
	 * This method computes the number of disks involved with respect to
	 * different number of raid-levels.
	 * @param raid
	 * @return
	 */	
	private double getHDDWithRaidLevels(double opSize, int readFlag){
		double hddCount = 0;
		int level = myRAIDDisk.getLevel().getValue();
		
		switch (level){
			case 1:
				if (readFlag == 1) return 1;
				else return totalHdd;			// the whole file is mirrored to all other disks, all disks will be involved.
				
			case 2:
				return (opSize*1024 * 1024 * 8)/myRAIDDisk.getBlockSize().getValue();	//counts into bits since strip size is in bits.
				
			case 3:
				return (((int)opSize)*1024 * 1024)/myRAIDDisk.getBlockSize().getValue();	//counts into bits since strip size is in bits.
				
			case 10:
				if (readFlag == 1)	return (opSize*1024)/myRAIDDisk.getBlockSize().getValue();	// Due to strip, more than one drives can be involved.
				else return totalHdd;	// Due to mirroring, all hard drives will be involved.
				
			default:
				hddCount = (opSize*1024)/myRAIDDisk.getBlockSize().getValue();	// convert IORate into kilo bytes.
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

