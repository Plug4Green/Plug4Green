
package org.f4g.entropy.plan.action;

import org.f4g.controller.IController;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.ServerType;

import entropy.plan.action.Startup;


public class F4GStartup extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private Startup action;

	
	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GStartup(Startup a, IController myController, FIT4GreenType myModel) {
		super(a, myController, myModel);
		action = a;
	}
	

	@Override
	public AbstractBaseActionType getActionToExecute() {

		PowerOnActionType powerOn = new PowerOnActionType();
				
		ServerType server = Utils.findServerByName(model,  action.getNode().getName());
		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) server.getFrameworkRef();
		
		powerOn.setNodeName(action.getNode().getName());
		powerOn.setFrameworkName(fc.getFrameworkName());
		
		return powerOn;
		
	}
}
