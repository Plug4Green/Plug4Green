
package f4g.optimizer.btrplace.plan.constraint.api;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;
import java.util.Collection;
import java.util.Collections;

/**
 * A constraint that enforces a certain number of "spare" nodes.
 * A spare node is a node online, but with no workload.
 * 
 * @see SatConstraint
 */
public class SpareNodes extends SatConstraint {

    private int minSpareNodes;

	/**
     * Make a new constraint.
     *
     * @param vm    the VM identifiers
     * @param nodes the nodes identifiers
     */
    public SpareNodes(Collection<Node> nodes, int minSpareNodes) {
        super(Collections.<VM>emptySet(), nodes, false);
        this.minSpareNodes = minSpareNodes;
    }


    @Override
    public String toString() {
        return "spareNodes(" + ", nodes=" + getInvolvedNodes() + ", discrete)";
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
//        return new SpareNodesChecker(this);
//    }

    public int getMinSpareNodes() {
		return minSpareNodes;
	}


}
