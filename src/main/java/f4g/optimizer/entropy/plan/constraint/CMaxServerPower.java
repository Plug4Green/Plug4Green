

package f4g.optimizer.entropy.plan.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import btrplace.model.view.ModelView;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.constraint.ChocoConstraint;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;
import btrplace.solver.choco.view.ChocoModelView;
import choco.Choco;
import choco.cp.solver.constraints.integer.bool.BooleanFactory;
import choco.cp.solver.constraints.reified.ReifiedFactory;
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
            
            IntVar powerVMS = VariableFactory.bounded("powerVMS", 0, Integer.MAX_VALUE / 100, solver);
            IntConstraintFactory.times(rp.getNbRunningVMs()[rp.getNode(node)], pv.getPowerperVM(node), powerVMS);

            IntVar powerIdle = VariableFactory.fixed(pv.getPowerIdle(node), solver);
			
			solver.post(IntConstraintFactory.arithm(powerVMS, "+", powerIdle, "<=", constraint.getMaxServerPower()));
			
        }

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
