package f4g.f4gGui.gui.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @class F4gGuiXmlEditor
 * 
 * This class loads the xamplet xml editor in a GWT application, with the 
 * f4g-model.xsd scheme.
 * 
 * The class supposes that the files xamplet.jar and f4g-model.xsd are located
 * in the root folder of the GWT application (in the folder war) 
 * 
 * @author jos
 *
 */
public class XmlEditor extends SimplePanel {
	// TODO: create a unique id for each created editor, such that we can have multiple editors opened
	// TODO: put schemas location in configuration file
	// TODO: get schemas from server????
	// The schemas must be located in the following folder in the war directory
	// This is client side!
	private final String DIR_SCHEMAS = "schemas";

	private final String DOM_ELEMENT_ID = "FXApplet";
	private String content = "";
	private HTML html = new HTML();
	
	XmlEditor () {
		create();
	}
	
	XmlEditor (String content) {
		this.content = content;
		create();
	}

	/**
	 * Create the xml editor applet
	 */
	void create() {
		String schema = DIR_SCHEMAS + "/" + getXmlSchema(content);
		
		init(DOM_ELEMENT_ID, content);
		
		// TODO: can we get a warning when the xml editor could not load the 
		// file correctly?
		
		this.setStyleName("configXmlEditor");
		this.add(html);
		html.setWidth("100%");
		html.setHeight("100%");
		html.setHTML(
			"<applet codebase='./' code='com.fg.fxapplet.FXApplet.class'" + 
			"	ARCHIVE='xamplet.jar' id='" + DOM_ELEMENT_ID + "'" +
			"	 width='100%' height='100%' hspace='0' vspace='0' MAYSCRIPT>" +
			"	  <param name='ON_START' value='onAppletStarted'>" +
			"	  <param name='XML_SCHEMA' value='" + schema + "'>" +
			"	  <param name='XML_SOURCE' value=''>" +
			"	  <param name='DOC_NAME' value=''>" +
			"	  <param name='BASE_URL' value=''>" +
			"	</applet>"
			);
	}

	/**
	 * Create javascript needed to load the editor
	 * @param elementId
	 * @param xml
	 */
	private native void init(String elementId, String xml)  /*-{
		$wnd.appletStarted = false;

        // when the applet is started (parameter "ON_START"), it executes
        // the function onAppletStarted
		$wnd.onAppletStarted = function(editor)
		{
		  $wnd.appletStarted = true;
		  
		  var FXApplet = $wnd.document.getElementById(elementId);
		  //FXApplet.setXMLSchema("f4g-model.xsd"); // TODO: cleanup
		  FXApplet.setXMLSource('');
		  FXApplet.setBaseURL('');
		  FXApplet.setDocName('');
		  // TODO
		  //FXApplet.setElement('FIT4Green');  // needed for a new file
		  //FXApplet.loadXMLDocument(null);    // for a new file  
		  FXApplet.loadXMLDocument(xml);
		}
	}-*/;

	
    /**
     * Retrieve the Schema name from the xml contents. 
     * The function reads the url in the parameter targetNamespace
     * and returns the last directory in this url as a xsd filename
     * For example when 
     * targetNamespace="http://www.fit4green.eu/schemes/MetaModel"
     * the function returns "MetaModel.xsd"	
	 * @param xml     The contents of an xml file
	 * @return        The schema name of the xml file, or an empty string
	 *                when not found
	 */
    public static String getXmlSchema (final String xml) {
    	final String search = "\"http://www.fit4green.eu/schemes/";
    	int url = xml.indexOf(search);
    	int schemaStart = (url >= 0) ? url + search.length() : -1;
    	int schemaEnd   = (url >= 0) ? xml.indexOf("\"", schemaStart+1) : -1;
    	
    	if (schemaStart >= 0 && schemaEnd > schemaStart) {
	    	String schema = xml.substring(schemaStart, schemaEnd);
	    	return schema + ".xsd";
    	}

 	    // not found
   	    return "";	
    }	
	
	/**
	 * Load xml into the xml editor 
	 * @param xml		The text of an Fit4Green model in xml
	 */	
	public void setText(String xml) {
		String schema = DIR_SCHEMAS + "/" + getXmlSchema(xml);
		setText(DOM_ELEMENT_ID, schema, xml);
	}
	
	/**
	 * Load xml into an xml editor 
	 * @param elementId The id of the DOM applet element
	 * @param schema	The full path of the schema of the xml file, for example 
	 *                  "schemas/MetaModel.xsd"
	 * @param xml		The text of an Fit4Green model in xml
	 */
	private native void setText(String elementId, String schema, String xml) /*-{
		if (!$wnd.appletStarted) {
			//throw "Error: XML editor is not yet loaded";  
			// TODO: test if throwing error works
			return;
		}
	
	  	$wnd.document.getElementById(elementId).setXMLSchema(schema);
		$wnd.document.getElementById(elementId).loadXMLDocument(xml);    
	}-*/;
	
	/**
	 * Retrieve xml as text from the xml editor 
	 * @return xml		The text of an Fit4Green model in xml
	 */	
	public String getText() {
		return getText(DOM_ELEMENT_ID);
	}
	
	/**
	 * Retrieve xml as text from a xml editor 
	 * @param elementId The id of the DOM applet element
	 * @return xml		The text of an Fit4Green model in xml
	 */
	private native String getText(String elementId) /*-{
		if (!$wnd.appletStarted) {
			//throw "Error: XML editor is not yet loaded";  
			// TODO: test if throwing error works
			
			return "";
		}
		
		var xml = $wnd.document.getElementById(elementId).getXMLDocumentAsText();
		return xml;		
	}-*/;
		
}