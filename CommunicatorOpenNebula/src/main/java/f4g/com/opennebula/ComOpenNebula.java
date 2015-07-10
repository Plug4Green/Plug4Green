package f4g.com.opennebula;

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

public class ComOpenNebula extends AbstractCom {
    private final Logger log = Logger.getLogger(getClass());

    private OpenNebulaAPIs opennebulaAPI;

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
	opennebulaAPI = new OpenNebulaAPIs();
	opennebulaAPI.init();

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
	InputStream input = null;
        try {

            input = new FileInputStream(new File("src/main/config/Ipmi/config.yaml"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> config = (Map<String, Map<String, String>>) yaml.load(input);
	String ipipmi = config.get(action.getNodeName()).get("ipipmi");
	Ipmi ipmi = new Ipmi();
	ipmi.init(ipipmi);
	if (ipmi.getStatus().equals("down")) ipmi.powerOn();
	ipmi.endSession();

	opennebulaAPI.enableHost(action.getNodeName());

//em falta posar en estat ON el host a ONE

	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean powerOff(PowerOffAction action) {
	log.info("PowerOff action for id:" + action.getNodeName());
        InputStream input = null;
        try {

            input = new FileInputStream(new File("src/main/config/Ipmi/config.yaml"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> config = (Map<String, Map<String, String>>) yaml.load(input);
        String ipipmi = config.get(action.getNodeName()).get("ipipmi");
        Ipmi ipmi = new Ipmi();
        ipmi.init(ipipmi);
        if (ipmi.getStatus().equals("up")) ipmi.powerOff();
        ipmi.endSession();

        opennebulaAPI.disableHost(action.getNodeName());

//em falta posar en estat OFF el host a ONE

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
        log.info("Migrate action for id:" + action.getVirtualMachine()
                + " to dest " + action.getDestNodeController());
        return moveVm(action.getDestNodeController(),
                action.getVirtualMachine());
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
	    for (String hyperVisorName : opennebulaAPI.getComputeNames()) {
		String key = this.comName + "_" + hyperVisorName;
		ComOperationCollector operations = new ComOperationCollector();

		// Test if host is in the model
		if (((ConcurrentLinkedQueue<ComOperationCollector>) this
			.getQueuesHashMap().get(key)) != null) {
		    Set<String> actualVMsList = createHostVirtualMachineList(hyperVisorName);

		    // ADD virtual machines for host in fit4green model
		    Set<String> hypervisorVMs = opennebulaAPI
			    .getVMsId(hyperVisorName);
		    for (String vmId : hypervisorVMs) {

			if (actualVMsList.contains(vmId) == false
				&& vmId != null) {
			    // ADD a virtual machine to the model

			    if (opennebulaAPI.getVMCPUs(new Integer(vmId)).isPresent()) {
				operation = new ComOperation(
					ComOperation.TYPE_ADD,
					ComOperation.VM_ON_HYPERVISOR_PATH,
					vmId
						+ " a a "
						+ opennebulaAPI.getVMCPUs(new Integer(vmId))
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
					+ opennebulaAPI.getVMCPUs(new Integer(vmId)));
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

//		    monitor.logModel();
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

 	 try {
 	 	String powerState = opennebulaAPI.getPowerState(hyperVisorName);
 		 if ("ON".equals(powerState) == true) {
 			 operationSet.add(new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatus.ON));
 		 } else {
 			 operationSet.add(new ComOperation(ComOperation.TYPE_UPDATE, "./status", ServerStatus.OFF));
		 }
 	
 	} catch (NullPointerException exception) {
	 log.debug("Cannot retrieve CPU information from the DC");
	}

	// get the measured power for the host
	// TODO: not implemented?

	// get the memory usage
	try {
	    opennebulaAPI.getUsedRAM(hyperVisorName).ifPresent(
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
		return moveVm(dstServerId, vmId);
	}

    private boolean moveVm(String dstServerId, String vmId) {
        opennebulaAPI = new OpenNebulaAPIs();
        opennebulaAPI.init();
	if (opennebulaAPI.migrateVM(dstServerId, new Integer(vmId)))
	{
		log.info("Virtual Machine "+vmId+" successfull migrated");
		return true;
	}else{
		log.warn("Not successfully migrated");
		return false;
	}
    }
}
