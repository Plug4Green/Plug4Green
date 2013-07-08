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
package f4g.optimizer.entropy.plan.constraint;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GCapacity extends F4GConstraint {
	private int max;

    private ManagedElementSet<Node> nodes;

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to consider
     * @param m  the maximum hosting capacity of all the nodes.
     */
    public F4GCapacity(ManagedElementSet<Node> nodes, int m) {
        max = m;
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "capacity(" + nodes.toString() + ", " + max + ")";
    }

    @Override
    public void inject(ReconfigurationProblem core) {
    	
		
		// For each node, we define a set denoting the VMs it may hosts
		IntDomainVar[] cards = new IntDomainVar[nodes.size()];

        Cardinalities c = PackingBasedCardinalities.getInstances();
        if (c == null) {
            c = new PackingBasedCardinalities(core, 50);
        }

        for (int i = 0; i < nodes.size(); i++) {
			cards[i] = c.getCardinality(nodes.get(i));
			core.post(core.leq(cards[i], max));
		}
    	
    }

    /**
     * Check that the nodes does not host a number of VMs greater
     * than the maximum specified
     *
     * @param configuration the configuration to check
     * @return {@code true} if the constraint is satisfied.
     */
    @Override
    public boolean isSatisfied(Configuration configuration) {
    	for (Node n : nodes) {
        	if (configuration.getRunnings(n).size() > max){
            	return false;
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    /**
     * If the amount of VMs exceed its capacity, it returns all the hosted VMs
     *
     * @param configuration the configuration to check
     * @return a set of virtual machines that may be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
    	ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        int nb = 0;
        for (Node n : nodes) {
            ManagedElementSet<VirtualMachine> vms = configuration.getRunnings(n);
            nb += vms.size();
            bad.addAll(vms); // just in case to avoid a double loop
        }
        if (nb < getMaximumCapacity()) {
            bad.clear(); //Its clean, so no VMs are misplaced
        }
        return bad;
    }

    /**
     * Get the nodes involved in the constraint.
     *
     * @return a set of nodes. Should not be empty
     */
    public ManagedElementSet<Node> getNodes() {
        return this.nodes;
    }

    /**
     * Get the maximum number of virtual machines
     * the set of nodes can host simultaneously
     *
     * @return a positive integer
     */
    public int getMaximumCapacity() {
        return this.max;
    }

}
