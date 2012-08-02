package org.f4g.entropy.plan.constraint;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;

/**
 * A core-RP extension to related to the management of the vCPU and pCPU.
 * In practice, it allows to count the number of vCPUs hosted on each server
 * and the number of used pCPUs.
 *
 * @author Fabien Hermenier
 */
public interface VcpuPcpuMapping {

    /**
     * Reset the model.
     */
    void reset();

    /**
     * get the number of vCPU on a server.
     * @param nIdx the server index
     * @return the associated variable
     */
    IntDomainVar getvCPUCount(int nIdx);


    /**
     * get the number of vCPU on a server.
     * @param n the server
     * @return the associated variable
     */
    IntDomainVar getvCPUCount(Node n);

    /**
     * Get the number of pCPU used on a server.
     * @param nIdx the server index
     * @return the variables indicating the number of pCPU used.
     */
    IntDomainVar getPcpuUsage(int nIdx);

    /**
     * Get the number of pCPU used on a server.
     * @param n the server
     * @return the variables indicating the number of pCPU used.
     */
    IntDomainVar getPcpuUsage(Node n);
}
