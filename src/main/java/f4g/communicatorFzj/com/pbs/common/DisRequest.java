/**
* ============================== Header ============================== 
* file:          DisRequest.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   DIS encoding class
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;



/**
 * With this class a DIS encoded request for a PBS compatible RM system can be 
 * created. It can be sent trough a PbsConnection.
 * 
 * The DIS protocol basically defines how integer and strings are encoded.
 * This classes not only consider the DIS encode but also the message structure 
 * for the PBS protocol. 
 * 
 * Further information about the DIS protocol can be found in the PBS 
 * documentation (e.g. <a href="http://www.fhi-berlin.mpg.de/th/locserv/
 * software_all/PBS/full/ERS-194.html">here</a>)
 * In this document also information about the messages are available.
 *  
 * @see PbsConnection
 * @see DisResponse
 *
 * @author Daniel Brinkers
 */
public class DisRequest {
	private String request_;
	private boolean extension_ = false;
	private boolean body_ = false;
	private boolean header_ = false;
	
	public enum RequestType{
		CONNECT,
		QUEUEJOB,
		JOBCREDENTIAL,
		JOBSCRIPT,
		RDYTOCOMMIT,
		COMMIT,
		DELETEJOB,
		HOLDJOB,
		MANAGER,
		MESSAGEJOB,
		MODIFYJOB,
		MOVEJOB,
		RELEASEJOB,
		RERUNJOB,
		RUNJOB,
		SELECTJOBS,
		SERVERSHUTDOWN,
		SIGNALJOB,
		STATUSJOB,
		STATUSQUEUE,
		STATUSSERVER,
		TRACKJOB,
		AUTHENUSER,
		ORDERJOB,
		SELSTAT,
		REGISTERDEPENDENT,
		COPYFILES,
		DELFILES,
		JOBOBIT,
		MVJOBFILE,
		STATUSNODE,
	}
	
	/**
	 * 
	 * Standard constructor creates a emty request.
	 *
	 * @author Daniel Brinkers
	 */
	DisRequest(){
		setRequest("");
	}
	
	/**
	 * Get the DIS encoded request as String (completes the request with a 
	 * empty extension, if not already present)
	 * 
	 * @see toString
	 *
	 * @author Daniel Brinkers
	 * @return the request as String
	 */
	String get(){
		if(!hasExtension())
			addExtension();
		return getRequest();
	}
	
	/**
	 * Get DIS encoded request in the current state (the request is not checked, or completed).
	 * 
	 * @see get() 
	 *
	 * @author Daniel Brinkers
	 * @return the request as String
	 */
	@Override
	public String toString(){
		return getRequest();
	}

	/**
	 *
	 * Creates the header of a request
	 * 
	 * @param requestType the type of the request to be created
	 *
	 * @author Daniel Brinkers
	 */
	protected void addHeader(RequestType requestType) {
		setHasHeader(true);
		//type
		addInt(2);
		//version
		addInt(1);
		switch (requestType){
		case RUNJOB:
			addInt(15);
			break;
		case STATUSJOB:
			addInt(19);
			break;
		case STATUSQUEUE:
			addInt(20);
			break;
		case STATUSSERVER:
			addInt(21);
			break;
		case AUTHENUSER:
			addInt(49);
			break;
		case STATUSNODE:
			addInt(58);
			break;
		default:
			break;
		}
		addString("root");
	}

	/**
	 * Adds a string value to the request
	 * 
	 * @param string the string to be added
	 *
	 * @author Daniel Brinkers
	 */
	void addString(String string) {
		//A string is encoded with just a integer with the size of the string 
		//followed by the string itself
		addInt(string.length());
		attendRequest(string);
	}


	/**
	 * Adds an integer value to the request
	 * 
	 * @param i the integer to be added
	 *
	 * @author Daniel Brinkers
	 */
	void addInt(int i) {
		String sign = "";
		/*
		 * A single digit is encoded with just its sign in front of the digit
		 */
		if(i >=0 && i < 10){
			attendRequest("+" + new Integer(i).toString());
		}else if(i<0 && i > -10){
			attendRequest("-" + new Integer(-i).toString()); 
		}else{
			/*
			 * save sign
			 */
			if(i < 0){
				sign = "-";
				i *= -1;
			}else{
				sign = "+";
			}
			/*
			 * multidigit integers are represented by the number of digits 
			 * followed by the sign and the absolute value (in ascii) 
			 */
			int l, ll;
			l =  (new Integer(i)).toString().length();
			/*
			 * less then 10 digits => just one digit for the number of digits
			 */
			if(l < 10){
				attendRequest(new Integer(l).toString() + sign + new Integer(i).toString());
			/*
			 * with more digits the number of digits itself must be encoded by number of digits followed by the number
			 */
			}else{
				ll = (new Integer(l)).toString().length();
				attendRequest(new Integer(ll).toString() + new Integer(l).toString() + sign + new Integer(i).toString());
			}
		}

	}

	/**
	 * Adds an (empty) extension to the request (mandatory for protocol)
	 *
	 * @author Daniel Brinkers
	 */
	protected void addExtension() {
		addInt(0);
		setHasExtension(true);
	}

	/**
	 * @param request the request to set (internal function)
	 */
	void setRequest(String request) {
		this.request_ = request;
	}

	/**
	 * @return the request (internal function)
	 */
	String getRequest() {
		return request_;
	}
	/**
	 * Extends the request string (internal function)
	 * 
	 * @param string
	 *
	 * @author Daniel Brinkers
	 */
	void attendRequest(String string) {
		setRequest(getRequest() + string);
	}
	/**
	 * @param extension if a extension is already added to the request
	 */
	void setHasExtension(boolean extension) {
		this.extension_ = extension;
	}

	/**
	 * @return if a extension is already added to the request
	 */
	boolean hasExtension() {
		return extension_;
	}

	/**
	 * @param body set if a body is already added
	 */
	void setHasBody(boolean body) {
		this.body_ = body;
	}

	/**
	 * @return if a body is already added
	 */
	boolean hasBody() {
		return body_;
	}

	/**
	 * @param header set if a header is already added
	 */
	public void setHasHeader(boolean header) {
		this.header_ = header;
	}

	/**
	 * @return if a header is already added
	 */
	public boolean hasHeader() {
		return header_;
	}
}