/**
* ============================== Header ============================== 
* file:          Monitor.java
* project:       FIT4Green/Manager
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2012-06-21 16:44:15 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1500 $
* 
* short description:
*   Implements the Monitor component for the FIT4Green framework.
* ============================= /Header ==============================
*/
package f4g.manager.monitor;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.InputStream;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.log4j.Logger;

import f4g.commons.com.ICom;
import f4g.commons.monitor.IMonitor;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.com.util.PowerData;
import f4g.commons.core.Configuration;
import f4g.commons.core.Constants;
import f4g.commons.core.IMain;
import f4g.manager.couchDB.ConvertToJSON;
import f4g.manager.couchDB.DataBase;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.FrameworkStatus;
import f4g.schemas.java.metamodel.ObjectFactory;
import f4g.schemas.java.metamodel.Site;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.commons.util.Util;
import f4g.commons.util.JXPathCustomFactory;

import com.rits.cloning.Cloner;

/**
 * Implements the Monitor component for the FIT4Green framework.
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 */
public class Monitor implements IMonitor {
	
	static Logger log = Logger.getLogger(Monitor.class.getName()); 
	
	FIT4Green model = null;
	
	private HashMap<String, Object> mapping = new HashMap<String, Object>();
	
	private IMain main = null;
	
	private Timer requestTimer = null;
	
	private Timer computeTimer = null;
	private PowerData computedPower;
	
	private int maxSize;
	
	private final String DESIGN_ID = "_design/monitor_utils";
	private final String DESIGN_NAME = "monitor_utils";
	private final String SORT_BY_DATETIME_VIEW = "sortByDatetime";
	private final String RETRIEVE_FRAMEWORK_STATUS_VIEW = "retrieveFrameworkStatus";
	
	public Monitor(IMain main) {
	
		this.main = main;
		
		String currentFilePath = Constants.F4G_MODEL_FILE_PATH;
		String currentModelPathName = Configuration.get(currentFilePath);
		boolean isModelLoaded = loadModel(currentModelPathName);
		if(!isModelLoaded){
			log.error("Error while loading model; exiting");
			System.exit(0);
		}
		
		createMapping();
		
		scheduleRequestTask();
		scheduleComputeTask();
		
		// read maximum database size from configuration file in GB
		String maxSizeString = Configuration.get(Constants.MAX_SIZE);
		// 1 gigabyte = 1 073 741 824 bytes
		// here 1 000 000 000 is preferred just to include some buffer
		maxSize = Integer.parseInt(maxSizeString) * 1000000000;
		
		// create model database if it does not already exist
		DataBase db = new DataBase();
		db.setUrl(Configuration.get(Constants.DB_URL));
		
		log.debug("About to create database " + Configuration.get(Constants.MODELS_DB_NAME));
		
		int rc = db.create(Configuration.get(Constants.MODELS_DB_NAME));
		
		if (rc == 412) {
			log.debug("Database \"" + Configuration.get(Constants.MODELS_DB_NAME) + "\" already exists");
		} else if ( rc == 201 ) {
			log.debug("Database \"" + Configuration.get(Constants.MODELS_DB_NAME) + "\" created");
		} else {
			log.error("Error while creating database \"" + Configuration.get(Constants.MODELS_DB_NAME)	+  "\"; " + db.getMessage());
		}
		
		// create design document containing monitor's utility views
		if (rc == 201 || rc == 412) {
			String body = createDesignBody();
			db.createDocument(Configuration.get(Constants.MODELS_DB_NAME), DESIGN_ID, body);
		} else {
			log.error("Error while creating design document \"" + DESIGN_ID +  "\"; " + db.getMessage());
		}

	}
	
