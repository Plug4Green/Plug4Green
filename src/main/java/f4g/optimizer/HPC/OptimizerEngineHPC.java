/**
* ============================== Header ============================== 
* file:          OptimizerEngineHPC.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-04-27 15:09:52 +0200 (vie, 27 abr 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1386 $
* 
* short description:
*   This class contains the algorithm for HPC computing.
*   
* ============================= /Header ==============================
*/

package f4g.optimizer.HPC;


import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.actions.ActionRequest.ActionList;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.StandByAction;
import f4g.schemas.java.actions.StartJobAction;

import f4g.schemas.java.allocation.HpcClusterAllocation;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.HpcClusterAllocationResponse;

import org.apache.commons.jxpath.JXPathContext;
import f4g.commons.optimizer.ICostEstimator;
import f4g.optimizer.OptimizerEngine;
import f4g.optimizer.HPC.OptimalJob;
import f4g.optimizer.utils.Utils;
import f4g.commons.controller.IController;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.ApplicationBenchmark;
import f4g.schemas.java.metamodel.CoreLoad;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.Datacenter;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.Fan;
import f4g.schemas.java.metamodel.HardDisk;
import f4g.schemas.java.IDREFS;
import f4g.schemas.java.metamodel.IoRate;
import f4g.schemas.java.metamodel.JobPriority;
import f4g.schemas.java.metamodel.JobTime;
import f4g.schemas.java.metamodel.Mainboard;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.NrOfCores;
import f4g.schemas.java.metamodel.NrOfNodes;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.Queue;
import f4g.schemas.java.metamodel.RPM;
import f4g.schemas.java.metamodel.Site;
import f4g.schemas.java.metamodel.JobStatus;
import f4g.schemas.java.metamodel.Job;
import f4g.schemas.java.metamodel.NodeStatus;
import f4g.schemas.java.metamodel.RAMStick;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.RackableServer;
import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.CPU;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.actions.ObjectFactory;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBElement;

import java.io.InputStream;
import java.util.Properties;
import java.util.Collections;



/**
 * This class contains the algorithm for HPC computing.
 * 
 * @author cdupont
 *
 */
public class OptimizerEngineHPC extends OptimizerEngine{
	
	static List<Job> jobList = new LinkedList<Job>(); // List of jobs in the queue
	static List<Server> srvList = new LinkedList<Server> (); // List of Servers
	static List<Job> runningList = new LinkedList<Job>(); // List of running jobs
	static List<String> jobIdList = new ArrayList<String>();
	static List<Boolean> reservedList = new ArrayList<Boolean> (); 
	static List<Double> serverMemoryList = new ArrayList<Double> ();
	static List<Integer> serverCoresList = new ArrayList<Integer> ();
	static List<Integer> serverCoresInUseList = new ArrayList<Integer> ();
	ActionRequest actionRequest = new ActionRequest(); 
	ActionList actionList = new ActionList();
	FIT4Green model = null;
	List<PowerOnAction> serverOns = new ArrayList<PowerOnAction> ();
	List<PowerOffAction> serverOffs = new ArrayList<PowerOffAction> ();

	static int scheduling = 0; // 0 = fifo, 1 = bff, 2 = bbf
	static int threshold = 0;
	static boolean poweroff = false;
	

	/**
	 * @param main
	 */
	public OptimizerEngineHPC(IController controller, IPowerCalculator powerCalculator, ICostEstimator costEstimator) {
		super(controller, powerCalculator, costEstimator);
		
		Properties prop = new Properties();
		
		// Set the simulation flag as false, considering measured power
		powerCalculator.setSimulationFlag(false);
	    
	    try
	    {
	    	InputStream is = this.getClass().getClassLoader().getResourceAsStream("optimizer/OptimizerEngineHPC.properties");
	    	prop.load(is);
	    	scheduling = Integer.parseInt(prop.getProperty("scheduling").trim());
	    	threshold = Integer.parseInt(prop.getProperty("threshold").trim());
	    	poweroff = Boolean.parseBoolean(prop.getProperty("poweroff").trim());
	    	log.debug("Scheduling parameter is set as: " + scheduling);
	    	log.debug("Threshold parameter is set as: " + threshold);
	    	log.debug("Poweroff parameter is set as: " + poweroff);
	    }
	    catch (Exception e)
	    {
	    	// Some error with handling files, setting the scheduling as fifo
	    	scheduling = 0;
	    	threshold = 50;
	    	poweroff = false;
	    	log.debug("Error with handling files, setting the scheduling as fifo: " + scheduling);
	    	log.debug("Error with handling files, setting the threshold as: " + threshold);
	    	log.debug("Error with handling files, setting the poweroff as: " + poweroff);
	    }
	}
	
	/**
	 * Gets a best cluster for a job in the federated scenario
	 * 
	 * @param int nodes, int cores, long memory, long wallTime, FIT4Green model
	 * @return Data Centre ID
	 */
	public String getBestCluster(HpcClusterAllocation hpcRequest, FIT4Green model) {
		
		log.debug("OptimizerEngineHPC: getBestCluster: Getting best cluster for the job");

		String dcID = new String();
		if(hpcRequest.isEnergyAware() == false)
		{
			log.debug("OptimizerEngineHPC: Using fast as possible scheduling");
			dcID = getDCfastAsPossible(hpcRequest, model);
			log.debug("OptimizerEngineHPC: Selected cluster for job execution: " +dcID);
		}
		else if(hpcRequest.isEnergyAware() == true)
		{
			log.debug("OptimizerEngineHPC: Using energy aware scheduling");
			dcID = getDCpowerCalculator(hpcRequest, model);
			log.debug("OptimizerEngineHPC: Selected cluster for job execution: " +dcID);
		}
		return dcID;
			
	}
	
	/**
	 * Gets a best cluster fast as possible for a job in the federated scenario
	 * 
	 * @param int nodes, int cores, long memory, long wallTime, FIT4Green model
	 * @return Data Centre ID
	 */
	
	public String getDCfastAsPossible(HpcClusterAllocation hpcRequest, FIT4Green model) {
		
		String dcID = new String();
				
		String tempString = hpcRequest.getSuitableClusters();
		String[] suitableClusters = tempString.split(" ");

		Job myJob = new Job();
		NrOfCores nOfCores = new NrOfCores();
		nOfCores.setValue(hpcRequest.getNeededCoresPerNode());
		myJob.setNeededCoresPerNode(nOfCores);
		
		MemoryUsage neededMemory = new MemoryUsage ();
		neededMemory.setValue(hpcRequest.getNeededMemory());
		myJob.setNeededMemory(neededMemory);
		
		NrOfNodes nrOfNodes = new NrOfNodes ();
		nrOfNodes.setValue(hpcRequest.getNumberOfNodes());
		myJob.setNumberOfNodes(nrOfNodes);
		
		JobTime wallTime = new JobTime ();
		wallTime.setValue(hpcRequest.getWallTime());
		myJob.setWallTime(wallTime);
		
		//int candidateQueueSize = -1;
		boolean isSuitable = false;
		//long latestFinish = hpcRequest.getLatestFinish();
		long estimatedLatestFinish;
		long estimatedWaitTime;
		long candidateFinish = -1;
		
		int dcRank = 0;
		int smallestRank = 0;
		double C = 0.0;
		
		// Get the smallest rank from metamodel
		smallestRank = getSmallestRank(hpcRequest.getBenchmarkName(), model);
		
		// Go through all the data centers of the meta-model
		String query = "//datacenter";
		JXPathContext context = JXPathContext.newContext(model);
		Iterator elements = context.iterate(query);
		
        // Iteration over the "datacenter" items
        while (elements.hasNext())
        {       	
        	Object element = elements.next();
        	
        	//Find out if the data centre is suitable for executing the job
        	String tempID = ((Datacenter)element).getFrameworkCapabilities().get(0).getId();
        	isSuitable = isSuitableCluster(tempID, suitableClusters);
        	estimatedLatestFinish = 0;
        	estimatedWaitTime = 0;
        		
        	if(isSuitable)
        	{
        		log.debug("Data Centre is suitable: " +tempID);
        		dcRank = 0;
        		C = 0.0;
        		dcRank = getDCRank(hpcRequest.getBenchmarkName(), ((Datacenter)element));
    			log.debug("smallestRank: " + smallestRank);
    			log.debug("dcRank: " + dcRank);
    			C = (double)smallestRank/(double)dcRank;
    			log.debug("C: " + C);
        		
				//Get the estimated wait time
				estimatedWaitTime = (long) (getEstimatedWaitTime(((Datacenter)element), myJob) * 1.1);					
				estimatedLatestFinish = getEpochTime() + estimatedWaitTime + (long)(myJob.getWallTime().getValue() * C);
				log.debug("Data Centre estimatedWaitTime: " +estimatedWaitTime);
				log.debug("Data Centre estimatedLatestFinish: " +estimatedLatestFinish);
				
				// If the estimated waitTime is zero,
				// the data centre can start job execution immediately --> select it.
				if(estimatedWaitTime == 0)
				{
					log.debug("Resources were found and the size of the queue in the data centre is zero.");
					dcID = ((Datacenter)element).getFrameworkCapabilities().get(0).getId();
					break;
				}
				//If no data centre was found that can execute the job immediately, find the data centre
				//that can start job execution in a minimal delay, i.e., has the smallest queue size.
				else if(estimatedLatestFinish < candidateFinish || candidateFinish == -1) {
					log.debug("estimatedLatestFinish in the data centre: " +getEpochDate(estimatedLatestFinish));
					candidateFinish = estimatedLatestFinish;
					dcID = ((Datacenter)element).getFrameworkCapabilities().get(0).getId();
					log.debug("Selecting data center: "+dcID +", with the lowest estimated wait time.");
				}
//				else if(queueSize < candidateQueueSize || candidateQueueSize == -1)
//				{
//					log.debug("Size of the queue in the data centre: " +queueSize); 
//					log.debug("Resources found: " +resourcesFound);
//					candidateQueueSize = queueSize;
//					dcID = ((Datacenter)element).getFrameworkCapabilities().get(0).getId();
//				}
        	}
        }
		
		return dcID;
		
	}
	
	
	/**
	 * Gets a best cluster energy aware by utilizing the power calculator 
	 * for a job in the federated scenario
	 * 
	 * @param int nodes, int cores, long memory, long wallTime, FIT4Green model
	 * @return Data Centre ID
	 */
	
