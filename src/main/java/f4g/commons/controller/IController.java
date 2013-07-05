package org.f4g.controller;

import org.f4g.schema.actions.ActionRequestType;

/**
 * Interface to be implemented by the Controller component.
 * 
 * The purpose of the Controller is to:
 * <li> handle a request for a set of actions wrapped into an ActionRequestType object </li>
 * <li> group them based on the COMcomponents which are the target receivers </li>
 * <li> define the best strategies for the sending of the requests </li>
 * <li> dispatch the request in the optimal way to the COM objects </li>
 * </ul>
 * 
 * The Controller also creates the actions database and updates it each time new actions are 
 * suggested by the Optimizer; it also exposes the method to store actions (used by COMs)
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 *
 */
public interface IController {


	/**
	 * Handles a set of requests and dispatches them to the responsible COM components
	 * in an optimal way.
	 * 
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */
	boolean executeActionList(ActionRequestType actionRequest);
	
	/**
	 * Called by the core component responsible for starting up and shutting down the FIT4Green plug-in. 
	 * It must implement all the operations needed to dispose the component in a clean way (e.g. 
	 * stopping dependent threads, closing connections, sockets, file handlers, etc.)
	 * 
	 * @return true if successful, false otherwise
	 */
	boolean dispose();
	
	/**
	 * Indicate whether actions were approved by the data centre operator
	 * 
	 * @param actionsApproved
	 */
	void setActionsApproved(boolean actionsApproved);
	
	/**
	 * Indicate whether data centre operator answer has been sent
	 * 
	 * @param approvalSent
	 */
	void setApprovalSent(boolean approvalSent);

}
