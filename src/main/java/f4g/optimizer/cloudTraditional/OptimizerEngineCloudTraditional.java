/**
 * ============================== Header ============================== 
 * file:          OptimizerEngineCloud.java
 * project:       FIT4Green/Optimizer
 * created:       26 nov. 2010 by cdupont
 * last modified: $LastChangedDate: 2012-05-04 10:34:00 +0200 (vie, 04 may 2012) $ by $LastChangedBy: paolo.barone@hp.com $
 * revision:      $LastChangedRevision: 1422 $
 * 
 * short description:
 *   This class contains the algorithm for Cloud computing.
 *   
 * ============================= /Header ==============================
 */

package f4g.optimizer.cloudTraditional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.apache.log4j.Logger;

import f4g.commons.controller.IController;
import f4g.commons.core.Constants;
import f4g.commons.power.IPowerCalculator;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.util.Util;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.action.F4GDriverFactory;
import f4g.optimizer.entropy.plan.constraint.CNoStateChange;
import f4g.optimizer.entropy.plan.constraint.api.NoStateChange;
import f4g.optimizer.entropy.plan.objective.CPowerObjective;
import f4g.optimizer.entropy.plan.objective.api.PowerObjective;
import f4g.optimizer.utils.IOptimizerServer;
import f4g.optimizer.OptimizerEngine;
import f4g.optimizer.cloudTraditional.SLAReader;
import f4g.optimizer.utils.Utils;
import f4g.optimizer.cloudTraditional.NetworkControl;
import f4g.optimizer.utils.OptimizerWorkload;
import f4g.optimizer.Optimizer.CloudTradCS;
import f4g.schemas.java.metamodel.*;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.ActionRequestType.ActionList;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.RequestType;
import f4g.schemas.java.allocation.AllocationResponseType;
import f4g.schemas.java.allocation.CloudVmAllocationResponseType;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.TraditionalVmAllocationResponseType;
import f4g.schemas.java.allocation.TraditionalVmAllocationType;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ConstraintType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType.Cluster;

import btrplace.plan.DependencyBasedPlanApplier;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.TimeBasedPlanApplier;
import btrplace.plan.event.Action;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import btrplace.model.DefaultModel;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Root;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ModelView;

/**
 * This class contains the algorithm for Cloud computing.
 * 
 * @author cdupont
 * @uml.dependency supplier="f4g.optimizer.SLAReader"
 */

public class OptimizerEngineCloudTraditional extends OptimizerEngine {


    public enum AlgoType {
		CLOUD, TRADITIONAL
	}

	CloudTradCS computingStyle;

	ServerGroupType serverGroups;

    private boolean optimize = true;

	public void setServerGroups(ServerGroupType sg) {
		this.serverGroups = sg;
	}

	/**
	 * Virtual Machines types retrieved from SLA
	 */
	private VMTypeType vmTypes;

	/**
	 * Cluster definition
	 */
	private ClusterType clusters;

	/**
	 * SLA definition
	 */
	private ConstraintType ct;
	private PolicyType policies;
	private FederationType federation;
	private SLAType SLAs;

    /**
     * Maximum solving duration. 0 for no time limit. Default is 5 seconds
     */
    private int searchTimeLimit = 5;


    /**
     * Get the search time limit.
     * A duration equals to 0 indicates no time limit.
     * @return a duration in seconds
     */
    public int getSearchTimeLimit() {
        return searchTimeLimit;
    }

    /**
     * Set the search time limit.
     * A duration equals to 0 indicates no time limit.
     * @param searchTimeLimit a duration in second.
     */
    public void setSearchTimeLimit(int searchTimeLimit) {
        this.searchTimeLimit = searchTimeLimit;
    }


    public SLAType getSla() {
		return SLAs;
	}

	public void setSla(SLAType sla) {
		this.SLAs = sla;
	}

	public void setFederation(FederationType federation) {
		this.federation = federation;
	}

	public FederationType getFederation() {
		return federation;
	}
	
	public PolicyType getPolicies() {
		return policies;
	}

	public void setPolicies(PolicyType policies) {
		this.policies = policies;
	}

	public void setConstraintType(ConstraintType ct) {
		this.ct = ct;
	}

