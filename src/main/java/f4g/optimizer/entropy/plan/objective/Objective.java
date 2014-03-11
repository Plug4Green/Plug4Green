
package f4g.optimizer.entropy.plan.objective;

import btrplace.solver.choco.ReconfigurationProblem;
import choco.kernel.solver.variables.integer.IntDomainVar;


/**
 * abstract class that creates a new optimisation objective
 *
 * @author 
 */
public abstract class Objective  {

	IntDomainVar objective;
   
	public Objective() {
		objective = null;
	}
	
    public IntDomainVar getObjective(){
    	return objective; 
    }
    
    /**
     * create the optimisation objective
     */
    public void makeObjective(ReconfigurationProblem model) {}

}
