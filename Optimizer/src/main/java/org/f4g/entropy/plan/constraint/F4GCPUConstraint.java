/**
* ============================== Header ============================== 
* file:          F4GBan.java
* project:       FIT4Green/Optimizer
* created:       07.10.2011 by ts
* last modified: $LastChangedDate: 2010-11-26 11:33:26 +0100 (Fr, 26 Nov 2010) $ by $LastChangedBy: corentin.dupont@create-net.org $
* revision:      $LastChangedRevision: 150 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.entropy.plan.constraint;

import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.*;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GCPUConstraint extends F4GConstraint {
	
	 /**
     * The set of nodes to exlude.
     */
    private ManagedElementSet<Node> nodes;
    private double x;

  
    /**
     * Make a new constraint.
     *
     * @param vms   the VMs to assign
     * @param nodes the nodes to exclude
     */
    public F4GCPUConstraint(ManagedElementSet<Node> nodes, Double x) {
        this.nodes = nodes;
        this.x = x*100;

    }

    /**
     * Get the set of nodes involved in the constraint.
     *
     * @return a set of nodes
     */
    public ManagedElementSet<Node> getNodes() {
        return this.nodes;
    }    

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        F4GCPUConstraint that = (F4GCPUConstraint) o;

        return (nodes.equals(that.nodes) && getAllVirtualMachines().equals(that.getAllVirtualMachines()));
    }

    @Override
    public int hashCode() {
        int result = getAllVirtualMachines().hashCode();
        result = 31 * result + nodes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("F4GCPUConstraint(");
        buffer.append(x);
        buffer.append(",");
        buffer.append(nodes.toString());
        buffer.append(")");
        return buffer.toString();
    }


    /**
     * Apply the constraint to the plan to all the VMs in a future running state.
     * FIXME: What about running VMs that will be suspended ?
     *
     * @param core the plan to customize. Must implement {@link entropy.plan.choco.ChocoCustomizablePlannerModule}
     */
    @Override
    public void inject(ReconfigurationProblem core) {

    	for (Node n : getNodes()) {
    		core.post(core.leq(core.getUsedCPU(n), (int) x));
        }
    	
    }

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the VMs are not running on the banned nodes.
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        ManagedElementSet<Node> ns = getNodes();
        int i;
        for (Node n : getNodes()) {
    		       i = 0;
        for (VirtualMachine vm : cfg.getAllVirtualMachines()) {
            if (cfg.isRunning(vm) &&  cfg.getLocation(vm).equals(n)) {
                i = i + vm.getCPUConsumption();
            }
            if ((i/n.getNbOfCPUs()) > x)
            	return false;
        }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
    	ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<VirtualMachine> realyBad = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<Node> ns = getNodes();
        int i;
        for (Node n : getNodes()) {
    		       i = 0;
    		       bad.clear();
        for (VirtualMachine vm : getAllVirtualMachines()) {
            if (cfg.isRunning(vm) &&  cfg.getLocation(vm).equals(n)) {
                bad.add(vm);
            }            
        }
        if ((i/n.getNbOfCPUs()) > x)
        	realyBad.addAll(bad);
        }
        
        return realyBad;
    }


}
