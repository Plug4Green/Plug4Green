/**
* ============================== Header ============================== 
* file:          PerformanceInformation.java
* project:       FIT4Green/CommunicatorEni
* created:       01/08/2011 by jclegea
* 
* $LastChangedDate: 2011-08-05 09:48:43 +0200 (vie, 05 ago 2011) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 672 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorEni.vmware;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public class PerformanceInformation {
	Double value_;
	String type_;
	String instance_;
	
	public PerformanceInformation(){
		value_ = 0.0;
		type_ = "";
		instance_ = "";		
	}
	
	public Double getValue(){
		return value_;
	}
	
	public String getType(){
		return type_;
	}
	
	public String getInstance(){
		return instance_;
	}
	
	public void setValue(Double value){
		value_ = value;
	}
	
	public void setType(String type){
		type_ = type;
	}
	
	public void setInstance(String instance){
		instance_ = instance;
	}
}
