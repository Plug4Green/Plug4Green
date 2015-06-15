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

package f4g.optimizer.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

import f4g.commons.controller.IController;
import f4g.commons.core.Constants;
import f4g.commons.power.IPowerCalculator;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.util.Util;
import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.action.F4GDriver;
import f4g.optimizer.entropy.plan.action.F4GDriverFactory;
import f4g.optimizer.entropy.plan.constraint.CMaxServerPower;
import f4g.optimizer.entropy.plan.constraint.CNoStateChange;
import f4g.optimizer.entropy.plan.constraint.CSpareNodes;
import f4g.optimizer.entropy.plan.constraint.factories.ClusterConstraintFactory;
import f4g.optimizer.entropy.plan.constraint.factories.ModelConstraintFactory;
import f4g.optimizer.entropy.plan.constraint.factories.PolicyConstraintFactory;
import f4g.optimizer.entropy.plan.constraint.factories.SLAConstraintFactory;
import f4g.optimizer.entropy.plan.objective.CPowerObjective;
import f4g.optimizer.entropy.plan.objective.api.PowerObjective;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.utils.IOptimizerServer;
import f4g.optimizer.OptimizerEngine;
import f4g.optimizer.cloud.NetworkControl;
import f4g.optimizer.cloud.SLAReader;
import f4g.optimizer.utils.Utils;
import f4g.optimizer.utils.OptimizerWorkload;
import f4g.schemas.java.metamodel.*;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.ActionRequest.ActionList;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.Request;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType.Cluster;

import org.btrplace.plan.DependencyBasedPlanApplier;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.TimeBasedPlanApplier;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootVM;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Running;
import org.btrplace.model.constraint.SatConstraint;

/**
 * This class contains the algorithm for Cloud computing.
 * 
 * @author cdupont
 */

public class OptimizerEngineCloud extends OptimizerEngine {

	private ServerGroupType serverGroups;

	 //Virtual Machines types retrieved from SLA
	private VMFlavorType vms;


	//Cluster definition
	private ClusterType clusters;

	//SLA definition
	private PolicyType policies;
	private FederationType federation;
	private SLAType SLAs;
	
	//CPU constraint VM per VM
	private Map<String, Integer> VMCPUConstraint;


	/**
	 * constructor for production
	 * 
	 */
	public OptimizerEngineCloud(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());

