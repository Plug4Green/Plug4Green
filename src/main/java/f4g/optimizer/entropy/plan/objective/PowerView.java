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

package f4g.optimizer.entropy.plan.objective;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.view.ModelView;
import btrplace.model.view.ShareableResource;

import java.util.*;

/**
 *
 */
public class PowerView implements ModelView {

    /**
     * The base of the view identifier. Once instantiated, it is completed
     * by the resource identifier.
     */
    public static final String VIEW_ID_BASE = "PowerView";

    public static class Powers { 
    	public Integer PIdle; 
    	public Integer PperVM; }
    
    private Map<Node, Powers> powersNode;

    private int PIdleNoValue;
    private int PperVMNoValue;

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
        this(r, DEFAULT_NO_VALUE, DEFAULT_NO_VALUE);
    }

    /**
     * Make a new resource.
     *
     * @param id             the resource identifier
     * @param defCapacity    the nodes default capacity
     * @param defConsumption the VM default consumption
     */
    public PowerView(String id, int defPIdle, int defPperVM) {
    	powersNode = new HashMap<>();
        this.rcId = id;
        this.viewId = VIEW_ID_BASE + rcId;
        this.PIdleNoValue = defPIdle;
        this.PperVMNoValue = defPperVM;
    }
   
    /**
     * Get the node power Idle.
     *
     * @param n the node
     * @return its capacity if it was defined otherwise the default value.
     */
    public int getPowerIdle(Node n) {
        if (powersNode.containsKey(n)) {
            return powersNode.get(n).PIdle;
        }
        return PIdleNoValue;
    }

    public void setPowers(Node n, Powers p) {
        powersNode.put(n, p);
    }
    
    /**
     * Get the node power per VM.
     *
     * @param n the node
     * @return its capacity if it was defined otherwise the default value.
     */
    public int getPowerperVM(Node n) {
        if (powersNode.containsKey(n)) {
            return powersNode.get(n).PperVM;
        }
        return PperVMNoValue;

    }
    
    /**
     * Get the nodes with defined capacities
     *
     * @return a set that may be empty
     */
    public Set<Node> getDefinedNodes() {
        return powersNode.keySet();
    }

    /**
     * Get the capacity for a list of nodes.
     *
     * @param ids the node identifiers
     * @return the capacity of each node. The order is maintained
     */
    public List<Integer> getPowerIdles(List<Node> ids) {
        List<Integer> res = new ArrayList<>(ids.size());
        for (Node n : ids) {
            res.add(getPowerIdle(n));
        }
        return res;
    }
    
    /**
     * Get the capacity for a list of nodes.
     *
     * @param ids the node identifiers
     * @return the capacity of each node. The order is maintained
     */
    public List<Integer> getPowerperVMs(List<Node> ids) {
        List<Integer> res = new ArrayList<>(ids.size());
        for (Node n : ids) {
            res.add(getPowerperVM(n));
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
    public int getDefaultPowerIdle() {
        return PIdleNoValue;
    }
    /**
     * Get the default node capacity.
     *
     * @return the value.
     */
    public int getDefaultPowerPerVM() {
        return PperVMNoValue;
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

        if (!that.getDefinedNodes().equals(powersNode.keySet())){
            return false;
        }

        for (Node k : powersNode.keySet()) {
            if (!powersNode.get(k).PIdle.equals(that.getPowerIdle(k))) {
                return false;
            }
        }
        
        for (Node k : powersNode.keySet()) {
            if (!powersNode.get(k).PperVM.equals(that.getPowerperVM(k))) {
                return false;
            }
        }
        
        return rcId.equals(that.getResourceIdentifier()) && getDefaultPowerPerVM() == that.getDefaultPowerPerVM()
                && getDefaultPowerIdle() == that.getDefaultPowerIdle();
    }

    @Override
    public int hashCode() {
        return Objects.hash(rcId, powersNode, PIdleNoValue, PperVMNoValue);
    }
    
    @Override
    public PowerView clone() {
    	PowerView rc = new PowerView(rcId, PIdleNoValue, PperVMNoValue);
        for (Map.Entry<Node, Powers> e : powersNode.entrySet()) {
            rc.powersNode.put(e.getKey(), e.getValue());
        }
        return rc;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("rc:").append(rcId).append(':');
        for (Iterator<Map.Entry<Node, Powers>> ite = powersNode.entrySet().iterator(); ite.hasNext(); ) {
            Map.Entry<Node, Powers> e = ite.next();
            buf.append("<node ").append(e.getKey().toString()).append(',').append(e.getValue().PIdle).append(',').append(e.getValue().PperVM).append('>');
            if (ite.hasNext()) {
                buf.append(',');
            }
        }
        return buf.toString();
    }

}
