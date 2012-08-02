/**
 * ============================== Header ============================== 
 * file:          F4GBan.java
 * project:       FIT4Green/Optimizer
 * created:       07.10.2011 by ts
 * last modified: $LastChangedDate: 2012-05-02 00:47:35 +0200 (mié, 02 may 2012) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 1411 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package org.f4g.entropy.plan.constraint;


import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import org.apache.log4j.Logger;

/**
 * New version that uses VcpuPcpuMapping.
 * This constraint establishes a relationship between the number of vCPU hosted on a server
 * and the number of pCPU used.
 * The overbooking factor indicates the number of vCPU one pCPU can host.
 * @author ts
 */
public class F4GCPUOverbookingConstraint2 extends F4GConstraint {

	double overbookingFactor; // Percent of overbooking (e.g. 1.20)
	public Logger log;

	/**
	 * The set of nodes
	 */
	private ManagedElementSet<Node> nodes;

	/**
	 * Make a new constraint.
	 *
	 * @param nodes
	 *            the nodes to exclude
     * @param MyOverbookingFactor the overbooking factor
	 */
	public F4GCPUOverbookingConstraint2(ManagedElementSet<Node> nodes, Double MyOverbookingFactor) {
		this.nodes = nodes;
		this.overbookingFactor = MyOverbookingFactor;
		log = Logger.getLogger(this.getClass().getName());
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
		F4GCPUOverbookingConstraint2 that = (F4GCPUOverbookingConstraint2) o;

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

		buffer.append("F4GCPUOverbookingConstraint(");
		buffer.append(overbookingFactor);
		buffer.append(", ");
		buffer.append(nodes.toString());
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Apply the constraint to the plan to all the VMs in a future running
	 * state. FIXME: What about running VMs that will be suspended ?
	 * 
	 * @param core the plan to customize. Must implement
	 */
	@Override
	public void inject(ReconfigurationProblem core) {

        VcpuPcpuMapping mapping = DefaultVcpuPcpuMapping.getInstances();
        if (mapping == null) {
            mapping = new DefaultVcpuPcpuMapping(core);
        }

        for (Node n : nodes) {
            IntDomainVar nbVCpus = mapping.getvCPUCount(n);
            IntDomainVar usedPcpus = mapping.getPcpuUsage(n);
            core.eq(usedPcpus, core.div(nbVCpus, (int)overbookingFactor));
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
        	int virtualCores = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualCores += vm.getNbOfCPUs();
        	}
        	if (virtualCores > n.getNbOfCPUs() * overbookingFactor){
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
        for (Node n : nodes) {
        	int virtualCores = 0;
        	for(VirtualMachine vm : configuration.getRunnings(n)) {
        		virtualCores += vm.getNbOfCPUs();
        	}
            ManagedElementSet<VirtualMachine> vms = configuration.getRunnings(n);
            if (virtualCores > n.getNbOfCPUs() * overbookingFactor){
            	bad.addAll(vms);
            }
        }
        return bad;
    }

}
