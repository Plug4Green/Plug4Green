 
package f4g.optimizer.entropy.configuration;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSets;
import entropy.configuration.Node;
import entropy.configuration.ResourcePicker;
import entropy.configuration.VirtualMachine;

/**
 * A tool to retrieve a resource from a virtual machine or a node using an identifier.
 *
 * @author Corentin Dupont
 */
public final class F4GResourcePicker {

    /**
     * The resources of a virtual machine.
     */
    public static enum VMRc {

        /**
         * retrieve {@link VirtualMachine#getNbOfCPUs()} .
         */
        nbOfCPUs,

        /**
         * retrieve {@link VirtualMachine#getCPUConsumption()}.
         */
        cpuConsumption,

        /**
         * retrieve {@link VirtualMachine#getCPUDemand()} .
         */
        cpuDemand,

        /**
         * retrieve {@link VirtualMachine#getMemoryConsumption()} .
         */
        memoryConsumption,

        /**
         * retrieve {@link VirtualMachine#getMemoryDemand()} .
         */
        memoryDemand
    }

    /**
     * The resources of a node.
     */
    public static enum NodeRc {

        /**
         * retrieve {@link Node#getNbOfCPUs()} .
         */
        nbOfCPUs,

        /**
         * retrieve {@link Node#getCPUCapacity()} .
         */
        cpuCapacity,

        /**
         * retrieve {@link Node#getMemoryCapacity()} .
         */
        memoryCapacity,
        
        /**
        * retrieve {@link Node#getPIdle()} .
        */
        powerIdle,
        
        /**
         * retrieve {@link Node#getPperVM()} .
         */
         powerPerVM,
        
        /**
         * 
         */
        cpuRemaining,
        
        /**
         * 
         */
        memoryRemaining,
        
        /**
         * 
         */
        numberVMsInitial
        
    }

    
    /**
     * Get the resource value of a virtual machine associated to an identifier.
     *
     * @param vm the virtual machine
     * @param r  the resource to retrieve
     * @return the value of the resource
     */
    public static int get(VirtualMachine vm, VMRc r) {
        switch (r) {
            case cpuConsumption:
                return vm.getCPUConsumption();
            case cpuDemand:
                return vm.getCPUDemand();
            case memoryConsumption:
                return vm.getMemoryConsumption();
            case memoryDemand:
                return vm.getMemoryDemand();
            case nbOfCPUs:
                return vm.getNbOfCPUs();
        }
        return -1;
    }

    /**
     * Get the resource value of a node associated to an identifier.
     *
     * @param n the node
     * @param r the resource to retrieve
     * @param cfg the initial configuration
     * @return the value of the resource
     */
    public static int get(F4GNode n, NodeRc r, Configuration cfg) {
        switch (r) {
            case cpuCapacity:
                return n.getCPUCapacity();
            case memoryCapacity:
                return n.getMemoryCapacity();
            case nbOfCPUs:
                return n.getNbOfCPUs();
            case powerIdle:
            	return n.getPIdle();
            case powerPerVM:
            	return n.getPperVM();
            case cpuRemaining:
                return n.getCPUCapacity() - ManagedElementSets.sum(cfg.getRunnings(n), ResourcePicker.VMRc.cpuConsumption)[0];
            case memoryRemaining:
                return n.getMemoryCapacity() - ManagedElementSets.sum(cfg.getRunnings(n), ResourcePicker.VMRc.memoryConsumption)[0];
            case numberVMsInitial:
                return cfg.getRunnings(n).size();

        }
        return -1;
    }
}