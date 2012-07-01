/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.f4g.entropy.plan.search_heuristic;

import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.branching.domwdeg.DomOverWDegBranchingNew;
import choco.cp.solver.search.integer.valiterator.IncreasingDomain;
import choco.cp.solver.search.integer.valselector.MinVal;
import choco.cp.solver.search.integer.varselector.StaticVarOrder;
import choco.cp.solver.search.set.AssignSetVar;
import choco.cp.solver.search.set.MinDomSet;
import choco.cp.solver.search.set.MinEnv;
import choco.cp.solver.search.set.StaticSetVarOrder;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.set.SetVar;
import entropy.plan.choco.PlanHeuristic;
import entropy.plan.choco.ReconfigurationProblem;

/**
 * A dummy placement heuristic.
 * Branch on all the variables in a static manner, and select the minimum value for each selected variable.
 *
 * @author Fabien Hermenier
 */
public class F4GDummyPlacementHeuristic implements PlanHeuristic {

    @Override
    public void add(ReconfigurationProblem m) {
        IntDomainVar [] foo = new IntDomainVar[m.getNbIntVars()];
        SetVar [] bar = new SetVar[m.getNbSetVars()];

        for (int i = 0; i < foo.length; i++) {
            foo[i] = m.getIntVarQuick(i);
        }

//        for (int i = 0; i < bar.length; i++) {
//            bar[i] = m.getSetVarQuick(i);
//        }

        m.addGoal(new AssignVar(new StaticVarOrder(m,
                foo), new MinVal()));
        //m.addGoal(new AssignSetVar(new StaticSetVarOrder(m,
        //        bar), new MinVal()));

       //m.addGoal(new DomOverWDegBranchingNew(m, new IncreasingDomain()));

        //CDU move
        m.addGoal(new AssignSetVar(new MinDomSet(m), new MinEnv()));
    }
}
