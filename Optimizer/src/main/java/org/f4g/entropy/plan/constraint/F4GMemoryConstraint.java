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

import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.DatacenterType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.RAMStickType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.SiteType;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.*;

/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GMemoryConstraint extends F4GConstraint {

	private FIT4GreenType metamodel;
	int x; // GB of HDD for each VM.

	/**
	 * The set of nodes to exlude.
	 */
	private ManagedElementSet<Node> nodes;

	/**
	 * Make a new constraint.
	 * 
	 * @param vms
	 *            the VMs to assign
	 * @param nodes
	 *            the nodes to exclude
	 */
	public F4GMemoryConstraint(ManagedElementSet<Node> nodes, FIT4GreenType metamodel, int x) {
		this.nodes = nodes;
		this.metamodel = metamodel;
		this.x = x;
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
		F4GMemoryConstraint that = (F4GMemoryConstraint) o;

		return (nodes.equals(that.nodes) && getAllVirtualMachines().equals(
				that.getAllVirtualMachines()));
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

		buffer.append("F4GMemoryConstraint(");
		buffer.append(x);
		buffer.append(",");
		buffer.append(nodes.toString());
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Apply the constraint to the plan to all the VMs in a future running
	 * state. FIXME: What about running VMs that will be suspended ?
	 * 
	 * @param core
	 *            the plan to customize. Must implement
	 *            {@link entropy.plan.choco.ChocoCustomizablePlannerModule}
	 */
	@Override
	public void inject(ReconfigurationProblem core) {
		
		// For each node, we define a set denoting the VMs it may hosts
		IntDomainVar[] cards = new IntDomainVar[nodes.size()];
		
		for (int i = 0; i < nodes.size(); i++) {
			cards[i] = core.getSetModel(nodes.get(i)).getCard();
			core.post(core.leq(cards[i], getRAMCap(nodes.get(i).getName())/x));
		}
	}

	private int getRAMCap(String nodeName) {
		int i = 0;
		
		for (SiteType st : metamodel.getSite())
			for (DatacenterType dt : st.getDatacenter()) {
				// get all nodes in a DC
				for (ServerType server : Utils.getAllServers(dt)) {
					if (server.getFrameworkID().equals(nodeName))
						for (MainboardType m : server.getMainboard())
							for (RAMStickType r : m.getRAMStick())
								i += (int) (r.getSize().getValue());
				}
			}
		i = i/1024;
		return i;
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
        	if (configuration.getRunnings(n).size() > (getRAMCap(n.getName())/x)){
            	return false;
            }
        }
        return true;
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
            if (configuration.getRunnings(n).size() > getRAMCap(n.getName())/x){
            	bad.addAll(vms);
            }
        }
        return bad;
    }

}
