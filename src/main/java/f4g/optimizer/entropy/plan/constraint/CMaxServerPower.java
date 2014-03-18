package f4g.optimizer.entropy.plan.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import solver.Solver;
import solver.constraints.IntConstraintFactory;
import solver.variables.IntVar;
import solver.variables.VariableFactory;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Ban;
import btrplace.model.constraint.Constraint;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.constraint.ChocoConstraint;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;

import f4g.optimizer.entropy.plan.constraint.api.MaxServerPower;
import f4g.optimizer.entropy.plan.constraint.api.SpareNodes;
import f4g.optimizer.entropy.plan.objective.PowerView;


public class CMaxServerPower implements ChocoConstraint {
	
	private final MaxServerPower constraint;

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes
     */
    public CMaxServerPower(MaxServerPower maxServerPower) {
        this.constraint = maxServerPower;
    }
    

    @Override
    public boolean inject(ReconfigurationProblem rp) {
    	
    	Collection<Node> nodes = constraint.getInvolvedNodes();	
    	Solver solver = rp.getSolver();
        PowerView pv = (PowerView) rp.getSourceModel().getView(PowerView.VIEW_ID_BASE);
        
        for (Node node : nodes) {	
                        
            IntVar powerIdle = VariableFactory.fixed(pv.getPowerIdle(node), solver);
            IntVar powerperVM = VariableFactory.fixed(pv.getPowerperVM(node), solver);
            IntVar powerVMS = VariableFactory.bounded("powerVMS", 0, Integer.MAX_VALUE / 100, solver);
            
            IntConstraintFactory.times(rp.getNbRunningVMs()[rp.getNode(node)], powerperVM, powerVMS);
			solver.post(IntConstraintFactory.arithm(powerVMS, "+", powerIdle, "<=", constraint.getMaxServerPower()));
        }

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
            return Ban.class;
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
