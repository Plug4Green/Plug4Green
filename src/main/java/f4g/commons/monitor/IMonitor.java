package f4g.commons.monitor;

import java.util.HashMap;

import f4g.commons.com.ICom;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.com.util.PowerData;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkStatusType;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.AllocationResponseType;

/**
 * 
 * Interface for the f4g Monitor component
 * 
 * @author FIT4Green
 *
 */
public interface IMonitor {


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
	public AllocationResponseType allocateResource(AllocationRequestType allocationRequest);
	
	/**
	 * Method for requesting a global optimization to the Optimizer.
	 */
	public void requestGlobalOptimization();
	
	/**
	 * Method to load the f4g model from a xml file and to transform it into an objcet 
	 * hierarchy representation.
	 * 
	 * @param modelPathName path to the XML model file
	 * @return true if success, false otherwise
	 */
	public boolean loadModel(String modelPathName);
	
	/**
	 * Allows a Com component to update the f4g model instance upon runtime modifications
	 * in the elements monitored by the Com.
	 * It works as a 'callback' operation. The Com invokes this method passing itself as a 
	 * parameter.
	 * The Monitor, in turn, invokes an update method on the com. This is needed for preserving 
	 * the integrity of the object representation of the model, which can be updated by 
	 * several components at the same time.
	 *  
	 * @param id The 'frameworkID' value of the node to update
	 * @param comObject The Com component which is asking for the update
	 * @return
	 */
	public boolean updateNode(String id, ICom comObject);
	
	/**
	 * Allows a Com component to update directly (without passing through the 'callback' mechanism)
	 * the f4g model instance upon runtime modifications in the elements monitored by the Com.
	 *  
	 * @param id The 'frameworkID' value of the node to update
	 * @param operations The ComOperationCollector collecting the operations to be performed
	 * @return
	 */
	public  boolean simpleUpdateNode(String id, ComOperationCollector operations);
	
	/**
	 * Allows to get a deep copy of the f4g model
	 * @return the object representation of the f4g model
	 */
	public FIT4GreenType getModelCopy();
	
	/**
	 * Allows to get a deep copy of the subset of the f4g model related to a com
	 * @return the object representation of the f4g model
	 */
	public HashMap getMonitoredObjectsCopy(String comName);
	
	/**
	 * Provides the set of all the nodes in the model which are handled by a given Com.
	 *  
	 * @param comName the name of the Com
	 * @return a map of all the objects handled by the 'comName' Com. 
	 */
	public HashMap<String, ICom> getMonitoredObjectsMap(String comName);
	
	/**
	 * Utility method for logging the xml representation of the current f4g model instance
	 */
	public void logModel();
	
	/**
	 * 
	 * This method is called by the core component responsible for starting up and shutting
	 * down the F4G plugin. It must implement all the operations needed to dispose the component
	 * in a clean way (e.g. stopping dependent threads, closing connections, sockets, file handlers, etc.)
	 * 
	 * @return
	 *
	 * @author FIT4Green
	 */
	boolean dispose();
	
	/**
	 * 
	 * Method to get the current total power (in Watt) consumption as computed 
	 * by the Monitor
	 * 
	 * @return PowerData object
	 *
	 * @author FIT4Green
	 */
	public PowerData getComputedPower();
	
	public void setFrameworkStatus (String frameworkName, FrameworkStatusType status);

}
