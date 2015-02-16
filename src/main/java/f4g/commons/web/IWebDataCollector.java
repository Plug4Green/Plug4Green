/**
* ============================== Header ============================== 
* file:          IWebDataCollector.java
* project:       FIT4Green/Commons
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2011-02-25 14:50:54 +0100 (vr, 25 feb 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 606 $
* 
* short description:
*   Interface for the web data collector.
* ============================= /Header ==============================
*/
package f4g.commons.web;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.JAXBElement;

import f4g.schemas.java.actions.AbstractBaseAction;

/**
 * Interface for the web data collector.
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 */
public interface IWebDataCollector {

	public ArrayList<JAXBElement<?extends AbstractBaseAction>> getActionList();
	public void addAction(JAXBElement<?extends AbstractBaseAction> elem);
	public void clearActions();
	
	public boolean isAutomatic();
	public void setAutomatic(boolean automatic);
	
	public String getOperatorName();
	public void setOperatorName(String operatorName);
	
	public Date getLastUpdateTimestamp();
	
	public void setComputedPowerBefore(double computedPowerBefore);
	public double getComputedPowerBefore();
	public void setComputedPowerAfter(double computedPowerAfter);
	public double getComputedPowerAfter();
	
	public boolean isObsolete();
	public void setObsolete(boolean obsolete);
	
}
