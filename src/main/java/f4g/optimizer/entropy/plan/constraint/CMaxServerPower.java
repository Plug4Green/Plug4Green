package f4g.optimizer.entropy.plan.constraint;

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
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.constraint.api.MaxServerPower;
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
        PowerView pvServers = (PowerView) rp.getSourceModel().getView(F4GConfigurationAdapter.VIEW_POWER_IDLES);
        PowerView pvVMs = (PowerView) rp.getSourceModel().getView(F4GConfigurationAdapter.VIEW_POWER_PER_VM);

        for (Node node : nodes) {

            IntVar powerIdle = VariableFactory.fixed(pvServers.getPower(node), solver);
            IntVar powerperVM = VariableFactory.fixed(pvVMs.getPower(node), solver);
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
            return MaxServerPower.class;
        }

        @Override
        public CMaxServerPower build(Constraint cstr) {
            return new CMaxServerPower((MaxServerPower) cstr);
        }
    }

	@Override
	public Set<VM> getMisPlacedVMs(Model m) {
		return Collections.emptySet();
	}

}
