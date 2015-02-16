/**
* ============================== Header ============================== 
* file:          MoveVMThread.java
* project:       FIT4Green/CommunicatorEni
* created:       28/07/2011 by jclegea
* 
* $LastChangedDate: 2012-06-21 16:41:43 +0200 (jue, 21 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1497 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorEni.com;

import org.apache.log4j.Logger;
import f4g.schemas.java.actions.MoveVMAction;

import f4g.communicatorEni.vmware.MigrateRelocateVirtualMachine;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public class MoveVMThread extends Thread{
	static Logger log = Logger.getLogger(MoveVMThread.class.getName());
	
	int numCpus_ = -1;
	String[] actionArguments = new String[16];
	MoveVMAction action_;
	MigrateRelocateVirtualMachine migrateRelocateVirtualMachine_;
	
	public MoveVMThread(String url, String userName, String password, MoveVMAction action){				
		actionArguments[0] = "--" + ComEniConstants.URL; 
		actionArguments[1] = url;
		actionArguments[2] = "--" + ComEniConstants.USER_NAME;
		actionArguments[3] = userName;
		actionArguments[4] = "--" + ComEniConstants.PASSWORD;
		actionArguments[5] = password;
		actionArguments[6] = "--" + ComEniConstants.VIRTUAL_MACHINE_NAME;
		actionArguments[7] = action.getVirtualMachine();
		actionArguments[8] = "--" + ComEniConstants.TARGET_HOST;
		actionArguments[9] = action.getDestNodeController();
		actionArguments[10] = "--" + ComEniConstants.SOURCE_HOST;
		actionArguments[11] = action.getSourceNodeController();
		actionArguments[12] = "--" + ComEniConstants.TARGET_POOL;
		actionArguments[13] = "";
		actionArguments[14] = "--" + ComEniConstants.TARGET_DATA_STORE;
		actionArguments[15] = "";
		
		action_ = action;
		
		log.debug("About to relocate element with id="
				+ actionArguments[7] + " to "
				+ actionArguments[9] + " of framework "
				+ action.getFrameworkName() + " controller: " + action.getDestNodeController());
		migrateRelocateVirtualMachine_ = new MigrateRelocateVirtualMachine(actionArguments, ComEniConstants.MIGRATE,action_,null);
	}
	
	public int getNumCpus(){
		return numCpus_;
	}
	
	public String getVmName(){
		return actionArguments[7];
	}
	
	public String getTargetHostName(){
		return actionArguments[9];
	}

	public void run(){
		
		try {			
			boolean isMoved = false;			
			
			
			log.debug("virtual machine: " + action_.getVirtualMachine() + " source: " + action_.getSourceNodeController() + " destination: " + action_.getDestNodeController());			
			isMoved = migrateRelocateVirtualMachine_ .startMigrateOrRelocate(ComEniConstants.MIGRATE);
			numCpus_ = migrateRelocateVirtualMachine_.getNumCpus();
		} catch (Exception exception) {
			log.error("Action arguments cannot be initialized",exception);
		}

		return;
	}

}
