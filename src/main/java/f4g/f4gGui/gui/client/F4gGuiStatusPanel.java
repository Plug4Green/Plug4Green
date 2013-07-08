/**
* ============================== Header ============================== 
* file:          F4gGuiStatusPanel.java
* project:       FIT4Green/F4gGui
* created:       14 dec 2010 by jos@almende.org
* 
* $LastChangedDate: 2011-09-07 16:08:44 +0200 (wo, 07 sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 716 $
* 
* short description:
*   Provides information on the status of the plug-in and 
*   interaction possibilities.
* ============================= /Header ==============================
*/
package f4g.f4gGui.gui.client;

import java.util.Date;
import java.util.Vector;

import f4g.f4gGui.gui.shared.ActionData;
import f4g.f4gGui.gui.shared.Status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RadioButton;

/**
* Provides information on the status of the plug-in and 
* interaction possibilities.
*   
* @author Jos de Jong, Vasiliki Georgiadou
*/
public class F4gGuiStatusPanel extends LayoutPanel {
	private static final int REFRESH_INTERVAL = 5000; 
	
	private Boolean isRunning = false;
    
	final Button btnStart = new Button("Start");
    final Button btnStop = new Button("Stop");
    final Button btnOptimize = new Button("Optimize");
    final Button btnOK = new Button("OK");
	final Button btnCancel = new Button("Cancel");
	
	final RadioButton rbPower = new RadioButton("OptimizationObjectives", "Power");
	final RadioButton rbCO2 = new RadioButton("OptimizationObjectives", "CO2");
	
	final Label lblStatus = new Label();
	final Label lblPowerConsumption = new Label();
	final Label lblInfo = new Label();
	final Label lblActionsInfo = new Label();
	final Label lblApprovalInfo = new Label();
	final Label lblTableUpdateInfo = new Label();
    final Label lblFrameworkStatus = new Label();
    
	final HTML htmlPowerReduction = new HTML();

    final FlexTable tblActions = new FlexTable();	

	private NumberFormat fmt = NumberFormat.getFormat("0"); // round to integers
    
	F4gGuiStatusPanel() {
    	create();    	
    }
    
	/**
	 * Create a remote service proxy to talk to the server-side ActionService 
	 * service.
	 */
	private final ActionServiceAsync actionService = 
		GWT.create(ActionService.class);

