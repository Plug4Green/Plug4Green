/**
* ============================== Header ============================== 
* file:          ControllerActionList.java
* project:       FIT4Green/Manager
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2012-06-21 16:44:15 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1500 $
* 
* short description:
*   Wrapper class for collecting actions
* ============================= /Header ==============================
*/
package org.f4g.controller;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Wrapper class for collecting actions
 * 
 * @author FIT4Green
 */
public class ControllerActionList {

	ArrayList<ControllerAction> actions = null;
	
	public ControllerActionList() {
		actions = new ArrayList<ControllerAction>();
	}
	
	public ControllerActionList(Collection<ControllerAction> c) {
		this.actions = new ArrayList<ControllerAction>(c);
	}
	
	public boolean add(ControllerAction action){
		return actions.add(action);
	}
	
	public boolean remove(ControllerAction action){
		return actions.remove(action);
	}
	
	public ArrayList<ControllerAction> getActions() {
		return actions;
	}
	
}
