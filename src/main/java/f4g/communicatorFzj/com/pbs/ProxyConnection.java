/**
* ============================== Header ============================== 
* file:          ProxyConnection.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-10-04 09:31:11 +0200 (Di, 04 Okt 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 850 $
* 
* short description:
*   SSL secured connection from the F4G plug-in to the Proxy
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;
import f4g.commons.com.pbs.common.ProxyRequest;
import f4g.commons.com.pbs.common.ProxyResponse;

/**
 * SSL secured connection from the F4G plug-in to the Proxy
 * The connection can be used as ObjectStream for ProxyRequest and ProxyResponse objects
 * 
 * @see ProxyRequest
 * @see ProxyResponse
 *
 * @author Daniel Brinkers
 */
public class ProxyConnection {
	static Logger log = Logger.getLogger(ProxyConnection.class.getName());
	
	private String host_;
	private int port_;
	private String keystore;
	private String pass;
	private Socket socket_ = null;
	private static SSLContext sslContext_ = null;
	
	/**
	 * Connect to a Proxy
	 * @param host host to connect to
	 * @param port port to cennect to
	 * @throws IOException
	 */
	public ProxyConnection(String host, int port, String cli_keystore, String cli_pass) throws IOException{
		setHost(host);
		setPort(port);
		setKeystore(cli_keystore);
		setPass(cli_pass);
		
		if(getSslContext() == null){
			try {
				char ksp[] = getPass().toCharArray();//"secret".toCharArray();
				KeyStore ks;
				ks = KeyStore.getInstance("JKS");	
				InputStream clientjks = 
					this.getClass().getClassLoader().getResourceAsStream(getKeystore());//"resources/ClientKeystore.jks");//zam1073client.jks");
				log.debug("Reading client keystore from: " + getKeystore());
				//ks.load(new FileInputStream("client.jks"), ksp);
				ks.load(clientjks, ksp);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(ks, ksp);
		
				char tmp[] = getPass().toCharArray();//"secret".toCharArray();
				ks = KeyStore.getInstance("JKS");
				InputStream trustjks = 
					//this.getClass().getClassLoader().getResourceAsStream("resources/client.jks");
					this.getClass().getClassLoader().getResourceAsStream(getKeystore());//zam1073truststore.jks");
				log.debug("Reading truststore from: " + getKeystore());
				//ks.load(new FileInputStream("servers.jks"), tmp);
				ks.load(trustjks, tmp);
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				tmf.init(ks);
			
				setSslContext(SSLContext.getInstance("SSLv3"));
				getSslContext().init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			} catch (Exception e) {
				e.printStackTrace();
				setSslContext(null);
				throw new IOException("Could not create SSL Context");
			}
		}
		create_Socket(host, port);
			//setSocket(getSslContext().getSocketFactory().createSocket(host, port));
	}
	
	synchronized private void create_Socket(String host, int port) throws IOException{
		setSocket(getSslContext().getSocketFactory().createSocket(host, port));
	}
	
	
	/**
	 * Gets the InputStream of the connection.
	 * @return the InputStream of the connection.
	 * @throws IOException 
	 */
	public InputStream getInputStream() throws IOException{
		return getSocket().getInputStream();
	}

	/**
	 * Gets the OutputStream of the connection.
	 * @return the OutputStream of the connection.
	 * @throws IOException 
	 */
	public OutputStream getOutputStream() throws IOException{
		return getSocket().getOutputStream();
	}

	/**
	 * Closes the connection.
	 * @throws IOException
	 */
	public void close() throws IOException{
		if(getSocket() != null){
			getSocket().close();
			setSocket(null);
		}
	}

	/**
	 * @param host_ the host_ to set
	 */
	public void setHost(String host_) {
		this.host_ = host_;
	}
	/**
	 * @return the host_
	 */
	public String getHost() {
		return host_;
	}
	/**
	 * @param port_ the port_ to set
	 */
	public void setPort(int port_) {
		this.port_ = port_;
	}
	/**
	 * @return the port_
	 */
	public int getPort() {
		return port_;
	}
	
	
	/**
	 * @return the keystore
	 */
	public String getKeystore() {
		return keystore;
	}
	/**
	 * @param keystore the keystore to set
	 */
	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}
	/**
	 * @return the pass
	 */
	public String getPass() {
		return pass;
	}
	/**
	 * @param pass the pass to set
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}
	/**
	 * @param socket_ the socket_ to set
	 */
	public void setSocket(Socket socket_) {
		this.socket_ = socket_;
	}
	/**
	 * @return the socket_
	 */
	public Socket getSocket() {
		return socket_;
	}
	/**
	 * @param sslContext the sslContext to set
	 */
	public static void setSslContext(SSLContext sslContext) {
		ProxyConnection.sslContext_ = sslContext;
	}
	/**
	 * @return the sslContext
	 */
	public static SSLContext getSslContext() {
		return sslContext_;
	}

}
