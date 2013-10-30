
package f4g.optimizer.entropy.plan.constraint;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GDelayBetweenOnOff extends F4GConstraint {

	/**
     * The Nodes to manipulate.
     */
    private ManagedElementSet<Node> nodes;

    /**
     * Make a new constraint.
     *
    */
    public F4GDelayBetweenOnOff(ManagedElementSet<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * TODO: documentation
     *
     * @param core
     */
    @Override
    public void inject(ReconfigurationProblem core) {
        for (Node node : nodes) {
        	
            if (! core.getFutureOfflines().contains(node) && ! core.getFutureOnlines().contains(node)) { //Manageable state

                ManageableNodeActionModel action = (ManageableNodeActionModel) core.getAssociatedAction(node);

                int online = core.getSourceConfiguration().isOnline(node) ? 1 : 0;
                core.post(core.eq(action.getState(), online));
            }            
        }
    }

    /**
     * Entailed method
     *
     * @param configuration the configuration to check
     * @return {@code true}
     */
    @Override
    public boolean isSatisfied(Configuration configuration) {
        return true;
    }

    /**
     * Get the VMs involved in the constraint.
     *
     * @return a set of virtual machines, should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return nodes;
    }

    /**
     * Entailed method. No VMs may be misplaced without consideration of the reconfiguration plan.
     *
     * @param configuration the configuration to check
     * @return an empty set
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    @Override
    public String toString() {
        return new StringBuilder("F4GDelayBetweenOnOff(").append(getAllVirtualMachines()).append(")").toString();
    }

	
}
