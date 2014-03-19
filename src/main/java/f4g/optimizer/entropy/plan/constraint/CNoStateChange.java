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

package f4g.optimizer.entropy.plan.constraint;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Constraint;
import btrplace.model.constraint.Online;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.actionModel.ActionModel;
import btrplace.solver.choco.constraint.ChocoConstraint;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;
import solver.Cause;
import solver.exception.ContradictionException;

import java.util.Collections;
import java.util.Set;

import f4g.optimizer.entropy.plan.constraint.api.NoStateChange;


/**
 * Choco implementation of {@link btrplace.model.constraint.Online}.
 *
 * @author Fabien Hermenier
 */
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
    public boolean inject(ReconfigurationProblem rp) throws SolverException {
        Node nId = cstr.getInvolvedNodes().iterator().next();
        ActionModel m = rp.getNodeAction(nId);
        try {
            m.getState().instantiateTo(1, Cause.Null);
        } catch (ContradictionException ex) {
            rp.getLogger().error("Unable to force node '{}' no to change state", nId);
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
