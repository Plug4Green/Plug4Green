
package f4g.optimizer.btrplace.plan.action;


import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.event.MigrateVM;
import f4g.commons.controller.IController;
import f4g.optimizer.btrplace.NamingService;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.VirtualMachine;



public class F4GMigration extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private MigrateVM action;

	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws btrplace.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GMigration(MigrateVM a, IController myController, FIT4Green myModel, NamingService<Node> nodeNS, NamingService<VM> VMNS) {
		super(a, myController, myModel, nodeNS, VMNS);
		action = a;
		
	}
	

	@Override
	public AbstractBaseAction getActionToExecute() {


		VirtualMachine VM = Utils.findVirtualMachineByName(Utils.getAllVMs(model), VMNS.getName(action.getVM()));
		FrameworkCapabilities fc = (FrameworkCapabilities) VM.getFrameworkRef();
				
		Boolean internalMove = Utils.getServerDatacenterbyName(nodeNS.getName(action.getSourceNode()), model) == 
			                   Utils.getServerDatacenterbyName(nodeNS.getName(action.getDestinationNode()), model);
		
		AbstractBaseAction moveVMAction;
		
		//Create a move VM action or a Live Migrate based on the framework capabilities
		if((internalMove && fc.getVm().isIntraLiveMigrate()) 
		|| !internalMove && fc.getVm().isInterLiveMigrate()) {
			moveVMAction = new LiveMigrateVMAction();
			((LiveMigrateVMAction)moveVMAction).setVirtualMachine(VMNS.getName(action.getVM()));
			((LiveMigrateVMAction)moveVMAction).setSourceNodeController(nodeNS.getName(action.getSourceNode()));
			((LiveMigrateVMAction)moveVMAction).setDestNodeController(nodeNS.getName(action.getDestinationNode()));
		} else {
			moveVMAction = new MoveVMAction();
			((MoveVMAction)moveVMAction).setVirtualMachine(VMNS.getName(action.getVM()));
			((MoveVMAction)moveVMAction).setSourceNodeController(nodeNS.getName(action.getSourceNode()));
			((MoveVMAction)moveVMAction).setDestNodeController(nodeNS.getName(action.getDestinationNode()));
		}
				
		moveVMAction.setFrameworkName(fc.getFrameworkName());
		moveVMAction.setID("");
		
		
		return moveVMAction;
		
	}
}
