

package org.f4g.entropy.plan.search_heuristic;


import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.constraint.pack.CustomPack;


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import org.apache.log4j.Logger;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * A Var selector that focuses on the assignment var of the demanding slices.
 * To improve the process, it is possible to desactivate the scheduling constraints
 * at the beginning of the heuristic. However, don't forget to activate them at the end.
 *
 * @author Fabien Hermenier
 */
public class LowestLoadHosterVarSelector extends AbstractIntVarSelector {

	public Logger log;
    
    ReconfigurationProblem pb;
    IntDomainVar[] hosters;
    ManagedElementSet<Node> nodes;
    ManagedElementSet<VirtualMachine> vms;
    

    public class NodeCompareName implements Comparator<Node> {
	   	@Override
        public int compare(Node n1, Node n2) {return n1.getName().compareTo(n2.getName());}
    }
    
    /**
     * Make a new heuristic.
     * By default, the heuristic doesn't touch the scheduling constraints.
     *
     * @param solver the solver to use to extract the assignment variables
     * @param slices the slices to consider
     */
    public LowestLoadHosterVarSelector(ReconfigurationProblem myPb, ManagedElementSet<Node> myNodes) {
    	super(myPb);
    	log = Logger.getLogger(this.getClass().getName());
		pb = myPb;
		nodes = myNodes;
		
		vms = myPb.getSourceConfiguration().getRunnings(myNodes);
		hosters = new IntDomainVar[vms.size()];
		for(int i = 0; i < vms.size(); i++) {
			VirtualMachine vm = vms.get(i);
			hosters[i] = myPb.getAssociatedAction(vm).getDemandingSlice().hoster();
		}
		Collections.sort(nodes, new NodeCompareName());
	   
    }
    
    //select the VMs on the servers with the minimum VMs
    @Override
    public IntDomainVar selectVar() {
    	
    	Predicate<VirtualMachine> isNotIntanciated = new Predicate<VirtualMachine>() { 
		    @Override public boolean apply(VirtualMachine vm) { return !hosters[vms.indexOf(vm)].isInstantiated(); }};
		    
		Collection<VirtualMachine> moveable = Collections2.filter(vms, isNotIntanciated);
			        
		if(moveable.size() == 0) {
			return null;
		}
		
		Comparator<VirtualMachine> cmpRemSpace = new Utils.VMCompareRemainingSpaceOnOrigin(pb.getSourceConfiguration(), vms, nodes, hosters);
		VirtualMachine vm = Collections.max(moveable, cmpRemSpace);
		log.debug("VM selected: " + vm.getName());
		return hosters[vms.indexOf(vm)];
    		
    }
    		
    
    //get the VMs moveable on a node
    ManagedElementSet<VirtualMachine> getRemainingVMs(Node n, IntDomainVar[] hosters) {
    	
    	ManagedElementSet<VirtualMachine> remainingVMsOnNode = pb.getSourceConfiguration().getRunnings(n);
    	ManagedElementSet<VirtualMachine> vms = pb.getSourceConfiguration().getAllVirtualMachines();
    	    	
    	for(VirtualMachine vm : remainingVMsOnNode) {
    		int indexVM = vms.indexOf(vm);
    		if(hosters[indexVM].isInstantiated()) {
    			remainingVMsOnNode.remove(vm);
    		}
    	}
    	
    	return remainingVMsOnNode;
    }

}


