
package org.f4g.entropy.plan;

import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.search.ISolutionPool;
import choco.kernel.solver.search.SolutionPoolFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.configuration.parser.FileConfigurationSerializer;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.plan.*;
import entropy.plan.action.Action;
import entropy.plan.choco.BasicPlacementHeuristic2;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.DummyPlacementHeuristic;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsSimpleBP;
import entropy.plan.durationEvaluator.DurationEvaluationException;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.plan.parser.FileTimedReconfigurationPlanSerializerFactory;
import entropy.plan.visualization.DotVisualizer;
import entropy.plan.visualization.PlanVisualizer;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;

import java.io.IOException;
import java.util.*;


import org.apache.log4j.Logger;
import org.f4g.entropy.plan.objective.PowerObjective;
import org.f4g.entropy.plan.search_heuristic.F4GPlacementHeuristic;

import org.f4g.entropy.plan.constraint.sliceScheduling.*;

/**
 * A CustomizablePlannerModule based on Choco.
 *
 * @author Corentin Dupont
 */
public class F4GPlanner extends CustomizablePlannerModule {

    private List<SConstraint> costConstraints;
    
    public Logger log;

    /**
     * The model.
     */
    private ReconfigurationProblem model;

    private boolean repair = false;

    private List<VJob> queue;
    
    private PowerObjective objective;

    /**
     * Indicates if the planner stops after computing a first solution.
     * @return {@code true} if it continues
     */
    public boolean doOptimize() {
        return optimize;
    }

    /**
     * Indicates if the planner must stop after computing a first solution.
     * @return {@code true} if it continues
     */
    public void doOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    private boolean optimize = true;
     /**
     * the class to instantiate to generate the global constraint.
     * Default is SatisfyDemandingSlicesHeightsSimpleBP.
     */
    private SatisfyDemandingSliceHeights packingConstraintClass = new SatisfyDemandingSlicesHeightsFastBP(); //Fast

    /**
     * @return the globalConstraintClass
     */
    public SatisfyDemandingSliceHeights getPackingConstraintClass() {
        return packingConstraintClass;
    }

    /**
     * @param c the globalConstraintClass to set
     */
    public void setPackingConstraintClass(SatisfyDemandingSliceHeights c) {
        packingConstraintClass = c;
    }
    
    /**
     * Make a new plan module.
     *
     * @param eval to evaluate the duration of the actions.
     */
    public F4GPlanner(PowerObjective myObjective) {
        super(new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9));
        costConstraints = new LinkedList<SConstraint>();
        objective = myObjective;
        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Get the model.
     *
     * @return the model to express constraints.
     */
    public ReconfigurationProblem getModel() {
        return this.model;
    }

    @Override
    public List<SolutionStatistics> getSolutionsStatistics() {
        if (model == null) {
            return new ArrayList<SolutionStatistics>();
        }
        return this.model.getSolutionsStatistics();
    }

