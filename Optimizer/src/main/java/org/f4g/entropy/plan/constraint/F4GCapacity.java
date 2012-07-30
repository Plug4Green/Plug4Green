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
     * @param nodes2 the nodes to consider
     * @param m  the maximum hosting capacity of all the nodes.
     */
    public F4GCapacity(ManagedElementSet<Node> nodes2, int m) {
        max = m;
        nodes = nodes2;
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
    	
//        ManagedElementSet<Node> elems = nodes.getElements().clone();
//
//        elems.retainAll(core.getFutureOnlines());
//
//        //For each node, we define a set denoting the VMs it may hosts
//        IntDomainVar[] cards = new IntDomainVar[elems.size()];
//
//        if (max == 0 && elems.size() > 0) {
//            //max == 0, so we directly remove the nodes
//            // from the VMs d-slices hoster variable.
//            int[] nIdxs = new int[nodes.size()];
//            int i = 0;
//            for (Node n : nodes.flatten()) {
//                nIdxs[i++] = core.getNode(n);
//            }
//            for (VirtualMachineActionModel a : core.getVirtualMachineActions()) {
//                DemandingSlice dSlice = a.getDemandingSlice();
//                if (dSlice != null) {
//                    for (int x = 0; i < nIdxs.length; x++) {
//                        try {
//                            dSlice.hoster().remVal(nIdxs[x]);
//                        } catch (ContradictionException e) {
//                            VJob.logger.error(e.getMessage(), e);
//                        }
//                    }
//                }
//            }
//        } else if (elems.size() > 1) {   //More than one node, so we restrict the sum of sets cardinality
//            Plan.logger.debug(elems.toString());
//            for (int i = 0; i < elems.size(); i++) {
//                SetVar s = core.getSetModel(elems.get(i));
//                //Plan.logger.debug(elems.get(i).getName() + " " + s.pretty());
//                cards[i] = core.getSetModel(elems.get(i)).getCard();
//            }
//            core.post(core.leq(core.sum(cards), max));
//        } else { //One node, only restrict the cardinality of its set model.
//        	cards[0] = core.getSetModel(elems.get(0)).getCard();
//            core.post(core.leq(cards[0], max));
//        }
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
//        int nb = 0;
//        for (Node n : nodes.getElements()) {
//            nb += configuration.getRunnings(n).size();
//        }
//        if (nb > max) {
//            VJob.logger.debug(nodes.pretty() + " host " + nb + " virtual machinew but maximum allowed is " + max);
//            return false;
//        }
//        return true;
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
