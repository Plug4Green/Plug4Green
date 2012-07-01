package org.f4g.com.simulation;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.f4g.com.ICom;
import org.f4g.com.util.ComOperation;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;

public abstract class ComUpdateSimulationThread extends GenericSimulationThread{
	static Logger log = Logger.getLogger(ComUpdateSimulationThread.class.getName()); // 
	
	IMonitor monitor = null;
	
	public ComUpdateSimulationThread(ICom comObject, long interval, IMonitor monitor) {
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
			if(state == STATE_RUNNING){
				
				Set keys = comObject.getMonitoredObjects().keySet();
				int i = ((int)(Math.random()*10))%(keys.size());
				String key = ((String[])keys.toArray(new String[0]))[i];
				simulateDataChange(key);
				
				if(((ConcurrentLinkedQueue<ComOperationCollector>)comObject.getQueuesHashMap().get(key)).size()>0){
					//monitor.updateNode(key, comObject);
					log.debug("About to ask for simple updates...");
					ComOperationCollector operationSet = ((ConcurrentLinkedQueue<ComOperationCollector>)comObject.getQueuesHashMap().get(key)).poll();
					monitor.simpleUpdateNode(key, operationSet);
				}
				//Monitor.getInstance().logModel(); 
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	abstract void simulateDataChange(String key);

}
