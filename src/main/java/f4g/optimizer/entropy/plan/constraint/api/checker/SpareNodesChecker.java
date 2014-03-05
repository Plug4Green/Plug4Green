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

import f4g.optimizer.entropy.plan.constraint.api.SpareNodes;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Ban;
import btrplace.model.constraint.checker.AllowAllConstraintChecker;
import btrplace.model.view.ShareableResource;
import btrplace.plan.event.BootNode;
import btrplace.plan.event.RunningVMPlacement;
import btrplace.plan.event.ShutdownNode;
import btrplace.solver.choco.view.CShareableResource;

/**
 * Checker for the {@link Ban} constraint
 *
 * @author Fabien Hermenier
 * @see Ban
 */
public class SpareNodesChecker extends AllowAllConstraintChecker<SpareNodes> {
    
    /**
     * Make a new checker.
     *
     * @param b the associated constraint
     */
    public SpareNodesChecker(SpareNodes b) {
        super(b);
    }

    @Override
    //Constraint is satisfied if the node is ON and without VMs.
    public boolean startsWith(Model mo) {
        if (getConstraint().isContinuous()) {
            return getNumberFreeNodes(mo) >= getConstraint().getMinSpareNodes();
        }
        return true;
    }

    @Override
    public boolean endsWith(Model mo) {
        int freeNodes = getNumberFreeNodes(mo);
        return freeNodes <= getConstraint().getMinSpareNodes();
    }
    
	private int getNumberFreeNodes(Model mo) {
		Mapping map = mo.getMapping();
		int freeNodes = 0;
		for (Node n : getConstraint().getInvolvedNodes()) {
		    if (map.isOnline(n)) {
		    	if(map.getRunningVMs(n).isEmpty()) {
		    		freeNodes++;	
		    	}
		    }
		}
		return freeNodes;
	}
}
