/**
* ============================== Header ============================== 
* file:          Controller.java
* project:       FIT4Green/Manager
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2012-06-21 16:44:15 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1500 $
* 
* short description:
*   Implements the Controller component for the FIT4Green framework.
* ============================= /Header ==============================
*/
package org.f4g.controller;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;
import org.f4g.com.ICom;
import org.f4g.core.Configuration;
import org.f4g.core.Constants;
import org.f4g.core.IMain;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.web.IWebDataCollector;
import org.f4g.web.WebDataCollector;
import org.f4g.couchDB.ConvertToJSON;
import org.f4g.couchDB.DataBase;

/**
 * The purpose of the Controller is to:
 * <li> handle a request for a set of actions, wrapped into an ActionRequestType object </li>
 * <li> group them based on the COM components which are the target receivers </li>
 * <li> define the best strategies for the sending of the requests </li>
 * <li> dispatch the request in the optimal way to the COM objects </li>
 * </ul> 
 * 
 * The Controller also creates the actions database and updates it each time new actions are 
 * suggested by the Optimizer.
 * The actions are stored once, after they are processed by the COMs (successfully or not)
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 */
public class Controller implements IController {
	
	static Logger log = Logger.getLogger(Controller.class.getName());

	IMain main = null;
	IWebDataCollector webDataCollector = null;
	
	private int op;
	
	private Timer approvalTimer = null;
	
	private boolean actionsApproved;
	private boolean approvalSent;
	
	private int maxSize;
	
	private final String DESIGN_ID = "_design/controller_utils";
	private final String DESIGN_NAME = "controller_utils";
	private final String SORT_BY_DATETIME_VIEW = "sortByDatetime";
	
	public Controller(IMain main) {
		
		this.main = main;
		this.webDataCollector = WebDataCollector.getInstance();
		
		// Set operation mode from configuration file
		String operationMode = Configuration.get(Constants.OPERATION_MODE);
		this.op = Integer.parseInt(operationMode);
		
		// Initialize flags related to actions approval
		this.actionsApproved = false;
		this.approvalSent = false;
		
		// read maximum database size from configuration file in GB
		String maxSizeString = Configuration.get(Constants.MAX_SIZE);
		// 1 gigabyte = 1 073 741 824 bytes
		// here 1 000 000 000 is used just to include some buffer
		maxSize = Integer.parseInt(maxSizeString) * 1000000000;
		
		// Create actions database if it does not already exist
		DataBase db = new DataBase();
		db.setUrl(Configuration.get(Constants.DB_URL));
		
		log.debug("About to create database " + Configuration.get(Constants.ACTIONS_DB_NAME));
		
		int rc = db.create(Configuration.get(Constants.ACTIONS_DB_NAME));
		
		if (rc == 412) {
			log.debug("Database \"" + Configuration.get(Constants.ACTIONS_DB_NAME) + "\" already exists");
		} else if ( rc == 201 ) {
			log.debug("Database \"" +	Configuration.get(Constants.ACTIONS_DB_NAME) + "\" created");
		} else {
			log.error("Error while creating database \"" + Configuration.get(Constants.ACTIONS_DB_NAME) + "\"; " + db.getMessage());
		}
		
		// create design document containing controller's utility views
		if (rc == 201 || rc == 412) {
			String body = createDesignBody();
			db.createDocument(Configuration.get(Constants.ACTIONS_DB_NAME), DESIGN_ID, body);
		} else {
			log.error("Error while creating design document \"" + DESIGN_ID + "\"; " + db.getMessage());
		}
		
	}
	
	public boolean isActionsApproved() {
		return actionsApproved;
	}

	@Override
	public void setActionsApproved(boolean actionsApproved) {
		this.actionsApproved = actionsApproved;
	}
	
	public boolean isApprovalSent() {
		return approvalSent;
	}

	@Override
	public void setApprovalSent(boolean approvalSent) {
		this.approvalSent = approvalSent;
	}
	
	