    /**
     * @return some statistics about the solving process
     */
    @Override
    public SolvingStatistics getSolvingStatistics() {
        if (model == null) {
            return SolvingStatistics.getStatisticsForNotSolvingProcess();
        }
        return model.getSolvingStatistics();
    }

    
    @Override
    public TimedReconfigurationPlan compute(Configuration src,
                                            ManagedElementSet<VirtualMachine> run,
                                            ManagedElementSet<VirtualMachine> wait,
                                            ManagedElementSet<VirtualMachine> sleep,
                                            ManagedElementSet<VirtualMachine> stop,
                                            ManagedElementSet<Node> on,
                                            ManagedElementSet<Node> off,
                                            List<VJob> q) throws PlanException {

		
	    queue = q;	
	
	    ManagedElementSet<VirtualMachine> vms = null;
	    if (repair) {
	        //Look for the VMs to consider
	        vms = new SimpleManagedElementSet<VirtualMachine>();
	//            for (VJob v : queue) {
	//                for (PlacementConstraint c : v.getConstraints()) {
	//                    if (!c.isSatisfied(src)) {
	//                    	Plan.logger.debug("Constraint " + c.toString() + " is not satisfied");
	//                        vms.addAll(c.getMisPlaced(src));
	//                    }
	//                }
	//            }
	//            //Hardcore way for the packing. TODO: externalize
	//            //System.err.println("pack issue:" + src.getRunnings(src.getUnacceptableNodes()));
	//            vms.addAll(src.getRunnings(Configurations.futureOverloadedNodes(src)));
	    } else {
	        vms = src.getAllVirtualMachines();
	    }

	    //creation of the solver
	    model = new DefaultReconfigurationProblem(src, run, wait, sleep, stop, vms, on, off, this.getDurationEvaluator());

	    Map<Class, Integer> occurences = new HashMap<Class, Integer>();
	    int nbConstraints = 0;
	    
	    for (VJob vjob : queue) {
	        for (PlacementConstraint c : vjob.getConstraints()) {
	            try {
	            	log.debug("Active constraint:" + c.toString());
	                c.inject(model);
	                if (!occurences.containsKey(c.getClass())) {
	                    occurences.put(c.getClass(), 0);
	                }
	                nbConstraints++;
	                occurences.put(c.getClass(), 1 + occurences.get(c.getClass()));
	            } catch (Exception e) {
	            	log.error(e.getMessage(), e);
	            }
	        }
	    }

	    log.debug("adding packing constraint");
        packingConstraintClass.add(model);
        new SlicesPlanner().add(model);

        /*try {
            model.getEnd().setSup(50);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } */
	    /*
	    * A pretty print of the problem
	    */
        log.debug(run.size() + wait.size() + sleep.size() + stop.size() + " VMs: " +
	            run.size() + " will run; " + wait.size() + " will wait; " + sleep.size() + " will sleep; " + stop.size() + " will be stopped");
        log.debug(src.getAllNodes().size() + " nodes: " + on.size() + " must be on, " + off.size() + " must be off. " + (src.getAllNodes().size() - on.size() - off.size()) + " manageable");
        log.debug("Manage " + vms.size() + " VMs (" + (repair ? "repair" : "rebuild") + ")");
        log.debug("Timeout is " + getTimeLimit() + " seconds");
	
	    //The constraints
	    StringBuilder b = new StringBuilder();
	    b.append(nbConstraints + " constraints: ");
	    for (Map.Entry<Class, Integer> e : occurences.entrySet()) {
	        b.append(e.getValue() + " " + e.getKey().getSimpleName() + "; ");
	    }
	    log.debug(b.toString());
	 	    
	    //create and set the optimization objective in the engine
	    objective.makeObjective(model);
	    model.setObjective(objective.getObjective());
	   
	    //time limit of the search
        if (getTimeLimit() > 0) {
	        model.setTimeLimit(getTimeLimit()* 1000); // Conversion into seconds
        }

        //model.clearGoals();
	    //Add the F4G heuristics
        //new BasicPlacementHeuristic2(null).add(this);
	    new F4GPlacementHeuristic().add(this);

	    //other goals
        new DummyPlacementHeuristic().add(model);
	    //model.addGoal(((DefaultReconfigurationProblem)model).generateDefaultIntGoal());
	    //model.addGoal(((DefaultReconfigurationProblem)model).generateSetDefaultGoal());

	    log.debug(generationTime + "ms to build the solver, " + model.getNbIntConstraints() + " constraints, " + model.getNbIntVars() + " integer variables, " + model.getNbBooleanVars() + " boolean variables, " + model.getNbConstants() + " constants");
	    //Launch the solver
        model.setDoMaximize(false);
        model.setFirstSolution(!optimize);
        model.generateSearchStrategy();
        ISolutionPool sp = SolutionPoolFactory.makeInfiniteSolutionPool(model.getSearchStrategy());
        model.getSearchStrategy().setSolutionPool(sp);

        //model.solve();
        model.launch();
	    Boolean ret = model.isFeasible();
	    if (ret == null) {
	        throw new PlanException("Unable to check wether a solution exists or not");
	    } else {
	    	
	    	log.debug("#nodes= " + model.getNodeCount() +
	                ", #backtracks= " + model.getBackTrackCount() +
	                ", #duration= " + model.getTimeCount() +
	                ", #nbsol= " + model.getNbSolutions());
	    	//Plan.logger.debug("objective: " + objective.getObjective().getDomain());
	        if (Boolean.FALSE.equals(ret)) {
	            throw new PlanException("No solution");
	        } else {
	            TimedReconfigurationPlan plan = model.extractSolution();
                DotVisualizer v = new DotVisualizer("dot.dot");
                v.buildVisualization(plan);
	            Configuration res = plan.getDestination();
	            ManagedElementSet<VirtualMachine> resVms = res.getAllVirtualMachines();
                for (Node n : res.getAllNodes()) {
                    if (res.getRunnings(n).isEmpty() && ! res.isOffline(n)) {
                        System.err.println(n.getName() + " should be shutted down ");
                    }
                }
/*	            for(VirtualMachine vm: resVms) {
	            	System.out.println("VM " + vm.getName() + " running on: " + res.getLocation(vm));
	            }*/
	            
	            
	            if (Configurations.futureOverloadedNodes(res).size() != 0) {
	                throw new PlanException("Resulting configuration is not viable: Overloaded nodes=" + Configurations.futureOverloadedNodes(res));
	            }
	
	            int cost = 0;
	            for (Action a : plan) {
	                cost += a.getFinishMoment();
	            }
	//                if (cost != globalCost.getVal()) {
	//                    throw new PlanException("Practical cost of the plan (" + cost + ") and objective (" + globalCost.getVal() + ") missmatch:\n" + plan);
	//                }
	            for (VJob vjob : queue) {
	                for (PlacementConstraint c : vjob.getConstraints()) {
	                    if (!c.isSatisfied(res)) {
	                        throw new PlanException("Resulting configuration does not satisfy '" + c.toString() + "'");
	                    }
	                }
	            }
	            return plan;
            }
        }
    }

