/**
 * ============================== Header ============================== 
 * file:          MigrateVirtualMachine.java
 * project:       FIT4Green/CommunicatorEni
 * created:       24/11/2010 by jclegea
 * 
 * $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
 * $LastChangedBy: jclegea $
 * $LastChangedRevision: 1497 $
 * 
 * short description:
 *   Class used to migrate Virtual Machines
 * ============================= /Header ==============================
 */
package f4g.communicatorEni.vmware;



import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;

import com.vmware.apputils.AppUtil;
import com.vmware.apputils.OptionSpec;

import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.VirtualMachineMovePriority;
import com.vmware.vim.VirtualMachinePowerState;
import com.vmware.vim.VirtualMachineRelocateSpec;
import com.vmware.vim.VirtualMachineConfigInfo;

import f4g.communicatorEni.com.ComEniConstants; 
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;

/**
 * Migrate or Relocate virtual machines
 * 
 * 
 * @author jclegea
 */
public class MigrateRelocateVirtualMachine {
	static Logger log = Logger.getLogger(MigrateRelocateVirtualMachine.class.getName());

	private static AppUtil appUtil_;
	private OptionSpec[] optionalParameters_ = null;
	private String[] actionArguments_ = null;
	private MoveVMActionType moveAction_;
	private LiveMigrateVMActionType migrateAction_;
	private int numCpus_ = -1;
	 
