package f4g.optimizer.entropy.plan.constraint.api;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;

import java.util.Collection;
import java.util.Collections;


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

//    @Override
//    public SatConstraintChecker getChecker() {
//        return new MaxServerPowerChecker(this);
//    }

    public int getMaxServerPower() {
		return maxServerPower;
	}


}
