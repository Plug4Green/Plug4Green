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
package org.f4g.com.pbs.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author agiesler
 */
public class ProxyRequestAlterJob extends ProxyRequest {
	
	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(ProxyRequestAlterJob.class.getName());
	
	private String execCmd;	
	private String execDir;	
	private static final long serialVersionUID = 1L;
	private String[] nodes;
	private int[] cores;
	public void setCores(int[] cores) {
		this.cores = cores;
	}

	private String jobId;
	
	public ProxyRequestAlterJob(String jobId, String[] nodes, int[] cores, String execCmd, String execDir){
		setExecCmd(execCmd);
		setExecDir(execDir);
		setId(jobId);
		setNodes(nodes);
		setCores(cores);		
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.Fzj.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponseAlterJob execute() throws IOException {
		ProxyResponseAlterJob proxyResponseAlterJob = new ProxyResponseAlterJob();
		proxyResponseAlterJob.setSuccess(true);
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		
		String nodes_param = "nodes=";
		for(int i=0;i<nodes.length;i++){
			nodes_param += nodes[i] + ":ppn=" + cores[i];
			log.debug("nodes_param: " + nodes_param);
			if(i+1<nodes.length){
				nodes_param += "+";
			}
		}
		
		processBuilder.command(execCmd, "-l", nodes_param, getId());
		//log.debug("Job alter command: " + execCmd +  " -l " + nodes_param + " " + getId());
		List<String> com_and_args = processBuilder.command();
		for(String s : com_and_args){
			log.debug("arg: " + s);
		}
		
		processBuilder.directory(new File(execDir));
		log.trace("Exec Dir " + execDir);
		Process process = null;
		try {
			process = processBuilder.start();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				log.error("Cant't get exit code for " + getExecCmd() + 
						". " + e.getMessage());
			}

			if(process.exitValue() !=0) {

				byte[] bytes = new byte[1024];
				InputStream errorStream = process.getErrorStream();
				errorStream.read(bytes);
				String error = new String(bytes) + "Exit code for " + execCmd + " is not zero";
				log.error(error);
				proxyResponseAlterJob.setSuccess(false);
				proxyResponseAlterJob.setMessage(error);
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
			proxyResponseAlterJob.setSuccess(false);
			proxyResponseAlterJob.setMessage(e1.getMessage());
		}
		finally {
			if (process != null) {
				close(process.getOutputStream());
				close(process.getInputStream());
				close(process.getErrorStream());
				process.destroy();
				log.debug("Closed all streams and destroyed process");
			}
		}
		
		return proxyResponseAlterJob;
	}
	
	private void close(Closeable c) {
	    if (c != null) {
	      try {
	        c.close();
	      } catch (IOException e) {
	    	  log.debug("Error when closing process stream");
	      }
	    }
	  }

	/**
	 * @param id_ the id_ to set
	 */
	public void setId(String id_) {
		this.jobId = id_;
	}

	/**
	 * @return the id_
	 */
	public String getId() {
		return jobId;
	}
	
	/**
	 * @return the cores
	 */
	public int[] getCores() {
		return cores;
	}

	/**
	 * @param cores the cores to set
	 */

	/**
	 * @param nodes_ the nodes_ to set
	 */
	public void setNodes(String[] nodes) {
		this.nodes = nodes;
	}

	/**
	 * @return the nodes_
	 */
	public String[] getNodes() {
		return nodes;
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