	/**
	 * Handles a set of requests and dispatches them to the responsible COM 
	 * components in an optimal way.
	 * 
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean executeActionList(ActionRequestType actionRequest) {
		
		// Cancel previous timer if still running
		if (this.approvalTimer != null) {
			this.approvalTimer.cancel();
			setApprovalSent(false);
			setActionsApproved(false);
			log.debug("Cancelling previous approval timer...");
		}

		// clearing actions; setting ICT power before and after to 0; setting obsolete to false
		webDataCollector.clearActions();

		// Check whether Optimizer has suggested any actions
		if (actionRequest.getActionList().getAction().size() == 0 ) {
			log.debug("No actions suggested by the Optimizer");
		} else {
		
			// Check operation mode; set "automatic" flag
			if (this.op == 1) {
				actionRequest.setIsAutomatic(true);
				webDataCollector.setAutomatic(true);
			} else {
				actionRequest.setIsAutomatic(false);
				webDataCollector.setAutomatic(false);
			}
		
			// TODO (phase 3) Set data centre operator name from configuration file
			actionRequest.setOperatorName("unknown");
			webDataCollector.setOperatorName("unknown");
		
			// Extract computed ICT power before and after and set web data collector
			if (actionRequest.getComputedPowerAfter() != null 
					|| actionRequest.getComputedPowerBefore() != null) {
				webDataCollector.setComputedPowerBefore(actionRequest.getComputedPowerBefore().getValue());
				webDataCollector.setComputedPowerAfter(actionRequest.getComputedPowerAfter().getValue());
			} else {
				log.warn("Computed ICT power after and before have not been set by the Optimizer");
				webDataCollector.setComputedPowerBefore(0.0);
				webDataCollector.setComputedPowerAfter(0.0);
			}
			
			// Set unique IDs per action
			for (int i = 0; i < actionRequest.getActionList().getAction().size(); i++) {
				String value = UUID.randomUUID().toString();
				actionRequest.getActionList().getAction().get(i).getValue().setID(value);
			}
			
			//Group actions by target Com
			log.debug("Grouping actions by Com component...");

			ActionRequestType.ActionList actionList = actionRequest.getActionList();

			HashMap groupedActions = new HashMap();
			
			for (int i = 0; i < actionList.getAction().size(); i++) {		
				String frameworkName = actionList.getAction().get(i).getValue().getFrameworkName();
			
				if(groupedActions.get(frameworkName) == null){
					groupedActions.put(frameworkName, new ArrayList());
				}
			
				((ArrayList)groupedActions.get(frameworkName)).add(actionList.getAction().get(i));
				log.debug("Adding " + frameworkName + " - " 
						+ actionList.getAction().get(i).getClass().getName() + " to Web Component");
				webDataCollector.addAction(actionList.getAction().get(i));
			}
			
			Iterator iter = groupedActions.keySet().iterator();
			log.debug("Dispatching actions to the responsible Com components...");
		
			while(iter.hasNext()){
				String comName = (String)iter.next();
				ArrayList actions = (ArrayList)groupedActions.get(comName);
			
				// check operation mode
				if (this.op == 1) {			// fully-automatic
					dispatchActions(comName, actions, actionRequest);
					webDataCollector.setObsolete(true);
				} else if (this.op == 2) {	// semi-automatic
					log.info("Actions pending for approval...");
					scheduleApprovalTask(comName, actions, actionRequest);
				} else if (this.op == 3) {	// "what if" analysis
					// Store suggested actions to a new document in the action database
					createActionsDocument(actionRequest);
					log.info("Silent mode; no actions dispatched to Com " + comName);
				} else {
					log.error("Invalid operation mode; no actions dispacted to Coms");
				}
			} // while next
		} // if any actions
		return true;
	}

	/**
	 * Dispatches a set of actions to the responsible Com components
	 * 
	 * @param comName the target Com component
	 * @param actions the set of actions
	 */
	private void dispatchActions(String comName, ArrayList actions, ActionRequestType actionRequest){
		
		log.debug("Dispatching " + actions.size() + " actions to Com component " + comName);
		
		ICom com = main.getComByName(comName);
		
		if(com != null){
			boolean res = com.executeActionList(actions);
			if (res) {
				// Store status of actions after handled by the Com
				ActionRequestType.ActionList updatedActions = new ActionRequestType.ActionList(actions);
				actionRequest.setActionList(updatedActions);
			}
		} else {
			log.error("Com object not found. Name: " + comName);
		}
		createActionsDocument(actionRequest);
		
	}
	
	@Override
	public boolean dispose() {
		if (this.approvalTimer != null) {
			this.approvalTimer.cancel();
		}
		return true;
	}
	
