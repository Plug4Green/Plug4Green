package f4g.optimizer.btrplace.plan.constraint.factories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.Lonely;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.ResourceCapacity;
import org.btrplace.model.constraint.Overbook;

import f4g.optimizer.btrplace.configuration.F4GConfigurationAdapter;
import f4g.optimizer.btrplace.plan.constraint.api.MaxServerPower;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType.SLA;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType.ServerGroup;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.Server;



/**
 * Constraints linked to the SLAs
 */
public class SLAConstraintFactory  extends ConstraintFactory {

	private FIT4Green F4GModel;
	private ServerGroupType sg;

	int minPriority;
	/**
	 * Cluster definition
	 */
	private ClusterType clusters;

	/**
	 * Constructor needing an instance of the SLAReader and BtrPlace
	 * configuration element.
	 */
	public SLAConstraintFactory(ClusterType clusters, Model model, FIT4Green F4GModel, int minPriority, ServerGroupType sg) {
		super(model);
		this.minPriority = minPriority;
		this.clusters = clusters;
		this.sg = sg;
		this.F4GModel = F4GModel;
		
		log = Logger.getLogger(this.getClass().getName());
		log.debug("minPriority=" + minPriority);
	}

	public List<SatConstraint> createSLAConstraints() {

		List<SatConstraint> v = new ArrayList<SatConstraint>(); 

		List<Cluster> clusterList = clusters.getCluster();
		for (Cluster c : clusterList) {
		// get all nodes in a cluster
			Set<Node> nodes = new HashSet<Node>();
			for (String nodeName : c.getNodeController().getNodeName()) {
					Node n = nodeNames.getElement(nodeName);
					if (n != null) {
						nodes.add(n);
					}
			}

			// get all VMs for these nodes
			Set<VM> vms = new HashSet<VM>();
			for (Node node : nodes) {
				Set<VM> vm = map.getRunningVMs(node);
				vms.addAll(vm);
			}
			if (c.getBoundedSLAs() != null) {
				// get all bounded SLAs and add constraints to vjob
				for (SLA s : c.getBoundedSLAs().getSLA()) {
					if (vms.size() > 0 && nodes.size() > 0)
						v.addAll(getConstraintsForSLA(s.getIdref(), vms, nodes));
				}
			}
		}
		v.addAll(getSLAsForServerGroup());
		
	return v;
	}

	private List<SatConstraint> getSLAsForServerGroup() {
		List<SatConstraint> v = new ArrayList<SatConstraint>(); 

		if (sg != null) {
			List<ServerGroup> serverGroupList = sg.getServerGroup();
			for (ServerGroup s : serverGroupList) {
				// get all nodes in a cluster
				Set<Node> nodes = new HashSet<Node>();
				for (String nodeName : s.getNodeController().getNodeName()) {
					Node n = nodeNames.getElement(nodeName);
					if (n != null) {
						nodes.add(n);
					}
				}
				// get all VMs for these nodes
				Set<VM> vms = new HashSet<VM>();
				for (Node node : nodes) {
					Set<VM> vm = map.getRunningVMs(node);
					vms.addAll(vm);
				}
				// get all bounded SLAs and add constraints to vjob
				for (SLA slaRef : s.getBoundedSLAs().getSLA()) {
					if (vms.size() > 0 && nodes.size() > 0)
						v.addAll(getConstraintsForSLA(slaRef.getIdref(), vms, nodes));
				}
			}
		}
		return v;
	}

	private List<SatConstraint> getConstraintsForSLA(SLAType.SLA sla, Set<VM> vms, Set<Node> nodes) {

		List<SatConstraint> v = new ArrayList<SatConstraint>(); 
		
		if (sla.getQoSConstraints() != null) {
			v.addAll(getQoS(sla.getQoSConstraints(), nodes, vms));
		}
		if (sla.getSecurityConstraints() != null) {
			v.addAll(getSecurity(sla.getSecurityConstraints(), vms, nodes));
		}
		if (sla.getHardwareConstraints() != null) {
			v.addAll(getHardware(sla.getHardwareConstraints(), vms, nodes));
		}
		if (sla.getEnergyConstraints() != null) {
			v.addAll(getEnergy(sla.getEnergyConstraints(), vms, nodes));
		}
		return v;
	}

