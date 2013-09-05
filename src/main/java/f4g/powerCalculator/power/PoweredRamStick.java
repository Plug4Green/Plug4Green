package f4g.powerCalculator.power;
import f4g.powerCalculator.power.PoweredComponent;
import f4g.schemas.java.metamodel.BufferTypeType;
import f4g.schemas.java.metamodel.FrequencyType;
import f4g.schemas.java.metamodel.RAMSizeType;
import f4g.schemas.java.metamodel.RAMTypeType;
import f4g.schemas.java.metamodel.RAMStickType;
import f4g.schemas.java.metamodel.VoltageType;
import f4g.schemas.java.metamodel.RAMTypeVendorType;
import org.apache.log4j.Logger;

public class PoweredRamStick extends RAMStickType implements PoweredComponent{	
	
	private final static double constantFrequency = 1600;
	private final static double constantFactor = 0.000026;
	private final static double beta = 7.347;
	private double factor=0;
	static Logger log = Logger.getLogger(PoweredRamStick.class.getName());
	
	public PoweredRamStick(FrequencyType frequency, BufferTypeType bufferType, RAMSizeType size, RAMTypeVendorType vendor,RAMTypeType type, double factor, VoltageType voltage){
		this.frequency = frequency;
		this.size = size;
		this.bufferType = bufferType;	
		this.type = type;
		this.factor = factor;
		this.voltage = voltage;	
		this.vendor = vendor;		
	}
	
	@Override
	public double computePower() {
		
		double idleRAMPower=0;
		double loadedRAMPower=0;
		
		if (this.frequency == null || this.frequency.getValue() <= 0){
			log.debug("RAM Frequency is Null or Negative or Zero");
			return 0.0;
			
		}else if (this.size == null || this.size.getValue() <= 0){
			log.debug("RAM Size is Null or Negative or Zero");
			return 0.0;
			
		}else if (this.bufferType == null){
				log.debug("RAM Buffer type is Null");
				return 0.0;
		}
		
		if(this.type == RAMTypeType.DDR_2){
			
			if(this.vendor != null && this.vendor == RAMTypeVendorType.KINGSTON){
				idleRAMPower = this.frequency.getValue()*0.001*this.size.getValue();
				loadedRAMPower = idleRAMPower + factor*beta;				
				
				if(this.bufferType== BufferTypeType.FULLY_BUFFERED){ 
					idleRAMPower = idleRAMPower*2.2;
					loadedRAMPower = idleRAMPower + factor*2.3*beta;				
				}
			
			} else if (this.vendor != null && this.vendor == RAMTypeVendorType.SAMSUNG){
				idleRAMPower = this.frequency.getValue()*0.001*0.95*this.size.getValue();
				loadedRAMPower = idleRAMPower + factor*beta;
				
				if(this.bufferType== BufferTypeType.FULLY_BUFFERED){
					idleRAMPower = idleRAMPower*4.26;
					loadedRAMPower = idleRAMPower + factor*2.3*beta;
				}
			} else if (this.vendor != null && this.vendor == RAMTypeVendorType.HYNIX){
				idleRAMPower = this.frequency.getValue()*0.001*1.9*this.size.getValue();
				loadedRAMPower = idleRAMPower + factor*beta;
				if(this.bufferType== BufferTypeType.FULLY_BUFFERED){
					idleRAMPower = idleRAMPower*1.65;
					loadedRAMPower = idleRAMPower + factor*2.3*beta;
				}
			}else{
				//if no vendor information is provided, then a rough approximation
				idleRAMPower = this.frequency.getValue()*0.001*1.425*this.size.getValue();
				loadedRAMPower = idleRAMPower + factor*beta;
				if(this.bufferType== BufferTypeType.FULLY_BUFFERED){
					idleRAMPower = idleRAMPower*2.7;
					loadedRAMPower = idleRAMPower + factor*2.3*beta;
				}
			}//end else			
		
		}else if(this.type == RAMTypeType.DDR_3){	

			if (this.voltage == null || this.voltage.getValue() <= 0){
				log.debug("RAM Voltage is Null or Negative or Zero");
				return 0.0;
			}
			idleRAMPower = this.size.getValue()*((this.frequency.getValue()*0.00013)*this.voltage.getValue()*this.voltage.getValue());
			loadedRAMPower = idleRAMPower + factor*1.3*beta;
			
			//A rough approximation on the fully buffered DDR3 RAMs
			if(this.bufferType== BufferTypeType.FULLY_BUFFERED){
				idleRAMPower = idleRAMPower*2.0;
				loadedRAMPower = idleRAMPower + factor*1.9*beta;
			}
		
		}else{
			//A rough approximation on the other types of RAMs
			idleRAMPower =2.7;
			loadedRAMPower = idleRAMPower + factor*beta;
		}
     
		return loadedRAMPower;	
		
	}//end of computePower method

}//end of class