	public String getDCpowerCalculator(HpcClusterAllocation hpcRequest, FIT4Green model) {
		
		String dcID = new String();
		double jobPower = 0.0;
		double candidateJobPower = -1;
		//int queueSize = 0;
		//int numServers = 0;
		//double threshold = 0.0;
		double optObjective = 0.0;
		boolean clusterFound = false;
		//int candidateQueueSize = -1;
		long estimatedWaitTime;
		long estimatedLatestFinish;
		long candidateFinish = -1;
		long latestFinish = hpcRequest.getLatestFinish();
		
		int dcRank = 0;
		int smallestRank = 0;
		double C = 0.0;
		double energyChange = 0.0;
		
		// Get the smallest rank from metamodel
		smallestRank = getSmallestRank(hpcRequest.getBenchmarkName(), model);
		log.debug("Smallest rank: "+smallestRank);
		
		Job myJob = new Job();

		NrOfCores nOfCores = new NrOfCores();
		nOfCores.setValue(hpcRequest.getNeededCoresPerNode());
		myJob.setNeededCoresPerNode(nOfCores);
		
		MemoryUsage neededMemory = new MemoryUsage ();
		neededMemory.setValue(hpcRequest.getNeededMemory());
		myJob.setNeededMemory(neededMemory);
		
		NrOfNodes nrOfNodes = new NrOfNodes ();
		nrOfNodes.setValue(hpcRequest.getNumberOfNodes());
		myJob.setNumberOfNodes(nrOfNodes);
		
		JobTime wallTime = new JobTime ();
		wallTime.setValue(hpcRequest.getWallTime());
		myJob.setWallTime(wallTime);
						
		
		String tempString = hpcRequest.getSuitableClusters();
		String[] suitableClusters = tempString.split(" ");
		boolean isSuitable = false;
		
		log.debug("Optimization objective: " +getOptiObjective());
		
		// Go through all the sites of the meta-model
		String query = "//site";
		JXPathContext context = JXPathContext.newContext(model);
		Iterator elements = context.iterate(query);
		
        // Iteration over the "site" items
        while (elements.hasNext())
        {       	
        	Object element = elements.next();
        	
        	//Use either PUE or CUE based on the optimization objective
        	if(getOptiObjective().toString().matches("Power"))
        		optObjective = ((Site)element).getPUE().getValue();
        	else
        		optObjective = ((Site)element).getCUE().getValue();
        		
			Iterator<Datacenter> dcItr = ((Site)element).getDatacenter().iterator();
			while(dcItr.hasNext()) 
			{
	    		//queueSize = 0;
	    		//numServers = 0;
	    		//threshold = 0.0;
				dcRank = 0;
	    		jobPower = 0.0;
	    		estimatedLatestFinish = 0;
	    		estimatedWaitTime = 0;
	    		C = 0.0;
	    		energyChange = 0.0;
	    		
				Datacenter dcObject = dcItr.next();
				dcRank = getDCRank(hpcRequest.getBenchmarkName(), dcObject);
				//numServers = getClusterNumServers(dcObject);
				//queueSize = getQueueSize(dcObject);
				//log.debug("Cluster queueSize: " +queueSize + ", numServers: " +numServers);
				
	        	//Find out if the data centre is suitable for executing the job
	        	String tempID = dcObject.getFrameworkCapabilities().get(0).getId();
	        	isSuitable = isSuitableCluster(tempID, suitableClusters);
	        	if(isSuitable)
	        	{				
					Iterator<RackableServer> serverItr = dcObject .getRack().get(0).getRackableServer().iterator();					
					while(serverItr.hasNext()) 
					{
						Server serverObject = serverItr.next();

		        		// The node is a compute node
		        		if(serverObject.getName().toString().matches("HPC_COMPUTE_NODE"))
		        		{
		        					        					      			
		        			//Estimate the power consumption of the job in the cluster
		        			// by using a single server (all servers are homogeneous)
		        			Server tempServer = getServerIdle(serverObject, powerCalculator);
		        			
		        			double idlePower = 0.0;
		        			idlePower = computeIdlePower(tempServer, powerCalculator);
		        					
		        			log.debug("Idle power consumption of a server: " +idlePower);
		        			
		        			int serverCores = Utils.getNbCores(tempServer);
		        			int CPUs = getNbCPU(tempServer);
		        			int coresPerCPU = getNbCoresCPU(tempServer);
		        			
		        			log.debug("Number of total cores: " + serverCores);
		        			log.debug("Number of CPUs: " + CPUs );
		        			log.debug("Number of cores per CPU: " + coresPerCPU);
		        					        					        					        					        				        					        			
							// Server in the cluster should have enough cores
		        			if(serverCores >= myJob.getNeededCoresPerNode().getValue())
		        			{		        				
		        				jobPower = computePowerForJob(tempServer, myJob, powerCalculator);
		        				
		        			}
		        			log.debug("Change in power: " +(jobPower - idlePower));
		        			// Compute with job walltime and CUE to get emissions estimate
		        			log.debug("smallestRank: " + smallestRank);
		        			log.debug("dcRank: " + dcRank);
		        			C = (double)smallestRank/(double)dcRank;
		        			log.debug("C: " + C);
		        			jobPower = jobPower * myJob.getWallTime().getValue() * C * optObjective;
		        			log.debug("Job Energy Estimate: " + jobPower);
		        			energyChange = idlePower * myJob.getWallTime().getValue() * C * optObjective;
		        			log.debug("Idle Energy Estimate: " + energyChange);
		        			energyChange = jobPower - energyChange;
		        			log.debug("Change in energy: " + energyChange);
		        			
		        			break;
		        		}
						
					}
					//threshold = (double)queueSize/numServers;
					
					//Get the estimated wait time
					estimatedWaitTime = (long) (getEstimatedWaitTime(dcObject, myJob) * 1.1);					
					estimatedLatestFinish = getEpochTime() + estimatedWaitTime + (long)(myJob.getWallTime().getValue() * C);
					log.debug("Estimated wait time of the cluster: " +estimatedWaitTime);
					log.debug("Estimated start time of the job: " + getEpochDate(getEpochTime() + estimatedWaitTime));
					log.debug("Estimated Latest Finish date of the Job: " + getEpochDate(estimatedLatestFinish));
					log.debug("User defined latest Finish date of the Job: " + getEpochDate(latestFinish));									
					
				
					
					//If the threshold is below 0.7, select the cluster
					// and set clusterFound = true
					//if(threshold < 0.7)
					
					//If the estimated Latest Finish time of the job is smaller or equal
					//to the user defined latest finish time, select the cluster
					//and set clusterFound = true
					if(estimatedLatestFinish <= latestFinish || latestFinish == 0)
					{
						if(jobPower < candidateJobPower || candidateJobPower == -1)
						{
							candidateJobPower = jobPower;
							dcID = dcObject.getFrameworkCapabilities().get(0).getId();
							log.debug("Estimated latest finish <= latestFinish");
							log.debug("Selecting cluster with the lowest energy estimate: " +dcID);
							clusterFound = true;
						}					
					}
					//If no cluster has been found where estimated Latest Finish time of the job
					//is smaller or equal to the user defined latest finish time,
					//select a cluster with the smallest estimated Latest Finish time.
					else if(clusterFound == false)
					{
						
						if(estimatedLatestFinish < candidateFinish || candidateFinish == -1)
						{
							log.debug("estimatedLatestFinish in the data centre: " +getEpochDate(estimatedLatestFinish));
							candidateFinish = estimatedLatestFinish;
							dcID = dcObject.getFrameworkCapabilities().get(0).getId();
							log.debug("Selecting cluster with the lowest estimatedLatestFinish: " +dcID);
						}
						
//						if(queueSize < candidateQueueSize || candidateQueueSize == -1)
//						{
//							log.debug("Size of the queue in the data centre: " +queueSize); 
//							candidateQueueSize = queueSize;
//							dcID = dcObject.getFrameworkCapabilities().get(0).getId();
//						}
						
					}
	        	}
			}
        }
		
		return dcID;
		
	}
	
	/**
	 * Checks if a data centre has free resources for a job
	 * 
	 * @param Datacenter datacenter, Job myJob
	 * @return true if resources found, false otherwise
	 */
	
	public boolean checkDataCenter(Datacenter datacenter, Job myJob)
	{
		
		double serverMemory;	
		int serverCores;
		int coresInUse;
			
		// Go through the meta-model and forms temporary data structures for
		// the job queue, jobs that are currently running, and servers
		
		Iterator<RackableServer> myItr = datacenter.getRack().get(0).getRackableServer().iterator();

        // Iteration over the "rackableServer" items
        while (myItr.hasNext())
        {       	
        	Server serverObject = myItr.next();
           			
        	// The node is the RMS
    		if(((Server)serverObject).getName().toString().matches("HPC_RESOURCE_MANAGEMENT"))
    		{
    			
    			/// Form temporary data structures for the queue and running jobs
    			Iterator<Job> itr = ((Server)serverObject).getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
    			while(itr.hasNext()) 
    			{
    				// Retrieve the job from the meta-model and place it into a list
    				Job jobObject = itr.next();  								
    				enqueue(jobObject);
    			}
 		
    		}
        		
    		// Else:  the node is a computing node
    		else
    		{
    			// Create a temporary server structure
    			Server server = serverObject;   			
				serverMemory = 0;				
				serverCores = 0;
				coresInUse = 0;
				
				// Iteration over the "Cores" objects
				Iterator<CPU> cpuIter = serverObject.getMainboard().get(0).getCPU().iterator();
				while(cpuIter.hasNext())
				{
					CPU cpuObject = cpuIter.next();
					Iterator<Core> coreIter = cpuObject.getCore().iterator();
					while(coreIter.hasNext())
					{
						Core coreObject = coreIter.next();
						serverCores++;
					}
				}
								
				coresInUse = server.getNativeOperatingSystem().getNode().get(0).getCoresInUse().getValue();
    			
				// Iteration over the RAMStick objects
    			Iterator<RAMStick> iter = serverObject.getMainboard().get(0).getRAMStick().iterator();
				while(iter.hasNext())
				{
					RAMStick tempObject =  iter.next();
					serverMemory = serverMemory + tempObject.getSize().getValue();
					
				}
				
				try {
					
	    			Iterator<Object> jobIter = serverObject.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().iterator();
	    			while(jobIter.hasNext())
	    			{
	    				Job tempJob =  (Job) jobIter.next();
	    				serverMemory = serverMemory - tempJob.getNeededMemory().getValue();					
	    			}
				}
				catch (NullPointerException e)
				{
					
				}
				  									
				serverMemoryList.add(serverMemory);
				serverCoresList.add(serverCores);
				serverCoresInUseList.add(coresInUse);
				reservedList.add(false);
				srvList.add(server); 						
    		}       

        }
        
        int[] serverNumber = new int[myJob.getNumberOfNodes().getValue()];
		searchServers(myJob.getNumberOfNodes().getValue(), myJob.getNeededMemory().getValue(), myJob.getNeededCoresPerNode().getValue(), serverNumber);
		boolean serversFound = true;
		serversFound = checkServerNumber(serverNumber, serversFound, myJob.getNumberOfNodes().getValue());
		
		// Clear all temporary data structures
		jobList.clear();
		runningList.clear();
		srvList.clear();
		jobIdList.clear();
		reservedList.clear();
		serverMemoryList.clear();
		serverCoresList.clear();
		serverCoresInUseList.clear();
		
		if(serversFound == true)
		{
			return true;
		}
		else
		{
			return false;
		}	
		
	}
	
	/**
	 * Checks if a data centre is suitable for a job
	 * 
	 * @param Datacenter ID, List of suitable clusters
	 * @return true if suitable, false otherwise
	 */
	
	public boolean isSuitableCluster(String dcID, String[] suitableClusters)
	{
		boolean isSuitable = false;
		
        for(int i=0;i<suitableClusters.length;i++)
        {
        	if(suitableClusters[i].matches(dcID))
        	{
        		isSuitable = true;
        		break;
        	}
        }
		
		return isSuitable;
	}
	
	/**
	 * Used for testing purposes
	 * 
	 */
	
