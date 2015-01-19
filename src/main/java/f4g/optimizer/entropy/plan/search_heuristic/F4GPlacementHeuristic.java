

package f4g.optimizer.entropy.plan.search_heuristic;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.scheduler.choco.transition.Transition;
import org.btrplace.scheduler.choco.transition.TransitionUtils;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.SliceUtils;
import org.btrplace.scheduler.choco.constraint.CObjective;
import org.btrplace.scheduler.choco.constraint.mttr.MovementGraph;
import org.btrplace.scheduler.choco.constraint.mttr.MovingVMs;
import org.btrplace.scheduler.choco.constraint.mttr.RandomVMPlacement;
import org.btrplace.scheduler.choco.constraint.mttr.StartOnLeafNodes;
import org.btrplace.scheduler.choco.constraint.mttr.VMPlacementUtils;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.VariableSelector;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;

import org.apache.log4j.Logger;
import gnu.trove.map.hash.TLongIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class F4GPlacementHeuristic {

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
    //private VirtualMachineComparator dsc = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);

    private void injectPlacementHeuristic(ReconfigurationProblem p, CObjective objective) { //, IntVar cost

        Model mo = p.getSourceModel();
        Mapping map = mo.getMapping();

        OnStableNodeFirst schedHeuristic = new OnStableNodeFirst(p, objective);

        //Get the VMs to place
        Set<VM> onBadNodes = new HashSet<>(p.getManageableVMs());

        //Get the VMs that runs and have a pretty low chances to move
        Set<VM> onGoodNodes = map.getRunningVMs(map.getOnlineNodes());
        onGoodNodes.removeAll(onBadNodes);

        VMTransition[] goodActions = p.getVMActions(onGoodNodes);
        VMTransition[] badActions = p.getVMActions(onBadNodes);

        Solver s = p.getSolver();

        //Get the VMs to move for exclusion issue
        Set<VM> vmsToExclude = new HashSet<>(p.getManageableVMs());
        for (Iterator<VM> ite = vmsToExclude.iterator(); ite.hasNext(); ) {
            VM vm = ite.next();
            if (!(map.isRunning(vm) && p.getFutureRunningVMs().contains(vm))) {
                ite.remove();
            }
        }
        List<AbstractStrategy> strategies = new ArrayList<>();

        Map<IntVar, VM> pla = VMPlacementUtils.makePlacementMap(p);
        if (!vmsToExclude.isEmpty()) {
            List<VMTransition> actions = new LinkedList<>();
            //Get all the involved slices
            for (VM vm : vmsToExclude) {
                if (p.getFutureRunningVMs().contains(vm)) {
                    actions.add(p.getVMAction(vm));
                }
            }
            IntVar[] scopes = SliceUtils.extractHoster(TransitionUtils.getDSlices(actions));

            strategies.add(new IntStrategy(scopes, new MovingVMs(p, map, actions), new RandomVMPlacement(p, pla, true)));
        }

        placeVMs(strategies, badActions, schedHeuristic, pla, p);
        placeVMs(strategies, goodActions, schedHeuristic, pla, p);

        //VMs to run
/*        Set<VM> vmsToRun = new HashSet<>(map.getReadyVMs());
        vmsToRun.removeAll(p.getFutureReadyVMs());

        VMTransition[] runActions = p.getVMActions(vmsToRun);

        placeVMs(strategies, runActions, schedHeuristic, pla);
  */
        
        if (p.getNodeActions().length > 0) {
            //Boot some nodes if needed
            strategies.add(new IntStrategy(TransitionUtils.getStarts(p.getNodeActions()), new InputOrder<>(), new IntDomainMin()));
        }

        ///SCHEDULING PROBLEM
        MovementGraph gr = new MovementGraph(p);
        strategies.add(new IntStrategy(SliceUtils.extractStarts(TransitionUtils.getDSlices(p.getVMActions())), new StartOnLeafNodes(p, gr), new IntDomainMin()));
        strategies.add(new IntStrategy(schedHeuristic.getScope(), schedHeuristic, new IntDomainMin()));

        //At this stage only it matters to plug the cost constraints
        //strategies.add(new IntStrategy(new IntVar[]{p.getEnd(), cost}, new InputOrder<>(), new IntDomainMin()));

        s.getSearchLoop().set(new StrategiesSequencer(s.getEnvironment(), strategies.toArray(new AbstractStrategy[strategies.size()])));
    }
    
    /*
     * Try to place the VMs associated on the actions in a random node while trying first to stay on the current node
     */
    private void placeVMs(List<AbstractStrategy> strategies, VMTransition[] actions, OnStableNodeFirst schedHeuristic, Map<IntVar, VM> map, ReconfigurationProblem rp) {
        if (actions.length > 0) {
            IntVar[] hosts = SliceUtils.extractHoster(TransitionUtils.getDSlices(actions));
            if (hosts.length > 0) {
                strategies.add(new IntStrategy(hosts, new HostingVariableSelector(schedHeuristic), new RandomVMPlacement(rp, map, true)));
            }
        }
    }
    
//    @Override
//    public void add(ReconfigurationProblem rp) {
//
//        Configuration src = rp.getSourceConfiguration();
//        rp.clearGoals();
//        //Get the VMs to move
//        ManagedElementSet<VirtualMachine> onBadNodes = new SimpleManagedElementSet<VirtualMachine>();
//        ManagedElementSet<VirtualMachine> onGoodNodes = src.getRunnings().clone();
//        ManagedElementSet<VirtualMachine> vmsToRun = src.getWaitings().clone();
//
//        vmsToRun.removeAll(rp.getFutureWaitings());
//                
//        for (Node n : Configurations.futureOverloadedNodes(src)) {
//            onBadNodes.addAll(src.getRunnings(n));
//        }
//
//        onBadNodes.addAll(src.getSleepings());
//        onGoodNodes.removeAll(onBadNodes);
//
//        Collections.sort(onGoodNodes, dsc);
//        Collections.sort(onBadNodes, dsc);
//
//        List<VirtualMachineActionModel> goodActions = rp.getAssociatedActions(onGoodNodes);
//        List<VirtualMachineActionModel> badActions = rp.getAssociatedActions(onBadNodes);
//        List<VirtualMachineActionModel> runActions = rp.getAssociatedActions(vmsToRun);
//
//        
//        //add heuristic for groups
//        //addVMGroup(rp);
//
//        ManagedElementSet<VirtualMachine> relocalisables = plan.getModel().getFutureRunnings();
//	    TLongIntHashMap oldLocation = new TLongIntHashMap(relocalisables.size());
//	     
//        for (VirtualMachine vm : relocalisables) {
//            int idx = rp.getVirtualMachine(vm);
//            VirtualMachineActionModel a = rp.getAssociatedVirtualMachineAction(idx);
//            if (a.getClass() == MigratableActionModel.class || a.getClass() == ResumeActionModel.class || a.getClass() == ReInstantiateActionModel.class) {
//                oldLocation.put(a.getDemandingSlice().hoster().getIndex(), rp.getCurrentLocation(idx));
//            }
//        }
//        
//        //add heuritic for fixing broken constraints
//        addStayFirst(plan, badActions, oldLocation);
//        
//        //add heuritic for runs
//        addStayFirst(plan, runActions, oldLocation);
//        
//        addEnergyPacking(rp, oldLocation, plan, goodActions);
//
//    
//        ///SCHEDULING PROBLEM
//        List<ActionModel> actions = new ArrayList<ActionModel>();
//        for (VirtualMachineActionModel vma : rp.getVirtualMachineActions()) {
//            actions.add(vma);
//        }
//
//
//        rp.addGoal(new AssignOrForbidIntVarVal(new PureIncomingFirst2(plan, rp, actions), new MinVal()));
//
//        
//        EmptyNodeVarSelector selectShutdown = new EmptyNodeVarSelector(rp, rp.getSourceConfiguration().getAllNodes());
//        rp.addGoal(new AssignVar(selectShutdown, new MinVal()));
//
//        rp.addGoal(new AssignVar(new StaticVarOrder(rp, new IntDomainVar[]{rp.getEnd()}), new MinVal()));
//
//    }

//	private void addEnergyPacking(ReconfigurationProblem rp, TLongIntHashMap oldLocation, ReconfigurationProblem plan, List<Transition> goodActions) {
//		
//        //consolidate first: move VMs to low load nodes to high load nodes
//        rp.addGoal(new SimpleVMPacking(rp, rp.getSourceModel().get.getSourceConfiguration().getAllNodes()) ); //new AssignVar(selectVM, selectServer));
//		
//        //add heuritic for remaining VMs
//        addStayFirst(plan, goodActions, oldLocation);
//	}
//
//	private void addStayFirst(F4GPlanner plan, List<VirtualMachineActionModel> actions, TLongIntHashMap oldLocation) {
//		 	        
//		HosterVarSelector select = new HosterVarSelector(plan.getModel(), ActionModels.extractDemandingSlices(actions));
//		plan.getModel().addGoal(new AssignVar(select, new StayFirstSelector2(plan.getModel(), oldLocation, plan.getPackingConstraintClass(), StayFirstSelector2.Option.bfMem)));
//	}

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

//	private void addVMGroup(ReconfigurationProblem rp) {
//		//Go for the VMgroup variable
//        VMGroupVarSelector vmGrp = new VMGroupVarSelector(rp);
//        rp.addGoal(new AssignVar(vmGrp, new NodeGroupSelector(rp, NodeGroupSelector.Option.bfMem)));
//	}
}
