
package org.f4g.entropy.plan.constraint;

import static choco.cp.solver.CPSolver.*;

import choco.Choco;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import static entropy.plan.choco.Chocos.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/**
 * 
 *
 */
public class SpareCPUCapacities implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    //the global spare CPU in %
    public int minSpareCPU;


    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put offline if they don't host any running VM.
     */
    public SpareCPUCapacities(ManagedElementSet<Node> nodes, int myMinSpareCPU) {
        this.nodes = nodes;
        minSpareCPU = myMinSpareCPU;
        
    }
    

    @Override
    public void inject(ReconfigurationProblem core) {
    	
		IntDomainVar[] spareCPU = new IntDomainVar[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			if (core.getFutureOfflines().contains(n)) {
				spareCPU[i] = core.createIntegerConstant("", 0);
			} else if (core.getFutureOnlines().contains(n)) {
				spareCPU[i] = core.createBoundIntVar("SpareCPUCapacities" + i, 0, Choco.MAX_UPPER_BOUND);
				//TODO: correct trick: we multiply by 2 the used CPU because most VMs of SLA have a 
				//consumption > 2, to find the number os VCPUs
				core.post(core.eq(spareCPU[i], minus(n.getCPUCapacity(), mult(core, core.getUsedCPU(n), 2))));
			} else {
				spareCPU[i] = core.createBoundIntVar("SpareCPUCapacities" + i, 0,	Choco.MAX_UPPER_BOUND);
				ManageableNodeActionModel a = (ManageableNodeActionModel) core.getAssociatedAction(n);

				IntDomainVar tmpSpare = core.createBoundIntVar("", 0, Choco.MAX_UPPER_BOUND);
				core.post(core.eq(tmpSpare, minus(n.getCPUCapacity(), mult(core, core.getUsedCPU(n), 2))));
				core.post(core.eq(spareCPU[i], mult(core, a.getState(), tmpSpare)));
			}

		}
		core.post(core.leq(minSpareCPU, sum(spareCPU)));
		
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
    	int spareCPU = 0;
        for (Node n : nodes) {
        	int CPUOccupied = 0;
        	for(VirtualMachine vm : cfg.getRunnings(n)){
        		CPUOccupied += vm.getCPUConsumption();
        	}
        	spareCPU += n.getCPUCapacity() - CPUOccupied;
            
        }
        if (spareCPU < minSpareCPU)
            return false;
        else
        	return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return new SimpleManagedElementSet<VirtualMachine>();
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
        b.append("<constraint id=\"SpareCPUCapacities\">");
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
        return new StringBuilder("SpareCPUCapacities(").append(nodes).append(", ").append(minSpareCPU).append("%)").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpareCPUCapacities q = (SpareCPUCapacities) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "SpareCPUCapacities".hashCode() * 31 + nodes.hashCode();
    }
}
