package f4g.f4gGui.gui.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import f4g.f4gGui.gui.shared.ConfigurationData;

import com.chap.links.client.Graph;
import com.chap.links.client.Graph.DateRange;
import com.chap.links.client.Network;
import com.chap.links.client.Timeline;
import com.chap.links.client.events.TimeChangedHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dev.shell.remoteui.MessageTransport.RequestException;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.events.RangeChangeHandler;
import com.google.gwt.visualization.client.events.SelectHandler;

public class F4gGuiStatisticsPanel extends LayoutPanel {

	final int REFRESH_INTERVAL = 60 * 1000 * 60; // milliseconds
	int viewNumber;
	String couchDB;
	String actionsDB;
	String modelsDB;

	final String COUCHDB_ACTIONS_DESIGN = "webUI_utils";
	final String COUCHDB_ACTIONS_VIEW = "retrieveActions";
	boolean actionsViewCreated;

	final String COUCHDB_MODELS_DESIGN = "webUI_utils_01";
	final String COUCHDB_COMPUTEDPOWER_VIEW = "computePower";
	boolean computedPowerViewCreated;

	final String COUCHDB_SERVERS_DESIGN = "webUI_utils_02";
	final String COUCHDB_SERVERLIST_VIEW = "retrieveServerList";
	boolean serverlistViewCreated;

	final FlowPanel panelMain = new FlowPanel();
	final FlowPanel panelComputedPower = new FlowPanel();
	// Graph CheckBox
	final HorizontalPanel panelGraphContainer = new HorizontalPanel();
	final HorizontalPanel panelChecKBox = new HorizontalPanel();
	final Label headerComputedPower = new Label();
	CheckBox checkBox;
	String[] options = { "Total ICT Power", "ICT Power Sites",
			"Total Sites Power", "Sites Power", "Total CO2 Emissions",
			"Sites CO2 Emissions" };
	final Map<CheckBox, Boolean> theCheckBoxMap = new HashMap<CheckBox, Boolean>();
	final List<String> checkedOptions = new ArrayList<String>();
	ArrayList<CheckBox> disableCheckBox = new ArrayList<CheckBox>();

	// actions
	final FlowPanel panelActions = new FlowPanel();
	final Label infoComputedPower = new Label();
	final Label infoActions = new Label();
		
	Graph graphComputedPower = null;
	Timeline timelineActions = null;
	Network network = null;
	Timeline timelines = null;
	//network popup
	final Label infoNetwork = new Label();
	final FlowPanel networkPanel = new FlowPanel();
	final FlowPanel panelMainPopup = new FlowPanel();
	final FlowPanel panelPopupTimeLine = new FlowPanel();
	final Label  infoPopup = new Label();
	String startKey = null;
	Button button = new Button("Show network Topology");
	
	// Example of the datetime format: "2011-09-14T13:00:00.000Z"
	DateTimeFormat dtf = DateTimeFormat
			.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	/**
	 * Create a remote service proxy to talk to the server-side FetchService
	 * service.
	 */
	private final FetchServiceAsync fetchService = GWT
			.create(FetchService.class);

	/**
	 * Create a remote service proxy to talk to the server-side ConfigService
	 * service.
	 */
	private final ConfigServiceAsync configService = GWT
			.create(ConfigService.class);

	F4gGuiStatisticsPanel() {
		retrieveConfigurationData();
		create();
	}

	private class MyPopup extends PopupPanel {
		public MyPopup(Widget w){
			super(true);
			setWidget(w);
		}
	};
		
	private void retrieveConfigurationData() {

		configService
				.getConfigurationData(new AsyncCallback<ConfigurationData>() {

					public void onSuccess(ConfigurationData conf) {

						couchDB = conf.getUrlDB();
						actionsDB = conf.getActionsDB();
						modelsDB = conf.getModelsDB();

					}

					public void onFailure(Throwable caught) {

					}
				});
	}

