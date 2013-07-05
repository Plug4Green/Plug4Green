package org.f4g.gui.client;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class F4gGui implements EntryPoint {
	private static final String GUI_VERSION = "Version 3.A (Beta)";
	
    // Models panel
    final Button btnLoadModel = new Button("Load");
    final Button btnSaveModel = new Button("Save");
    final Button btnCloseModel = new Button("Close");

	private F4gGuiStatusPanel statusPanel = new F4gGuiStatusPanel();
	private F4gGuiStatisticsPanel statisticsPanel = new F4gGuiStatisticsPanel();
	private F4gGuiConfigPanel configPanel = new F4gGuiConfigPanel();
	private F4gGuiHelpPanel helpPanel = new F4gGuiHelpPanel();

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		DockLayoutPanel panelMain = new DockLayoutPanel(Unit.PX);
	    RootLayoutPanel.get().add(panelMain);
	    
	    panelMain.setStyleName("panelMain");
		panelMain.addNorth(
				new HTML("<img src='F4Green_logo.jpg' align='right' style='height:40px;'>" +
						 "<h1>FIT4Green Management</h1>"), 40);
		Label versionInfo = new Label();
		versionInfo.setText(GUI_VERSION);
		versionInfo.setStyleName("version");
		panelMain.addSouth(versionInfo, 16);
		//panelMain.addWest(new HTML("navigation"), 10);

		TabLayoutPanel panelTab = new TabLayoutPanel(40, Unit.PX);
	    panelMain.add(panelTab);
	    
	    panelTab.add(statusPanel, "Status");
	    panelTab.add(statisticsPanel, "Statistics");
	    panelTab.add(configPanel, "Configuration");
	    panelTab.add(helpPanel, "Help");
	}
}
