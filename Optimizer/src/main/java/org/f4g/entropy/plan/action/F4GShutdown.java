
package org.f4g.entropy.plan.action;

import org.f4g.controller.IController;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.ServerType;

import entropy.plan.action.Shutdown;


public class F4GShutdown extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private Shutdown action;
	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GShutdown(Shutdown a, IController myController, FIT4GreenType myModel) {
		super(a, myController, myModel);
		action = a;
	}
	

	@Override
	public AbstractBaseActionType getActionToExecute() {

		PowerOffActionType powerOff = new PowerOffActionType();
		ServerType server = Utils.findServerByName(model,  action.getNode().getName());
		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) server.getFrameworkRef();
		
		powerOff.setNodeName(action.getNode().getName());
		powerOff.setFrameworkName(fc.getFrameworkName());

		return powerOff;
		
	}
}
