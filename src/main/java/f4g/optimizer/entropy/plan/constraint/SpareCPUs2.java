
package org.f4g.entropy.plan.constraint;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.Chocos;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import static choco.cp.solver.CPSolver.minus;
import static choco.cp.solver.CPSolver.sum;

/**
 *  New implementation for SpareCPU that use the core-RP extension {@link VcpuPcpuMapping}.
 * This constraint ensures all that there will be at least a certain amount of unused pCPU
 * among all the servers given as a parameter.
 *
 * This constraint is only meaningful if there is a constraint restricting the number of vCPUs per
 * pCPU.
 *  @author Fabien Hermenier
 *
 */
public class SpareCPUs2 implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    //the global spare CPU in number of CPUs
    public int minSpareCPU;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put offline if they don't host any running VM.
     * @param myMinSpareCPU the number of pCPU that should be available
     */
    public SpareCPUs2(ManagedElementSet<Node> nodes, int myMinSpareCPU) {
        this.nodes = nodes;
        minSpareCPU = myMinSpareCPU;
    }
    

    @Override
    public void inject(ReconfigurationProblem core) {
    	
    	VcpuPcpuMapping mapping = DefaultVcpuPcpuMapping.getInstances();
        if (mapping == null) {
            mapping = new DefaultVcpuPcpuMapping(core);
        }

    	IntDomainVar[] freePcpu = new IntDomainVar[nodes.size()];

        int i = 0;
		for (Node n : nodes) {
            IntDomainVar nbFree;
			if (core.getFutureOfflines().contains(n)) { //Offline, no free pCPU
				nbFree = core.createIntegerConstant("", 0);
			} else if (core.getFutureOnlines().contains(n)) { //Online, equals to the capacity - the nb. of used
				nbFree = core.createBoundIntVar("freePcpu(" + n +")", 0, n.getNbOfCPUs());
                core.post(core.eq(nbFree, minus(n.getNbOfCPUs(), mapping.getPcpuUsage(n))));
			} else {
                IntDomainVar nbFreePure = core.createBoundIntVar("freePcpuTmp(" + n +")", 0, n.getNbOfCPUs());
                nbFree = core.createBoundIntVar("freePcpu(" + n +")", 0, n.getNbOfCPUs());
                core.post(core.eq(nbFreePure, minus(n.getNbOfCPUs(), mapping.getPcpuUsage(n))));
				ManageableNodeActionModel a = (ManageableNodeActionModel) core.getAssociatedAction(n);
				core.post(core.eq(nbFree, Chocos.mult(core, a.getState(), nbFreePure))); //0 if offline, nbOfCPUs() - usage() otherwise
			}
            freePcpu[i++] = nbFree;
		}
		core.post(core.leq(minSpareCPU, sum(freePcpu)));
    }


    @Override
    public boolean isSatisfied(Configuration cfg) {
        //TODO: Unable to calculate as we don't know how many pCPU is used by a VM.
/*        int nbSpare = 0;
        for (Node n : nodes) {
            if (cfg.isOnline(n)) {
                int nb = 0;
                for (VirtualMachine vm : cfg.getRunnings(n)) {
                    nb += vm.getNbOfCPUs();
                }
                nbSpare += (n.getCPUCapacity() - nb);
            }
        }
        return nbSpare >= (minSpareCPU * overbooking); */
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
        b.append("<constraint id=\"SpareCPUs\">");
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
        return new StringBuilder("SpareCPUs2(").append(nodes).append(", ").append(minSpareCPU).append(")").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpareCPUs2 q = (SpareCPUs2) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "noIdleOnline".hashCode() * 31 + nodes.hashCode();
    }
}
