
package f4g.optimizer.entropy.plan.action;

import f4g.commons.controller.IController;
import f4g.commons.optimizer.utils.Utils;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.ServerType;

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
