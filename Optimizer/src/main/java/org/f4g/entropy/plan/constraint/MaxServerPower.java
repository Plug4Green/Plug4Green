

package org.f4g.entropy.plan.constraint;

import org.f4g.entropy.configuration.F4GNode;

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

public class MaxServerPower implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes
     */
    public MaxServerPower(ManagedElementSet<Node> nodes, int maxServerPower) {
        this.nodes = nodes;
        this.maxServerPower = maxServerPower;
    }
    
    private int maxServerPower;

    @Override
    public void inject(ReconfigurationProblem core) {
    	
        Cardinalities c = PackingBasedCardinalities.getInstances();
        if (c == null) {
            c = new PackingBasedCardinalities(core, 50);
        }
        for (int i = 0; i < nodes.size(); i++) {
        	F4GNode f4gNode = (F4GNode)nodes.get(i);		

            IntDomainVar card = c.getCardinality(nodes.get(i));
            IntDomainVar power = core.createBoundIntVar("ServerPower" + i, 0, Choco.MAX_UPPER_BOUND);
			core.post(core.eq(core.plus(core.mult(card, f4gNode.getPperVM()), f4gNode.getPIdle()), power));
			core.post(core.leq(power, maxServerPower));
        }

    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
    	for (Node n : nodes) {
    		F4GNode f4gNode = (F4GNode)n;		
    		int power = f4gNode.getPIdle() + cfg.getRunnings(n).size() * f4gNode.getPperVM();
    		if (power > maxServerPower) 
    			return false;
        }
        return true;
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
        b.append("<constraint id=\"MaxServerPower\">");
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
        return new StringBuilder("MaxServerPower(").append(nodes).append(',').append(maxServerPower).append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MaxServerPower q = (MaxServerPower) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "MaxServerPower".hashCode() * 31 + nodes.hashCode();
    }
}
