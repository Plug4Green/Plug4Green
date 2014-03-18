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

package f4g.optimizer.entropy;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.view.ModelView;
import btrplace.model.view.ShareableResource;

import java.util.*;

/**
 *
 */
public class NamingService implements ModelView {

    /**
     * The base of the view identifier. Once instantiated, it is completed
     * by the resource identifier.
     */
    public static final String VIEW_ID_BASE = "NamingService.";
    
    private Map<Node, String> nodeNames;
    private Map<VM, String> VMNames;

    private String viewId;

    private String rcId;
    /**
     * Make a new resource that use {@link #DEFAULT_NO_VALUE}
     * for both VMs and nodes.
     *
     * @param r the resource identifier
     */
    public NamingService(String r) {
        this(r, new HashMap<Node, String>(), new HashMap<VM, String>());
    }

    /**
     * Make a new resource.
     *
     * @param id             the resource identifier
     * @param defCapacity    the nodes default capacity
     * @param defConsumption the VM default consumption
     */
    public NamingService(String id, Map<Node, String> nodeNames, Map<VM, String> VMNames) {
    	
        this.rcId = id;
        this.viewId = VIEW_ID_BASE + rcId;
        this.nodeNames = nodeNames;
        this.VMNames = VMNames;
    }
   
    /**
     * Get the node power Idle.
     *
     * @param n the node
     * @return its capacity if it was defined otherwise the default value.
     */
    public String getNodeName(Node n) {
        return nodeNames.get(n);
    }

        
    /**
     * Get the nodes with defined capacities
     *
     * @return a set that may be empty
     */
    public Set<Node> getNodes() {
        return nodeNames.keySet();
    }

    /**
     * Get the capacity for a list of nodes.
     *
     * @param ids the node identifiers
     * @return the capacity of each node. The order is maintained
     */
    public List<String> getNodeNames(List<Node> ids) {
        List<String> res = new ArrayList<>(ids.size());
        for (Node n : ids) {
            res.add(getNodeName(n));
        }
        return res;
    }
    /**
     * Get the node power Idle.
     *
     * @param n the node
     * @return its capacity if it was defined otherwise the default value.
     */
    public String getVMName(VM n) {
        return VMNames.get(n);
    }

        
    /**
     * Get the nodes with defined capacities
     *
     * @return a set that may be empty
     */
    public Set<VM> getVMs() {
        return VMNames.keySet();
    }

    /**
     * Get the capacity for a list of nodes.
     *
     * @param ids the node identifiers
     * @return the capacity of each node. The order is maintained
     */
    public List<String> getVMNames(List<VM> ids) {
        List<String> res = new ArrayList<>(ids.size());
        for (VM n : ids) {
            res.add(getVMName(n));
        }
        return res;
    }
    
    public void putNodeName(Node n, String s) {
    	nodeNames.put(n, s);
    }
    
    public void putVMName(VM vm, String s) {
    	VMNames.put(vm, s);
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
    public String getDefaultNodeName() {
        return "DefaultNode";
    }
    /**
     * Get the default node capacity.
     *
     * @return the value.
     */
    public String getDefaultVMName() {
        return "DefaultVM";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NamingService that = (NamingService) o;

        if (!that.getNodes().equals(nodeNames.keySet())){
            return false;
        }

        for (Node k : nodeNames.keySet()) {
            if (!nodeNames.get(k).equals(that.getNodeName(k))) {
                return false;
            }
        }
        
        for (VM k : VMNames.keySet()) {
            if (!VMNames.get(k).equals(that.getVMName(k))) {
                return false;
            }
        }
        
        return rcId.equals(that.getResourceIdentifier()) && getDefaultVMName() == that.getDefaultVMName()
                && getDefaultNodeName() == that.getDefaultNodeName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(rcId, nodeNames, VMNames);
    }
    
    @Override
    public NamingService clone() {
    	NamingService rc = new NamingService(rcId);
        for (Map.Entry<Node, String> e : nodeNames.entrySet()) {
            rc.nodeNames.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<VM, String> e : VMNames.entrySet()) {
            rc.VMNames.put(e.getKey(), e.getValue());
        }
        return rc;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("rc:").append(rcId).append(':');
        for (Iterator<Map.Entry<Node, String>> ite = nodeNames.entrySet().iterator(); ite.hasNext(); ) {
            Map.Entry<Node, String> e = ite.next();
            buf.append("<node ").append(e.getKey().toString()).append(',').append(e.getValue()).append('>');
            if (ite.hasNext()) {
                buf.append(',');
            }
        }
        for (Iterator<Map.Entry<VM, String>> ite = VMNames.entrySet().iterator(); ite.hasNext(); ) {
            Map.Entry<VM, String> e = ite.next();
            buf.append("<VM ").append(e.getKey().toString()).append(',').append(e.getValue()).append('>');
            if (ite.hasNext()) {
                buf.append(',');
            }
        }
        return buf.toString();
    }

}