	/**
	 * Loads the FIT4Green model from an XML file and transforms it into an object 
	 * hierarchy representation.
	 * 
	 * @param modelPathName path to the XML model file
	 * @return true if success, false otherwise
	 * 
	 * @author FIT4Green
	 */
	@Override
	public boolean loadModel(String modelPathName) {
		
		InputStream isModel = 
			this.getClass().getClassLoader().getResourceAsStream(modelPathName);
		
		log.debug("modelPathName: " + modelPathName + ", isModel: " + isModel);
		
		JAXBElement<?> poElement = null;
		try {
			// create an Unmarshaller
			Unmarshaller u = Util.getJaxbContext().createUnmarshaller();

			// ****** VALIDATION ******
			URL url = 
				this.getClass().getClassLoader().getResource("schema/MetaModel.xsd");
			
			log.debug("URL: " + url);
			
			SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			try {
				Schema schema = sf.newSchema(url);
				u.setSchema(schema);
				u.setEventHandler(new ValidationEventHandler() {
					// allow unmarshalling to continue even if there are errors
					@Override
					public boolean handleEvent(ValidationEvent ve) {
						// ignore warnings
						if (ve.getSeverity() != ValidationEvent.WARNING) {
							ValidationEventLocator vel = ve.getLocator();
							log.warn("Line:Col["
									+ vel.getLineNumber() + ":"
									+ vel.getColumnNumber() + "]:"
									+ ve.getMessage());
						}
						return true;
					}
				});
			} catch (org.xml.sax.SAXException se) {
				log.error("Unable to validate due to following error: ", se );
			}
			// *********************************

			// unmarshal an XML document into a tree of Java content
			// objects composed of classes from the "f4gschema" package.
			poElement = (JAXBElement<?>) u.unmarshal(isModel);
			
			model = (FIT4Green) poElement.getValue();
			

		} catch (JAXBException je) {
			log.error(je);
			return false;
		}
		
		return true;
	}

	/**
	 * Updates a node on behalf of a COM object.
	 * Synchronized with respect to {@link #getModelCopy()}.

	 * @param key key of the mapping for the COM object
	 * @param comObject targeted COM object
	 * 
	 * @author FIT4Green, Vasiliki Georgiadou
	 */
	@Override
	public synchronized boolean updateNode(String key, ICom comObject) {
		long start = System.currentTimeMillis();

		Object obj = mapping.get(key);
		boolean res = comObject.executeUpdate(key, obj);
		log.debug("Time Metric: monitor updateNode took " + (System.currentTimeMillis()-start) + " ms.");
		return res;

	}

	/**
	 * Gets a deep copy of the FIT4Green model, provided that it has been updated
	 * by a COM component.
	 * Synchronized with respect to {@link #updateNode(String, ICom)}.
	 * 
	 * @return the object representation of the FIT4Green model
	 * 
	 * @author FIT4Green, Vasiliki Georgiadou
	 */
	@Override
	public synchronized FIT4Green getModelCopy() {
		// This method returns a deep copy (using com.rits.cloning.Cloner deep clone)

		Cloner cloner=new Cloner();
		FIT4Green modelClone = cloner.deepClone(model);

		return modelClone;
	}
	
	/**
	 * Allows to update a node on behalf of a COM object. 
	 */
	@Override
	public synchronized boolean simpleUpdateNode(String key, ComOperationCollector operations) {
		long start = System.currentTimeMillis();
		Object obj = mapping.get(key);
		
		log.debug("Performing SIMPLE updates on object:");
		log.debug("\t key: " + key);
		log.debug("\t obj: " + obj);
		
		ComOperation[] operationSet = (ComOperation[])operations.getOperations().toArray(new ComOperation[0]);
		for(int i=0; i<operationSet.length; i++){
			ComOperation operation = operationSet[i];
			log.debug("Processing operation: " + operation.getType() + ", " + operation.getExpression() + ", " + (operation.isObjectValue()?operation.getObjValue():operation.getValue()));
			if(operation.getType().equals(ComOperation.TYPE_UPDATE)){
				JXPathContext context = JXPathContext.newContext(obj);
				context.setFactory(new JXPathCustomFactory());
					try {
						if(!operation.isObjectValue()){
							context.setValue(operation.getExpression()+"/value", operation.getValue());							
						} else {
							context.setValue(operation.getExpression(), operation.getObjValue());
						}
					} catch (JXPathException e) {
						//e.printStackTrace();
						log.warn("Error in direct setting. Trying to create the missing path...");
						try {
							context.createPathAndSetValue(operation.getExpression()+"/value", operation.getValue());
							log.debug("...done!");
						} catch (Exception e1) {
							e1.printStackTrace();
							log.error("Unable to create new path and set  value");
						}
					}
			} else {
				log.warn("Skipping not supported operation: " + operation.getType());
			}
			log.debug("Time Metric: simple updates cycle took " + (System.currentTimeMillis()-start) + " ms.");
				
		}
		//logModel();
		return true;
	}

