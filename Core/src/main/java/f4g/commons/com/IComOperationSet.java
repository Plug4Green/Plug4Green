package f4g.commons.com;

import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.StartJobAction;
import f4g.schemas.java.actions.StandByAction;

/**
 * Interface representing all the possible operations allowed on a Com component
 * Every component must implement it. If a method is not supported, an error response
 * is returned.
 * 
 * @author FIT4Green
 *
 */
public interface IComOperationSet {

	boolean powerOn(PowerOnAction action);
	
	boolean powerOff(PowerOffAction action);
	
	boolean liveMigrate(LiveMigrateVMAction action);
	
	boolean moveVm(MoveVMAction action);
	
	boolean startJob(StartJobAction action);
	
	boolean standBy(StandByAction action);
	
}
