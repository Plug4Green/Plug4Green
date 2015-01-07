
package f4g.optimizer.entropy.plan.action;


import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootNode;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.plan.event.ShutdownNode;
import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.schemas.java.metamodel.FIT4GreenType;

public class F4GDriverFactory {
	
	
	IController controller;
	FIT4GreenType model;
	NamingService<Node> nodeNames;
	NamingService<VM> vmNames;
	
	/**
	 * Create a new Factory.
	 * @param clusters 
	 * @param properties The properties used to create the factory
	 */
	public F4GDriverFactory(IController controller, FIT4GreenType model, NamingService<Node> nodeNames, NamingService<VM> vmNames) {
		this.controller = controller;
		this.model = model;
		this.nodeNames = nodeNames;
		this.vmNames = vmNames;
	}
		
	/**
	 * Transform an action to a driver to execute it.
	 * @param action the action to transform
	 * @return a driver to perform the action
	 * @throws DriverInstantiationException if an error occured during the transformation
	 */
	public F4GDriver transform(Action action) {
		
		if (action instanceof MigrateVM) {
			return new F4GMigration((MigrateVM) action, controller, model, nodeNames, vmNames);
		} else if (action instanceof BootNode) {
			return new F4GStartup((BootNode) action, controller, model, nodeNames, vmNames);
		} else if (action instanceof ShutdownNode) {
			return new F4GShutdown((ShutdownNode) action, controller, model, nodeNames, vmNames);		
		} else {
			return null;		
		} 
	}
}
