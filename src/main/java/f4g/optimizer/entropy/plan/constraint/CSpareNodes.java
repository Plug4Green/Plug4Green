package f4g.optimizer.entropy.plan.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import solver.Solver;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import static solver.constraints.IntConstraintFactory.*;
import static solver.variables.VariableFactory.*;
import util.tools.StringUtils;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Constraint;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.constraint.ChocoConstraint;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;
import btrplace.solver.choco.extensions.FastIFFEq;
import btrplace.solver.choco.extensions.FastImpliesEq;

import f4g.optimizer.entropy.plan.constraint.api.SpareNodes;

/**
 * Choco implementation of SpareNodes.
 * The constraint enforces that a minimum number of servers must be "free". 
 * A free server is ON but doesn't have CPU activity.
 */
public class CSpareNodes implements ChocoConstraint {

    private final SpareNodes constraint;

    public CSpareNodes(SpareNodes b) {
    	constraint = b;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) {
  	
    	Solver solver = rp.getSolver();
        Collection<Node> nodes = constraint.getInvolvedNodes();	
        
        BoolVar[] free = new BoolVar[nodes.size()];
        int i = 0;
        for (Node node : constraint.getInvolvedNodes()) {
        	BoolVar state = rp.getNodeAction(node).getState();
        	IntVar NbVms = rp.getNbRunningVMs()[rp.getNode(node)];
    	        	    
    	    free[i] = bool(StringUtils.randomName(), solver);
    	       	    
    	    //if the server is off (state = false), then it is not free
    	    solver.post(new FastImpliesEq(not(state), free[i], 0));
    	    //if the server hosts VMs, then it is not free
    	    solver.post(new FastImpliesEq(arithm(NbVms, "!=", 0).reif(), free[i], 0));
    	    i++;
        }
        
        IntVar freeNumber = bounded("freeNumber", 0, nodes.size(), solver);
        solver.post(sum(free, freeNumber));
        solver.post(arithm(freeNumber, ">=", constraint.getMinSpareNodes()));
        
        return true;
    }

    @Override
    public String toString() {
        return constraint.toString();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return SpareNodes.class;
        }

        @Override
        public CSpareNodes build(Constraint cstr) {
            return new CSpareNodes((SpareNodes) cstr);
        }
    }

	@Override
	public Set<VM> getMisPlacedVMs(Model m) {
		return Collections.emptySet();
	}
}
