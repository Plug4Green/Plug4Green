/**
 * ============================== Header ============================== 
 * file:          SLAConstraintFactory.java
 * project:       FIT4Green/Optimizer
 * created:       09.10.2011 by ts
 * last modified: $LastChangedDate: 2010-11-26 11:33:26 +0100 (Fr, 26 Nov 2010) $ by $LastChangedBy: corentin.dupont@create-net.org $
 * revision:      $LastChangedRevision: 150 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package org.f4g.entropy.plan.constraint;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.f4g.optimizer.OptimizerEngine;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.EnergyConstraintsType;
import org.f4g.schema.constraints.optimizerconstraints.HardwareConstraintsType;
import org.f4g.schema.constraints.optimizerconstraints.QoSConstraintsType;
import org.f4g.schema.constraints.optimizerconstraints.SecurityConstraintsType;
import org.f4g.schema.constraints.optimizerconstraints.ServerGroupType;
import org.f4g.schema.constraints.optimizerconstraints.BoundedSLAsType.SLA;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.ServerGroupType.ServerGroup;
import org.f4g.schema.metamodel.FIT4GreenType;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Lonely;
import entropy.vjob.VJob;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class SLAConstraintFactory {

	private VJob v;
	private Configuration src;
	private FIT4GreenType model;
	public Logger log;
	int minPriority;
	private ServerGroupType sg;
	

	/**
	 * Cluster definition
	 */
	private ClusterType clusters;

	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public SLAConstraintFactory(ClusterType myClusters, Configuration src,
			FIT4GreenType model, int minPriority, ServerGroupType sg) {
		v = new DefaultVJob("slaVJob");
		this.src = src;
		this.model = model;
		this.minPriority = minPriority;
		clusters = myClusters;
		log = Logger.getLogger(this.getClass().getName());
		this.sg = sg;
		log.debug("minPriority=" + minPriority);
		
	}

	public VJob createSLAConstraints() {

		try {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				// get all nodes in a cluster
				ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
				for (String nodeName : c.getNodeController().getNodeName()) {
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
						ManagedElementSet<VirtualMachine> vm = src.getRunnings(node);
						vms.addAll(vm);
					} catch (Exception e) {
					}

				}

				if (c.getBoundedSLAs() != null) {
					// get all bounded SLAs and add constraints to vjob
					for (SLA s : c.getBoundedSLAs().getSLA()) {
						if (vms.size() > 0 && nodes.size() > 0)
							addConstraintsForSLA(s.getIdref(), vms, nodes);
					}
				}
			}
			addSLAsForServerGroup();
		} catch (Exception e) {
		}
		return v;
	}

	private void addSLAsForServerGroup() {
		try {
			if (sg != null) {
				List<ServerGroup> serverGroupList = sg.getServerGroup();
				for (ServerGroup s : serverGroupList) {
					// get all nodes in a cluster
					ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
					for (String nodeName : s.getNodeController().getNodeName()) {
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
					for (SLA slaRef : s.getBoundedSLAs().getSLA()) {
						if (vms.size() > 0 && nodes.size() > 0)
							addConstraintsForSLA(slaRef.getIdref(), vms, nodes);
					}
				}
			}
		} catch (Exception e) {
		}
	}

	private void addConstraintsForSLA(
			org.f4g.schema.constraints.optimizerconstraints.SLAType.SLA sla,
			ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> nodes) {

		if (sla.getQoSConstraints() != null) {
			addQoS(sla.getQoSConstraints(), nodes, vms);
		}
		if (sla.getSecurityConstraints() != null) {
			addSecurity(sla.getSecurityConstraints(), vms, nodes);
		}
		if (sla.getHardwareConstraints() != null) {
			addHardware(sla.getHardwareConstraints(), vms, nodes);
		}
		if (sla.getEnergyConstraints() != null) {
			addEnergy(sla.getEnergyConstraints(), vms, nodes);
		}

	}

	private void addQoS(QoSConstraintsType type, ManagedElementSet<Node> nodes,
			ManagedElementSet<VirtualMachine> vms) {

		try {
			// Bandwidth Constraint
			if (type.getBandwidth() != null) {
				if (type.getBandwidth().getPriority() >= minPriority) {
					v.addConstraint(new F4GHardwareConstraint(5, type
							.getBandwidth().getValue(), nodes, vms, model));
				}
			}
		} catch (Exception e) {

		}

		try {
			// Maximum vCPU per core
			if (type.getMaxVirtualCPUPerCore() != null) {
				if (type.getMaxVirtualCPUPerCore().getPriority() >= minPriority) {
					if (vms.size() != 0) {
						v.addConstraint(new F4GCPUOverbookingConstraint(nodes,
								(double) type.getMaxVirtualCPUPerCore().getValue()));
					}
				}
			}
		} catch (Exception e) {

		}

		try {
			// Max Virtual CPU Load per core
			if (type.getMaxVirtualLoadPerCore() != null && type.getMaxVirtualLoadPerCore().getValue() != 0) {
				if (type.getMaxVirtualLoadPerCore().getPriority() >= minPriority) {
					v.addConstraint(new F4GVirtualLoadPerCoreConstraint(nodes,
							model, (double) type.getMaxVirtualLoadPerCore()
									.getValue(), vms));
				}
			}
		} catch (Exception e) {

		}

		try {
			// Max Server CPU Load			
			if (type.getMaxServerCPULoad() != null && type.getMaxServerCPULoad().getValue() != 0) {
				if (type.getMaxServerCPULoad().getPriority() >= minPriority) {
					v.addConstraint(new F4GCPUConstraint(nodes, (double) type
							.getMaxServerCPULoad().getValue()));
				}
			}
		} catch (Exception e) {

		}

		try {
			// Memory Overbooking on Server Level
			if (type.getMaxVRAMperPhyRAM() != null && type.getMaxVRAMperPhyRAM().getValue() != 0) {
				if (type.getMaxVRAMperPhyRAM().getPriority() >= minPriority) {
					v.addConstraint(new F4GMemoryOverbookingConstraint(nodes,
							model, (double) type.getMaxVRAMperPhyRAM()
									.getValue(), clusters));
				}
			}
		} catch (Exception e) {

		}
		
		
		try {
			// CPU Overbooking on Cluster Level
			if (type.getMaxServerAvgVCPUperCore() != null && type.getMaxServerAvgVCPUperCore().getValue() != 0) {
				if (type.getMaxServerAvgVCPUperCore().getPriority() >= minPriority) {
						v.addConstraint(new F4GAvgCPUOverbooking(nodes,
						(double) type.getMaxServerAvgVCPUperCore()
									.getValue(), model));						
					
				}
			}
		} catch (Exception e) {
		}
		
		try {
			// Memory Overbooking on Cluster Level
			if (type.getMaxServerAvgVRAMperPhyRAM() != null && type.getMaxServerAvgVRAMperPhyRAM().getValue() != 0) {
				if (type.getMaxServerAvgVRAMperPhyRAM().getPriority() >= minPriority) {
					v.addConstraint(new F4GAvgMemoryOverbooking(nodes,
						(double) type.getMaxServerAvgVRAMperPhyRAM()
									.getValue(), model));
				}
			}
		} catch (Exception e) {
		}
		

		try {
			// Max VMs per Server
			if (type.getMaxVMperServer() != null && type.getMaxVMperServer().getValue() != 0) {
				if (type.getMaxVMperServer().getPriority() >= minPriority) {
					v.addConstraint(new F4GCapacity(nodes, type
							.getMaxVMperServer().getValue()));
				}
			}
		} catch (Exception e) {

		}

	}

	private void addSecurity(SecurityConstraintsType type,
			ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> nodes) {

		try {
			// Dedicated server
			if (type.getDedicatedServer() != null && type.getDedicatedServer().isValue()) {
				if (type.getDedicatedServer().getPriority() >= minPriority) {
					Lonely lonely = new Lonely(vms);
					v.addConstraint(lonely);
				}
			}
		} catch (Exception e) {

		}

		try {
			// secure access (boolean)
			if (type.getSecureAccessPossibility() != null && type.getSecureAccessPossibility().isValue()) {
				if (type.getSecureAccessPossibility().getPriority() >= minPriority) {
					List<ManagedElementSet<Node>> group = new LinkedList<ManagedElementSet<Node>>();
					group.add(nodes);
					Set<ManagedElementSet<Node>> s = new HashSet<ManagedElementSet<Node>>(group);
					
					//TODO reactivate
					//OneOf oneOf = new OneOf(s, vms);
					//v.addConstraint((PlacementConstraint) oneOf);
				}
			}
		} catch (Exception e) {

		}
	}

	private void addHardware(HardwareConstraintsType type,
			ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> nodes) {
		try {
			// CPU Frequency of the node
			if (type.getCompPowerGHz() != null && type.getCompPowerGHz().getValue() != 0) {
				if (type.getCompPowerGHz().getPriority() >= minPriority) {
					v.addConstraint(new F4GHardwareConstraint(1, type
							.getCompPowerGHz().getValue(), nodes, vms, model));
				}
			}
		} catch (Exception e) {

		}

		try {
			// GPU Frequency of the node
			F4GHardwareConstraint GPU;
			if (type.getGPUFreqGHz() != null && type.getGPUFreqGHz().getValue() != 0) {
				if (type.getGPUFreqGHz().getPriority() >= minPriority) {
					GPU = new F4GHardwareConstraint(2, type.getGPUFreqGHz()
							.getValue(), nodes, vms, model);
					v.addConstraint(GPU);
				}
			}
		} catch (Exception e) {

		}

		try {
			// Harddisk capacity of the node
			if (type.getHDDCapacity() != null && type.getHDDCapacity().getValue() != 0) {
				if (type.getHDDCapacity().getPriority() >= minPriority) {
					v.addConstraint(new F4GHDDConstraint(nodes, model,
							(int) type.getHDDCapacity().getValue()));
				}
			}
		} catch (Exception e) {

		}

		try {
			// RAM space for vms guaranteed
			if (type.getMemorySpaceGB() != null && type.getMemorySpaceGB().getValue() != 0) {
				if (type.getMemorySpaceGB().getPriority() >= minPriority) {
					v.addConstraint(new F4GMemoryConstraint(nodes, model,
							(int) type.getMemorySpaceGB().getValue()));
				}
			}
		} catch (Exception e) {

		}

		try {
			// number of cores of a node
			if (type.getNbOfCores() != null && type.getNbOfCores().getValue() != 0) {
				if (type.getNbOfCores().getPriority() >= minPriority) {
					v.addConstraint(new F4GHardwareConstraint(4, type
							.getNbOfCores().getValue(), nodes, vms, model));
				}
			}
		} catch (Exception e) {

		}

		try {
			// Raid level
			if (type.getGPUFreqGHz() != null && type.getGPUFreqGHz().getValue() != 0) {
				if (type.getGPUFreqGHz().getPriority() >= minPriority) {
					v.addConstraint(new F4GHardwareConstraint(3, type
							.getRAIDLevel(), nodes, vms, model));
				}
			}
		} catch (Exception e) {

		}
	}

	private void addEnergy(EnergyConstraintsType energyConstraints,
			ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> nodes) {

				if (energyConstraints.getMaxPowerServer().getPriority() >= minPriority) {
					v.addConstraint(new MaxServerPower(nodes, energyConstraints.getMaxPowerServer().getValue()));
				}
			}
		
}
