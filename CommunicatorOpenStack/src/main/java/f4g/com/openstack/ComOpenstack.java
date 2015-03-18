package f4g.com.openstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.openstack.OSFactory;
import org.yaml.snakeyaml.Yaml;

import f4g.commons.com.AbstractCom;
import f4g.commons.com.util.ComOperation;
import f4g.commons.com.util.ComOperationCollector;
import f4g.commons.monitor.IMonitor;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.StandByAction;
import f4g.schemas.java.actions.StartJobAction;
import f4g.schemas.java.metamodel.FrameworkStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.VirtualMachine;

public class ComOpenstack extends AbstractCom {
    private final Logger log = Logger.getLogger(getClass());

    private OpenStackAPIs openstackAPI;

    @Override
    public boolean init(String comName, IMonitor monitor) {
	// comDemoStatus_ = "STOPPED";

	return super.init(comName, monitor);

    }

    @Override
    public void run() {

	// Start ComDemo
	log.debug("Start:" + this.comName);

	// initialize retrieve information
	openstackAPI = new OpenStackAPIs();
	openstackAPI.init();

	// while the state of fit4green plugin is not stopped retrieve data from
	// DC
	// and update the meta model XML
	while (state != STATE_STOPPED) {
	    // log.debug("state not stopped");
	    try {

		// // connect with the datacenter, in case of disconnection
		// reconnect
		// if(retrieveOSInfo.connect()){
		// // set the status of comDemo
		// if("Stopped".equals(comDemoStatus_) &&
		// !"Starting".equals(comDemoStatus_)){
		// // update the status in the model to starting
		// setStatus(FrameworkStatus.STARTING);
		// }
		// else if(!"Running".equals(comDemoStatus_)){
		// // update the status in the model to running
		// setStatus(FrameworkStatus.RUNNING);
		// }
		// }

		// start retrieving datacenter information
		// startRetrievingInformation();
		insertInformationDatamodel();

		Thread.sleep(10000);
	    } catch (InterruptedException e) {
		log.error("End of ComEni Loop", e);
		// e.printStackTrace();
	    }
	}

	// stop all the threads and free all the connections
	// datacenterInformation_.disconnect();
    }

