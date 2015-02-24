/**
* ============================== Header ============================== 
* file:          TestMarshalling.java
* project:       FIT4Green/Manager
* created:       Sep 29, 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-09-16 16:41:55 +0200 (Fri, 16 Sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 780 $
* 
* short description:
*   To test marshalling and conversion to JSON
* ============================= /Header ==============================
*/
package f4g.manager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import f4g.commons.core.Configuration;
import f4g.commons.core.Constants;
import f4g.manager.couchDB.ConvertToJSON;
import f4g.manager.couchDB.DataBase;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.metamodel.CUE;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.PUE;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.Site;

import com.rits.cloning.Cloner;

/**
 * To test marshalling and conversion to JSON * 
 *
 * @author Vasiliki Georgiadou
 */
public class TestMarshalling {
	
	private static void dispatch (ActionRequest actionRequest, ArrayList actions) {
		
		ConvertToJSON con = new ConvertToJSON();
		String data;
		
		ActionRequest.ActionList updatedActionList = new ActionRequest.ActionList(actions);
		actionRequest.setActionList(updatedActionList);
		
		System.out.println("Converting to JSON updated action request...");
		data = con.convert(actionRequest);
		
		System.out.println(data);
		
	}
	
	private static ActionRequest createActions () {
		
		PowerOffAction off = new PowerOffAction();
		off.setID(UUID.randomUUID().toString());
		off.setFrameworkName("a-com");
		off.setNodeName("a-node");
		
		PowerOffAction off1 = new PowerOffAction();
		off1.setID(UUID.randomUUID().toString());
		off1.setFrameworkName("another-com");
		off1.setNodeName("another-node");
		
		PowerOnAction on = new PowerOnAction();
		on.setID(UUID.randomUUID().toString());
		on.setFrameworkName("a-com");
		on.setNodeName("yet-another-node");
		
		ActionRequest actionRequest = new ActionRequest();
		ActionRequest.ActionList actionList = new ActionRequest.ActionList();
		
		f4g.schemas.java.actions.ObjectFactory actionFactory = new f4g.schemas.java.actions.ObjectFactory();
		
		actionList.getAction().add(actionFactory.createPowerOff(off));
		actionList.getAction().add(actionFactory.createPowerOff(off1));
		actionList.getAction().add(actionFactory.createPowerOn(on));
		
		actionRequest.setIsAutomatic(true);
		try {
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			actionRequest.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		actionRequest.setActionList(actionList);
		
		return actionRequest;
	
	}

	/**
	 * Main
	 * 
	 * @param args
	 *
	 * @author vasiliki
	 * @throws DatatypeConfigurationException 
	 */
	public static void main(String[] args) throws DatatypeConfigurationException {
		
		System.out.println("Starting...");
		
//		System.out.println("Creating dummy action request...");
//		ActionRequest actionRequest = createActions();
//		
//		System.out.println("Converting action request to JSON...");
//		ConvertToJSON con = new ConvertToJSON();
//		String data = con.convert(actionRequest);
//		System.out.println(data);
		
		DataBase db = new DataBase();
		db.setUrl("http://localhost:5984");
		
//		System.out.println("Creating database \"actions\"...");
//		int rc = db.create("actions");
//		
//		if (rc == 412) {
//			System.out.println("Database actions already exists");
//		} else if ( rc == 201 ) {
//			System.out.println("Database actions created");
//		} else {
//			System.out.println("Error while creating database actions; " + db.getMessage());
//		}
//		
//		String id = UUID.randomUUID().toString();
//		
//		if (data == null) {
//			System.out.println("No data; exiting");
//			System.exit(0);
//		}
//		
//		System.out.println("Creating document on database...");
//		rc = db.createDocument("actions", id, data);
//		
//		if (rc == 201) {
//			System.out.println("Document " + id + " created");
//		} else {
//			System.out.println("Document " + id + " was not created; " +
//					"HTPP PUT request return code: " + rc);
//		}
		
//		System.out.println("Creating design document \"utils\" on models database...");
//		String name = "models";
//		String designID = "/_design/utils";
//		
//		String view1 = "sortByDatetime";
//		String map1 = 		
//			"function(doc) {" + "\n" +  
//			"  if (doc.Datetime) {" + "\n" + 
//			"    emit(doc.Datetime,doc._rev);" + "\n" +  
//			"  }" + "\n" + 
//			"}";
//		
//		String view2 = "retrieveFrameworkStatus";
//		String map2 =
//			"function(doc) {" + "\n" +
//			"  var data = \\\"\\\";" + "\n" +
//			"  var sites = doc[\\\"ns2:Site\\\"];" + "\n" +
//			"  if (sites.length == undefined) {" + "\n" +
//			"    sites = [sites];" + "\n" +
//			"  }" + "\n" +
//			"  for (var i = 0; i < sites.length; i++) {" + "\n" + 
//			"   var datacentres = sites[i][\\\"ns2:Datacenter\\\"];" + "\n" +
//			"    if (datacentres.length == undefined) {" + "\n" +
//			"      datacentres = [datacentres];" + "\n" +
//			"    }" + "\n" +
//			"    for (var j = 0; j < datacentres.length; j++) {" + "\n" +
//			"      var frameworks = datacentres[j][\\\"ns2:FrameworkCapabilities\\\"];" + "\n" +
//			"      if (frameworks.length === undefined) {" + "\n" +
//			"        frameworks = [frameworks];" + "\n" +
//			"      }" + "\n" +
//			"      for (var k = 0; k < frameworks.length; k++) {" + "\n" +
//			"        data = data + frameworks[k].frameworkName + \\\";\\\" + frameworks[k].status + \\\",\\\";" + "\n" +
//			"      }" + "\n" +
//			"    }" + "\n" +
//			"  }" + "\n" +
//			"  emit (doc.Datetime,data);" + "\n" +
//			"}";
//		
//		String body = 		
//			"{" + "\n" +
//			"   \"language\": \"javascript\"," + "\n" +
//			"   \"views\": {" + "\n" +
//			"      \"" + view1 + "\": {" + "\n" +
//			"         \"map\": \"" + map1 + "\"" + "\n" +
//			"      }," + "\n" +
//			"      \"" + view2 + "\": {" + "\n" +
//			"         \"map\": \"" + map2 + "\"" + "\n" +
//			"      }" + "\n" +
//			"   }" + "\n" +
//			"}";
//		
//		db.createDocument(name, designID, body);
//		
//		System.out.println("Retrieving latest framework status...");
//		
//		String response = db.query("models", "utils", "retrieveFrameworkStatus", 
//				"limit=1&descending=true");
//		
//		String[] tokens = response.split("[{}:\",]+");
//	
//		for (int i=0; i<tokens.length; i++) {
//	
//		    if (tokens[i].compareTo("value") == 0) {
//		    	 
//		    	for (int j = i+1; j < tokens.length; j++) {
//		    		String[] data = tokens[j].split(";");
//		    		
//		    		for (int k = 0; k < data.length; k++) {
//		    			if (data[k].compareTo("ComHP") == 0) {
//		    				System.out.println("Framework " + data[k] + " status: " + data[k+1]);
//		    			}
//		    		}
//		    		 
//		    	}
//		    	i = tokens.length;
//		    }
//		}

		
//		System.out.println("Creating design document on actions database...");
//		String name = "actions";
//		String designID = "/_design/utils"; 
//		
//		String view = "sortByDatetime";
//		String map = 		
//			"function(doc) {" + "\n" +  
//			"  if (doc.Datetime) {" + "\n" + 
//			"    emit(doc.Datetime,doc._rev);" + "\n" +  
//			"  }" + "\n" + 
//			"}";
//		String body = 		
//			"{" + "\n" +
//			"   \"language\": \"javascript\"," + "\n" +
//			"   \"views\": {" + "\n" +
//			"      \"" + view + "\": {" + "\n" +
//			"         \"map\": \"" + map + "\"" + "\n" +
//			"      }" + "\n" +
//			"   }" + "\n" +
//			"}";
//		
//		db.createDocument(name, designID, body);
//		
		int maxDatabaseSize = 40000;  // 40 KB
		
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

		
		System.out.println(dfm.format(new Date()) + ": About to retrieve properties");
		db.retrieveProperties("actions");
		
		System.out.println(dfm.format(new Date()) + ": Retrieved properties");
		
		if (db.getProperties().getDiskSize() >= maxDatabaseSize) {
			System.out.println(dfm.format(new Date()) + ": About to query database");
			String response = db.query("actions", "controller_utils", "sortByDatetime", "limit=1");
			
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
			
			System.out.println(dfm.format(new Date()) + ": About to delete document: \"" + delDocID + "\"");
			
			db.deleteDocument("actions", delDocID, rev);
			
			System.out.println(dfm.format(new Date()) + ": Deleted document: \"" + delDocID + "\"");
			
			System.out.println(dfm.format(new Date()) + ": Running compaction...");
			db.compact("actions");
			
			System.out.println(dfm.format(new Date()) + ": Compaction on-going...");

			
//			db.retrieveProperties("actions");
//			System.out.println("DB size " + db.getProperties().getDiskSize() + " bytes " +
//					" doc count " + db.getProperties().getDocCount() +
//					" doc del count " + db.getProperties().getDocDelCount());
//			
		} else {
			System.out.println("Nothing to delete! Database size: " + db.getProperties().getDiskSize());
		}
//		
		
		
//		System.out.println("Emulating the controller...");
//		//Group actions by target Com
//		HashMap groupedActions = new HashMap();
//		for (int i = 0; i < actionList.getAction().size(); i++) {
//			String frameworkName = actionList.getAction().get(i).getValue().getFrameworkName();
//			if(groupedActions.get(frameworkName) == null){
//				groupedActions.put(frameworkName, new ArrayList());
//			}
//			((ArrayList)groupedActions.get(frameworkName)).add(actionList.getAction().get(i));
//		}
//		
//		Iterator iter = groupedActions.keySet().iterator();
//
//		System.out.println("Dispatching actions to the responsible Com components...");
//		
//		while(iter.hasNext()){
//			String comName = (String)iter.next();
//			ArrayList actions = (ArrayList)groupedActions.get(comName);
//			
//			JAXBElement<? extends AbstractBaseAction> elem;
//			Iterator iter1 = actions.iterator();
//			
//			while (iter1.hasNext()) {
//
//				elem = (JAXBElement<? extends AbstractBaseAction>) iter1.next();
//				Object action = elem.getValue();
//				action = elem.getValue().getClass().cast(action);
//				((AbstractBaseAction)action).setForwarded(true);
//			}
//			
//			dispatch(actionRequest,actions);
//		}
			
		
//		System.out.println("Creating dummy model...");
//		FIT4Green model = new FIT4Green();
//		
//		try {
//			TimeZone gmt = TimeZone.getTimeZone("GMT");
//			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
//			model.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
//		} catch (DatatypeConfigurationException e) {
//			e.printStackTrace();
//		}
//		
//		Site site = new Site();
//		Power p = new Power(0.0);
//		CUE cue = new CUE(0.5);
//		PUE pue = new PUE(1.5);
//		site.setComputedPower(p);
//		site.setCUE(cue);
//		site.setPUE(pue);
//		model.getSite().add(site);
//		
//		id = UUID.randomUUID().toString();
//		
//		System.out.println("Converting model java object to JSON...");
//		data = con.convert(model);
//		
//		if (data == null) {
//			System.out.println("No data; exiting");
//			System.exit(0);
//		}
//		
//		System.out.println("Creating document on database...");
//		rc = db.createDocument("models", id, data);
//		
//		if (rc == 201) {
//			System.out.println("Document " + id + " created");
//		} else {
//			System.out.println("Document " + id + " was not created; " +
//					"HTPP PUT request return code: " + rc);
//		}
//		
		System.out.println("Bye bye!");


	}
}
