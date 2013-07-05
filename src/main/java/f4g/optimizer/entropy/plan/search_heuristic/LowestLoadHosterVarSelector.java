

package org.f4g.entropy.plan.search_heuristic;


import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;

import java.util.Collections;
import java.util.Comparator;
import org.apache.log4j.Logger;
import org.f4g.entropy.configuration.F4GResourcePicker;
import org.f4g.entropy.configuration.F4GNodeComparator;


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
		nodes = myNodes.clone();

		//first criteria: select first the servers with a lot of CPU remaining to empty them
        F4GNodeComparator cmp = new F4GNodeComparator(false, F4GResourcePicker.NodeRc.cpuRemaining, (Configuration) myPb.getSourceConfiguration());
        //second criteria: select first the servers with a big power idle (to free them and switch them off)
        cmp.appendCriteria(true, F4GResourcePicker.NodeRc.powerIdle);
        
        Collections.sort(nodes, cmp);

        vms = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : nodes) {
            vms.addAll(pb.getSourceConfiguration().getRunnings(n));
        }

        hosters = new IntDomainVar[vms.size()];
        for(int i = 0; i < vms.size(); i++) {
            VirtualMachine vm = vms.get(i);
            hosters[i] = myPb.getAssociatedAction(vm).getDemandingSlice().hoster();
        }

    }

    //select the VMs on the servers with the minimum VMs
    @Override

    public IntDomainVar selectVar() {
        for (int i = 0; i < hosters.length; i++) {
            if (!hosters[i].isInstantiated()) {
                return hosters[i];
            }
        }
        return null;

        /*
    	Predicate<VirtualMachine> isNotIntanciated = new Predicate<VirtualMachine>() { 
		    @Override public boolean apply(VirtualMachine vm) { return !hosters[vms.indexOf(vm)].isInstantiated(); }};
		    
		Collection<VirtualMachine> moveable = Collections2.filter(vms, isNotIntanciated);
			        
		if(moveable.size() == 0) {
			return null;
		}
		
		//Comparator<VirtualMachine> cmpRemSpace = new org.f4g.entropy.plan.search_heuristic.Utils.VMCompareRemainingSpaceOnOrigin(pb.getSourceConfiguration(), vms, nodes, hosters);
        Comparator<VirtualMachine> cmpRemSpace = new org.f4g.entropy.plan.search_heuristic.Utils.VMCompareRemainingSpaceOnOriginFast(pb, pb.getSourceConfiguration(), vms, nodes, hosters);
		VirtualMachine vm = Collections.max(moveable, cmpRemSpace);
		//log.debug("VM selected: " + vm.getName());
		return hosters[vms.indexOf(vm)];   */
    		
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