	public void testAllocateResource()
	{
		AllocationRequest request = new AllocationRequest();
		//Creates a request
		HpcClusterAllocation hpcRequest = new HpcClusterAllocation();
		hpcRequest.setNeededCoresPerNode(4);
		hpcRequest.setNeededMemory(2);
		hpcRequest.setNumberOfNodes(1);
		hpcRequest.setWallTime(600);
		hpcRequest.setEnergyAware(true);
		hpcRequest.setLatestFinish(getEpochTime() + 500);
		//hpcRequest.setSuitableClusters("FzjComJufit FzjComJuggle ComFzjDune");
		hpcRequest.setSuitableClusters("FzjComJuggle FzjComJufit ComVTTDune");
		hpcRequest.setBenchmarkName("Linpack");
		
		f4g.schemas.java.allocation.ObjectFactory allocationFactory = new f4g.schemas.java.allocation.ObjectFactory();		
		request.setRequest((allocationFactory.createHpcClusterAllocation(hpcRequest)));
		
		AllocationResponse response = allocateResource(request, getModelCopy());

		HpcClusterAllocationResponse hpcResponse = (HpcClusterAllocationResponse)response.getResponse().getValue();
		
		log.debug("Selected cluster ID: " +hpcResponse.getClusterId());
	}
		
	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model the f4g model
	 * @return none
	 */
	@Override
	public void runGlobalOptimization(FIT4Green model) {
		
		//TODO: this function should be able to distinguish which cluster wants global optimization
		
		log.debug("OptimizerEngineHPC: runGlobalOptimization: Performing Global Optimization");		
		setModelCopy(model);
		
		//Just used for testing purposes
		//testAllocateResource();
		
				
		// Clear the actionList in the beginning, 
		// to make sure that it is empty before creating any actions
		try {		
			actionRequest.getActionList().getAction().clear();
			actionList.getAction().clear();
		}
		catch (NullPointerException e)
		{
			
		}

		double serverMemory;	
		int serverCores;
		int coresInUse;
			
		// Go through the meta-model and forms temporary data structures for
		// the job queue, jobs that are currently running, and servers

		String myQuery = "//datacenter";
		JXPathContext context = JXPathContext.newContext(getModelCopy());
		Iterator elements = context.iterate(myQuery);
		
        // Iteration over the "dataCenter" items
        while (elements.hasNext())
        {
    		// Clear the actionList in the beginning, 
    		// to make sure that it is empty before creating any actions
    		try {		
    			actionRequest.getActionList().getAction().clear();
    			actionList.getAction().clear();
    		}
    		catch (NullPointerException e)
    		{
    			
    		}
    		
        	Datacenter datacenter = (Datacenter)elements.next();
        	        	
    		if(datacenter.getFrameworkCapabilities().get(0).getStatus().toString().matches("RUNNING"))
    		{
            	log.debug("OptimizerEngineHPC: Performing Global Optimization on Data Centre: " +datacenter.getFrameworkCapabilities().get(0).getId());		

				// Iteration over the "RackableServer" objects
				Iterator<RackableServer> serverIter = datacenter.getRack().get(0).getRackableServer().iterator();
				while(serverIter.hasNext())
				{
					Server server = serverIter.next();
		        	// The node is the RMS
		    		if(server.getName().toString().matches("HPC_RESOURCE_MANAGEMENT"))
		    		{
		    			
		    			/// Form temporary data structures for the queue and running jobs
		    			Iterator<Job> itr = server.getNativeOperatingSystem().getClusterManagement().get(0).getQueue().getJobs().iterator();
		    			while(itr.hasNext()) 
		    			{
		    				// Retrieve the job from the meta-model and place it into a list
		    				Job jobObject = itr.next();  								
		    				enqueue(jobObject);
		    			}
		 		
		    		}
		        		
		    		// Else:  the node is a computing node
		    		else
		    		{
		    			// Create a temporary server structure
		    			Server tempServer = server;   			
						serverMemory = 0;				
						serverCores = 0;
						coresInUse = 0;
						
						// Iteration over the "CPU" objects
						Iterator<CPU> cpuIter = server.getMainboard().get(0).getCPU().iterator();
						while(cpuIter.hasNext())
						{
							CPU cpuObject = cpuIter.next();
							
							// Iteration over the "Core" objects
							Iterator<Core> coreIter = cpuObject.getCore().iterator();
							while(coreIter.hasNext())
							{
								Core coreObject = coreIter.next();
								serverCores++;
							}
						}
						
						coresInUse = tempServer.getNativeOperatingSystem().getNode().get(0).getCoresInUse().getValue();
		    			
						// Iteration over the RAMStick objects
		    			Iterator<RAMStick> iter = server.getMainboard().get(0).getRAMStick().iterator();
						while(iter.hasNext())
						{
							RAMStick tempObject =  iter.next();
							serverMemory = serverMemory + tempObject.getSize().getValue();
							
						}
						  			
	
						try {
							
			    			Iterator<Object> jobIter = tempServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().iterator();
			    			while(jobIter.hasNext())
			    			{
			    				Job tempJob =  (Job) jobIter.next();
			    				serverMemory = serverMemory - tempJob.getNeededMemory().getValue();		  					
			    			}
						}
						catch (NullPointerException e)
						{
							log.debug("No job ref associated.");
						}
								
						serverMemoryList.add(serverMemory);
						serverCoresList.add(serverCores);
						serverCoresInUseList.add(coresInUse);
						reservedList.add(false);
						srvList.add(tempServer); 						
		    		}
				}
       
    		}
        
	        log.debug("DATA STRUCTURES BEFORE SCHEDULING AND OPTIMIZATION:");
			
	        // Print the queue and the serverList, used for debugging
			printQueue();
			printServerList();
						
			// Go through the job queue and check which jobs can be executed
			dequeue();
			
			checkIdleServers(datacenter); // Check if some servers can be shut down
								
			// Print necessary lists after going through the queue, used for debugging
			log.debug("DATA STRUCTURES AFTER SCHEDULING AND OPTIMIZATION:");
			printQueue();
			printServerList();
			
					
			// Clear all temporary data structures
			jobList.clear();
			runningList.clear();
			srvList.clear();
			jobIdList.clear();
			reservedList.clear();
			serverMemoryList.clear();
			serverCoresList.clear();
			serverCoresInUseList.clear();
						
			// Set the action list to the actionRequest
			actionRequest.setActionList(actionList);
				
				
			// Check if there are any actions to execute
			boolean isActionListEmpty = checkActionList();
			
			if(isActionListEmpty == false)
			{
				printActionList();
				//Report the power before optimization
				//Datacenter tempdatacenter = Utils.getFirstDatacenter(model);
				double powerSaved = 0.0;
				double powerFormerly = 0.0;
				//double powerFormerly = powerCalculator.computePowerDatacenter(tempdatacenter).getActualConsumption();
				log.debug("datacenter power consumption before:" + powerFormerly);
				Power powerBefore = new Power();
				powerBefore.setValue(powerFormerly);
				
				// Get the Power Off and Power On actions from  the action list
//				for (JAXBElement<? extends AbstractBaseAction> action : actionList.getAction()){
//					if (action.getValue() instanceof PowerOnAction) 
//						serverOns.add((PowerOnAction)action.getValue());
//				}
//				
//				for (JAXBElement<? extends AbstractBaseAction> action : actionList.getAction()){
//					if (action.getValue() instanceof PowerOffAction) 
//					{
//						serverOffs.add((PowerOffAction)action.getValue());
//					}
//				}
									    
				//Report the power after optimization
//			    Datacenter newDatacenter = performOnOffs(serverOns, serverOffs, datacenter);
//				double powerAfter = powerCalculator.computePowerDatacenter(newDatacenter).getActualConsumption();
//				log.debug("datacenter power consumption after:" + powerAfter);
	
				//Report the saved power
				//powerSaved = powerAfter - powerBefore;
				Power powerAfter = new Power();
				powerAfter.setValue(0.0);
				log.debug("powerSaved: " + powerSaved);
				actionRequest.setComputedPowerBefore(powerBefore);
				actionRequest.setComputedPowerAfter(powerAfter);
				// invoke the Controller
				controller.executeActionList(actionRequest);
			}
			else
			{
				log.debug("The action list is empty, no actions to be done.");
			}
										
			// Clear the actionList
			actionRequest.getActionList().getAction().clear();
			actionList.getAction().clear();
		
        }
		
	}

	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest Data structure describing the resource allocation request 
	 * @return A data structure representing the result of the allocation
	 */
	@Override
	public AllocationResponse allocateResource(AllocationRequest allocationRequest, FIT4Green model) {
		
		log.debug("Processing request: " + allocationRequest);
		log.debug(" Request Operation: " + allocationRequest.getRequest().getValue());
		setModelCopy(model);
		
		// The allocation request must be of type HPCClusterAllocation
		if (allocationRequest == null ||
				allocationRequest.getRequest() == null ||	
				! (allocationRequest.getRequest().getValue() instanceof HpcClusterAllocation)) {

				log.warn("Allocation request is not correct for HPC");
				return null;
		}
		
		HpcClusterAllocation hpcRequest = (HpcClusterAllocation)allocationRequest.getRequest().getValue();
		
		log.debug(" Request Nodes: " + hpcRequest.getNumberOfNodes());
		log.debug(" Request Cores: " + hpcRequest.getNeededCoresPerNode());
		log.debug(" Request Memory: " + hpcRequest.getNeededMemory());
		log.debug(" Request WallTime: " + hpcRequest.getWallTime());
				
		//Based on the request values, get a suitable cluster
		String clusterID = new String();
		clusterID = getBestCluster(hpcRequest, model);
		
		//Creates a response
		HpcClusterAllocationResponse hpcResponse = new HpcClusterAllocationResponse();	
		hpcResponse.setClusterId(clusterID);
		
		f4g.schemas.java.allocation.ObjectFactory allocationFactory = new f4g.schemas.java.allocation.ObjectFactory();
		
		AllocationResponse response  = new AllocationResponse();
		response.setResponse((allocationFactory.createHpcClusterAllocationResponse(hpcResponse)));
					
		//response.setAllocationLog("OptimizerHPC: Allocated: expr=" + allocationRequest.getExpression() + ", value=" + allocationRequest.getValue());
		return response;
	}
	
	/**
	 * Method for searching servers
	 * 
	 * @param requirements for a job 
	 * @return servers found for the job
	 */
	
	public int[] searchServers(int numNodes, double memory, int numCores, int[] serverNumber)
	{
		// Initialize all elements as -1
		for(int i = 0; i < numNodes ; i++)
		{
			serverNumber[i] = -1;
		}
		int coresInUse = 0;
		int availableCores = 0;
		int candidateCores;
		// Search for appropriate servers
		for(int i = 0; i < numNodes; i++)
		{
			candidateCores = -1;
			//log.debug("Searching for number: " +i);
			
			for(int j = 0; j < srvList.size(); j++)
			{
				Server server = srvList.get(j);
				coresInUse = serverCoresInUseList.get(j);
				availableCores = serverCoresList.get(j) - coresInUse;
				
				NodeStatus status = server.getNativeOperatingSystem().getNode().get(0).getStatus();
				
				//log.debug("Server cores: " +serverCoresList.get(j));
				//log.debug("Available cores: " +availableCores); .toString().matches("IDLE")
				//log.debug("Cores in use: " +coresInUse);
				
				// The server must have enough free cores and enough memory
				// Status can be even running
				if(reservedList.get(j) == false && serverMemoryList.get(j) >= memory && availableCores >= numCores)
				{
					
					//log.debug("Candidate cores: " +candidateCores);	
					//Select the server with the least amount of available cores
					if(availableCores <= candidateCores || candidateCores == -1)
					{						
						serverNumber[i] = j;
						candidateCores = availableCores;
						//log.debug("Selecting server: " + j +" CoresInUse: " +(serverCoresInUseList.get(j) + numCores));				
						continue;
					}	
					else if((availableCores == candidateCores)  && (status.toString().matches("IDLE")))
					{
						serverNumber[i] = j;
						candidateCores = availableCores;
						//log.debug("Selecting server: " + j +" CoresInUse: " +(serverCoresInUseList.get(j) + numCores));				
						continue;
					}
				}
			}// for j
			
			if(serverNumber[i] != -1)
			{
				reservedList.set(serverNumber[i], true);
			}
			
		} // for i
				
		return serverNumber;
	}
	
	/**
	 * Method for checking if servers were found
	 * 
	 * @param serverNumber[], serversFound, number of nodes a job requires 
	 * @return boolean serversFound
	 */
	
	public boolean checkServerNumber(int[] serverNumber, boolean serversFound, int numNodes)
	{
		
		// Check if all needed servers were found
		for(int i = 0; i < numNodes ; i++)
		{
			if(serverNumber[i] == -1)
			{
				serversFound = false;
			}
			else
			{
				reservedList.set(serverNumber[i], false);
			}
		}
	
		return serversFound;
	}
	
	/**
	 * Method for placing a Job object into jobList
	 * Forms the queue based on the job priority
	 * @param Job
	 */
	public void enqueue(Job job)
	{
		
		int jobPriority = job.getPriority().getValue();
		long jobCompTime = job.getTimeOfStart().getValue() + job.getWallTime().getValue();
		int tempPriority;
		long tempCompTime;
		boolean jobInserted;
		
		jobInserted = false;
		
		// If the job is queued, add it into the job queue
		if(job.getStatus().toString().matches("QUEUED"))
		{
			// If the list is empty, just place it at the end
			if(jobList.isEmpty())
			{
				jobList.add(job);
			}	
			else
			{
				// Form the queue based on the priority values,
				// high priority jobs going into the top
				for(int i = 0; i < jobList.size(); i++)
				{
					Job tempjob = jobList.get(i);
					tempPriority = tempjob.getPriority().getValue();
					if(jobPriority > tempPriority)
					{
						jobList.add(i, job);
						jobInserted = true;
						break;
					}				
				}
				if(jobInserted == false)
				{
					jobList.add(job);
				}		
			}
		}
		// Else: add the job into the running jobs list
		else
		{
			// If the list is empty, just place it at the end
			if(runningList.isEmpty())
			{
				runningList.add(job);
			}	
			else
			{
				// Form queue based on priority
				for(int i = 0; i < runningList.size(); i++)
				{
					Job tempjob = runningList.get(i);
					tempCompTime = tempjob.getTimeOfStart().getValue() + tempjob.getWallTime().getValue();
					if(jobCompTime < tempCompTime)
					{
						runningList.add(i, job);
						jobInserted = true;
						break;
					}				
				}
				if(jobInserted == false)
				{
					runningList.add(job);
				}		
			}
		}
	}	
	
