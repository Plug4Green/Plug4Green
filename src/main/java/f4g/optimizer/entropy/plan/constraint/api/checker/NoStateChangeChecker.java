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

package f4g.optimizer.entropy.plan.constraint.api.checker;

import f4g.optimizer.entropy.plan.constraint.api.NoStateChange;
import btrplace.model.Model;
import btrplace.model.constraint.checker.AllowAllConstraintChecker;
import btrplace.plan.event.ShutdownNode;


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
