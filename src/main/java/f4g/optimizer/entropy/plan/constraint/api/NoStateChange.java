/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4g.optimizer.entropy.plan.constraint.api;

import btrplace.model.Node;
import btrplace.model.constraint.NodeStateConstraint;
import btrplace.model.constraint.checker.OnlineChecker;
import btrplace.model.constraint.checker.SatConstraintChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import f4g.optimizer.entropy.plan.constraint.api.checker.NoStateChangeChecker;

/**
 * A constraint to force a node at being online.
 * <p/>
 * The restriction provided by the constraint is discrete.
 * however, if the node is already offline, its
 * state will be unchanged.
 *
 * @author Fabien Hermenier
 */
public class NoStateChange extends NodeStateConstraint {

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

    /**
     * Make a new constraint.
     *
     * @param n the node
     */
    public NoStateChange(Node n) {
        super("noStateChange", n);
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new NoStateChangeChecker(this);
    }

}
