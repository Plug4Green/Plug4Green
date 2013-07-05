package org.f4g.gui.server;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.f4g.core.Constants;
import org.f4g.gui.client.ConfigService;
import org.f4g.gui.shared.ConfigurationData;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ConfigServiceImpl extends RemoteServiceServlet
	implements ConfigService {
	
	private static final long serialVersionUID = 1L;
	
	// TODO logging is not working
	// same goes for Action and Fetch Service
	static Logger log = Logger.getLogger(ActionServiceImpl.class.getName());

	// FIXME currently file path is based on source files...
	// *.properties files is not the one found within tomcat server
	// creates problem when deploying while not using ISO image!!!
	private static final String CONFIG_FILE = "config/f4ggui.properties";
	private static final String PROPERTIES_FILE = "config/f4gconfig.properties";
	
	Map<String, String> directories = new HashMap<String, String>();
	ConfigurationData conf = new ConfigurationData();
	
	/* TODO: constructor gives problems
	ConfigServiceImpl() {
		super();
	}
	
	ConfigServiceImpl(java.lang.Object delegate) {
		super(delegate);
	}
	*/
	
	/**
	 * retrieve the directory location from a directory title
	 */
	private String getDirectory(String directoryTitle) 
			throws Exception {
		try {
			if (directories.containsKey(directoryTitle)) {
				return directories.get(directoryTitle);
			}
			
			// TODO: nicer error
			throw new Exception("Corresponding directory for '" + directoryTitle + "' not found");
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
	}
	
	/**
	 * Read the directories from the configuration file
	 */
	private void loadConfig() throws Exception {
		
		try {
			directories.clear();

			Properties properties = new Properties();
			ClassLoader cl = this.getClass().getClassLoader();
			InputStream is = cl.getResourceAsStream(CONFIG_FILE);
			if (is != null)
				properties.load(is);
			else 
				throw new Exception("Cannot find configuration file");

			//properties.load(new FileInputStream(CONFIG_FILE));
			for(String key : properties.stringPropertyNames()) {
				  String value = properties.getProperty(key);
				  directories.put(key, value);
			}
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
		
	}

	private void loadConfigurationProperties() throws Exception {
		
		try {
			
			Properties properties = new Properties();
			ClassLoader cl = this.getClass().getClassLoader();
			InputStream is = cl.getResourceAsStream(PROPERTIES_FILE);
			if (is != null)
				properties.load(is);
			else 
				throw new Exception("Cannot find FIT4Green configuration file");
			
			conf.setUrlDB(properties.getProperty(Constants.DB_URL));
			conf.setActionsDB(properties.getProperty(Constants.ACTIONS_DB_NAME));
			conf.setModelsDB(properties.getProperty(Constants.MODELS_DB_NAME));
			
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
		
	}
	
	/**
	 * retrieve the available directory titles
	 */
	@Override
	public Vector<String> getDirectoryTitles() throws Exception {
		Vector<String> titles = new Vector<String>();

		try {
			// TODO: put loading the configuration inside the constructor?
			loadConfig();
		
			Iterator<String> iterator = directories.keySet().iterator();
			   
			while(iterator.hasNext()){        
				String key = iterator.next();
				titles.add(key);
			}
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
		
		return titles;
	}
	
	/**
	 * Retrieve a list with all files in the given directory
	 * @param directoryTitle    The title of the directory where the file is located
	 * @return
	 * @throws Exception 
	 */
	@Override
	public Vector<String> getFileList(String directoryTitle) throws Exception {
		Vector<String> filenames = new Vector<String>();
		try {
			String directory = getDirectory(directoryTitle);
			
			// This filter only returns files (no directories)
			FileFilter fileFilter = new FileFilter() {
			    public boolean accept(File file) {
			    	return file.isFile();
			        
			    }
			};
			
			File dir = new File(directory);		
			File[] files = dir.listFiles(fileFilter);
			if (files == null) {
			    // Either dir does not exist or is not a directory
			} else {
			    for (int i=0; i < files.length; i++) {
			        String filename = files[i].getName();
			        filenames.add(filename);
			    }
			}
			
			// sort the filenames
			Collections.sort(filenames);
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}

		return filenames; 	
	}
	


	/**
	 * Create a new file 
	 * @param directoryTitle    The title of the directory where the file is located
	 * @param filename    	 	The name of the configuration file
	 * @param content     		The initial contents of the new file
	 * @return message     		A confirmation message
	 * @throws IOException 
	 */	
	@Override
	public String newFile(String directoryTitle, String filename, String content) 
			throws Exception {	
		try {
			String directory = getDirectory(directoryTitle);
			
			String safeFilename = stripPath(filename);
	
			// create a new file if the file does not exist
			File file = new File(directory + "/" + safeFilename);
			if(!file.exists()) {
				// create a new file
				file.createNewFile();
	
				setFile(directoryTitle, safeFilename, content);
				
				return "File " + filename + " created";
			}
			else {
				// file already exists.
				throw new Exception("File " + filename + " already exists");
			}
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
	}	
	
	
	/**
	 * Retrieve the file contents of a configuration file
	 * @param directoryTitle    The title of the directory where the file is located
	 * @param filename    	 	The name of the configuration file
	 * @return content     		The contents of the configuration file
	 * @throws IOException 
	 */	
	@Override
	public String getFile(String directoryTitle, String filename) 
		throws Exception {

		String content = "";
		try {
			String directory = getDirectory(directoryTitle);
			
			String safeFilename = stripPath(filename);
			
			FileInputStream fis = 
				new FileInputStream(directory + "/" + safeFilename);
			
			int c = -1;
		    while ((c = fis.read()) != -1) {
		        content += (char)c;
		    }
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}

		return content;
	}	
	
	/**
	 * Save the contents of a configuration file
	 * Warning: Existing file will be overwritten
	 * @param directoryTitle    The title of the directory where the file is located
	 * @param filename    		The name of the configuration file
	 * @param content     		The contents for the configuration file
	 * @return message    		A confirmation message when the file is saved
	 */
	@Override
	public String setFile(String directoryTitle, String filename, String content) 
		throws Exception {
		
		try {
			String directory = getDirectory(directoryTitle);
			String safeFilename = stripPath(filename);
	
			FileOutputStream fos = 
				new FileOutputStream(directory + "/" + safeFilename);
	
			for (int i = 0; i < content.length(); i++) {
				fos.write(content.charAt(i));
			}
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}

		return "File '" + filename + "' saved"; 
	}
		
	/**
	 * Rename a file
	 * @param directoryTitle    The title of the directory where the file is located
	 * @param oldFilename    	The old name of the configuration file
	 * @param newFilename    	The new name of the configuration file
	 * @return message    		A confirmation message when the file is deleted
	 */
	@Override
	public String renameFile(String directoryTitle, String oldFilename, String newFilename) 
			throws Exception {
		try {
			String directory = getDirectory(directoryTitle);
	
			String safeOldFilename = stripPath(oldFilename);
			String safeNewFilename = stripPath(newFilename);
	
			File file = new File(directory + "/" + safeOldFilename);
			if(file.exists()) {
				file.renameTo(new File(directory + "/" + safeNewFilename));
				
				return "File renamed to " + safeNewFilename;
			}
			else {
				throw new Exception("File " + safeOldFilename + " not found");
			}		
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}
	}
			
	
	/**
	 * Delete a file
	 * @param directoryTitle    The title of the directory where the file is located
	 * @param filename    		The name of the configuration file
	 * @return message    		A confirmation message when the file is deleted
	 */
	@Override
	public String deleteFile(String directoryTitle, String filename) 
			throws Exception {
		try {
			String directory = getDirectory(directoryTitle);
			String safeFilename = stripPath(filename);
	
		    File f = new File(directory + "/" + safeFilename);
	
		    // Make sure the file exists and isn't write protected
		    if (!f.exists())
		      throw new Exception("Delete: no such file or directory: " + filename);
	
		    if (!f.canWrite())
		      throw new Exception("Delete: write protected: " + filename);
		    
		    // If it is a directory, make sure it is empty
		    if (f.isDirectory())
		        throw new Exception("Delete: Cannot delete a directory");
	
		    // Attempt to delete it
		    boolean success = f.delete();
			
		    if (!success)
		        throw new Exception("Delete: deletion failed");
		}
		catch (Exception err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			throw new Exception(err);
		}

		return "File '" + filename + "' deleted"; 
	}
	
	/**
	 * Strip any path information from the given filename
	 */
	private String stripPath(String filename) {
		String safeFilename = filename;

		int pos = safeFilename.lastIndexOf("/");
		if (pos >= 0) {
			safeFilename = safeFilename.substring(pos);
		}

		pos = safeFilename.lastIndexOf("\\");
		if (pos >= 0) {
			safeFilename = safeFilename.substring(pos);
		}

		return safeFilename;
	}	
	
	@Override
	public ConfigurationData getConfigurationData() throws Exception {
		
		try {
			if (conf.getUrlDB() == null) 
				loadConfigurationProperties();
			
			return conf;
		}
		catch (Throwable err) {
			// Re-throwing the error is necessary because GWT cannot 
			// serialize some type of Exceptions.
			err.printStackTrace();
			throw new Exception(err.getClass().getName() + ": " + err.getMessage());
		}			
	}
	
}
