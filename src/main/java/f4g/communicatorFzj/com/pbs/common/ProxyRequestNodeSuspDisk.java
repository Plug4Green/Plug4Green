/**
* ============================== Header ============================== 
* file:          ProxyRequestStartJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       Dec 8, 2010 by brinkers
* 
* $LastChangedDate: 2012-02-21 19:21:17 +0100 (Tue, 21 Feb 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1167 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Executes a shell command to set a node in deep sleep modus
 * 
 *
 * @author Andre Giesler
 */
public class ProxyRequestNodeSuspDisk extends ProxyRequest {
	
	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(ProxyRequestNodeSuspDisk.class.getName());
	
	private String execCmd;	
	private String execDir;	
	private static final long serialVersionUID = 1L;
	private String node;
	
	ProxyRequestNodeSuspDisk(String node, String execCmd, String execDir){
		setExecCmd(execCmd);
		setExecDir(execDir);
		setNode(node);
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.Fzj.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponse execute() throws IOException {
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		List<String> com_and_args = processBuilder.command();
		for(String s : com_and_args){
			log.debug("arg: " + s);
		}	
		processBuilder.command(execCmd,  "-node", getNode());
		processBuilder.directory(new File(execDir));

		Process process;
		try {
			process = processBuilder.start();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				log.error("Cant't get exit code for " + execCmd + 
						" connect" + ". " + e.getMessage());
			}

			if(process.exitValue() !=0) {

				byte[] bytes = new byte[1024];
				InputStream errorStream = process.getErrorStream();
				errorStream.read(bytes);
				String error = new String(bytes);

				log.error(error + "Exit code for " + execCmd + " is not zero");
			}
			else
			{
				byte[] outBytes = new byte[1024];
				InputStream inputStream = process.getInputStream();
				inputStream.read(outBytes);

				//String output = new String(outBytes);
				//log.info(output);				
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			log.info(e1.getMessage());
		}

		return null;
	}


	/**
	 * @param nodes_ the nodes_ to set
	 */
	public void setNode(String node_) {
		this.node = node_;
	}

	/**
	 * @return the nodes_
	 */
	public String getNode() {
		return node;
	}
	
	/**
	 * @return the execCmd
	 */
	public String getExecCmd() {
		return execCmd;
	}

	/**
	 * @param execCmd the execCmd to set
	 */
	public void setExecCmd(String execCmd) {
		this.execCmd = execCmd;
	}
	
	/**
	 * @return the execDir
	 */
	public String getExecDir() {
		return execDir;
	}

	/**
	 * @param execDir the execDir to set
	 */
	public void setExecDir(String execDir) {
		this.execDir = execDir;
	}
}
