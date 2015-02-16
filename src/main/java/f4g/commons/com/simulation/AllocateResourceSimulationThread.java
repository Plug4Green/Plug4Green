package f4g.commons.com.simulation;

import java.util.Random;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import f4g.commons.com.ICom;
import f4g.commons.monitor.IMonitor;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.Request;
import f4g.schemas.java.allocation.Response;

public class AllocateResourceSimulationThread extends GenericSimulationThread{
	static Logger log = Logger.getLogger(AllocateResourceSimulationThread.class.getName()); // 

	IMonitor monitor = null;
	Random random = new Random();
	
	String[] vms = new String[]{"m1.small", "m1.medium", "m1.large"};
	
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
			AllocationRequest request = new AllocationRequest();
			//Simulates a CloudVmAllocation operation
			JAXBElement<CloudVmAllocation>  operationType = (new ObjectFactory()).createCloudVmAllocation(new CloudVmAllocation());
			CloudVmAllocation operation = new CloudVmAllocation();
			operation.getClusterId().add(String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setImageId(String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setUserId("user"+String.valueOf(Math.abs(random.nextInt(50000))));
			operation.setVm(vms[random.nextInt(3)]);
			operationType.setValue(operation);
			request.setRequest(operationType);
			
			AllocationResponse response = monitor.allocateResource(request);
			
			if(response != null &&
			   response.getResponse() != null){
				
				CloudVmAllocationResponse operationResponse = (CloudVmAllocationResponse)response.getResponse().getValue();
				
				log.debug("ResourceAllocationResponse: " );
				log.debug("	ClusterID: " + operationResponse.getClusterId());
				log.debug("	ImageId: " + operationResponse.getImageId());
				log.debug("	UserId: " + operationResponse.getUserId());
				log.debug("	VmType: " + operationResponse.getVm());
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
