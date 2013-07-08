package f4g.commons.com;

import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.StartJobActionType;
import f4g.schemas.java.actions.StandByActionType;

/**
 * Interface representing all the possible operations allowed on a Com component
 * Every component must implement it. If a method is not supported, an error response
 * is returned.
 * 
 * @author FIT4Green
 *
 */
public interface IComOperationSet {

	boolean powerOn(PowerOnActionType action);
	
	boolean powerOff(PowerOffActionType action);
	
	boolean liveMigrate(LiveMigrateVMActionType action);
	
	boolean moveVm(MoveVMActionType action);
	
	boolean startJob(StartJobActionType action);
	
	boolean standBy(StandByActionType action);
	
}
