/**
 * ============================== Header ============================== 
 * file:          F4GBan.java
 * project:       FIT4Green/Optimizer
 * created:       07.10.2011 by ts
 * last modified: $LastChangedDate: 2012-05-02 00:47:35 +0200 (mié, 02 may 2012) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 1411 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package f4g.optimizer.entropy.plan.constraint;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import choco.cp.solver.variables.integer.BoolVarNot;
import org.apache.log4j.Logger;

import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.cp.solver.variables.integer.BooleanVarImpl;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.constraint.pack.FastBinPacking;

/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GCPUOverbookingConstraint extends F4GConstraint {

	double overbookingFactor; // Percent of overbooking (e.g. 1.20)
	public Logger log;
	int maximum_capacity = 1000;
	
	/**
	 * The set of nodes
	 */
	private ManagedElementSet<Node> nodes;

	/**
	 * Make a new constraint.
	 * 
	 * @param vms
	 *            the VMs to assign
	 * @param nodes
	 *            the nodes to exclude
	 */
	public F4GCPUOverbookingConstraint(ManagedElementSet<Node> nodes, Double MyOverbookingFactor) {
		this.nodes = nodes;
		this.overbookingFactor = MyOverbookingFactor;
		log = Logger.getLogger(this.getClass().getName());
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
		F4GCPUOverbookingConstraint that = (F4GCPUOverbookingConstraint) o;

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

		buffer.append("F4GCPUOverbookingConstraint(");
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
	    	if(nodes.contains(n)) {
	    		capacities[i] = core.createBoundIntVar("VCPU capacity for node " + i, 0, (int) (overbookingFactor * n.getNbOfCPUs()));	
	    	} else { // else, set a big value
	    		capacities[i] = core.createBoundIntVar("VCPU capacity for node " + i, 0, maximum_capacity);
	    	}	    		
	    	
	    	if (core.getFutureOfflines().contains(n)) {
	            //an offline node as a capacity of zero
	    		core.post(core.eq(0, capacities[i])); 
	    			               
	        } else if (!core.getFutureOnlines().contains(n)){ //the server state is managed by Entropy
	        	ManageableNodeActionModel action = (ManageableNodeActionModel) core.getAssociatedAction(n);
	         	IntDomainVar isOffline = new BoolVarNot(core, "offline(" + n.getName() + ")", (BooleanVarImpl)action.getState());
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
        	int virtualCores = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualCores += vm.getNbOfCPUs();
        	}
        	if (virtualCores > n.getNbOfCPUs() * overbookingFactor){
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
        	int virtualCores = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualCores += vm.getNbOfCPUs();
        	}
            ManagedElementSet<VirtualMachine> vms = configuration.getRunnings(n);
            if (virtualCores > n.getNbOfCPUs() * overbookingFactor){
            	bad.addAll(vms);
            }
        }
        return bad;
    }

}
