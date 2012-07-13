package org.f4g.entropy.configuration;

import entropy.configuration.*;
import gnu.trove.TIntArrayList;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.f4g.entropy.configuration.F4GNode;
import org.f4g.entropy.configuration.F4GResourcePicker;

/**
 * Compare servers wrt. the amount of free resources on them.
 * @author Fabien Hermenier
 */
public class F4GNodeComparator implements Serializable, Comparator<Node> {

    /**
     * The comparison criteria.
     */
    private List<F4GResourcePicker.NodeRc> rcs;

    /**
     * Indicates the ordering type for each criterion.
     * 1 for an ascending, -1 otherwise
     */
    private TIntArrayList ascendings;

    private Configuration cfg;
    
    /**
     * Create a new comparator.
     *
     * @param asc true to make a ascending comparison
     * @param rc  the comparison criteria
     */
    public F4GNodeComparator(boolean asc, F4GResourcePicker.NodeRc rc, Configuration cfg) {
        this.rcs = new LinkedList<F4GResourcePicker.NodeRc>();
        this.ascendings = new TIntArrayList();
        this.appendCriteria(asc, rc);
        this.cfg = cfg;
    }

    /**
     * Add a sorting criteria.
     *
     * @param rc  the identifier of comparison criteria
     * @param asc true for an ascending comparison.
     */
    public final void appendCriteria(boolean asc, F4GResourcePicker.NodeRc rc) {
        this.rcs.add(rc);
        if (asc) {
            this.ascendings.add(1);
        } else {
            this.ascendings.add(-1);
        }
    }

    /**
     * Compare two managed element.
     * The comparison is made following the list of criterion specified in the constructor. The criterion
     * are compared in the order they were specified and the comparison stop after the first difference between
     * a comparison. If the value of a comparison criteria is null for e1 or e2, this criteria comparison is ignored.
     *
     * @param n1 The first element
     * @param n2 The second element
     * @return a negative, zero or positive integer that indicates respectively that
     *         e1 is before, equals or after e2 for the specific sort declared in the constructor.
     */
    @Override
    public int compare(Node n1, Node n2) {
        for (int i = 0; i < rcs.size(); i++) {
            int v1 = F4GResourcePicker.get((F4GNode)n1, rcs.get(i), cfg);
            int v2 = F4GResourcePicker.get((F4GNode)n2, rcs.get(i), cfg);
            int res = ascendings.get(i) * (v1 - v2);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

	
}
