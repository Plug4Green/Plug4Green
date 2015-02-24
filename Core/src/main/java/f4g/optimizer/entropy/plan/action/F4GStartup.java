
package f4g.optimizer.entropy.plan.action;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.event.BootNode;
import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.Server;


public class F4GStartup extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private BootNode action;

	
	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GStartup(BootNode a, IController myController, FIT4Green myModel, NamingService<Node> nodeNS, NamingService<VM> VMNS) {
		super(a, myController, myModel, nodeNS, VMNS);
		action = a;
	}
	

	@Override
	public AbstractBaseAction getActionToExecute() {

		PowerOnAction powerOn = new PowerOnAction();
				
		Server server = Utils.findServerByName(model, nodeNS.getName(action.getNode()));
		FrameworkCapabilities fc = (FrameworkCapabilities) server.getFrameworkRef();
		
		powerOn.setNodeName(nodeNS.getName(action.getNode()));
		powerOn.setFrameworkName(fc.getFrameworkName());
		
		return powerOn;
		
	}
}
