package f4g.optimizer.entropy.plan.objective.api;

import btrplace.model.constraint.OptConstraint;


/**
 * An optimization constraint that minimizes the time to repair a non-viable model.
 * In practice it minimizes the sum of the ending moment for each actions.
 *
 * @author Fabien Hermenier
 */
public class PowerObjective extends OptConstraint {


    @Override
    public String id() {
        return "powerObjective";
    }
}
