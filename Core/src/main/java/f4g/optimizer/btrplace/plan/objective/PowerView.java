/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4g.optimizer.btrplace.plan.objective;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

import java.util.*;

/**
 *
 */
public class PowerView implements ModelView {

    /**
     * The base of the view identifier. Once instantiated, it is completed
     * by the resource identifier.
     */
    public static final String VIEW_ID_BASE = "PowerView.";
    
    private Map<Node, Integer> powers;

    private int powerNoValue;
    
    private String viewId;
    private String rcId;

    public static final int DEFAULT_NO_VALUE = 0;

    /**
     * Make a new resource that use {@link #DEFAULT_NO_VALUE}
     * for both VMs and nodes.
     *
     * @param r the resource identifier
     */
    public PowerView(String r) {
        this(r, DEFAULT_NO_VALUE);
    }

    /**
     * Make a new resource.
     *
     * @param id             the resource identifier
     * @param defCapacity    the nodes default capacity
     * @param defConsumption the VM default consumption
     */
    public PowerView(String id, Integer defPower) {
    	powers = new HashMap<>();
        this.rcId = id;
        this.viewId = VIEW_ID_BASE + rcId;
        this.powerNoValue = defPower;
    }
   
    /**
     * Get the node power Idle.
     *
     * @param n the node
     * @return its capacity if it was defined otherwise the default value.
     */
    public int getPower(Node n) {
        if (powers.containsKey(n)) {
            return powers.get(n);
        }
        return powerNoValue;
    }

    public void setPowers(Node n, Integer p) {
    	powers.put(n, p);
    }
    
    /**
     * Get the nodes with defined capacities
     *
     * @return a set that may be empty
     */
    public Set<Node> getDefinedNodes() {
        return powers.keySet();
    }

    /**
     * Get the capacity for a list of nodes.
     *
     * @param ids the node identifiers
     * @return the capacity of each node. The order is maintained
     */
    public List<Integer> getPowers(List<Node> ids) {
        List<Integer> res = new ArrayList<>(ids.size());
        for (Node n : ids) {
            res.add(getPower(n));
        }
        return res;
    }
    
    /**
     * Get the view identifier.
     *
     * @return {@code "ShareableResource.rcId"} where rcId equals {@link #getResourceIdentifier()}
     */
    @Override
    public String getIdentifier() {
        return viewId;
    }

	@Override
	public boolean substituteVM(VM curId, VM nextId) {
		// TODO Auto-generated method stub
		return false;
	}
   
    /**
     * Get the resource identifier
     *
     * @return a non-empty string
     */
    public String getResourceIdentifier() {
        return rcId;
    }

    /**
     * Get the default node capacity.
     *
     * @return the value.
     */
    public int getDefaultPower() {
        return powerNoValue;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PowerView that = (PowerView) o;

        if (!that.getDefinedNodes().equals(powers.keySet())){
            return false;
        }

        for (Node k : powers.keySet()) {
            if (!powers.get(k).equals(that.getPower(k))) {
                return false;
            }
        }
                
        return rcId.equals(that.getResourceIdentifier())
                && getDefaultPower() == that.getDefaultPower();
    }

    @Override
    public int hashCode() {
        return Objects.hash(rcId, powers, powerNoValue);
    }
    
    @Override
    public PowerView clone() {
    	PowerView rc = new PowerView(rcId, powerNoValue);
        for (Map.Entry<Node, Integer> e : powers.entrySet()) {
            rc.powers.put(e.getKey(), e.getValue());
        }
        return rc;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("rc:").append(rcId).append(':');
        for (Iterator<Map.Entry<Node, Integer>> ite = powers.entrySet().iterator(); ite.hasNext(); ) {
            Map.Entry<Node, Integer> e = ite.next();
            buf.append("<node ").append(e.getKey().toString()).append(',').append(e.getValue()).append('>');
            if (ite.hasNext()) {
                buf.append(',');
            }
        }
        return buf.toString();
    }

}
