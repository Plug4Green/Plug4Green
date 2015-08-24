

package f4g.optimizer.btrplace.plan.search_heuristic;

import org.btrplace.scheduler.choco.ReconfigurationProblem;


/**
 * An interface to specific the way the plan module will select the VMs to place and schedule
 * the actions.
 *
 * @author 
 */
public interface F4GCorePlanHeuristic {

    /**
     * Add the heuristic to the plan module.
     *
     * @param m the plan module
     */
    void add(ReconfigurationProblem m);
}
