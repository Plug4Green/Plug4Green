/**
* ============================== Header ============================== 
* file:          WebDataCollector.java
* project:       FIT4Green/Commons
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2011-02-25 14:50:54 +0100 (vr, 25 feb 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 606 $
* 
* short description:
*   Collects data to be retrieved by the web UI.
* ============================= /Header ==============================
*/
package f4g.commons.web;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import f4g.schemas.java.actions.AbstractBaseAction;

/**
 * Collects data to be retrieved by the web UI.
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 */
public class WebDataCollector implements IWebDataCollector{
	
	static Logger log = Logger.getLogger(WebDataCollector.class.getName());

	private static IWebDataCollector me = null;
	
	private ArrayList<JAXBElement<? extends AbstractBaseAction>> actionList = 
		new ArrayList<JAXBElement<? extends AbstractBaseAction>>();

	private boolean automatic;
	
	private String operatorName;
	
	private double computedPowerBefore;
	private double computedPowerAfter;
	
	private Date lastUpdateTimestamp;
	
	private boolean obsolete;
	
	private WebDataCollector() {
	}
	
	public static IWebDataCollector getInstance(){
		if(me == null){
			me = new WebDataCollector();
		}
		return me;
	}

	@Override
	public synchronized void addAction(JAXBElement<? extends AbstractBaseAction> elem) {
		actionList.add(elem);
		setLastUpdateTimestamp(new Date());			
	}

	@Override
	public synchronized void clearActions() {
		log.debug("Clearing list of actions");
		actionList.clear();		
		computedPowerBefore = 0.0;
		computedPowerAfter = 0.0;
		obsolete = false;
	}

	@Override
	public ArrayList<JAXBElement<? extends AbstractBaseAction>> getActionList() {
		log.debug("Returning list of actions");
		return actionList;
	}
	
	/**
	 * @return the automatic
	 */
	@Override
	public boolean isAutomatic() {
		return automatic;
	}

	/**
	 * @param automatic the automatic to set
	 */
	@Override
	public void setAutomatic(boolean automatic) {
		this.automatic = automatic;
	}

	/**
	 * @return the operatorName
	 */
	@Override
	public String getOperatorName() {
		return operatorName;
	}

	/**
	 * @param operatorName the operatorName to set
	 */
	@Override
	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	@Override
	public Date getLastUpdateTimestamp() {
		return lastUpdateTimestamp;
	}
	
	private void setLastUpdateTimestamp(Date lastUpdateTimestamp) {
		this.lastUpdateTimestamp = lastUpdateTimestamp;
	}

	/**
	 * @param computedPowerBefore the computedPowerBefore to set
	 */
	@Override
	public void setComputedPowerBefore(double computedPowerBefore) {
		this.computedPowerBefore = computedPowerBefore;
	}

	/**
	 * @return the computedPowerBefore
	 */
	@Override
	public double getComputedPowerBefore() {
		return computedPowerBefore;
	}

	/**
	 * @return the computedPowerAfter
	 */
	@Override
	public double getComputedPowerAfter() {
		return computedPowerAfter;
	}

	/**
	 * @param computedPowerAfter the computedPowerAfter to set
	 */
	@Override
	public void setComputedPowerAfter(double computedPowerAfter) {
		this.computedPowerAfter = computedPowerAfter;
	}

	public boolean isObsolete() {
		return obsolete;
	}

	public void setObsolete(boolean obsolete) {
		this.obsolete = obsolete;
	}

}
