package org.f4g.gui.client;


import java.util.Vector;

import org.f4g.gui.shared.ConfigurationData;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("ConfigService")
public interface ConfigService extends RemoteService {

	Vector<String> getDirectoryTitles()
		throws Exception;

	Vector<String> getFileList(String directoryTitle)
		throws Exception;

	String newFile(String directoryTitle, String filename, String content) 
		throws Exception;
	
	String getFile(String directoryTitle, String filename) 
		throws Exception;

	String setFile(String directoryTitle, String filename, String content) 
		throws Exception;

	String renameFile(String directoryTitle, String oldFilename, String newFilename) 
		throws Exception;


	String deleteFile(String directoryTitle, String filename)
		throws Exception;
	
	ConfigurationData getConfigurationData() 
		throws Exception;
}
