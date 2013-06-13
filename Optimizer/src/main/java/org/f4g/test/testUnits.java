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

import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;
import javax.measure.quantity.*;
import static javax.measure.unit.SI.*;

/**
 * example for physic units usage
 * 
 * @author cdupont
 */
//public class testUnits {
//
//
//	public static void main(String[] args) {
//	
//		//creating a new energy measure in Joules
//		Amount<Energy> energy =  Amount.valueOf(1, JOULE);
//		System.out.println("energy=" + energy.toString());
//		
//		//creating a new power measure in MW
//		Amount<Power> power =  Measure.valueOf(1, MEGA(WATT));
//		System.out.println("power=" + power.toString());
//						
//		//creating a new length measure in meters
//		Amount<Length> x = Amount.valueOf(1, METER);
//		System.out.println("x=" + x);
//		
//		//creating a new duration measure in s
//		Amount<Duration> t = Amount.valueOf(2, MICRO(SECOND));
//		System.out.println("t=" + t);
//	    
//		//creating a new velocity measure in m/s
//		//Amount<Velocity> v1 = Amount.valueOf(1000, METER_PER_SECOND);
//		//System.out.println("v1=" + v1);
//	    
//		//creating a new velocity out of x an t in Km/h
//		Amount<Velocity> v2 = (Amount<Velocity>) x.divide(t);
//		System.out.println("v2=" + v2.to( KILO(METER).divide(HOUR) ));  
//	    
//		//adding velocities with different units
//		Amount<Velocity> v3 = (Amount<Velocity>) v1.plus(v2);
//		//System.out.println("v3=" + v3.to( METER_PER_SECOND ));  
//	    	    
//	    //compute an energy from power and time
//		Amount <?> energy2 = power.times(t);
//	    System.out.println("energy2=" + energy2.to(JOULE));
//	    
//	    //ratio between 2 energies in percent
//	    Amount<Dimensionless> eRate = (Amount<Dimensionless>) energy.divide(energy2); 
//	    System.out.println("eRate=" + eRate.to(PERCENT));
//   
//	    //a data transfer rate in Bytes/Seconds
//	    Amount<?> dataTransfer = Amount.valueOf(1000, BYTE.divide(SECOND));
//	    System.out.println("dataTransfer=" + dataTransfer);
//	    
//	    //a frequency in mHz
//	    Amount<?> freq = Amount.valueOf(1000, MILLI(HERTZ));
//	    System.out.println("freq=" + freq);
//	    
//	    //creating a new length measure in foot
//	    Amount<Length> m = Amount.valueOf(33, FOOT).divide(11).times(2);
//	    System.out.println(m);
//	    System.out.println(m.isExact() ? "exact" : "inexact");
//	    System.out.println(m.getExactValue());
//
//	    //consumption of a car
//	    Amount<? extends Quantity> carMileage = Amount.valueOf(20, MILE.divide(GALLON_LIQUID_US));
//	    System.out.println("carMileage=" + carMileage);    
//        
//	    //expensive gaz
//	    Amount<?> gazPrice   = Amount.valueOf(1.2, EUR.divide(LITER)); // 1.2 €/L
//	    System.out.println("gazPrice=" + gazPrice);    
//       
//	    //adding a power and an energy:
//	    //produces a ConversionException at run time
//        //System.out.println( power.plus(energy) );
//       
//
//	}
//	
//}
