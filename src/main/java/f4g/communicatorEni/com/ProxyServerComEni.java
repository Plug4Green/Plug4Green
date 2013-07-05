/**
* ============================== Header ============================== 
* file:          ProxyServerComEni.java
* project:       FIT4Green/CommunicatorEni
* created:       08/02/2011 by jclegea
* 
* $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1497 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Properties;

import javax.net.ssl.SSLServerSocketFactory;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.f4g.com.TraditionalDataSend;
import org.f4g.monitor.IMonitor;
import org.f4g.schema.allocation.TraditionalVmAllocationResponseType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.allocation.TraditionalVmAllocationType;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public class ProxyServerComEni implements Runnable{
	static Logger log = Logger.getLogger(ProxyServerComEni.class.getName());
	
	private static final int STATE_CREATED = 0;
	protected static final int STATE_RUNNING = 1;
	protected static final int STATE_STOPPED = 2;
	private static final int STATE_PAUSED = 3;	

	protected static int state = STATE_CREATED;	
	private Thread t = null;
	ICom comObject = null;
	private static Properties configurationEni_ = null;
	
	IMonitor monitor_ = null;
	
	public ProxyServerComEni(IMonitor monitor){
		try {
			configurationEni_ = new Properties();
			InputStream configInputStream = this.getClass().getClassLoader().
								getResourceAsStream(ComEniConstants.CONFIGURATION_ENI);		
			configurationEni_.load(configInputStream);
			
			String comEniKeystore = this.getClass().getClassLoader().getResource(configurationEni_.getProperty(ComEniConstants.KEYSTORE)).getPath();			
			
			monitor_ = monitor;
				
			// Set the security values to communicate with VMWare WebServices			
			System.setProperty("javax.net.ssl.keyStore", 
					comEniKeystore);	
			System.setProperty("javax.net.ssl.keyStorePassword", 
					DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.KEYSTORE_PASSWORD)));
			System.setProperty("javax.net.ssl.trustStore", 
					comEniKeystore);
			System.setProperty("javax.net.ssl.trustStorePassword", 
					DesencryptString.desencryptString(configurationEni_.getProperty(ComEniConstants.KEYSTORE_PASSWORD)));
			
			//log.debug("KEYSTORE: " + System.getProperty("javax.net.ssl.keyStore"));
			//log.debug("KEYSTORE PASSWORD: " + System.getProperty("javax.net.ssl.keyStorePassword"));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		t = new Thread(this);
		start();
	}
	
	public void start() {
		state = STATE_RUNNING;
		t.start();
	}

	public void stop() {
		state = STATE_STOPPED;
		if(t != null){
			t.interrupt();
		}
	}

	public void pause() {
		state = STATE_PAUSED;
	}
	
	@Override
	public void run(){		
		TraditionalVmAllocationResponseType operationResponse = null;
		ServerSocket serverSocket = null;
		Socket socket = null;
		ObjectInputStream objectInputStream = null;
		AllocationRequestType request = null;
		TraditionalVmAllocationType operation = null;
		TraditionalDataSend receivedData = null;
		AllocationResponseType response = null;
		boolean isRunning = true;

		try {
			serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(
					Integer.valueOf(configurationEni_.getProperty(ComEniConstants.PORT)));
		} catch (NumberFormatException exception) {
			log.error("Error",exception);			
		} catch (BindException exception){
			log.error("BINDEXCEPTION",exception);
			isRunning = false;
		} catch (IOException exception) {			
			log.error("Error",exception);
		}
		
		

		while(isRunning){
			try{
				socket = serverSocket.accept();				
				objectInputStream = new ObjectInputStream(socket.getInputStream());

				// Send allocation request to fit4green				
				request = new AllocationRequestType();				
				JAXBElement<TraditionalVmAllocationType>  operationType = (new ObjectFactory()).createTraditionalVmAllocation(new TraditionalVmAllocationType());

				operation = new TraditionalVmAllocationType();
				log.debug("Waiting data");
				receivedData = (TraditionalDataSend)objectInputStream.readObject();				
				if("allocate".equals(receivedData.getOperation()) == true){
					// Send an allocation resource request
					operation.setMemoryUsage(receivedData.getMemoryUsage());
					operation.setNumberOfCPUs(receivedData.getNumberOfCPUs());
					operation.setStorageUsage(receivedData.getStorageUsage());
					operation.setCPUUsage(receivedData.getCPUUsage());
					operation.setDiskIORate(receivedData.getDiskIORate());
					operation.setNetworkUsage(receivedData.getNetworkUsage());
					operation.setMinPriority(receivedData.getPriority());
					
					for(int i=0;i<receivedData.getCluster().size();i++)
					{
						operation.getClusterId().add(receivedData.getCluster().get(i));
					}
					
					log.debug("received - CPUUsage:" + operation.getCPUUsage());
					operationType.setValue(operation);					
					request.setRequest(operationType);

					response = monitor_.allocateResource(request);
				}
				else{
					// send a global optimization request
					monitor_.requestGlobalOptimization();
				}

				if(response != null &&
						response.getResponse() != null){
					operationResponse = (TraditionalVmAllocationResponseType)response.getResponse().getValue();
					log.debug("ResourceAllocationResponse: " + operationResponse.getNodeId());
					// Creating response
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

					log.debug("Sending Response");
					log.debug("nodeid: " + operationResponse.getNodeId() + " clusterId: " + operationResponse.getClusterId());
					
				
					// Writing the 2 values
					objectOutputStream.writeObject(operationResponse.getNodeId());
					objectOutputStream.writeObject(operationResponse.getClusterId());
					objectOutputStream.flush();
				}



				socket.close();
			}catch(Exception exception){
				log.error("Error",exception);
			}
		}


	}

}
