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
import btrplace.model.VM;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.constraint.checker.BanChecker;
import btrplace.model.constraint.checker.SatConstraintChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import f4g.optimizer.entropy.plan.constraint.api.checker.SpareNodesChecker;

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

    @Override
    public SatConstraintChecker getChecker() {
        return new SpareNodesChecker(this);
    }

    public int getMinSpareNodes() {
		return minSpareNodes;
	}


}
