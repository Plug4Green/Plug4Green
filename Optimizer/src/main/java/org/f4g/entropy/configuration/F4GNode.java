
package org.f4g.entropy.configuration;

import entropy.configuration.SimpleNode;

/**
 * @author Corentin Dupont
 */
public class F4GNode extends SimpleNode {

	private int PIdle;
	
	/**
     * Make a node and specify its resource capacity
     *
     * @param name           the identifier of the node
     * @param nbOfCPUs       the number of physical CPUs available to the VMs
     * @param cpuCapacity    the capacity of each physical CPU
     * @param memoryCapacity the memory capacity of each node
     * @param PIdle          the idle power of each node
     */
    public F4GNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity, int PIdle) {
        super(name, nbOfCPUs, cpuCapacity, memoryCapacity, null, null);
        this.PIdle = PIdle;
    }

    public int getPIdle() {
		return PIdle;
	}

	public void setPIdle(int pIdle) {
		PIdle = pIdle;
	}

}
