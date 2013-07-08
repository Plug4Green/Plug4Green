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
import f4g.schemas.java.metamodel.FIT4GreenType;

/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GOverbookingConstraint extends F4GConstraint {

	private FIT4GreenType metamodel;
	private ManagedElementSet<VirtualMachine> vms;
	double x; // Percent of overbooking (e.g. 1.20)

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
	public F4GOverbookingConstraint(ManagedElementSet<Node> nodes, FIT4GreenType metamodel, Double x, ManagedElementSet<VirtualMachine> vms) {
		this.nodes = nodes;
		this.metamodel = metamodel;
		this.x = x;
		this.vms = vms;
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
		F4GOverbookingConstraint that = (F4GOverbookingConstraint) o;

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

		buffer.append("F4GOverbookingConstraint(");
		buffer.append(x);
		buffer.append(",");
		buffer.append(vms.toString());
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

        Cardinalities c = PackingBasedCardinalities.getInstances();
        if (c == null) {
            c = new PackingBasedCardinalities(core, 50);
        }

        if(vms.size()!=0) {
			for (int i = 0; i < nodes.size(); i++) {
				cards[i] = c.getCardinality(nodes.get(i));
				core.post(core.leq(cards[i], calculateMaxVMsPerNode(nodes.get(i))));
			}
		}
		
	}

	private int calculateMaxVMsPerNode(Node n) throws ArithmeticException {
		int i = 0;
		for (VirtualMachine vm : vms){
			if (vm.getNbOfCPUs() > i){
				i = vm.getNbOfCPUs();
			}
		}
		int k = ((n.getNbOfCPUs()/i)* (int) x);
		return k;
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
        if(vms.size() != 0) {
        	for (Node n : nodes) {
            	if (configuration.getRunnings(n).size() > (calculateMaxVMsPerNode(n))){
                	return false;
                }
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
        if(vms.size() != 0) {
	        for (Node n : nodes) {
	            ManagedElementSet<VirtualMachine> vms = configuration.getRunnings(n);
	            if (configuration.getRunnings(n).size() > calculateMaxVMsPerNode(n)){
	            	bad.addAll(vms);
	            }
	        }
        }
        return bad;
    }
    
    @Override
	public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
		return vms;
	}

}