	/**
	 * Provides the set of all the nodes in the model which are handled by a given Com.
	 *  
	 * @param comName the name of the Com
	 * @return a map of all the objects handled by the 'comName' Com. 
	 */
	@Override
	public HashMap getMonitoredObjectsMap(String comName) {
		
		HashMap<String, String> monitoredObjectsMap = new HashMap<String, String>();
		
		Object[] keys = mapping.keySet().toArray();
		
		for(int i=0; i<keys.length; i++){
			if(((String)keys[i]).contains(comName)){
				
				monitoredObjectsMap.put((String)keys[i], mapping.get(keys[i]).getClass().getCanonicalName());
				log.debug("Inserted elem: " + keys[i] + ", " + monitoredObjectsMap.get(keys[i]));
			}
		}
		
		return monitoredObjectsMap;
	}
		
	/**
	 * Provides the set of all the nodes in the model which are handled by a given Com.
	 *  
	 * @param comName the name of the Com
	 * @return a map of all the objects handled by the 'comName' Com. 
	 */
	@Override
	public HashMap getMonitoredObjectsCopy(String comName) {
		
		HashMap<String, Object> monitoredObjectsCopy = new HashMap<String, Object>();
		
		Object[] keys = mapping.keySet().toArray();
		
		Cloner cloner = new Cloner();
		
		for(int i=0; i<keys.length; i++){
			if(((String)keys[i]).contains(comName)){
				
				monitoredObjectsCopy.put((String)keys[i], cloner.deepClone(mapping.get(keys[i])));
				log.debug("Inserted elem: " + keys[i] + ", " + monitoredObjectsCopy.get(keys[i]));
			}
		}
		
		return monitoredObjectsCopy;
	}
	
	/**
	 * Allows a component to request a resource allocation. The Monitor will forward 
	 * the request to the Optimizer.
	 * It is invoked by the Com components.
	 * 
	 * @param allocationRequest data structure containing the f4g model instance and the 
	 * specification of the resource to allocate 
	 * @return a ResourceAllocationResponse object containing the results of the allocation
	 * request
	 */
	@Override
	public AllocationResponse allocateResource(
			AllocationRequest allocationRequest) {
		
		long start = System.currentTimeMillis();
		
		log.debug("In allocateRequest()");
		
		FIT4Green modelCopy = getModelCopy();
		log.debug("Metric: got model copy in " + (System.currentTimeMillis() - start) + " ms.");
		
		start = System.currentTimeMillis();
		main.getPowerCalculator().computePowerFIT4Green(modelCopy);
		log.debug("Metric: computed power in " + (System.currentTimeMillis() - start) + " ms.");

		start = System.currentTimeMillis();
		createModelDocument(modelCopy);
		log.debug("Metric: created model document in " + (System.currentTimeMillis() - start) + " ms.");
		
		start = System.currentTimeMillis();
		main.getOptimizer().setOptimizationObjective(main.getOptimizationObjective());
		log.debug("Metric: set optimization objective in " + (System.currentTimeMillis() - start) + " ms.");
		
		start = System.currentTimeMillis();
		AllocationResponse response = main.getOptimizer().allocateResource(allocationRequest, modelCopy);
		log.debug("Metric: allocated resource in " + (System.currentTimeMillis() - start) + " ms.");
		
		log.debug("Response in Monitor: " + response);
		
		return response;
	}
	
	/**
	 * Method for requesting a global optimization to the Optimizer.
	 */
	@Override
	public void requestGlobalOptimization() {
		
		FIT4Green modelCopy = getModelCopy();
		
		main.getPowerCalculator().computePowerFIT4Green(modelCopy);
		createModelDocument(modelCopy);
		
		main.getOptimizer().setOptimizationObjective(main.getOptimizationObjective());
		
		main.getOptimizer().performGlobalOptimization(modelCopy);

	}

	/**
	 * Utility method for logging the XML representation of the current FIT4Green 
	 * model instance.
	 * 
	 * @author FIT4Green
	 */
	@Override
	public void logModel(){
		JAXBContext jaxbCtx = Util.getJaxbContext();
		
		// create a Marshaller and marshal to System.out
		try {
			
			log.debug("*** F4G MODEL in Monitor *****");
			
			JAXBElement<FIT4Green> element = (new ObjectFactory()).createFIT4Green(model);
			
			Marshaller m = jaxbCtx.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(element, System.out);
		} catch (PropertyException e) {
			log.error(e);
		} catch (JAXBException e) {
			log.error(e);
		}

	}
	
	/* (non-Javadoc)
	 * @see f4gmonitor.IMonitor#dispose()
	 */
	@Override
	public boolean dispose() {
		if (requestTimer != null) {
			requestTimer.cancel();
		}
		if (computeTimer != null) {
			computeTimer.cancel();
		}
		return true;
	}	

