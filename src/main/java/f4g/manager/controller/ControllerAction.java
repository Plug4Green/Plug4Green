/**
* ============================== Header ============================== 
* file:          ControllerAction.java
* project:       FIT4Green/Manager
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2012-06-21 16:44:15 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1500 $
* 
* short description:
*   Utility class for representing an action.
* ============================= /Header ==============================
*/
package f4g.manager.controller;

import java.util.HashMap;

/**
 * Utility class for representing an action.
 * 
 * @author FIT4Green
 */
public class ControllerAction {

	//The Com component target for the action
	private Object targetElement = null;
	
	//The action type
	private String actionType = null;
	
	//The action parameters (if any)
	private HashMap parameters = null;

	public ControllerAction(Object targetElement, String actionType) {
		
		this.targetElement = targetElement;
		this.actionType = actionType;
	}
	
	public ControllerAction(Object targetElement, String actionType, HashMap parameters) {
		this(targetElement, actionType);
		this.parameters = parameters;
	}
	
	
	public Object getTargetElement() {
		return targetElement;
	}

	public void setTargetElement(Object targetElement) {
		this.targetElement = targetElement;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public HashMap getParameters() {
		return parameters;
	}

	public void setParameters(HashMap parameters) {
		this.parameters = parameters;
	}
	
}
