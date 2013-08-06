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
package f4g.optimizer.entropy.plan.constraint;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import f4g.optimizer.cloudTraditional.SLAReader;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.RequestType;
import f4g.schemas.java.allocation.TraditionalVmAllocationType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.metamodel.FIT4GreenType;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Fence;
import entropy.vjob.VJob;

/**
 * A Class containing everything relevant to cluster constraints
 * 
 * 
 * @author TS
 */
public class ClusterConstraintFactory {
	public Logger log;
	
	private Configuration src;

	/**
	 * Cluster definition
	 */
	private ClusterType clusters;

	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public ClusterConstraintFactory(ClusterType myClusters, Configuration src) {
		clusters = myClusters;
		this.src = src;
		log = Logger.getLogger(ClusterConstraintFactory.class.getName());
	}

	public ClusterConstraintFactory() {
	}

	/**
	 * 
	 * Creates the boundaries of the cluster, that means, that no VM can be
	 * migrated outside a cluster
	 * 
	 * @return A VJob which must be added to the queue before optimization
	 * 
	 * @author TS
	 */
	public VJob createClusterConstraints() {

		DefaultVJob v = new DefaultVJob("slaVJob");
		try {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				// get all nodes in a cluster
				ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
				for (String nodeName : c.getNodeController().getNodeName()) {
					Node n = src.getAllNodes().get(nodeName);
					if(n!=null) {
						nodes.add(n);
					}
					
				}

				// get all VMs for these nodes
				ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
				for (Node node : nodes) {
					vms.addAll(src.getRunnings(node));
				}
				if (vms.size() > 0 && nodes.size() > 0) {
					v.addConstraint(new Fence(vms, nodes));
				}	
			}
		} catch (Exception e) {
		}

		return v;
	}

	/**
	 * 
	 * Returns the nodes of a specific cluster
	 * 
	 * @param clusterName
	 *            The Name of the cluster under consideration
	 * @return
	 * 
	 * @author TS
	 */
	public ManagedElementSet<Node> getAllNodesforACluster(String clusterName) {

		ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
		try {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				if (c.getName().equals(clusterName)) {
					// get all nodes in a cluster
					for (String nodeName : c.getNodeController().getNodeName()) {
						Node n = src.getAllNodes().get(nodeName);
						if(n!=null) {
							nodes.add(n);
						}
					}
				}
			}
		} catch (Exception e) {
		}
		return nodes;
	}

	/**
	 * 
	 * Specifies the constraint, that the vm can only be allocated to a certain
	 * cluster
	 * 
	 * @param clusterName
	 *            The Name of the cluster under consideration
	 * 
	 * @param VirtualMachine
	 *            The VM (of type: entropy.configuration.VirtualMachine) to be
	 *            allocated
	 * 
	 * @return A VJob which must be added to the queue before allocation
	 * 
	 * @author TS
	 */
	public VJob restrictPlacementToClusters(RequestType request, VirtualMachine vm) {

		List<String> clusterList = new ArrayList<String>();
		if(request instanceof CloudVmAllocationType) {
			clusterList = ((CloudVmAllocationType)request).getClusterId();	
		} else {
			clusterList = ((TraditionalVmAllocationType)request).getClusterId();	
		}
		
		DefaultVJob v = new DefaultVJob("slaVJob");
		try {

			ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
			vms.add(vm);
			ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
			for(String clusterName : clusterList) {
				for (Node node : getAllNodesforACluster(clusterName)) {
					nodes.add(src.getAllNodes().get(node.getName()));
				}
			}		
			if (vms.size() > 0 && nodes.size() > 0) {
				v.addConstraint(new Fence(vms, nodes));
			}
			if (nodes.size() == 0) {
				log.warn("Allocation on a cluster with no servers or all servers overloaded!");
			}
		} catch (Exception e) {
		}

		return v;
	}

}
