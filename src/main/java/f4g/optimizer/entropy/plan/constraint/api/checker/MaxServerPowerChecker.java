

package f4g.optimizer.entropy.plan.constraint.api.checker;

import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.constraint.api.MaxServerPower;
import f4g.optimizer.entropy.plan.objective.PowerView;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.constraint.Ban;
import btrplace.model.constraint.checker.AllowAllConstraintChecker;

/**
 * Checker for the {@link Ban} constraint
 *
 * @author Fabien Hermenier
 * @see Ban
 */
public class MaxServerPowerChecker extends AllowAllConstraintChecker<MaxServerPower> {
    
    /**
     * Make a new checker.
     *
     * @param b the associated constraint
     */
    public MaxServerPowerChecker(MaxServerPower b) {
        super(b);
    }

    @Override
    //Constraint is satisfied if the node is ON and without VMs.
    public boolean startsWith(Model mo) {
        return isSatisfied(mo);
    }

    @Override
    public boolean endsWith(Model mo) {
    	return isSatisfied(mo);
    }
    
	private boolean isSatisfied(Model mo) {
		PowerView powerIdles = (PowerView) mo.getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_IDLES);
		PowerView powerPerVMs = (PowerView) mo.getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_PER_VM);
		
		if (getConstraint().isContinuous()) {
        	for(Node node : getConstraint().getInvolvedNodes()){
        		if(powerIdles.getPower(node) + powerPerVMs.getPower(node) * mo.getMapping().getRunningVMs(node).size() >= getConstraint().getMaxServerPower()) {
        			return false;
        		}
        	}            
        }
        return true;
	}
}
