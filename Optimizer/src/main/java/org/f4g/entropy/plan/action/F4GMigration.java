
package org.f4g.entropy.plan.action;


import org.f4g.controller.IController;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.VirtualMachineType;

import entropy.plan.action.Migration;


public class F4GMigration extends F4GDriver {


	/**
	 * The action to execute.
	 */
	private Migration action;

	/**
	 * Create and configure the driver to execute a migration action.
	 * @param a the action to execute
	 * @param properties the properties to configure the driver
     * @throws entropy.PropertiesHelperException if an error occurred while configuring the driver
     *
	 */
	public F4GMigration(Migration a, IController myController, FIT4GreenType myModel) {
		super(a, myController, myModel);
		action = a;
		
	}
	

	@Override
	public AbstractBaseActionType getActionToExecute() {

		VirtualMachineType VM = Utils.findVirtualMachineByName(Utils.getAllVMs(model), action.getVirtualMachine().getName());
		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) VM.getFrameworkRef();
				
		Boolean internalMove = Utils.getServerDatacenterbyName(action.getHost().getName(), model) == 
			                   Utils.getServerDatacenterbyName(action.getDestination().getName(), model);
		
		AbstractBaseActionType moveVMAction;
		
		//Create a move VM action or a Live Migrate based on the framework capabilities
		if((internalMove && fc.getVm().isIntraLiveMigrate()) 
		|| !internalMove && fc.getVm().isInterLiveMigrate()) {
			moveVMAction = new LiveMigrateVMActionType();
			((LiveMigrateVMActionType)moveVMAction).setVirtualMachine(action.getVirtualMachine().getName());
			((LiveMigrateVMActionType)moveVMAction).setSourceNodeController(action.getHost().getName());
			((LiveMigrateVMActionType)moveVMAction).setDestNodeController(action.getDestination().getName());
		} else {
			moveVMAction = new MoveVMActionType();
			((MoveVMActionType)moveVMAction).setVirtualMachine(action.getVirtualMachine().getName());
			((MoveVMActionType)moveVMAction).setSourceNodeController(action.getHost().getName());
			((MoveVMActionType)moveVMAction).setDestNodeController(action.getDestination().getName());
		}
				
		moveVMAction.setFrameworkName(fc.getFrameworkName());
		moveVMAction.setID("");
		
		
		return moveVMAction;
		
	}
}
