/**
* ============================== Header ============================== 
* file:          Main.java
* project:       FIT4Green/Plugin_Core
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2011-02-25 12:11:27 +0100 (vr, 25 feb 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 596 $
* 
* short description:
*   Entry point to the FIT4Green framework.
* ============================= /Header ==============================
*/
package f4g.pluginCore.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import f4g.commons.com.ICom;
import f4g.commons.com.util.PowerData;
import f4g.commons.controller.Controller;
import f4g.commons.controller.IController;
import f4g.commons.core.Configuration;
import f4g.commons.core.DatabaseConfiguration;
import f4g.commons.core.IMain;
import f4g.commons.monitor.IMonitor;
import f4g.manager.monitor.Monitor;
import f4g.commons.optimizer.IOptimizer;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.Optimizer;
import f4g.commons.power.IPowerCalculator;
import f4g.commons.power.PowerCalculator;

/**
 * Entry point to the FIT4Green framework
 * 
 * @author FIT4Green, Vasiliki Georgiadou
 *
 */
public class Main implements IMain {
	static Logger log = Logger.getLogger(Main.class.getName());  
	
	//Collect the configuration parameters
	public static Configuration configuration = null;
	private static Properties log4jProperties;

	//Mapping between the Com object names (as defined in the config file)
	//and the related classes
	private static HashMap comMapping = new HashMap();
	
	//Singleton instance
	private static IMain me = null;
	
	private IOptimizer optimizer= null;
	
	private IMonitor monitor = null;
	
	private IController controller = null;
	
	private IPowerCalculator powerCalculator = null;

	// Status flags
	private boolean running = false;
	private String statusMessage = "Not started";
	
	// Error flags
	private boolean error = false;
	private String errorMessage = null;
	
	// default optimization objective
	private OptimizationObjective optimizationObjective = OptimizationObjective.Power;
	
	private boolean initialized = false;
	
	private Main() {
	}
	
	/**
	 * Entry point to the framework. 
	 * @return The reference to the singleton representing the FIT4Green instance
	 */
	public static IMain getInstance(){
		if(me == null){
			me = new Main();
		}
		return me;
	}
	

