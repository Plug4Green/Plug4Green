/**
* ============================== Header ============================== 
* file:          Pair.java
* project:       FIT4Green/Optimizer
* created:       12 janv. 2011 by cdupont
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   a simple triple utility.
* ============================= /Header ==============================
*/
package org.f4g.optimizer.utils;

/**
 * a simple pair utility.
 * 
 *
 * @author cdupont
 */

public class Triple<F, S, T>
	{
	  public Triple(F f, S s, T t)
	  { 
	    first = f;
	    second = s;
	    third = t;
	    
	  }

	  public F getFirst()
	  {
	    return first;
	  }

	  public S getSecond() 
	  {
	    return second;
	  }
	  
	  public T getThird() 
	  {
	    return third;
	  }

	  public String toString()
	  { 
	    return "(" + first.toString() + ", " + second.toString() + ", " + third.toString() + ")"; 
	  }

	  private F first;
	  private S second;
	  private T third;
}

