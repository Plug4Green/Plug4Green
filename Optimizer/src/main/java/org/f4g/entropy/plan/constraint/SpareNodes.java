

package org.f4g.entropy.plan.constraint;

import static choco.cp.solver.CPSolver.minus;
import choco.Choco;
import choco.cp.solver.constraints.integer.bool.BooleanFactory;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.xml.XmlVJobSerializer;

public class SpareNodes implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put offline if they don't host any running VM.
     */
    public SpareNodes(ManagedElementSet<Node> nodes, int minSpareNodes) {
        this.nodes = nodes;
        this.minSpareNodes = minSpareNodes;
    }
    
    private int minSpareNodes;

    @Override
    public void inject(ReconfigurationProblem core) {
    	
//    	IntDomainVar[] free = new IntDomainVar[nodes.size()];
//		for (int i = 0; i<nodes.size(); i++) {
//        	Node n = nodes.get(i);
//        	if (core.getFutureOfflines().contains(n)) {
//        		free[i] = core.createIntegerConstant("", 0);
//			} else if (core.getFutureOnlines().contains(n)) {
//				free[i] = core.createBoundIntVar("spareCPU" + i, 0, Choco.MAX_UPPER_BOUND);
//				//core.post(core.eq(free[i], minus((int)(n.getNbOfCPUs() * overbooking), NbVCPU)));
//			} else {
//            if (!core.getFutureOnlines().contains(n) && !core.getFutureOfflines().contains(n)) {
//                ManageableNodeActionModel a = (ManageableNodeActionModel) core.getAssociatedAction(n);
//                IntDomainVar unused = core.createBooleanVar("unused(" + n.getName() + ')');
//                core.post(ReifiedFactory.builder(unused, core.eq(core.getUsedMem(n), 0), core));
//                core.post(ReifiedFactory.builder(free[i], BooleanFactory.and(a.getState(), unused), core));
//            }
			
            
        //}
        //core.post(core.leq(minSpareNodes, core.sum(free)));
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        int freeNodes = 0;
    	for (Node n : nodes) {
            if (cfg.isOnline(n) && cfg.getRunnings(n).isEmpty()) {
            	freeNodes++;
            }
        }
        return freeNodes >= minSpareNodes;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return empty;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return nodes;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        //All the VMs on the nodes sadly
        ManagedElementSet<VirtualMachine> all = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : nodes) {
            all.addAll(cfg.getRunnings(n));
        }
        return all;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder();
        b.append("<constraint id=\"SpareNodes\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        
        return null;
    }

    @Override
    public Type getType() {
        return Type.relative;
    }

    @Override
    public String toString() {
        return new StringBuilder("SpareNodes(").append(nodes).append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpareNodes q = (SpareNodes) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "SpareNodes".hashCode() * 31 + nodes.hashCode();
    }
}
