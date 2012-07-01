
package org.f4g.entropy.plan.action;

import java.util.List;

import entropy.PropertiesHelperException;
import entropy.execution.driver.Driver;
import entropy.execution.driver.DriverException;
import entropy.plan.action.Action;

import javax.xml.bind.JAXBElement;

import org.f4g.controller.IController;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.metamodel.FIT4GreenType;



public abstract class F4GDriver extends Driver {
			

	IController controller;
	protected FIT4GreenType model;
	

	/**
	 * Create and configure the driver to execute an action.
	 * @param a the action to execute
	 * @param props the properties to configure the driver
     * @throws PropertiesHelperException if an error occurred while configuring the driver
	 */
	public F4GDriver(Action a, IController myController, FIT4GreenType myModel) {
		super(a);
		controller = myController;
		model = myModel;
	}
	
	public F4GDriver(List<Action> a, IController myController, FIT4GreenType myModel) {
		//TODO fix
		super(a.get(0));
		controller = myController;
		model = myModel;

	}
		
	
	
	@Override
	public void execute() throws DriverException {
				
		ActionRequestType actionRequest = new ActionRequestType();
		ActionRequestType.ActionList actionList = new ActionRequestType.ActionList();
		
		AbstractBaseActionType action = getActionToExecute();
		
		JAXBElement<AbstractBaseActionType> JAXBAction = (new org.f4g.schema.actions.ObjectFactory()).createAction(action);
			    	
		actionList.getAction().add(JAXBAction);
		
		actionRequest.setActionList(actionList);
		
		//TODO decorate actionRequest
		
		controller.executeActionList(actionRequest);
		
	}
	
	/**
	 * Get the command to execute on the remote host.
	 * @return a shell command
	 */
	public abstract AbstractBaseActionType getActionToExecute();
	
	@Override
	public String toString() {
		return "F4G(" + this.getAction().toString() + ")";
	}
}
