package org.f4g.com.simulation;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.f4g.com.ICom;
import org.f4g.com.util.ComOperation;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;

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
