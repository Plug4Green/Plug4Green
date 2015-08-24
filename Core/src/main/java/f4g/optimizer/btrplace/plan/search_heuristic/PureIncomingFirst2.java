//package f4g.optimizer.entropy.plan.search_heuristic;
//
//import choco.kernel.common.Constant;
//import choco.kernel.common.logging.ChocoLogging;
//import choco.kernel.memory.IStateInt;
//import choco.kernel.solver.ContradictionException;
//import choco.kernel.solver.constraints.SConstraint;
//import choco.kernel.solver.search.integer.AbstractIntVarSelector;
//import choco.kernel.solver.variables.integer.IntDomainVar;
//import btrplace.configuration.*;
//import btrplace.plan.Plan;
//import btrplace.plan.choco.ReconfigurationProblem;
//import btrplace.plan.choco.actionModel.ActionModel;
//import btrplace.plan.choco.actionModel.ActionModels;
//import btrplace.plan.choco.actionModel.VirtualMachineActionModel;
//import btrplace.plan.choco.actionModel.slice.DemandingSlice;
//import f4g.optimizer.btrplace.plan.F4GPlanner;
//
//import java.util.BitSet;
//import java.util.Collections;
//import java.util.List;
//
///**
// * An heuristic to branch first on the start moment of actions
// * that arrive on nodes without any outgoing actions.
// *
// * @author Fabien Hermenier
// */
//public class PureIncomingFirst2 extends AbstractIntVarSelector {
//
//
//    private IntDomainVar[] hoster;
//
//    private IntDomainVar[] starts;
//
//    private ManagedElementSet<VirtualMachine> vms;
//    private int[] oldPos;
//
//    private BitSet[] outs;
//
//    private BitSet[] ins;
//
//    private List<SConstraint> constraints;
//
//    /**
//     * Make a new heuristics
//     *
//     * @param solver  the solver to use
//     * @param actions the actions to consider.
//     */
//    public PureIncomingFirst2(F4GPlanner planner,ReconfigurationProblem solver, List<ActionModel> actions) {
//        super(solver, ActionModels.extractStarts(actions.toArray(new ActionModel[actions.size()])));
//        this.pb = solver;
//        this.planner = planner;
//        Configuration cfg = solver.getSourceConfiguration();
//
//        hoster = new IntDomainVar[solver.getVirtualMachineActions().size()];
//        starts = new IntDomainVar[solver.getVirtualMachineActions().size()];
//        this.vms = solver.getFutureRunnings().clone();
//        List<VirtualMachineActionModel> vmActions = solver.getVirtualMachineActions();
//        oldPos = new int[hoster.length];
//        outs = new BitSet[solver.getNodes().length];
//        ins = new BitSet[solver.getNodes().length];
//        for (int i = 0; i < solver.getNodes().length; i++) {
//            outs[i] = new BitSet();
//            ins[i] = new BitSet();
//        }
//
//        for (int i = 0; i < hoster.length; i++) {
//            VirtualMachineActionModel action = vmActions.get(i);
//            DemandingSlice slice = action.getDemandingSlice();
//            if (slice != null) {
//                IntDomainVar h = vmActions.get(i).getDemandingSlice().hoster();
//                IntDomainVar s = vmActions.get(i).getDemandingSlice().start();
//                hoster[i] = h;
//                if (s != solver.getEnd()) {
//                    starts[i] = s;
//                }
//                VirtualMachine vm = action.getVirtualMachine();
//                Node n = cfg.getLocation(vm);
//                if (n == null) {
//                    oldPos[i] = -1;
//                } else {
//                    oldPos[i] = solver.getNode(n);
//                    outs[solver.getNode(n)].set(i);     //VM i was on node n
//                }
//            }
//        }
//
//        pos = pb.getEnvironment().makeInt(0);
//        curNode = pb.getEnvironment().makeInt(-1);
//    }
//
//    private boolean first = true;
//
//    private ReconfigurationProblem pb;
//
//    private IStateInt pos;
//
//    private F4GPlanner planner;
//
//    @Override
//    public IntDomainVar selectVar() {
//        if (first) {
//            first = !first;
//            planner.activateQualityOrientedConstraints();
//        }
//
//
//
//        for (int i = 0; i < ins.length; i++) {
//            ins[i].clear();
//        }
//
//        BitSet stays = new BitSet();
//        //At this moment, all the hoster of the demanding slices are computed.
//        //for each node, we compute the number of incoming and outgoing
//        for (int i = 0; i < hoster.length; i++) {
//            if (hoster[i] != null && hoster[i].isInstantiated()) {
//                int newPos = hoster[i].getVal();
//                if (oldPos[i] != -1 && newPos != oldPos[i]) {
//                    //The VM has move
//                    ins[newPos].set(i);
//                } else if (oldPos[i] != -1 && newPos == oldPos[i]) {
//                    stays.set(i);
//                }
//            }
//        }
//
//        //TODO: start with nodes with a sufficient amount of free resources at startup
//        for (int x = 0; x < outs.length; x++) {   //Node per node
//            if (outs[x].cardinality() == 0) { //no outgoing VMs, can be launched directly.
//                BitSet in = ins[x];
//                for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
//                    if (starts[i] != null && !starts[i].isInstantiated()) {
//                        return starts[i];
//                    }
//                }
//            }
//        }
//        //TODO: Decreasing stay at end
//        //TODO: association between slice on the same node
//        for (int i = stays.nextSetBit(0); i >= 0; i = stays.nextSetBit(i + 1)) {
//            if (starts[i] != null && !starts[i].isInstantiated()) {
//                return starts[i];
//            }
//        }
//
//        /*
//        Take a starts(), then focus on an incoming on the node that hosted the start()
//       pick first VMs start moment, store oldPos
//       pick an incoming on node oldPos otherwise pick first available VM
//        */
//      /* int stIdx = -1;
//
//        if (curNode.get() < 0) { //First call, get a first VM
//            stIdx = randomStartMoment();
//            if (stIdx >= 0) {
//                curNode.set(getOriginalLocation(stIdx));
//            }
//        }
//        if (curNode.get() >= 0) {
//            stIdx = firstIncoming(curNode.get()); //Get an incoming on the current node
//            if (stIdx < 0) {  //No VM left on that node
//                //ChocoLogging.getSearchLogger().info("No incoming left on " + curNode.get());
//                int i = randomStartMoment(); //New VM
//                if (i >= 0) {
//                    stIdx = i;
//                }
//            }
//
//            if (stIdx >= 0) {
//                ChocoLogging.getSearchLogger().info("Looking on " + starts[stIdx].pretty() + " that must go to " + curNode.get() + " from " + oldPos[stIdx]);
//                curNode.set(oldPos[stIdx]);
//                ChocoLogging.getSearchLogger().info("New curNode=" + curNode.get());
//                return starts[stIdx];
//            }
//        }
//        return null;   */
//
//        return  minInf();
//    }
//
//    private void activateCostConstraints() {
//    }
//
//
//    private IntDomainVar minInf() {
//        IntDomainVar best = null;
//        VirtualMachine bestVM = null;
//        int x = 0;
//        for (int i = 0; i < starts.length; i++) {
//            IntDomainVar v = starts[i];
//            VirtualMachine vm = vms.get(i);
//            if (v != null && !v.isInstantiated() &&
//                    (best == null || best.getInf() > v.getInf() || (best.getInf() == v.getInf() && vm.getMemoryDemand() > bestVM.getMemoryDemand()))) {
//                bestVM = vm;
//                best = v;
//                x = i;
//            }
//        }
//        if (best != null) {
//            ChocoLogging.getSearchLogger().info("Looking on " + best.pretty() + " that must go to " + hoster[x].getVal() + " from " + oldPos[x]);
//        }
//        return best;
//    }
//
//    private IStateInt curNode;
//
//    private int firstIncoming(int node) {
//        int x = -1;
//        IntDomainVar best = null;
//        VirtualMachine bestVM = null;
//        for (int i = ins[node].nextSetBit(0); i >= 0; i = ins[node].nextSetBit(i + 1)) {
//            IntDomainVar v = starts[i];
//            VirtualMachine vm = vms.get(i);
//            if (v != null && !v.isInstantiated() &&
//                    (x == -1 || (best.getInf() > v.getInf() || (best.getInf() == v.getInf() && vm.getMemoryDemand() > bestVM.getMemoryDemand())))
//                    ) {
//                bestVM = vm;
//                best = v;
//                x = i;
//            }
//        }
//        return x;
//    }
//
//    private int randomStartMoment() {
//        for (int i = 0; i < starts.length; i++) {
//            if (starts[i] != null && !starts[i].isInstantiated()) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    private int getOriginalLocation(int i) {
//        return oldPos[i];
//    }
//
//}
