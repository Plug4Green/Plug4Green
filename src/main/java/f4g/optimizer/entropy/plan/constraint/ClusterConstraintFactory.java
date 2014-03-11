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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import btrplace.model.Mapping;
import btrplace.model.VM;
import btrplace.model.constraint.SatConstraint;
import f4g.optimizer.cloudTraditional.SLAReader;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.RequestType;
import f4g.schemas.java.allocation.TraditionalVmAllocationType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.metamodel.FIT4GreenType;

import btrplace.model.Node;
/**
 * A Class containing everything relevant to cluster constraints
 * 
 * 
 * @author TS
 */
public class ClusterConstraintFactory {
	public Logger log;
	
	private Mapping src;

	/**
	 * Cluster definition
	 */
	private ClusterType clusters;

	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public ClusterConstraintFactory(ClusterType myClusters, Mapping src) {
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
	public List<SatConstraint> createClusterConstraints() {

		List<SatConstraint> v = new LinkedList<SatConstraint>();
		try {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				// get all nodes in a cluster
				Set<Node> nodes = new HashSet<Node>();
				for (String nodeName : c.getNodeController().getNodeName()) {
					Node n = src.getAllNodes().get(nodeName);
					if(n!=null) {
						nodes.add(n);
					}
					
				}

				// get all VMs for these nodes
				Set<VM> vms = new HashSet<VM>();
				for (Node node : nodes) {
					vms.addAll(src.getRunningVMs(node));
				}
				if (vms.size() > 0 && nodes.size() > 0) {
					v.add(new Fence(vms, nodes));
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
	public Set<Node> getAllNodesforACluster(String clusterName) {

		Set<Node> nodes = new HashSet<Node>();
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
	public List<SatConstraint> restrictPlacementToClusters(RequestType request, VM vm) {

		List<SatConstraint> v = new LinkedList<SatConstraint>();
		
		List<String> clusterList = new ArrayList<String>();
		if(request instanceof CloudVmAllocationType) {
			clusterList = ((CloudVmAllocationType)request).getClusterId();	
		} else {
			clusterList = ((TraditionalVmAllocationType)request).getClusterId();	
		}
		
		try {

			Set<VM> vms = new HashSet<VM>();
			vms.add(vm);
			Set<btrplace.model.Node> nodes = new HashSet<Node>();
			for(String clusterName : clusterList) {
				for (Node node : getAllNodesforACluster(clusterName)) {
					nodes.add(src.getAllNodes().get(node.getName()));
				}
			}		
			if (vms.size() > 0 && nodes.size() > 0) {
				v.add(new Fence(vms, nodes));
			}
			if (nodes.size() == 0) {
				log.warn("Allocation on a cluster with no servers or all servers overloaded!");
			}
		} catch (Exception e) {
		}

		return v;
	}

}
