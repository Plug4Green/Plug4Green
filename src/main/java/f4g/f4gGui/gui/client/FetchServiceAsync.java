package f4g.f4gGui.gui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
 
/**
 * The async counterpart of <code>CouchService</code>.
 */
public interface FetchServiceAsync {
	void get(String url, AsyncCallback<String> callback);
	void get(String url, String contentType, AsyncCallback<String> callback);
	void post(String url, String body, AsyncCallback<String> callback);
	void post(String url, String body, String contentType, 
			AsyncCallback<String> callback);
	void put(String url, String body, AsyncCallback<String> callback);
	void put(String url, String body, String contentType, 
			AsyncCallback<String> callback);
}
