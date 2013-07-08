/**
* ============================== Header ============================== 
* file:          ActionServiceImpl.java
* project:       FIT4Green/F4gGui
* created:       14 dec 2010 by jos@almende.org
* 
* $LastChangedDate: 2011-09-07 16:08:44 +0200 (wo, 07 sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 716 $
* 
* short description:
*   Communicates with the FIT4Green plug-in.
* ============================= /Header ==============================
*/
package f4g.f4gGui.gui.server;

import org.apache.log4j.Logger;
import f4g.commons.core.IMain;
import f4g.pluginCore.core.Main;
import f4g.commons.controller.IController;
import f4g.commons.monitor.IMonitor;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.StartJobActionType;
import f4g.schemas.java.actions.StandByActionType;
import f4g.commons.web.IWebDataCollector;
import f4g.commons.web.WebDataCollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import javax.xml.bind.JAXBElement;

import f4g.f4gGui.gui.client.ActionService;
import f4g.f4gGui.gui.shared.ActionData;
import f4g.f4gGui.gui.shared.Status;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Communicates with the FIT4Green plug-in
 * Propagates requests to start and shutdown the plug-in and start global optimization
 * Continuously polling the actions suggested by the optimizer
 * 
 * @author Jos de Jong, Vasiliki Georgiadou
 */