	/**
	 * Create the status panel where the action history is shown, and from where
	 * the data centre operator can start and stop the FIT4green plug-in, 
	 * or request optimization
	 * @return
	 */
	public void create() {
		
	    this.setStyleName("panelStatus");

	    FlowPanel main = new FlowPanel();
	    main.setStyleName("panelTabContent");
	    this.add(main);
	    
	    // create header, buttons, and info label	    
	    main.add(btnStart);
	    btnStart.setStyleName("actionButton");
		btnStart.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					startup();
				}
			}	
		);
	    main.add(btnStop);
	    btnStop.setStyleName("actionButton");
		btnStop.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					shutdown();
				}
			}	
		);
	    main.add(btnOptimize);
	    btnOptimize.setStyleName("actionButton");
		btnOptimize.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						optimize();
					}
				}	
			);	    
		
		main.add(rbPower);
		main.add(rbCO2);
		rbPower.addClickHandler(
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						setOptObjective(0);
					}
				});
		
		rbCO2.addClickHandler(
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						setOptObjective(1);
					}
				});
		
		main.add(lblStatus);
		lblStatus.setStyleName("status");
		main.add(lblPowerConsumption);
		lblPowerConsumption.setStyleName("status");
	    main.add(lblInfo);
	    lblInfo.setStyleName("info");
	    
	    // create acknowledgment buttons and labels
	    Label headerActionApproval = new Label("Action's approval");
	    headerActionApproval.setStyleName("h2");
	    main.add(headerActionApproval);
	    main.add(btnOK);
	    btnOK.setStyleName("actionButton");
		btnOK.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					approveActions();
				}
			}	
		);
		btnOK.setEnabled(false);
		main.add(btnCancel);
	    btnCancel.setStyleName("actionButton");
		btnCancel.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					cancelActions();
				}
			}	
		);
		btnCancel.setEnabled(false);
		main.add(lblActionsInfo);
		main.add(lblApprovalInfo);
		lblActionsInfo.setStyleName("info");
		lblApprovalInfo.setStyleName("info");
		
	    // create the table of suggested actions
	    Label headerActionHistory = new Label("Actions suggested");
	    headerActionHistory.setStyleName("h2");
	    main.add(headerActionHistory);	  
	    main.add(htmlPowerReduction);
	    htmlPowerReduction.setStyleName("status");
	    
	    main.add(tblActions);
	    tblActions.setTitle("Action history");

	    main.add(lblTableUpdateInfo);
	    lblTableUpdateInfo.setStyleName("info");
	    
	    Label headerFrameworkStatus = new Label("Framework status");
        headerFrameworkStatus.setStyleName("h2");
        main.add(headerFrameworkStatus);
        
        main.add(lblFrameworkStatus);
        lblFrameworkStatus.setStyleName("info-error");

	    // Setup timer to retrieve the current status of the system.
	    Timer statusTimer = new Timer() {
			@Override
			public void run() {
				retrieveStatus();
			}
	    };
	    statusTimer.scheduleRepeating(REFRESH_INTERVAL);	 
	    
		retrieveStatus();   
	    
	    // Setup timer to refresh action list automatically.
	    Timer actionsTimer = new Timer() {
			@Override
			public void run() {
				retrieveActions();
				retrievePowerReduction();
			}
	    };
	    actionsTimer.scheduleRepeating(REFRESH_INTERVAL);	    
	    
	    retrieveActions();
	    retrievePowerReduction();
	    
	    enableActionButtons(false);
	}
	

	/**
	 * Perform a startup action via an asynchronous callback to the server  
	 */
	private void startup() {
		enableActionButtons(false);
		lblInfo.setText("Requesting startup...");
		lblInfo.setStyleName("info");
		
		actionService.startup(
				new AsyncCallback<String>() {
					public void onSuccess(String result) {
						enableActionButtons(true);

						lblInfo.setText(result);
						lblInfo.setStyleName("info");						
					}

					public void onFailure(Throwable caught) {
						enableActionButtons(true);
						
						lblInfo.setText("Error: " + caught.getMessage());
						lblInfo.setStyleName("info-error");						
					}
				});		
	}

	/**
	 * Perform a shutdown action via an asynchronous callback to the server  
	 */
	private void shutdown() {
		enableActionButtons(false);
		lblInfo.setText("Requesting shutdown...");
		lblInfo.setStyleName("info");
		
		actionService.shutdown(
				new AsyncCallback<String>() {
					public void onSuccess(String result) {
						enableActionButtons(true);

						lblInfo.setText(result);
						lblInfo.setStyleName("info");						
					}

					public void onFailure(Throwable caught) {
						enableActionButtons(true);
						
						lblInfo.setText("Error: " + caught.getMessage());
						lblInfo.setStyleName("info-error");						
					}
				});		
		lblActionsInfo.setText("");
		lblApprovalInfo.setText("");
	}

	/**
	 * Perform an optimization request via an asynchronous callback to the server  
	 */
	private void optimize() {
		enableActionButtons(false);
		lblInfo.setText("Requesting optimization...");
		lblInfo.setStyleName("info");
		
		actionService.optimize(
				new AsyncCallback<String>() {
					public void onSuccess(String result) {
						enableActionButtons(true);

						lblInfo.setText(result);
						lblInfo.setStyleName("info");						
					}

					public void onFailure(Throwable caught) {
						enableActionButtons(true);
						
						lblInfo.setText("Error: " + caught.getMessage());
						lblInfo.setStyleName("info-error");						
					}
				});		
	}
	
	/**
	 * Propagate data centre operator's decision with regard to actions: approved  
	 */
	private void approveActions() {
		
		btnOK.setEnabled(false);
		btnCancel.setEnabled(false);
		lblApprovalInfo.setText("Sending actions' approval...");
		lblApprovalInfo.setStyleName("info");
		
		actionService.approveActions(
				new AsyncCallback<String>() {
					public void onSuccess(String result) {
						lblApprovalInfo.setText(result);
						lblApprovalInfo.setStyleName("info");		
						retrieveActions();
					}

					public void onFailure(Throwable caught) {
						btnOK.setEnabled(true && isRunning);
						btnCancel.setEnabled(true && isRunning);
						lblApprovalInfo.setText("Error: " + caught.getMessage());
						lblApprovalInfo.setStyleName("info-error");						
					}
				});		
	}
	
	/**
	 * Propagate data centre operator's decision with regard to actions: canceled  
	 */
	private void cancelActions() {
		
		btnOK.setEnabled(false);
		btnCancel.setEnabled(false);
		lblApprovalInfo.setText("Sending actions' approval...");
		lblApprovalInfo.setStyleName("info");
		
		actionService.cancelActions(
				new AsyncCallback<String>() {
					public void onSuccess(String result) {
						lblApprovalInfo.setText(result);
						lblApprovalInfo.setStyleName("info");	
						retrieveActions();
					}

					public void onFailure(Throwable caught) {
						btnOK.setEnabled(true && isRunning);
						btnCancel.setEnabled(true && isRunning);
						lblApprovalInfo.setText("Error: " + caught.getMessage());
						lblApprovalInfo.setStyleName("info-error");						
					}
				});		
	}
	
	/**
	 * Set optimization objective
	 * @param option	0: power 
	 * 					1: CO2 (carbon)
	 */
	private void setOptObjective(int option) {
		
		lblInfo.setText("Setting optimization objective...");
		lblInfo.setStyleName("info");
		
		actionService.setOptimizationObjective(option,
				new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						// nothing to do		
					}

					public void onFailure(Throwable caught) {						
						lblInfo.setText("Error: " + caught.getMessage());
						lblInfo.setStyleName("info-error");						
					}
				});		
	}
	
	/**
	 * Retrieve the actions table via an asynchronous callback, and update the
	 * table in the GUI
	 */
	private void retrieveActions() {
		actionService.getActionList(new AsyncCallback< Vector<ActionData> >() {
					public void onSuccess(Vector<ActionData> actions) {
						updateActionsTable(actions);

						lblTableUpdateInfo.setText("Refreshed " + new Date());
						lblTableUpdateInfo.setStyleName("info");						
					}

					public void onFailure(Throwable caught) {
						lblTableUpdateInfo.setText("Error: " + caught.getMessage());
						lblTableUpdateInfo.setStyleName("info-error");
					}
				});
	}

	/**
	 * Retrieve the ICT power difference and update the corresponding label
	 */
	private void retrievePowerReduction() {
		actionService.getPowerReduction(new AsyncCallback<Double>() {
					public void onSuccess(Double powerReduction) {
						updatePowerReduction(powerReduction);

						lblTableUpdateInfo.setText("Refreshed " + new Date());
						lblTableUpdateInfo.setStyleName("info");						
					}

					public void onFailure(Throwable caught) {
						lblTableUpdateInfo.setText("Error: " + caught.getMessage());
						lblTableUpdateInfo.setStyleName("info-error");
					}
				});
	}

	
	/**
	 * Retrieve the status of the plug-in via an asynchronous call-back: stopped 
	 * or running, the computed ICT power consumption, and the optimization objective  
	 */
	private void retrieveStatus() {
		actionService.getStatus(
				new AsyncCallback< Status >() {
					public void onSuccess(Status status) {
						isRunning = status.isRunning;
						updateStatus(status.statusMessage);
						updateFrameworkStatusError(status.errorMessage);
						if (status.powerConsumption == -1){
							lblPowerConsumption.setText("ICT power consumption is not computed; for more information, see the log file");
						} else {
							updatePowerConsumption(status.powerConsumption);
						}
						rbPower.setValue(status.isObjectivePower);
						rbCO2.setValue(!status.isObjectivePower);
						
						enableActionButtons(true);
					}

					public void onFailure(Throwable caught) {
						updateStatus("Unknown");
						updatePowerConsumption("Unknown");
						
						lblInfo.setText(caught.getMessage());
						lblInfo.setStyleName("info-error");
					}
				});
	}
	
	/**
	 * Enable or disable the action buttons
	 * When enable is true, only the actions which are currently available
	 * will be enabled.
	 * @param enable
	 */
	void enableActionButtons(boolean enable) {
		btnStart.setEnabled(enable && !isRunning);
		btnStop.setEnabled(enable && isRunning);
		btnOptimize.setEnabled(enable && isRunning);
		rbPower.setEnabled(enable && isRunning);
		rbCO2.setEnabled(enable && isRunning);
	}

	/**
	 * Update the table on screen with the provided list with actions
	 * @param actions
	 */
	void updateActionsTable(Vector<ActionData> actions) {
		
		boolean enable = false;
		String txt = "No actions pending for approval";
		
		if (actions.size() > 0) {
			if (!actions.get(0).isAutomatic()) {
				if (!actions.get(0).isObsolete()){
					enable = true;
					txt = "Actions require approval";
				} else {
					enable = false;
					txt = "Actions do not require approval";
				}
			}
		}
		
		btnOK.setEnabled(enable && isRunning);
		btnCancel.setEnabled(enable && isRunning);
		lblActionsInfo.setText(txt);
		lblActionsInfo.setStyleName("info");
		
		tblActions.removeAllRows();
		
		tblActions.setText(0, 0, "Com name");
		tblActions.setText(0, 1, "Action");
		tblActions.setText(0, 2, "Parameter(s)");
		tblActions.setText(0, 3, "Date");
	    
	    tblActions.getCellFormatter().addStyleName(0, 0, "actionsTableHeader");
	    tblActions.getCellFormatter().addStyleName(0, 1, "actionsTableHeader");
	    tblActions.getCellFormatter().addStyleName(0, 2, "actionsTableHeader");
	    tblActions.getCellFormatter().addStyleName(0, 3, "actionsTableHeader");
 
	    tblActions.getRowFormatter().addStyleName(0, "actionListHeader");		
	    tblActions.addStyleName("actionsList");
	    
	    String style = "actionsTableCell";
	    
	    if (actions.size() > 0) {
	    	if (actions.get(0).isObsolete()){
	    		style = "obsoleteActionsTableCell";
	    	} 
	    }

		for(int i=0; i<actions.size(); i++){
			ActionData action = actions.get(i);
			tblActions.setText(i+1, 0, action.getComName());
			tblActions.setText(i+1, 1, action.getAction());
			
			// TODO (phase 3) if possible find a more elegant solution
			String param = "";
			if (action.getAction().compareTo("PowerOff") == 0 
					|| action.getAction().compareTo("PowerOn") == 0 
					|| action.getAction().compareTo("StandBy") == 0) {
				param = "Node " + action.getNodeName();
			} else if (action.getAction().compareTo("LiveMigrateVM") == 0 
					|| action.getAction().compareTo("MoveVM") == 0) {
				param = "VM " + action.getVirtualMachine() + 
				        " from " + action.getSrcNodeName() +
				        " to " + action.getDstNodeName();
			} else if (action.getAction().compareTo("StartJob") == 0) {
				param = "Job " + action.getJobID();
			}
			tblActions.setText(i+1, 2, param);
			tblActions.setText(i+1, 3, action.getLastUpdateTimestamp().toString());
			
			tblActions.getCellFormatter().addStyleName(i+1, 0, style);
			tblActions.getCellFormatter().addStyleName(i+1, 1, style);		
			tblActions.getCellFormatter().addStyleName(i+1, 2, style);
			tblActions.getCellFormatter().addStyleName(i+1, 3, style);
		}
	}
	
	private void updateStatus(String statusMessage) {
		lblStatus.setText("Status: " + statusMessage);
	}
	
	private void updateFrameworkStatusError(String errorMessage){
        if(errorMessage != null && errorMessage.length() !=0){
                lblFrameworkStatus.setText(errorMessage);
                lblFrameworkStatus.setStyleName("info-error");
                Window.alert("Communication failure" + "\n\n" + "For more information, go to the Status tab");
        }
}

	private void updatePowerConsumption(String powerConsumption) {
		lblPowerConsumption.setText("Current ICT power consumption: " + 
				powerConsumption + " Watt");
	}
	
	private void updatePowerConsumption(double powerConsumption) {
		String power = fmt.format(powerConsumption);
		updatePowerConsumption(power);
	}
	
	private void updatePowerReduction(double powerReduction) {
		String power = fmt.format(powerReduction);
		
		// colorize depending on the sign of the reduction value
		String color = "black";
		if (powerReduction > 0) {
			color = "green";
		}
		else if (powerReduction < 0) {
			color = "red";
		}

		htmlPowerReduction.setHTML("Expected ICT power savings: " + 
			"<span style='color: " + color + "; font-weight: bold;'>" + 
			power + " Watt</span>" + 
			" <span class='info'>(based on the latest set of actions)</span>");		
	}
}