	/**
	 * @return the computedPower
	 */
	@Override
	public PowerData getComputedPower() {
		return computedPower;
	}
	
	public void setFrameworkStatus (String frameworkName, FrameworkStatus status) {
		log.debug("Setting framework status...");
		double start = System.currentTimeMillis();
		
		JXPathContext context = JXPathContext.newContext(model);
		
		String myQuery = "./site/datacenter/frameworkCapabilities[frameworkName='"+frameworkName+"']";
		
		Iterator fCapabilitiesIterator = context.iterate(myQuery);
		log.debug("Time Metric: context.iterate(myQuery) took " + (System.currentTimeMillis() - start) + " ms.");
		
		while (fCapabilitiesIterator.hasNext()){
        	
        	FrameworkCapabilities fCapability = 
        		(FrameworkCapabilities)fCapabilitiesIterator.next();
        	log.debug("Time Metric: fCapabilitiesIterator.next took " + (System.currentTimeMillis() - start) + " ms.");
        	
        		JXPathContext context2 = JXPathContext.newContext(fCapability);
        		try  {
        			context2.setValue("status",status);
        		} catch (JXPathException e) {
					e.printStackTrace();
        		}
        		
        		log.debug("Time Metric: context2.setValue took " + (System.currentTimeMillis() - start) + " ms.");
        		
        		// check current status
        		if (status.value().compareTo(FrameworkStatus.RUNNING.toString()) == 0 ) {
        			main.setError(false);
            		main.setErrorMessage(null);
        		} else if (status.value().compareTo(FrameworkStatus.STOPPED.toString()) == 0 ) {
        			// check previous status as retrieved from couchDB
        			String previousStatus = retrieveFrameworkStatus(frameworkName);
            		if (previousStatus.compareTo(FrameworkStatus.RUNNING.toString()) == 0) {
            			main.setError(true);
                		main.setErrorMessage("Framework \"" + frameworkName + "\" has stopped; trying to re-connect; see log for more info.");
            		}
        		}
        		log.debug("Time Metric: while iteration took " + (System.currentTimeMillis() - start) + " ms.");
 		}

		FIT4Green modelCopyUpdated = getModelCopy();
		log.debug("Time Metric: getModelCopy took " + (System.currentTimeMillis() - start) + " ms.");
		main.getPowerCalculator().computePowerFIT4Green(modelCopyUpdated);
		log.debug("Time Metric: computePowerFIT4Green took " + (System.currentTimeMillis() - start) + " ms.");
		createModelDocument(modelCopyUpdated);

		log.debug("Time Metric: framework status set took " + (System.currentTimeMillis() - start) + " ms.");
	}
	
		
	/**
	 * Creates a mapping of all the nodes containing the "frameworkID" identifier.
	 * The mapping is stored in a HashMap, with key the 'comName_frameworkID' and 
	 * value the containing node.
	 * This mapping is used for handling update requests coming from the COM 
	 * components, which pass to the Monitor the key of the node to be updated and 
	 * a reference to themselves.
	 * 
	 * @author FIT4Green 
	 */
	private void createMapping(){
		log.debug("Creating mapping of the model objects...");
		JXPathContext context = JXPathContext.newContext(model);
		
		String myQuery = "//frameworkCapabilities";
		
        Iterator fCapabilitiesIterator = context.iterate(myQuery);
       
        // Iteration over the "FrameworkCapabilities" items
        while(fCapabilitiesIterator.hasNext()){
        	FrameworkCapabilities fCapability = 
        		(FrameworkCapabilities)fCapabilitiesIterator.next();
            log.debug("fCapability name " + fCapability.getFrameworkName() 
            		                      + " (" + fCapability + ")");
            
            // Query to find elements that have a reference to the current 
            // framework capability item
            myQuery = "//*[frameworkRef='" + fCapability + "']";
            
            Iterator referrersIterator = context.iterate(myQuery);
            JXPathContext context2 = null;
            while(referrersIterator.hasNext()){
               Object obj = referrersIterator.next();
               context2 = JXPathContext.newContext(obj);
           		
               // Query to find the "frameworkID" (this allows avoiding casting 
               // by reflection)
               myQuery = "./frameworkID";
               
               String frameworkID = context2.getValue(myQuery).toString();
           	
               log.debug("Elem to be added: " + "["+fCapability.getFrameworkName()+ "_" +frameworkID + ", " + obj.getClass().getCanonicalName() + "]");
               mapping.put(fCapability.getFrameworkName()+ "_" +frameworkID, obj);
            }           	            
       }   
		log.debug("Mapping of the model objects created");
	}
	
