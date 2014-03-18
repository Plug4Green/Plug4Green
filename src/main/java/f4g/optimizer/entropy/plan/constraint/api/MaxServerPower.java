package f4g.optimizer.entropy.plan.constraint.api;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.constraint.checker.BanChecker;
import btrplace.model.constraint.checker.SatConstraintChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import f4g.optimizer.entropy.plan.constraint.api.checker.MaxServerPowerChecker;
import f4g.optimizer.entropy.plan.constraint.api.checker.SpareNodesChecker;

/**
 * A constraint that enforces a certain number of "spare" nodes.
 * A spare node is a node online, but with no workload.
 * 
 * @see SatConstraint
 */
public class MaxServerPower extends SatConstraint {

    private int maxServerPower;

	/**
     * Make a new constraint.
     *
     * @param vm    the VM identifiers
     * @param nodes the nodes identifiers
     */
    public MaxServerPower(Collection<Node> nodes, int maxServerPower) {
        super(Collections.<VM>emptySet(), nodes, false);
        this.maxServerPower = maxServerPower;
    }


    @Override
    public String toString() {
        return "MaxServerPower(" + ", nodes=" + getInvolvedNodes() + ", discrete)";
    }

    @Override
    public boolean setContinuous(boolean b) {
        if (!b) {
            super.setContinuous(b);
        }
        return !b;
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new MaxServerPowerChecker(this);
    }

    public int getMaxServerPower() {
		return maxServerPower;
	}


}
