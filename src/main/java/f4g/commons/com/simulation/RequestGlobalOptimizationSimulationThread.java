package f4g.commons.com.simulation;

import org.apache.log4j.Logger;
import f4g.commons.com.ICom;
import f4g.commons.monitor.IMonitor;

public class RequestGlobalOptimizationSimulationThread extends GenericSimulationThread{
	static Logger log = Logger.getLogger(RequestGlobalOptimizationSimulationThread.class.getName()); // 

	IMonitor monitor = null;
	
	public RequestGlobalOptimizationSimulationThread(ICom comObject, long interval, IMonitor monitor) {
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
			monitor.requestGlobalOptimization();
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}

}
