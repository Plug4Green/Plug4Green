
package org.f4g.entropy.plan.action;


import org.f4g.controller.IController;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.metamodel.FIT4GreenType;

import entropy.execution.driver.DriverInstantiationException;
import entropy.plan.action.*;
import entropy.plan.action.Shutdown;


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
	public F4GDriver transform(Action action) throws DriverInstantiationException {
		
		if (action instanceof Migration) {
			return new F4GMigration((Migration) action, controller, model);
		} else if (action instanceof Startup) {
			return new F4GStartup((Startup) action, controller, model);
		} else if (action instanceof Shutdown) {
			return new F4GShutdown((Shutdown) action, controller, model);		
		} else if (action instanceof Run) {
			return null;		
		} 
		throw new DriverInstantiationException(action);
	}
}
