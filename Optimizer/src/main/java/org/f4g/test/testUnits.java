/**
* ============================== Header ============================== 
* file:          testUnits.java
* project:       FIT4Green/Optimizer
* created:       3 janv. 2011 by cdupont
* last modified: $LastChangedDate: 2010-11-26 11:33:26 +0100 (ven. 26 nov. 2010) $ by $LastChangedBy: corentin.dupont@create-net.org $
* revision:      $LastChangedRevision: 150 $
* 
* short description:
*   example for physic units usage
* ============================= /Header ==============================
*/
package org.f4g.test;


import javax.measure.quantities.*;
import static javax.measure.units.NonSI.*;
import static javax.measure.units.SI.*;

import org.jscience.physics.measures.Measure;
import static org.jscience.economics.money.Currency.*;

/**
 * example for physic units usage
 * 
 * @author cdupont
 */
public class testUnits {


	public static void main(String[] args) {
	
		//creating a new energy measure in Joules
		Measure<Energy> energy =  Measure.valueOf(1, JOULE);
		System.out.println("energy=" + energy.toString());
		
		//creating a new power measure in MW
		Measure<Power> power =  Measure.valueOf(1, MEGA(WATT));
		System.out.println("power=" + power.toString());
						
		//creating a new length measure in meters
		Measure<Length> x = Measure.valueOf(1, METER);
		System.out.println("x=" + x);
		
		//creating a new duration measure in s
		Measure<Duration> t = Measure.valueOf(2, MICRO(SECOND));
		System.out.println("t=" + t);
	    
		//creating a new velocity measure in m/s
		Measure<Velocity> v1 = Measure.valueOf(1000, METER_PER_SECOND);
		System.out.println("v1=" + v1);
	    
		//creating a new velocity out of x an t in Km/h
		Measure<Velocity> v2 = (Measure<Velocity>) x.divide(t);
		System.out.println("v2=" + v2.to( KILO(METER).divide(HOUR) ));  
	    
		//adding velocities with different units
		Measure<Velocity> v3 = (Measure<Velocity>) v1.plus(v2);
		System.out.println("v3=" + v3.to( METER_PER_SECOND ));  
	    	    
	    //compute an energy from power and time
	    Measure <?> energy2 = power.times(t);
	    System.out.println("energy2=" + energy2.to(JOULE));
	    
	    //ratio between 2 energies in percent
	    Measure<Dimensionless> eRate = (Measure<Dimensionless>) energy.divide(energy2); 
	    System.out.println("eRate=" + eRate.to(PERCENT));
   
	    //a data transfer rate in Bytes/Seconds
	    Measure<?> dataTransfer = Measure.valueOf(1000, BYTE.divide(SECOND));
	    System.out.println("dataTransfer=" + dataTransfer);
	    
	    //a frequency in mHz
	    Measure<?> freq = Measure.valueOf(1000, MILLI(HERTZ));
	    System.out.println("freq=" + freq);
	    
	    //creating a new length measure in foot
	    Measure<Length> m = Measure.valueOf(33, FOOT).divide(11).times(2);
	    System.out.println(m);
	    System.out.println(m.isExact() ? "exact" : "inexact");
	    System.out.println(m.getExactValue());

	    //consumption of a car
	    Measure<? extends Quantity> carMileage = Measure.valueOf(20, MILE.divide(GALLON_LIQUID_US));
	    System.out.println("carMileage=" + carMileage);    
        
	    //expensive gaz
	    Measure<?> gazPrice   = Measure.valueOf(1.2, EUR.divide(LITER)); // 1.2 €/L
	    System.out.println("gazPrice=" + gazPrice);    
       
	    //adding a power and an energy:
	    //produces a ConversionException at run time
        //System.out.println( power.plus(energy) );
       

	}
	
}
