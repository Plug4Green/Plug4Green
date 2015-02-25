package f4g.commons.com;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.log4j.Logger;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.HostedHypervisor;
import f4g.schemas.java.metamodel.IoRate;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.NativeHypervisor;
import f4g.schemas.java.metamodel.NetworkUsage;
import f4g.schemas.java.metamodel.NrOfCpus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.StorageUsage;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.StandByAction;
import f4g.schemas.java.actions.StartJobAction;

public abstract class AbstractCom implements ICom, IComOperationSet, Runnable {
	static Logger log = Logger.getLogger(AbstractCom.class.getName()); //

	protected static final int STATE_CREATED = 0;
	protected static final int STATE_RUNNING = 1;
	protected static final int STATE_STOPPED = 2;
	protected static final int STATE_PAUSED = 3;

	protected static int state = STATE_CREATED;

	// TODO: to be set from configuration
	protected long interval = 5000;

	HashMap monitoredObjects = null;

	private Thread t = null;

	private HashMap queuesHashMap = new HashMap();

	protected IMonitor monitor = null;

	protected String comName = null;

	public AbstractCom() {
		log.debug(this.getClass().getCanonicalName() + " instantiated");
	}

	/**
	 * Initialize the component. Retrieves from the Monitor a list of the node
	 * objects monitored by the component. Creates a new map with the same keys,
	 * but for each key (meaning for each node) e queue is created. The queue
	 * will contain the list of pending updates to be performed.
	 */
	@Override
	public boolean init(String comName, IMonitor monitor) {
		// TODO Auto-generated method stub

		this.comName = comName;

		// Create the mapping
		this.monitor = monitor;

		log.debug("Getting objectsMap for com " + comName);
		monitoredObjects = monitor.getMonitoredObjectsMap(comName);
		log.debug("Got " + monitoredObjects.size() + " elements");

		Iterator iter = monitoredObjects.keySet().iterator();
		while (iter.hasNext()) {
			queuesHashMap.put((String) (iter.next()),
					new ConcurrentLinkedQueue<ComOperationCollector>());
		}

		t = new Thread(this);
		start();

		return true;

	}

	public void start() {
		state = STATE_RUNNING;
		t.start();
	}

	public void stop() {
		state = STATE_STOPPED;
		if (t != null) {
			t.interrupt();
		}
		t = null;
	}

	public void pause() {
		state = STATE_PAUSED;
	}

	@Override
	public boolean startUpdate() {
		start();
		return true;
	}

	@Override
	public boolean stopUpdate() {
		pause();
		return true;
	}

	/* (non-Javadoc)
	 * @see f4gcom.ICom#dispose()
	 */
	@Override
	public boolean dispose() {
		stop();
		return true;
	}
	
