package org.f4g.core;

import org.f4g.com.ICom;
import org.f4g.controller.IController;
import org.f4g.monitor.IMonitor;
import org.f4g.optimizer.IOptimizer;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.power.IPowerCalculator;

/**
 * Interface representing the launcher class for the f4g framework.
 * Implementing class: Main
 * @author FIT4Green
 *
 */
public interface IMain {

	public static Configuration configuration = null;

	/**
	 * Initialize the framework: enable the log4j logging and loads the configuration file 
	 * specified in the first Java launch argument
	 * @param f4gConfigPathName
	 * @return true if successful, false otherwise
	 */
	public boolean init(String f4gConfigPathName);
	
	/**
	 * Starts up the framework: 
	 * 	 <ul>Initialize the Monitor
	 *   <ul>loads and initialize the Com objects specified in the 'f4gconfig.properties'configuration file.
	 * @return true if properly started up, false otherwise
	 */
	public boolean startup();
	
	/**
	 * Returns a reference to a Com object active in the system
	 * @param the Com object name, as configured in the f4gconfig.properties file
	 * @return a reference to a Com object active in the system
	 */
	public ICom getComByName(String comName);
	
	/**
	 * Shuts down the platform
	 * @return true if properly shut down, false otherwise
	 */
	public boolean shutdown();
	
	public IOptimizer getOptimizer();
	
	public IMonitor getMonitor();
	
	public IController getController();
	
	public IPowerCalculator getPowerCalculator();	
	
	/**
	 * 
	 * Method to detect if the f4g plugin is running
	 * 
	 * @return true if it is running, false otherwise
	 *
	 * @author FIT4Green
	 */
	public boolean isRunning();

	/**
	 * 
	 * Method to get a short status description
	 * 
	 * @return a short message describing the current status of the f4g plugin
	 *
	 * @author FIT4Green
	 */
	public String getStatusMessage();
	
	public boolean errorExists();

	public void setError(boolean error);

	public String getErrorMessage();

	public void setErrorMessage(String errorMessage);
	
	public OptimizationObjective getOptimizationObjective();

	public void setOptimizationObjective(OptimizationObjective optimizationObjective);
	
	/**
	 * 
	 * Method to get the current total power (in Watt) consumption as computed 
	 * by the Monitor
	 * 
	 * @return the current total power consumption as computed by the Monitor 
	 *
	 * @author Vasiliki Georgiadou
	 */
	public double getComputedPower();
	
	public DatabaseConfiguration getDatabaseConfiguration();
	
	
}
