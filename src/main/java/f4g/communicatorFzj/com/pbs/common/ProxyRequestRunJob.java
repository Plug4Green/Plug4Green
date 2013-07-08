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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Executes a shell command to run a job in the queue
 * 
 *
 * @author Andre Giesler
 */
public class ProxyRequestRunJob extends ProxyRequest {
	
	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(ProxyRequestRunJob.class.getName());
	
	private String execCmd;	
	private String execDir;	
	private static final long serialVersionUID = 1L;
	private String jobId;
	
	public ProxyRequestRunJob(String jobId, String execCmd, String execDir){
		setExecCmd(execCmd);
		setExecDir(execDir);
		setId(jobId);
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.Fzj.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponseStartJob execute() throws IOException {
		ProxyResponseStartJob proxyResponseStartJob = new ProxyResponseStartJob();
		proxyResponseStartJob.setSuccess(true);
		
		boolean success = true;
		String message = "";
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		
		processBuilder.command(execCmd, getId());
		log.trace("Job run command: " + execCmd +  " " + getId());
		List<String> com_and_args = processBuilder.command();
		for(String s : com_and_args){
			log.debug("arg: " + s);
		}
		processBuilder.directory(new File(execDir));

		Process process = null;
		try {
			process = processBuilder.start();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				log.error("Cant't get exit code for " + execCmd + 
						". " + e.getMessage());
			}

			if(process.exitValue() !=0) {

				byte[] bytes = new byte[1024];
				InputStream errorStream = process.getErrorStream();
				errorStream.read(bytes);
				String error = new String(bytes) + " Exit code for " + execCmd + " is not zero";
				log.error(error);				
				success = false;
				message = error;
			}
			else
			{
				InputStream inputStream = process.getInputStream();
				InputStreamReader tempReader = new InputStreamReader(
						new BufferedInputStream(inputStream));
				BufferedReader reader = new BufferedReader(tempReader);
				StringBuffer buf = new StringBuffer();
				while (true){
					String line = reader.readLine();
					buf.append(line);
					if (line == null)
						break;
					log.debug(line);
				}
				message = buf.toString();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			log.info(e1.getMessage());
			success = false;
			message = e1.getMessage();
		}
		finally {
			if (process != null) {
				close(process.getOutputStream());
				close(process.getInputStream());
				close(process.getErrorStream());
				process.destroy();
				log.debug("Closed all streams and destroyed process");
				proxyResponseStartJob.setSuccess(success);
				proxyResponseStartJob.setMessage(message);
			}
		}
		
		return proxyResponseStartJob;
		
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
