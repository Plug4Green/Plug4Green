
package f4g.optimizer.entropy.configuration;

import entropy.configuration.SimpleNode;

/**
 * Augments Entropy simple nodes with energetic data.
 * @author Corentin Dupont
 */
public class F4GNode extends SimpleNode {

	//Power idle of the server
	private int PIdle;
	
	//Power consumed by a VM on average on this server
	private int PperVM;
	
	
	/**
     * Make a node and specify its resource capacity
     *
     * @param name           the identifier of the node
     * @param nbOfCPUs       the number of physical CPUs available to the VMs
     * @param cpuCapacity    the capacity of each physical CPU
     * @param memoryCapacity the memory capacity of each node
     * @param PIdle          the idle power of each node
     */
    public F4GNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity, int PIdle, int PperVM) {
        super(name, nbOfCPUs, cpuCapacity, memoryCapacity, null, null);
        this.PIdle = PIdle;
        this.PperVM = PperVM;
    }

    public int getPIdle() {
		return PIdle;
	}

	public void setPIdle(int pIdle) {
		PIdle = pIdle;
	}

	public int getPperVM() {
		return PperVM;
	}

	public void setPperVM(int pperVM) {
		PperVM = pperVM;
	}

}
