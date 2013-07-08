/**
* ============================== Header ============================== 
* file:          Memory.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-12-06 18:33:26 +0100 (Di, 06 Dez 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1147 $
* 
* short description:
*   auxiliary function to parse memory notation (kb..) 
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import org.apache.log4j.Logger;

/**
 * auxiliary function to parse memory notation (kb..) 
 *
 * @author Daniel Brinkers
 */
public class Memory {
	static Logger log = Logger.getLogger(Memory.class.getName());
	
	static long parse(String in){
		long mem;
		String str = in.substring(0, in.length()-2);
		mem = Long.decode(str);
		str = in.substring(in.length()-2);
		if(str.equals("eb"))
			mem <<= 50;
		else if(str.equals("tb"))
			mem <<= 40;
		else if(str.equals("gb"))
			mem <<= 30;
		else if(str.equals("mb"))
			mem <<= 20;
		else if(str.equals("kb"))
			mem <<= 10;
		log.trace("Set free memory to: " + mem);
		return mem;
	}
}
