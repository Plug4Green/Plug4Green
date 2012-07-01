/**
* ============================== Header ============================== 
* file:          PbsConnection.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1244 $
* 
* short description:
*   Handles the connaction and communication to a PBS compatible RM
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;



/**
 * Handles the connection and communication to a PBS compatible RM
 *
 * @author Daniel Brinkers
 */
public class PbsConnection {
	
	static Logger log = Logger.getLogger(PbsConnection.class.getName());
	
	private String host_;
	private Socket socket_;
	OutputStream outputStream;
	InputStream inputStream;

	/**
	 * @param host Hostname of the computer running the PBS compatible resource manager
	 * @throws IOException 
	 */
	public PbsConnection(String host) throws IOException {
		connect(host);
	}

	/**
	 * Connect to a PBS RM
	 * 
	 * @param host Hostname of the computer running the PBS compatible resource manager
	 *
	 * @author Daniel Brinkers
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	void connect(String host) throws UnknownHostException, IOException {
		setHost(host);
		setSocket(new Socket(getHost(), 15001));
		auth(getSocket().getLocalPort());
	}
	
	/**
	 * Destructor
	 * 
	 * @throws IOException
	 */
	protected void finalize() throws IOException{
		close();
	}

	/**
	 * Close the connection
	 * 
	 * @throws IOException 
	 */
	public void close() throws IOException {
		if(this.inputStream != null)
			inputStream.close();
		if(this.outputStream != null)
			outputStream.close();	
		if(getSocket() != null)
			getSocket().close();
		setSocket(null);
	}

	/**
	 * Authenticate the connection at the RM as user root
	 * 
	 * @param port the port of the outgoing connection, which should be authorized as root to the RMS
	 * 
	 * @throws IOException 
	 */
	private void auth(int port) throws IOException {
		Socket authSocket = new Socket();
		authSocket.setReuseAddress(true);
		authSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 1));
		authSocket.connect(new InetSocketAddress(InetAddress.getByName(getHost()), 15001));
		DisRequest disRequest = DisRequestAuth.make(port);
		send(authSocket, disRequest);
		//response parse?
		if(authSocket != null && !authSocket.isClosed()){
			authSocket.getInputStream().close();			
			//authSocket.getOutputStream().close();
			authSocket.close();
		}		
		authSocket = null;
	}

	/**
	 * Send a DIS encoded request through the connection
	 * 
	 * @param DIS encoded request
	 * @return DIS encoded response
	 * 
	 * @throws IOException 
	 */
	public DisResponse send(DisRequest disRequest) throws IOException {
		return send(socket_, disRequest);
	}

	/**
	 * Sends a DIS encoded request through a Socket
	 * 
	 * @param authSocket the Socket to use
	 * @param disRequest the request to send
	 * @return The response of the RMS
	 *
	 * @throws IOException 
	 */
	private DisResponse send(Socket authSocket, DisRequest disRequest) throws IOException {
		outputStream = authSocket.getOutputStream();
		inputStream = authSocket.getInputStream();
		outputStream.write(disRequest.getRequest().getBytes());
		outputStream.flush();
		return DisResponse.read(inputStream);
	}

	/**
	 * @param host the host to set
	 */
	private void setHost(String host) {
		this.host_ = host;
	}

	/**
	 * @return the host
	 */
	private String getHost() {
		return host_;
	}

	/**
	 * @param socket the socket to set
	 */
	private void setSocket(Socket socket) {
		this.socket_ = socket;
	}

	/**
	 * @return the socket
	 */
	private Socket getSocket() {
		return socket_;
	}

}