	/**
	 * Method for dequeuing a job from queue
	 * 
	 */
	
	public void dequeue()
	{
		// Priority FIFO
		if(scheduling == 0)
		{
			if(jobList.isEmpty())
			{
				log.debug("Queue empty.");
				//checkIdleServers(); // Check if some servers can be shut down
			}
			else
			{
				findResources(); // Find resources for the first job in the queue
				log.debug("Finished checking the queue, now checking for idle servers...");
				//checkIdleServers(); // Check if some servers can be shut down
			}
		}
		// Backfill first fit algorithm
		else if (scheduling == 1)
		{
			if(jobList.isEmpty())
			{
				log.debug("Queue empty.");
				//checkIdleServers(); // Check if some servers can be shut down
			}
			else
			{
				findResources(); // Find resources for the first job in the queue
				
				// Do backfilling if the list is not empty and there is more than one element in the queue
				if(!jobList.isEmpty() && jobList.size() > 1)
				{
					log.debug("Executing backfill first fit..");					
					int index = 1; // Index starts from 1, since the 1st element in the queue is 0
					
					do
					{
						
						Job temp_job = jobList.get(index); // Pop a job from the queue
						boolean resourcesFound = true;
						int nodes;
						nodes = temp_job.getNumberOfNodes().getValue();
						int[] serverNumber = new int[nodes];
						
						// Search for appropriate servers
						searchServers(nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), serverNumber);
						
						// Check if the servers were found
						resourcesFound = checkServerNumber(serverNumber, resourcesFound, nodes);
						
						if (resourcesFound == false)
						{
							// No servers found, continue the search
							log.debug("No servers were found for the job index: " +index);
						}
						else
						{
							log.debug("Servers were found for the job index: " +index);
							
							// Get the estimated completion time of the new job
							long compTime = getEpochTime() + temp_job.getWallTime().getValue();
													
							// Get the estimated start time of the 1st job in the queue
							long startTime;
							startTime = getEstimatedStartTime();
							log.debug("Estimated start time of the 1st job: " +startTime);
							log.debug("Completion time of the new job: " + compTime);
							
							// Check if the job can be executed before the 1st job
							//if (startTime > compTime)
						//	{
								log.debug("The job can be executed before the 1st element in the queue.");
								temp_job.getTimeOfStart().setValue(getEpochTime());
								
								//boolean generateStartJob = true;
								// Go through the selected servers and check if there are servers
								// that are not powered ON. If all the servers are not ON, just generate
								// a POWER_ON action to the OFF servers. Start Job action should be generated
								// when all servers are available.
								
//								for(int i = 0; i < nodes; i++)
//								{
//									Server server = srvList.get(serverNumber[i]);
//									if (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED"))
//									{
//										generateStartJob = false;
//									}
//								}
																								
								// Generate actions that the server needs to do
								for(int i = 0; i < nodes; i++)
								{
									Server server = srvList.get(serverNumber[i]);
									if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
										server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
									}
									else {
										IDREFS jobRef = new IDREFS();						
										server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
										server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
									}
							
									server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
									serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
									serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));
									
									log.debug("Startup server: " + serverNumber[i]);
									createActionRequest("START_JOB", serverNumber[i], temp_job.getId());


								}
//								for(int i = 0; i < nodes; i++)
//								{
//									Server server = srvList.get(serverNumber[i]);
//									if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
//										server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
//									}
//									else {
//										IDREFS jobRef = new IDREFS();						
//										server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
//										server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
//									}
//									//server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
//									if(server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("RUNNING"))
//									{								
//										server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
//										serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
//										serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));
//										if(generateStartJob == true)
//										{
//											log.debug("Startup server: " + serverNumber[i]);
//											createActionRequest("START_JOB", serverNumber[i], temp_job.getId());
//										}
//										
//									}
//									else if ((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED")))
//									{						
//										log.debug("POWER_ON server: " + serverNumber[i]);
//										//reservedList.set(serverNumber[i], true);
//										serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
//										serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));
//										//server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RESERVED"));
//										createActionRequest("POWER_ON", serverNumber[i], "");
//									}
//
//								}
								jobList.remove(index); // remove the job from the temporary queue
								
								// Include the job into the running jobs list
								temp_job.setStatus(JobStatus.valueOf("RUNNING"));
								enqueue(temp_job);
								
								// Decrement the index, to continue to the next element in the queue
								index--;
						//	}
						//	else
						//	{
							//	log.debug("The job cannot be executed before the 1st element.");
						//	}
							
						}
						
						// increment the index
						index++;
						
						// Continue backfill first fit until the list becomes empty, 
						// end of the queue is reached, or there are no resources available
					} while(!jobList.isEmpty() && index != jobList.size() && isResourcesAvailable() == true);
				}
				log.debug("Backfill first fit finished.");				
				//checkIdleServers(); // Check if some servers can be shut down
			}
			
		}
		// Backfill Best Fit algorithm
		else if (scheduling == 2)
		{
			if(jobList.isEmpty())
			{
				log.debug("Queue empty.");
				//checkIdleServers(); // Check if some servers can be shut down
			}
			else
			{			
				// Find resources for the 1st job in the queue
				findResources();
				
				// Continue back fill best fit if the list is not empty 
				// and there is more than one element in the queue
				if(!jobList.isEmpty() && jobList.size() > 1)
				{
					log.debug("Executing backfill best fit...");
					
					// Go  through the queue:
					
					// 1. Check if the job can be executed with the free resources
					// - what resources are needed
					// - can  the job be executed before the 1st job in the queue
					
					// 2. Check what resources are left free after the job is allocated
					// - how many nodes, cores, memory are left free
					
					// 3. Go through the queue and compare "spare parts"
					
					// 4. Select the job that uses most of the free resources
					
					// 5. repeat parts 1-4 until no suitable job is found or the queue runs empty.
					
					boolean stopSearch;
					boolean jobFound;
					int index;
					int freeNodes;
					double freeMemory;
					int freeCores;
					
					do
					{						
						jobFound = false;
						index = 1;
						
						// Add info about the optimal job
						OptimalJob OptJob = new OptimalJob();
						
						do
						{
							freeNodes = freeCores = 0;
							freeMemory = 0;
														
							Job temp_job = jobList.get(index); // Pop a job from queue
							boolean resourcesFound = true;
							int nodes;
							nodes = temp_job.getNumberOfNodes().getValue();
							int[] serverNumber = new int[nodes];
							
							// Search for appropriate servers
							searchServers(nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), serverNumber);
							
							// Check if the servers were found
							resourcesFound = checkServerNumber(serverNumber, resourcesFound, nodes);
							
							if (resourcesFound == false)
							{
								// No servers found, continue the search
								log.debug("No servers were found for the job index: " +index);
							}
							else
							{
								log.debug("Servers were found for the job index: " +index);
								
								// Get the estimated completion time of the new job
								long compTime = getEpochTime() + temp_job.getWallTime().getValue();
										
								// Get the estimated start time of the 1st job in the queue
								long startTime;
								startTime = getEstimatedStartTime();
								log.debug("Estimated start time of the 1st job: " + startTime);
								log.debug("Completion time of the new job: " + compTime);
								
								// Check if the job can be executed
								//if (startTime > compTime)
								//{
									log.debug("The job can be executed before the 1st element in the queue.");
									jobFound = true;
																										
									// Calculate what resources are left free if the job is executed
									freeNodes = getFreeNodes(nodes, temp_job.getNeededCoresPerNode().getValue());
									freeMemory = getFreeMemory(temp_job.getNeededMemory().getValue(), nodes);
									freeCores = getFreeCores(temp_job.getNeededCoresPerNode().getValue(), nodes);
									
									// Set this job as the optimal job if it has less free resources than the optimal job
//									if(freeNodes < OptJob.getFreeNodes() || OptJob.getFreeNodes() == -1)
//									{
//										log.debug("The job is selected as the optimal job. Nodes.");
//										OptJob.setJobIndex(index);
//										OptJob.setFreeNodes(freeNodes);
//										OptJob.setFreeMemory(freeMemory);
//										OptJob.setFreeCores(freeCores);
//										addOptimalJob(OptJob, nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), temp_job.getWallTime().getValue(), serverNumber);
//									}
//									else if(freeNodes == OptJob.getFreeNodes())
//									{
										if(freeCores < OptJob.getFreeCores() || OptJob.getFreeCores() == -1)
										{
											log.debug("The job is selected as the optimal job. Cores.");
											OptJob.setJobIndex(index);
											OptJob.setFreeNodes(freeNodes);
											OptJob.setFreeMemory(freeMemory);
											OptJob.setFreeCores(freeCores);
											// Add optimal job
											addOptimalJob(OptJob, nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), temp_job.getWallTime().getValue(), serverNumber);
										}
										else if(freeCores == OptJob.getFreeCores() )
										{
											if(freeMemory < OptJob.getFreeMemory() || OptJob.getFreeMemory() == -1)
											{
												log.debug("The job is selected as the optimal job. Memory.");
												OptJob.setJobIndex(index);
												OptJob.setFreeNodes(freeNodes);
												OptJob.setFreeMemory(freeMemory);
												OptJob.setFreeCores(freeCores);
												// Add optimal job
												addOptimalJob(OptJob, nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), temp_job.getWallTime().getValue(), serverNumber);
											}
										}
									//}
								//}							
								// Else: the job cannot be executed
								//else
								//{
								//	log.debug("The job cannot be executed before the 1st element.");
								//}							
							}
							// Increment the index
							index++;
							
							// Continue until the queue becomes empty or end of the queue is reached
						} while(!jobList.isEmpty() && index != jobList.size());
						
						// If a job was found, add it into running jobs list, and generate needed actions
						if(jobFound == true)
						{
							stopSearch = false;	
							
							// Include the optimal job into the running jobs list
							Job newJob = new Job();
							newJob.setStatus(JobStatus.valueOf("RUNNING"));
							
							JobTime newStart = new JobTime ();
							newStart.setValue(getEpochTime());
							newJob.setTimeOfStart(newStart);
							
							JobPriority newPriority = new JobPriority ();
							newPriority.setValue(0);							
							newJob.setPriority(newPriority);
														
							NrOfCores newCores = new NrOfCores ();
							newCores.setValue(OptJob.getCores());
							newJob.setNeededCoresPerNode(newCores);
							
							MemoryUsage newMemory = new MemoryUsage ();
							newMemory.setValue(OptJob.getMemory());
							newJob.setNeededMemory(newMemory);
							
							NrOfNodes newNodes = new NrOfNodes ();
							newNodes.setValue(OptJob.getNodes());
							newJob.setNumberOfNodes(newNodes);
							
							JobTime newWall = new JobTime ();
							newWall.setValue(OptJob.getWallTime());
							newJob.setWallTime(newWall);
							
							newJob.setId(jobList.get(OptJob.getJobIndex()).getId());
							enqueue(newJob);
							
							//boolean generateStartJob = true;
							// Go through the selected servers and check if there are servers
							// that are not powered ON. If all the servers are not ON, just generate
							// a POWER_ON action to the OFF servers. Start Job action should be generated
							// when all servers are available.
							
//							for(int i = 0; i < OptJob.getNodes(); i++)
//							{
//								Server server = srvList.get(OptJob.getServerNumber(i));
//								if (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY"))
//								{
//									generateStartJob = false;
//								}
//							}
							
							// Generate actions that the server needs to do
							for(int i = 0; i < OptJob.getNodes(); i++)
							{
								Server server = srvList.get(OptJob.getServerNumber(i));
								if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
									server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(newJob);
								}
								else {
									IDREFS jobRef = new IDREFS();						
									server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
									server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(newJob);
								}

								server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
								serverCoresInUseList.set(OptJob.getServerNumber(i), (serverCoresInUseList.get(OptJob.getServerNumber(i)) + newJob.getNeededCoresPerNode().getValue()));
								serverMemoryList.set(OptJob.getServerNumber(i), (serverMemoryList.get(OptJob.getServerNumber(i)) - newJob.getNeededMemory().getValue()));
								log.debug("Startup server: " + OptJob.getServerNumber(i));
								createActionRequest("START_JOB", OptJob.getServerNumber(i), newJob.getId());

							}

							// Generate actions that the server needs to do
//							for(int i = 0; i < OptJob.getNodes(); i++)
//							{
//								Server server = srvList.get(OptJob.getServerNumber(i));
//								if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
//									server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(newJob);
//								}
//								else {
//									IDREFS jobRef = new IDREFS();						
//									server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
//									server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(newJob);
//								}
//								//server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(newJob);
//								if(server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("RUNNING"))
//								{
//									server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
//									serverCoresInUseList.set(OptJob.getServerNumber(i), (serverCoresInUseList.get(OptJob.getServerNumber(i)) + newJob.getNeededCoresPerNode().getValue()));
//									serverMemoryList.set(OptJob.getServerNumber(i), (serverMemoryList.get(OptJob.getServerNumber(i)) - newJob.getNeededMemory().getValue()));
//									if(generateStartJob == true)
//									{
//										log.debug("Startup server: " + OptJob.getServerNumber(i));
//										createActionRequest("START_JOB", OptJob.getServerNumber(i), newJob.getId());
//										
//									}
//								}
//								else if (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED"))
//								{						
//									log.debug("POWER_ON server: " + OptJob.getServerNumber(i));
//									//reservedList.set(OptJob.getServerNumber(i), true);
//									serverCoresInUseList.set(OptJob.getServerNumber(i), (serverCoresInUseList.get(OptJob.getServerNumber(i)) + newJob.getNeededCoresPerNode().getValue()));
//									serverMemoryList.set(OptJob.getServerNumber(i), (serverMemoryList.get(OptJob.getServerNumber(i)) - newJob.getNeededMemory().getValue()));
//									//server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RESERVED"));
//									createActionRequest("POWER_ON", OptJob.getServerNumber(i), "");
//								}	
//							}
							jobList.remove(OptJob.getJobIndex()); // remove the optimal job from the temporary queue						
						}
						// Else: No job was found, stop the search for jobs
						else
						{
							stopSearch = true;
						}
						
						// Continue until no job is found and there are no resources available
					} while(stopSearch == false && isResourcesAvailable() == true  && jobList.size() > 1);
					
					log.debug("Backfill best fit finished.");				
					//checkIdleServers(); // Check if some servers can be shut down
				}
			}			
		}		
	}
	
	/**
	 * Method for checking if the 1st element in the queue 
	 * can be executed with the current nodes
	 * 
	 */
	
	public void findResources()
	{
		// Extract jobs from the queue until the queue becomes empty
		// or no suitable servers are found for the 1st job in the queue
		boolean resourcesFound;
		do 
		{
			log.debug("Popping a job from queue.");
			Job temp_job = jobList.get(0);
			resourcesFound = true;
			int nodes;
			nodes = temp_job.getNumberOfNodes().getValue();
			int[] serverNumber = new int[nodes];
						
			// Search for appropriate servers
			searchServers(nodes, temp_job.getNeededMemory().getValue(), temp_job.getNeededCoresPerNode().getValue(), serverNumber);
						
			// Check if the servers were found
			resourcesFound = checkServerNumber(serverNumber, resourcesFound, nodes);
			
			if(resourcesFound == false)
			{
				log.debug("Suitable servers were not found.");
			}
			else
			{
				log.debug("Suitable servers were found.");
				temp_job.getTimeOfStart().setValue(getEpochTime());
				//boolean generateStartJob = true;
				// Go through the selected servers and check if there are servers
				// that are not powered ON. If all the servers are not ON, just generate
				// a POWER_ON action to the OFF servers. Start Job action should be generated
				// when all servers are available.
				
//				for(int i = 0; i < nodes; i++)
//				{
//					Server server = srvList.get(serverNumber[i]);
//					if (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED"))
//					{
//						generateStartJob = false;
//					}
//				}
				
				// Generate actions that the server needs to do
				for(int i = 0; i < nodes; i++)
				{
					Server server = srvList.get(serverNumber[i]);
					if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
						server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
					}
					else {
						IDREFS jobRef = new IDREFS();						
						server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
						server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
					}
					
					server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
					serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
					serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));

					log.debug("Startup server: " + serverNumber[i]);
					createActionRequest("START_JOB", serverNumber[i], temp_job.getId());
				}
				
