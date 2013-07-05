/**
* ============================== Header ============================== 
* file:          HTTPRequest.java
* project:       FIT4Green/Manager
* created:       13 sep. 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-11-09 12:42:17 +0100 (mié, 09 nov 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 1085 $
* 
* short description:
*   Utility class for HTTP requests
* ============================= /Header ==============================
*/
package org.f4g.couchDB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for HTTP requests
 * 
 * @author Vasiliki Georgiadou
 */
public class HTTPRequest { 
	
	private final int TIME_OUT = 10000;		// max is 10 sec
	
	private int returnCode;
	private String contentType;

	public HTTPRequest() {
		this.returnCode = 0;
		this.contentType = null;
	}
	
	public HTTPRequest(String contentType) {
		this.returnCode = 0;
		this.contentType  = contentType;
	}

	public int getReturnCode() {
		return returnCode;
	}

	/**
	* Sends an HTTP GET request to a url. 
	* Content-Type may be specified by means of the constructor.
	*
	* @param url 		The url
	* @param data 		(optional) The parameters to the request, for example 
	* 					"param1=val1&param2=val2"; "?" added internally
	* @return 		 	The response string
	* @throws Exception
	*/
	public String sendGetRequest(String url, String data) throws Exception {	
		String response = null;
		try {
			if (data != null){
				url += "?" + data;
			}
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setReadTimeout(TIME_OUT);
			conn.setConnectTimeout(TIME_OUT);
			if (this.contentType != null) {
		    	conn.setRequestProperty("Content-Type", this.contentType);
		    }
			this.returnCode = conn.getResponseCode();
			if (this.returnCode == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				response = streamToString(is);
				is.close();
			} else {
				response = "return code: " + this.returnCode;
			}
		}
		catch( IOException e ){
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	* Sends an HTTP PUT request to a url. 
	* Content-Type may be specified by means of the constructor.
	*
	* @param url 		The url
	* @param data 		The body of the request
	* @return 		 	The response string
	* @throws Exception
	*/
	public String sendPutRequest(String url, String data) throws Exception {
		String response = null;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setReadTimeout(TIME_OUT);
			conn.setConnectTimeout(TIME_OUT);
			conn.setRequestMethod("PUT");
			if (this.contentType != null) {
		    	conn.setRequestProperty("Content-Type", this.contentType);
		    }
			if (data != null) {
			OutputStreamWriter os = new OutputStreamWriter (conn.getOutputStream());
				os.write(data);
				os.flush();
				os.close();
			}
			this.returnCode = conn.getResponseCode();
			if (this.returnCode == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				response = streamToString(is);
				is.close();
			} else {
				response = "return code: " + this.returnCode;
			}
		}
		catch( IOException e ){
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	* Sends an HTTP DELETE request to a url. 
	* Overriding Content-Type with "application/x-www-form-urlencoded".
	*
	* @param url 		The url
	* @param data 		(optional) The parameters to the request, for example 
	* 					"param1=val1&param2=val2"; "?" added internally
	* @return 		 	The response string
	* @throws Exception
	*/
	public String sendDeleteRequest(String url, String data) throws Exception {
		String response = null;
		try {
			if (data != null){
				url += "?" + data;
			}
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setReadTimeout(TIME_OUT);
			conn.setConnectTimeout(TIME_OUT);
			//  Overriding content type!!!
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded" );
			conn.setRequestMethod("DELETE");
			conn.connect();
			this.returnCode= conn.getResponseCode();
			if (this.returnCode == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				response = streamToString(is);
				is.close();
			} else {
				response = "return code: " + this.returnCode;
			}
		}
		catch( IOException e ){
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	* Sends an HTTP POST request to a url.
	* Content-Type may be specified by means of the constructor.
	*
	* @param url 		The url
	* @param data 		(optional) The body of the request
	* @return 		 	The response string
	* @throws Exception
	*/
	public String sendPostRequest(String url, String data) throws Exception{
		String response = null;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setReadTimeout(TIME_OUT);
			conn.setConnectTimeout(TIME_OUT);
			if (this.contentType != null) {
		    	conn.setRequestProperty("Content-Type", this.contentType);
		    }
			conn.setRequestMethod("POST");
			if (data != null ){
				OutputStream os = conn.getOutputStream();
				os.write( data.getBytes() );
				os.flush();
				os.close();
			}
			this.returnCode = conn.getResponseCode();
			if (this.returnCode == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				response = streamToString(is);
				is.close();
			} else {
				response = "return code: " + this.returnCode;
			}
		}
		catch( IOException e ){
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	 * Converts a stream to a string; UTF8 encoding
	 * 
	 * @param in		The InputStream
	 * @return			The String
	 * @throws Exception
	 */
	private String streamToString(InputStream in) throws Exception {
		
		InputStreamReader inreader = new InputStreamReader(in, "UTF8");
		BufferedReader reader = new BufferedReader(inreader);
		StringBuilder out = new StringBuilder();
		String line = null;
		
		while ((line = reader.readLine()) != null) {
			out.append(line + "\n");
		}
		
		return out.toString();
	}

}