	/**
	 * Constructor that initializes the action with the corresponding arguments.
	 *  
	 * @param actionArguments
	 * @param actionMigrateOrRelocate
	 * @param MoveVMActionType 
	 */
	public MigrateRelocateVirtualMachine(String[] actionArguments, String actionMigrateOrRelocate, MoveVMActionType moveAction, LiveMigrateVMActionType migrateAction) {
		
		optionalParameters_ = new OptionSpec[8];
		optionalParameters_ = constructOptions();
		log.debug("Migrate/Relocate: " + actionMigrateOrRelocate + " arguments[" + actionArguments.length + "] " + actionArguments[7]);

		try {
			actionArguments_ = actionArguments;
			appUtil_ = AppUtil.initialize(actionMigrateOrRelocate, optionalParameters_,actionArguments);			
			
			migrateAction_ = migrateAction;
			moveAction_ = moveAction;
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
	}
	
	/**
	 * 
	 * return the number of cpus of the Virtual Machine relocated
	 * 
	 * @return
	 *
	 * @author jclegea
	 */
	public int getNumCpus()
	{
		return numCpus_;
	}
	
	
	/**
	 * 
	 * validation of optionals parameters of Migration.
	 * 
	 * @return true if validations are correct, false otherwise.
	 * 
	 * @author jclegea
	 */
	private boolean customValidation(){
		boolean isValidated = true;
		if (appUtil_.option_is_set(ComEniConstants.STATE)) {
			String state = appUtil_.get_option(ComEniConstants.STATE);

			if (appUtil_.get_option(ComEniConstants.STATE) == null) {
				log.error("Value for state cannot be null\n");
				isValidated = false;
			} else {
				if (!state.equalsIgnoreCase(ComEniConstants.POWERED_ON)
						&& !state.equalsIgnoreCase(ComEniConstants.POWERED_OFF)
						&& !state.equalsIgnoreCase(ComEniConstants.SUSPENDED)) {
					log.error("Must specify 'poweredOn', 'poweredOff' or 'suspended' for 'state' option\n");
					isValidated = false;
				}
			}
		}
		if (appUtil_.option_is_set(ComEniConstants.PRIORITY)) {
			String prior = appUtil_.get_option(ComEniConstants.PRIORITY);
			if (appUtil_.get_option(ComEniConstants.PRIORITY) == null) {
				log.error("Value for priority cannot be null\n");
				isValidated = false;
			} else {
				if (!prior.equals(ComEniConstants.DEFAULT_PRIORITY) && 
						!prior.equals(ComEniConstants.HIGH_PRIORITY)
						&& !prior.equals(ComEniConstants.LOW_PRIORITY)) {
					log.error("Must specify 'defaultPriority',"	+ 
									  " 'highPriority 'or 'lowPriority'" + 
									  " for 'priority' option\n");
					isValidated = false;
				}
			}
		}

		return isValidated;
	}
	
	/**
	 * This function is used to check whether relocation is to be done or
	 * migration is to be done. If two hosts have a shared datastore then
	 * migration will be done and if there is no shared datastore relocation will
	 * be done.
	 * 
	 * @param String name of the source host.
	 * @param String name of the target host.
	 * @param String name of the target datastore.
	 * 
	 * @return String mentioning migration or relocation.
	 */
	private String check_operation_type(String targetHost, String sourceHost, String targetDS) {
		String operation = "";
		try {
//			log.debug("targetHost: " + targetHost + " sourceHost: " + sourceHost + " targetDS: " + targetDS);
			ManagedObjectReference targetHostMOR = getMor(targetHost, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference sourceHostMOR = getMor(sourceHost, ComEniConstants.HOST_SYSTEM, null);
			if ((targetHostMOR == null) || (sourceHostMOR == null)) {
				return "";
			}
			ManagedObjectReference[] dsTarget = (ManagedObjectReference[]) appUtil_.getServiceUtil().getDynamicProperty(targetHostMOR, ComEniConstants.DATA_STORE);
			ManagedObjectReference[] dsSource = (ManagedObjectReference[]) appUtil_.getServiceUtil().getDynamicProperty(sourceHostMOR,ComEniConstants.DATA_STORE);
			
			ManagedObjectReference targetHostDS = null;
			ManagedObjectReference sourceHostDS = null;
			if(targetDS != null && "".equals(targetDS) == false  ){
				targetHostDS = browseDataStoreMor(dsTarget,targetDS);	
				sourceHostDS = browseDataStoreMor(dsSource,targetDS);
				if ((targetHostDS != null) && (sourceHostDS != null)) {
					// we have a shared datastore we can do migration
					operation = ComEniConstants.MIGRATE;
				} else {
					operation = ComEniConstants.RELOCATE;
				}
			}
			else{
				// if targetDS is not defined try to migrate 
				operation = ComEniConstants.MIGRATE;
			}

		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}

		return operation;

	}


	/**
	 * 
	 * get the managed object references in the server, virtualmachines, host, 
	 * datastores...
	 * 
	 * @param name of the object to get.
	 * @param type of the object.
	 * @param root in the hierarchy of objects.
	 * 
	 * @return the managed object reference with name and type from the arguments. 
	 * 
	 * @author jclegea
	 */
	private ManagedObjectReference getMor(String name, String type,
			ManagedObjectReference root){

		ManagedObjectReference nameMor=null;
		try {
			nameMor = (ManagedObjectReference) appUtil_.getServiceUtil().getDecendentMoRef(root, type, name);

		if (nameMor == null) {
			log.error("Error:: " + name + " not found");
		} 
		}
		catch (Exception exception) {
			log.error("Exception",exception);	
		}
		
		return nameMor;
	}

	/**
	 * 
	 * Function that seeks a datastore within an array of managed objects.
	 * 
	 * @param datastoreMor an array of managed objects references.
	 * @param datastoreName to seek within managed objects.
	 * 
	 * @return dataStore found in the array, null otherwise.
	 * 
	 * @author jclegea
	 */
	private ManagedObjectReference browseDataStoreMor(ManagedObjectReference[] datastoreMor, String datastoreName) {
		ManagedObjectReference dataMOR = null;
		try {
			if (datastoreMor != null && datastoreMor.length > 0) {
				for (int i = 0; i < datastoreMor.length; i++) {
					String targetDatastoreName = (String) appUtil_.getServiceUtil().getDynamicProperty(datastoreMor[i], "summary.name");
					if (targetDatastoreName.equalsIgnoreCase(datastoreName)) {
						dataMOR = datastoreMor[i];
					}
				}
			}
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
		return dataMOR;
	}
	

	/**
	 * 
	 * migrate or relocate a virtual machines.
	 * 
	 * @param actionMigrateOrRelocate parameter that indicate if the action is 
	 * migrate or relocate.
	 * 
	 * @return true if action is success, false otherwise.
	 *
	 * @author jclegea
	 */
	public boolean migrateOrRelocateVM(String actionMigrateOrRelocate){
		// first we need to check if the VM should be migrated of relocated
		// If target host and source host both contains
		// the datastore, virtual machine needs to be migrated
		// If only target host contains the datastore,machine needs to be relocated

		String vmName = actionArguments_[7];
		String targetHost = actionArguments_[9];
		String targetPool = actionArguments_[13];
		String sourceHost = actionArguments_[11];
		String targetDatastore = actionArguments_[15];

		//log.debug("migorel: " + vmName + " get_option: " + appUtil_.get_option(ComEniConstants.VIRTUAL_MACHINE_NAME));
		
		String operationName = check_operation_type(targetHost, sourceHost, targetDatastore);
		boolean isMigratedOrRelocated = false;
		
		try{
		if (operationName.equalsIgnoreCase(ComEniConstants.MIGRATE) && 
				actionMigrateOrRelocate.equalsIgnoreCase(ComEniConstants.MIGRATE)) {
				isMigratedOrRelocated = migrateVM(vmName, targetPool, targetHost, sourceHost);
		}
		else {
			log.warn("Target Host incompatible with liveMigrate. Try using moveVm");
			isMigratedOrRelocated = relocateVM(vmName, targetPool, targetHost, targetDatastore, sourceHost);
		}
		
		}
		catch(Exception exception){
			log.error("Exception",exception);
		}
		
		
		return isMigratedOrRelocated;
	}

		
	/**
	 * 
	 * migrate a virtual machine.
	 * 
	 * @param vmname virtual machine to migrate.
	 * @param pool where to migrate the virtual machine.
	 * @param tHost target host to migrate the virtual machine.
	 * @param srcHost source host of the virtual machine to migrate.
	 * 
	 * @return true if action is success, false otherwise.
	 * 
	 * @author jclegea
	 */
	public boolean migrateVM(String vmName, String pool, String targetHost, String sourceHost){		
		VirtualMachinePowerState stateVirtualMachine = null;
		VirtualMachineMovePriority migratePriority = null;
		
		
		if (appUtil_.option_is_set(ComEniConstants.STATE)) {			
			stateVirtualMachine = VirtualMachinePowerState.fromString(appUtil_.get_option(ComEniConstants.STATE));
		}
		if (!appUtil_.option_is_set(ComEniConstants.PRIORITY)) {
			migratePriority = VirtualMachineMovePriority.defaultPriority;
		} else {
			migratePriority = VirtualMachineMovePriority.fromString(appUtil_.get_option(ComEniConstants.PRIORITY));
		}
		try {
			ManagedObjectReference sourceMOR = getMor(sourceHost, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference vmMOR = getMor(vmName, "VirtualMachine", sourceMOR);
			ManagedObjectReference poolMOR = null;
			ManagedObjectReference hostMOR = getMor(targetHost, ComEniConstants.HOST_SYSTEM, null);
			VirtualMachineConfigInfo virtualMachineConfig = null;
			boolean isMigrated=false;
			
			
			if (vmMOR == null || sourceMOR == null || hostMOR == null) {
				log.debug("vm not relocated");
				return false;
			}			
			
			// get the number of CPUs of the VM
			virtualMachineConfig = (VirtualMachineConfigInfo)appUtil_.getServiceUtil().getDynamicProperty(vmMOR,"config");			
			numCpus_ = virtualMachineConfig.getHardware().getNumCPU();			
			
			poolMOR = null;			
			ManagedObjectReference taskMOR = appUtil_.getConnection().getService().migrateVM_Task(vmMOR, poolMOR, hostMOR, migratePriority,stateVirtualMachine);
			
			// set the fowarded boolean
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			migrateAction_.setForwarded(true);
			migrateAction_.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));						
			
			// WAIT for task to be completed			
//			String res = appUtil_.getServiceUtil().waitForTask(taskMOR);
//						
//			if (res.equalsIgnoreCase(ComEniConstants.SUCCESS)) {
//				log.debug("Migration of Virtual Machine " + vmName + " done successfully to " + targetHost);
//				isMigrated=true;
//			} else {
//				log.error("Migration failed");
//				isMigrated=false;
//			}			
//			return isMigrated;
			return true;
		} 
		catch (Exception exception) {
			if (exception instanceof org.apache.axis.AxisFault) {
				// set the fowarded boolean
				if(migrateAction_ != null){					
					migrateAction_.setForwarded(false);
				}
				org.apache.axis.AxisFault fault = (org.apache.axis.AxisFault) exception;
				org.w3c.dom.Element[] errors = fault.getFaultDetails();
				for (int i = 0; i < errors.length; i++) {
					if (errors[i].toString().indexOf("InvalidPowerState") != -1) {
						log.error("The attempted operation cannot be performed in the current state");
					} else if (errors[i].toString().indexOf("SnapshotCopyNotSupported") != -1) {
						log.error("Migration of virtual machines with snapshots is not supported between the source and destination");
					} else if (errors[i].toString().indexOf("InvalidState") != -1) {
						log.error("Operation cannot be performed because of the virtual machine's current state");
					} else if (errors[i].toString().indexOf("VmConfigFault") != -1) {
						log.error("virtual machine is not compatible with the destination host");
					} else if (errors[i].toString().indexOf("InvalidArgument") != -1) {
						log.error("target host and target pool are not associated with the same compute resource ");
					} else if (errors[i].toString().indexOf("RuntimeFault") != -1) {
						log.error(errors[i].toString());
					} else {
						log.error(errors[i].toString());
					}
				}			
			}
			
		}
		return false;
	}
	
	/**
	 * 
	 * relocate a virtual machine.
	 * 
	 * @param vmname virtual machine to migrate.
	 * @param pool where to migrate the virtual machine.
	 * @param tHost target host to migrate the virtual machine.
	 * @param srcHost source host of the virtual machine to migrate.
	 * 
	 * @return true if action is success, false otherwise.
	 * 
	 * @author jclegea
	 */
	public boolean relocateVM(String vmName, String pool, String targetHost,String targetDataStore, String sourceHost){		
		try {
			ManagedObjectReference sourceMOR = getMor(sourceHost, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference vmMOR = getMor(vmName, "VirtualMachine", sourceMOR);
			ManagedObjectReference poolMOR = null;
			ManagedObjectReference hostMOR = getMor(targetHost, ComEniConstants.HOST_SYSTEM, null);
			ManagedObjectReference[] datastoreTarget = (ManagedObjectReference[]) appUtil_.getServiceUtil().getDynamicProperty(hostMOR,ComEniConstants.DATA_STORE);
			ManagedObjectReference dataStoreMOR = browseDataStoreMor(datastoreTarget, targetDataStore);
			VirtualMachineConfigInfo virtualMachineConfig = null;
			
			if (dataStoreMOR == null) {
				log.error("Datastore " + targetDataStore + " not found");
			}
			if (vmMOR == null || dataStoreMOR == null || hostMOR == null || dataStoreMOR == null) {
				return false;
			}
			VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
			relocateSpec.setDatastore(dataStoreMOR);
			relocateSpec.setHost(hostMOR);
//			log.debug("Relocating the Virtual Machine " + vmName);
			ManagedObjectReference taskMOR = appUtil_.getConnection().getService()
					.relocateVM_Task(vmMOR, relocateSpec);

			// get the number of CPUs of the VM
			virtualMachineConfig = (VirtualMachineConfigInfo)appUtil_.getServiceUtil().getDynamicProperty(vmMOR,"config");
			numCpus_ = virtualMachineConfig.getHardware().getNumCPU();

			// set the fowarded boolean
			TimeZone gmt = TimeZone.getTimeZone("GMT");
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(gmt);
			moveAction_.setForwarded(true);
			moveAction_.setForwardedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
			
			// WAIT for task to be completed
//			String res = appUtil_.getServiceUtil().waitForTask(taskMOR);
//			boolean isRelocated = false;
//			if (res.equalsIgnoreCase(ComEniConstants.SUCCESS)) {
//				log.debug("Relocation done successfully of " + vmName
//						+ "to host " + targetHost);
//				isRelocated = true;
//			} else {
//				log.error("Relocation failed");
//				isRelocated = false;
//			}
//			return isRelocated;
			return true;
		} 
		catch (Exception exception) {
			if (exception instanceof org.apache.axis.AxisFault) {
				// set the fowarded boolean
				if(migrateAction_ != null){					
					migrateAction_.setForwarded(false);
				}				
				org.apache.axis.AxisFault fault = (org.apache.axis.AxisFault) exception;
				org.w3c.dom.Element[] errors = fault.getFaultDetails();
				for (int i = 0; i < errors.length; i++) {
					if (errors[i].toString().indexOf("InvalidPowerState") != -1) {
						log.error("The attempted operation cannot be performed in the current state");
					} else if (errors[i].toString().indexOf("SnapshotCopyNotSupported") != -1) {
						System.out
								.println("Migration of virtual machines with snapshots is not supported between the source and destination");
					} else if (errors[i].toString().indexOf("InvalidState") != -1) {
						log.error("Operation cannot be performed because of the virtual machine's current state");
					} else if (errors[i].toString().indexOf("VmConfigFault") != -1) {
						log.error("virtual machine is not compatible with the destination host");
					} else if (errors[i].toString().indexOf("NotSupported") != -1) {
						System.out
								.println("The operation is not supported on the Virtual Machine the same compute resource ");
					} else if (errors[i].toString().indexOf("InvalidArgument") != -1) {
						System.out
								.println("target host and target pool are not associated with the same compute resource ");
					} else if (errors[i].toString().indexOf("RuntimeFault") != -1) {
						log.error(errors[i].toString());
					} else {
						log.error(errors[i].toString());
					}
				}
			} 
		}
		return false;
	}

	/**
	 * 
	 * initialize optional arguments to the actions.
	 * 
	 * @return an Array of optional arguments
	 *
	 * @author jclegea
	 */
  public static OptionSpec[] constructOptions() {
    OptionSpec [] useroptions = new OptionSpec[8];
    useroptions[0] = new OptionSpec("vmname","String",1,
                         "Name of the virtual machine"
                                    ,null);
    useroptions[1] = new OptionSpec("targethost","String",1,
                         "Target host on which VM is to be migrated",
                                    null);
    useroptions[2] = new OptionSpec("targetpool","String",1,
                         "Name of the target resource pool",
                                    null);
    useroptions[3] = new OptionSpec("priority","String",0,
                         "The priority of the migration task: defaultPriority, highPriority, lowPriority",
                                    null);
    useroptions[4] = new OptionSpec("validate","String",0,
                         "Check whether the vmotion feature is legal for 4.0 servers",
                                    null);
    useroptions[5] = new OptionSpec("sourcehost","String",1,
                         "Name of the host containg the virtual machine.",
                                    null);        
    useroptions[6] = new OptionSpec("targetdatastore","String",1,
                         "Name of the target datastore",
                                    null);        
    useroptions[7] = new OptionSpec("state","String",0,
                         "State of the VM poweredon,poweredoff, suspended",
                         null);
    return useroptions;
 }
  
  public String getVMName(){
  	
  	return actionArguments_[7];
  }
	
	
	/**
	 * 
	 * entry point to migrate or relocate a virtual machine.
	 * 
	 * @param actionMigrateOrRelocate indicates if is a migration or a relocation.
	 * 
	 * @return true if action is success, false otherwise.
	 *
	 * @author jclegea
	 */
	public boolean startMigrateOrRelocate(String actionMigrateOrRelocate) {
		boolean valid;
		boolean isMigratedOrRelocated = false;
		try {
			valid = customValidation();
			if (valid) {				
				appUtil_ = AppUtil.initialize(actionMigrateOrRelocate,
																			optionalParameters_,actionArguments_);
				
				appUtil_.connect();				
				isMigratedOrRelocated = migrateOrRelocateVM(actionMigrateOrRelocate);
				if(appUtil_.getConnection().isConnected()){
					appUtil_.disConnect();
				}
			}			
		} 
		catch (Exception exception) {
			log.error("Exception",exception);
		}
		return isMigratedOrRelocated;
	}

}