		try {
			String currentSlaClusterPathName = f4g.commons.core.Configuration.get(Constants.SLA_CLUSTER_FILE_PATH);
			log.trace("SLA pathname:" + currentSlaClusterPathName);
			SLAReader slaReader = new SLAReader(currentSlaClusterPathName);

			vms = slaReader.getVMtypes();
			clusters = slaReader.getCluster();
			serverGroups = slaReader.getServerGroup();
			policies = slaReader.getPolicies();
			federation = slaReader.getFeds();
			SLAs = slaReader.getSLAs();
			VMCPUConstraint = new HashMap<String, Integer>();

			showVMs(vms);
		
		} catch (Exception e) {
			log.warn("error in SLA");
			log.warn(e);
		}
	}

	/**
	 * constructor for Unit Testing
	 */
	public OptimizerEngineCloud(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator,
			VMFlavorType theVMFlavors, PolicyType myPolicies, FederationType myFederation) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());

		vms = theVMFlavors;
		
		if(myFederation.getBoundedCluster() != null) {
			clusters = new ClusterType();
			for(Cluster c : myFederation.getBoundedCluster().getCluster()) {
				clusters.getCluster().add(c.getIdref());
			}
		}		
		
		serverGroups = null;
		SLAs = null;
		policies = myPolicies;
		federation = myFederation;
		VMCPUConstraint = new HashMap<String, Integer>();
		
		showVMs(vms);
		
		
	}
	
	/**
	 * constructor for production
	 * 
	 * @param traditional
	 */
	public OptimizerEngineCloud(IController controller,
			IPowerCalculator powerCalculator, ICostEstimator costEstimator,
			SLAReader slaReader) {
		super(controller, powerCalculator, costEstimator);
		log = Logger.getLogger(this.getClass().getName());
		vms = slaReader.getVMtypes();
		clusters = slaReader.getCluster();
		serverGroups = slaReader.getServerGroup();
		policies = slaReader.getPolicies();
		federation = slaReader.getFeds();
		SLAs = slaReader.getSLAs();
		showVMs(vms);
		
	}

	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest
	 *            Data structure describing the resource allocation request
	 * @return A data structure representing the result of the allocation
	 */
	public AllocationResponse allocateResource(AllocationRequest allocationRequest, FIT4Green F4GModel) {
		log.debug("allocateResource");
		if (allocationRequest == null || allocationRequest.getRequest() == null) {
			log.warn("Allocation request is not correct");
			return null;
		}
		
		Optional<ReconfigurationPlan> oPlan = computePlan(F4GModel, Optional.of(allocationRequest));
		
		if(oPlan.isPresent()) {
			ReconfigurationPlan plan = oPlan.get();
			NamingService<Node> nodeNames = (NamingService<Node>) plan.getOrigin().getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.NODE_NAMING_SERVICE);
			showAllocation(allocationRequest);
		    
			Node dest = null;
            for(Action action : plan.getActions()) {
            	if (action instanceof BootVM) {
            		dest = ((BootVM)action).getDestinationNode();
            		break;
            	}			
            }
            if(dest != null) {
            	return createAllocationResponseFromServer(dest, allocationRequest.getRequest().getValue(), nodeNames);	
            } else {
            	return new AllocationResponse();
            }
            
            
        } else {
           	return new AllocationResponse();
        } 
	}
	

	/**
	 * Handles a request for a global optimization
	 */
	@Override
	public void runGlobalOptimization(FIT4Green F4GModel) {
		log.debug("Performing Global Optimization");
		
		Optional<ReconfigurationPlan> oPlan = computePlan(F4GModel, Optional.<AllocationRequest>absent());
		
		if(oPlan.isPresent()) {
			ReconfigurationPlan plan = oPlan.get();
			NamingService<Node> nodeNames = (NamingService<Node>) plan.getOrigin().getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.NODE_NAMING_SERVICE);
			NamingService<VM> vmNames = (NamingService<VM>) plan.getOrigin().getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.VM_NAMING_SERVICE);
            
            F4GDriverFactory f4GDriverFactory = new F4GDriverFactory(controller, F4GModel, nodeNames, vmNames);
            
            List<AbstractBaseAction> actions = new ArrayList<AbstractBaseAction>();
			for (Action action : plan.getActions()) {
    			log.debug("action: " + action.getClass().getName());

    			F4GDriver f4gDriver = f4GDriverFactory.transform(action);
    			if(f4gDriver != null) {
    				AbstractBaseAction f4gAction = f4gDriver.getActionToExecute();
        			if(f4gAction != null){
        				actions.add(f4gAction);	
        			}	
    			}
    			    			 
    		}
    		
    		List<PowerOnAction> powerOns = new ArrayList<PowerOnAction>();
    		List<PowerOffAction> powerOffs = new ArrayList<PowerOffAction>();
    		List<AbstractBaseAction> moves = new ArrayList<AbstractBaseAction>();
    		for (AbstractBaseAction action : actions) {
    			if (action instanceof PowerOnAction)
    				powerOns.add((PowerOnAction) action);
    			if (action instanceof PowerOffAction)
    				powerOffs.add((PowerOffAction) action);
    			if (action instanceof MoveVMAction 
    			 || action instanceof LiveMigrateVMAction)
    				moves.add(action);
    		}
    		
    		//sort actions (first ON, then moves, then OFF)
    		Collections.sort(actions, new ActionComparator());
    		
    		ActionList actionList = new ActionList();
    		// create JAXB actions
    		for (AbstractBaseAction action : actions) {
    			actionList.getAction().add((new f4g.schemas.java.actions.ObjectFactory()).createAction(action));
    		}
    		
    		// compute the new datacenter with only moves
    		FIT4Green newFederationWithMoves = performMoves(moves, F4GModel);
    		FIT4Green newFederation = performOnOffs(powerOns, powerOffs, newFederationWithMoves);

    		// -------------------
    		// ON/OFF actions on network equipmentF4GModel
    		ActionList myNetworkActionList = NetworkControl.getOnOffActions(newFederation, F4GModel);
    		actionList.getAction().addAll(myNetworkActionList.getAction());
    		newFederation = NetworkControl.performOnOffs(newFederation,
    				myNetworkActionList);
    		// -------------------

    		ActionRequest actionRequest = getActionRequest(actionList, F4GModel, newFederation);
    		controller.executeActionList(actionRequest);
		}		
	}
	
	public Optional<ReconfigurationPlan> computePlan(FIT4Green F4GModel, Optional<AllocationRequest> oAllocationRequest) {
			
		Model model = new DefaultModel();
		F4GConfigurationAdapter confAdapter = new F4GConfigurationAdapter(F4GModel, vms, powerCalculator, optiObjective);
		confAdapter.addConfiguration(model);

		List<SatConstraint> cstrs = getConstraints(F4GModel, model);
        
		if(oAllocationRequest.isPresent()) {
			VM VMtoAllocate = model.newVM();
			model.getMapping().addReadyVM(VMtoAllocate);
			cstrs.add(new Running(VMtoAllocate));
			
			Request request = (Request) oAllocationRequest.get().getRequest().getValue();	
			confAdapter.addVMViews(VMtoAllocate, (CloudVmAllocation) request, model); //TODO generalize
		}
		
		ChocoScheduler cra = new DefaultChocoScheduler();
		//register all F4G constraints and the objective
		registerF4GConstraints(cra);
		//cra.setVerbosity(3);
		cra.doOptimize(true);
		cra.setTimeLimit(5);
		ReconfigurationPlan plan = null;
		try {
            System.err.println(model);
            System.err.println(cstrs);
			plan = cra.solve(model, cstrs, new PowerObjective());
		    if(plan != null) {
		       	System.out.println("Time-based plan:");
			    System.out.println(new TimeBasedPlanApplier().toString(plan));
			    System.out.println("\nDependency based plan:");
			    System.out.println(new DependencyBasedPlanApplier().toString(plan));
			    return Optional.of(plan);
		    } else {
		    	return Optional.absent();
		    }
	    } catch (Exception ex) {
		    System.out.println("computePlan exception: " + ex.getLocalizedMessage());
		    return Optional.absent();
		} finally {
            System.out.println(cra.getStatistics());
        }
	}
			 
			
	public void registerF4GConstraints(ChocoScheduler cra) {
		
		cra.getConstraintMapper().register(new CPowerObjective.Builder());
		cra.getConstraintMapper().register(new CNoStateChange.Builder());
		cra.getConstraintMapper().register(new CSpareNodes.Builder());
		cra.getConstraintMapper().register(new CMaxServerPower.Builder());
		
	}
	
	
	private List<SatConstraint> getConstraints(FIT4Green F4Gmodel, Model model) {
		
		List<SatConstraint> constraints = new LinkedList<SatConstraint>();

		if (clusters != null) {
			constraints.addAll(new SLAConstraintFactory(clusters, model, F4Gmodel, -1, serverGroups).createSLAConstraints());
			constraints.addAll(new ClusterConstraintFactory(clusters, model).createClusterConstraints());
		}

		constraints.addAll(new PolicyConstraintFactory(clusters, model, F4Gmodel, federation, vms, powerCalculator, costEstimator).createPolicyConstraints());
		constraints.addAll(new ModelConstraintFactory(model, F4Gmodel, vms, VMCPUConstraint).getModelConstraints());
		
		return constraints;
	}
	
	private List<SatConstraint> getConstraints(FIT4Green model, Request request, Mapping src, VM VMtoAllocate) {
		
		
		int minPriority = 1;
		List<SatConstraint> queue = new LinkedList<SatConstraint>(); 
		if (((CloudVmAllocation) request).getMinPriority() != null)
			minPriority = ((CloudVmAllocation) request).getMinPriority();
				
		if (clusters != null) {
//			queue.addAll(new SLAConstraintFactory(clusters, src, model, minPriority, serverGroups).createSLAConstraints());
//			ClusterConstraintFactory clusterConstraintFactory = new ClusterConstraintFactory(clusters, src);
//			queue.addAll(clusterConstraintFactory.createClusterConstraints());
			//queue.addAll(clusterConstraintFactory.restrictPlacementToClusters(request, VMtoAllocate));
		}
		
		//queue.addAll(new ModelConstraintFactory(src, model).getModelConstraints());
		return queue;
	}
	

	private List<AbstractBaseAction> computeActions(FIT4Green F4GModel, Model model, List<SatConstraint> cstrs, NamingService<Node> nodeNames, NamingService<VM> vmNames) {
		
		List<AbstractBaseAction> actions = new ArrayList<AbstractBaseAction>();
		
        ChocoScheduler cra = new DefaultChocoScheduler();
        
        try {
            ReconfigurationPlan plan = cra.solve(model, cstrs, new PowerObjective());
            System.out.println("Time-based plan:");
            System.out.println(new TimeBasedPlanApplier().toString(plan));
            System.out.println("\nDependency based plan:");
            System.out.println(new DependencyBasedPlanApplier().toString(plan));
        
            F4GDriverFactory f4GDriverFactory = new F4GDriverFactory(controller, F4GModel, nodeNames, vmNames);
            
            for (Action action : plan.getActions()) {
    			log.debug("action: " + action.getClass().getName());

    			AbstractBaseAction f4gAction = f4GDriverFactory.transform(action).getActionToExecute();
    			if(f4gAction != null){
    				actions.add(f4gAction);	
    			}    			 
    		}
            
        } catch (Exception ex) {
            System.err.println(ex.getMessage());            
        }
		
		return actions;
	}

	
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
	protected AllocationResponse createAllocationResponseFromServer(Node node, Request request, NamingService<Node> ns) {
		// Creates a response
		AllocationResponse response = new AllocationResponse();

		if (node != null) {

			CloudVmAllocationResponse cloudVmAllocationResponse = getResponse(node, request, ns);
			response.setResponse((new ObjectFactory()).createCloudVmAllocationResponse(cloudVmAllocationResponse));
			
			log.debug("Allocated on: " + ns.getName(node));

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

	protected OptimizerWorkload getOptimizerWorkload(Request request) {

		// Get the VM type from SLA.
		VMFlavorType.VMFlavor SLAVM;
		try {
			SLAVM = Util.findVMByName(
					((CloudVmAllocation) request).getVm(), vms);
		} catch (NoSuchElementException e1) {
			log.warn("VM type not found in SLA, allocation impossible");
			return null;
		}
			OptimizerWorkload WL = new OptimizerWorkload(SLAVM, "No name");
		return WL;

	}

	protected ArrayList<IOptimizerServer> getOptimizerServers(Datacenter datacenter) {

		// get translations between F4G types and optimizer types
		final ArrayList<IOptimizerServer> optimizerServers = Utils.getAllOptimizerServersCloud(datacenter, vms);
		return optimizerServers;
	}

	protected ArrayList<IOptimizerServer> getOptimizerServers(FIT4Green federation) {

		// get translations between F4G types and optimizer types
		final ArrayList<IOptimizerServer> optimizerServers = new ArrayList<IOptimizerServer>();
		for (Site s : federation.getSite()) {
			for (Datacenter dc : s.getDatacenter()) {
				optimizerServers.addAll(getOptimizerServers(dc));
			}
		}
		return optimizerServers;
	}

	public CloudVmAllocationResponse getResponse(Node node,	Request request, NamingService<Node> ns) {

		CloudVmAllocationResponse cloudVmAllocationResponse = new CloudVmAllocationResponse();
		CloudVmAllocation CloudOperation = (CloudVmAllocation) request;

		// setting the response
		cloudVmAllocationResponse.setNodeId(ns.getName(node));

		cloudVmAllocationResponse.setClusterId(Utils.getClusterId(ns.getName(node), clusters));
		cloudVmAllocationResponse.setImageId(CloudOperation.getImageId());
		cloudVmAllocationResponse.setUserId(CloudOperation.getUserId());
		cloudVmAllocationResponse.setVm(CloudOperation.getVm());
		return cloudVmAllocationResponse;
	}

	/**
	 * shows all VMs from SLA.
	 */
	protected void showVMs(VMFlavorType VMs) {
		log.debug("VMs from SLA:");
		for (VMFlavorType.VMFlavor VM : VMs.getVMFlavor())
			showVM(VM);
	}

	/**
	 * shows one VM.
	 */
	void showVM(VMFlavorType.VMFlavor VM) {
		log.debug("Name: " + VM.getName());
		log.debug("CPUs: " + VM.getCapacity().getVCpus().getValue());
		log.debug("CPU consumption: " + VM.getExpectedLoad().getVCpuLoad().getValue());
		log.debug("RAM: " + VM.getCapacity().getVRam().getValue());
		log.debug("HD: " + VM.getCapacity().getVHardDisk().getValue());
	}

	public void setVmTypes(VMFlavorType vms) {
		this.vms = vms;
	}

	public VMFlavorType getVmTypes() {
		return vms;
	}

	public ClusterType getClusters() {
		return clusters;
	}

	public void setClusters(ClusterType clusterType) {
		this.clusters = clusterType;
	}

	public void showAllocation(AllocationRequest allocationRequest) {
		Request request = (Request) allocationRequest.getRequest()
				.getValue();
		if (request instanceof CloudVmAllocation) {
			CloudVmAllocation r = (CloudVmAllocation) request;
			log.debug("allocation type Cloud:");
			String ids = new String();
			for (String id : r.getClusterId()) {
				ids += id + ";";
			}
			log.debug("Cluster IDs = " + ids);
			log.debug("Image ID = " + r.getImageId());
			log.debug("VM type = " + r.getVm());
			log.debug("User ID = " + r.getUserId());

		} 

		log.debug("Name: " + allocationRequest.getRequest().getValue());
	}
	
	
    //Compare two nodes and returns the one with less remaining space on CPU
    public static class ActionComparator implements Comparator<AbstractBaseAction> {

    	@Override
        public int compare(AbstractBaseAction a1, AbstractBaseAction a2) {
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
    	
    	public int getPosition(AbstractBaseAction action) {
    		if (action instanceof PowerOnAction) {
    			return 1;
    		} else if (action instanceof MoveVMAction) {
    			return 2;
    		} else if (action instanceof LiveMigrateVMAction) {
    			return 2;
    		} else if (action instanceof PowerOffAction) {
    			return 3;
    		} else {
    			return 4;
    		}			
    	}   	
    }


	public void setCPUOvercommit(float cpuovercommit) {
		
		log.debug("setting CPU overcommit=" + cpuovercommit);
		SLAs.getSLA().get(0).getQoSConstraints().getMaxVirtualCPUPerCore().setValue(cpuovercommit);
		
	}
	
	public void setVMCPUConstraint(String VMName, int VMConsumption) {
		VMCPUConstraint.put(VMName, VMConsumption);
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

	public void setServerGroups(ServerGroupType sg) {
		this.serverGroups = sg;
	}
}