//				// Generate actions that the server needs to do
//				for(int i = 0; i < nodes; i++)
//				{
//					Server server = srvList.get(serverNumber[i]);
//					if(server.getNativeOperatingSystem().getNode().get(0).getJobRef() != null){
//						server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
//					}
//					else {
//						IDREFS jobRef = new IDREFS();						
//						server.getNativeOperatingSystem().getNode().get(0).setJobRef(jobRef);
//						server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().add(temp_job);
//					}
//
//					if(server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("RUNNING"))
//					{						
//						server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RUNNING"));
//						serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
//						serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));
//						if(generateStartJob == true)
//						{
//							log.debug("Startup server: " + serverNumber[i]);
//							createActionRequest("START_JOB", serverNumber[i], temp_job.getId());
//							
//						}
//						else
//						{
//							log.debug("Not generating start job: " +serverNumber[i]);
//						}
//					}
//					else if (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY") || server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED"))
//					{						
//						log.debug("POWER_ON server: " + serverNumber[i]);
//						//reservedList.set(serverNumber[i], true);
//						serverCoresInUseList.set(serverNumber[i], (serverCoresInUseList.get(serverNumber[i]) + temp_job.getNeededCoresPerNode().getValue()));
//						serverMemoryList.set(serverNumber[i], (serverMemoryList.get(serverNumber[i]) - temp_job.getNeededMemory().getValue()));
//						//server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("RESERVED"));
//						createActionRequest("POWER_ON", serverNumber[i], "");
//					}	
//				}

				// Include the job into the running jobs list
				temp_job.setStatus(JobStatus.valueOf("RUNNING"));
				enqueue(temp_job);
				
				jobList.remove(0); // remove the job from the temporary queue
				
			}			
		} while (jobList.isEmpty() == false && resourcesFound == true);
	}
	
	/**
	 * Method for adding info about a optimal job
	 * 
	 */
	public void addOptimalJob(OptimalJob OptJob, int nodes, double memory, int cores, long wallTime, int [] serverNumber)
	{		
		OptJob.clearServerNumber();		
		OptJob.setNodes(nodes);
		OptJob.setMemory(memory);
		OptJob.setCores(cores);
		OptJob.setWallTime(wallTime);
		
		for(int i = 0; i < nodes; i++)
		{
			OptJob.addServerNumber(serverNumber[i]);
		}		
	}
	
	/**
	 * Creates a set of action requests, to be forwarded to the Controller.
	 * 
	 * @param actionType, serverIndex
	 * @return none
	 */
	public void createActionRequest(String actionType, int serverIndex, String jobID){
		
		// Get the name corresponding to the serverIndex
		Server server = srvList.get(serverIndex);
		String serverName = server.getFrameworkID();
						
		//String myQuery = "//rackableServer";	
		//JXPathContext context = JXPathContext.newContext(model);
		//Iterator elements = context.iterate(myQuery);
		
				
		for(Datacenter datacenter : model.getSite().get(0).getDatacenter())
		{
			log.debug("Framework name: " + datacenter.getFrameworkCapabilities().get(0).getFrameworkName());
			for(Server tempServer : datacenter.getRack().get(0).getRackableServer())
			{
				if(tempServer.getFrameworkID().matches(serverName))
	        	{ 
					log.debug("Adding an action to server ID: " + tempServer.getFrameworkID());
					if(actionType.equals("POWER_ON"))
	        		{       		
		        		log.debug("Adding power on action");
		        		
		    			JAXBElement<PowerOnAction>  pOn = (new ObjectFactory()).createPowerOn(new PowerOnAction());
		    			pOn.getValue().setNodeName(tempServer.getFrameworkID());
		    			
		    			//Datacenter datacenter = Utils.getFirstDatacenter(model);
		    			//String frameworkName = datacenter.getFrameworkCapabilities().get(0).getFrameworkName();	
		    			FrameworkCapabilities framework = (FrameworkCapabilities) tempServer.getFrameworkRef();   			
		    			String frameworkName = framework.getFrameworkName();
		    			
		    			pOn.getValue().setFrameworkName(frameworkName);  			
		    			actionList.getAction().add(pOn);

		    			
	        		}
	        		else if(actionType.equals("STANDBY"))
	        		{
	        			log.debug("Adding a standby action");
	        				        			
	        			JAXBElement<StandByAction>  stdBy = (new ObjectFactory()).createStandBy(new StandByAction());
	        			stdBy.getValue().setNodeName(tempServer.getFrameworkID());
		    			
		    			FrameworkCapabilities framework = (FrameworkCapabilities) tempServer.getFrameworkRef();   			
		    			String frameworkName = framework.getFrameworkName();
		    			
		    			stdBy.getValue().setFrameworkName(frameworkName);			
		    			actionList.getAction().add(stdBy);
	        		} 
	        		else if(actionType.equals("POWER_OFF"))
	        		{
	        			log.debug("Adding a power off action");
	        			
	        			JAXBElement<PowerOffAction>  pOff = (new ObjectFactory()).createPowerOff(new PowerOffAction());
		    			pOff.getValue().setNodeName(tempServer.getFrameworkID());
		    			
		    			FrameworkCapabilities framework = (FrameworkCapabilities) tempServer.getFrameworkRef();   			
		    			String frameworkName = framework.getFrameworkName();
		    			
		    			pOff.getValue().setFrameworkName(frameworkName);			
		    			actionList.getAction().add(pOff);
	        		}        		
	        		else if(actionType.equals("START_JOB"))
	        		{
	        				        			
	        			// If there is already an action containing this jobId
	        			if(jobIdList.contains(jobID))
	        			{
	        				log.debug("JobIdList has the jobId." + actionRequest.getActionList().getAction().size());
		        			for(int i = 0; i < actionRequest.getActionList().getAction().size(); i++)
		        			{
		        				if(actionRequest.getActionList().getAction().get(i).getDeclaredType().getSimpleName().toString().matches("StartJobAction"))
		        				{
		        					Object obj = actionRequest.getActionList().getAction().get(i).getValue();
		        					String actionJobId = ((StartJobAction)obj).getJobID();	    
		        					if(jobID.matches(actionJobId))
		        					{
		        						// Add the server to the Start Job action
		        						((StartJobAction)obj).getNodeName().add(tempServer.getFrameworkID());
		        					}
		        				}
		        			}
	        			}
	        			// Else: create a new start job action
	        			else
	        			{
	        				log.debug("Adding jobId to jobIdList.");
	        				jobIdList.add(jobID);
	        				JAXBElement<StartJobAction>  startJob = (new ObjectFactory()).createStartJob(new StartJobAction());
	        				startJob.getValue().getNodeName().add(tempServer.getFrameworkID());
	        				startJob.getValue().setJobID(jobID);
	        				
	    	    			//Datacenter datacenter = Utils.getFirstDatacenter(model);
	    	    			//String frameworkName = datacenter.getFrameworkCapabilities().get(0).getFrameworkName();
	    	    			FrameworkCapabilities framework = (FrameworkCapabilities) tempServer.getFrameworkRef();   			
	    	    			String frameworkName = framework.getFrameworkName();	
	    	    			startJob.getValue().setFrameworkName(frameworkName);			
	    	    			actionList.getAction().add(startJob);
	        			}      			     			      			
	        		}   
	        	}
			}
			
		}
		
		// Add the actionList to the actionRequest
		actionRequest.setActionList(actionList);
		
	}
		
	/**
	 * Method for checking the queue 
	 * if there are any idle servers
	 * and can they be shut down
	 *
	 */	
	
	public void checkIdleServers(Datacenter datacenter)
	{		
		boolean shutDown;
		long startTime = 0;
		boolean idleServers  = false;
		
		if(poweroff == true)
		{
		
			log.debug("Checking for idle servers..");
			
			// Go through the serverList and check if there are any idle servers
			for(int i = 0; i < srvList.size(); i++)
			{
				if(srvList.get(i).getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE"))
				{
					idleServers = true;
						break;
				}
			}
								
			if(idleServers == true)
			{
				
				if(jobList.isEmpty())
				{
					shutDown = true;
				}
				else
				{
					// Get the estimated start time of the 1st job in the queue
					startTime = getEstimatedStartTime() - getEpochTime();	// seconds between current time and estimated start time
					log.debug("start Time : "+ startTime);
					log.debug("start date: " +getEpochDate((startTime+getEpochTime())));
					log.debug("threshold : "+ threshold);
					
					// Generate a stdby/suspend action if the startTime is larger than threshold
					if((startTime) < threshold)
					{
						shutDown = false;
					}
					else
					{
						shutDown =  true;
					}
					
				}
										
											
				for (int i = 0; i < srvList.size(); i++)
				{
					Server server = srvList.get(i);
					if(server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE"))
					{				
						if(shutDown == true)
						{
//							if(datacenter.getFrameworkCapabilities().get(0).getNode().isStandBy())
//							{
//								log.debug("Standby is supported.");
//							}
//							else if(datacenter.getFrameworkCapabilities().get(0).getNode().isPowerOff())
//							{
//								log.debug("Power Off is supported.");
//							}
							createActionRequest("STANDBY", i, "0");
							server.getNativeOperatingSystem().getNode().get(0).setStatus(NodeStatus.valueOf("STANDBY"));
							log.debug("Setting server to standby: " + i);
						}					
					}
				}				
			}
		}
	}
	
	/**
	 * Method for checking the queue 
	 * if there are available resources
	 * 
	 */	
	
	public boolean isResourcesAvailable()
	{		
		boolean available = false;
		for (int i = 0; i < srvList.size(); i++)
		{
			Server server = srvList.get(i);
			if((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED")))
			{				
				available = true;
				break;
			}
			else if(server.getNativeOperatingSystem().getNode().get(0).getCoresInUse().getValue() < serverCoresList.get(i))
			{
				available = true;
				break;
			}
		}		
		return available;
	}
	
	public int getFreeNodes(int nodes, int cores)
	{		
		int freeNodes = 0;
		//int coresAvailable = 0;

//		for (int i = 0; i < srvList.size(); i++)
//		{
//			//Server server = srvList.get(i);
//			// If the server has free cores it is considered free
//			if((serverCoresInUseList.get(i) < serverCoresList.get(i)))
//			{
//				coresAvailable = (serverCoresList.get(i) - serverCoresInUseList.get(i));
//				// If the number of free cores is above zero, then a node is considered free
//				// if this job would be allocated
//				if((coresAvailable - cores) > 0)
//					freeNodes++;
//			}
//
////			if((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED")))
////			{
////				freeNodes++;
////
////			}
//		}
		freeNodes = srvList.size() - nodes;
		log.debug("Amount of Free nodes: " +freeNodes);
		return freeNodes;
	}
	
	public int getFreeCores(int cores, int nodes)
	{
		int freeCores = 0;
		// Free cores: NumberOfCores - CoresInUse
		for (int i = 0; i < srvList.size(); i++)
		{
			//Server server = srvList.get(i);
			freeCores = freeCores + (serverCoresList.get(i) - serverCoresInUseList.get(i));
			
//			if((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED")))
//			{
//				freeCores = freeCores + serverCoresList.get(i); ;
//
//			}
		}
		log.debug("Amount of Free cores before job: " +freeCores);
		log.debug("Amount of needed cores for job: " +cores);
		log.debug("Amount of needed nodes for job: " +nodes);
		freeCores = freeCores - (cores * nodes);
		log.debug("Amount of Free cores after job: " +freeCores);
		return freeCores;			
	}
	
	public double getFreeMemory(double memory, int nodes)
	{
		double freeMemory = 0;
		// Check what resources are currently free
		for (int i = 0; i < srvList.size(); i++)
		{
			//Server server = srvList.get(i);
			if((serverCoresInUseList.get(i)< serverCoresList.get(i)))
			{
				freeMemory = freeMemory + serverMemoryList.get(i);
			}
//			if((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED")))
//			{
//				freeMemory = freeMemory + serverMemoryList.get(i);
//
//			}
		}
		freeMemory = freeMemory - (memory * nodes);
		return freeMemory;				
	}
	
//	public int [] calculateFreeResources(int nodes, long memory, int cores, int [] resourceArray)
//	{
//		// Check what resources are currently free
//		for (int i = 0; i < srvList.size(); i++)
//		{
//			Server server = srvList.get(i);
//			if((server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("OFF")) || (server.getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY")))
//			{
//				resourceArray[0] = resourceArray[0] + 1;
//				resourceArray[1] = resourceArray[1] + serverMemoryList.get(i);
//				resourceArray[2] = resourceArray[2] + serverCoresList.get(i); 
//			}
//		}
//		
//		// Check what resources would be free after the job allocation
//		resourceArray[0] = resourceArray[0] - nodes;
//		resourceArray[1] = resourceArray[1] - (memory * nodes);
//		resourceArray[2] = resourceArray[2] - (cores * nodes);
//				
//		return resourceArray;
//		
//	}
	
	
	/**
	 * Gets the estimated start time of 
	 * the first job in the queue
	 * @param none
	 * @return estimated start time of the 1st job in queue
	 * 
	 */	
	
	public long getEstimatedStartTime()
	{	
		Job job = jobList.get(0); // Get the 1st job in the queue
		int numNodes = job.getNumberOfNodes().getValue();
		int numCores = job.getNeededCoresPerNode().getValue();
		int availableNodes = 0;
		int availableCores = 0;
		long startTime = 0;				
		Vector<Long> compTime = new Vector<Long> ();
		Vector<Long> serverCompTime = new Vector<Long> ();
		long serverStartTime = 0;
		List<Job> tempJobList = new LinkedList<Job>(); // List of jobs in the queue
		
		//1. check for idle servers
		for(int i = 0; i < srvList.size(); i++)
		{
			if(srvList.get(i).getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("IDLE"))
			{
				compTime.add((long) 0); // since the server is idle, it is immediately available
				availableNodes++;
				if(availableNodes == numNodes)
					break;
			}		
		}
		//2. check for servers in standby mode
		//TODO: standby/hybernated servers could have a jobRef associated
		if(availableNodes < numNodes)
		{
			for(int i = 0; i < srvList.size(); i++)
			{
				if(srvList.get(i).getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("STANDBY") && reservedList.get(i) == false)
				{
					compTime.add((long) 10); // since the server is in standby, it will be available in 5s
					availableNodes++;
					if(availableNodes == numNodes)
						break;
				}		
			}
			//3. check servers in hybernation
			if(availableNodes < numNodes)
			{
				for(int i = 0; i < srvList.size(); i++)
				{
					if(srvList.get(i).getNativeOperatingSystem().getNode().get(0).getStatus().toString().matches("HYBERNATED") && reservedList.get(i) == false)
					{
						compTime.add((long) 90); // since the server is hybernated, it will be available in 90s
						availableNodes++;
						if(availableNodes == numNodes)
							break;
					}		
				}
			}
		}
		
		
		log.debug("Idle/sleeping/hybernated servers: " +availableNodes);
		double time = 0.0;
		for (int i = 0; i < compTime.size(); i++)
		{
			time = time + compTime.get(i);
		}
		log.debug("startUp time for Idle/sleeping/hybernated servers: " +time);
		log.debug("Needed nodes for the first job in queue: " + numNodes);
		
		//4. check running jobs
		if(availableNodes < numNodes)
		{			
			//Go through each running server, and define when it will be ready
			//for the job execution. Finally get the servers that have the smallest
			//comptime and select them
			
			// Go through the servers that are running a job
			for(int i = 0; i < srvList.size(); i++)
			{				

				Server server = srvList.get(i);
				NodeStatus serverStatus = server.getNativeOperatingSystem().getNode().get(0).getStatus();
				serverStartTime = 0;
				log.debug("Server name: " +server.getFrameworkID() + " status: " +serverStatus.toString());
				
				if(serverStatus.toString().matches("RUNNING") || serverStatus.toString().matches("RESERVED"))
				{
					// Go through the jobs that the server is running
					availableCores = 0;
					availableCores = serverCoresList.get(i) - serverCoresInUseList.get(i);
					log.debug("Need cores: " +numCores);
					log.debug("availableCores without jobs: " +availableCores);
					
					if(availableCores >= numCores)
					{
						if(serverStatus.toString().matches("RUNNING"))
							serverCompTime.add((long) 0); // since there are enough cores, it is immediately available
						else if(serverStatus.toString().matches("RESERVED")) //TODO: redundant?
							serverCompTime.add((long) 10); 
						
					}
					
					else
					{
															
						// Go through the jobs that the server holds and form a temporary job list
						// which is sorted according to the completion times of the jobs
						Iterator<Object> jobIter = server.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().iterator();
						while(jobIter.hasNext())
						{						
							//Get the jobs to a temporary list
							Job serverJob =  (Job) jobIter.next();	
							boolean jobInserted = false;
							double serverJobCompTime = 0;
							
							if(serverStatus.toString().matches("RUNNING"))
								serverJobCompTime = serverJob.getTimeOfStart().getValue() + serverJob.getWallTime().getValue();
							else if(serverStatus.toString().matches("RESERVED"))
								serverJobCompTime = serverJob.getTimeOfStart().getValue() + serverJob.getWallTime().getValue() + 10;
								
							
							// If the list is empty, just place it at the end
							if(tempJobList.isEmpty())
							{
								tempJobList.add(serverJob);
							}	
							else
							{
								// Form the list based on the completion time values,
								// smallest completion times going into the top
								for(int j = 0; j < tempJobList.size(); j++)
								{
									Job tempJob = tempJobList.get(j);
									double tempCompTime = tempJob.getTimeOfStart().getValue() + tempJob.getWallTime().getValue();
									if(serverJobCompTime < tempCompTime)
									{
										tempJobList.add(j, serverJob);
										jobInserted = true;
										break;
									}				
								}
								if(jobInserted == false)
								{
									tempJobList.add(serverJob);
								}		
							}
									
						}
						
						for(int k = 0; k < tempJobList.size(); k++)
						{
							Job tempJob = tempJobList.get(k);
							log.debug("tempJob index: " + i + " cores: " + tempJob.getNeededCoresPerNode() + " comptime: " + (tempJob.getTimeOfStart().getValue()+tempJob.getWallTime().getValue()));

						}
																		
						// Go through the temporary job list and add the number of cores the job holds
						// to the amount of available cores
						for(int k = 0; k < tempJobList.size(); k++)
						{
							Job tempJob = tempJobList.get(k);
							availableCores = availableCores + tempJob.getNeededCoresPerNode().getValue();
							serverStartTime = serverStartTime + ((tempJob.getTimeOfStart().getValue()+tempJob.getWallTime().getValue()) - serverStartTime);
							
							// If there are enough cores available the node is considered free
							// at this moment in time
							if(availableCores  >= numCores)
							{
								serverCompTime.add(serverStartTime);
								tempJobList.clear();
								break;
							}
						}						
					}
				}
			}
			
			
			//Sort the server comptime list with smallest values first
			// and count the number of available nodes
			// add the values to the comptime vector
								
			Collections.sort(serverCompTime);
			
			for (int i = 0; i < serverCompTime.size(); i++)
			{
				log.debug("serverCompTime index: " + i + " Value: " + serverCompTime.get(i));
				compTime.add(serverCompTime.get(i));
				availableNodes++;
				if(availableNodes == numNodes)
					break;
			}					
			
		}
						
		for (int i = 0; i < compTime.size(); i++)
		{
			log.debug("CompTime index: " + i + " Value: " + compTime.get(i));
			startTime = startTime + (compTime.get(i) - startTime);
		}
		
		//log.debug("Needed nodes for the first job in queue: " + numNodes);
		//log.debug("Estimated start Time: " + startTime);
		
		compTime.clear();
		serverCompTime.clear();
		return startTime;
	}
	
	/**
	 * Gets the current system time in seconds (epoch time). 
	 */
	
	public long getEpochTime()
	{
		long epoch = System.currentTimeMillis()/1000;	
		return epoch;
	}
	
	/**
	 * Converts an epoch time to a date format.
	 */
	
	public Date getEpochDate(long epoch)
	{
	    Date epochDate = new Date(epoch*1000);
	    return epochDate;
	}
	
	/**
	 * Checks if actionList is empty.
	 */
	
	public boolean checkActionList()
	{
		boolean empty;
		try
		{
			actionRequest.getActionList().getAction().size();
			log.debug("Size of action list: " +actionRequest.getActionList().getAction().size());
			if(actionRequest.getActionList().getAction().size() == 0)
				empty = true;
			else
				empty = false;
			
		}
		catch (NullPointerException e)
		{
			empty = true;
		}
		return empty;
	}
		
	/**
	 * Utility Method for printing the contents of  the 
	 * queue 
	 * 
	 */
	
	public void printQueue()
	{
		for(int i = 0; i < jobList.size(); i++)
		{
			Job tempjob = jobList.get(i);
			log.debug("JobList index: "+ i + " priority: "+ tempjob.getPriority().getValue() + " numNodes: " + tempjob.getNumberOfNodes().getValue() + " numCores: " +tempjob.getNeededCoresPerNode().getValue() + " memory " +tempjob.getNeededMemory().getValue() + " wallTime: " +tempjob.getWallTime().getValue() + " compTime: " +(tempjob.getTimeOfStart().getValue()+ tempjob.getWallTime().getValue()));
		}
		System.out.println("");
		for(int i = 0; i < runningList.size(); i++)
		{
			Job tempjob = runningList.get(i);
			log.debug("Running Job index: "+ i + " priority: "+ tempjob.getPriority().getValue() + " numNodes: " + tempjob.getNumberOfNodes().getValue() + " numCores: " +tempjob.getNeededCoresPerNode().getValue() + " memory " +tempjob.getNeededMemory().getValue() + " wallTime: " +tempjob.getWallTime().getValue() + " jobID: " +tempjob.getId());			
		}
	}
	
	/**
	 * Utility Method for printing the contents of  the 
	 * ServerData structure
	 * 
	 */
	
	public void printServerList()
	{
		for(int i = 0; i < srvList.size(); i++)
		{
			Server tempServer = srvList.get(i);
			String state = tempServer.getNativeOperatingSystem().getNode().get(0).getStatus().toString();					
			int coresInUse = serverCoresInUseList.get(i);
			double compTime = 0;
			
			log.debug("Server index: "+ i + " state: "+ state + " serverMemory: "+ serverMemoryList.get(i) + " coresInUse: " +coresInUse);
			// Iteration over the Job objects
			try
			{
				Iterator<Object> jobIter = tempServer.getNativeOperatingSystem().getNode().get(0).getJobRef().getValue().iterator();
				while(jobIter.hasNext())
				{
					Job tempJob =  (Job) jobIter.next();
					compTime = tempJob.getTimeOfStart().getValue() + tempJob.getWallTime().getValue();
					log.debug(" job ID: " +tempJob.getId());
					log.debug(" completion time: " +compTime );				
				}
			}
			catch(NullPointerException e)
			{
				log.debug("No jobs with this server.");	
				
			}
		}
	}
	
	/**
	 * Utility Method for printing the contents of  the 
	 * actionList 
	 * 
	 */
	
	public void printActionList()
	{
		for(int i = 0; i < actionRequest.getActionList().getAction().size(); i++)
		{
			if(actionRequest.getActionList().getAction().get(i).getDeclaredType().getSimpleName().toString().matches("PowerOffAction"))
			{
				Object element = actionRequest.getActionList().getAction().get(i).getValue();
				log.debug("ActionList index: " +i +" Type: Power off NodeName: " +((PowerOffAction)element).getNodeName() +" FrameworkName: " +((PowerOffAction)element).getFrameworkName());			
			}
			else if(actionRequest.getActionList().getAction().get(i).getDeclaredType().getSimpleName().toString().matches("PowerOnAction"))
			{
				Object element = actionRequest.getActionList().getAction().get(i).getValue();
				log.debug("ActionList index: " +i +" Type: Power on. NodeName: " +((PowerOnAction)element).getNodeName());
			}
			else if(actionRequest.getActionList().getAction().get(i).getDeclaredType().getSimpleName().toString().matches("StandByAction"))
			{
				Object element = actionRequest.getActionList().getAction().get(i).getValue();
				log.debug("ActionList index: " +i +" Type: Standby. NodeName: " +((StandByAction)element).getNodeName());
			}
			else if(actionRequest.getActionList().getAction().get(i).getDeclaredType().getSimpleName().toString().matches("StartJobAction"))
			{
				Object element = actionRequest.getActionList().getAction().get(i).getValue();	
				log.debug("ActionList index: " +i +" Type: Start job. NodeName: " +((StartJobAction)element).getNodeName() + " Job ID: " + ((StartJobAction)element).getJobID());
			}
		}						
	}
	
	/**
	 * Utility Method for getting the number of CPUs
	 * of a server 
	 * 
	 */
	
	public static int getNbCPU(Server server) {
		int cpus=0;
		for(Mainboard mainboard : server.getMainboard())
			cpus += mainboard.getCPU().size();
		return cpus;
	}
	
	/**
	 * Utility Method for getting the number of Cores
	 * per CPU of a server 
	 * 
	 */
	
	public static int getNbCoresCPU(Server server) {
		int coresPerCPU=0;
		for(Mainboard mainboard : server.getMainboard())
		{
			CPU cpu = mainboard.getCPU().get(0);
			coresPerCPU = cpu.getCore().size();
		}
			
		return coresPerCPU;
	}
	
	
	/**
	 * Get the server in idle state (suppress every loads) 
	 * 
	 */
	public static Server getServerIdle(Server server, IPowerCalculator powerCalculator) {
	    
	    	Server myServer = (Server) server.clone();
	    	    	    
	    	//zeroing the CPU loads, core loads, memory usage and HDD write/read rate
	    	Mainboard mainboard = myServer.getMainboard().get(0);
	    	if(mainboard != null) {
	    		
	    		// Zero the memory usage
	    		mainboard.setMemoryUsage(new MemoryUsage (0.0));	    		
	    		// Zero the CPU loads, core loads
	    		for(CPU cpu : mainboard.getCPU()) {
	    			cpu.setCpuUsage(new CpuUsage(0.0) );
	    			
	    			for(Core core : cpu.getCore()) {
	    				core.setCoreLoad(new CoreLoad(0.0) );
	    			}    			
	    		}  
	    		// Zero the HDD write/read rate
	    		for(HardDisk hdd : mainboard.getHardDisk()) {
	    			hdd.setReadRate(new IoRate(0.0));
	    			hdd.setWriteRate(new IoRate(0.0));
	    		}
	    	}
	    		    		    	
	    	//Zero the FAN actual RPM
	    	if(myServer instanceof RackableServer) {
	    		RackableServer myRackableServer = (RackableServer) myServer;   		
	    		for(Fan fan : myRackableServer.getFan()) {
	    			fan.setActualRPM(new RPM(0));
	    		}
	    	}
	    		    		    	
	    	return myServer; 
	}
	
	/**
	 * Compute the power induced by one Job on a server
	 * 
	 */
    public double computePowerForJob(Server server, Job myJob, IPowerCalculator powerCalculator) {
    	
    	CoreLoad coreLoad = new CoreLoad ();
    	MemoryUsage memUsage = new MemoryUsage();
		memUsage.setValue(myJob.getNeededMemory().getValue());
		
		
		double fanRPMpercentage = 0.0;
		double hddReadRatepercentage = 0.0;
		double hddWriteRatepercentage = 0.0;
					
		log.debug("Simulation flag: " +powerCalculator.getSimulationFlag());
		
		Properties prop = new Properties();
	    try
	    {
	    	InputStream is = this.getClass().getClassLoader().getResourceAsStream("optimizer/OptimizerEngineHPC.properties");
	    	prop.load(is);
	    	fanRPMpercentage = Double.parseDouble(prop.getProperty("fanRPMpercentage").trim());
	    	hddReadRatepercentage = Double.parseDouble(prop.getProperty("hddReadRatepercentage").trim());
	    	hddWriteRatepercentage = Double.parseDouble(prop.getProperty("hddWriteRatepercentage").trim());
	    	log.debug("fanRPMpercentage is set as: " + fanRPMpercentage);
	    	log.debug("hddReadRatepercentage is set as: " + hddReadRatepercentage);
	    	log.debug("hddWriteRatepercentage is set as: " + hddWriteRatepercentage);
	    }
	    catch (Exception e)
	    {
	    	// Some error with handling files
	    	log.debug("Error with handling files...");
	    }
    	   	
    	Mainboard mainboard = server.getMainboard().get(0);
    	if(mainboard != null) {
    		
    		//1. Set memory usage
    		mainboard.setMemoryUsage(memUsage);
    		
    		//2. Set Hard disk read/write rate
    		for(HardDisk hdd : mainboard.getHardDisk()) {
    			hdd.setReadRate(new IoRate(hddReadRatepercentage * hdd.getMaxReadRate().getValue()));
    			hdd.setWriteRate(new IoRate(hddWriteRatepercentage * hdd.getMaxWriteRate().getValue()));
    		}
    	}
				
		//3. Set actual RPM caused by the job
    	if(server instanceof RackableServer) {
    		RackableServer myRackableServer = (RackableServer) server;
    		//log.debug("PSU Load: " +myRackableServer.getPSU().get(0).getLoad().getValue());
    		for(Fan fan : myRackableServer.getFan()) {
    			fan.setActualRPM(new RPM((int)(fanRPMpercentage * fan.getMaxRPM().getValue())));
    		}
    	}
    	    		
		//4. Set core load of the job
		int CPUs = getNbCPU(server);
		int coresPerCPU = getNbCoresCPU(server);
		double jobPower = 0.0;
		Vector<Double> estimatedPower = new Vector<Double> ();		
		double estimateLoad = 0.0;
		
		//double requested_cores = myJob.getNeededCoresPerNode().getValue();
		double total_cores = CPUs * coresPerCPU;
		//double core_per_node_factor = requested_cores / total_cores;
		server.setMeasuredPower(null);
		
		
		//Compute the estimated power consumption with different core loads
		for(int index = 0; index < 10; index++)
		{
			estimateLoad+=10.0;
			log.debug("Estimated load: " +estimateLoad);
			coreLoad.setValue(estimateLoad);
			int cores = myJob.getNeededCoresPerNode().getValue();
			int loadedCores = 0;
					   						        			
			//Set estimated coreLoad to needed cores
			for(int i = 0; i < CPUs; i++)
			{
				if(loadedCores == cores)
					break;
				for(int j = 0; j < coresPerCPU; j++)
				{
    				server.getMainboard().get(0).getCPU().get(i).getCore().get(j).setCoreLoad(coreLoad);
    				//log.debug("Core Load: " +server.getMainboard().get(0).getCPU().get(i).getCore().get(j).getCoreLoad().getValue());
    				
    				loadedCores++;
    				if(loadedCores == cores)
    					break;
				}
			}
			jobPower = powerCalculator.computePowerServer(server).getActualConsumption();
			estimatedPower.add(jobPower);
		}
		double sum = 0.0;
		jobPower = 0.0;
		
		for(int i= 0; i < estimatedPower.size(); i++)
			sum+=estimatedPower.get(i);

		jobPower = sum/estimatedPower.size();		
		log.debug("Average power consumption of a server: " +jobPower);
		//log.debug("Idle power consumption of a server: " +idlePower);
		
		//jobPower *= core_per_node_factor;
		//log.debug("core_per_node_factor: " +core_per_node_factor);
		log.debug("total_cores: " +total_cores);
		log.debug("Average power consumption of a server using " +myJob.getNeededCoresPerNode().getValue() + " cores: " +jobPower);
		
		return jobPower;
    	
    	
    }
    
	/**
	 * Compute the idle power on a server
	 * 
	 */
    public double computeIdlePower(Server server, IPowerCalculator powerCalculator) {
    	
		
		Server tempServer = (Server) server.clone();
		double idlePower = 0.0;
		
		double fanRPMpercentage = 0.0;
		double hddReadRatepercentage = 0.0;
		double hddWriteRatepercentage = 0.0;
					
		log.debug("Simulation flag: " +powerCalculator.getSimulationFlag());
		
		Properties prop = new Properties();
	    try
	    {
	    	InputStream is = this.getClass().getClassLoader().getResourceAsStream("optimizer/OptimizerEngineHPC.properties");
	    	prop.load(is);
	    	fanRPMpercentage = Double.parseDouble(prop.getProperty("fanRPMpercentage").trim());
	    	hddReadRatepercentage = Double.parseDouble(prop.getProperty("hddReadRatepercentage").trim());
	    	hddWriteRatepercentage = Double.parseDouble(prop.getProperty("hddWriteRatepercentage").trim());
	    	log.debug("fanRPMpercentage is set as: " + fanRPMpercentage);
	    	log.debug("hddReadRatepercentage is set as: " + hddReadRatepercentage);
	    	log.debug("hddWriteRatepercentage is set as: " + hddWriteRatepercentage);
	    }
	    catch (Exception e)
	    {
	    	// Some error with handling files
	    	log.debug("Error with handling files...");
	    }
    	   	
//    	Mainboard mainboard = server.getMainboard().get(0);
//    	if(mainboard != null) {
//    		
//    		//1. Set memory usage
//    		mainboard.setMemoryUsage(memUsage);
//    		
//    		//2. Set Hard disk read/write rate
//    		for(HardDisk hdd : mainboard.getHardDisk()) {
//    			hdd.setReadRate(new IoRate(hddReadRatepercentage * hdd.getMaxReadRate().getValue()));
//    			hdd.setWriteRate(new IoRate(hddWriteRatepercentage * hdd.getMaxWriteRate().getValue()));
//    		}
//    	}
				
		//3. Set actual RPM caused by the job
    	if(tempServer  instanceof RackableServer) {
    		RackableServer myRackableServer = (RackableServer) tempServer ;
    		//log.debug("PSU Load: " +myRackableServer.getPSU().get(0).getLoad().getValue());
    		for(Fan fan : myRackableServer.getFan()) {
    			fan.setActualRPM(new RPM((int)(fanRPMpercentage * fan.getMaxRPM().getValue())));
    		}
    	}
    	tempServer.setMeasuredPower(null);				
		idlePower = powerCalculator.computePowerServer(tempServer).getActualConsumption();
		
		return idlePower;
    	    	
    }
    
	/**
	 * Get the estimated wait time of the cluster
	 * 
	 */
    
    public long getEstimatedWaitTime(Datacenter datacenter, Job myJob)
    {
    	long estimatedWaitTime = 0;
    	Datacenter tempDC = (Datacenter) datacenter.clone();
    	long runningJobsRemainingTime = 0;
    	long queuedJobsTime = 0;
    	long totalCores = 0;
    	int queueSize = 0;
    	
		boolean resourcesFound = false;   
		//Check if there are free resources for the job
		resourcesFound = checkDataCenter(tempDC, myJob);
		queueSize = getQueueSize(tempDC);
		
		//If resources are found
		if(resourcesFound == true) {
			//If the queue size is zero, the job can be started immediately
			if(queueSize == 0) {
				estimatedWaitTime = 0;
			}
			//If there are jobs in the queue
			else {
				runningJobsRemainingTime = getRunningJobsRemainingTime(tempDC);
				queuedJobsTime = getQueuedJobsTime(tempDC);
				totalCores = getClusterTotalCores(tempDC);

				estimatedWaitTime = (runningJobsRemainingTime  + queuedJobsTime) / totalCores;		
			}
		}
		//If resources are not found
		else {
			runningJobsRemainingTime = getRunningJobsRemainingTime(tempDC);
			queuedJobsTime = getQueuedJobsTime(tempDC);
			totalCores = getClusterTotalCores(tempDC);

			estimatedWaitTime = (runningJobsRemainingTime  + queuedJobsTime) / totalCores;
		}

    	return estimatedWaitTime;
    }
    
	/**
	 * Get the remaining wall time of the running jobs
	 * 
	 */
   
    public long getRunningJobsRemainingTime(Datacenter datacenter)
    {
    	
    	List<Job> tempRunningList = new LinkedList<Job>(); // temp List of running jobs
    	long remainingTime = 0;
    	long jobRemainingTime = 0;
    	
    	//Form temporary list for the running jobs
    	for(Server server : datacenter.getRack().get(0).getRackableServer()) {    		
    		if(server.getName().toString().matches("HPC_RESOURCE_MANAGEMENT")) {
    			Queue queue = server.getNativeOperatingSystem().getClusterManagement().get(0).getQueue();
	    		for(Job job: queue.getJobs()) {
	    			if(job.getStatus().toString().matches("RUNNING")) {
	    				tempRunningList.add(job);
	    			}
	    		}
    		}
    	}
    	
    	//Get the remaining wall time of the running jobs
    	for(int i = 0; i <  tempRunningList.size(); i++)
    	{
    		Job tempJob = tempRunningList.get(i);
    		jobRemainingTime = ((tempJob.getTimeOfStart().getValue() + tempJob.getWallTime().getValue()) - getEpochTime()) * tempJob.getNeededCoresPerNode().getValue() * tempJob.getNumberOfNodes().getValue();
    		remainingTime += jobRemainingTime;
    	}
    	
    	log.debug("Remaining Time for running jobs: " +remainingTime);
    	tempRunningList.clear();
    	return remainingTime;
    }
    
	/**
	 * Get the total wall time of the queued jobs
	 * 
	 */
    
    public long getQueuedJobsTime(Datacenter datacenter)
    {
    	List<Job> tempJobList = new LinkedList<Job>(); // temp List of jobs in the queue
    	long queuedTime = 0;
    	long jobTime = 0;
    	   	
    	//Form temporary list for the queued jobs
    	for(Server server : datacenter.getRack().get(0).getRackableServer()) {    		
    		if(server.getName().toString().matches("HPC_RESOURCE_MANAGEMENT")) {
    			Queue queue = server.getNativeOperatingSystem().getClusterManagement().get(0).getQueue();
	    		for(Job job: queue.getJobs()) {
	    			if(job.getStatus().toString().matches("QUEUED")) {
	    				tempJobList.add(job);
	    			}
	    		}
    		}
    	}
    	
    	//Get the wall time of the queued jobs
    	for(int i = 0; i <  tempJobList.size(); i++) {
    		Job tempJob = tempJobList.get(i);
    		jobTime = tempJob.getWallTime().getValue() * tempJob.getNeededCoresPerNode().getValue() * tempJob.getNumberOfNodes().getValue();
    		queuedTime += jobTime;
    	}
    	
    	log.debug("Total amount of wall time for queued jobs: " +queuedTime);
     	tempJobList.clear();
    	return queuedTime;
    }
    
	/**
	 * Get the total number of cores in the cluster
	 * 
	 */
    
    public int getClusterTotalCores(Datacenter datacenter)
    {
    	int totalCores = 0;	
    	for(Server server : datacenter.getRack().get(0).getRackableServer()) {   		
    		if(server.getName().toString().matches("HPC_COMPUTE_NODE")) {
	    		for(CPU cpu: server.getMainboard().get(0).getCPU()) {
	    			totalCores += cpu.getCore().size();		
	    		}
    		}
    	}
    	
    	log.debug("Cluster total cores: " +totalCores);
   	
    	return totalCores;
    }
    
	/**
	 * Get the total number of compute nodes in the cluster
	 * 
	 */
    
    public int getClusterNumServers(Datacenter datacenter)
    {
    	Datacenter tempDC = (Datacenter) datacenter.clone();
    	int numServers = 0;		   	
    	for(Server server : tempDC.getRack().get(0).getRackableServer()) { 		
    		if(server.getName().toString().matches("HPC_COMPUTE_NODE")){
    			numServers++;
    		}
    	}
    	
    	log.debug("Cluster total compute nodes: " +numServers);
   	
    	return numServers;
    }
    
	/**
	 * Get the queue size in the cluster
	 * 
	 */
    public int getQueueSize(Datacenter datacenter)
    {
    	Datacenter tempDC = (Datacenter) datacenter.clone();
    	int queueSize = 0;
    	
    	for(Server server : tempDC.getRack().get(0).getRackableServer()) {    		
    		if(server.getName().toString().matches("HPC_RESOURCE_MANAGEMENT")) {
    			Queue queue = server.getNativeOperatingSystem().getClusterManagement().get(0).getQueue();
	    		for(Job job: queue.getJobs()) {
	    			if(job.getStatus().toString().matches("QUEUED")) {
	    				queueSize++;
	    			}
	    		}
    		}
    	}
 	
    	return queueSize;
    }
    
	/**
	 * Gets the smallest application rank from all the DCs
	 * 
	 */
    
    public int getSmallestRank(String benchmarkName, FIT4Green model)
    {
    	int smallestRank = 0;
    	int candidateRank = -1;
    	
    	//If the user has specified a benchmark
    	if(!benchmarkName.isEmpty())
    	{
			for(Datacenter datacenter : model.getSite().get(0).getDatacenter())
			{
				for(ApplicationBenchmark benchmark : datacenter.getApplicationBenchmark())
				{
					if(benchmark.getBenchmarkID().matches(benchmarkName))
					{
						if(benchmark.getRank().getValue() < candidateRank || candidateRank == -1)
						{
							smallestRank = benchmark.getRank().getValue();
							candidateRank = benchmark.getRank().getValue();
						}
						
					}
				}
			}
    	}
    	//Else: no benchmark has been specified, get the average smallest value from all benchmarks of DC
    	else
    	{
    		int sum = 0;
    		int n = 0;
    		int dcSmallest = 0;
    		
    		// get the smallest rank value from all clusters
			for(Datacenter datacenter : model.getSite().get(0).getDatacenter())
			{	    		
				for(ApplicationBenchmark benchmark : datacenter.getApplicationBenchmark())
				{
					if(benchmark.getRank().getValue() < candidateRank || candidateRank == -1)
					{
						dcSmallest = benchmark.getRank().getValue();
						candidateRank = dcSmallest;
					}				
				}
				log.debug("Smallest rank of the data centre: "+dcSmallest);
				sum += dcSmallest;
				n++;
				candidateRank = -1;				
			} 
			//Get the average value from the smallest rank values
			smallestRank = sum/n;
    	}
    	
    	return smallestRank;
    }
    
	/**
	 * Gets the application rank of a  DC
	 * 
	 */
    
    public int getDCRank(String benchmarkName, Datacenter datacentre)
    {
    	int dcRank = 0;
    	
    	//If the user has specified as benchmark
    	if(!benchmarkName.isEmpty())
    	{
			for(ApplicationBenchmark benchmark : datacentre.getApplicationBenchmark())
			{
				if(benchmark.getBenchmarkID().matches(benchmarkName))
				{
					dcRank = benchmark.getRank().getValue();			
				}
			}
    	}
    	//Else: no benchmark has been specified, get the average value from all benchmarks
    	else
    	{
    		int sum = 0;
    		int n = 0;
			for(ApplicationBenchmark benchmark : datacentre.getApplicationBenchmark())
			{
				sum += benchmark.getRank().getValue();
				n++;
			}
			dcRank = sum/n; 		
    	}
    	
    	return dcRank;
    	
    }
	
	
	public void setModelCopy(FIT4Green model)
	{
		this.model = model;
	}
	public FIT4Green getModelCopy()
	{
		return this.model;
	}

	
	
}