    @Override
    public boolean powerOn(PowerOnAction action) {
	log.info("PowerOn action for id:" + action.getNodeName());
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean powerOff(PowerOffAction action) {
	log.info("PowerOff action for id:" + action.getNodeName());
	return false;
    }

    @Override
    public boolean liveMigrate(LiveMigrateVMAction action) {
	log.info("Livemigrate action for id:" + action.getVirtualMachine()
		+ " to dest " + action.getDestNodeController());
	return liveMigrate(action.getDestNodeController(),
		action.getVirtualMachine());

    }

    @Override
    public boolean moveVm(MoveVMAction action) {
	log.warn("Not implemented");
	return false;
    }

    @Override
    public boolean startJob(StartJobAction action) {
	log.warn("Not implemented");
	return false;
    }

    @Override
    public boolean standBy(StandByAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    /**
     * 
     * get the information from the datacenter and inserts them into FIT4Green
     * metamodel
     * 
     *
     * @author jclegea
     */
    /**
     * 
     * Creates a list of virtual machines.
     * 
     * @param actualVMsList
     *            to store the hosts list.
     *
     * @author jclegea
     */
    private Set<String> createHostVirtualMachineList(String serverId)
	    throws Exception {
	Set<String> actualVMsList = new HashSet<String>();
	try {
	    String key = this.comName + "_" + serverId;
	    HashMap serverMap = monitor.getMonitoredObjectsCopy(this.comName);
	    Server server = (Server) serverMap.get(key);
	    if (server != null) {
		List<VirtualMachine> virtualMachineList;
		virtualMachineList = server.getNativeHypervisor()
			.getVirtualMachine();
		for (Iterator iterator = virtualMachineList.iterator(); iterator
			.hasNext();) {
		    VirtualMachine serverVirtualMachine = (VirtualMachine) iterator
			    .next();
		    // log.debug("SERVERTYPE: " +
		    // serverVirtualMachine.getFrameworkID());
		    actualVMsList.add(serverVirtualMachine.getFrameworkID());
		}
	    }

	} catch (Exception exception) {
	    log.error("Cannot create actual virtual machine list");
	}
	return actualVMsList;
    }

    /**
     * 
     * Retrieve information from hosts like virtual machines and insert into
     * fit4gree model.
     * 
     * @return true if information is inserted, false otherwise.
     *
     * @author jclegea
     */
    private boolean insertInformationDatamodel() {
	ComOperation operation;
	// ArrayList<String> actualVMsList = new ArrayList<String>();
	boolean isInserted = false;
	log.debug("insertInformationDataModel");
	try {
	    for (String hyperVisorName : openstackAPI.getComputeNames()) {
		String key = this.comName + "_" + hyperVisorName;
		ComOperationCollector operations = new ComOperationCollector();
		// Test if host is in the model
		if (((ConcurrentLinkedQueue<ComOperationCollector>) this
			.getQueuesHashMap().get(key)) != null) {
		    Set<String> actualVMsList = createHostVirtualMachineList(hyperVisorName);

		    // ADD virtual machines for host in fit4green model
		    Set<String> hypervisorVMs = openstackAPI
			    .getVMsId(hyperVisorName);
		    for (String vmId : hypervisorVMs) {

			if (actualVMsList.contains(vmId) == false
				&& vmId != null) {
			    // ADD a virtual machine to the model

			    if (openstackAPI.getVMCPUs(vmId).isPresent()) {
				operation = new ComOperation(
					ComOperation.TYPE_ADD,
					ComOperation.VM_ON_HYPERVISOR_PATH,
					vmId
						+ " a a "
						+ openstackAPI.getVMCPUs(vmId)
							.get());
				operations.add(operation);

				((ConcurrentLinkedQueue<ComOperationCollector>) this
					.getQueuesHashMap().get(key))
					.add(operations);
				log.info("Adding VM: " + vmId);
				monitor.updateNode(key, this);
				operations.remove(operation);
			    }
			    ((ConcurrentLinkedQueue<ComOperationCollector>) this
				    .getQueuesHashMap().get(key)).poll();
			} else {
			    // Remove Virtual machine from actualVMsList
			    actualVMsList.remove(vmId);
			}
		    }

		    // DELETE virtual machines for host in fit4green model

		    for (String vmId : actualVMsList) {
			operation = new ComOperation(ComOperation.TYPE_REMOVE,
				ComOperation.VM_ON_HYPERVISOR_PATH, vmId
					+ " a a "
					+ openstackAPI.getVMCPUs(vmId));
			operations.add(operation);
			((ConcurrentLinkedQueue<ComOperationCollector>) this
				.getQueuesHashMap().get(key)).add(operations);
			monitor.updateNode(key, this);
			operations.remove(operation);
			((ConcurrentLinkedQueue<ComOperationCollector>) this
				.getQueuesHashMap().get(key)).poll();
		    }

		    ComOperationCollector operationSet = updateServerDynamicValues(hyperVisorName);

		    if (operationSet != null) {
			isInserted = monitor
				.simpleUpdateNode(key, operationSet);
		    }

		    monitor.logModel();
		}
	    }
	} catch (Exception exception) {
	    log.error("Exception inserting data on to the fit4green model",
		    exception);
	}
	return isInserted;
    }

    private ComOperationCollector updateServerDynamicValues(
	    String hyperVisorName) {
	ComOperationCollector operationSet = new ComOperationCollector();

	// get all virtual machines dynamic values from the datacenter
	// and add an update operation for each value

	// get the power status
	// TODO:
	// try {
	// powerState = retrieveOSInfo.getPowerState();
	// if ("ON".equals(powerState) == true) {
	// operationSet.add(new ComOperation(ComOperation.TYPE_UPDATE,
	// "./status", ServerStatus.ON));
	// } else {
	// operationSet.add(new ComOperation(ComOperation.TYPE_UPDATE,
	// "./status", ServerStatus.OFF));
	// }
	//
	// } catch (NullPointerException exception) {
	// // in case that is impossible to get the information, whatever the
	// // reason
	// // simply don't update the model
	// log.debug("Cannot retrieve CPU information from the DC");
	// }

	// get the measured power for the host
	// TODO: not implemented?

	// get the memory usage
	try {
	    openstackAPI.getUsedRAM(hyperVisorName).ifPresent(
		    mem -> operationSet.add(new ComOperation(
			    ComOperation.TYPE_UPDATE, ".[frameworkID='"
				    + hyperVisorName
				    + "']/mainboard/memoryUsage", String
				    .valueOf(mem))));
	} catch (NullPointerException exception) {
	    // in case that is impossible to get the information, whatever the
	    // reason
	    // simply don't update the model
	    log.debug("Cannot retrieve CPU information from the DC");
	}

	// get the hard diks usages
	// TODO: to be implemented?

	// get the CPU usages
	try {
	    // TODO: CPU Load
	    // openstackAPI.getCurrentWorkload(hyperVisorName)
	    // .ifPresent(
	    // cpuUsage -> operationSet.add(new ComOperation(
	    // ComOperation.TYPE_UPDATE,
	    // "/mainboard/CPU[frameworkID='"
	    // + hyperVisorName + "']/cpuUsage",
	    // String.valueOf(cpuUsage))));
	} catch (NullPointerException exception) {
	    // in case that is impossible to get the information, whatever
	    // the reason
	    // simply don't update the model
	    log.debug("Cannot retrieve read rate information from the HD");
	}

	// Core Load
	// TODO:

	return operationSet;
    }

    /**
     * Execute a list of actions on behalf of the Controller
     * 
     * @param actionRequest
     * @return true if successful, false otherwise
     */
    @Override
    public boolean executeActionList(ArrayList actionList) {

	PowerOnAction powerOnAction;
	PowerOffAction powerOffAction;
	LiveMigrateVMAction migrateAction;
	MoveVMAction moveAction;
	int i = 0;

	String key = null;
	ComOperation operation;
	ComOperationCollector operationSet = null;

	// initialize operationSet
	operationSet = new ComOperationCollector();

	// First
	log.debug(this.comName + ": executing action list...");
	JAXBElement<? extends AbstractBaseAction> elem;
	Iterator iter = actionList.iterator();
	while (iter.hasNext()) {
	    elem = (JAXBElement<? extends AbstractBaseAction>) iter.next();

	    Object action = elem.getValue();
	    action = elem.getValue().getClass().cast(action);
	    try {
		if (action.getClass().equals(PowerOffAction.class)) {
		    // perform power off action
		    powerOffAction = (PowerOffAction) action;

		    // set the status of powering on to the servers
		    key = comName + "_" + powerOffAction.getNodeName();
		    operation = new ComOperation(ComOperation.TYPE_UPDATE,
			    "./status", ServerStatus.POWERING_OFF);
		    operationSet.add(operation);
		    if (operationSet != null) {
			monitor.simpleUpdateNode(key, operationSet);
		    }

		    this.powerOff(powerOffAction);
		} else if (action.getClass().equals(PowerOnAction.class)) {
		    // perform power on action

		    powerOnAction = (PowerOnAction) action;

		    // set the status of powering on to the servers
		    key = comName + "_" + powerOnAction.getNodeName();
		    operation = new ComOperation(ComOperation.TYPE_UPDATE,
			    "./status", ServerStatus.POWERING_ON);
		    operationSet.add(operation);
		    if (operationSet != null) {
			monitor.simpleUpdateNode(key, operationSet);
		    }
		    // call the method to power on
		    this.powerOn(powerOnAction);
		} else if (action.getClass().equals(LiveMigrateVMAction.class)) {
		    // perform migrate vm action
		    migrateAction = (LiveMigrateVMAction) action;
		    this.liveMigrate(migrateAction);
		}
	    } catch (SecurityException e) {
		log.error("Exception", e);
	    } catch (IllegalArgumentException e) {
		log.error("Exception", e);
	    }
	}

	return true;
    }

    private boolean liveMigrate(String dstServerId, String vmId) {
	InputStream input = null;
	OSClient admin = null;
	try {

	    input = new FileInputStream(new File(
		    "src/main/config/ComOpenstack/config.yaml"));
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Yaml yaml = new Yaml();
	Map<String, String> config = (Map<String, String>) yaml.load(input);
	try {
	    admin = OSFactory
		    .builder()
		    .endpoint(
			    "http://" + config.get("ip") + ":"
				    + config.get("port") + "/v2.0")
		    .credentials(config.get("user"), config.get("password"))
		    .tenantName(config.get("tenant")).authenticate();
	} catch (AuthenticationException e) {

	    log.error("Connection to OpenStack datacenter fails: {}", e);
	}
	log.info("Dentro!!!");
	String dstServerIdClean = dstServerId.replace(".domain.tld", "");

	LiveMigrateOptions options = LiveMigrateOptions.create().host(
		dstServerIdClean);
	admin.compute().servers().liveMigrate(vmId, options);
	int i = 1;
	while (i < 120) {
	    i++;
	    admin.compute().servers().get(vmId.trim());
	    if (admin.compute().servers().get(vmId).getHypervisorHostname()
		    .equals(dstServerId)) {
		return true;
	    }
	    try {
		Thread.currentThread().sleep(1000);
	    } catch (InterruptedException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	}
	log.warn("Not successfully migrated");
	return false;
    }
}
