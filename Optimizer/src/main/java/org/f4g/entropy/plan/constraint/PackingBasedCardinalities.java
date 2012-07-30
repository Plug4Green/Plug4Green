package org.f4g.entropy.plan.constraint;

import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.constraint.pack.FastBinPacking;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 28/07/12
 * Time: 18:21
 * To change this template use File | Settings | File Templates.
 */
public class PackingBasedCardinalities implements Cardinalities {

    private static Cardinalities instances = null;

    private IntDomainVar [] cards;

    private static final int DEFAULT_MAX = 50;

    private ReconfigurationProblem rp;

    private int max;
    public PackingBasedCardinalities(ReconfigurationProblem rp) {
        this(rp, DEFAULT_MAX);
    }

    public PackingBasedCardinalities(ReconfigurationProblem rp, int max) {
        if (instances != null) {
            Plan.logger.error("Module already instantiated");
        }
        cards = new IntDomainVar[rp.getNodes().length];
        this.rp = rp;
        instances = this;
        this.max = max;

        List<DemandingSlice> dSlices = rp.getDemandingSlices();

        Node[] ns = rp.getNodes();
        if (!dSlices.isEmpty()) {
            IntDomainVar[] assigns = new IntDomainVar[dSlices.size()];

            cards = new IntDomainVar[ns.length];
            IntDomainVar[] demand = new IntDomainVar[dSlices.size()];
            for (int i = 0; i < ns.length; i++) {
                Node n = rp.getNode(i);
                cards[i] = rp.createBoundIntVar("capa(" + n.getName() + ")", 0, max);
            }
            for (int i = 0; i < demand.length; i++) {
                DemandingSlice s = dSlices.get(i);
                demand[i] = rp.createIntegerConstant("",1);
                assigns[i] = s.hoster();
            }

            SConstraint s = new FastBinPacking(rp.getEnvironment(), cards, demand, assigns);
            rp.post(s);
        }
        Plan.logger.debug("SatisfyDemandingSlicesHeightsFastBP branched");

    }
    public static Cardinalities getInstances() {
        return instances;
    }

    @Override
    public void reset() {
        this.rp = null;
        this.cards = null;
        instances = null;
    }
    @Override
    public IntDomainVar getCardinality(int nIdx) {
        return cards[nIdx];
    }

    @Override
    public IntDomainVar getCardinality(Node n) {
        return getCardinality(rp.getNode(n));
    }
}
