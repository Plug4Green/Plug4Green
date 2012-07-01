

package org.f4g.entropy.plan.search_heuristic;


import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;

import org.apache.log4j.Logger;

/**
 * A Var selector that focuses on the assignment var of the demanding slices.
 * To improve the process, it is possible to desactivate the scheduling constraints
 * at the beginning of the heuristic. However, don't forget to activate them at the end.
 *
 * @author Fabien Hermenier
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
     * @param slices the slices to consider
     */
    public EmptyNodeVarSelector(ReconfigurationProblem myPb, ManagedElementSet<Node> myNodes) {
    	super(myPb);
    	log = Logger.getLogger(this.getClass().getName());
		pb = myPb;
		nodes = myNodes;
		
    }
    
    //select the shutdown for idle nodes
    @Override
    public IntDomainVar selectVar() {
    
	    for(int i = 0; i < nodes.size(); i++) {
	    	Node n = nodes.get(i);
		   
	        if(!pb.getFutureOnlines().contains(n) && !pb.getFutureOfflines().contains(n)) {
	        	ManageableNodeActionModel action = (ManageableNodeActionModel) pb.getAssociatedAction(n);
	        	log.debug("action.getState().isInstantiated():" + action.getState().isInstantiated());
	        	if(!action.getState().isInstantiated() && pb.getSourceConfiguration().isOnline(n)) {
	        		log.debug("switch off node:" + n.getName());
	        		return action.getState();
	        	}
	        }    
	    }
	    return null;
    }
}


