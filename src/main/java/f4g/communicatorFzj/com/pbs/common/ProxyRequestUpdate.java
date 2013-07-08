/**
 * ============================== Header ============================== 
 * file:          ProxyRequestUpdate.java
 * project:       FIT4Green/CommunicatorFzj
 * created:       Nov 25, 2010 by Daniel Brinkers
 * 
 * $LastChangedDate: 2011-12-01 20:56:31 +0100 (Do, 01 Dez 2011) $ 
 * $LastChangedBy: f4g.julichde $
 * $LastChangedRevision: 1146 $
 * 
 * short description:
 *   Response for an update request
 * ============================= /Header ==============================
 */
package f4g.communicatorFzj.com.pbs.common;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * 
 * Request for an Update
 * 
 * @see ProxyResponseUpdate
 *
 * @author Daniel Brinkers
 */
public class ProxyRequestUpdate extends ProxyRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static double[] readIos;
	private static double[] writeIos;
	
	private static NodeInfo[] tmp_nodes_worker;

	static Logger log = Logger.getLogger(ProxyRequestUpdate.class.getName());

	private boolean system_mon;
	private String execPowerCons;
	private String[] execCPUUsage;
	private String[] execCoreFreqs;
	private String[] execCoreVoltage;
	private String[] execMemUsage;
	private String[] execFANUsage;
	private String[] execIOSTATS;
	private String execDir;
	private String ipmi_core_voltage_id;
	private String mpstat_idle_col;

	public ProxyRequestUpdate(String execDir, String execPowerCons, 
			String[] execCPUUsage, String[] execCoreFreqs, String[] execCoreVoltage,
			String[] execMemUsage, String[] execFANUsage, String[] execIOSTATS,
			boolean system_mon) {
		super();
		this.execDir = execDir;
		this.execPowerCons = execPowerCons;
		this.execCPUUsage = execCPUUsage;
		this.execCoreFreqs = execCoreFreqs;
		this.execCoreVoltage = execCoreVoltage;
		this.execMemUsage = execMemUsage;
		this.execFANUsage = execFANUsage;
		this.execIOSTATS = execIOSTATS;
		this.system_mon = system_mon;
	}

	/**
	 * Issue the request to the RM 
	 * @see org.f4g.com.fzj.pbs.common.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponse execute() throws IOException {
		NodeInfo[] nodeInfo;
		JobInfo[] jobInfo;
		PbsConnection pbsConnection;
		DisRequest disRequest;
		DisResponse disResponse;
		DisResponseStatus disResponseStatus = null;

		pbsConnection = new PbsConnection(InetAddress.getLocalHost().getHostAddress());
		disRequest = DisRequestStatusNode.make();
		disResponse = pbsConnection.send(disRequest);
		pbsConnection.close();
		
		int number_of_nodes = 0;

		if(!(disResponse instanceof DisResponseStatus)){
			nodeInfo = null;
		}else{
			disResponseStatus = (DisResponseStatus) disResponse;
			nodeInfo = new NodeInfo[disResponseStatus.getStati().length];
			for(int i=0; i<disResponseStatus.getStati().length; ++i){
				nodeInfo[i] = new NodeInfo(disResponseStatus.getStati()[i]);
			}
		}
		
		number_of_nodes = disResponseStatus.getStati().length;
			
		tmp_nodes_worker = new NodeInfo[number_of_nodes];
		for(int i=0; i<disResponseStatus.getStati().length; ++i){
			tmp_nodes_worker[i] = new NodeInfo(disResponseStatus.getStati()[i]);
		}
		
		ProxyRequestUpdate.readIos = new double[number_of_nodes];
		ProxyRequestUpdate.writeIos = new double[number_of_nodes];

		//Getting the power consumption
		ProcessBuilder processBuilder = new ProcessBuilder();

		processBuilder.command(execPowerCons);
		List<String> com_and_args = processBuilder.command();
		for(String s : com_and_args){
			log.trace("com_and_args: " + s);
		}
		processBuilder.directory(new File(execDir));

		Process process;

		String output = "";
		try {
			process = processBuilder.start();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				log.error("Cant't get exit code for " + execPowerCons + 
						". " + e.getMessage());
			}

			if(process.exitValue() !=0) {

				byte[] bytes = new byte[1024];
				InputStream errorStream = process.getErrorStream();
				errorStream.read(bytes);
				String error = new String(bytes);

				log.error(error + "Exit code for " + execPowerCons + " is not zero");
			}
			else
			{
				byte[] outBytes = new byte[1024];
				InputStream inputStream = process.getInputStream();
				inputStream.read(outBytes);

				output = new String(outBytes);
				output = output.trim();
				log.info("Node's power consumption:" + output);				
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			log.info(e1.getMessage());
		}

		if(system_mon){
			//Getting the cpu usage	
			ThreadGroup group = new ThreadGroup("cpuUsage");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){				
					processBuilder = new ProcessBuilder();			
					String[] commandArgs = new String[2+this.execCPUUsage.length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execCPUUsage){
						commandArgs[j] = s;
						j++;
					}
					processBuilder.command(commandArgs);	
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting cpu usage Worker Node"+nodeInfo[i].getId());
					CPUusageWorker procWorker = new CPUusageWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
			}

			Thread[] threadArray = new Thread[ group.activeCount() ];
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering cpu usage Workers");
				for(int i=0;i<nodeInfo.length;i++){
					if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)){
						nodeInfo[i].setCoreUsages(ProxyRequestUpdate.tmp_nodes_worker[i].getCoreUsages());
					}
					else{
						nodeInfo[i].setCpuUsage(0.0);
						nodeInfo[i].setCoreUsages(new double[nodeInfo[i].getnCores()]);
						for(int j=0;j<nodeInfo[i].getnCores();j++){			            
							double usage_val = 0.0;  
							nodeInfo[i].getCoreUsages()[j] = usage_val;			            
						}	
					}
				}  
			}

			//Getting frequency of cores
			group = new ThreadGroup("coreFreq");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){				
					processBuilder = new ProcessBuilder();			
					String[] commandArgs = new String[2+this.execCoreFreqs.length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execCoreFreqs){
						commandArgs[j] = s;
						j++;
					}
					processBuilder.command(commandArgs);	
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting core frequency Worker Node"+nodeInfo[i].getId());
					CoreFreqWorker procWorker = new CoreFreqWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
			}

			threadArray = new Thread[ group.activeCount() ];
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering core frequency Workers");
				for(int i=0;i<nodeInfo.length;i++){
					if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){
						nodeInfo[i].setCoreFrequencies(ProxyRequestUpdate.tmp_nodes_worker[i].getCoreFrequencies());
					}
					else{
						nodeInfo[i].setCoreFrequencies(new double[nodeInfo[i].getnCores()]);
						for(int j=0;j<nodeInfo[i].getnCores();j++){			            
							double freq_val = 2400.0;  
							nodeInfo[i].getCoreFrequencies()[j] = freq_val;			            
						}	
					}
				}  
			}

			//Getting voltage of cores
			group = new ThreadGroup("coreVoltage");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)){				
					processBuilder = new ProcessBuilder();			
					//processBuilder.command("ssh", nodeInfo[i].getId(), "ipmitool", "sensor", "|", "grep", ipmi_core_voltage_id );
					int length = 3+this.execCoreVoltage.length;
					String[] commandArgs = new String[length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execCoreVoltage){
						commandArgs[j] = s;
						j++;
					}
					commandArgs[length-1] = ipmi_core_voltage_id;
					processBuilder.command(commandArgs);	
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting core voltage Worker Node"+nodeInfo[i].getId());
					CoreVoltageWorker procWorker = new CoreVoltageWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
			}

			threadArray = new Thread[ group.activeCount() ];
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering core voltage Workers");
				for(int i=0;i<nodeInfo.length;i++){
					if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){
						nodeInfo[i].setCoreVoltage(ProxyRequestUpdate.tmp_nodes_worker[i].getCoreVoltage());
					}
					else{
						nodeInfo[i].setCoreVoltage(null);
					}
				}  
			}

			//Getting memory usage		    
			group = new ThreadGroup("Memory");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)){				
					processBuilder = new ProcessBuilder();
					String[] commandArgs = new String[2+this.execMemUsage.length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execMemUsage){
						commandArgs[j] = s;
						j++;
					}
					processBuilder.command(commandArgs);				
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting memory Worker Node"+nodeInfo[i].getId());
					MemoryWorker procWorker = new MemoryWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
			}

			threadArray = new Thread[ group.activeCount() ];
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering Memory Workers");
				for(int i=0;i<nodeInfo.length;i++){
					if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){
						nodeInfo[i].setMemoryUsage(ProxyRequestUpdate.tmp_nodes_worker[i].getMemoryUsage());
					}
					else{
						long mem_val = 0;
						nodeInfo[i].setMemoryUsage(mem_val);
					}
					log.trace("Set MemoryUsage of Node " + nodeInfo[i].getId() + " to " + nodeInfo[i].getMemoryUsage());
				}  
			}

			//Getting actual fan RPMs
			group = new ThreadGroup("fanRPM");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)){	
					processBuilder = new ProcessBuilder();
					String[] commandArgs = new String[2+this.execFANUsage.length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execFANUsage){
						commandArgs[j] = s;
						j++;
					}
					processBuilder.command(commandArgs);
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting fan RPM Worker Node"+nodeInfo[i].getId());
					FanRPMWorker procWorker = new FanRPMWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
			}

			threadArray = new Thread[ group.activeCount() ];
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering Fan RPM Workers");
				for(int i=0;i<nodeInfo.length;i++){
					if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){
						if(ProxyRequestUpdate.tmp_nodes_worker[i].getFanActualRPMs()!=null){
							int nr_of_fans = ProxyRequestUpdate.tmp_nodes_worker[i].getFanActualRPMs().length;
							nodeInfo[i].setFanActualRPMs(new int[nr_of_fans]);
							for(int j=0;j<nr_of_fans;j++){
								log.debug("Set Fan RPM of node " + nodeInfo[i].getId() + " to:" + ProxyRequestUpdate.tmp_nodes_worker[i].getFanActualRPMs()[j]);
								nodeInfo[i].getFanActualRPMs()[j] = ProxyRequestUpdate.tmp_nodes_worker[i].getFanActualRPMs()[j];
							}	
						}
						else{
							nodeInfo[i].setFanActualRPMs(null);
						}
					}
					else{
						nodeInfo[i].setFanActualRPMs(null);
					}
				}  
			}

			//Getting storage unit's read/write rates
			group = new ThreadGroup("io");
			for(int i=0;i<nodeInfo.length;i++){
				if(!nodeInfo[i].getState().equals(NodeInfo.State.STANDBY)&&ProxyRequestUpdate.tmp_nodes_worker[i]!=null){

					processBuilder = new ProcessBuilder();			
					//processBuilder.command("ssh", nodeInfo[i].getId(),"iostat","-xm","1","2","|","grep","'sda '"); // -dk
					String[] commandArgs = new String[2+this.execIOSTATS.length];
					commandArgs[0] = "ssh";
					commandArgs[1] = nodeInfo[i].getId();
					int j=2;
					for(String s : execIOSTATS){
						commandArgs[j] = s;
						j++;
					}
					processBuilder.command(commandArgs);	
					com_and_args = processBuilder.command();
					for(String s : com_and_args){
						log.trace("com_and_args: " + s);
					}
					processBuilder.directory(new File(execDir));
					log.info("Starting IO Worker Node"+nodeInfo[i].getId());
					ProcessBuilderWorker procWorker = new ProcessBuilderWorker(group, "Node"+nodeInfo[i].getId(), i, processBuilder);
					procWorker.start();
				}
				else{
					nodeInfo[i].setStorageUnitReadRate(0.0);	
					nodeInfo[i].setStorageUnitWriteRate(0.0);	
				}
			}

			threadArray = new Thread[ group.activeCount() ];
			// Array mit allen Threads der Grouppe group fuellen
			group.enumerate( threadArray );

			// Array ausgeben
			for ( Thread t : threadArray )
				log.trace( t );

			synchronized( group )
			{
				while ( group.activeCount() > 0 ){
					log.trace("Active Threads: " + group.activeCount());
					try {
						group.wait( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.info("Gathering IO Workers");
				for(int i=0;i<nodeInfo.length;i++){
					nodeInfo[i].setStorageUnitReadRate(ProxyRequestUpdate.readIos[i]);
					log.debug("Read IO of " + nodeInfo[i].getId() + " :" + ProxyRequestUpdate.readIos[i]);
					nodeInfo[i].setStorageUnitWriteRate(ProxyRequestUpdate.writeIos[i]);
					log.debug("Write IO of " + nodeInfo[i].getId() + " :" + ProxyRequestUpdate.writeIos[i]);
				}  
			}

			//update again jobrefs
			pbsConnection = new PbsConnection(InetAddress.getLocalHost().getHostAddress());
			disRequest = DisRequestStatusNode.make();
			disResponse = pbsConnection.send(disRequest);
			pbsConnection.close();

			if(!(disResponse instanceof DisResponseStatus)){
				nodeInfo = null;
			}else{
				disResponseStatus = (DisResponseStatus) disResponse;
				for(int i=0; i<disResponseStatus.getStati().length; ++i){
					NodeInfo nodeInfotmp = new NodeInfo(disResponseStatus.getStati()[i]);
					nodeInfo[i].setJobRefs(nodeInfotmp.getJobRefs());
					nodeInfo[i].setJobs(nodeInfotmp.getJobs());
				}
			}
		}
		else{
			for(int i=0;i<nodeInfo.length;i++){
				nodeInfo[i].setCpuUsage(0);
				nodeInfo[i].setCoreUsages(null);
				nodeInfo[i].setCoreFrequencies(null);
				nodeInfo[i].setCoreVoltage(null);
				nodeInfo[i].setMemoryUsage(0);
				nodeInfo[i].setFanActualRPMs(null);
				nodeInfo[i].setStorageUnitReadRate(0.0);	
				nodeInfo[i].setStorageUnitWriteRate(0.0);
			}
		}	

		pbsConnection = new PbsConnection(InetAddress.getLocalHost().getHostAddress());
		disRequest = DisRequestStatusJob.make();
		disResponse = pbsConnection.send(disRequest);
		pbsConnection.close();

		if(!(disResponse instanceof DisResponseStatus)){
			jobInfo = null;
		}else{
			disResponseStatus = (DisResponseStatus) disResponse;
			jobInfo = new JobInfo[disResponseStatus.getStati().length];
			for(int i=0; i<disResponseStatus.getStati().length; ++i){
				jobInfo[i] = new JobInfo(disResponseStatus.getStati()[i]);
			}
		}


		//Summarizing updates to ProxyResponse
		ProxyResponseUpdate proxyResponseUpdate = new ProxyResponseUpdate();

		proxyResponseUpdate.setJobInfo(jobInfo);

		proxyResponseUpdate.setNodeInfo(nodeInfo);

		proxyResponseUpdate.setPower_Consumption(output);

		return proxyResponseUpdate;
	}



	/**
	 * @return the execPowerCons
	 */
	public String getExecPowerCons() {
		return execPowerCons;
	}



	/**
	 * @return the mpstat_idle_col
	 */
	public String getMpstat_idle_col() {
		return mpstat_idle_col;
	}

	/**
	 * @param mpstat_idle_col the mpstat_idle_col to set
	 */
	public void setMpstat_idle_col(String mpstat_idle_col) {
		this.mpstat_idle_col = mpstat_idle_col;
	}

	/**
	 * @return the ipmi_core_voltage_id
	 */
	public String getIpmi_core_voltage_id() {
		return ipmi_core_voltage_id;
	}

	/**
	 * @param ipmi_core_voltage_id the ipmi_core_voltage_id to set
	 */
	public void setIpmi_core_voltage_id(String ipmi_core_voltage_id) {
		this.ipmi_core_voltage_id = ipmi_core_voltage_id;
	}

	/**
	 * @return the execCPUUsage
	 */
	public String[] getExecCPUUsage() {
		return execCPUUsage;
	}

	/**
	 * @return the execDir
	 */
	public String getExecDir() {
		return execDir;
	}
	
	class ProcessBuilderWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public ProcessBuilderWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process = null;
			try {
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);		            
					String line = "";
					int j=0;
					while(j<1){
						line = in.readLine();
						j++;
					}	
					log.trace("Parsing line "+ line);
					String[] parts = line.split("\\s+");
					parts[5] = parts[5].trim();
					double readrate_val = Double.parseDouble(parts[5]);
					parts[6] = parts[6].trim();
					double writerate_val = Double.parseDouble(parts[6]);
					log.debug("ReadRate of " + name + " " + parts[0] + "[MB]: " + readrate_val);
					log.debug("WriteRate of " + name + " " + parts[0] + "[MB]: " + writerate_val);
					ProxyRequestUpdate.readIos[id] = readrate_val;
					log.trace("IOWorker " + name + " read io " + readrate_val);
					ProxyRequestUpdate.writeIos[id] = writerate_val;
					log.trace("IOWorker " + name + " write io " + writerate_val);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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

	}
	
	class FanRPMWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public FanRPMWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process = null;
			try {				
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);		            
					String line;

					List<Integer> fans = new ArrayList<Integer>();	

					int j = 0;
					line = in.readLine();
					while (line != null && j < 100) {
						log.trace("Parsing line "+ line);
						String[] parts = line.split("\\|");
						if(!parts[0].trim().contains("ivisor")){
							parts[1] = parts[1].trim();
							int rpm_val = 0;
							try{
								String intval = parts[1].substring(0, parts[1].length()-4);
								rpm_val = Integer.parseInt(intval);
							}
							catch(NumberFormatException nr_ex){
								log.debug("RPM of Fan-nr." + j + " : couldn't be detected");
								rpm_val = 0;
							}
							catch(StringIndexOutOfBoundsException strex){
								log.debug("RPM of Fan-nr." + j + " : couldn't be detected");
								rpm_val = 0;
							}
							catch(Exception ex){
								log.debug("RPM of Fan-nr." + j + " : couldn't be detected");
								rpm_val = 0;
							}								
							log.debug("RPM of " + ProxyRequestUpdate.tmp_nodes_worker[id].getId() + " of Fan-nr." + j + " :" + rpm_val);
							fans.add(rpm_val);
							j++;								
						}
						line = in.readLine();
					}
					ProxyRequestUpdate.tmp_nodes_worker[id].setFanActualRPMs(new int[fans.size()]);
					for(int k=0;k<fans.size();k++){
						ProxyRequestUpdate.tmp_nodes_worker[id].getFanActualRPMs()[k] = fans.get(k);
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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
	}
	
	class CoreVoltageWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public CoreVoltageWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process= null;
			try {
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);
					List<Double> voltage_per_cpu_list = new ArrayList<Double>();
					String line;
					int index = 0;
					line = in.readLine();
					while(line!=null){
						log.trace("Parsing line "+ line);
						String[] parts = line.split("\\|");
						double volt_val = 0.0;
						try {
							volt_val = Double.parseDouble(parts[1].trim());
						} catch (NumberFormatException e) {
							log.debug("Could not parse voltage value!");
							log.debug("Current value is: " + parts[1].trim() + " , Set to 0.0V");
							volt_val = 0.0;
						}
						log.debug("Voltage of "+ ProxyRequestUpdate.tmp_nodes_worker[id].getId() + " of Core: " + volt_val);
						voltage_per_cpu_list.add(index++, volt_val);
						line = in.readLine();
					}

					double[] volt_values = new double[voltage_per_cpu_list.size()];
					for (int k = 0; k < voltage_per_cpu_list.size(); k++) {
						Double volt  = voltage_per_cpu_list.get(k);
						volt_values[k] = volt.doubleValue();
					}						
					ProxyRequestUpdate.tmp_nodes_worker[id].setCoreVoltage(volt_values);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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
	}
	
	class CoreFreqWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public CoreFreqWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process = null;
			try {
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);		            
					String line;
					ProxyRequestUpdate.tmp_nodes_worker[id].setCoreFrequencies(new double[ProxyRequestUpdate.tmp_nodes_worker[id].getnCores()]);
					for(int j=0;j<ProxyRequestUpdate.tmp_nodes_worker[id].getnCores();j++){
						line = in.readLine();
						log.trace("Parsing line "+ line);
						String[] parts = line.split(":");
						double freq_val = Double.parseDouble(parts[1].trim());
						log.debug("Frequency of node "+ ProxyRequestUpdate.tmp_nodes_worker[id].getId() + " of Core-nr." + j + " :" + freq_val);
						ProxyRequestUpdate.tmp_nodes_worker[id].getCoreFrequencies()[j] = freq_val/1000;			            
					}		
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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
	}
	
	class CPUusageWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public CPUusageWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process = null;
			try {
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
					ProxyRequestUpdate.tmp_nodes_worker[id].setCpuUsage(0.0);
					ProxyRequestUpdate.tmp_nodes_worker[id].setCoreUsages(new double[ProxyRequestUpdate.tmp_nodes_worker[id].getnCores()]);
					for(int j=0;j<ProxyRequestUpdate.tmp_nodes_worker[id].getnCores();j++){			            
						double usage_val = 0.0;  
						ProxyRequestUpdate.tmp_nodes_worker[id].getCoreUsages()[j] = usage_val;			            
					}	
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);		            
					String line;
					//read first 3 header lines + 1 all CPU line + 
					//2 header lines + NR_of_cores lines 
					int lines_to_swallow = 5+1+ProxyRequestUpdate.tmp_nodes_worker[id].getnCores();
					for(int k=0;k<lines_to_swallow;k++){
						log.trace(in.readLine());
					}
					line = in.readLine();
					log.trace("Parsing line "+ line);
					String[] parts = line.split("\\s+");
					int col = Integer.parseInt(getMpstat_idle_col());
					
					double usage_val = 100 - Double.parseDouble(parts[col].trim());
					log.debug("CPU Usage of node " + ProxyRequestUpdate.tmp_nodes_worker[id].getId() + " : " + usage_val);
					ProxyRequestUpdate.tmp_nodes_worker[id].setCpuUsage(usage_val);
					ProxyRequestUpdate.tmp_nodes_worker[id].setCoreUsages(new double[ProxyRequestUpdate.tmp_nodes_worker[id].getnCores()]);

					for(int j=0;j<ProxyRequestUpdate.tmp_nodes_worker[id].getnCores();j++){
						line = in.readLine();
						log.trace("Parsing line "+ line);
						parts = line.split("\\s+");
						usage_val = 100 - Double.parseDouble(parts[col].trim());
						log.debug("CPU usage of Core-nr." + j + " :" + usage_val);
						ProxyRequestUpdate.tmp_nodes_worker[id].getCoreUsages()[j] = usage_val;			            
					}		
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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
	}
	
	class MemoryWorker extends Thread{
		ProcessBuilder pb;
		int id;
		String name;

		public MemoryWorker(ThreadGroup group, String name ,int id, ProcessBuilder pb )
		{
			super(group, name);
			this.pb = pb;
			this.id = id;
			this.name = name;
		}		 

		public void run()
		{
			Process process = null;
			try {
				process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					log.error("Cant't get exit code for " + pb.toString() + 
							". " + e.getMessage());
				}

				if(process.exitValue() !=0) {
					log.error("Exit code for " + pb.toString() + " is not zero");
				}
				else
				{
					InputStream inputStream = process.getInputStream();
					InputStreamReader r = new InputStreamReader(inputStream);
					BufferedReader in = new BufferedReader(r);		            
					String line;						
					line = in.readLine();
					log.trace("Parsing line "+ line);
					String[] parts = line.split(":");
					parts[1] = parts[1].trim();
					String[] subParts = parts[1].split("\\s");
					double mem_val = Double.parseDouble(subParts[0].trim());
					mem_val = mem_val / (1024 * 1024);
					log.debug("Memory usage of " + ProxyRequestUpdate.tmp_nodes_worker[id].getId() + " [GByte]: " + mem_val);
					ProxyRequestUpdate.tmp_nodes_worker[id].setMemoryUsage(mem_val);			            							
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info(e1.getMessage());
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
	}
}
