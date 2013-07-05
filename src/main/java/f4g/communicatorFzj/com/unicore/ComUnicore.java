/**
* ============================== Header ============================== 
* file:          ComFzj.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1244 $
* 
* short description:
*   Implementation of the ICom interface for the HPC scenario at the FZJ
* 
* @author Daniel Brinkers
* @see org.f4g.com.ICom
* ============================= /Header ==============================
*/
package org.f4g.com.unicore;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.f4g.com.ICom;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.HpcClusterAllocationResponseType;
import org.f4g.schema.allocation.HpcClusterAllocationType;

/**
 * Implementation of the ICom interface for the Unicore F4G communication
 * 
 * @see org.f4g.com.ICom
 * 
 * @author Andre Giesler
 */

public class ComUnicore implements ICom   {
	
	static Logger log = Logger.getLogger(ComUnicore.class.getName());
	
	public static final String COM_PROPERTIES_DIR = "config/";
	public static final String COM_PROPERTIES_SUFFIX = ".properties";
	String CLIENT_KEYSTORE_PATH;
	String CLIENT_KEYSTORE_PASS;
	String PROXY_HOST;
	int PROXY_PORT;
	String CAJOSERVERNAME;
	
	private Map<String, ICom> monitoredObjects_;
	private Map<String, ConcurrentLinkedQueue<ComOperationCollector>> queuesHashMap_;
	private IMonitor monitor_ = null;
	private String name_ = null;

	protected String generateHPCAllocRequest(int nr_of_nodes, int needed_cores, 
			int needed_mem,  long walltime, boolean energy_aware, 
			long latest_finish,	String suitable_clusters, String benchmark_id){
		AllocationRequestType request = new AllocationRequestType();

		//Creates a request
		HpcClusterAllocationType hpcRequest = new HpcClusterAllocationType();
		hpcRequest.setNumberOfNodes(nr_of_nodes);
		log.info("HPC Alloc Request: NrofNodes " + nr_of_nodes);
		hpcRequest.setNeededCoresPerNode(needed_cores);
		log.info("HPC Alloc Request: NrofCores " + needed_cores);
		hpcRequest.setNeededMemory(needed_mem);	
		log.info("HPC Alloc Request: NeededMem " + needed_mem);
		hpcRequest.setWallTime(walltime);
		log.info("HPC Alloc Request: Walltime " + walltime);
		hpcRequest.setEnergyAware(energy_aware);
		log.info("HPC Alloc Request: Energy aware " + energy_aware);
		hpcRequest.setLatestFinish(latest_finish);
		log.info("HPC Alloc Request: Latest finishing time " + latest_finish);
		hpcRequest.setSuitableClusters(suitable_clusters);
		log.info("HPC Alloc Request: Preselected suitabel clusters " + suitable_clusters);
		hpcRequest.setBenchmarkName(benchmark_id);
		log.info("HPC Alloc Request: Using benchmarks for " + benchmark_id);

		org.f4g.schema.allocation.ObjectFactory allocationFactory = new org.f4g.schema.allocation.ObjectFactory();              

		request.setRequest((allocationFactory.createHpcClusterAllocation(hpcRequest)));

		AllocationResponseType response = getMonitor().allocateResource(request);

		HpcClusterAllocationResponseType hpcResponse = (HpcClusterAllocationResponseType)response.getResponse().getValue();

		log.info("Selected cluster ID: " + hpcResponse.getClusterId()); 
		
		return hpcResponse.getClusterId();
	}
	
	/* (non-Javadoc)
	 * @see org.f4g.com.ICom#dispose()
	 */
	@Override
	public boolean dispose() {
		// TODO Auto-generated method stub
		return false;
	}
		
	@Override
	public boolean executeActionList(@SuppressWarnings("rawtypes") ArrayList arg0) {

		return false;
	}

	@Override
	public boolean executeUpdate(String arg0, Object arg1) {
		
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public HashMap getMonitoredObjects() {
		return (HashMap) getMonitoredObjects_();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public HashMap getQueuesHashMap() {
		return (HashMap) getQueuesHashMap_();
	}

	@Override
	public boolean init(String name, IMonitor monitor) {
		
		Properties prop = new Properties();	    
	    try
	    {
	    	InputStream is = this.getClass().getClassLoader().getResourceAsStream(COM_PROPERTIES_DIR + name + COM_PROPERTIES_SUFFIX);
	    	prop.load(is);
	    	CLIENT_KEYSTORE_PATH = prop.getProperty("clientKeystorePath").trim();
	    	CLIENT_KEYSTORE_PASS = prop.getProperty("clientKeystorePass").trim();
	    	PROXY_HOST = prop.getProperty("proxyHost").trim();
	    	PROXY_PORT = Integer.parseInt(prop.getProperty("proxyPort").trim());
	    	CAJOSERVERNAME = prop.getProperty("cajoServerName").trim();
	    	log.debug("Client keystore path: " + CLIENT_KEYSTORE_PATH);
	    	log.debug("Proxy host: " + PROXY_HOST);
	    	log.debug("Proxy port: " + PROXY_PORT);
	    	log.debug("CAJOSERVERNAME port: " + CAJOSERVERNAME);
	    	
	    }
	    catch (IOException ioe){
	    	log.debug("Couldn't read properties from relative path 'config/ComFzj.properties'.");
	    }
	    catch (Exception e)
	    {
	    	log.debug("Couldn't get information about proxy host or port. Stopping update.");
	    	stopUpdate();
	    }
		
		setMonitor(monitor);
		setName(name);
		setQueuesHashMap_(new HashMap<String, ConcurrentLinkedQueue<ComOperationCollector>>());
		
		//Start CajoServer to reveive Job requests from Unicore Servlet
		startUnicoreJobServer();
		
		return true;
	}
	
	/**
	 * Starts the cajo server to receive job allocation requests from Unicore 
	 * over the ForwardJobAllocationRequest Servlet
	 * 
	 */
	private void startUnicoreJobServer(){
		try {
			log.info("Create connection");
            Remote.config(null, PROXY_PORT, null, 0);
            ItemServer.bind(new InterServletComServer(this), "UnicoreComServer");
            log.info("UnicoreComServer is running");
        } catch(Exception e) {
            e.printStackTrace();
        }
	}
	
	@Override
	public boolean startUpdate() {
		//getThread().start();
		return false;
	}

	@Override
	public boolean stopUpdate() {
		//getThread().interrupt();
		return false;
	}
	
	public void setMonitoredObjects_(Map<String, ICom> monitoredObjects_) {
		this.monitoredObjects_ = monitoredObjects_;
	}

	public Map<String, ICom> getMonitoredObjects_() {
		return monitoredObjects_;
	}

	public void setQueuesHashMap_(
			Map<String, ConcurrentLinkedQueue<ComOperationCollector>> queuesHashMap_) {
		this.queuesHashMap_ = queuesHashMap_;
	}

	public Map<String, ConcurrentLinkedQueue<ComOperationCollector>> getQueuesHashMap_() {
		return queuesHashMap_;
	}

	public void setMonitor(IMonitor monitor) {
		this.monitor_ = monitor;
	}

	public IMonitor getMonitor() {
		return monitor_;
	}

	public void setName(String name) {
		this.name_ = name;
	}

	public String getName() {
		return name_;
	}
	
}