    /**
     * Estimate the lower and the upper bound of model.getEnd()
     *
     * @param totalDuration the totalDuration of all the action
     * @throws entropy.plan.durationEvaluator.DurationEvaluationException
     *          if an error occured during evaluation of the durations.
     */
    private void setTotalDurationBounds(IntDomainVar totalDuration, ManagedElementSet<VirtualMachine> vms) throws DurationEvaluationException {
        int maxEnd = 0;
        for (VirtualMachine vm : vms) { //FIXME: Bad, should consider the real actions
            maxEnd += getDurationEvaluator().evaluateMigration(vm);
        }
        int sup = ReconfigurationProblem.MAX_TIME;//Math.min(maxEnd, ReconfigurationProblem.MAX_TIME);
        int min = 0;
        try {
            model.getEnd().setInf(min);
            model.getEnd().setSup(sup);
            totalDuration.setInf(min);
            totalDuration.setSup(sup);
        } catch (Exception e) {
        	log.warn(e.getMessage(), e);
        }
        log.debug(totalDuration.pretty());
        log.debug(model.getEnd().pretty());
    }

    /**
     * Update the upper bounds of all the variable to simplify the problem.
     */
    private void updateUB() {
        int ub = model.getEnd().getSup();
        List<ActionModel> allActionModels = new LinkedList<ActionModel>(model.getNodeMachineActions());
        allActionModels.addAll(model.getVirtualMachineActions());

        try {
            for (VirtualMachineActionModel a : model.getVirtualMachineActions()) {
                if (a.end().getSup() > ub) {
                    a.end().setSup(ub);
                }
                if (a.start().getSup() > ub) {
                    a.start().setSup(ub);
                }

                if (a.getGlobalCost().getSup() > ub) {
                    a.getGlobalCost().setSup(ub);
                }

                Slice task = a.getDemandingSlice();
                if (task != null) {
                    if (task.end().getSup() > ub) {
                        task.end().setSup(ub);
                    }
                    if (task.start().getSup() > ub) {
                        task.start().setSup(ub);
                    }
                    if (task.duration().getSup() > ub) {
                        task.duration().setSup(ub);
                    }
                }

                task = a.getConsumingSlice();
                if (task != null) {
                    if (task.end().getSup() > ub) {
                        task.end().setSup(ub);
                    }
                    if (task.start().getSup() > ub) {
                        task.start().setSup(ub);
                    }
                    if (task.duration().getSup() > ub) {
                        task.duration().setSup(ub);
                    }
                }
            }
        } catch (Exception e) {
        	log.warn(e.getMessage(), e);
        }
    }

    /**
     * Get all the vjobs managed by the module
     *
     * @return a list of vjobs, may be empty
     */
    public List<VJob> getQueue() {
        return queue;
    }

    /**
     * Use the repair mode.
     *
     * @param b {@code true} to use the repair mode
     */
    public void setRepairMode(boolean b) {
        this.repair = b;
    }


    /**
     * Make a sum of a large number of variables using
     * decomposition
     *
     * @param m    the model
     * @param vars the variables to sum
     * @param step the size of the subsums.
     * @return the variable storing the result of the sum.
     */
    private IntExp explodedSum(ReconfigurationProblem m, IntDomainVar[] vars, int step, boolean post) {
        int s = vars.length > step ? step : vars.length;
        IntDomainVar[] subSum = new IntDomainVar[s];
        int nbSubs = (int) Math.ceil(vars.length / step);
        if (vars.length % step != 0) {
            nbSubs++;
        }
        IntDomainVar[] ress = new IntDomainVar[nbSubs];

        int curRes = 0;
        int shiftedX = 0;
        for (int i = 0; i < vars.length; i++) {
            subSum[shiftedX++] = vars[i];
            if (shiftedX == subSum.length) {
                IntDomainVar subRes = m.createBoundIntVar("subSum[" + (i - shiftedX + 1) + ".." + i + "]", 0, ReconfigurationProblem.MAX_TIME);
                SConstraint c = m.eq(subRes, m.sum(subSum));
                if (post) {
                    m.post(c);
                } else {
                    costConstraints.add(c);
                }
                ress[curRes++] = subRes;
                if (i != vars.length - 1) {
                    int remainder = vars.length - (i + 1);
                    s = remainder > step ? step : remainder;
                    subSum = new IntDomainVar[s];
                }
                shiftedX = 0;
            }
        }
        return m.sum(ress);
    }

    public List<SConstraint> getCostConstraints() {
        return this.costConstraints;
    }
}
