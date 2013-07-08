
package f4g.optimizer.entropy.plan.constraint;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.*;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GDelayBetweenMove extends F4GConstraint {

	/**
     * The VMs to manipulate.
     */
    private ManagedElementSet<VirtualMachine> vms;
    private int minutes;

    /**
     * Make a new constraint.
     *
     * @param v the VMs to consider.
     */
    public F4GDelayBetweenMove(ManagedElementSet<VirtualMachine> v) {
        this.vms = v;
    }

    /**
     * TODO: documentation
     *
     * @param core
     */
    @Override
    public void inject(ReconfigurationProblem core) {
        for (VirtualMachine vm : vms) {
            VirtualMachineActionModel a = core.getAssociatedAction(vm);
            if (a != null) {
                ConsumingSlice cSlice = a.getConsumingSlice();
                DemandingSlice dSlice = a.getDemandingSlice();
                if (cSlice != null && dSlice != null) {
                    core.post(core.eq(cSlice.hoster(), dSlice.hoster()));
                }
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
        return vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return new SimpleManagedElementSet<Node>();
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
        return new StringBuilder("F4GDelayBetweenMove(").append(getAllVirtualMachines()).append(")").toString();
    }

	
}
