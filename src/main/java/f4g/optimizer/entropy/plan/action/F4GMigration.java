
package f4g.optimizer.entropy.plan.action;


import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.plan.event.MigrateVM;
import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.VirtualMachineType;



public class F4GMigration extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private MigrateVM action;

	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GMigration(MigrateVM a, IController myController, FIT4GreenType myModel, NamingService<Node> nodeNS, NamingService<VM> VMNS) {
		super(a, myController, myModel, nodeNS, VMNS);
		action = a;
		
	}
	

	@Override
	public AbstractBaseActionType getActionToExecute() {


		VirtualMachineType VM = Utils.findVirtualMachineByName(Utils.getAllVMs(model), VMNS.getName(action.getVM()));
		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) VM.getFrameworkRef();
				
		Boolean internalMove = Utils.getServerDatacenterbyName(nodeNS.getName(action.getSourceNode()), model) == 
			                   Utils.getServerDatacenterbyName(nodeNS.getName(action.getDestinationNode()), model);
		
		AbstractBaseActionType moveVMAction;
		
		//Create a move VM action or a Live Migrate based on the framework capabilities
		if((internalMove && fc.getVm().isIntraLiveMigrate()) 
		|| !internalMove && fc.getVm().isInterLiveMigrate()) {
			moveVMAction = new LiveMigrateVMActionType();
			((LiveMigrateVMActionType)moveVMAction).setVirtualMachine(VMNS.getName(action.getVM()));
			((LiveMigrateVMActionType)moveVMAction).setSourceNodeController(nodeNS.getName(action.getSourceNode()));
			((LiveMigrateVMActionType)moveVMAction).setDestNodeController(nodeNS.getName(action.getDestinationNode()));
		} else {
			moveVMAction = new MoveVMActionType();
			((MoveVMActionType)moveVMAction).setVirtualMachine(VMNS.getName(action.getVM()));
			((MoveVMActionType)moveVMAction).setSourceNodeController(nodeNS.getName(action.getSourceNode()));
			((MoveVMActionType)moveVMAction).setDestNodeController(nodeNS.getName(action.getDestinationNode()));
		}
				
		moveVMAction.setFrameworkName(fc.getFrameworkName());
		moveVMAction.setID("");
		
		
		return moveVMAction;
		
	}
}
