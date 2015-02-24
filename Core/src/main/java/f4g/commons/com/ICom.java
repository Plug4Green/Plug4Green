package f4g.commons.com;

import java.util.ArrayList;
import java.util.HashMap;

import f4g.commons.monitor.IMonitor;


/**
 * Interface to be implemented by every Com components
 * 
 * @author FIT4Green
 *
 */
public interface ICom {

	/**
	 * This method is invoked as a 'callback' operation by the Monitor component. 
	 * The Monitor passes a node of the model into the obj parameter. The Com retrieves
	 * a set of update operations queued for this node and apply it to the node
	 * 
	 * @param key key of the mapping for the Com object
	 * @param obj a node in the f4g model
	 * @return true if successful, false otherwise
	 */
	boolean executeUpdate(String key, Object obj);
	
	/**
	 * Initialize the component. Retrieves from the Monitor a list of the node objects 
	 * monitored by the component.
	 * Creates a new map with the same keys, but for each key (meaning for each node) e
	 * queue is created. The queue will contain the list of pending updates to be performed.
	 */
	boolean init(String name, IMonitor monitor);
	
	/**
	 * 
	 * Starts up the control loop of the Com, for the monitoring of the data and the update of the model
	 * 
	 * @return true if starts up with no errors, false otherwise
	 *
	 * @author FIT4Green
	 */
	boolean startUpdate();
	
	/**
	 * 
	 * Stops the control loop of the Com 
	 * 
	 * @return true if stopsp with no errors, false otherwise
	 *
	 * @author FIT4Green
	 */
	boolean stopUpdate();
	
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
	 * Execute a list of actions on behalf of the Controller
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */
	boolean executeActionList(ArrayList actionRequest);
	
	/**
	 * 
	 * Provides the set of all the nodes in the model which are handled by the Com
	 * 
	 * @return the set of  the nodes in the model handled by the Com
	 *
	 * @author FIT4Green
	 */
	HashMap getMonitoredObjects();
	
	/**
	 * 
	 * For each element handled by the com, there is a ConcurrentLinkedQueue structure, 
	 * acting as a collector for the update operations to be performed on the model for 
	 * that element.
	 * When a complex update operation is invoked on the Monitor for an element, the 
	 * corresponding queue is evaluated and the related update operations executed on 
	 * the model. 
	 * This method returns the HashMap containing the mapping <element, ConcurrentLinkedQueue>
	 * 
	 * @return
	 *
	 * @author FIT4Green
	 */
	HashMap getQueuesHashMap();
	
	
}
