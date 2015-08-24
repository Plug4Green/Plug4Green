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
package f4g.optimizer.btrplace.plan.constraint.factories;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.btrplace.model.Mapping;
import org.btrplace.model.constraint.Root;
import org.btrplace.model.constraint.SatConstraint;

import f4g.commons.optimizer.ICostEstimator;
import f4g.optimizer.cloud.SLAReader;
import f4g.optimizer.btrplace.NamingService;
import f4g.optimizer.btrplace.configuration.F4GConfigurationAdapter;
import f4g.optimizer.btrplace.plan.constraint.api.NoStateChange;
import f4g.optimizer.btrplace.plan.constraint.api.SpareNodes;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.Load;
import f4g.schemas.java.constraints.optimizerconstraints.Period;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;


/**
 * Constraints linked to the policies
 */
public class PolicyConstraintFactory {

	private List<SatConstraint> v;
	private Mapping map;
	private FIT4Green F4Gmodel;
	public Logger log;
	private FederationType federation;
    private NamingService<Node> nodeNames;
    private NamingService<VM> vmNames;
    
	/**
	 * Cluster definition
	 */
	private ClusterType clusters;
	private VMFlavorType SLAvms;
	private IPowerCalculator powerCalculator;
	private ICostEstimator costEstimator;

	/**
	 * Constructor needing an instance of the SLAReader and BtrPlace
	 * configuration element.
	 */
	public PolicyConstraintFactory(ClusterType myClusters, Model model,
			FIT4Green F4Gmodel, FederationType federation, 
			VMFlavorType myVMs, 
			IPowerCalculator myPowerCalculator, 
			ICostEstimator myCostEstimator) {

		v = new LinkedList<SatConstraint>();
		this.F4Gmodel = F4Gmodel;
		this.clusters = myClusters;
		this.log = Logger.getLogger(this.getClass().getName());
		this.federation = federation;
		this.SLAvms = myVMs;
		this.powerCalculator = myPowerCalculator;	
		this.costEstimator = myCostEstimator;
		this.nodeNames = (NamingService<Node>) model.getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.NODE_NAMING_SERVICE);
		this.vmNames = (NamingService<VM>) model.getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.VM_NAMING_SERVICE);
		this.map = model.getMapping();
	}

	public List<SatConstraint> createPolicyConstraints() {

		if (federation != null) {
			int delayTimeBetweenMove = 0;
			int delayTimeBetweenOnOff = 0;
			Integer myPaybackTime = null;
			List<Period> periodVMThreshold = null;
			for (Policy pol : federation.getBoundedPolicies().getPolicy()) {
				PolicyType.Policy myPol = pol.getIdref();
				if (myPol.getDelayBetweenMove() != null
						&& myPol.getDelayBetweenMove() > delayTimeBetweenMove) {
					delayTimeBetweenMove = myPol.getDelayBetweenMove();
				}
				if (myPol.getDelayBetweenOnOff() != null
						&& myPol.getDelayBetweenOnOff() > delayTimeBetweenOnOff) {
					delayTimeBetweenOnOff = myPol.getDelayBetweenOnOff();
				}
				if (myPol.getVMMigrationPaybacktime() != null) {
					myPaybackTime = pol.getIdref()
							.getVMMigrationPaybacktime();

				}
				if (myPol.getPeriodVMThreshold() != null) {
					periodVMThreshold = pol.getIdref().getPeriodVMThreshold();
					Set<Node> nodes = Utils.getNodesFromFederation(federation, nodeNames);
					if (nodes.size() != 0) {
						addPeriodVMThreshold(nodes, periodVMThreshold, 1);
					} else {
						//if the federation contains no servers, we include them all (conservative measure for testing)
						addPeriodVMThreshold(map.getAllNodes(),	periodVMThreshold, 1);
					}
				}
			}
//			if (federation.getBoundedCluster() != null) {
//				for (BoundedClustersType.Cluster bc : federation
//						.getBoundedCluster().getCluster()) {
//					Cluster c = bc.getIdref();
//					addDelayBetweenMoveConstraint(c,  delayTimeBetweenMove, true);
//					addDelayBetweenOnOffConstraint(c, delayTimeBetweenOnOff, true);
//					if (myPaybackTime != null) {
//						addVMPaybackTimeConstraint(c, myPaybackTime);
//					}
//
//				}
//			}
		}

		if(clusters != null) {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				addDelayBetweenMoveConstraint(c, 0, false);
				addDelayBetweenOnOffConstraint(c, 0, false);
//				if(c.getBoundedPolicies() != null && c.getBoundedPolicies().getPolicy().size() != 0) {
//					PolicyType.Policy firstPol = c.getBoundedPolicies().getPolicy().get(0).getIdref();
//					if(firstPol.getVMMigrationPaybacktime() != null) {
//						addVMPaybackTimeConstraint(c, firstPol.getVMMigrationPaybacktime());
//					}							
//				}

				if (c.getBoundedPolicies() != null) {
					for (Policy pol : c.getBoundedPolicies().getPolicy()) {
						PolicyType.Policy myPol = pol.getIdref();
						if (myPol.getPeriodVMThreshold() != null) {

							float overbooking = 1;
							if(c.getBoundedSLAs() != null && (c.getBoundedSLAs().getSLA().size() > 0) && 
									c.getBoundedSLAs().getSLA().get(0).getIdref().getQoSConstraints() != null &&
									c.getBoundedSLAs().getSLA().get(0).getIdref().getQoSConstraints().getMaxVirtualCPUPerCore() != null) {
								overbooking = c.getBoundedSLAs().getSLA().get(0).getIdref().getQoSConstraints().getMaxVirtualCPUPerCore().getValue();
							}
							List<Period> periodVMThreshold = pol.getIdref().getPeriodVMThreshold();
							Set<Node> nodes = Utils.getNodesFromCluster(c, nodeNames);
							addPeriodVMThreshold(nodes, periodVMThreshold, overbooking);
						}
					}
				}	
			}
		}



		return v;
	}

	private void addDelayBetweenMoveConstraint(Cluster c, int delayTimeBetweenMove, boolean isFederation) {

		log.debug("Adding DelayBetweenMoveConstraint constraint...");
		log.debug("delayTimeBetweenMove from method parameter: " + delayTimeBetweenMove);
		Set<Node> nodes = Utils.getNodesFromCluster(c, nodeNames);
		List<Server> allServers = Utils.getAllServers(F4Gmodel);

		// get all VMs for these nodes
		Set<VM> vms = new HashSet<VM>();

		if (!isFederation && c.getBoundedPolicies() != null) {
			for (Policy pol : c.getBoundedPolicies().getPolicy()) {
				if (pol.getIdref().getDelayBetweenMove() != null && pol.getIdref().getDelayBetweenMove() > delayTimeBetweenMove) {
					delayTimeBetweenMove = pol.getIdref().getDelayBetweenMove();
					log.debug("delayTimeBetweenMove from policy: " + delayTimeBetweenMove);
				}
			}
		}
		if(delayTimeBetweenMove <= 0)
			return;

		if (F4Gmodel.getDatetime() != null) {
			XMLGregorianCalendar earliestLastTimeMove = F4Gmodel.getDatetime();
			log.debug("earliestLastTimeMove: " + earliestLastTimeMove);
			DatatypeFactory factory;
			try {
				factory = DatatypeFactory.newInstance();
				Duration duration = factory.newDuration(false, 0, 0, 0, 0,
						delayTimeBetweenMove, 0); // negative Duration

				earliestLastTimeMove.add(duration);
				log.debug("earliestLastTimeMove plus delayBetweenMove: " + earliestLastTimeMove);
				for (Node node : nodes) {

					for (Server st : allServers) {
						if (st.getFrameworkID().equals(nodeNames.getName(node))) {
							List<VirtualMachine> vmModel = f4g.optimizer.utils.Utils.getVMs(st);
							for (VirtualMachine vmt : vmModel) {
								// lastMigration is greater than
								// "Now"-delayTime -> within the interval
								// where the VM should be ignored for
								// optimization
								log.debug("checking vm " + vmt.getFrameworkID());
								log.debug("*** vmt lastMigrationTimestamp: " + vmt.getLastMigrationTimestamp());
								if (vmt.getLastMigrationTimestamp() != null
										&& vmt
										.getLastMigrationTimestamp()
										.compare(
												earliestLastTimeMove) == DatatypeConstants.GREATER) {
									log.debug("*** comparison is TRUE");
									vms.add(vmNames.getElement(vmt.getFrameworkID()));
								}
							}
						}
					}					
				}

				if (vms.size() != 0) {
					log.debug("Adding F4GDelayBetweenMove constraint");
					v.addAll(Root.newRoots(vms));
				}
			} catch (DatatypeConfigurationException e1) {
				log.error("Exception", e1);
			}

		} else {
			log.debug("No model datetime is set");
		}
	}

	
	private void addDelayBetweenOnOffConstraint(Cluster c, int delayTimeBetweenOnOff, boolean isFederation) {

		log.debug("Adding addDelayBetweenOnOffConstraint constraint...");
		log.debug("delayTimeBetweenOnOff from method parameter: " + delayTimeBetweenOnOff);
		Set<Node> nodes = Utils.getNodesFromCluster(c, nodeNames);
		List<Server> allServers = Utils.getAllServers(F4Gmodel);

		// node to apply the constraint to
		Set<Node> ns = new HashSet<Node>();

		if (!isFederation && c.getBoundedPolicies() != null) {
			for (Policy pol : c.getBoundedPolicies().getPolicy()) {
				if (pol.getIdref().getDelayBetweenOnOff() != null && pol.getIdref().getDelayBetweenOnOff() > delayTimeBetweenOnOff) {
					delayTimeBetweenOnOff = pol.getIdref().getDelayBetweenOnOff();
					log.debug("delayTimeBetweenOnOff from policy: " + delayTimeBetweenOnOff);
				}
			}
		}
		if(delayTimeBetweenOnOff <= 0)
			return;

		if (F4Gmodel.getDatetime() != null) {
			XMLGregorianCalendar earliestLastTimeOnOff = F4Gmodel.getDatetime();
			log.debug("earliestLastTimeOnOff: " + earliestLastTimeOnOff);
			DatatypeFactory factory;
			try {
				factory = DatatypeFactory.newInstance();
				Duration duration = factory.newDuration(false, 0, 0, 0, 0,
						delayTimeBetweenOnOff, 0); // negative Duration

				earliestLastTimeOnOff.add(duration);
				log.debug("earliestLastTimeOnOff plus delayBetweenOnOff: " + earliestLastTimeOnOff);
				for (Node node : nodes) {
					
					for (Server st : allServers) {
						if (st.getFrameworkID().equals(nodeNames.getName(node))) {
							if (st.getLastOnOffTimestamp() != null
									&& st
									.getLastOnOffTimestamp()
									.compare(
											earliestLastTimeOnOff) == DatatypeConstants.GREATER) {
								log.debug("*** comparison is TRUE");
								ns.add(node);
							}							
						}
					}					
				}

				if (ns.size() != 0) {
					log.debug("Adding F4GDelayBetweenOnOff constraint");
					v.addAll(NoStateChange.newNoStateChanges(ns));
				}
			} catch (DatatypeConfigurationException e1) {
				log.error("Exception", e1);
			}

		} else {
			log.debug("No model datetime is set");
		}
	}

