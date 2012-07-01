

package org.f4g.entropy.plan.search_heuristic;

import org.f4g.entropy.plan.F4GPlanner;

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
    void add(F4GPlanner m);
}
