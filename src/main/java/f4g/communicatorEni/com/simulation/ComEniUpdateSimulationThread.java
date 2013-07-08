package f4g.communicatorEni.com.simulation;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import f4g.commons.com.ICom;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;

public class ComEniUpdateSimulationThread  extends ComUpdateSimulationThread {
	static Logger log = Logger.getLogger(ComEniUpdateSimulationThread.class.getName()); // 

	public ComEniUpdateSimulationThread(ICom comObject, long interval, IMonitor monitor) {
		super(comObject, interval, monitor);
	}

	void simulateDataChange(String key){
		log.debug("Simulating data change..." + key);
		
		ComOperationCollector operations = new ComOperationCollector();
		if(key.equals("ComEni_ENSV1HB9"))
		{
			ComOperation operation = new ComOperation(ComOperation.TYPE_UPDATE, "actualCPUUsage[../frameworkID='ENSV1HB9']", String.valueOf((Math.random())*100));		
			operations.add(operation);
		
			((ConcurrentLinkedQueue<ComOperationCollector>)comObject.getQueuesHashMap().get(key)).add(operations);
			
		}
		
	}
}
