package org.f4g.com.simulation;

import java.util.Random;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.f4g.com.ICom;
import org.f4g.monitor.IMonitor;
import org.f4g.schema.allocation.CloudVmAllocationResponseType;
import org.f4g.schema.allocation.ObjectFactory;
import org.f4g.schema.allocation.CloudVmAllocationType;
import org.f4g.schema.allocation.AllocationRequestType;
import org.f4g.schema.allocation.AllocationResponseType;
import org.f4g.schema.allocation.RequestType;
import org.f4g.schema.allocation.ResponseType;

public class AllocateResourceSimulationThread extends GenericSimulationThread{
	static Logger log = Logger.getLogger(AllocateResourceSimulationThread.class.getName()); // 

	IMonitor monitor = null;
	Random random = new Random();
	
	String[] vmTypes = new String[]{"m1.small", "m1.medium", "m1.large"};
	
	public AllocateResourceSimulationThread(ICom comObject, long interval, IMonitor monitor) {
		super(comObject, interval);

		this.monitor = monitor;
	}

	@Override
	public void run() {

		long waiting = (((int)(Math.random()*100))%20);
		//Random timeout [0-20] secs before really starting
		try {
			log.debug("About " + waiting +" secs. to start...");
			Thread.sleep(waiting*1000);
		} catch (InterruptedException e1) {
			log.error(e1);
		}

		while(state != STATE_STOPPED){
			AllocationRequestType request = new AllocationRequestType();
			//Simulates a CloudVmAllocationType operation
			JAXBElement<CloudVmAllocationType>  operationType = (new ObjectFactory()).createCloudVmAllocation(new CloudVmAllocationType());
			CloudVmAllocationType operation = new CloudVmAllocationType();
			operation.getClusterId().add(String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setImageId(String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setUserId("user"+String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setVmType(vmTypes[random.nextInt(3)]);
			operationType.setValue(operation);
			request.setRequest(operationType);
			
			AllocationResponseType response = monitor.allocateResource(request);
			
			if(response != null &&
			   response.getResponse() != null){
				
				CloudVmAllocationResponseType operationResponse = (CloudVmAllocationResponseType)response.getResponse().getValue();
				
				log.debug("ResourceAllocationResponse: " );
				log.debug("	ClusterID: " + operationResponse.getClusterId());
				log.debug("	ImageId: " + operationResponse.getImageId());
				log.debug("	UserId: " + operationResponse.getUserId());
				log.debug("	VmType: " + operationResponse.getVmType());
				log.debug("	NodeId: " + operationResponse.getNodeId());
				
			}
			
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}

}
