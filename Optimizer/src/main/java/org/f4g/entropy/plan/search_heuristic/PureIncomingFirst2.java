package org.f4g.entropy.plan.search_heuristic;

import choco.kernel.common.Constant;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.ActionModels;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.DemandingSlice;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * An heuristic to branch first on the start moment of actions
 * that arrive on nodes without any outgoing actions.
 *
 * @author Fabien Hermenier
 */
public class PureIncomingFirst2 extends AbstractIntVarSelector {


    private IntDomainVar[] hoster;

    private IntDomainVar[] starts;

    private ManagedElementSet<VirtualMachine> vms;
    private int[] oldPos;

    private BitSet[] outs;

    private BitSet[] ins;

    private List<SConstraint> constraints;

    /**
     * Make a new heuristics
     *
     * @param solver  the solver to use
     * @param actions the actions to consider.
     */
    public PureIncomingFirst2(ReconfigurationProblem solver, List<ActionModel> actions, List<SConstraint> costConstraints) {
        super(solver, ActionModels.extractStarts(actions.toArray(new ActionModel[actions.size()])));
        this.pb = solver;
        this.constraints = costConstraints;
        Configuration cfg = solver.getSourceConfiguration();

        hoster = new IntDomainVar[solver.getVirtualMachineActions().size()];
        starts = new IntDomainVar[solver.getVirtualMachineActions().size()];
        this.vms = solver.getFutureRunnings().clone();
        List<VirtualMachineActionModel> vmActions = solver.getVirtualMachineActions();
        oldPos = new int[hoster.length];
        outs = new BitSet[solver.getNodes().length];
        ins = new BitSet[solver.getNodes().length];
        for (int i = 0; i < solver.getNodes().length; i++) {
            outs[i] = new BitSet();
            ins[i] = new BitSet();
        }

        for (int i = 0; i < hoster.length; i++) {
            VirtualMachineActionModel action = vmActions.get(i);
            DemandingSlice slice = action.getDemandingSlice();
            if (slice != null) {
                IntDomainVar h = vmActions.get(i).getDemandingSlice().hoster();
                IntDomainVar s = vmActions.get(i).getDemandingSlice().start();
                hoster[i] = h;
                if (s != solver.getEnd()) {
                    starts[i] = s;
                }
                VirtualMachine vm = action.getVirtualMachine();
                Node n = cfg.getLocation(vm);
                if (n == null) {
                    oldPos[i] = -1;
                } else {
                    oldPos[i] = solver.getNode(n);
                    outs[solver.getNode(n)].set(i);     //VM i was on node n
                }
            }
        }

        pos = pb.getEnvironment().makeInt(0);
        curNode = pb.getEnvironment().makeInt(-1);
    }

    private boolean first = true;

    private ReconfigurationProblem pb;

    private IStateInt pos;
    @Override
    public IntDomainVar selectVar() {
        //ChocoLogging.getSearchLogger().info("New call");
        if (first) {
            first = !first;
            Plan.logger.debug("Activate cost constraints");
            Plan.logger.debug("End:" + pb.getEnd().pretty());
            for (SConstraint sc : constraints) {
                pb.postCut(sc);
            }
            try {
                pb.propagate();
            } catch (ContradictionException e) {
                pb.setFeasible(false);
                pb.post(Constant.FALSE);
            }
        }
        for (int i = 0; i < ins.length; i++) {
            ins[i].clear();
        }

        BitSet stays = new BitSet();
        //At this moment, all the hoster of the demanding slices are computed.
        //for each node, we compute the number of incoming and outgoing
        for (int i = 0; i < hoster.length; i++) {
            if (hoster[i] != null && hoster[i].isInstantiated()) {
                int newPos = hoster[i].getVal();
                if (oldPos[i] != -1 && newPos != oldPos[i]) {
                    //The VM has move
                    ins[newPos].set(i);
                } else if (oldPos[i] != -1 && newPos == oldPos[i]) {
                    stays.set(i);
                }
            }
        }

        //TODO: start with nodes with a sufficient amount of free resources at startup
        for (int x = 0; x < outs.length; x++) {   //Node per node
            if (outs[x].cardinality() == 0) { //no outgoing VMs, can be launched directly.
                BitSet in = ins[x];
                for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
                    if (starts[i] != null && !starts[i].isInstantiated()) {
                        return starts[i];
                    }
                }
            }
        }
        //TODO: Decreasing stay at end
        //TODO: association between slice on the same node
        for (int i = stays.nextSetBit(0); i >= 0; i = stays.nextSetBit(i + 1)) {
            if (starts[i] != null && !starts[i].isInstantiated()) {
                return starts[i];
            }
        }


        return minInf();
        /*
        Take a starts(), then focus on an incoming on the node that hosted the start()
       pick first VMs start moment, store oldPos
       pick an incoming on node oldPos otherwise pick first available VM
        */
/*        int stIdx;

        if (curNode.get() < 0) { //First call, get a first VM
            stIdx = randomStartMoment();
            if (stIdx >= 0) {
                curNode.set(getOriginalLocation(stIdx));
            }
        }
        if (curNode.get() >= 0) {
            stIdx = firstIncoming(curNode.get()); //Get an incoming on the current node
            if (stIdx < 0) {  //No VM left on that node
                //ChocoLogging.getSearchLogger().info("No incoming left on " + curNode.get());
                int i = randomStartMoment(); //New VM
                if (i >= 0) {
                    stIdx = i;
                } //else { //No starts to instantiate
                    //ChocoLogging.getSearchLogger().info("Nothing left to do");
                }   //
            }

            if (stIdx >= 0) {
                //ChocoLogging.getSearchLogger().info("Looking on " + starts[stIdx].pretty() + " that must go to " + curNode.get() + " from " + oldPos[stIdx]);
                curNode.set(oldPos[stIdx]);
                //ChocoLogging.getSearchLogger().info("New curNode=" + curNode.get());
                return starts[stIdx];
            }
        }      */

        /*
        Collections.sort(vms, new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryDemand));
        //TODO: work by VM instead by node
        for (VirtualMachine vm : vms) {
            DemandingSlice s = pb.getAssociatedAction(vm).getDemandingSlice();
            if (s != null && !s.start().isInstantiated()) {
                return s.start();
            }
        } */

        /*for (int i = 0; i < starts.length; i++) {
            IntDomainVar start = starts[i];
            if (starts[i] != null && !start.isInstantiated()) {
                Plan.logger.info(starts[i].pretty());
                return start;
            }
        } */
        //ChocoLogging.getBranchingLogger().info("No more variables to instantiate here");
        //return null;
    }


    private IntDomainVar minInf() {
        IntDomainVar best = null;
        for (int i = 0; i < starts.length; i++) {
            IntDomainVar v = starts[i];
            if (v != null && !v.isInstantiated() && (best == null || best.getInf() > v.getInf())) {
                best = v;
            }
        }
        return best;
    }

    private IStateInt curNode;

    private int firstIncoming(int node) {
        for (int i = ins[node].nextSetBit(0); i >= 0; i = ins[node].nextSetBit(i + 1)) {
            if (starts[i] != null && !starts[i].isInstantiated()) {
                return i;
            }
        }
        return -1;
    }

    private int randomStartMoment() {
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] != null && !starts[i].isInstantiated()) {
                return i;
            }
        }
        return -1;
    }

    private int getOriginalLocation(int i) {
        return oldPos[i];
    }

}