    /**
     * Indicates if the planner stops after computing a first solution.
     * @return {@code true} if it continues
     */
    public boolean doOptimize() {
        return optimize;
    }

    /**
     * Indicates if the planner must stop after computing a first solution.
     * @return {@code true} if it continues
     */
    public void doOptimize(boolean optimize) {
        this.optimize = optimize;
    }
	
	/**
	 * constructor for production
	 * 
	 * @param traditional
	 */
	public OptimizerEngineCloudTraditional(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator,
			CloudTradCS cs) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());
		computingStyle = cs;

		try {
			String currentSlaClusterPathName = f4g.commons.core.Configuration
					.get(Constants.SLA_CLUSTER_FILE_PATH);
			log.trace("SLA pathname:" + currentSlaClusterPathName);
			SLAReader slaReader = new SLAReader(currentSlaClusterPathName);

			vmTypes = slaReader.getVMtypes();
			clusters = slaReader.getCluster();
			serverGroups = slaReader.getServerGroup();
			ct = slaReader.getPCs();
			policies = slaReader.getPolicies();
			federation = slaReader.getFeds();
			SLAs = slaReader.getSLAs();
			showVMs(vmTypes);

		} catch (Exception e) {
			log.warn("error in SLA");
			log.warn(e);
		}
	}

	/**
	 * constructor for Unit Testing
	 */
	public OptimizerEngineCloudTraditional(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator,
			VMTypeType theVMTypes, PolicyType myPolicies, FederationType myFederation) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());
		// default to Cloud
		computingStyle = CloudTradCS.CLOUD;

		vmTypes = theVMTypes;
		
		if(myFederation.getBoundedCluster() != null) {
			clusters = new ClusterType();
			for(Cluster c : myFederation.getBoundedCluster().getCluster()) {
				clusters.getCluster().add(c.getIdref());
			}
		}		
		
		serverGroups = null;
		ct = null;
		SLAs = null;
		policies = myPolicies;
		federation = myFederation;
		
		showVMs(vmTypes);
		
	}
	
	/**
	 * constructor for production
	 * 
	 * @param traditional
	 */
	public OptimizerEngineCloudTraditional(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator,
			CloudTradCS cs, SLAReader slaReader) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());
		computingStyle = cs;
		vmTypes = slaReader.getVMtypes();
		clusters = slaReader.getCluster();
		serverGroups = slaReader.getServerGroup();
		ct = slaReader.getPCs();
		policies = slaReader.getPolicies();
		federation = slaReader.getFeds();
		SLAs = slaReader.getSLAs();
		showVMs(vmTypes);
		
	}

	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest
	 *            Data structure describing the resource allocation request
	 * @return A data structure representing the result of the allocation
	 */
	public AllocationResponseType allocateResource(
			AllocationRequestType allocationRequest, FIT4GreenType model) {
		log.debug("in allocateResource");

		if (allocationRequest == null || allocationRequest.getRequest() == null) {
			log.warn("Allocation request is not correct");
			return null;
		}
		showAllocation(allocationRequest);
		
		Model mo = new DefaultModel();
		F4GConfigurationAdapter confAdapter = new F4GConfigurationAdapter(model, vmTypes, powerCalculator, optiObjective);
		confAdapter.attachViews(mo);
				
		RequestType request = (RequestType) allocationRequest.getRequest().getValue();
				
		VM VMtoAllocate = mo.newVM();
		mo.getMapping().addReadyVM(VMtoAllocate);
		confAdapter.addVMViews(VMtoAllocate, (CloudVmAllocationType) request, mo); //TODO generalize
	
		List<SatConstraint> cstrs = new ArrayList<>();
		cstrs.add(new Running(VMtoAllocate));

		cstrs.addAll(getConstraints(model, request, mo.getMapping(), VMtoAllocate));
				
		ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
		cra.getConstraintMapper().register(new CPowerObjective.Builder());
		cra.getConstraintMapper().register(new CNoStateChange.Builder());
        try {
            ReconfigurationPlan plan = cra.solve(mo, cstrs, new PowerObjective());
            
            if(plan != null) {
            	System.out.println("Time-based plan:");
                System.out.println(new TimeBasedPlanApplier().toString(plan));
                System.out.println("\nDependency based plan:");
                System.out.println(new DependencyBasedPlanApplier().toString(plan));

                Node dest = plan.getResult().getMapping().getVMLocation(VMtoAllocate);
            
                // create the response
                NamingService ns = (NamingService) mo.getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.NAMING_SERVICE);
      			return createAllocationResponseFromServer(dest, allocationRequest.getRequest().getValue(), ns);
            } else {
            	return new AllocationResponseType();
            }
                    
        } catch (Exception e) {
            System.err.println(e.getMessage());
            log.debug("Allocation impossible, returning empty allocation");
			return new AllocationResponseType();
        }
        

	}

	

	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model
	 *            the f4g model
	 * @return true if successful, false otherwise
	 */
	@Override
	public void runGlobalOptimization(FIT4GreenType F4Gmodel) {
		log.debug("Performing Global Optimization");

		Model model = new DefaultModel();
		F4GConfigurationAdapter confAdapter = new F4GConfigurationAdapter(F4Gmodel, vmTypes, powerCalculator, optiObjective);
		confAdapter.attachViews(model);
		
		if (model.getMapping().getAllNodes().size() == 0) {
			log.warn("No Nodes");
		} else {
			List<AbstractBaseActionType> actions = new ArrayList<AbstractBaseActionType>();

			if (model.getMapping().getAllVMs().size() == 0) {
				log.debug("No VMs");
			}
			
			// create the constraints
			List<SatConstraint> cstrs = getConstraints(F4Gmodel, model.getMapping());
			
			ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
			cra.getConstraintMapper().register(new CPowerObjective.Builder());
			cra.setTimeLimit(5);
			
			Mapping resultMapping;
			 try {
		            ReconfigurationPlan plan = cra.solve(model, cstrs, new PowerObjective());
		            
		            if(plan != null) {
		            	System.out.println("Time-based plan:");
			            System.out.println(new TimeBasedPlanApplier().toString(plan));
			            System.out.println("\nDependency based plan:");
			            System.out.println(new DependencyBasedPlanApplier().toString(plan));

			            resultMapping = plan.getResult().getMapping();
		            }
		            		             
		        } catch (SolverException ex) {
		            System.err.println(ex.getMessage());
		        }
			 
		ActionList actionList = new ActionList();
		// initialise actions in case of empty list
		actionList.getAction();
		
		List<PowerOnActionType> powerOns = new ArrayList<PowerOnActionType>();
		List<PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();
		List<AbstractBaseActionType> moves = new ArrayList<AbstractBaseActionType>();
		for (AbstractBaseActionType action : actions) {
			if (action instanceof PowerOnActionType)
				powerOns.add((PowerOnActionType) action);
			if (action instanceof PowerOffActionType)
				powerOffs.add((PowerOffActionType) action);
			if (action instanceof MoveVMActionType 
			 || action instanceof LiveMigrateVMActionType)
				moves.add(action);
		}
		
		//sort actions (first ON, then moves, then OFF)
		Collections.sort(actions, new ActionComparator());
		
		// create JAXB actions
		for (AbstractBaseActionType action : actions) {
			actionList.getAction().add((new f4g.schemas.java.actions.ObjectFactory()).createAction(action));
		}
		
		// compute the new datacenter with only moves
		FIT4GreenType newFederationWithMoves = performMoves(moves, F4Gmodel);
		FIT4GreenType newFederation = performOnOffs(powerOns, powerOffs, newFederationWithMoves);

		// -------------------
		// ON/OFF actions on network equipment
		ActionList myNetworkActionList = NetworkControl.getOnOffActions(newFederation, F4Gmodel);
		actionList.getAction().addAll(myNetworkActionList.getAction());
		newFederation = NetworkControl.performOnOffs(newFederation,
				myNetworkActionList);
		// -------------------

		ActionRequestType actionRequest = getActionRequest(actionList, F4Gmodel, newFederation);
		controller.executeActionList(actionRequest);
		}
	}
			
	//a simple allocation should not result in VM migration or node on/off
	private List<SatConstraint> getAllocationConstraints(Mapping map) {
	
		List<SatConstraint> res = new ArrayList<SatConstraint>();
		res.addAll(NoStateChange.newNoStateChanges(map.getAllNodes()));
		res.addAll(Root.newRoot(map.getAllVMs()));
		return res;
		
	}

	private List<SatConstraint> getConstraints(FIT4GreenType model, Mapping src) {
		List<SatConstraint> queue = new LinkedList<SatConstraint>();
		
		if (clusters != null) {
//			queue.addAll(new SLAConstraintFactory(clusters, src, model, -1, serverGroups).createSLAConstraints());
//			queue.addAll(new ClusterConstraintFactory(clusters, src).createClusterConstraints());
//			if (ct != null) {
//				queue.add(new PlacementConstraintFactory(src, model, serverGroups).createPCConstraints());
//			}
		}
//		queue.addAll(new PolicyConstraintFactory(clusters, src, model, federation, vmTypes, powerCalculator, costEstimator).createPolicyConstraints());
//		queue.addAll(new ModelConstraintFactory(src, model).getModelConstraints());
		return queue;
	}
	
	private List<SatConstraint> getConstraints(FIT4GreenType model, RequestType request, Mapping src, VM VMtoAllocate) {
		
		
		int minPriority = 1;
		List<SatConstraint> queue = new LinkedList<SatConstraint>(); 
		
		if (computingStyle == CloudTradCS.CLOUD) {
			if (((CloudVmAllocationType) request).getMinPriority() != null)
				minPriority = ((CloudVmAllocationType) request).getMinPriority();
		} else {
			if (((TraditionalVmAllocationType) request).getMinPriority() != null)
				minPriority = ((TraditionalVmAllocationType) request).getMinPriority();
		}
		
		if (clusters != null) {
//			queue.addAll(new SLAConstraintFactory(clusters, src, model, minPriority, serverGroups).createSLAConstraints());
//			ClusterConstraintFactory clusterConstraintFactory = new ClusterConstraintFactory(clusters, src);
//			queue.addAll(clusterConstraintFactory.createClusterConstraints());
//			if (ct != null) {
//				queue.addAll(new PlacementConstraintFactory(src, model, serverGroups).createPCConstraints());
//			}
			//queue.addAll(clusterConstraintFactory.restrictPlacementToClusters(request, VMtoAllocate));
		}
		
		//queue.addAll(new ModelConstraintFactory(src, model).getModelConstraints());
		return queue;
	}
	

	private List<AbstractBaseActionType> computeActions(FIT4GreenType F4GModel, Model model, List<SatConstraint> cstrs, NamingService nameService) {
		
		List<AbstractBaseActionType> actions = new ArrayList<AbstractBaseActionType>();
		
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        
        try {
            ReconfigurationPlan plan = cra.solve(model, cstrs, new PowerObjective());
            System.out.println("Time-based plan:");
            System.out.println(new TimeBasedPlanApplier().toString(plan));
            System.out.println("\nDependency based plan:");
            System.out.println(new DependencyBasedPlanApplier().toString(plan));
        
            F4GDriverFactory f4GDriverFactory = new F4GDriverFactory(controller, F4GModel, nameService);
            
            for (Action action : plan.getActions()) {
    			log.debug("action: " + action.getClass().getName());

    			AbstractBaseActionType f4gAction = f4GDriverFactory.transform(action).getActionToExecute();
    			if(f4gAction != null){
    				actions.add(f4gAction);	
    			}    			 
    		}
            
        } catch (SolverException ex) {
            System.err.println(ex.getMessage());            
        }
		
		return actions;
	}

	/**
	 * suppress from the configuration all VMs and nodes associated to non satisfied constraints
	 * TODO: reactivate
	 */
