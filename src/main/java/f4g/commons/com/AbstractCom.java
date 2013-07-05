package org.f4g.com;

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
import org.f4g.com.util.ComOperation;
import org.f4g.com.util.ComOperationCollector;
import org.f4g.monitor.IMonitor;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.HostedHypervisorType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NativeHypervisorType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.NrOfCpusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageUsageType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.StandByActionType;
import org.f4g.schema.actions.StartJobActionType;

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

	IMonitor monitor = null;

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
	 * @see org.f4g.com.ICom#dispose()
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
		JAXBElement<? extends AbstractBaseActionType> elem;
		Iterator iter = actionList.iterator();
		boolean result = true;
		try {
			while (iter.hasNext()) {

				elem = (JAXBElement<? extends AbstractBaseActionType>) iter
						.next();

				Object action = elem.getValue();
				action = elem.getValue().getClass().cast(action);

				boolean actionResult = false;
				try {
					if (action.getClass().equals(PowerOffActionType.class)) {
						actionResult = this.powerOff((PowerOffActionType) action);
					} else if (action.getClass().equals(PowerOnActionType.class)) {
						actionResult = this.powerOn((PowerOnActionType) action);
					} else if (action.getClass().equals(LiveMigrateVMActionType.class)) {
						actionResult = this.liveMigrate((LiveMigrateVMActionType) action);
					} else if (action.getClass().equals(MoveVMActionType.class)) {
						actionResult = this.moveVm((MoveVMActionType) action);
					} else if (action.getClass().equals(StartJobActionType.class)) {
						actionResult = this.startJob((StartJobActionType) action);
					} else if (action.getClass().equals(StandByActionType.class)) {
						actionResult = this.standBy((StandByActionType) action);
					} else {
						log.error("Invalid action");
					}
					
					((AbstractBaseActionType)action).setForwarded(actionResult);
					if(actionResult){
						try {
							TimeZone gmt = TimeZone.getTimeZone("GMT");
							GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
							((AbstractBaseActionType)action).setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
							log.debug("Setting forwarderAt to " + ((AbstractBaseActionType)action).getForwardedAt());
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
						if (obj instanceof ServerType) {
							String expr = operation.getExpression();
							String[] values = operation.getValue().split(" ");

							if (expr.equals(ComOperation.VM_ON_OS_PATH)) {

								// !!!!HINT!!!! here you can have more
								// hosted hypervisors! To be defined how to get
								// the right one!
								// Maybe add the frameworkId?
								if (operation.getType().equals(
										ComOperation.TYPE_ADD)) {
									
									VirtualMachineType vm = fillVmData(values);

									vm.setFrameworkRef(((ServerType) obj).getFrameworkRef());
									((ServerType) obj)
											.getNativeOperatingSystem()
											.getHostedHypervisor().get(0)
											.getVirtualMachine().add(vm);
									vm.setFrameworkRef(((ServerType) obj).getFrameworkRef());
								} else {
									removeVirtualMachine(((ServerType) obj)
											.getNativeOperatingSystem()
											.getHostedHypervisor().get(0)
											.getVirtualMachine(), values[0]);
								}
							} else if (expr
									.equals(ComOperation.VM_ON_HYPERVISOR_PATH)) {
								if (operation.getType().equals(
										ComOperation.TYPE_ADD)) {
									
									VirtualMachineType vm = fillVmData(values);

									vm.setFrameworkRef(((ServerType) obj).getFrameworkRef());
									((ServerType) obj).getNativeHypervisor()
											.getVirtualMachine().add(vm);
								} else {
									removeVirtualMachine(((ServerType) obj)
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

	private boolean removeVirtualMachine(List<VirtualMachineType> vmList,
			String id) {
		for (int i = 0; i < vmList.size(); i++) {
			VirtualMachineType vm = vmList.get(i);
			if (vm.getFrameworkID().equals(id)) {
				vmList.remove(i);
				log.debug("VM removed!");
				return true;
			}
		}
		log.debug("VM NOT removed! (not in list of vms)");
		return false;
	}
	
	private VirtualMachineType fillVmData(String[] values){
		VirtualMachineType vm = new VirtualMachineType();
		vm.setFrameworkID(values[0]);
		vm.setCloudVmImage(values[1]);
		vm.setCloudVmType(values[2]);
		
		//HINT! Here we add by convention in 4th position the number of CPUs
		//assuming that they are passed in the xpath query
		if(values.length > 3){
			NrOfCpusType nOfCpus = new NrOfCpusType();
			nOfCpus.setValue(Integer.valueOf(values[3]));
			vm.setNumberOfCPUs(nOfCpus);
		}
		//HINT! Here we add by convention in 5th position the vm name
		//assuming that they are passed in the xpath query
		if(values.length > 4){
			vm.setName(values[4]);
		}
		
		//TODO: to be verified if they must be set as mandatory in the model (so that they are created automatically)
//		CpuUsageType cpuUsage = new CpuUsageType();
//		cpuUsage.setValue(0.0);
//		vm.setActualCPUUsage(cpuUsage);
//		
//		IoRateType ioRate = new IoRateType();
//		ioRate.setValue(0.0);
//		vm.setActualDiskIORate(ioRate);
//		
//		MemoryUsageType memoryUsage = new MemoryUsageType();
//		memoryUsage.setValue(0.0);
//		vm.setActualMemoryUsage(memoryUsage);
//		
//		NetworkUsageType networkUsage = new NetworkUsageType();
//		networkUsage.setValue(0.0);
//		vm.setActualNetworkUsage(networkUsage);
//		
//		StorageUsageType storageUsage = new StorageUsageType();
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