	/**
	 * Schedules, if enabled, the time-based global optimization requests
	 * period and delay configured within f4gconfig.properties
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private void scheduleRequestTask() {
		String periodString = Configuration.get(Constants.GLOBAL_OPTIMIZATION_PERIOD);
		String delayString = Configuration.get(Constants.GLOBAL_OPTIMIZATION_DELAY);
		// x (min) * 60 (sec) * 1000 (millisec)
		long period = Long.parseLong(periodString) * 60000; 
		long delay = Long.parseLong(delayString) * 60000;
		// TODO (phase 3) Delay is not counted after the model is ready
		if (period > 0) {
			log.info("Global optimization task is enabled; period is "  
					+ period/60000 + ", delay is " + delay/60000 + " (min)");
			requestTimer = new Timer();
		    requestTimer.schedule(new GlobalOptimizationTask(), delay, period);
		}
		else {
			log.info("Global optimization task is disabled");
		}	
	}
	
	/**
	 * Schedules, if enabled, the task for computing the total ICT power consumption
	 * period configured within f4gconfig.properties
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private void scheduleComputeTask(){
		String periodString = Configuration.get(Constants.COMPUTE_POWER_PERIOD);
		// x (min) * 60 (sec) * 1000 (millisec)
		// delay is 0
		long period = Long.parseLong(periodString) * 60000; 
		if (period > 0) {
			log.info("Compute ICT power task is enabled; period is "  
					+ period/60000 + " (min)");
			computeTimer = new Timer();
		    computeTimer.schedule(new ComputePowerTask(), 0, period);
		}
		else {
			log.info("Compute ICT power task is disabled");
		}
	}
	
	
	/**
	 * Implements a task dedicated to sending time-based global 
	 * optimization request
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private class GlobalOptimizationTask extends TimerTask {
		/**
		* Implements TimerTask abstract run method
		*/
		@Override
		public void run(){
			if (main != null && main.isRunning()) {
				requestGlobalOptimization();
				log.debug("Time-based global optimization requested...");
			} 
			else {
				log.warn("Cannot request global optimization");
			}
		}
	}
	
	/**
	 * Implements a task dedicated to computing total ICT power consumption
	 * and saving the current model instance to the database
	 * 
	 * @author Vasiliki Georgiadou
	 */
	private class ComputePowerTask extends TimerTask {
		/**
		* Implements TimerTask abstract run method
		*/
		@Override
		public void run(){
			
			computedPower = new PowerData();
			if (main != null && main.isRunning()) {
				FIT4Green modelCopy = getModelCopy();
				computedPower = main.getPowerCalculator().computePowerFIT4Green(modelCopy);
				createModelDocument(modelCopy);
				//getMetrics(modelCopy);
				log.debug("The total ICT power consumption is " + computedPower.getActualConsumption() + " W");
			} else {
				log.warn("Cannot compute total ICT power consumption");
			}
		}
	}

	
	/**
	 * Creates new document in the model database
	 *
	 * @author Vasiliki Georgiadou
	 */
	private void createModelDocument (FIT4Green model) {
			
		DataBase db = new DataBase();
		db.setUrl(Configuration.get(Constants.DB_URL));
		
		String id = UUID.randomUUID().toString();
		
		try {
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			model.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
		} catch (DatatypeConfigurationException e) {
			log.error(e);
		}
		
		ConvertToJSON con = new ConvertToJSON();
		String data = con.convert(model);
	
		// check database current size and delete oldest document if applicable
		db.retrieveProperties(Configuration.get(Constants.MODELS_DB_NAME));
		
		if (db.getProperties().getDiskSize() >= maxSize ) {
			
			log.debug("Database \"" + Configuration.get(Constants.MODELS_DB_NAME) + "\" exceeds allowed maximum size...");

			String response = db.query(Configuration.get(Constants.MODELS_DB_NAME), 
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
			db.deleteDocument(Configuration.get(Constants.MODELS_DB_NAME), delDocID, rev);
			
			log.debug("Running compaction on database \"" + Configuration.get(Constants.MODELS_DB_NAME) + "\"...");
			db.compact(Configuration.get(Constants.MODELS_DB_NAME));
			
		}
		
		log.debug("About to create document with id " + id + " at \""
				+ Configuration.get(Constants.MODELS_DB_NAME) + "\" database");
		
		int rc = db.createDocument(Configuration.get(Constants.MODELS_DB_NAME), id, data);
		
		if (rc == 201) {
			log.debug("Document " + id + " created");
		} else {
			log.error("Error while creating document " + id + "; " + db.getMessage());
		}
	}
	
	private String createDesignBody () {
		
		String map1 = 		
			"function(doc) {" + "\n" +  
			"  if (doc.Datetime) {" + "\n" + 
			"    emit(doc.Datetime,doc._rev);" + "\n" +  
			"  }" + "\n" + 
			"}";
		
		String map2 =
			"function(doc) {" + "\n" +
			"  var data = \\\"\\\";" + "\n" +
			"  var sites = doc[\\\"ns2:Site\\\"];" + "\n" +
			"  if (sites.length == undefined) {" + "\n" +
			"    sites = [sites];" + "\n" +
			"  }" + "\n" +
			"  for (var i = 0; i < sites.length; i++) {" + "\n" + 
			"   var datacentres = sites[i][\\\"ns2:Datacenter\\\"];" + "\n" +
			"    if (datacentres.length == undefined) {" + "\n" +
			"      datacentres = [datacentres];" + "\n" +
			"    }" + "\n" +
			"    for (var j = 0; j < datacentres.length; j++) {" + "\n" +
			"      var frameworks = datacentres[j][\\\"ns2:FrameworkCapabilities\\\"];" + "\n" +
			"      if (frameworks.length === undefined) {" + "\n" +
			"        frameworks = [frameworks];" + "\n" +
			"      }" + "\n" +
			"      for (var k = 0; k < frameworks.length; k++) {" + "\n" +
			"        data = data + frameworks[k].frameworkName + \\\";\\\" + frameworks[k].status + \\\",\\\";" + "\n" +
			"      }" + "\n" +
			"    }" + "\n" +
			"  }" + "\n" +
			"  emit (doc.Datetime,data);" + "\n" +
			"}";
		
		String body = 		
			"{" + "\n" +
			"   \"language\": \"javascript\"," + "\n" +
			"   \"views\": {" + "\n" +
			"      \"" + SORT_BY_DATETIME_VIEW + "\": {" + "\n" +
			"         \"map\": \"" + map1 + "\"" + "\n" +
			"      }," + "\n" +
			"      \"" + RETRIEVE_FRAMEWORK_STATUS_VIEW + "\": {" + "\n" +
			"         \"map\": \"" + map2 + "\"" + "\n" +
			"      }" + "\n" +
			"   }" + "\n" +
			"}";
		
		return body;
	}
	
	private String retrieveFrameworkStatus (String frameworkName) {
		
		String status=null;
		
		DataBase db = new DataBase();
		db.setUrl(Configuration.get(Constants.DB_URL));
		String response = db.query(Configuration.get(Constants.MODELS_DB_NAME), DESIGN_ID, 
				RETRIEVE_FRAMEWORK_STATUS_VIEW,"limit=1&descending=true");

		String[] tokens = response.split("[{}:\",]+");

		for (int i=0; i<tokens.length; i++) {
			if (tokens[i].compareTo("value") == 0) {
				for (int j = i+1; j < tokens.length; j++) {
					String[] data = tokens[j].split(";");
					for (int k = 0; k < data.length; k++) {
						if (data[k].compareTo(frameworkName) == 0) {
    							status = data[k+1];
						}
					}
				}
				i = tokens.length;
			}
		}
		return status;
	}
	
	private void getMetrics(FIT4Green model) {
	
		double totalIctPower = 0.0;
		double totalSitePower = 0.0;
		double totalCarbonEmissions = 0.0;
		
		JXPathContext context = JXPathContext.newContext(model);
		String myQuery = "//site";
		Iterator siteIterator = context.iterate(myQuery);
       
        while(siteIterator.hasNext()){
        	Site site = (Site)siteIterator.next();
        	totalIctPower += site.getComputedPower().getValue();
        	totalSitePower += site.getComputedPower().getValue() * site.getPUE().getValue();
        	totalCarbonEmissions += site.getComputedPower().getValue() * site.getCUE().getValue();
        }
        
        log.debug("The total ICT power consumption is " + totalIctPower + " W");
        log.debug("The total Site power consumption is " + totalSitePower + " W");
        log.debug("The total carbon emissions are " + totalCarbonEmissions + " grCO2eq/1000h");
	}
	
}
