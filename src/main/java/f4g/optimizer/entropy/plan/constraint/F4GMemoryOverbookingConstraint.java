/**
 * ============================== Header ============================== 
 * file:          F4GBan.java
 * project:       FIT4Green/Optimizer
 * created:       07.10.2011 by ts
 * last modified: $LastChangedDate: 2012-02-23 17:59:34 +0100 (Do, 23 Feb 2012) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 1168 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package org.f4g.entropy.plan.constraint;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import choco.cp.solver.variables.integer.BoolVarNot;
import choco.cp.solver.variables.integer.BooleanVarImpl;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.metamodel.FIT4GreenType;

import choco.Choco;
import choco.cp.solver.constraints.reified.FastIFFEq;
import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.cp.solver.constraints.set.InverseSetInt;
import choco.kernel.memory.IEnvironment;
import choco.kernel.solver.variables.Var;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.set.SetVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModels;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.plan.choco.actionModel.slice.SliceComparator;
import entropy.plan.choco.actionModel.slice.Slices;
import entropy.plan.choco.constraint.pack.FastBinPacking;
import entropy.plan.choco.constraint.pack.SimpleBinPacking;
import entropy.vjob.*;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GMemoryOverbookingConstraint extends F4GConstraint {

	double overbookingFactor; // Percent of overbooking (e.g. 1.20)

	/**
	 * The set of nodes
	 */
	private ManagedElementSet<Node> nodes;
	private final int maximum_capacity = 100000000; //MB


	/**
	 * Make a new constraint.
	 * 
	 * @param vms
	 *            the VMs to assign
	 * @param nodes
	 *            the nodes to exclude
	 */
	public F4GMemoryOverbookingConstraint(ManagedElementSet<Node> nodes, FIT4GreenType metamodel, Double MyOverbookingFactor, ClusterType clusters) {
		this.nodes = nodes;
		this.overbookingFactor = MyOverbookingFactor;
	}

	/**
	 * Get the set of nodes involved in the constraint.
	 * 
	 * @return a set of nodes
	 */
	public ManagedElementSet<Node> getNodes() {
		return this.nodes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		F4GMemoryOverbookingConstraint that = (F4GMemoryOverbookingConstraint) o;

		return (nodes.equals(that.nodes) && getAllVirtualMachines().equals(
				that.getAllVirtualMachines()));
	}

	@Override
	public int hashCode() {
		int result = getAllVirtualMachines().hashCode();
		result = 31 * result + nodes.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		buffer.append("F4GMemoryOverbookingConstraint(");
		buffer.append(overbookingFactor);
		buffer.append(", ");
		buffer.append(nodes.toString());
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Apply the constraint to the plan to all the VMs in a future running
	 * state. FIXME: What about running VMs that will be suspended ?
	 * 
	 * @param core
	 *            the plan to customize. Must implement
	 *            {@link entropy.plan.choco.ChocoCustomizablePlannerModule}
	 */
	@Override
	public void inject(ReconfigurationProblem core) {

		ManagedElementSet<VirtualMachine> vms = core.getFutureRunnings().clone(); 
		ManagedElementSet<Node> nodes = core.getSourceConfiguration().getAllNodes();
				
		//myAssigns contains the possible hosters for each VM 
	    IntDomainVar[] myAssigns = new IntDomainVar[vms.size()];    
	    //Sort in descending order according to the needed Memory
        Collections.sort(vms, new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryDemand));
        
	    //vMemorys is a constant list which contains the VMemory asked by each VM 
	    List<IntDomainVar> vMemory = new ArrayList<IntDomainVar>();
	    for (int i = 0; i < vms.size(); i++) { 
	    	VirtualMachine vm = vms.get(i);
	    	vMemory.add(core.createIntegerConstant("VMemory for VM " + i, vm.getMemoryDemand()));
	    	myAssigns[i] = core.getAssociatedAction(vm).getDemandingSlice().hoster();
	    }
	    IntDomainVar[] demands = vMemory.toArray(new IntDomainVar[vMemory.size()]);
	   
	    //capacities hosts the possible numbers of VCPU per nodes (from 0 to overbookingFactor * number of CPU)
	    IntDomainVar[] capacities = new IntDomainVar[nodes.size()];
	    for(int i = 0; i < nodes.size(); i++) {
	    	Node n = nodes.get(i);
	    	if (this.nodes.contains(n)){
	    		capacities[i] = core.createBoundIntVar("VMem capacity for node " + i, 0, (int) (overbookingFactor * n.getMemoryCapacity()));
	    	} else{
	    		capacities[i] = core.createBoundIntVar("VMem capacity for node " + i, 0, maximum_capacity );
		   	}
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
	    //Try SimpleBinPacking first, then FastBinPacking as I'm not sure this last one works...
		core.post(new FastBinPacking(core.getEnvironment(), capacities, demands, myAssigns));
		
	}
	
	
	/**
     * Check that the nodes does not host a number of VMs greater
     * than the maximum specified
     *
     * @param configuration the configuration to check
     * @return {@code true} if the constraint is satisfied.
     */
    @Override
    public boolean isSatisfied(Configuration configuration) {
        for (Node n : nodes) {
        	int virtualMem = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualMem += vm.getMemoryDemand();
        	}
        	if (virtualMem > n.getMemoryCapacity()* overbookingFactor){
            	return false;
            }
        }
        return true;
    }

    
    /**
     * If the amount of VMs exceed its capacity, it returns all the hosted VMs
     *
     * @param configuration the configuration to check
     * @return a set of virtual machines that may be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
    	ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : nodes) {
        	int virtualMem = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualMem += vm.getMemoryDemand();
        	}
            ManagedElementSet<VirtualMachine> vms = configuration.getRunnings(n);
            if (virtualMem > n.getMemoryCapacity() * overbookingFactor){
            	bad.addAll(vms);
            }
        }
        return bad;
    }

}
