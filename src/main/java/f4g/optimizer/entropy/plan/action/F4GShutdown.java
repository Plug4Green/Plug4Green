
package f4g.optimizer.entropy.plan.action;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.event.ShutdownNode;
import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.ServerType;

public class F4GShutdown extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private ShutdownNode action;
	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GShutdown(ShutdownNode a, IController myController, FIT4GreenType myModel, NamingService<Node> nodeNS, NamingService<VM> VMNS) {
		super(a, myController, myModel, nodeNS, VMNS);
		action = a;
	}
	

	@Override
	public AbstractBaseActionType getActionToExecute() {

		PowerOffActionType powerOff = new PowerOffActionType();
		ServerType server = Utils.findServerByName(model, nodeNS.getName(action.getNode()));
		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) server.getFrameworkRef();
		
		powerOff.setNodeName(nodeNS.getName(action.getNode()));
		powerOff.setFrameworkName(fc.getFrameworkName());

		return powerOff;
		
	}
}