	private List<SatConstraint> getQoS(QoSConstraintsType qos, Set<Node> nodes, Set<VM> vms) {

		List<SatConstraint> v = new ArrayList<SatConstraint>(); 
		// Bandwidth Constraint
		if (qos.getBandwidth() != null && qos.getBandwidth().getPriority() >= minPriority) {
			List<Node> filteredNodes = new ArrayList<Node>();
			for (Server n : Utils.getAllServers(F4GModel)){
				if (Utils.getBandwidth(n).isPresent() && Utils.getBandwidth(n).get().getValue() >= qos.getBandwidth().getValue()){ 
					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
				}
			}
			v.addAll(Fence.newFence(vms, filteredNodes));
		}
		
		// Maximum vCPU per core
		if (qos.getMaxVirtualCPUPerCore() != null) {
			if (qos.getMaxVirtualCPUPerCore().getPriority() >= minPriority) {
				if (vms.size() != 0) {
					for(Node n : nodes) {
						v.add(new Overbook(n, F4GConfigurationAdapter.SHAREABLE_RESOURCE_CPU, (double) qos.getMaxVirtualCPUPerCore().getValue(), false));
					}
				}
			}
		}
		
		// Max Virtual CPU Load per core
//		if (type.getMaxVirtualLoadPerCore() != null && type.getMaxVirtualLoadPerCore().getValue() != 0) {
//			if (type.getMaxVirtualLoadPerCore().getPriority() >= minPriority) {
//				v.add(new F4GVirtualLoadPerCoreConstraint(nodes, model, (double) type.getMaxVirtualLoadPerCore().getValue(), vms));
//			}
//		}
		
		// Max Server CPU Load			
		if (qos.getMaxServerCPULoad() != null && qos.getMaxServerCPULoad().getValue() != 0) {
			if (qos.getMaxServerCPULoad().getPriority() >= minPriority) {
				for(Node n : nodes) {
					//Todo multiply by number of cores
					v.add(new ResourceCapacity(n, F4GConfigurationAdapter.SHAREABLE_RESOURCE_CPU, (int)qos.getMaxServerCPULoad().getValue()));
				}
				
			}
		}
		
//		// Memory Overbooking on Server Level
//		if (type.getMaxVRAMperPhyRAM() != null && type.getMaxVRAMperPhyRAM().getValue() != 0) {
//			if (type.getMaxVRAMperPhyRAM().getPriority() >= minPriority) {
//				v.add(new F4GMemoryOverbookingConstraint(nodes,	model, (double) type.getMaxVRAMperPhyRAM().getValue(), clusters));
//			}
//		}
//		
//		// CPU Overbooking on Cluster Level
//		if (type.getMaxServerAvgVCPUperCore() != null && type.getMaxServerAvgVCPUperCore().getValue() != 0) {
//			if (type.getMaxServerAvgVCPUperCore().getPriority() >= minPriority) {
//					v.add(new F4GAvgCPUOverbooking(nodes, (double) type.getMaxServerAvgVCPUperCore().getValue(), model));						
//			}
//		}
//		
//		// Memory Overbooking on Cluster Level
//		if (type.getMaxServerAvgVRAMperPhyRAM() != null && type.getMaxServerAvgVRAMperPhyRAM().getValue() != 0) {
//			if (type.getMaxServerAvgVRAMperPhyRAM().getPriority() >= minPriority) {
//				v.add(new F4GAvgMemoryOverbooking(nodes, (double) type.getMaxServerAvgVRAMperPhyRAM().getValue(), model));
//			}
//		}
//		
//		// Max VMs per Server
//		if (type.getMaxVMperServer() != null && type.getMaxVMperServer().getValue() != 0) {
//			if (type.getMaxVMperServer().getPriority() >= minPriority) {
//				v.add(new F4GCapacity(nodes, type.getMaxVMperServer().getValue()));
//			}
//		}
		
		return v;
	}