//	private void addVMPaybackTimeConstraint(Cluster c, int myPaybackTime) {
//
//		Set<Node> nodes = Utils.getNodesFromCluster(c, nodeNames);
//		Set<VM> vms = Utils.getVMsFromNodes(nodes, vmNames);
//
//		if(myPaybackTime > 0) {
//			v.add(new F4GVMPaybackTimeConstraint(vms, myPaybackTime, model, SLAvms, powerCalculator, costEstimator ));
//		}			
//	}

	private void addPeriodVMThreshold(Set<Node> nodes, List<Period> periods, float overbooking) {

		if(F4Gmodel.getDatetime() != null) {
			Load load = SLAReader.getVMSlotsThreshold(F4Gmodel.getDatetime().toGregorianCalendar().getTime(), periods);					
			if(nodes.size() !=0 && load != null) {
//				if(load.getSpareCPUs() != null && load.getSpareCPUs().getValue() > 0 ) {
//
//					int nbCores = 0;
//					switch (load.getSpareCPUs().getUnitType()) {
//					case ABSOLUTE: nbCores = load.getSpareCPUs().getValue(); break;
//					case RELATIVE: nbCores = load.getSpareCPUs().getValue() * map.getAllNodes().size() / 100; break;
//					}
//
//					v.add(new SpareCPUs(nodes, nbCores, overbooking));
//				}


				if(load.getSpareNodes() != null && load.getSpareNodes().getValue() > 0 ) {

					int nbNodes = 0;
					switch (load.getSpareNodes().getUnitType()) {
					case ABSOLUTE: nbNodes = load.getSpareNodes().getValue(); break;
					case RELATIVE: nbNodes = load.getSpareNodes().getValue() * map.getAllNodes().size() / 100; break;
					}

					v.add(new SpareNodes(nodes, nbNodes));
				}
			}




		} else {
			log.warn("the model doesn't specify the current time: cannot use the on/off policy");
		}	

	}


}