	/**
	 * Initializes the framework.
	 * Enables the log4j logging and loads the configuration file specified in 
	 * the first Java argument
	 * @return
	 */
	@Override
	public synchronized boolean init(String f4gConfigPathName) {

		try {
			log.debug("Inside Main 'init'...");
			if(!initialized){
				log4jProperties = new Properties();
				if(System.getProperty("log4j.configuration") != null){
					PropertyConfigurator.configure(System.getProperty("log4j.configuration"));				
				} else {
					InputStream isLog4j = this.getClass().getClassLoader().getResourceAsStream("config/log4j.properties");
					log4jProperties.load(isLog4j);
					PropertyConfigurator.configure(log4jProperties);				
				}
				log.info("Loading configuration...");
				
				//TODO: Currently the config file is a .properties file. 
				//Will become XML as soon as the related schema will be finalized
				configuration = new Configuration(f4gConfigPathName);
				initialized = true;
			
				log.debug("The Main has been initialized");
			} else {
				log.debug("The Main was already initialized!");
			}

		} catch (IOException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	/**
	 * Starts up the framework 
	 * Initializes the Monitor and loads and initializes the Com objects specified 
	 * in the 'f4gconfig.properties'configuration file.
	 * @return true if properly started up, false otherwise
	 */
	@Override
	public boolean startup() {
		
		// Check operation mode
		String operationMode = configuration.get(Constants.OPERATION_MODE);
		int op = Integer.parseInt(operationMode);
		String msg = "Operation mode is set to ";
		switch (op) {
		case 1: msg = msg + "fully automatic";  break;
		case 2: msg = msg + "semi-automatic";  break;
		case 3: msg = msg + "manual";  break;
		case 4: msg = msg + "silent";  break;
		default: op = 1; msg = "Invalid operation mode; default fully automatic";  break;
		}
		log.debug(msg);
				
		monitor = new Monitor(this);
		// If in silent mode the optimizer and the controller are disabled
		if (op != 4) {
			controller = new Controller(this);
			optimizer = new Optimizer(this);
			optimizer.setOptimizationObjective(optimizationObjective);
		}
		powerCalculator = new PowerCalculator(this);
		
		//Loads and initialize the Com components
		String[] comNames = configuration.get("comNames").split(",");
		String comName = null;
		for(int i=0; i<comNames.length; i++){
			comName = comNames[i];
			log.debug("Loading COM: " + comName);
			String className = configuration.get(comName);
			log.debug("Loading class: " + className);
			try {
				Class comClass = Main.class.getClassLoader().loadClass(className);
				log.debug("Class " + comClass.getCanonicalName() + " loaded");
				ICom comInstance = (ICom)comClass.newInstance();
				comMapping.put(comName, comInstance);
				comInstance.init(comName, monitor);
			} catch (ClassNotFoundException e) {
				log.error(e);
				setStatusMessage(e.getMessage());
				setRunning(false);
				return false;
			} catch (InstantiationException e) {
				log.error(e);
				setStatusMessage(e.getMessage());
				setRunning(false);
				return false;
			} catch (IllegalAccessException e) {
				log.error(e);
				setStatusMessage(e.getMessage());
				setRunning(false);
				return false;
			}
		}
		
		setStatusMessage("On");
		setRunning(true);
		return true;
	}

	@Override
	public IMonitor getMonitor() {
		return (monitor != null ? monitor : new Monitor(this));
	}

	@Override
	public IOptimizer getOptimizer() {
		return (optimizer != null ? optimizer : new Optimizer(this));
	}

	@Override
	public IController getController() {
		return (controller != null ? controller : new Controller(this));
	}

	@Override
	public IPowerCalculator getPowerCalculator() {
		return (powerCalculator != null ? powerCalculator : new PowerCalculator(this));
	}


	/**
	 * Returns a reference to a Com object active in the system
	 * @param the Com object name, as configured in the f4gconfig.properties file
	 * @return a reference to a Com object active in the system
	 */
	public ICom getComByName(String comName) {
		return (ICom)comMapping.get(comName);
	}

	@Override
	public boolean shutdown() {
		
		Iterator iter = comMapping.keySet().iterator();
		String key = null;
		while(iter.hasNext()){
			key = (String)iter.next();
			ICom com = (ICom)comMapping.get(key);
			log.info("Stopping Com: " + key);
			
			com.dispose();
			com = null;
			
		}
		
		comMapping.clear();
		
		if(optimizer != null){
			optimizer.dispose();
			optimizer = null;
		}

		if(powerCalculator != null){
			powerCalculator.dispose();
			powerCalculator = null;
		}
		
		if(controller != null){
			controller.dispose();
			controller = null;
		}
		
		if(monitor != null){
			monitor.dispose();
			monitor = null;
		}
		
		setRunning(false);
		setStatusMessage("Off");
		
		return true;
	}
	

	/**
	 * Launcher method
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length == 0){
			System.out.println("Please provide the path of config file as an argument (usually config/f4gconfig.properties).");
			System.exit(1);
		}
		
		IMain f4gInstance = new Main();
		boolean res =  f4gInstance.init(args[0]);
		
		if(res){
			res = f4gInstance.startup();
		} else {
			System.exit(1);
		}


		f4gInstance.isRunning();
		f4gInstance.getStatusMessage();
		try {
			Thread.sleep(35000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		f4gInstance.shutdown();
		
	}

	public boolean isRunning() {
		log.debug("isRunning(): " + running);
		return running;
	}

	public void setRunning(boolean running) {
		log.debug("setting 'running' to " + running);
		this.running = running;
	}

	public String getStatusMessage() {
		log.debug("getStatusMessage(): " + statusMessage);
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		log.debug("setting 'statusMessage' to " + statusMessage);
		this.statusMessage = statusMessage;
	}
	
	public boolean errorExists() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getErrorMessage() {
		log.debug("getErrorMessage(): " + errorMessage);
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		log.debug("setting 'errorMessage' to " + errorMessage);
		this.errorMessage = errorMessage;
	}
	
	public OptimizationObjective getOptimizationObjective() {
		return optimizationObjective;
	}

	public void setOptimizationObjective(OptimizationObjective optimizationObjective) {
		log.debug("setting optimization objective to " + optimizationObjective);
		this.optimizationObjective = optimizationObjective;
	}

	public double getComputedPower() {
		if (monitor == null) {
			log.debug("The monitor is not available; no power computed");
			return -1;
		}
		long period = Long.parseLong(Configuration.get(Constants.COMPUTE_POWER_PERIOD));
		if (period <= 0) {
			log.debug("Compute power task is disabled");
			return -1;
		}
		PowerData p = monitor.getComputedPower();
		return p.getActualConsumption();
	}
	
	@Override
	public DatabaseConfiguration getDatabaseConfiguration() {
		
		DatabaseConfiguration conf = new DatabaseConfiguration();
		
		if (this.running) {
			conf.setDatabaseUrl(Configuration.get(Constants.DB_URL));
			conf.setActionsDatabaseName(Configuration.get(Constants.ACTIONS_DB_NAME));
			conf.setModelsDatabaseName(Configuration.get(Constants.MODELS_DB_NAME));
		} 
		
		return conf;
		
	}

}
