
package f4g.optimizer.entropy.plan.action;


import btrplace.plan.event.Action;
import btrplace.plan.event.BootNode;
import btrplace.plan.event.MigrateVM;
import btrplace.plan.event.ShutdownNode;
import f4g.commons.controller.IController;
import f4g.schemas.java.metamodel.FIT4GreenType;

public class F4GDriverFactory {
	
	
	IController controller;
	
	FIT4GreenType model;
	
	/**
	 * Create a new Factory.
	 * @param clusters 
	 * @param properties The properties used to create the factory
	 */
	public F4GDriverFactory(IController myController, FIT4GreenType myModel) {
		controller = myController;
		model = myModel;
	}
		
	/**
	 * Transform an action to a driver to execute it.
	 * @param action the action to transform
	 * @return a driver to perform the action
	 * @throws DriverInstantiationException if an error occured during the transformation
	 */
	public F4GDriver transform(Action action) {
		
		if (action instanceof MigrateVM) {
			return new F4GMigration((MigrateVM) action, controller, model);
		} else if (action instanceof BootNode) {
			return new F4GStartup((BootNode) action, controller, model);
		} else if (action instanceof ShutdownNode) {
			return new F4GShutdown((ShutdownNode) action, controller, model);		
		} else {
			return null;		
		} 
	}
}
