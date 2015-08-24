package f4g.optimizer.btrplace.plan.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.extensions.FastImpliesEq;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.StringUtils;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.constraints.ICF;

import f4g.optimizer.btrplace.plan.constraint.api.SpareNodes;

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
        
        BoolVar[] spareNode = new BoolVar[nodes.size()];
        int i = 0;
        for (Node node : constraint.getInvolvedNodes()) {
        	BoolVar state = rp.getNodeAction(node).getState();
        	IntVar NbVms = rp.getNbRunningVMs()[rp.getNode(node)];
    	        	    
        	spareNode[i] = VF.bool(StringUtils.randomName(), solver);
    	       	    
    	    //if the server is off (state = false), then it is not free
    	    solver.post(new FastImpliesEq(VF.not(state), spareNode[i], 0));
    	    //if the server hosts VMs, then it is not free
    	    solver.post(new FastImpliesEq(ICF.arithm(NbVms, "!=", 0).reif(), spareNode[i], 0));
    	    i++;
        }
        
        IntVar spareNodes = VF.bounded("freeNumber", 0, nodes.size(), solver);
        solver.post(ICF.sum(spareNode, spareNodes));
        solver.post(ICF.arithm(spareNodes, ">=", constraint.getMinSpareNodes()));
        
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
