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

package org.f4g.test;


import java.util.Random;
import org.apache.log4j.Logger;


import org.f4g.schema.constraints.optimizerconstraints.CapacityType;
import org.f4g.schema.constraints.optimizerconstraints.ExpectedLoadType;
import org.f4g.schema.constraints.optimizerconstraints.FIT4GreenOptimizerConstraint;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType;
import org.f4g.schema.constraints.optimizerconstraints.SLAType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.RAMSizeType;
import org.f4g.schema.metamodel.StorageCapacityType;


public class SLAGenerator {
	
	
	public Logger log;  

	
	public SLAGenerator() {
		
		log = Logger.getLogger(SLAGenerator.class.getName()); 

	}
	
	public static SLAType createDefaultSLA(){
		SLAType slas = new SLAType();
		SLAType.SLA sla = new SLAType.SLA();
		QoSDescriptionType qos = new QoSDescriptionType();
		sla.setCommonQoSRelatedMetrics(qos);		
		slas.getSLA().add(sla);
		return slas;
	}
	

	
	
	/**
	 * create a VM
	 *
	 * @author cdupont
	 */
	public static VMTypeType createVirtualMachineType(){
		
		VMTypeType VMs = new VMTypeType();
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("m1.small");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(0.125), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type1);
		
		VMTypeType.VMType type2 = new VMTypeType.VMType();

		type2.setName("m1.medium");
		type2.setCapacity(new CapacityType(new NrOfCpusType(2), new RAMSizeType(0.5), new StorageCapacityType(6)));
		type2.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(60), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type2);
				
		VMTypeType.VMType type3 = new VMTypeType.VMType();

		type3.setName("m1.large");
		type3.setCapacity(new CapacityType(new NrOfCpusType(18), new RAMSizeType(1), new StorageCapacityType(12)));
		type3.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(70), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
		VMs.getVMType().add(type3);
		
		VMTypeType.VMType type4 = new VMTypeType.VMType();

		type4.setName("m1.xlarge");
		type4.setCapacity(new CapacityType(new NrOfCpusType(18), new RAMSizeType(1), new StorageCapacityType(12)));
		type4.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(80), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
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



