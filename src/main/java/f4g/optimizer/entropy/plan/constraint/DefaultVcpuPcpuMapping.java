package f4g.optimizer.entropy.plan.constraint;

import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.cp.solver.variables.integer.BoolVarNot;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.constraint.pack.FastBinPacking;

import java.util.Collections;

/**
 * Default implementation for {@link VcpuPcpuMapping}.
 *
 * @author Fabien Hermenier
 */
public class DefaultVcpuPcpuMapping implements VcpuPcpuMapping {

    private static VcpuPcpuMapping instances = null;

    private IntDomainVar [] vcpuCount;

    private IntDomainVar [] pCPUUsage;

    private static final int DEFAULT_MAX = 500;

    private ReconfigurationProblem rp;

    private int maxCapacity;
    /**
     * Initialize the extension with a maximum hosting capacity equals to {@link #DEFAULT_MAX}.
     * @param rp the core-RP to plug this extension to.
     */
    public DefaultVcpuPcpuMapping(ReconfigurationProblem rp) {
        this(rp, DEFAULT_MAX);
    }

    /**
     * Initialize the extension.
     * @param rp the core-RP to plug this extension to.
     * @param maxCapacity the default maximum capacity for each node
     */
    public DefaultVcpuPcpuMapping(ReconfigurationProblem rp, int maxCapacity) {
        if (instances != null) {
            Plan.logger.error("Module already instantiated");
        }
        this.rp = rp;
        this.maxCapacity = maxCapacity;
        Plan.logger.debug("DefaultVcpuPcpuMapping branched");
    }

    private void makevCPUCount() {
        vcpuCount = new IntDomainVar[rp.getNodes().length];
        instances = this;


        Node[] ns = rp.getNodes();
        ManagedElementSet<VirtualMachine> vms = rp.getFutureRunnings().clone();

        if (!vms.isEmpty()) {
            Collections.sort(vms, new VirtualMachineComparator(false, ResourcePicker.VMRc.nbOfCPUs));
            IntDomainVar [] vCPUSs = new IntDomainVar[vms.size()];
            IntDomainVar[] assigns = new IntDomainVar[vms.size()];
            for (int i = 0; i < vms.size(); i++) {
                VirtualMachine vm = vms.get(i);
                vCPUSs[i]  = rp.createIntegerConstant("vCpu(" + vm.getName() + ")", vm.getNbOfCPUs());
                assigns[i] = rp.getAssociatedAction(vm).getDemandingSlice().hoster();
            }

            for (int i = 0; i < ns.length; i++) {
                Node n = rp.getNode(i);
                if (rp.getFutureOfflines().contains(n)) {
                    vcpuCount[i] = rp.createIntegerConstant("vCPUCapa(" + n.getName() + ")", 0);
                } else if (rp.getFutureOnlines().contains(n)) {
                    vcpuCount[i] = rp.createBoundIntVar("vCPUCapa(" + n.getName() + ")", 0, maxCapacity);
                } else { //Manageable state
                    vcpuCount[i] = rp.createBoundIntVar("vCPUCapa(" + n.getName() + ")", 0, maxCapacity);
                    ManageableNodeActionModel action = (ManageableNodeActionModel) rp.getAssociatedAction(n);
                    IntDomainVar isOffline = new BoolVarNot(rp, "offline(" + n.getName() + ")", action.getState());
                    rp.post(new FastImpliesEq(isOffline, vcpuCount[i], 0));
                }

            }

            SConstraint s = new FastBinPacking(rp.getEnvironment(), vcpuCount, vCPUSs, assigns);
            rp.post(s);
        }
    }
    private void makePcpuUsage() {
        pCPUUsage = new IntDomainVar[rp.getNodes().length];
        for (int i = 0; i < pCPUUsage.length; i++) {
            Node n = rp.getNode(i);
            pCPUUsage[i] = rp.createBoundIntVar("usedPcpu(" + n.getName() + ")", 0, n.getNbOfCPUs());
        }
    }

    @Override
    public IntDomainVar getPcpuUsage(int nIdx) {
        if (instances == null) {
            makePcpuUsage();
            makevCPUCount();
        }
        return pCPUUsage[nIdx];
    }

    @Override
    public IntDomainVar getPcpuUsage(Node n) {
        return getPcpuUsage(rp.getNode(n));
    }

    @Override
    public void reset() {
        this.rp = null;
        this.vcpuCount = null;
        this.pCPUUsage = null;
        instances = null;
    }

    @Override
    public IntDomainVar getvCPUCount(int nIdx) {
        if (instances == null) {
            makePcpuUsage();
            makevCPUCount();
        }
        return vcpuCount[nIdx];
    }

    @Override
    public IntDomainVar getvCPUCount(Node n) {
        return getvCPUCount(rp.getNode(n));
    }

    public static VcpuPcpuMapping getInstances() {
        return instances;
    }
}
