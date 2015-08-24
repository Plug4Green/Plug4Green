package f4g.optimizer.btrplace.plan.constraint.api;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class NoStateChange extends SatConstraint {

   
    public NoStateChange(Node n) {
		super(Collections.<VM>emptySet(), Collections.singleton(n), false);
		// TODO Auto-generated constructor stub
	}

	/**
     * Instantiate constraints for a collection of nodes.
     *
     * @param nodes the nodes to integrate
     * @return the associated list of constraints
     */
    public static List<NoStateChange> newNoStateChanges(Collection<Node> nodes) {
        List<NoStateChange> l = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            l.add(new NoStateChange(n));
        }
        return l;
    }



}
