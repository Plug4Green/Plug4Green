package f4g.optimizer.entropy.plan.constraint;

import choco.cp.solver.constraints.global.Occurrence;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.Slices;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 28/07/12
 * Time: 18:21
 * To change this template use File | Settings | File Templates.
 */
public class OccurencesBasedCardinalities implements Cardinalities {

    private static Cardinalities instances = null;

    private IntDomainVar [] cards;

    private static final int DEFAULT_MAX = 50;

    private ReconfigurationProblem rp;

    private int max;
    public OccurencesBasedCardinalities(ReconfigurationProblem rp) {
        this(rp, DEFAULT_MAX);
    }

    public OccurencesBasedCardinalities(ReconfigurationProblem rp, int max) {
        if (instances != null) {
            Plan.logger.error("Module already instantiated");
        }
        cards = new IntDomainVar[rp.getNodes().length];
        this.rp = rp;
        instances = this;
        this.max = max;
    }
    public static Cardinalities getInstances() {
        return instances;
    }

    @Override
    public void reset() {
        cards = null;
        instances = null;
    }
    @Override
    public IntDomainVar getCardinality(int nIdx) {
        if (cards[nIdx] == null) {
            Node n = rp.getNode(nIdx);
            cards[nIdx] = rp.createBoundIntVar("cap(" + n.getName() + ")", max, max);
            IntDomainVar[] hs = Slices.extractHosters(rp.getDemandingSlices());
            SConstraint c = new Occurrence(ArrayUtils.append(hs, new IntDomainVar[]{cards[nIdx]}), nIdx, false, true, rp.getEnvironment());
            rp.post(c);
        }
        return cards[nIdx];
    }

    @Override
    public IntDomainVar getCardinality(Node n) {
        return getCardinality(rp.getNode(n));
    }
}
