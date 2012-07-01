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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;


import choco.cp.solver.constraints.reified.FastIFFEq;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GAvgCPUOverbooking extends F4GConstraint {
	private double max;
	private FIT4GreenType model;
	private ManagedElementSet<Node> nodes;
	ManagedElementSet<VirtualMachine> freshVMs;

	/**
	 * Make a new constraint.
	 * 
	 * @param ns
	 *            the nodes to consider
	 * @param m
	 *            the maximum hosting capacity of all the nodes.
	 */
	public F4GAvgCPUOverbooking(ManagedElementSet<Node> ns, double m,
			FIT4GreenType myModel) {
		max = m;
		nodes = ns;
		model = myModel;
	}

	@Override
	public String toString() {
		return "CPUOverbookingAvgConstraint(" + nodes.toString() + ", " + max
				+ ")";
	}

	@Override
	public void inject(ReconfigurationProblem core) {

		ManagedElementSet<VirtualMachine> vms = core.getFutureRunnings();
		freshVMs = vms.clone();
		List<IntDomainVar> vCPUSs = new ArrayList<IntDomainVar>();
		for (int i = 0; i < vms.size(); i++) {
			VirtualMachine vm = vms.get(i);
			if (isInCluster(vm.getName(), vms))
				vCPUSs.add(core.createIntegerConstant("VCPUs for VM " + vm.getName(),
						vm.getNbOfCPUs()));
		}
		int fresh = vCPUSs.size();
		for (VirtualMachine vm : freshVMs){
			vCPUSs.add(core.createIntegerConstant("VCPUs for VM " + fresh++,
					vm.getNbOfCPUs()));
		}
		IntDomainVar[] demands = vCPUSs
				.toArray(new IntDomainVar[vCPUSs.size()]);

		IntDomainVar[] eidleServer = new IntDomainVar[nodes.size()];
		int i = 0;
		double rest;
		for (Node n : nodes) {
			IntDomainVar card = core.getSetModel(n).getCard();
			int nbOfCores = n.getNbOfCPUs();
			rest = (nbOfCores * max * 100) % 100;
			int overbookedNumberOfCores = (int) ((int) nbOfCores * max);
			overbookedNumberOfCores = overbookedNumberOfCores + (int) rest;
			eidleServer[i] = core.createEnumIntVar("NumberOfCores" + n.getName(),
					new int[] { 0, overbookedNumberOfCores });

			// A boolean variable to indicate whether the node is used or not
			IntDomainVar used = core.createBooleanVar("used(" + n.getName()
					+ ")");

			// A reified constraint: the boolean variable will be set to true if
			// and only if (iff) the constraint is consistent.
			// In practice, used =1 <=> cards[i] != 0
			core.post(ReifiedFactory.builder(used, core.neq(card, 0), core));

			// A special version of Iff: used = 1 <=> Eidle[i] =
			// overbookedNumberOfCores
			// so if the node is used, the Eidle[i] is set to the constant
			// value, otherwise it will be set to 0 (the only other possible
			// value)
			core.post(new FastIFFEq(used, eidleServer[i],
					overbookedNumberOfCores));
			i++;
			if (rest > 1)
				rest = rest -1;
		}

		core.post(core.leq(core.sum(demands), core.sum(eidleServer)));
	}

	private boolean isInCluster(String s, ManagedElementSet<VirtualMachine> vms) {
		List<ServerType> allServers = Utils.getAllServers(model);		
		for (ServerType server : allServers) {
			for (Node n : nodes){ 
				if (n.getName().equals(server.getFrameworkID())){
					for (VirtualMachineType vm : Utils.getVMs(server)) {
						if (vm.getFrameworkID().equals(s))
							return true;
					}
				}
			}
			//in order to find VMs that haven't been allocated before (mainly for new allocation request)
			for (VirtualMachine vm : vms){
				for (VirtualMachineType vmt : Utils.getVMs(server)) {
				if (vmt.getFrameworkID().equals(vm.getName()))
					freshVMs.remove(vm); 
				}
			}
		}
		return false;
	}

	/**
	 * Check that the nodes does not host a number of VMs greater than the
	 * maximum specified
	 * 
	 * @param configuration
	 *            the configuration to check
	 * @return {@code true} if the constraint is satisfied.
	 */
	@Override
	public boolean isSatisfied(Configuration configuration) {
//		double virtualCores = 0;
//		int physicalCores = 0;
//		for (Node n : configuration.getOnlines()) {
//			if (nodes.contains(n)) {
//				for (VirtualMachine vm : configuration.getRunnings(n)) {
//					virtualCores += vm.getNbOfCPUs();
//				}
//				if (configuration.getRunnings(n).size() > 0)
//					physicalCores += n.getNbOfCPUs() * max;
//			}
//		}
//
//		if (virtualCores > (int) (physicalCores)) {
//			return false;
//		}
		return true;
	}

	@Override
	public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
		return new SimpleManagedElementSet<VirtualMachine>();
	}

	/**
	 * If the amount of VMs exceed its capacity, it returns all the hosted VMs
	 * 
	 * @param configuration
	 *            the configuration to check
	 * @return a set of virtual machines that may be empty
	 */
	@Override
	public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
		double virtualCores = 0;
		int physicalCores = 0;
		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
		for (Node n : nodes) {
			for (VirtualMachine vm : configuration.getRunnings(n)) {
				virtualCores += vm.getNbOfCPUs();
				vms.add(vm);
			}
			physicalCores += n.getNbOfCPUs();
		}
		if (virtualCores <= (int) (physicalCores * max)) {
			vms.clear();
		}

		return vms;

	}

	/**
	 * Get the nodes involved in the constraint.
	 * 
	 * @return a set of nodes. Should not be empty
	 */
	public ManagedElementSet<Node> getNodes() {
		return this.nodes;
	}

	/**
	 * Get the maximum number of virtual machines the set of nodes can host
	 * simultaneously
	 * 
	 * @return a positive integer
	 */
	public double getMaximumCapacity() {
		return this.max;
	}

}
