package f4g.f4gGui.gui.client;

import java.util.Vector;

import f4g.f4gGui.gui.shared.ActionData;
import f4g.f4gGui.gui.shared.Status;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */

@RemoteServiceRelativePath("ActionService")
public interface ActionService extends RemoteService {
	
	String startup() throws Exception;
	String shutdown() throws Exception;	
	String optimize() throws Exception;
	String approveActions() throws Exception;
	String cancelActions() throws Exception;
		
	Status getStatus() throws Exception;	
	Double getPowerReduction() throws Exception;	
	
	Vector<ActionData> getActionList() throws Exception;
	
	void setOptimizationObjective(int option) throws Exception;
	
}
