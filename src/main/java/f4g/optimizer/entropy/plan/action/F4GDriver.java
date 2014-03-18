
package f4g.optimizer.entropy.plan.action;

import java.util.List;

import javax.xml.bind.JAXBElement;

import btrplace.plan.event.Action;

import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.metamodel.FIT4GreenType;

/**
 * Adding energetic considerations to NodeComparator
 * @author Corentin Dupont
 */
public abstract class F4GDriver extends Driver {
			

	IController controller;
	protected FIT4GreenType model;
	NamingService nameService;

	/**
	 * Create and configure the driver to execute an action.
	 * @param a the action to execute
	 * @param props the properties to configure the driver
     * @throws PropertiesHelperException if an error occurred while configuring the driver
	 */
	public F4GDriver(Action a, IController controller, FIT4GreenType model, NamingService nameService) {
		super(a);
		this.controller = controller;
		this.model = model;
		this.nameService = nameService;
	}
	
	public F4GDriver(List<Action> a, IController myController, FIT4GreenType myModel, NamingService nameService) {
		//TODO fix
		super(a.get(0));
		this.controller = myController;
		this.model = myModel;
		this.nameService = nameService;
	}
		
	
	
	@Override
	public void execute(){
				
		ActionRequestType actionRequest = new ActionRequestType();
		ActionRequestType.ActionList actionList = new ActionRequestType.ActionList();
		
		AbstractBaseActionType action = getActionToExecute();
		
		JAXBElement<AbstractBaseActionType> JAXBAction = (new f4g.schemas.java.actions.ObjectFactory()).createAction(action);
			    	
		actionList.getAction().add(JAXBAction);
		
		actionRequest.setActionList(actionList);
		
		//TODO decorate actionRequest
		
		controller.executeActionList(actionRequest);
		
	}
	
	/**
	 * Get the command to execute on the remote host.
	 * @return a shell command
	 */
	public abstract AbstractBaseActionType getActionToExecute();
	
	@Override
	public String toString() {
		return "F4G(" + this.getAction().toString() + ")";
	}
}
