package org.f4g.gui.client;

import java.util.Vector;

import org.f4g.gui.shared.ActionData;
import org.f4g.gui.shared.Status;

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
