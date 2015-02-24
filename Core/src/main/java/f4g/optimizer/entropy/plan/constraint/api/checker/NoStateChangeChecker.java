package f4g.optimizer.entropy.plan.constraint.api.checker;

import f4g.optimizer.entropy.plan.constraint.api.NoStateChange;
import org.btrplace.model.Model;
import org.btrplace.model.constraint.AllowAllConstraintChecker;
import org.btrplace.plan.event.ShutdownNode;


public class NoStateChangeChecker extends AllowAllConstraintChecker<NoStateChange> {

    /**
     * Make a new checker.
     *
     * @param o the associated constraint
     */
    public NoStateChangeChecker(NoStateChange o) {
        super(o);
    }

    @Override
    public boolean start(ShutdownNode a) {
        return !getNodes().contains(a.getNode());
    }

    @Override
    public boolean endsWith(Model mo) {
        return true;
    }
}
