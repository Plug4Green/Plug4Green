package f4g.f4gGui.gui.client;

import java.util.Vector;

import f4g.f4gGui.gui.shared.ActionData;
import f4g.f4gGui.gui.shared.Status;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>ActionService</code>.
 */
public interface ActionServiceAsync {
	//void performAction(String input, AsyncCallback<String> callback);
	
	//void getStatus(AsyncCallback<Status> callback);	
	
	void startup(AsyncCallback<String> callback);
	void shutdown(AsyncCallback<String> callback);
	void optimize(AsyncCallback<String> callback);
	void approveActions(AsyncCallback<String> callback);
	void cancelActions(AsyncCallback<String> callback);
		
	void getStatus(AsyncCallback<Status> callback);
	void getPowerReduction(AsyncCallback<Double> callback);
	
	void getActionList(AsyncCallback< Vector<ActionData> > callback);
	
	void setOptimizationObjective(int option, AsyncCallback<Void> callback);;
}
