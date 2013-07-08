package f4g.optimizer.entropy.plan.constraint;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 28/07/12
 * Time: 19:18
 * To change this template use File | Settings | File Templates.
 */
public interface Cardinalities {
    void reset();

    IntDomainVar getCardinality(int nIdx);

    IntDomainVar getCardinality(Node n);
}
