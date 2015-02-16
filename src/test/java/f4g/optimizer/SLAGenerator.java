/**
* ============================== Header ============================== 
* file:          SLAGenerator.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-04-23 17:07:38 +0200 (lun, 23 abr 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1364 $
* 
* short description:
*   Generates a SLA for test purpose.
* ============================= /Header ==============================
*/

package f4g.optimizer;


import java.util.Random;
import org.apache.log4j.Logger;


import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoad;
import f4g.schemas.java.constraints.optimizerconstraints.FIT4GreenOptimizerConstraint;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.IoRate;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.NetworkUsage;
import f4g.schemas.java.metamodel.NrOfCpus;
import f4g.schemas.java.metamodel.RAMSize;
import f4g.schemas.java.metamodel.StorageCapacity;


public class SLAGenerator {
	
	
	public Logger log;  

	
	public SLAGenerator() {
		
		log = Logger.getLogger(SLAGenerator.class.getName()); 

	}
	
	public static SLAType createDefaultSLA(){
		SLAType slas = new SLAType();
		SLAType.SLA sla = new SLAType.SLA();
		QoSConstraintsType qos = new QoSConstraintsType();
		sla.setQoSConstraints(qos);	
		HardwareConstraintsType hwm = new HardwareConstraintsType();
		sla.setHardwareConstraints(hwm);
		EnergyConstraintsType en = new EnergyConstraintsType();
		sla.setEnergyConstraints(en);
		slas.getSLA().add(sla);
		return slas;
	}
	

	
	
	/**
	 * create a VM
	 *
	 * @author cdupont
	 */
	public static VMTypeType createVirtualMachine(){
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpus(1), new RAMSize(0.125), new StorageCapacity(1)));
		type1.setExpectedLoad(new ExpectedLoad(new CpuUsage(50), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
		VMs.getVMType().add(type1);
		
		VMTypeType.VMType type2 = new VMTypeType.VMType();

		type2.setName("m1.medium");
		type2.setCapacity(new CapacityType(new NrOfCpus(2), new RAMSize(0.5), new StorageCapacity(6)));
		type2.setExpectedLoad(new ExpectedLoad(new CpuUsage(60), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
		VMs.getVMType().add(type2);
				
		VMTypeType.VMType type3 = new VMTypeType.VMType();

		type3.setName("m1.large");
		type3.setCapacity(new CapacityType(new NrOfCpus(18), new RAMSize(1), new StorageCapacity(12)));
		type3.setExpectedLoad(new ExpectedLoad(new CpuUsage(70), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
		VMs.getVMType().add(type3);
		
		VMTypeType.VMType type4 = new VMTypeType.VMType();

		type4.setName("m1.xlarge");
		type4.setCapacity(new CapacityType(new NrOfCpus(18), new RAMSize(1), new StorageCapacity(12)));
		type4.setExpectedLoad(new ExpectedLoad(new CpuUsage(80), new MemoryUsage(0), new IoRate(0), new NetworkUsage(0)));
		VMs.getVMType().add(type4);
		
		
		return VMs;
	}
	
	

	/**
	 * compute a random int value uniformly distributed between aStart (inclusive) and aEnd (inclusive)
	 *
	 * @author cdupont
	 */
	public int genRandomInteger(int aStart, int aEnd, Random aRandom)
	{
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range
	    int range = aEnd - aStart + 1;
	    // compute a fraction of the range, 0 <= frac < range
	    int fraction = aRandom.nextInt(range);

	    return (aStart + fraction);    
	}

	
	/**
	 * compute a random double value uniformly distributed between aStart (inclusive) and aEnd (exclusive)
	 *
	 * @author cdupont
	 */
	public double genRandomDouble(double aStart, double aEnd, Random aRandom)
	{
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range, casting to long to avoid overflow problems
	    double range = aEnd - aStart;
	    // compute a fraction of the range, 0 <= frac < range
	    double fraction = range * aRandom.nextDouble();
 
	    return fraction + aStart; 
	}
	


}