	private void createActionsDocument (ActionRequestType actionRequest) {
		
		DataBase db = new DataBase();
		db.setUrl(Configuration.get(Constants.DB_URL));
		
		String id = UUID.randomUUID().toString();
		
		try {
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			actionRequest.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
		} catch (DatatypeConfigurationException e) {
			log.error(e);
		}
		
		ConvertToJSON con = new ConvertToJSON();
		String data = con.convert(actionRequest);
		
		// check database current size and delete oldest document if applicable
		db.retrieveProperties(Configuration.get(Constants.ACTIONS_DB_NAME));
		
		if (db.getProperties().getDiskSize() >= maxSize ) {
			
			log.debug("Database \"" + Configuration.get(Constants.ACTIONS_DB_NAME) + "\" exceeds allowed maximum size...");

			String response = db.query(Configuration.get(Constants.ACTIONS_DB_NAME), 
					DESIGN_NAME, SORT_BY_DATETIME_VIEW, "limit=1");
			
			String delDocID = "";
			String rev = "";
			String[] tokens = response.split("[{}:\",]+");
			
			for (int i=0; i<tokens.length; i++) {
				if (tokens[i].compareTo("id") == 0) {
					delDocID = tokens[i+1];
				} else if (tokens[i].compareTo("value") == 0) {
					rev = tokens[i+1];
				}
			}
			
			log.debug("About to delete oldest document: \"" + delDocID + "\"...");
			db.deleteDocument(Configuration.get(Constants.ACTIONS_DB_NAME), delDocID, rev);
			
			log.debug("Running compaction on database \"" + Configuration.get(Constants.ACTIONS_DB_NAME) + "\"...");
			db.compact(Configuration.get(Constants.ACTIONS_DB_NAME));
			
		}

		log.debug("About to create document with id " + id + " at \"" 
				+ Configuration.get(Constants.ACTIONS_DB_NAME) + "\" database");
		
		int rc = db.createDocument(Configuration.get(Constants.ACTIONS_DB_NAME), id, data);
		
		if (rc == 201) {
			log.debug("Document " + id + " created");
		} else {
			log.error("Error while creating document " + id + "; " + db.getMessage());
		}
		
	}
	
	private String createDesignBody () {
		
		String map = 		
			"function(doc) {" + "\n" +  
			"  if (doc.Datetime) {" + "\n" + 
			"    emit(doc.Datetime,doc._rev);" + "\n" +  
			"  }" + "\n" + 
			"}";
				
		String body = 		
			"{" + "\n" +
			"   \"language\": \"javascript\"," + "\n" +
			"   \"views\": {" + "\n" +
			"      \"" + SORT_BY_DATETIME_VIEW + "\": {" + "\n" +
			"         \"map\": \"" + map + "\"" + "\n" +
			"      }" + "\n" +
			"   }" + "\n" +
			"}";
		
		return body;
	}
	
	/**
	 * Schedules task for checking whether actions are approved or not
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private void scheduleApprovalTask(String comName, ArrayList actions, ActionRequestType actionRequest) {
		
		// x (min) * 60 (sec) * 1000 (millisec)
		long period = 5000; 
		long delay = 0;
		
		this.approvalTimer = new Timer();
		this.approvalTimer.schedule(new ApprovalTask(comName,actions,actionRequest), delay, period);
			
	}
	
	/**
	 * Implements a task dedicated to retrieve approval of latest set of actions
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private class ApprovalTask extends TimerTask {
		
		// TODO (phase 3) Set time out from configuration file?
		// x (min) * 60 (sec) * 1000 (millisec)
		private final long TIME_OUT = 3*60000;
		
		private long startTime;
		private String comName;
		private ArrayList actions;
		private ActionRequestType actionRequest;
		
		/**
		 * @param comName
		 * @param actions
		 */
		public ApprovalTask(String comName, ArrayList actions,ActionRequestType actionRequest) {
			this.comName = comName;
			this.actions = actions;
			this.actionRequest = actionRequest;
			this.startTime = System.currentTimeMillis();
		}
		
		/**
		* Implements TimerTask abstract run method
		*/
		@Override
		public void run(){
			
			if (isApprovalSent()) {
				if (isActionsApproved()) {
					log.debug("Actions approved!");
					dispatchActions(this.comName,this.actions,this.actionRequest);
				} else {
					log.debug("Actions were not approved!");
					createActionsDocument(actionRequest);
				}
				setApprovalSent(false);
				setActionsApproved(false);
				webDataCollector.setObsolete(true);
				cancel();
			} else {
				log.debug("Approval pending...");
			}	
			
			if ((System.currentTimeMillis() - this.startTime) > this.TIME_OUT) {
				log.info("Actions approval time-out!");
				setApprovalSent(false);
				setActionsApproved(false);
				webDataCollector.setObsolete(true);
				cancel();
			}
			
		}
	}
	
}
