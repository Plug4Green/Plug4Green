package org.f4g.com.simulation;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.f4g.com.ICom;
import org.f4g.com.util.ComOperation;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;

public abstract class GenericSimulationThread implements Runnable{

	private static final int STATE_CREATED = 0;
	protected static final int STATE_RUNNING = 1;
	protected static final int STATE_STOPPED = 2;
	private static final int STATE_PAUSED = 3;
	

	protected static int state = STATE_CREATED;
	
	protected long interval = 5000;

	private Thread t = null;

	ICom comObject = null;
	
	public GenericSimulationThread(ICom comObject, long interval) {
		this.comObject = comObject;
		this.interval = interval;
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
	public abstract void run();



}
