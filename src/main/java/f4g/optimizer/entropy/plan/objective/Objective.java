
package org.f4g.entropy.plan.objective;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.plan.choco.ReconfigurationProblem;

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