public class ActionServiceImpl extends RemoteServiceServlet
	implements ActionService {
	
	static Logger log = Logger.getLogger(ActionServiceImpl.class.getName());

	private static final long serialVersionUID = 1L;

	@Override
	public String startup() throws Exception {
		String result = "";
		try {
			log.debug("Starting up the plug-in");
			IMain main = Main.getInstance();

			if (!main.isRunning()) {
				main.startup();
				result = "Startup request sent to plugin.";
			} else {
				result = "The plugin is already running.";
			}
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}
		
		return result;	
	}
	
	@Override
	public String shutdown() throws Exception {
		String result = "";
		try {
			log.debug("Shutting down the plug-in");
			IMain main = Main.getInstance();

			if (main.isRunning()) {
				main.shutdown();			
				result = "Shutdown request sent to plugin.";
			} else {
				result = "The plugin is already stopped.";
			}
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}
		
		return result;	
	}
	
	@Override
	public String optimize() throws Exception {
		String result = "";
		try {
			log.debug("Requesting global optimization");
			IMain main = Main.getInstance();

			if (main.isRunning()) {
				IMonitor monitor = main.getMonitor();
				monitor.requestGlobalOptimization();			
				result = "Optimization request sent. Optimization might be ongoing.";
			}
			else {
				throw new Exception("Plugin is not running. " +
						"First start the plugin, then choose optimize.");
			}
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}
		
		return result;	
	}
	
	@Override
	public String approveActions() throws Exception {
		String result = "";
		try {
			log.debug("Approving pending actions");
			IMain main = Main.getInstance();

			if (main.isRunning()) {
				IController controller = main.getController();	
				controller.setActionsApproved(true);
				controller.setApprovalSent(true);
				result = "Actions's approval has been sent to the plug-in; " +
						"pending actions are dispached to respective Coms";
			}
			else {
				throw new Exception("Plugin is not running.");
			}
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}		
		return result;
	}
	
	public String cancelActions() throws Exception {
		String result = "";
		try {
			log.debug("Cancelling pending actions");
			IMain main = Main.getInstance();

			if (main.isRunning()) {
				IController controller = main.getController();	
				controller.setActionsApproved(false);
				controller.setApprovalSent(true);
				result = "Pending actions are cancelled";
			}
			else {
				throw new Exception("Plugin is not running.");
			}
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}		
		return result;
	}

	@Override
	public Status getStatus() throws Exception {
		try {		
			log.debug("Getting status and power consumption");

			IMain main = Main.getInstance();

			Status status = new Status();
			status.isRunning = main.isRunning();
			status.statusMessage = main.getStatusMessage();
			status.powerConsumption = main.getComputedPower();
            status.error = main.errorExists();
            if(status.error){
            	status.errorMessage = main.getErrorMessage();
            }
            status.isObjectivePower = (main.getOptimizationObjective() 
            		== OptimizationObjective.Power);
			return status;
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}			
	}
	
	@Override
	public Vector<ActionData> getActionList() throws Exception {
		Vector<ActionData> result = new Vector<ActionData>();
		
		try {
			log.debug("Getting suggested actions");
			IWebDataCollector collector = WebDataCollector.getInstance();
			
			boolean automatic = collector.isAutomatic();
			String operatorName = collector.getOperatorName();
				
			ArrayList<JAXBElement<? extends AbstractBaseActionType>> actionList = collector.getActionList();
			Date lastUpdateTimestamp = collector.getLastUpdateTimestamp();
			
			boolean obsolete = collector.isObsolete();

			ActionData[] actions = new ActionData[actionList.size()];
			for(int i = 0; i < actionList.size(); i++){
				JAXBElement<? extends AbstractBaseActionType> elem = actionList.get(i);
				AbstractBaseActionType action = elem.getValue();
				
				String comName = action.getFrameworkName();
				
				// get action description from class name, and 
				// remove suffix "ActionType" from the string
				String actionString = action.getClass().getSimpleName();
				int suffix = actionString.indexOf("ActionType"); 
				if (suffix >= 0) {
					actionString = actionString.substring(0, suffix); 
				}

				// Retrieve parameters
				String nodeName = "";
				String srcNodeName = "";
				String dstNodeName = "";
				String virtualMachine = "";
				String jobID = "";
				
				if (action instanceof PowerOffActionType) {
					PowerOffActionType a = (PowerOffActionType)action;
					nodeName = a.getNodeName();
				} else if (action instanceof PowerOnActionType) {
					PowerOnActionType a = (PowerOnActionType)action;
					nodeName = a.getNodeName();
				} else if (action instanceof LiveMigrateVMActionType) {
					LiveMigrateVMActionType a= (LiveMigrateVMActionType)action;
					srcNodeName = a.getSourceNodeController();
					dstNodeName = a.getDestNodeController();
					virtualMachine = a.getVirtualMachine();
				} else if (action instanceof MoveVMActionType) {
					MoveVMActionType a= (MoveVMActionType)action;
					srcNodeName = a.getSourceNodeController();
					dstNodeName = a.getDestNodeController();
					virtualMachine = a.getVirtualMachine();
				} else if (action instanceof StartJobActionType) {
					StartJobActionType a = (StartJobActionType)action;
					jobID = a.getJobID();
				} else if (action instanceof StandByActionType) {
					StandByActionType a = (StandByActionType)action;
					nodeName = a.getNodeName();
				}	
				
				ActionData ad = new ActionData();
				ad.setAutomatic(automatic);
				ad.setOperatorName(operatorName);
				ad.setObsolete(obsolete);
				ad.setComName(comName);
				ad.setAction(actionString);
				ad.setLastUpdateTimestamp(lastUpdateTimestamp);
				ad.setNodeName(nodeName);
				ad.setDstNodeName(dstNodeName);
				ad.setSrcNodeName(srcNodeName);
				ad.setVirtualMachine(virtualMachine);
				ad.setJobID(jobID);
				actions[i] = ad;
			}

			result = new Vector<ActionData>(Arrays.asList(actions));
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}
		
		return result;
	}

	/**
	 * Calculate the power reduction for the last set of actions.
	 * The returned value is positive when the power after is lower than the
	 * power before.
	 * Returns reduction, equals to (powerBefore - powerAfter)
	 */
	@Override
	public Double getPowerReduction() throws Exception {
		Double reduction = 0.0;
		
		try {
			IWebDataCollector collector = WebDataCollector.getInstance();

			double powerBefore = collector.getComputedPowerBefore();
			double powerAfter = collector.getComputedPowerAfter();
			
			reduction = (powerBefore - powerAfter);
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}		
		
		return reduction;
	}

	@Override
	public void setOptimizationObjective(int option) throws Exception {
		try {		
			log.debug("Setting optimization objective");

			IMain main = Main.getInstance();
			
			if (option == 0) {  // power
				main.setOptimizationObjective(OptimizationObjective.Power);
			} else if (option == 1) {  // CO2
				main.setOptimizationObjective(OptimizationObjective.CO2);
			}
			
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}		
		
	}
}
