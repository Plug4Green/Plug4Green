
package f4g.optimizer.entropy.plan.action;

import javax.xml.bind.JAXBElement;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.event.Action;

import f4g.commons.controller.IController;
import f4g.optimizer.entropy.NamingService;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.metamodel.FIT4Green;

/**
 * Adding energetic considerations to NodeComparator
 * @author Corentin Dupont
 */
public abstract class F4GDriver extends Driver {
			

	IController controller;
	protected FIT4Green model;
	NamingService<Node> nodeNS;
	NamingService<VM> VMNS;

	/**
	 * Create and configure the driver to execute an action.
	 * @param a the action to execute
	 * @param props the properties to configure the driver
     * @throws PropertiesHelperException if an error occurred while configuring the driver
	 */
	public F4GDriver(Action a, IController controller, FIT4Green model, NamingService<Node> nodeNS, NamingService<VM> VMNS) {
		super(a);
		this.controller = controller;
		this.model = model;
		this.nodeNS = nodeNS;
		this.VMNS = VMNS;
	}
	
	
	@Override
	public void execute(){
				
		ActionRequest actionRequest = new ActionRequest();
		ActionRequest.ActionList actionList = new ActionRequest.ActionList();
		
		AbstractBaseAction action = getActionToExecute();
		
		JAXBElement<AbstractBaseAction> JAXBAction = (new f4g.schemas.java.actions.ObjectFactory()).createAction(action);
			    	
		actionList.getAction().add(JAXBAction);
		
		actionRequest.setActionList(actionList);
		
		//TODO decorate actionRequest
		
		controller.executeActionList(actionRequest);
		
	}
	
	/**
	 * Get the command to execute on the remote host.
	 * @return a shell command
	 */
	public abstract AbstractBaseAction getActionToExecute();
	
	@Override
	public String toString() {
		return "F4G(" + this.getAction().toString() + ")";
	}
}
