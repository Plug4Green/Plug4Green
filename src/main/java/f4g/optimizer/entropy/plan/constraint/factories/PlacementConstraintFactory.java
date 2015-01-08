///**
// * ============================== Header ============================== 
// * file:          ConstraintFactory.java
// * project:       FIT4Green/Optimizer
// * created:       28.09.2011 by Thomas Schulze
// * last modified: $LastChangedDate:  $ by $LastChangedBy: schulze@informatik.uni-mannheim.de $
// * revision:      $LastChangedRevision:  $
// * 
// * short description:
// *   {To be completed}
// * ============================= /Header ==============================
// */
//package f4g.optimizer.entropy.plan.constraint.factories;
//
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//import org.apache.log4j.Logger;
//
//import org.btrplace.model.Mapping;
//import org.btrplace.model.constraint.SatConstraint;
//import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
//import f4g.schemas.java.constraints.optimizerconstraints.Ban;
//import f4g.schemas.java.constraints.optimizerconstraints.Capacity;
//import f4g.schemas.java.constraints.optimizerconstraints.Fence;
//import f4g.schemas.java.constraints.optimizerconstraints.Gather;
//import f4g.schemas.java.constraints.optimizerconstraints.Lonely;
//import f4g.schemas.java.constraints.optimizerconstraints.Root;
//import f4g.schemas.java.constraints.optimizerconstraints.Split;
//import f4g.schemas.java.constraints.optimizerconstraints.Spread;
//import f4g.schemas.java.constraints.optimizerconstraints.VMGroup;
//import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType;
//import f4g.schemas.java.metamodel.FIT4GreenType;
//import f4g.schemas.java.constraints.optimizerconstraints.ConstraintType.PlacementConstraint;
//
//import org.btrplace.model.Node;
//import org.btrplace.model.VM;
///**
// * Class containing all methods and parameters needed to add placement
// * constraints read from the input file to the optimizer
// * 
// * @author TS
// */
//public class PlacementConstraintFactory {
//
//	private List<SatConstraint> v;
//	private Mapping src;
//	private ServerGroupType sg;
//
//	public PlacementConstraintFactory(Mapping src, FIT4GreenType model,
//			ServerGroupType serverGroup) {
//		Logger.getLogger(this.getClass().getName());
//		v = new LinkedList<SatConstraint>();
//		this.src = src;
//		this.sg = serverGroup;
//	}
//
//	/**
//	 * 
//	 * Create new Placement Constraints
//	 * 
//	 * @param cp
//	 *            The ConstraintReader previously created.
//	 */
//
//	private void createBan(PlacementConstraint cp,	Set<Node> nodes) {
//		List<f4g.schemas.java.constraints.optimizerconstraints.Ban> b = cp
//				.getBan();
//		for (Ban ban : b) {
//			List<String> vmNames = ban.getVMName();
//			Ban constraint = new Ban(getVMs(vmNames), nodes);
//			v.add(constraint);
//		}
//	}
//
//	private void createCapacity(PlacementConstraint cp, Set<Node> nodes) {
//		List<Capacity> c = cp.getCapacity();
//		for (Capacity cap : c) {
//			int size = cap.getMaxNbOfVMs();
//			F4GCapacity constraint = new F4GCapacity(nodes, size);
//			v.add(constraint);
//		}
//	}
//
//	private void createFence(PlacementConstraint cp, Set<Node> nodes) {
//		List<Fence> f = cp.getFence();
//		for (Fence fence : f) {
//			List<String> vmNames = fence.getVMName();
//			entropy.vjob.Fence constraint = new entropy.vjob.Fence(
//					getVMs(vmNames), nodes);
//			v.addConstraint(constraint);
//		}
//	}
//
//	private void createGather(PlacementConstraint cp) {
//		List<Gather> g = cp.getGather();
//		for (Gather gather : g) {
//			List<String> vmNames = gather.getVMName();
//			entropy.vjob.Gather constraint = new entropy.vjob.Gather(
//					getVMs(vmNames));
//			v.addConstraint(constraint);
//		}
//	}
//
//	private void createLonely(PlacementConstraint cp) {
//		List<Lonely> l = cp.getLonely();
//		for (Lonely lonely : l) {
//			List<String> vmNames = lonely.getVMName();
//			entropy.vjob.Lonely constraint = new entropy.vjob.Lonely(
//					getVMs(vmNames));
//			v.addConstraint(constraint);
//		}
//	}
//
//	private void createRoot(PlacementConstraint cp) {
//		List<Root> r = cp.getRoot();
//		for (Root root : r) {
//			List<String> vmNames = root.getVMName();
//			entropy.vjob.Root constraint = new entropy.vjob.Root(
//					getVMs(vmNames));
//			v.addConstraint(constraint);
//		}
//	}
//
//	private void createSplit(PlacementConstraint cp) {
//		List<Split> s = cp.getSplit();
//		for (Split split : s) {
//			VMGroup vms1 = split.getVMGroup1();
//			VMGroup vms2 = split.getVMGroup2();
//			entropy.vjob.LazySplit constraint = new LazySplit(
//					getVMs(vms1.getVMName()), getVMs(vms2.getVMName()));
//			v.addConstraint(constraint);
//		}
//	}
//
//	private void createSpread(PlacementConstraint cp) {
//		List<Spread> s2 = cp.getSpread();
//		for (Spread spread : s2) {
//			List<String> vmNames = spread.getVMName();
//			entropy.vjob.LazySpread constraint = new entropy.vjob.LazySpread(
//					getVMs(vmNames));
//			v.addConstraint(constraint);
//		}
//	}
//
//	private ManagedElementSet<VirtualMachine> getVMs(List<String> vmNames) {
//		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
//		for (String vm : vmNames) {
//			try {
//				vms.add(src.getAllVirtualMachines().get(vm));
//			} catch (Exception e) {
//			}
//		}
//		return vms;
//	}
//
//	
//	public VJob createPCConstraints() {
//
//		try {
//			List<f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType.ServerGroup> servergroups = sg
//					.getServerGroup();
//			for (f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType.ServerGroup serverG : servergroups) {
//				// get all nodes in a sg
//				Set<Node> nodes = new HashSet<Node>();
//				for (String nodeName : serverG.getNodeController()
//						.getNodeName()) {
//					try {
//						Node n = src.getAllNodes().get(nodeName);
//						if (n != null) {
//							nodes.add(n);
//						}
//					} catch (Exception e) {
//					}
//
//				}
//
//				// get all VMs for these nodes
//				Set<VM> vms = new HashSet<VM>();
//				for (Node node : nodes) {
//					try {
//						Set<VM> vm = src.getRunnings(node);
//						vms.addAll(vm);
//					} catch (Exception e) {
//					}
//
//				}
//
//				// get all bounded SLAs and add constraints to vjob
//				for (f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraintType.PlacementConstraint s : serverG
//						.getBoundedPlacementConstraints()
//						.getPlacementConstraint()) {
//					if (vms.size() > 0 && nodes.size() > 0)
//						addConstraintsForPC(s.getIdref(), nodes);
//				}
//			}
//		} catch (Exception e) {
//		}
//		return v;
//	}
//
//	private List<SatConstraint> addConstraintsForPC( PlacementConstraint cp, Set<Node> nodes) {
//
//		if (cp != null) {
//			createBan(cp, nodes);
//			createCapacity(cp, nodes);
//			createFence(cp, nodes);
//			createGather(cp);
//			createLonely(cp);
//			createRoot(cp);
//			createSplit(cp);
//			createSpread(cp);
//		}
//		return v;
//
//	}
//
//}
