package org.f4g.com;

import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.StartJobActionType;
import org.f4g.schema.actions.StandByActionType;

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