	private List<SatConstraint> getSecurity(SecurityConstraintsType type, Set<VM> vms, Set<Node> nodes) {
		List<SatConstraint> v = new ArrayList<SatConstraint>(); 
		// Dedicated server
		if (type.getDedicatedServer() != null && type.getDedicatedServer().isValue()) {
			if (type.getDedicatedServer().getPriority() >= minPriority) {
				Lonely lonely = new Lonely(vms);
				v.add(lonely);
			}
		}

		// secure access (boolean)
		if (type.getSecureAccessPossibility() != null && type.getSecureAccessPossibility().isValue()) {
			if (type.getSecureAccessPossibility().getPriority() >= minPriority) {
				List<Set<Node>> group = new LinkedList<Set<Node>>();
				group.add(nodes);
				Set<Set<Node>> s = new HashSet<Set<Node>>(group);
				
				//TODO reactivate
				//OneOf oneOf = new OneOf(s, vms);
				//v.addConstraint((PlacementConstraint) oneOf);
			}
		}
		return v;
	}

	private List<SatConstraint> getHardware(HardwareConstraintsType type, Set<VM> vms, Set<Node> nodes) {

		List<SatConstraint> v = new ArrayList<SatConstraint>();
		// CPU Frequency of the node
		if (type.getCompPowerGHz() != null && type.getCompPowerGHz().getPriority() >= minPriority) {
			List<Node> filteredNodes = new ArrayList<Node>();
			for (Server n : Utils.getAllServers(F4GModel)){
				if (Utils.getCPUFrequency(n).getValue() >= type.getCompPowerGHz().getValue()){ 
					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
				}
			}
			v.addAll(Fence.newFence(vms, filteredNodes));
		}
		
		// GPU Frequency of the node
		if (type.getGPUFreqGHz() != null && type.getGPUFreqGHz().getPriority() >= minPriority) {
			List<Node> filteredNodes = new ArrayList<Node>();
			for (Server n : Utils.getAllServers(F4GModel)){
				if (Utils.getGPUFrequency(n).isPresent() && Utils.getGPUFrequency(n).get().getValue() >= type.getGPUFreqGHz().getValue()){ 
					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
				}
			}
			v.addAll(Fence.newFence(vms, filteredNodes));
		}
		
		// Harddisk capacity of the node
		if (type.getHDDCapacity() != null && type.getHDDCapacity().getPriority() >= minPriority) {
			List<Node> filteredNodes = new ArrayList<Node>();
			for (Server n : Utils.getAllServers(F4GModel)){
				if (Utils.getHDDCapacty(n).getValue() >= type.getHDDCapacity().getValue()){ 
					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
				}
			}
			v.addAll(Fence.newFence(vms, filteredNodes));
		}
		
//		// RAM space for vms guaranteed
//		if (type.getMemorySpaceGB() != null && type.getMemorySpaceGB().getPriority() >= minPriority) {
//			List<Node> filteredNodes = new ArrayList<Node>();
//			for (Server n : Utils.getAllServers(F4GModel)){
//				if (Utils.getHDDCapacty(n).getValue() >= type.getHDDCapacity().getValue()){ 
//					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
//				}
//			}
//			v.addAll(Fence.newFences(vms, filteredNodes));
//			if () {
//				v.add(new F4GMemoryConstraint(nodes, model,	(int) type.getMemorySpaceGB().getValue()));
//			}
//		}
		
//		// number of cores of a node
//		if (type.getNbOfCores() != null && type.getNbOfCores().getValue() != 0) {
//			if (type.getNbOfCores().getPriority() >= minPriority) {
//				v.add(new F4GHardwareConstraint(4, type.getNbOfCores().getValue(), nodes, vms, model));
//			}
//		}
		
		if (type.getRAIDLevel() != null && type.getRAIDLevel().getPriority() >= minPriority) {
			List<Node> filteredNodes = new ArrayList<Node>();
			for (Server n : Utils.getAllServers(F4GModel)){
				if (Utils.getRAIDLevel(n).isPresent() && Utils.getRAIDLevel(n).get().getValue() >= type.getRAIDLevel().getValue()){ 
					filteredNodes.add(nodeNames.getElement(n.getFrameworkID()));
				}
			}
			v.addAll(Fence.newFence(vms, filteredNodes));
		}
		return v;
	}

	private List<SatConstraint> getEnergy(EnergyConstraintsType energyConstraints,	Set<VM> vms, Set<Node> nodes) {
		List<SatConstraint> v = new ArrayList<SatConstraint>();
		if (energyConstraints.getMaxPowerServer() != null && energyConstraints.getMaxPowerServer().getPriority() >= minPriority) {
			v.add(new MaxServerPower(nodes, energyConstraints.getMaxPowerServer().getValue()));
		}
		return v;
	}
		
}
