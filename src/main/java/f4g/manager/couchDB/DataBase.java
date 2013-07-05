/**
* ============================== Header ============================== 
* file:          DataBase.java
* project:       FIT4Green/Manager
* created:       16 sep. 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-10-13 16:56:03 +0200 (jue, 13 oct 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 888 $
* 
* short description:
*   Utility class to manage databases on couchDB server
* ============================= /Header ==============================
*/
package org.f4g.couchDB;

import java.net.HttpURLConnection;

import org.f4g.couchDB.HTTPRequest;
import org.f4g.couchDB.Properties;

/**
 * Utility class to manage databases on couchDB server
 *
 * @author Vasiliki Georgiadou
 */
public class DataBase {
	
	private String 	url;
	
	private Properties properties;
	
	private String message;
	
	
	public DataBase () {
		this.url = null;
		this.message = null;
		this.properties = new Properties();
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public String getMessage(){
		return message;
	}
	
	/**
	 * Create database
	 * 
	 * @param 	name	The database name
	 * @return			0 if url is not specified, otherwise the HTTP request return code:
	 * 					201, database created;
	 * 					412, database already exists;
	 * 					see HTTP request for more 
	 */
	public int create(String name) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest();
		String url = this.url + "/" + name + "/";
		
		try {
			String response = req.sendPutRequest(url, null);
			this.message = "Response to HTTP PUT request: " + response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return req.getReturnCode();
	}
	
	/**
	 * Get database information and set its properties
	 *  
	 * @param 	name 	The database name
	 * @return			0 if url is not specified, otherwise the HTTP request return code:
	 * 					200, HTTP OK (properties are set);
	 * 					see HTTP request for more 
	 */
	public int retrieveProperties(String name) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest();
		String url = this.url + "/" + name + "/";
		
		try {
			String response = req.sendGetRequest(url, null);
			this.message = "Response to HTTP GET request: " + response;
			if (req.getReturnCode() == HttpURLConnection.HTTP_OK) {
				properties.retrieveProperties(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return req.getReturnCode();
	}
	
	/**
	 * Run compaction of database
	 * 
	 * @param 	name	The database name
	 * @return			0 if url is not specified, otherwise the HTTP request return code:
	 * 					202, on success; 
	 * 					see HTTP request for more 
	 */	
	public int compact (String name) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest("application/json");
		String url = this.url + "/" + name + "/_compact";

		try {
			String response = req.sendPostRequest(url, null);
			this.message = "Response to HTTP DELETE request: " + response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return req.getReturnCode();		
		
	}
	
	/**
	 * Delete database
	 * 
	 * @param 	name	The database name
	 * @return			0 if url is not specified, otherwise the HTTP request return code:
	 * 					200, database deleted;
	 * 					404, database does not exist;
	 * 					see HTTP request for more 
	 */	
	public int delete(String name) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest();
		String url = this.url + "/" + name + "/";

		try {
			String response = req.sendDeleteRequest(url, null);
			this.message = "Response to HTTP DELETE request: " + response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return req.getReturnCode();		
	}
	
	/**
	 * Create new document in database. It can also be used to create a design document.
	 * 	
	 * @param 	name	The database name
	 * @param 	id		The document ID; for design documents this must be "_design/[unique_id]"
	 * @param 	data	The document data
	 * @return 			0 if url is not specified, otherwise the HTTP request return code:
	 * 					201, document created;
	 * 					see HTTP request for more 
	 */
	public int createDocument(String name, String id, String data) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest();
		String url = this.url + "/" + name + "/" + id;
		try {
			String response = req.sendPutRequest(url, data);
			this.message = "Response to HTTP PUT request: " + response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return req.getReturnCode();
	}
	
	/**
	 * Delete document in database.
	 * 	
	 * @param 	name	The database name
	 * @param 	id		The document ID
	 * @param 	rev		The document revision
	 * @return 			0 if url is not specified, otherwise the HTTP request return code:
	 * 					200, document deleted;
	 * 					otherwise, see HTTP request for more 
	 */
	public int deleteDocument(String name, String id, String rev) {
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
			return 0;
		}
		
		HTTPRequest req = new HTTPRequest();
		String url = this.url + "/" + name + "/" + id;
		String data = "rev=" + rev;
		
		try {
			String response = req.sendDeleteRequest(url, data);
			this.message = "Response to HTTP DELETE request: " + response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return req.getReturnCode();
	}
	
	/**
	 * Query documents in a database using an existing view.
	 * 	
	 * @param 	name	The database name
	 * @param 	id		The design document ID
	 * @param	view	The view name
	 * @param	data	The query parameters, for example "param1=val1&param2=val2"
	 * @return 			null if url is not specified, otherwise the HTTP request response 
	 */
	public String query(String name, String id, String view, String data) {
		
		String response = null;
		
		if (this.url == null) {
			this.message = "No url specified; no request sent";
		} else {
			HTTPRequest req = new HTTPRequest();
			String url = this.url + "/" + name + "/_design/" + id + "/_view/" + view;
			try {
				response = req.sendGetRequest(url, data);
				this.message = "Response to HTTP PUT request: " + response;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return response;
	}
	
}