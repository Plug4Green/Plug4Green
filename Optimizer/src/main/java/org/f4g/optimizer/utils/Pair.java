/**
* ============================== Header ============================== 
* file:          Pair.java
* project:       FIT4Green/Optimizer
* created:       12 janv. 2011 by cdupont
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   a simple pair utility.
* ============================= /Header ==============================
*/
package org.f4g.optimizer.utils;

/**
 * a simple pair utility.
 * 
 *
 * @author cdupont
 */

public class Pair<T, S>
	{
	  public Pair(T f, S s)
	  { 
	    first = f;
	    second = s;   
	  }

	  public T getFirst()
	  {
	    return first;
	  }

	  public S getSecond() 
	  {
	    return second;
	  }

	  public String toString()
	  { 
	    return "(" + first.toString() + ", " + second.toString() + ")"; 
	  }

	  private T first;
	  private S second;
}

