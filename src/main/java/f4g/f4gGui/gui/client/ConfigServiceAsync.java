package f4g.f4gGui.gui.client;

import java.util.Vector;

import f4g.f4gGui.gui.shared.ConfigurationData;

import com.google.gwt.user.client.rpc.AsyncCallback;
 
/**
 * The async counterpart of <code>ConfigurationService</code>.
 */
public interface ConfigServiceAsync {
	
	void getDirectoryTitles(AsyncCallback< Vector<String> > callback);	

	void getFileList(String directoryTitle, AsyncCallback< Vector<String> > callback);
	
	void newFile(String directoryTitle, String filename, String content, AsyncCallback<String> callback);

	void getFile(String directoryTitle, String filename, AsyncCallback<String> callback);

	void setFile(String directoryTitle, String filename, String content, AsyncCallback<String> callback);

	void renameFile(String directoryTitle, String oldFilename, String newFilename, AsyncCallback<String> callback);

	void deleteFile(String directoryTitle, String filename, AsyncCallback<String> callback);
	
	void getConfigurationData(AsyncCallback<ConfigurationData> callback);
	
}