//	private Configuration getDegradedConfiguration(Configuration src, List<VJob> queue) {
//		
//		Configuration deg = src.clone();
//		for(VJob q : queue) {
//			for(PlacementConstraint c : q.getConstraints()) {
//				if(!c.isSatisfied(src)) {
//					log.debug("broken constraint: " + c.getClass().getName());
//					for(VirtualMachine vm : c.getAllVirtualMachines()) {
//						log.debug("removing VM " + vm.getName());
//						deg.remove(vm);
//					}
//					for(Node n : c.getNodes()) {
//						for(VirtualMachine vm : src.getRunnings(n)) {
//							log.debug("removing VM " + vm.getName());
//							deg.remove(vm); //TODO remove this when Entropy is corrected
//						}
//						boolean ret = deg.remove(n);
//						if(ret) {
//							deg.getAllNodes().remove(n);
//							log.debug("removing node " + n.getName());
//						}
//						
//					}
//				}
//			}
//		}
//		return deg;
//	}

	private List<IOptimizerServer> getServersInCluster(final ArrayList<IOptimizerServer> optimizerServers, ClusterType.Cluster cluster) {
		List<IOptimizerServer> serversInCluster = new ArrayList<IOptimizerServer>();
		for (IOptimizerServer s : optimizerServers) {
			String clusterName = Utils.getClusterId(s.getFrameworkID(), clusters);
			if (clusterName.equals(cluster.getName())) {
				serversInCluster.add(s);
			}
		}
		return serversInCluster;
	}
		

	/**
	 * creates an allocation response from a server number
	 * 
	 * @param model
	 */
	protected AllocationResponseType createAllocationResponseFromServer(Node node, RequestType request, NamingService ns) {
		// Creates a response
		AllocationResponseType response = new AllocationResponseType();

		if (node != null) {

			if (computingStyle == CloudTradCS.CLOUD) {
				CloudVmAllocationResponseType cloudVmAllocationResponse = getResponse(node, request, ns);
				response.setResponse((new ObjectFactory()).createCloudVmAllocationResponse(cloudVmAllocationResponse));
			} else {
				TraditionalVmAllocationResponseType tradVmAllocationResponse = getResponse(node, ns);
				response.setResponse((new ObjectFactory()).createTradinitionalVmAllocationResponse(tradVmAllocationResponse));
			}

			log.debug("Allocated on: " + node.id());

			try {
				GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
				response.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));

			} catch (DatatypeConfigurationException e) {
				log.debug("Error in date");
			}

		} else
			log.debug("Allocation impossible");

		return response;
	}

	protected OptimizerWorkload getOptimizerWorkload(RequestType request) {

		if (computingStyle == CloudTradCS.CLOUD) {
			// Get the VM type from SLA.
			VMTypeType.VMType SLAVM;
			try {
				SLAVM = Util.findVMByName(
						((CloudVmAllocationType) request).getVmType(), vmTypes);
			} catch (NoSuchElementException e1) {
				log.warn("VM type not found in SLA, allocation impossible");
				return null;
			}

			OptimizerWorkload WL = new OptimizerWorkload(SLAVM, "No name");
			return WL;
		} else {
			return new OptimizerWorkload((TraditionalVmAllocationType) request,
					"No name");
		}

	}

	protected ArrayList<IOptimizerServer> getOptimizerServers(
			DatacenterType datacenter) {

		if (computingStyle == CloudTradCS.CLOUD) {
			// get translations between F4G types and optimizer types
			final ArrayList<IOptimizerServer> optimizerServers = Utils
					.getAllOptimizerServersCloud(datacenter, vmTypes);
			return optimizerServers;
		} else {
			// get translations between F4G types and optimizer types
			final ArrayList<IOptimizerServer> optimizerServers = Utils
					.getAllOptimizerServersTradi(datacenter);
			return optimizerServers;
		}

	}

	protected ArrayList<IOptimizerServer> getOptimizerServers(
			FIT4GreenType federation) {

		// get translations between F4G types and optimizer types
		final ArrayList<IOptimizerServer> optimizerServers = new ArrayList<IOptimizerServer>();
		for (SiteType s : federation.getSite()) {
			for (DatacenterType dc : s.getDatacenter()) {
				optimizerServers.addAll(getOptimizerServers(dc));
			}
		}
		return optimizerServers;
	}

	public CloudVmAllocationResponseType getResponse(Node node,	RequestType request, NamingService ns) {

		CloudVmAllocationResponseType cloudVmAllocationResponse = new CloudVmAllocationResponseType();
		CloudVmAllocationType CloudOperation = (CloudVmAllocationType) request;

		// setting the response
		cloudVmAllocationResponse.setNodeId(ns.getNodeName(node));

		cloudVmAllocationResponse.setClusterId(Utils.getClusterId(ns.getNodeName(node), clusters));
		cloudVmAllocationResponse.setImageId(CloudOperation.getImageId());
		cloudVmAllocationResponse.setUserId(CloudOperation.getUserId());
		cloudVmAllocationResponse.setVmType(CloudOperation.getVmType());
		return cloudVmAllocationResponse;
	}

	public TraditionalVmAllocationResponseType getResponse(Node node, NamingService ns) {

		TraditionalVmAllocationResponseType traditionalVmAllocationResponse = new TraditionalVmAllocationResponseType();

		// setting the response
		traditionalVmAllocationResponse.setNodeId(ns.getNodeName(node)); 

		traditionalVmAllocationResponse.setClusterId(Utils.getClusterId(ns.getNodeName(node), clusters));  
		traditionalVmAllocationResponse.setImageId("");
		traditionalVmAllocationResponse.setUserId("");
		traditionalVmAllocationResponse.setVmType("");
		return traditionalVmAllocationResponse;
	}

	/**
	 * shows all VMs from SLA.
	 */
	protected void showVMs(VMTypeType VMs) {
		log.debug("VMs from SLA:");
		for (VMTypeType.VMType VM : VMs.getVMType())
			showVM(VM);
	}

	/**
	 * shows one VM.
	 */
	void showVM(VMTypeType.VMType VM) {
		log.debug("Name: " + VM.getName());
		log.debug("CPUs: " + VM.getCapacity().getVCpus().getValue());
		log.debug("CPU consumption: " + VM.getExpectedLoad().getVCpuLoad().getValue());
		log.debug("RAM: " + VM.getCapacity().getVRam().getValue());
		log.debug("HD: " + VM.getCapacity().getVHardDisk().getValue());
	}

	public void setVmTypes(VMTypeType vmTypes) {
		this.vmTypes = vmTypes;
	}

	public VMTypeType getVmTypes() {
		return vmTypes;
	}

	public ClusterType getClusters() {
		return clusters;
	}

	public void setClusters(ClusterType clusterType) {
		this.clusters = clusterType;
	}

	public CloudTradCS getComputingStyle() {
		return computingStyle;
	}

	public void setComputingStyle(CloudTradCS computingStyle) {
		this.computingStyle = computingStyle;
	}

	public void showAllocation(AllocationRequestType allocationRequest) {
		RequestType request = (RequestType) allocationRequest.getRequest()
				.getValue();
		if (request instanceof CloudVmAllocationType) {
			CloudVmAllocationType r = (CloudVmAllocationType) request;
			log.debug("allocation type Cloud:");
			String ids = new String();
			for (String id : r.getClusterId()) {
				ids += id + ";";
			}
			log.debug("Cluster IDs = " + ids);
			log.debug("Image ID = " + r.getImageId());
			log.debug("VM type = " + r.getVmType());
			log.debug("User ID = " + r.getUserId());

		} else {
			TraditionalVmAllocationType r = (TraditionalVmAllocationType) request;
			log.debug("allocation type Traditional:");
			String ids = new String();
			for (String id : r.getClusterId()) {
				ids += id + ";";
			}
			log.debug("Cluster IDs = " + ids);
			log.debug("NB of CPU = " + r.getNumberOfCPUs());
			log.debug("CPU usage = " + r.getCPUUsage());
			log.debug("Disk IO rate = " + r.getDiskIORate());
			log.debug("Memory usage = " + r.getMemoryUsage());
			log.debug("Network usage = " + r.getNetworkUsage());
			log.debug("Storage usage = " + r.getStorageUsage());

		}

		log.debug("Name: " + allocationRequest.getRequest().getValue());
	}
	
	
    //Compare two nodes and returns the one with less remaining space on CPU
    public static class ActionComparator implements Comparator<AbstractBaseActionType> {

    	@Override
        public int compare(AbstractBaseActionType a1, AbstractBaseActionType a2) {
    		int pos1 = getPosition(a1);
    		int pos2 = getPosition(a2);
			if (pos1 > pos2 ) {
				return 1;
			} else if (pos1 == pos2) {
				return 0;
			} else {
				return -1;
			}
        }
    	
    	public int getPosition(AbstractBaseActionType action) {
    		if (action instanceof PowerOnActionType) {
    			return 1;
    		} else if (action instanceof MoveVMActionType) {
    			return 2;
    		} else if (action instanceof LiveMigrateVMActionType) {
    			return 2;
    		} else if (action instanceof PowerOffActionType) {
    			return 3;
    		} else {
    			return 4;
    		}			
    	}   	
    }

}
