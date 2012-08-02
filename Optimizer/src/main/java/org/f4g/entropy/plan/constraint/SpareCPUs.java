
package org.f4g.entropy.plan.constraint;

import choco.Choco;
import choco.cp.solver.constraints.integer.Absolute;
import choco.cp.solver.constraints.integer.MaxXYZ;
import choco.cp.solver.constraints.integer.MinXYZ;
import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.cp.solver.variables.integer.BoolVarNot;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.Chocos;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.constraint.pack.FastBinPacking;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static choco.cp.solver.CPSolver.minus;
import static choco.cp.solver.CPSolver.sum;

/**
 * 
 *
 */
public class SpareCPUs implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    //the global spare CPU in number of CPUs
    public int minSpareCPU;
    int maximum_capacity = 1000;
    float overbooking = 1;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put offline if they don't host any running VM.
     * @param myOverbooking 
     */
    public SpareCPUs(ManagedElementSet<Node> nodes, int myMinSpareCPU, float myOverbooking) {
        this.nodes = nodes;
        minSpareCPU = myMinSpareCPU;
        overbooking = myOverbooking;
        
    }
    

    @Override
    public void inject(ReconfigurationProblem core) {
    	
    	
    	
    	IntDomainVar[] NbVCPUs = getNbVCPUs(core);
    	
    	IntDomainVar[] spareCPU = new IntDomainVar[nodes.size()];
	    
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			IntDomainVar NbVCPU = NbVCPUs[core.getNode(n)];
						
			if (core.getFutureOfflines().contains(n)) {
				spareCPU[i] = core.createIntegerConstant("", 0);
			} else if (core.getFutureOnlines().contains(n)) {
				spareCPU[i] = core.createBoundIntVar("spareCPU" + i, 0, Choco.MAX_UPPER_BOUND);
				core.post(core.eq(spareCPU[i], minus((int)(n.getNbOfCPUs() * overbooking), NbVCPU)));
			} else {
				spareCPU[i] = core.createBoundIntVar("spareCPU" + i, 0,	Choco.MAX_UPPER_BOUND);
				ManageableNodeActionModel a = (ManageableNodeActionModel) core.getAssociatedAction(n);

				//compute the number of free CPUs, can be negative in case of overbooking
				IntDomainVar rawSpareCPU = core.createBoundIntVar("rawSpareCPU" + i, -1000, Choco.MAX_UPPER_BOUND);
				core.post(core.eq(rawSpareCPU, minus((int)(n.getNbOfCPUs() * overbooking), NbVCPU)));
				//the same floored to zero
				IntDomainVar flooredSpareCPU = core.createBoundIntVar("flooredSpareCPU" + i, 0, Choco.MAX_UPPER_BOUND);
				core.post(new MaxXYZ(core.createIntegerConstant("", 0), rawSpareCPU, flooredSpareCPU));
				//spareCPU is zero for a
				core.post(core.eq(spareCPU[i], Chocos.mult(core, a.getState(), flooredSpareCPU)));
			}
		}
		core.post(core.leq(minSpareCPU, sum(spareCPU)));
    }

    //get the variables denoting the number of virtual CPUs on each servers
	public IntDomainVar[] getNbVCPUs(ReconfigurationProblem core) {

		ManagedElementSet<VirtualMachine> vms = core.getFutureRunnings().clone(); 
		ManagedElementSet<Node> allNodes = core.getSourceConfiguration().getAllNodes();

		//myAssigns contains the possible hosters for each VM 
	    IntDomainVar[] myAssigns = new IntDomainVar[vms.size()]; 
	    
	    //Sort in descending order according to number of needed CPU
        Collections.sort(vms, new VirtualMachineComparator(false, ResourcePicker.VMRc.nbOfCPUs));
        
	    //vCPUs is a constant list which contains the number of VCPU asked by each VM 
	    List<IntDomainVar> vCPUSs = new ArrayList<IntDomainVar>();
	    for (int i = 0; i < vms.size(); i++) {
	    	VirtualMachine vm = vms.get(i);
	    	vCPUSs.add(core.createIntegerConstant("VCPUs for VM " + i, vm.getNbOfCPUs()));
	    	myAssigns[i] = core.getAssociatedAction(vm).getDemandingSlice().hoster();
	    }
	    IntDomainVar[] demands = vCPUSs.toArray(new IntDomainVar[vCPUSs.size()]);
	   	    
	    //capacities hosts the possible numbers of VCPU per nodes (from 0 to overbookingFactor * number of CPU)
	    IntDomainVar[] capacities = new IntDomainVar[allNodes.size()];
	    for(int i = 0; i < allNodes.size(); i++) {
	    	Node n = allNodes.get(i);
	    	//create a value for the VCPU capacity of each node
	    	//if the node is managed by the constraint, set the overbooking factor

    		capacities[i] = core.createBoundIntVar("VCPU capacity for node " + i, 0, maximum_capacity);
    		
	    	if (core.getFutureOfflines().contains(n)) {
	            //an offline node as a capacity of zero
	    		core.post(core.eq(0, capacities[i])); 
	    			               
	        } else if (!core.getFutureOnlines().contains(n)){ //the server state is managed by Entropy
	        	ManageableNodeActionModel action = (ManageableNodeActionModel) core.getAssociatedAction(n);
	         	IntDomainVar isOffline = new BoolVarNot(core, "offline(" + n.getName() + ")", action.getState());
	         	core.post(new FastImpliesEq(isOffline, capacities[i], 0));
	        }
	    }
	    
	    //Each VM must be packed within each CPU capacity  
		core.post(new FastBinPacking(core.getEnvironment(), capacities, demands, myAssigns));
		
		return capacities;
	}
	

/*    @Override
    public boolean isSatisfied(Configuration cfg) {
    	int nbCPUS = 0;
    	int nbVCPUS = 0;
    	for (Node n : nodes) {
    		nbCPUS += n.getNbOfCPUs();
        }
        for (VirtualMachine vm : cfg.getAllVirtualMachines()) {
        	nbVCPUS += vm.getNbOfCPUs();
        }
        if (nbCPUS * overbooking - nbVCPUS > minSpareCPU)
            return true;
        else
        	return false;
    }  */


    @Override
    public boolean isSatisfied(Configuration cfg) {
        int nbSpare = 0;
        for (Node n : nodes) {
            if (cfg.isOnline(n)) {
                int nb = 0;
                for (VirtualMachine vm : cfg.getRunnings(n)) {
                    nb += vm.getNbOfCPUs();
                }
                nbSpare += (n.getCPUCapacity() - nb);
            }
        }
        return nbSpare >= (minSpareCPU * overbooking);
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
        return new StringBuilder("SpareCPUs(").append(nodes).append(", ").append(minSpareCPU).append(", ").append(overbooking).append(")").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpareCPUs q = (SpareCPUs) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "noIdleOnline".hashCode() * 31 + nodes.hashCode();
    }
}
