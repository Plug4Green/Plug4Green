/**
 * ============================== Header ============================== 
 * file:          ConstraintFactory.java
 * project:       FIT4Green/Optimizer
 * created:       28.09.2011 by Thomas Schulze
 * last modified: $LastChangedDate:  $ by $LastChangedBy: schulze@informatik.uni-mannheim.de $
 * revision:      $LastChangedRevision:  $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package org.f4g.entropy.plan.constraint;

import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.entropy.configuration.F4GConfigurationAdapter;
import org.f4g.schema.constraints.optimizerconstraints.Ban;
import org.f4g.schema.constraints.optimizerconstraints.Capacity;
import org.f4g.schema.constraints.optimizerconstraints.Fence;
import org.f4g.schema.constraints.optimizerconstraints.Gather;
import org.f4g.schema.constraints.optimizerconstraints.Lonely;
import org.f4g.schema.constraints.optimizerconstraints.Root;
import org.f4g.schema.constraints.optimizerconstraints.Split;
import org.f4g.schema.constraints.optimizerconstraints.Spread;
import org.f4g.schema.constraints.optimizerconstraints.VMGroup;
import org.f4g.schema.constraints.optimizerconstraints.ServerGroupType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.constraints.optimizerconstraints.ConstraintType.PlacementConstraint;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.LazySplit;
import entropy.vjob.VJob;

/**
 * Class containing all methods and parameters needed to add placement
 * constraints read from the input file to the optimizer
 * 
 * @author TS
 */
public class PlacementConstraintFactory {

	private VJob v;
	private Configuration src;
	private ServerGroupType sg;

	public PlacementConstraintFactory(Configuration src, FIT4GreenType model,
			ServerGroupType serverGroup) {
		Logger.getLogger(this.getClass().getName());
		v = new DefaultVJob("slaVJob");
		this.src = src;
		this.sg = serverGroup;
	}

	/**
	 * 
	 * Create new Placement Constraints
	 * 
	 * @param cp
	 *            The ConstraintReader previously created.
	 */

	private void createBan(
			org.f4g.schema.constraints.optimizerconstraints.ConstraintType.PlacementConstraint cp,
			ManagedElementSet<Node> nodes) {
		List<org.f4g.schema.constraints.optimizerconstraints.Ban> b = cp
				.getBan();
		for (Ban ban : b) {
			List<String> vmNames = ban.getVMName();
			entropy.vjob.Ban constraint = new entropy.vjob.Ban(getVMs(vmNames),
					nodes);
			v.addConstraint(constraint);
		}
	}

	private void createCapacity(PlacementConstraint cp, ManagedElementSet<Node> nodes) {
		List<Capacity> c = cp.getCapacity();
		for (Capacity cap : c) {
			int size = cap.getMaxNbOfVMs();
			F4GCapacity constraint = new F4GCapacity(nodes, size);
			v.addConstraint(constraint);
		}
	}

	private void createFence(PlacementConstraint cp, ManagedElementSet<Node> nodes) {
		List<Fence> f = cp.getFence();
		for (Fence fence : f) {
			List<String> vmNames = fence.getVMName();
			entropy.vjob.Fence constraint = new entropy.vjob.Fence(
					getVMs(vmNames), nodes);
			v.addConstraint(constraint);
		}
	}

	private void createGather(PlacementConstraint cp) {
		List<Gather> g = cp.getGather();
		for (Gather gather : g) {
			List<String> vmNames = gather.getVMName();
			entropy.vjob.Gather constraint = new entropy.vjob.Gather(
					getVMs(vmNames));
			v.addConstraint(constraint);
		}
	}

	private void createLonely(PlacementConstraint cp) {
		List<Lonely> l = cp.getLonely();
		for (Lonely lonely : l) {
			List<String> vmNames = lonely.getVMName();
			entropy.vjob.Lonely constraint = new entropy.vjob.Lonely(
					getVMs(vmNames));
			v.addConstraint(constraint);
		}
	}

	private void createRoot(PlacementConstraint cp) {
		List<Root> r = cp.getRoot();
		for (Root root : r) {
			List<String> vmNames = root.getVMName();
			entropy.vjob.Root constraint = new entropy.vjob.Root(
					getVMs(vmNames));
			v.addConstraint(constraint);
		}
	}

	private void createSplit(PlacementConstraint cp) {
		List<Split> s = cp.getSplit();
		for (Split split : s) {
			VMGroup vms1 = split.getVMGroup1();
			VMGroup vms2 = split.getVMGroup2();
			entropy.vjob.LazySplit constraint = new LazySplit(
					getVMs(vms1.getVMName()), getVMs(vms2.getVMName()));
			v.addConstraint(constraint);
		}
	}

	private void createSpread(PlacementConstraint cp) {
		List<Spread> s2 = cp.getSpread();
		for (Spread spread : s2) {
			List<String> vmNames = spread.getVMName();
			entropy.vjob.LazySpread constraint = new entropy.vjob.LazySpread(
					getVMs(vmNames));
			v.addConstraint(constraint);
		}
	}

	private ManagedElementSet<VirtualMachine> getVMs(List<String> vmNames) {
		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
		for (String vm : vmNames) {
			try {
				vms.add(src.getAllVirtualMachines().get(vm));
			} catch (Exception e) {
			}
		}
		return vms;
	}

	
	public VJob createPCConstraints() {

		try {
			List<org.f4g.schema.constraints.optimizerconstraints.ServerGroupType.ServerGroup> servergroups = sg
					.getServerGroup();
			for (org.f4g.schema.constraints.optimizerconstraints.ServerGroupType.ServerGroup serverG : servergroups) {
				// get all nodes in a sg
				ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
				for (String nodeName : serverG.getNodeController()
						.getNodeName()) {
					try {
						Node n = src.getAllNodes().get(nodeName);
						if (n != null) {
							nodes.add(n);
						}
					} catch (Exception e) {
					}

				}

				// get all VMs for these nodes
				ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
				for (Node node : nodes) {
					try {
						ManagedElementSet<VirtualMachine> vm = src
								.getRunnings(node);
						vms.addAll(vm);
					} catch (Exception e) {
					}

				}

				// get all bounded SLAs and add constraints to vjob
				for (org.f4g.schema.constraints.optimizerconstraints.BoundedPlacementConstraintType.PlacementConstraint s : serverG
						.getBoundedPlacementConstraints()
						.getPlacementConstraint()) {
					if (vms.size() > 0 && nodes.size() > 0)
						addConstraintsForPC(s.getIdref(), nodes);
				}
			}
		} catch (Exception e) {
		}
		return v;
	}

	private VJob addConstraintsForPC(
			org.f4g.schema.constraints.optimizerconstraints.ConstraintType.PlacementConstraint cp,
			ManagedElementSet<Node> nodes) {

		if (cp != null) {
			createBan(cp, nodes);
			createCapacity(cp, nodes);
			createFence(cp, nodes);
			createGather(cp);
			createLonely(cp);
			createRoot(cp);
			createSplit(cp);
			createSpread(cp);
		}
		return v;

	}

}