	@SuppressWarnings("deprecation")
	private void create() {
		panelMain.setStyleName("panelTabContent");
		this.add(panelMain);

		headerComputedPower.setText("Computed Power");
		headerComputedPower.setStyleName("h2 top");
		panelMain.add(headerComputedPower);
		panelGraphContainer.addStyleName("checkboxContainer");
		panelChecKBox.addStyleName("checkBox");
		panelGraphContainer.add(generateCheckBox());
		panelMain.add(panelGraphContainer);
		panelMain.add(panelComputedPower);
				
		Label headerNetwork = new Label("Topology");
		headerNetwork.setStyleName("h2");
		panelMainPopup.add(headerNetwork);
		panelMainPopup.add(panelPopupTimeLine);
		panelMainPopup.add(infoNetwork);
		panelMainPopup.add(networkPanel);
		button.setEnabled(false);
		button.setTitle("Show network topology window");
		button.setHTML("<img src='network.png' style='height:70px;'>");
		button.addClickListener(new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				infoNetwork.setText("Updating...");
				infoNetwork.setStyleName("info");
				retrieveServers(new AsyncCallback<Map<String,JSONArray>>() {
				   @Override public void onFailure(Throwable caught) {
				   infoNetwork.setText(caught.getMessage());
				   infoNetwork.setStyleName("info-error"); }
				   
				   @Override public void onSuccess(Map<String, JSONArray> data) { 
					 if(data != null) {
						 Timeline.Options option = Timeline.Options.create();
						 option.setWidth("200px");
						 option.setHeight("50px");
						 timelines = new Timeline(new JSONArray().getJavaScriptObject(), option);
						 panelPopupTimeLine.clear();
						 panelPopupTimeLine.add(timelines);
						 Network.Options options = Network.Options.create();
						 options.setWidth("100%");
						 options.setHeight("400px");
						 options.setGroupBackgroundColor("Off", "#BDBDBD");
						 options.setGroupBackgroundColor("PoweringOff", "#D8D8D8");
						 options.setGroupBackgroundColor("PoweringOn", "#CEE3F6");
						 options.setGroupBackgroundColor("Standby", "#CEF6F5");
						 network = new Network(data.get("nodes").getJavaScriptObject(),data.get("links").getJavaScriptObject(), options);
						 networkPanel.clear(); 
						 networkPanel.add(network);
						 infoNetwork.setText("Updated " + new Date());
						 infoNetwork.setStyleName("info");
						 final MyPopup popup = new MyPopup(panelMainPopup);
						 popup.setPixelSize(Window.getClientWidth() - 200, Window.getClientHeight() - 250);
						 popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
							@Override
							public void setPosition(int offsetWidth, int offsetHeight) {
								 int left = (Window.getClientWidth() - offsetWidth)/3 ;
						         int top = ((Window.getClientHeight() - offsetHeight))/3;
						         popup.setPopupPosition(left, top);
							}
						});
						}
				    } });				
				}
		});
		
		panelMain.add(button);
		panelMain.add(infoComputedPower);
		Label headerActions = new Label("Actions");
		headerActions.setStyleName("h2");
		Label paddingElement = new Label();
		paddingElement.setStyleName("popupFormat");
		panelMain.add(paddingElement);
		panelMain.add(headerActions);
		panelMain.add(panelActions);
		panelMain.add(infoActions);
		// Create a callback to be called when the visualization API
		// has been loaded.
		Runnable onLoadCallback = new Runnable() {
			public void run() {
				timerRefresh.run();
				updateGraph(); 
				// start refreshing data every x seconds
				timerRefresh.scheduleRepeating(REFRESH_INTERVAL);
			}
		};

		// Load the visualization api, passing the onLoadCallback to be called
		// when loading is done.
		VisualizationUtils.loadVisualizationApi(onLoadCallback);

		actionsViewCreated = false;
		computedPowerViewCreated = false;
		serverlistViewCreated = false;
	}

	private HorizontalPanel generateCheckBox() {

		ClickHandler handler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				CheckBox clickedCheckBox = (CheckBox) event.getSource();
				if ((clickedCheckBox.getName().equals("Total CO2 Emissions") && clickedCheckBox
						.isChecked())
						|| (clickedCheckBox.getName().equals(
								"Sites CO2 Emissions") && clickedCheckBox
								.isChecked())) {
					theCheckBoxMap.put(clickedCheckBox,
							clickedCheckBox.isChecked());
					headerComputedPower.setStyleName("h2 top");
					headerComputedPower.setText("Computed CO2 Emissions");
					if (disableCheckBox.size() != 0) {
						Iterator<CheckBox> disableCheckBoxList = disableCheckBox
								.iterator();
						while (disableCheckBoxList.hasNext()) {
							// disable the rest of options 1,2,3,4 and update
							// the map
							CheckBox thisCheckBox = disableCheckBoxList.next();
							thisCheckBox.setEnabled(false);
							thisCheckBox.setChecked(false);
							theCheckBoxMap.put(thisCheckBox, false);
						}
						updateGraph();
					}
				} else if ((clickedCheckBox.getName().equals(
						"Total CO2 Emissions") && !(clickedCheckBox.isChecked()))
						|| (clickedCheckBox.getName().equals(
								"Sites CO2 Emissions") && !(clickedCheckBox
								.isChecked()))) {
					Set<CheckBox> aCheckBox = theCheckBoxMap.keySet();
					Iterator<CheckBox> setOfCheckBox = aCheckBox.iterator();
					boolean isTotalEmission = false;
					boolean isSitesEmission = false;
					while (setOfCheckBox.hasNext()) {
						CheckBox myCheckBox = setOfCheckBox.next();
						if ((myCheckBox.getName().equals("Sites CO2 Emissions") && (!myCheckBox
								.isChecked()))) {
							isSitesEmission = true;
						}
						if ((myCheckBox.getName().equals("Total CO2 Emissions") && (!myCheckBox
								.isChecked()))) {
							isTotalEmission = true;
						}
					}
					if ((disableCheckBox.size() != 0)
							&& (isTotalEmission && isSitesEmission)) {
						Iterator<CheckBox> disableCheckBoxList = disableCheckBox
								.iterator();
						while (disableCheckBoxList.hasNext()) {
							// enable the rest of options 1,2,3,4
							CheckBox thisCheckBox = disableCheckBoxList.next();
							thisCheckBox.setEnabled(true);
							if ((thisCheckBox.getName()
									.equals("Total ICT Power"))) {
								theCheckBoxMap.put(thisCheckBox, true);
								thisCheckBox.setChecked(true);
							}
						}
						disableCheckBox.clear();
						headerComputedPower.setStyleName("h2 top");
						headerComputedPower.setText("Computed Power");
					}
					theCheckBoxMap.put(clickedCheckBox,
							clickedCheckBox.isChecked());
					updateGraph();
				} else {
					theCheckBoxMap.put(clickedCheckBox,
							clickedCheckBox.isChecked());
					updateGraph();
				}
			}
		};
		for (int i = 0; i < options.length; i++) {
			checkBox = new CheckBox(options[i]);
			checkBox.setName(options[i]);
			checkBox.ensureDebugId("checkBox-" + options[i]);
			checkBox.addClickHandler(handler);
			panelChecKBox.add(checkBox);
			if (i == 0) {
				checkBox.setChecked(true);
				theCheckBoxMap.put(checkBox, true);
			} else
				theCheckBoxMap.put(checkBox, false);
		}
		return panelChecKBox;
	}

	protected void updateGraph() {

		Iterator<Map.Entry<CheckBox, Boolean>> entries = theCheckBoxMap
				.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<CheckBox, Boolean> thisEntry = entries.next();
			CheckBox aCheckBox = thisEntry.getKey();
			Boolean checked = thisEntry.getValue();
			if ((!aCheckBox.getName().equals("Total CO2 Emissions"))
					&& (!aCheckBox.getName().equals("Sites CO2 Emissions"))
					&& (!disableCheckBox.contains(aCheckBox))) {
				disableCheckBox.add(aCheckBox);
			}
			if ((checked) && (!checkedOptions.contains(aCheckBox.getName()))) {
				checkedOptions.add(aCheckBox.getName());
			} else if ((!checked)
					&& (checkedOptions.contains(aCheckBox.getName()))) {
				checkedOptions.remove(aCheckBox.getName());
			}
		}

		char[] myBinary = new char[] { '0', '0', '0', '0', '0', '0' };
		if (checkedOptions.contains("Total ICT Power")) {
			myBinary[5] = '1';
		}
		if (checkedOptions.contains("ICT Power Sites")) {
			myBinary[4] = '1';
		}
		if (checkedOptions.contains("Total Sites Power")) {
			myBinary[3] = '1';
		}
		if (checkedOptions.contains("Sites Power")) {
			myBinary[2] = '1';
		}
		if (checkedOptions.contains("Total CO2 Emissions")) {
			myBinary[1] = '1';
		}
		if (checkedOptions.contains("Sites CO2 Emissions")) {
			myBinary[0] = '1';
		}

		viewNumber = Integer.parseInt(new String(myBinary), 2);
		retrieveComputedPower(new AsyncCallback<JSONArray>() {
			@Override
			public void onFailure(Throwable caught) {
				infoComputedPower.setText(caught.getMessage());
				infoComputedPower.setStyleName("info-error");
			}

			@Override
			public void onSuccess(JSONArray data) {
				if (graphComputedPower == null) {
					createGraphComputedPower(data);
				} else {
					redrawGraphComputedPower(data);
				}

				infoComputedPower.setText("Updated " + new Date());
				infoComputedPower.setStyleName("info");
			}
		});

	}

	/**
	 * Create the view necessary to extract actions data from CouchDB
	 */
	private void createActionsView() {
		try {
			String url = couchDB + "/" + actionsDB + "/_design/"
					+ COUCHDB_ACTIONS_DESIGN;

			String map = "function(doc) {"
					+ "\n"
					+ "  var actionList = doc.ActionList;"
					+ "\n"
					+ "  var isAutomatic = doc.IsAutomatic;"
					+ "\n"
					+ "  var numberOfPowerOff = 0;"
					+ "\n"
					+ "  var numberOfPowerOn = 0;"
					+ "\n"
					+ "  var numberOfForwardedPowerOff = 0;"
					+ "\n"
					+ "  var numberOfForwardedPowerOn =0;"
					+ "\n"
					+ "  var numberOfStandBy = 0;"
					+ "\n"
					+ "  var numberOfStartJob = 0;"
					+ "\n"
					+ "  var numberOfForwardedStandBy = 0;"
					+ "\n"
					+ "  var numberOfForwardedStartJob =0;"
					+ "\n"
					+ "  var numberOfMoveVM = 0;"
					+ "\n"
					+ "  var numberOfLiveMigrateVM = 0;"
					+ "\n"
					+ "  var numberOfForwardedMoveVM = 0;"
					+ "\n"
					+ "  var numberOfForwardedLiveMigrateVM =0;"
					+ "\n"
					+ "  if (actionList) {"
					+ "\n"
					+ "    for (var action in actionList) {"
					+ "\n"
					+ "      if (actionList.hasOwnProperty(action)) {"
					+ "\n"
					+ "        var occurrences = actionList[action];"
					+ "\n"
					+ "        if (occurrences.length === undefined) {"
					+ "\n"
					+ "          occurrences = [occurrences];"
					+ "\n"
					+ "        }"
					+ "\n"
					+ "        for (var i = 0; i < occurrences.length; i++) {"
					+ "\n"
					+ "           if (action === \\\"PowerOff\\\"){ "
					+ "\n"
					+ "             numberOfPowerOff++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedPowerOff ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "           if (action === \\\"PowerOn\\\"){ "
					+ "\n"
					+ "             numberOfPowerOn++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedPowerOn ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "           if (action === \\\"StandBy\\\"){ "
					+ "\n"
					+ "             numberOfStandBy++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedStandBy ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "           if (action === \\\"StartJob\\\"){ "
					+ "\n"
					+ "             numberOfStartJob++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedStartJob ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "           if (action === \\\"MoveVM\\\"){ "
					+ "\n"
					+ "             numberOfMoveVM++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedMoveVM ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "           if (action === \\\"LiveMigrateVM\\\"){ "
					+ "\n"
					+ "             numberOfLiveMigrateVM++;"
					+ "\n"
					+ "             if (occurrences[i].Forwarded == \\\"true\\\"){"
					+ "\n"
					+ "               numberOfForwardedLiveMigrateVM ++;"
					+ "\n"
					+ "             }"
					+ "\n"
					+ "           }"
					+ "\n"
					+ "        }"
					+ "\n"
					+ "      }"
					+ "\n"
					+ "    }"
					+ "\n"
					+ "    emit(new Date(doc.Datetime), {"
					+ "\n"
					+ "         \\\"PowerOn\\\": numberOfPowerOn,"
					+ "\n"
					+ "         \\\"PowerOff\\\": numberOfPowerOff,"
					+ "\n"
					+ "         \\\"ForwardedPowerOff\\\": numberOfForwardedPowerOff,"
					+ "\n"
					+ "         \\\"ForwardedPowerOn\\\": numberOfForwardedPowerOn,"
					+ "\n"
					+ "         \\\"StandBy\\\": numberOfStandBy,"
					+ "\n"
					+ "         \\\"StartJob\\\": numberOfStartJob,"
					+ "\n"
					+ "         \\\"ForwardedStandBy\\\": numberOfForwardedStandBy,"
					+ "\n"
					+ "         \\\"ForwardedStartJob\\\": numberOfForwardedStartJob,"
					+ "\n"
					+ "         \\\"MoveVM\\\": numberOfMoveVM,"
					+ "\n"
					+ "         \\\"LiveMigrateVM\\\": numberOfLiveMigrateVM,"
					+ "\n"
					+ "         \\\"ForwardedMoveVM\\\": numberOfForwardedMoveVM,"
					+ "\n"
					+ "         \\\"ForwardedLiveMigrateVM\\\": numberOfForwardedLiveMigrateVM"
					+ "\n" + "         });" + "\n" + "      }" + "\n" + "   }";

			String body = "{" + "\n" + "   \"language\": \"javascript\","
					+ "\n" + "   \"views\": {" + "\n" + "      \""
					+ COUCHDB_ACTIONS_VIEW + "\": {" + "\n"
					+ "         \"map\": \"" + map + "\"" + "\n" + "      }"
					+ "\n" + "   }" + "\n" + "}";

			String contentType = "application/json";

			fetchService.put(url, body, contentType,
					new AsyncCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							actionsViewCreated = false;
						}

						@Override
						public void onSuccess(String result) {
							actionsViewCreated = true;
						}
					});
		} catch (Exception err) {
			// Do nothing with the error
		}
	}

	/**
	 * Create the view necessary to extract computed power data from CouchDB
	 */
	private void createComputedPowerView() {
		try {
			String url = couchDB + "/" + modelsDB + "/_design/"
					+ COUCHDB_MODELS_DESIGN;

			// TODO make the search more generic and not dependent on "ns2:"
			String map = "function(doc) {"
					+ "\n"
					+ "  var sites = doc[\\\"ns2:Site\\\"];"
					+ "\n"
					+ "  var total_ICT = \\\"0.0\\\";"
					+ "\n"
					+ "  var sitesArray = [];"
					+ "\n"
					+ "  var powerArray = [];"
					+ "\n"
					+ "  var emissionArray = [];"
					+ "\n"
					+ "  var total_power = \\\"0.0\\\";"
					+ "\n"
					+ "  var total_emission = \\\"0.0\\\";"
					+ "\n"
					+ "  if (sites.length === undefined) {"
					+ "\n"
					+ "    // one site"
					+ "\n"
					+ "    if(sites.computedPower){"
					+ "\n"
					+ "      sitesArray.push(parseFloat(sites.computedPower));"
					+ "\n"
					+ "      powerArray.push(parseFloat(sites.computedPower) * parseFloat(sites.PUE));"
					+ "\n"
					+ "      emissionArray.push(parseFloat(sites.computedPower) * parseFloat(sites.CUE));"
					+ "\n"
					+ "      emit(new Date(doc.Datetime), {"
					+ "\n"
					+ "        \\\"Total ICT\\\": parseFloat(sites.computedPower),"
					+ "\n"
					+ "        \\\"sites ICT\\\": sitesArray,"
					+ "\n"
					+ "        \\\"Total Power\\\": parseFloat(sites.computedPower) * parseFloat(sites.PUE),"
					+ "\n"
					+ "        \\\"sites power\\\": powerArray,"
					+ "\n"
					+ "        \\\"total emission\\\": parseFloat(sites.computedPower) * parseFloat(sites.CUE),"
					+ "\n"
					+ "        \\\"sites emission\\\": emissionArray,"
					+ "\n"
					+ "\n"
					+ "       });"
					+ "\n"
					+ "     }"
					+ "\n"
					+ "  }"
					+ "\n"
					+ "  else {"
					+ "\n"
					+ "    // multiple sites"
					+ "\n"
					+ "    for (var i = 0; i < sites.length; i++) {"
					+ "\n"
					+ "       if(sites[i].computedPower) {"
					+ "\n"
					+ "		       total_ICT = parseFloat(total_ICT) + parseFloat(sites[i].computedPower);"
					+ "\n"
					+ "			   total_power = parseFloat(total_power) +  ( parseFloat(sites[i].computedPower) * parseFloat(sites[i].PUE));"
					+ "\n"
					+ "			   total_emission = parseFloat(total_emission) +  ( parseFloat(sites[i].computedPower) * parseFloat(sites[i].CUE));"
					+ "\n"
					+ "            sitesArray.push(parseFloat(sites[i].computedPower));"
					+ "\n"
					+ "            powerArray.push(parseFloat(sites[i].computedPower) * parseFloat(sites[i].PUE));"
					+ "\n"
					+ "            emissionArray.push(parseFloat(sites[i].computedPower) * parseFloat(sites[i].CUE));"
					+ "\n" + "        }" + "\n" + "       }"
					+ "            emit(new Date(doc.Datetime), {" + "\n"
					+ "             \\\"Total ICT\\\": total_ICT," + "\n"
					+ "              \\\"sites ICT\\\": sitesArray," + "\n"
					+ "              \\\"Total Power\\\": total_power," + "\n"
					+ "              \\\"sites power\\\": powerArray," + "\n"
					+ "               \\\"total emission\\\": total_emission,"
					+ "\n"
					+ "               \\\"sites emission\\\": emissionArray"
					+ "\n" + "         });" + "\n" + "   }" + "\n" + "}";

			String body = "{" + "\n" + "   \"language\": \"javascript\","
					+ "\n" + "   \"views\": {" + "\n" + "      \""
					+ COUCHDB_COMPUTEDPOWER_VIEW + "\": {" + "\n"
					+ "         \"map\": \"" + map + "\"" + "\n" + "      }"
					+ "\n" + "   }" + "\n" + "}";

			String contentType = "application/json";

			fetchService.put(url, body, contentType,
					new AsyncCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							computedPowerViewCreated = false;
						}

						@Override
						public void onSuccess(String result) {
							computedPowerViewCreated = true;
						}
					});
		} catch (Exception err) {
			// Do nothing with the error
		}
	}

	/**
	 * Create the view necessary to extract server list from CouchDB
	 */
	private void createServerListView() {
		try {
			String url = couchDB + "/" + modelsDB + "/_design/"
					+ COUCHDB_SERVERS_DESIGN;

			String map = "function(doc) {" + "\n"
					+ " var nodesArrayList = [];" + "\n"
					+ " var linksArrayList = [];" + "\n"
					+ " var items = doc;" + "\n"
					+ " var required_fields = [ \\\"ns2:Site\\\", \\\"ns2:Datacenter\\\" , \\\"ns2:Rack\\\", \\\"ns2:RackableServer\\\", \\\"ns2:Enclosure\\\", \\\"ns2:BladeServer\\\" , \\\"ns2:TowerServer\\\"];" + "\n"
					+ " function recursiveServerList (items, parent,id,nodesArrayList, linksArrayList) {" +  "\n" 
					+ "    var nodes = {}; " + "\n"
					+ "    var links = {};"  + "\n"
					+ "    for (var key in items) { " + "\n"
					+ "      if (items.hasOwnProperty(key)) {" + "\n"
					+ "         if( required_fields.indexOf(key) != -1 ){ " + "\n"
					+ "            if(items[key].length != undefined){"  + "\n"
					+ "                for(var i=0;i < items[key].length; i++){ "  + "\n"
					+ "                  nodes.id = id ;"  + "\n" 
					+ "                  var name = (items[key][i].name) ? ( items[key][i].name ) : ( i + 1); " + "\n"
					+ "                  nodes.text = key + \\\"   \\\" + name ;" + "\n"
					+ "                  nodes.title = \\\"computedPower : \\\" + items[key][i].computedPower;" + "\n"
					+ "                  nodes.group = (key == \\\"ns2:BladeServer\\\")? " + "\n"
					+ "                                     (items[key][i].status == \\\"OFF\\\" ?" + "\n"
					+ "                                 \\\"Off\\\" : items[key][i].status == \\\"POWERING_OFF\\\" ? " + "\n"
					+ "                                 \\\"PoweringOff\\\": items[key][i].status == \\\"POWERING_ON\\\" ? " + "\n"
					+ "                                 \\\"PoweringOn\\\":items[key][i].status == \\\"STANDBY\\\" ? " + " \n"
					+ "                                 \\\"Standby\\\" : \\\"group_x\\\" ) : \\\"group_x\\\"; " + "\n"
					+ "                  nodesArrayList.push(nodes); " + "\n"
					+ "                  nodes = {};" + "\n"
					+ "                  if(parent != undefined){" + "\n"
					+ "                   links = {};  " + "\n"
					+ "                   links.from = parent;  " + "\n"
					+ "                   links.to = id;  " + "\n"
					+ "                   linksArrayList.push(links);  " + "\n"
					+ "                  }" + "\n"
					+ "                  else if(parent != undefined && id != 1){" + "\n"
					+ "                   links = {};  " + "\n"
					+ "                   links.from = 1;  " + "\n"
					+ "                   links.to = id;  " + "\n"
					+ "                   linksArrayList.push(links);  " + "\n"
					+ "                  }" + "\n"
					+ "                  id = recursiveServerList(items[key][i], id, id + 1,nodesArrayList, linksArrayList);" + "\n"
					+ "                 }" + "\n"
					+ "               }else { " + "\n"
					+ "                    nodes.id = id ; " + "\n"
					+ "                    nodes.text = key ; " + "\n"
					+ "                    nodes.title = \\\" computedPower :\\\" + items[key].computedPower; " + "\n"
					+ "                    nodes.group = \\\"group_x\\\";" + "\n"
					+ "                    nodesArrayList.push(nodes); " + "\n"
					+ "                    nodes = {} ; "  + "\n"
					+ "                    if(parent != undefined){"  + "\n"
					+ "                      links = {};  "  + "\n"
					+ "                      links.from = parent;  " + "\n"
					+ "                      links.to = id;  " + "\n"
					+ "                      linksArrayList.push(links);  " + "\n"
					+ "                     }" + "\n"
					+ "                  id = recursiveServerList(items[key], id, id + 1,nodesArrayList, linksArrayList);" + "\n"
					+ "                }" + "\n"
					+ "          }" + "\n" 
					+ "      }"  + "\n"
					+ "    }"  + "\n"
					+ "    return id;"  + "\n"
					+ "  }" + "\n"
					+ " recursiveServerList(items, undefined, 1, nodesArrayList, linksArrayList);"  + "\n"
					+ "         emit(new Date(doc.Datetime), {" + "\n"
					+ "          \\\"nodes\\\": nodesArrayList ," + "\n"
					+ "          \\\"links\\\": linksArrayList" + "\n"
					+ "         });" + "\n" + "  }";

			String body = "{" + "\n" + "   \"language\": \"javascript\","
					+ "\n" + "   \"views\": {" + "\n" + "      \""
					+ COUCHDB_SERVERLIST_VIEW + "\": {" + "\n"
					+ "         \"map\": \"" + map + "\"" + "\n" + "      }"
					+ "\n" + "   }" + "\n" + "}";

			String contentType = "application/json";

			fetchService.put(url, body, contentType,
					new AsyncCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							System.out.println("onFailure caught  : "+caught.getMessage());
							serverlistViewCreated = false;
						}

						@Override
						public void onSuccess(String result) {
							System.out.println("RESULT returned : " +result);
							serverlistViewCreated = true;
						}
					});
		} catch (Exception err) {
			// Do nothing with the error
		}

	}

	/**
	 * Create a new Graph displaying the power consumption
	 * 
	 * @param data
	 */
	private void createGraphComputedPower(JSONArray data) {
		Graph.Options options = Graph.Options.create();
		options.setWidth("100%");
		options.setHeight("300px");
		options.setLegendVisibility(true);
		options.setLegendWidth(185);
		options.setLineStyle(Graph.Options.LINESTYLE.DOTLINE);

		graphComputedPower = new Graph(data.getJavaScriptObject(), options);
		panelComputedPower.clear();
		panelComputedPower.add(graphComputedPower);
		linkGraphs();
	}

	/**
	 * Set new data and options for the current graph with the power consumption
	 * 
	 * @param data
	 */
	private void redrawGraphComputedPower(JSONArray data) {
		if (graphComputedPower == null) {
			return;
		}
		Graph.Options options = Graph.Options.create();
		options.setWidth("100%");
		options.setHeight("300px");
		options.setLegendVisibility(true);
		options.setLegendWidth(185);
		options.setLineStyle(Graph.Options.LINESTYLE.DOTLINE);
		
		graphComputedPower.draw(data.getJavaScriptObject(), options);
	}

	/**
	 * Retrieve historical data with the power consumption from the server
	 * 
	 * @param callback
	 * @throws RequestException
	 */
	private void retrieveComputedPower(final AsyncCallback<JSONArray> callback) {

		if (!computedPowerViewCreated) {
			createComputedPowerView();
		}

		final String url = couchDB + "/" + modelsDB + "/_design/"
				+ COUCHDB_MODELS_DESIGN + "/_view/"
				+ COUCHDB_COMPUTEDPOWER_VIEW;

		fetchService.get(url, new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(String result) {
				try {
					JSONArray data = parseComputedPower(result, viewNumber);
					callback.onSuccess(data);
				} catch (Exception err) {
					callback.onFailure(err);
				}
			}
		});
	}

	/**
	 * Create a timeline displaying the actions
	 * 
	 * @param data
	 */
	private void createTimelineActions(JSONArray data) {
		Timeline.Options options = Timeline.Options.create();
		options.setShowCustomTime(true);
		options.setWidth("100%");
		options.setHeight("300px");
		options.setStyle(Timeline.Options.STYLE.BOX);
		Date start = dtf.parseStrict(dtf.format(new Date(new Date().getTime() - 1 * 60 * 60 * 1000)));
		Date end = dtf.parseStrict(dtf.format(new Date(start.getTime() + 3 * 60 * 60 * 1000)));
		options.setStart(start);
		options.setEnd(end);
		timelineActions = new Timeline(data.getJavaScriptObject(), options);
		timelineActions.addSelectHandler(new SelectHandler() {
			@Override
			public void onSelect(SelectEvent event) {
				JsArray<Selection> selection = timelineActions.getSelections();
				if (selection.length() > 0) {
					int row = selection.get(0).getRow();
					Window.alert("You selected row " + row + "\n\n" + "Why?");
					// TODO: do something useful when selecting an item
				}
			}
		});

		panelActions.addStyleName("timelineActions");
		panelActions.clear();
		panelActions.add(timelineActions);

		linkGraphs();
	}

	/**
	 * Set updated data for the timeline displaying the actions
	 * 
	 * @param data
	 */
	private void redrawTimelineActions(JSONArray data) {
		if (timelineActions == null) {
			return;
		}
		Timeline.Options options = Timeline.Options.create();
		options.setShowCustomTime(true);
		options.setWidth("100%");
		options.setHeight("300px");
		options.setStyle(Timeline.Options.STYLE.BOX);
		Date start = dtf.parseStrict(dtf.format(new Date(new Date().getTime() - 1 * 60 * 60 * 1000)));
		Date end = dtf.parseStrict(dtf.format(new Date(start.getTime() + 3 * 60 * 60 * 1000)));
		options.setStart(start);
		options.setEnd(end);
		timelineActions.draw(data.getJavaScriptObject(), options);
	}

	/**
	 * Retrieve historical data with actions form the server
	 * 
	 * @param callback
	 */
	private void retrieveActions(final AsyncCallback<JSONArray> callback) {

		if (!actionsViewCreated) {
			createActionsView();
		}

		final String url = couchDB + "/" + actionsDB + "/_design/"
				+ COUCHDB_ACTIONS_DESIGN + "/_view/" + COUCHDB_ACTIONS_VIEW;

		fetchService.get(url, new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(String result) {
				try {
					JSONArray data = parseActions(result);
					callback.onSuccess(data);
				} catch (Exception err) {
					callback.onFailure(err);
				}
			}
		});
	}

	/*
	 * Retrieve list of server list from the models
	 * 
	 * @param callback
	 */
	private void retrieveServers(final AsyncCallback<Map<String,JSONArray>> callback) {

		if (!serverlistViewCreated) {
			createServerListView();
		}

		final String url = couchDB + "/" + modelsDB + "/_design/"
				+ COUCHDB_SERVERS_DESIGN + "/_view/" + COUCHDB_SERVERLIST_VIEW;

		final String newUrl = (startKey != null)? url + "?" + "startkey" +"=\""+startKey+"\"" + "&limit=1" : url;
		
		System.out.println(" new URL : " +newUrl);
		
		fetchService.get(newUrl, new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				infoNetwork.setText(caught.getMessage());
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(String result) {
				try {
					Map<String, JSONArray> json = parseServers(result);
					callback.onSuccess(json);
				} catch (Exception err) {
					callback.onFailure(err);
				}
			}
		});
	}

	private Map<String,JSONArray> parseServers(String json) {
		Map<String, JSONArray> nodes = new HashMap<String, JSONArray>();
		JSONArray aNode = new JSONArray();
		JSONArray aLink = new JSONArray();
		JSONObject valuejson;
		JSONObject jsonResult = JSONParser.parseStrict(json).isObject();
		JSONArray rows = jsonResult.get("rows").isArray();
		if (rows != null) {
			JSONObject row = rows.get(0).isObject();
			if ((valuejson = row.get("value").isObject()) != null) {
				aNode = valuejson.get("nodes").isArray();
				aLink = valuejson.get("links").isArray();
				nodes.put("nodes", aNode);
				nodes.put("links", aLink);
			}
		}
		return nodes;
	}

	/**
	 * Parse a DataTable with actions from a JSON response from CouchDB
	 * 
	 * @param json
	 * @return
	 */
	private JSONArray parseActions(String json) {
		JSONArray data = new JSONArray();
		JSONObject item;
		JSONObject jsonResult = JSONParser.parseStrict(json).isObject();
		JSONArray rows = jsonResult.get("rows").isArray();
		if (rows != null) {
			for (int i = 0; i < rows.size(); i++) {
				JSONObject row = rows.get(i).isObject();
				JSONObject value = row.get("value").isObject();
				
				Date start = dtf.parse(row.get("key").isString().stringValue());
				long timeZoneOffset = new Date().getTimezoneOffset();
				Date myCurrentZoneTime = new Date(start.getTime()
						- (timeZoneOffset * 60 * 1000));
				
				double powerOn = value.get("PowerOn").isNumber().doubleValue();
				double powerOff = value.get("PowerOff").isNumber()
						.doubleValue();
				double forwardedPowerOff = value.get("ForwardedPowerOff")
						.isNumber().doubleValue();
				double forwardedPowerOn = value.get("ForwardedPowerOn")
						.isNumber().doubleValue();
				double standBy = value.get("StandBy").isNumber().doubleValue();
				double startJob = value.get("StartJob").isNumber()
						.doubleValue();
				double forwardedStandBy = value.get("ForwardedStandBy")
						.isNumber().doubleValue();
				double forwardedStartJob = value.get("ForwardedStartJob")
						.isNumber().doubleValue();
				double moveVM = value.get("MoveVM").isNumber().doubleValue();
				double liveMigrateVM = value.get("LiveMigrateVM").isNumber()
						.doubleValue();
				double forwardedMoveVM = value.get("ForwardedMoveVM")
						.isNumber().doubleValue();
				double forwardedLiveMigrateVM = value.get("ForwardedLiveMigrateVM")
						.isNumber().doubleValue();
				
				int powerOnNumber = (int) powerOn;
				int powerOffNumber = (int) powerOff;
				int forwardedPowerOnNumber = (int) forwardedPowerOn;
				int forwardedPowerOffNumber = (int) forwardedPowerOff;
				int standByNumber = (int) standBy;
				int startJobNumber = (int) startJob;
				int forwardedStandByNumber = (int) forwardedStandBy;
				int forwardedStartJobNumber = (int) forwardedStartJob;
				int moveVMNumber = (int) moveVM;
				int liveMigrateVMNumber = (int) liveMigrateVM;
				int forwardedMoveVMNumber = (int) forwardedMoveVM;
				int forwardedLiveMigrateVMNumber = (int) forwardedLiveMigrateVM;
				
				powerOnNumber += 10;
				powerOffNumber += 10;
				standByNumber += 10;
				startJobNumber += 10;
				moveVMNumber += 10;
				liveMigrateVMNumber += 10;
				
				String content = "";
				
				if (powerOnNumber != 10) {
					content += "<div  style='height:" + powerOnNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin-right:2px;" +
							" position:relative; background-color:#00ff00; font-size:8px;" +
							" ' title='Forwarded Power On Actions :" + forwardedPowerOnNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Power On: " + 
							(powerOnNumber - 10) + "</label>";
				}
				
				if (powerOffNumber != 10) {
					content += "<div style='height:" + powerOffNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin:2px;" +
							" position:relative; background-color:#ff0000; font-size:8px;" +
							" ' title='Forwarded Power Off Actions :" + forwardedPowerOffNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Power Off: " + 
							(powerOffNumber - 10) + "</label>";
				}
				
				if (standByNumber != 10) {
					content += "<div style='height:" + standByNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin:2px;" +
							" position:relative; background-color:#adff2f; font-size:8px;" +
							" ' title='Forwarded Stand By Actions :" + forwardedStandByNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Stand By: " + 
							(standByNumber - 10) + "</label>";
				}
				
				if (startJobNumber != 10) {
					content += "<div style='height:" + startJobNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin:2px;" +
							" position:relative; background-color:#0000ff; font-size:8px;" +
							" ' title='Forwarded Start Job Actions :" + forwardedStartJobNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Start Job: " + 
							(startJobNumber - 10) + "</label>";
				}
				
				if (moveVMNumber != 10) {
					content += "<div style='height:" + moveVMNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin:2px;" +
							" position:relative; background-color:#ffff00; font-size:8px;" +
							" ' title='Forwarded Move VM Actions :" + forwardedMoveVMNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Move VM: " + 
							(moveVMNumber - 10) + "</label>";
				}
				
				if (liveMigrateVMNumber != 10) {
					content += "<div style='height:" + liveMigrateVMNumber + "px;" +
							" width:10px; padding:0px; display:inline-block; margin:2px;" +
							" position:relative; background-color:#ffae42; font-size:8px;" +
							" ' title='Forwarded Live Migrate VM Actions :" + forwardedLiveMigrateVMNumber + "'></div>" +
							"<label style='font-size:8px; margin-right:8px'> Live Migrate VM: " + 
							(liveMigrateVMNumber - 10) + "</label>";
				}
				content += "</br>";
				
				item = new JSONObject();
				item.put("start", new JSONNumber(myCurrentZoneTime.getTime()));
				item.put("content", new JSONString(content));
				data.set(data.size(), item);
			}
		}
		return data;
	}

	/**
	 * Parse the JSON response from CouchDB when checkBox(s) is selected
	 * 
	 * @param json
	 *            ,optionNumber
	 * @return JSONArray containing -key value pair of date and value
	 */
	private JSONArray parseComputedPower(String json, int optionNumber) {
		JSONObject jsonResult = JSONParser.parseStrict(json).isObject();
		JSONArray rows = jsonResult.get("rows").isArray();
		JSONArray data = new JSONArray();

		switch (optionNumber) {

		case 0: { // empty Graph
			data = setValueDataTable(0, rows, 0, false, false, false, false,
					false, false);
			break;
		}
		case 1: { // total ICT - 1 option
			data = setValueDataTable(1, rows, 0, true, false, false, false,
					false, false);
			break;
		}
		case 2: { // ICT site - 1 option
			data = setValueDataTable(0, rows, 1, false, true, false, false,
					false, false);
			break;
		}
		case 3: { // total ICT and sites ICT - 2 options
			data = setValueDataTable(1, rows, 1, true, true, false, false,
					false, false);
			break;
		}
		case 4: { // total power - 1 option
			data = setValueDataTable(1, rows, 0, false, false, true, false,
					false, false);
			break;
		}
		case 5: { // total ICT and total Power - 2 options
			data = setValueDataTable(2, rows, 0, true, false, true, false,
					false, false);
			break;
		}
		case 6: { // ICT sites and total Power - 2 options
			data = setValueDataTable(1, rows, 1, false, true, true, false,
					false, false);
			break;
		}
		case 7: { // total ICT, sites ICT, total power - 3 options
			data = setValueDataTable(2, rows, 1, true, true, true, false,
					false, false);
			break;
		}
		case 8: { // sites Power - 1 options
			data = setValueDataTable(0, rows, 1, false, false, false, true,
					false, false);
			break;
		}
		case 9: { // total ICT, sites Power - 2 options
			data = setValueDataTable(1, rows, 1, true, false, false, true,
					false, false);
			break;
		}
		case 10: { // sites ICT, sites Power - 2 options
			data = setValueDataTable(0, rows, 2, false, true, false, true,
					false, false);
			break;
		}
		case 11: { // Total ICT, sites ICT, sites Power - 3 options
			data = setValueDataTable(1, rows, 2, true, true, false, true,
					false, false);
			break;
		}
		case 12: { // site Power, total Power, - 2 options
			data = setValueDataTable(1, rows, 1, false, false, true, true,
					false, false);
			break;
		}
		case 13: { // site Power, total Power, total ICT - 3 options
			data = setValueDataTable(2, rows, 1, true, false, true, true,
					false, false);
			break;
		}
		case 14: { // site Power, site ICT, total Power - 3 options
			data = setValueDataTable(1, rows, 2, false, true, true, true,
					false, false);
			break;
		}
		case 15: { // Total Power, sites Power, Total ICT, sites ICT - 4 options
			data = setValueDataTable(2, rows, 2, true, true, true, true, false,
					false);
			break;
		}
		case 16: {// total emission
			data = setValueDataTable(1, rows, 0, false, false, false, false,
					true, false);
			break;
		}
		case 32: {// sites emission
			data = setValueDataTable(0, rows, 1, false, false, false, false,
					false, true);
			break;
		}
		case 48: {// total emission and sites emission
			data = setValueDataTable(1, rows, 1, false, false, false, false,
					true, true);
			break;
		}
		}
		return data;
	}

	/**
	 * Creates the DataTable and Graph.Options for the selected option(s)
	 * 
	 * @param numberOfTotalColumns
	 * @param rows
	 * @param numberOfSites
	 * @param totalICT
	 *            1
	 * @param siteICT
	 *            2
	 * @param totalPower
	 *            3
	 * @param sitePower
	 *            4
	 * @param totalEmission
	 *            5
	 * @param siteEmission
	 *            6
	 * @return JSONArray containing key value pair of date and value for the
	 *         Graph
	 */

	private JSONArray setValueDataTable(int numberOfTotalColumns,
			JSONArray rows, int numberOfSites, boolean totalICT,
			boolean siteICT, boolean totalPower, boolean sitePower,
			boolean totalEmission, boolean sitesEmission) {
		String name = null;
		String total_name = null;
		JSONObject dataSetA = new JSONObject();
		JSONObject dataSetB = new JSONObject();
		JSONArray dataA = new JSONArray();
		JSONArray dataB = new JSONArray();
		JSONArray data = new JSONArray();

		HashMap<String, List<JSONObject>> aDataSet = new HashMap<String, List<JSONObject>>();
		if (rows != null) {
			for (int i = 0; i < rows.size(); i++) {
				JSONObject row = rows.get(i).isObject();
				JSONObject value = row.get("value").isObject();
				Date start = dtf.parse(row.get("key").isString().stringValue());
				long timeZoneOffset = new Date().getTimezoneOffset();
				Date myCurrentZoneTime = new Date(start.getTime()
						- (timeZoneOffset * 60 * 1000));

				if ((numberOfTotalColumns > 1) && (totalICT) && (totalPower)) {
					double total_ICT = value.get("Total ICT").isNumber()
							.doubleValue();
					JSONObject pointA = new JSONObject();
					pointA.put("date",
							new JSONNumber(myCurrentZoneTime.getTime()));
					pointA.put("value", new JSONNumber(total_ICT));
					dataA.set(i, pointA);

					double total_power = value.get("Total Power").isNumber()
							.doubleValue();
					JSONObject pointB = new JSONObject();
					pointB.put("date",
							new JSONNumber(myCurrentZoneTime.getTime()));
					pointB.put("value", new JSONNumber(total_power));
					dataB.set(i, pointB);
				}
				if ((numberOfTotalColumns == 1)) {
					name = totalICT ? "Total ICT" : totalPower ? "Total Power"
							: totalEmission ? "total emission" : "";
					double total = value.get(name).isNumber().doubleValue();
					JSONObject pointA = new JSONObject();
					pointA.put("date",
							new JSONNumber(myCurrentZoneTime.getTime()));
					pointA.put("value", new JSONNumber(total));
					dataA.set(i, pointA);
				}
				if ((numberOfSites > 1) && (siteICT) && (sitePower)) {
					JSONArray sitesICT = value.get("sites ICT").isArray();
					JSONArray sitesPower = value.get("sites power").isArray();
					name = "Power Site [W] ";
					for (int j = 0; j < sitesPower.size(); j++) {
						JSONObject pointB = new JSONObject();
						pointB.put("date",
								new JSONNumber(myCurrentZoneTime.getTime()));
						pointB.put("value", new JSONNumber(sitesPower.get(j)
								.isNumber().doubleValue()));
						String key = name + " " + (j + 1);
						List<JSONObject> myList = aDataSet.get(key);
						if (myList == null) {
							myList = new ArrayList<JSONObject>();
							aDataSet.put(key, myList);
						}
						myList.add(pointB);
					}
					name = "ICT Power Site [W]";
					for (int j = 0; j < sitesICT.size(); j++) {
						JSONObject pointB = new JSONObject();
						pointB.put("date",
								new JSONNumber(myCurrentZoneTime.getTime()));
						pointB.put("value", new JSONNumber(sitesICT.get(j)
								.isNumber().doubleValue()));
						String key = name + " " + (j + 1);
						List<JSONObject> myList = aDataSet.get(key);
						if (myList == null) {
							myList = new ArrayList<JSONObject>();
							aDataSet.put(key, myList);
						}
						myList.add(pointB);
					}
				} else if ((numberOfSites == 1)) {
					name = siteICT ? "sites ICT" : sitePower ? "sites power"
							: sitesEmission ? "sites emission" : "";
					JSONArray sites = value.get(name).isArray();
					name = siteICT ? "ICT Power Site [W] "
							: sitePower ? "Power Site [W] "
									: sitesEmission ? "CO2 Emission Site "
											: name;
					for (int j = 0; j < sites.size(); j++) {
						JSONObject pointB = new JSONObject();
						pointB.put("date",
								new JSONNumber(myCurrentZoneTime.getTime()));
						pointB.put("value", new JSONNumber(sites.get(j)
								.isNumber().doubleValue()));
						String key = name + " " + (j + 1);
						List<JSONObject> myList = aDataSet.get(key);
						if (myList == null) {
							myList = new ArrayList<JSONObject>();
							aDataSet.put(key, myList);
						}
						myList.add(pointB);
					}
				}
			}// loop over n rows
		}// if rows are not zero.
		if (numberOfTotalColumns > 1) {
			dataSetB.put("label", new JSONString("Total Sites Power [W]"));
			dataSetB.put("data", dataB);
			data.set(1, dataSetB);
			dataSetA.put("label", new JSONString("Total ICT Power [W]"));
			dataSetA.put("data", dataA);
			data.set(0, dataSetA);
		} else if (numberOfTotalColumns == 1) {
			total_name = totalICT ? "Total ICT Power [W] "
					: totalPower ? "Total Sites Power [W]"
							: totalEmission ? "Total CO2 Emission " : "";
			dataSetA.put("label", new JSONString(total_name));
			dataSetA.put("data", dataA);
			data.set(0, dataSetA);
		}

		Iterator<Map.Entry<String, List<JSONObject>>> theDataMap = aDataSet
				.entrySet().iterator();
		while (theDataMap.hasNext()) {
			Map.Entry<String, List<JSONObject>> pairs = (Map.Entry<String, List<JSONObject>>) theDataMap
					.next();
			JSONObject dataset = new JSONObject();
			dataset.put("label", new JSONString(pairs.getKey()));
			dataset.put("data",
					JSONParser.parseStrict(pairs.getValue().toString()));
			data.set(data.size(), dataset);
		}
		return data;
	}

	/**
	 * Link the graphs together such that they move synchronously
	 */
	private boolean linked = false;

	private void linkGraphs() {
		if (graphComputedPower != null && timelineActions != null
				&& linked == false) {
			graphComputedPower.addRangeChangeHandler(new RangeChangeHandler() {
				@Override
				public void onRangeChange(RangeChangeEvent event) {
					timelineActions.setVisibleChartRange(event.getStart(),
							event.getEnd());
				}
			});

			timelineActions.addRangeChangeHandler(new RangeChangeHandler() {
				@Override
				public void onRangeChange(RangeChangeEvent event) {
					graphComputedPower.setVisibleChartRange(event.getStart(),
							event.getEnd());
					graphComputedPower.redraw();
					//graphComputedPower.
					// in the case of the graph, we need to redraw...
					// yeah, I know, sorry for the inconsistency
				}
			});
			timelineActions.addTimeChangedHandler(new TimeChangedHandler(){
				@Override
				public void onTimeChanged(TimeChangedEvent event) {
					startKey = dtf.format(new Date(event.getTime().getTime()  + (event.getTime().getTimezoneOffset() * 60 * 1000))); 
					button.setEnabled(true);
					System.out.println(" time returned : " + event.getTime());
					System.out.println(" GMT time (StartKEY) : " + startKey);
					
				}
				
			});
			//send time and range to topology tab
			// match initial range
			DateRange range = graphComputedPower.getVisibleChartRange();
			timelineActions.setVisibleChartRange(range.getStart(),
					range.getEnd());

			linked = true;
		}
	}

	
	/**
	 * The refresh timer will periodically retrieve updated data from the server
	 * and refresh the corresponding graphs
	 */
	private Timer timerRefresh = new Timer() {
		@Override
		public void run() {
			if (!isVisible()) {
				return;
			}

			infoComputedPower.setText("Updating...");
			infoComputedPower.setStyleName("info");

			retrieveComputedPower(new AsyncCallback<JSONArray>() {
				@Override
				public void onFailure(Throwable caught) {
					infoComputedPower.setText(caught.getMessage());
					infoComputedPower.setStyleName("info-error");
				}

				@Override
				public void onSuccess(JSONArray data) {
					System.out.println((new Date())
							+ ": computed power retrieved...");
					if (graphComputedPower == null) {
						// System.out.println(dfm.format(new Date()) +
						// ": creating graph for first time...");
						createGraphComputedPower(data);
						// System.out.println(dfm.format(new Date()) +
						// ": graph created...");
					} else {
						// System.out.println(dfm.format(new Date()) +
						// ": redrawning graph...");
						redrawGraphComputedPower(data);
						// System.out.println(dfm.format(new Date()) +
						// ": graph created...");
					}

					infoComputedPower.setText("Updated " + new Date());
					infoComputedPower.setStyleName("info");
				}
			});

			infoActions.setText("Updating...");
			infoActions.setStyleName("info");

			// System.out.println(dfm.format(new Date()) +
			// ": retrieving actions...");
			retrieveActions(new AsyncCallback<JSONArray>() {
				@Override
				public void onFailure(Throwable caught) {
					infoActions.setText(caught.getMessage());
					infoActions.setStyleName("info-error");
				}

				@Override
				public void onSuccess(JSONArray data) {
					// System.out.println(dfm.format(new Date()) +
					// ": actions retrieved...");
					if (timelineActions == null) {
						// System.out.println(dfm.format(new Date()) +
						// ": creating timeline for first time...");
						createTimelineActions(data);
						// System.out.println(dfm.format(new Date()) +
						// ": timeline created...");
					} else {
						// System.out.println(dfm.format(new Date()) +
						// ": redrawning timeline...");
						redrawTimelineActions(data);
						// System.out.println(dfm.format(new Date()) +
						// ": timeline redrawed...");
					}

					infoActions.setText("Updated " + new Date());
					infoActions.setStyleName("info");
				}
			});

		}
	};

	@Override
	public void onResize() {
		super.onResize();

		// graphs need to be redrawn on resize
		if (graphComputedPower != null) {
			graphComputedPower.redraw();
		}
		if (timelineActions != null) {
			timelineActions.redraw();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		// TODO this is not working as expected...
		// when changing from statistics tab to another tab it sends double
		// fetch services
		// when changing back to statistics it also sends double fetch services
		super.setVisible(visible);

		// force a resize of the graphs
		if (visible) {
			onResize();

			// force an update of the data now
			// System.out.println(dfm.format(new Date()) +
			// ": running timer within setVisible()...");
			timerRefresh.run();
		}
	}
}
