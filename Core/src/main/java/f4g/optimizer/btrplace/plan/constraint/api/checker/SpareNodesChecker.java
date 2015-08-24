
package f4g.optimizer.btrplace.plan.constraint.api.checker;

import f4g.optimizer.btrplace.plan.constraint.api.SpareNodes;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.constraint.Ban;
import org.btrplace.model.constraint.AllowAllConstraintChecker;

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
