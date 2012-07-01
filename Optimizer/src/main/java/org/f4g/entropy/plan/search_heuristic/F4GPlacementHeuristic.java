

package org.f4g.entropy.plan.search_heuristic;

import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valselector.MinVal;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModels;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.search.*;
import gnu.trove.TLongIntHashMap;

import org.apache.log4j.Logger;
import org.f4g.entropy.plan.F4GPlanner;

import java.util.Collections;
import java.util.List;
import entropy.plan.choco.actionModel.*;

/**
 * A placement heuristic focused on each VM.
 * First place the VMs, then plan the changes.
 *
 * @author Fabien Hermenier
 */
public class F4GPlacementHeuristic implements F4GCorePlanHeuristic {

	public Logger log;
	
    /**
     * Make a new placement heuristic.
     *
     */
    public F4GPlacementHeuristic(){
    	log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * To compare VMs in a descending order, wrt. their memory consumption.
     */
    private VirtualMachineComparator dsc = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);

    @Override
    public void add(F4GPlanner plan) {
    	ReconfigurationProblem rp = plan.getModel();
        Configuration src = rp.getSourceConfiguration();

        //Get the VMs to move
        ManagedElementSet<VirtualMachine> onBadNodes = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<VirtualMachine> onGoodNodes = src.getRunnings().clone();
        ManagedElementSet<VirtualMachine> vmsToRun = src.getWaitings().clone();

        vmsToRun.removeAll(rp.getFutureWaitings());
                
        for (Node n : Configurations.futureOverloadedNodes(src)) {
            onBadNodes.addAll(src.getRunnings(n));
        }

        onBadNodes.addAll(src.getSleepings());
        onGoodNodes.removeAll(onBadNodes);

        Collections.sort(onGoodNodes, dsc);
        Collections.sort(onBadNodes, dsc);

        List<VirtualMachineActionModel> goodActions = rp.getAssociatedActions(onGoodNodes);
        List<VirtualMachineActionModel> badActions = rp.getAssociatedActions(onBadNodes);
        List<VirtualMachineActionModel> runActions = rp.getAssociatedActions(vmsToRun);

        
        //add heuristic for groups
        //addVMGroup(rp);

        ManagedElementSet<VirtualMachine> relocalisables = plan.getModel().getFutureRunnings();
	    TLongIntHashMap oldLocation = new TLongIntHashMap(relocalisables.size());
	     
        for (VirtualMachine vm : relocalisables) {
            int idx = rp.getVirtualMachine(vm);
            VirtualMachineActionModel a = rp.getAssociatedVirtualMachineAction(idx);
            if (a.getClass() == MigratableActionModel.class || a.getClass() == ResumeActionModel.class || a.getClass() == ReInstantiateActionModel.class) {
                oldLocation.put(a.getDemandingSlice().hoster().getIndex(), rp.getCurrentLocation(idx));
            }
        }
        
        //Now the VMs associated to group of nodes
        //ManagedElementSet<VirtualMachine> inGroup = new DefaultManagedElementSet<VirtualMachine>();
        if (plan.getQueue().size() != 0) {

        	//add heuristic for excluded VMs
        	//TODO: reactivate
            //addExclusion(rp);    

            //addInGroupAction(rp);

            //add heuritic for fixing broken constraints
            addStayFirst(plan, badActions, oldLocation);
           
        }

        //add heuritic for runs
        addStayFirst(plan, runActions, oldLocation);
        
        //add heuristique to pack VMs on energy criteria
        addEnergyPacking(rp);
        
        //add heuritic for remaining VMs
        addStayFirst(plan, goodActions, oldLocation);
        
        //add heuristic for shutdowns
        EmptyNodeVarSelector selectShutdown = new EmptyNodeVarSelector(rp, rp.getSourceConfiguration().getAllNodes());
        rp.addGoal(new AssignVar(selectShutdown, new MinVal()));
                
    }

	private void addEnergyPacking(ReconfigurationProblem rp) {
		
		//selector to select the VM which is on the lowest loaded node 
		LowestLoadHosterVarSelector selectVM = new LowestLoadHosterVarSelector(rp, rp.getSourceConfiguration().getAllNodes());
		
		//selector to select the server with the highest load
		ConsolidateValSelector selectServer = new ConsolidateValSelector(rp, rp.getSourceConfiguration().getAllNodes());
       
        //consolidate first: move VMs to low load nodes to high load nodes
        rp.addGoal(new AssignVar(selectVM, selectServer));
		
	}

	private void addStayFirst(F4GPlanner plan, List<VirtualMachineActionModel> actions, TLongIntHashMap oldLocation) {
		 	        
		HosterVarSelector select = new HosterVarSelector(plan.getModel(), ActionModels.extractDemandingSlices(actions));
		plan.getModel().addGoal(new AssignVar(select, new StayFirstSelector2(plan.getModel(), oldLocation, plan.getPackingConstraintClass(), StayFirstSelector2.Option.wfMem)));
	}

//	private void addInGroupAction(ReconfigurationProblem rp) {
//		for (ManagedElementSet<VirtualMachine> vms : rp.getVMGroups()) {
//		    ManagedElementSet<VirtualMachine> sorted = vms.clone();
//		    Collections.sort(sorted, dsc);
//		    List<VirtualMachineActionModel> inGroupActions = rp.getAssociatedActions(sorted);
//		    addStayFirst(rp, inGroupActions);
//		}
//	}

//	private void addExclusion(ReconfigurationProblem rp) {
//		//Get the VMs to move for exclusion issue
//		ManagedElementSet<VirtualMachine> vmsToExlude = rp.getSourceConfiguration().getAllVirtualMachines().clone();
//		Collections.sort(vmsToExlude, dsc);
//		rp.addGoal(new AssignVar(new ExcludedVirtualMachines(rp, rp.getSourceConfiguration(), vmsToExlude), new StayFirstSelector2(rp, rp.getSatisfyDSlicesHeightConstraint(), StayFirstSelector2.Option.wfMem)));
//	}

	private void addVMGroup(ReconfigurationProblem rp) {
		//Go for the VMgroup variable
        VMGroupVarSelector vmGrp = new VMGroupVarSelector(rp);
        rp.addGoal(new AssignVar(vmGrp, new NodeGroupSelector(rp, NodeGroupSelector.Option.bfMem)));
	}
}