	/**
	 * Execute a list of actions on behalf of the Controller
	 * 
	 * @param actionRequest
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean executeActionList(ArrayList actionList) {

		log.debug(this.comName + ": executing action list...");
		JAXBElement<? extends AbstractBaseAction> elem;
		Iterator iter = actionList.iterator();
		boolean result = true;
		try {
			while (iter.hasNext()) {

				elem = (JAXBElement<? extends AbstractBaseAction>) iter
						.next();

				Object action = elem.getValue();
				action = elem.getValue().getClass().cast(action);

				boolean actionResult = false;
				try {
					if (action.getClass().equals(PowerOffAction.class)) {
						actionResult = this.powerOff((PowerOffAction) action);
					} else if (action.getClass().equals(PowerOnAction.class)) {
						actionResult = this.powerOn((PowerOnAction) action);
					} else if (action.getClass().equals(LiveMigrateVMAction.class)) {
						actionResult = this.liveMigrate((LiveMigrateVMAction) action);
					} else if (action.getClass().equals(MoveVMAction.class)) {
						actionResult = this.moveVm((MoveVMAction) action);
					} else if (action.getClass().equals(StartJobAction.class)) {
						actionResult = this.startJob((StartJobAction) action);
					} else if (action.getClass().equals(StandByAction.class)) {
						actionResult = this.standBy((StandByAction) action);
					} else {
						log.error("Invalid action");
					}
					
					((AbstractBaseAction)action).setForwarded(actionResult);
					if(actionResult){
						try {
							TimeZone gmt = TimeZone.getTimeZone("GMT");
							GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
							((AbstractBaseAction)action).setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
							log.debug("Setting forwarderAt to " + ((AbstractBaseAction)action).getForwardedAt());
						} catch (DatatypeConfigurationException e) {
							log.error("Error in setting 'forwardedAt' datetime");
							log.error(e);
						}
					}
				

				} catch (SecurityException e) {
					log.error(e);
				} catch (IllegalArgumentException e) {
					log.error(e);
				}

			}
		} catch (Exception e) {
			log.error("General exception: ", e);
			result = false;
		}

		return result;
	}

	/**
	 * This method is invoked as a 'callback' operation by the Monitor
	 * component. The Monitor passes a node of the model into the obj parameter.
	 * The Com retrieves a set of update operations queued for this node and
	 * apply it to the node
	 * 
	 * @param key
	 *            key of the mapping for the Com object
	 * @param obj
	 *            a node in the f4g model
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean executeUpdate(String key, Object obj) {
		long start = System.currentTimeMillis();

		
		// Get the type of the object
		String type = (String) monitoredObjects.get(key);

		log.debug("Performing COMPLEX updates on object:");
		log.debug("\t key: " + key);
		log.debug("\t type: " + type);
		log.debug("\t obj: " + obj);

		ConcurrentLinkedQueue<ComOperationCollector> actionSetsQueue = (ConcurrentLinkedQueue<ComOperationCollector>) (queuesHashMap
				.get(key));
		for (int i = 0; i < actionSetsQueue.size(); i++) {
			ComOperationCollector operationSet = actionSetsQueue.poll();
			log.debug("operationSet: " + operationSet.getOperations().size()
					+ " elements");

			if (operationSet.getOperations() != null) {
				Iterator iter = operationSet.getOperations().iterator();
				ComOperation operation = null;
				while (iter.hasNext()) {
					operation = (ComOperation) iter.next();
					log.debug("Processing operation: " + operation.getType()
							+ ", " + operation.getExpression() + ", "
							+ operation.getValue());
					if (operation.getType().equals(ComOperation.TYPE_UPDATE)) {
						JXPathContext context = JXPathContext.newContext(obj);
						context.setValue(operation.getExpression(),
								operation.getValue());
					} else if (operation.getType().equals(ComOperation.TYPE_ADD) || 
							operation.getType().equals(ComOperation.TYPE_REMOVE)) {
						// Code for adding virtual machines
						if (obj instanceof Server) {
							String expr = operation.getExpression();
							String[] values = operation.getValue().split(" ");

							if (expr.equals(ComOperation.VM_ON_OS_PATH)) {

								// !!!!HINT!!!! here you can have more
								// hosted hypervisors! To be defined how to get
								// the right one!
								// Maybe add the frameworkId?
								if (operation.getType().equals(
										ComOperation.TYPE_ADD)) {
									
									VirtualMachine vm = fillVmData(values);

									vm.setFrameworkRef(((Server) obj).getFrameworkRef());
									((Server) obj)
											.getNativeOperatingSystem()
											.getHostedHypervisor().get(0)
											.getVirtualMachine().add(vm);
									vm.setFrameworkRef(((Server) obj).getFrameworkRef());
								} else {
									removeVirtualMachine(((Server) obj)
											.getNativeOperatingSystem()
											.getHostedHypervisor().get(0)
											.getVirtualMachine(), values[0]);
								}
							} else if (expr
									.equals(ComOperation.VM_ON_HYPERVISOR_PATH)) {
								if (operation.getType().equals(
										ComOperation.TYPE_ADD)) {
									
									VirtualMachine vm = fillVmData(values);

									vm.setFrameworkRef(((Server) obj).getFrameworkRef());
									((Server) obj).getNativeHypervisor()
											.getVirtualMachine().add(vm);
								} else {
									removeVirtualMachine(((Server) obj)
											.getNativeHypervisor()
											.getVirtualMachine(), values[0]);
								}

							} else {
								log.error("Wrong operation expression!");
							}
						}
					}
					// (if there are any other operations) else...
				}
			}
		}

		log.debug("Time Metric: Abstract Com execute update task took" + (System.currentTimeMillis()-start) + " ms.");
		return true;
	}

	@Override
	//FIXME sleep action is not an option anymore; find workaround if necessary
//	public boolean sleep(SleepActionType action) {
//		log.debug("About to sleep for " + action.getSeconds()
//				+ " secs. on framework " + action.getFrameworkName());
//		try {
//			Thread.sleep((action.getSeconds().longValue()) * 1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		log.debug("...awaken after sleep");
//		return true;
//	}

	public HashMap getMonitoredObjects() {
		return monitoredObjects;
	}

	public HashMap getQueuesHashMap() {
		return queuesHashMap;
	}

	@Override
	public abstract void run();

	private boolean removeVirtualMachine(List<VirtualMachine> vmList,
			String id) {
		for (int i = 0; i < vmList.size(); i++) {
			VirtualMachine vm = vmList.get(i);
			if (vm.getFrameworkID().equals(id)) {
				vmList.remove(i);
				log.debug("VM removed!");
				return true;
			}
		}
		log.debug("VM NOT removed! (not in list of vms)");
		return false;
	}
	
	private VirtualMachine fillVmData(String[] values){
		VirtualMachine vm = new VirtualMachine();
		vm.setFrameworkID(values[0]);
		vm.setCloudVmImage(values[1]);
		vm.setCloudVm(values[2]);
		
		//HINT! Here we add by convention in 4th position the number of CPUs
		//assuming that they are passed in the xpath query
		if(values.length > 3){
			NrOfCpus nOfCpus = new NrOfCpus();
			nOfCpus.setValue(Integer.valueOf(values[3]));
			vm.setNumberOfCPUs(nOfCpus);
		}
		//HINT! Here we add by convention in 5th position the vm name
		//assuming that they are passed in the xpath query
		if(values.length > 4){
			vm.setName(values[4]);
		}
		
		//TODO: to be verified if they must be set as mandatory in the model (so that they are created automatically)
//		CpuUsage cpuUsage = new CpuUsage();
//		cpuUsage.setValue(0.0);
//		vm.setActualCPUUsage(cpuUsage);
//		
//		IoRate ioRate = new IoRate();
//		ioRate.setValue(0.0);
//		vm.setActualDiskIORate(ioRate);
//		
//		MemoryUsage memoryUsage = new MemoryUsage();
//		memoryUsage.setValue(0.0);
//		vm.setActualMemoryUsage(memoryUsage);
//		
//		NetworkUsage networkUsage = new NetworkUsage();
//		networkUsage.setValue(0.0);
//		vm.setActualNetworkUsage(networkUsage);
//		
//		StorageUsage storageUsage = new StorageUsage();
//		storageUsage.setValue(0.0);
//		vm.setActualStorageUsage(storageUsage);
		
		try {
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			vm.setLastMigrationTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
		} catch (DatatypeConfigurationException e) {
			log.error("Error in setting VM lastMigrationTimestamp: ", e);
		}
		return vm;
	}
}
