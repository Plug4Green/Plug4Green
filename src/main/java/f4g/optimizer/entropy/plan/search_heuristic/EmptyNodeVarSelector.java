

package f4g.optimizer.entropy.plan.search_heuristic;


import java.util.Collections;

import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;

import entropy.plan.choco.actionModel.NodeActionModel;
import org.apache.log4j.Logger;
import f4g.optimizer.entropy.configuration.F4GNodeComparator;
import f4g.optimizer.entropy.configuration.F4GResourcePicker;

/**
 * A Var selector that select the empty, energy inefficient nodes first
 *
 * @author Corentin Dupont
 */
public class EmptyNodeVarSelector extends AbstractIntVarSelector {

	public Logger log;
    
    ReconfigurationProblem pb;
    ManagedElementSet<Node> nodes;
   
    
    /**
     * Make a new heuristic.
     * By default, the heuristic doesn't touch the scheduling constraints.
     *
     * @param solver the solver to use to extract the assignment variables
     * @param nodes the nodes to consider
     */
    public EmptyNodeVarSelector(ReconfigurationProblem myPb, ManagedElementSet<Node> myNodes) {
    	super(myPb);
    	log = Logger.getLogger(this.getClass().getName());
		pb = myPb;
		nodes = myNodes;
		
		//first criteria: select first the servers that are energy inefficient to switch them off
        F4GNodeComparator cmp = new F4GNodeComparator(false, F4GResourcePicker.NodeRc.powerIdle, (Configuration) myPb.getSourceConfiguration());
  
        Collections.sort(nodes, cmp);
        
       
        
		
    }
    
    //select the node state variable in order
    @Override
    public IntDomainVar selectVar() {
//    	 ManagedElementSet<VirtualMachine> allVMS = pb.getSourceConfiguration().getAllVirtualMachines();
//         IntDomainVar[] hosters = new IntDomainVar[allVMS.size()];
//         for(int i = 0; i < allVMS.size(); i++) {
//             VirtualMachine vm = pb.getVirtualMachine(i);
//             hosters[i] = pb.getAssociatedAction(vm).getDemandingSlice().hoster();
//             if(hosters[i].isInstantiated()) {
//             	log.debug("hoster " + i + " instancied " + hosters[i].getVal());
//             } else {
//             	log.debug("hoster " + i + " not instancied ");
//             }   
//         }
    	
	    for(int i = 0; i < nodes.size(); i++) {
	    	Node n = nodes.get(i);
		   
	    	//future online and future offline should have their state set already
	        if(!pb.getFutureOnlines().contains(n) && !pb.getFutureOfflines().contains(n)) {
	        	ManageableNodeActionModel action = (ManageableNodeActionModel) pb.getAssociatedAction(n);
	        	log.debug("Node " + n.getName());
	        	log.debug("state instancied " + action.getState().isInstantiated());
	        	//Select the empty nodes first
	        	if(!action.getState().isInstantiated() && pb.getUsedMem(n).isInstantiatedTo(0)) {
	        		log.debug("select node " + n.getName() + " for switch off");
	        		return action.getState();
	        	}
                if (pb.getUsedMem(n).isInstantiatedTo(0) && !action.start().isInstantiated()) {
                	log.debug("select node " + n.getName() + " for start");
                    return action.start();
                }
            }
	    }
	    return null;
    }
}


