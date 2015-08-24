
package f4g.optimizer.btrplace.plan.constraint;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.transition.NodeTransition;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.Collections;
import java.util.Set;

import f4g.optimizer.btrplace.plan.constraint.api.NoStateChange;


public class CNoStateChange implements ChocoConstraint {

    private NoStateChange cstr;

    /**
     * Make a new constraint.
     *
     * @param o the {@link NoStateChange} to rely on
     */
    public CNoStateChange(NoStateChange o) {
        this.cstr = o;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) {
        Node nId = cstr.getInvolvedNodes().iterator().next();
        NodeTransition m = rp.getNodeAction(nId);
        try {
        	
        	//TODO instantiate the node transition to its current state
            m.getState().instantiateTo(1, Cause.Null);
        } catch (ContradictionException ex) {
            rp.getLogger().error("Unable to force node '{}' not to change state", nId);
            return false;
        }
        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return cstr.toString();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return NoStateChange.class;
        }

        @Override
        public CNoStateChange build(Constraint c) {
            return new CNoStateChange((NoStateChange) c);
        }
    }
}
