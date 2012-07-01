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

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.f4g.optimizer.ICostEstimator;
import org.f4g.optimizer.CloudTraditional.SLAReader;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.BoundedPoliciesType.Policy;
import org.f4g.schema.constraints.optimizerconstraints.BoundedClustersType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType;
import org.f4g.schema.constraints.optimizerconstraints.FederationType;
import org.f4g.schema.constraints.optimizerconstraints.LoadType;
import org.f4g.schema.constraints.optimizerconstraints.PeriodType;
import org.f4g.schema.constraints.optimizerconstraints.PolicyType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.ClusterType.Cluster;
import org.f4g.schema.constraints.optimizerconstraints.QoSDescriptionType.MaxVirtualCPUPerCore;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.VJob;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class PolicyConstraintFactory {

	private VJob v;
	private Configuration src;
	private FIT4GreenType model;
	public Logger log;
	private FederationType federation;

	/**
	 * Cluster definition
	 */
	private ClusterType clusters;
	private VMTypeType SLAvms;
	private IPowerCalculator powerCalculator;
	private ICostEstimator costEstimator;

	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public PolicyConstraintFactory(ClusterType myClusters, Configuration src,
			FIT4GreenType model, FederationType federation, 
    		VMTypeType myVMs, IPowerCalculator myPowerCalculator, ICostEstimator myCostEstimator) {
		
		v = new DefaultVJob("PolicyVJob");
		this.src = src;
		this.model = model;
		clusters = myClusters;
		log = Logger.getLogger(this.getClass().getName());
		this.federation = federation;
		SLAvms = myVMs;
		powerCalculator = myPowerCalculator;	
		costEstimator = myCostEstimator;
		
	}

	public VJob createPolicyConstraints() {

		if (federation != null) {
			int delayTimeBetweenMove = 0;
			Integer myPaybackTime = null;
			List<PeriodType> periodVMThreshold = null;
			for (Policy pol : federation.getBoundedPolicies().getPolicy()) {
				PolicyType.Policy myPol = pol.getIdref();
				if (myPol.getDelayBetweenMove() != null
						&& myPol.getDelayBetweenMove() > delayTimeBetweenMove) {
					delayTimeBetweenMove = myPol.getDelayBetweenMove();
				}
				if (myPol.getVMMigrationPaybacktime() != null) {
					myPaybackTime = pol.getIdref()
							.getVMMigrationPaybacktime();

				}
				if (myPol.getPeriodVMThreshold() != null) {
					periodVMThreshold = pol.getIdref().getPeriodVMThreshold();
					ManagedElementSet<Node> nodes = Utils.getNodesFromFederation(federation, src);
					if (nodes.size() != 0) {
						addPeriodVMThreshold(nodes, periodVMThreshold, 1);
					} else {
						//if the federation contains no servers, we include them all (conservative measure for testing)
						addPeriodVMThreshold(src.getAllNodes(),	periodVMThreshold, 1);
					}

				}

			}
			if (federation.getBoundedCluster() != null) {
				for (BoundedClustersType.Cluster bc : federation
						.getBoundedCluster().getCluster()) {
					Cluster c = bc.getIdref();
					addDelayBetweenMoveConstraint(c, delayTimeBetweenMove,
							true);
					if (myPaybackTime != null) {
						addVMPaybackTimeConstraint(c, myPaybackTime);
					}

				}
			}
		}
		
		if(clusters != null) {
			List<Cluster> clusterList = clusters.getCluster();
			for (Cluster c : clusterList) {
				addDelayBetweenMoveConstraint(c, 0, false);
				if(c.getBoundedPolicies() != null && c.getBoundedPolicies().getPolicy().size() != 0) {
					PolicyType.Policy firstPol = c.getBoundedPolicies().getPolicy().get(0).getIdref();
					if(firstPol.getVMMigrationPaybacktime() != null) {
						addVMPaybackTimeConstraint(c, firstPol.getVMMigrationPaybacktime());
					}							
				}
				
				if (c.getBoundedPolicies() != null) {
					for (Policy pol : c.getBoundedPolicies().getPolicy()) {
						PolicyType.Policy myPol = pol.getIdref();
						if (myPol.getPeriodVMThreshold() != null) {
							
							float overbooking = 1;
							if(c.getBoundedSLAs() != null && (c.getBoundedSLAs().getSLA().size() > 0) && 
									c.getBoundedSLAs().getSLA().get(0).getIdref().getCommonQoSRelatedMetrics() != null &&
									c.getBoundedSLAs().getSLA().get(0).getIdref().getCommonQoSRelatedMetrics().getMaxVirtualCPUPerCore() != null) {
								overbooking = c.getBoundedSLAs().getSLA().get(0).getIdref().getCommonQoSRelatedMetrics().getMaxVirtualCPUPerCore().getValue();
							}
							List<PeriodType> periodVMThreshold = pol.getIdref().getPeriodVMThreshold();
							ManagedElementSet<Node> nodes = Utils.getNodesFromCluster(c, src);
							addPeriodVMThreshold(nodes, periodVMThreshold, overbooking);
						}
					}
				}	
			}
		}
		


		return v;
	}

	private void addDelayBetweenMoveConstraint(Cluster c, int delayTimeBetweenMove,
		boolean isFederation) {

		log.debug("Adding DelayBetweenMoveConstraint constraint...");
		log.debug("delayTimeBetweenMove from method parameter: " + delayTimeBetweenMove);
		ManagedElementSet<Node> nodes = Utils.getNodesFromCluster(c, src);

		List<ServerType> allServers = org.f4g.optimizer.utils.Utils
				.getAllServers(model);

		Utils.getVMs(allServers.get(0));

		// get all VMs for these nodes
		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();

		if (!isFederation && c.getBoundedPolicies() != null) {
			for (Policy pol : c.getBoundedPolicies().getPolicy()) {
				if (pol.getIdref().getDelayBetweenMove() != null && pol.getIdref().getDelayBetweenMove() > delayTimeBetweenMove) {
					delayTimeBetweenMove = pol.getIdref()
							.getDelayBetweenMove();
					log.debug("delayTimeBetweenMove from policy: " + delayTimeBetweenMove);
				}
			}
		}

		if (model.getDatetime() != null) {
			XMLGregorianCalendar earliestLastTimeMove = model.getDatetime();
			log.debug("earliestLastTimeMove: " + earliestLastTimeMove);
			DatatypeFactory factory;
			try {
				factory = DatatypeFactory.newInstance();
				Duration duration = factory.newDuration(false, 0, 0, 0, 0,
						delayTimeBetweenMove, 0); // negative Duration
			
				earliestLastTimeMove.add(duration);
				log.debug("earliestLastTimeMove plus delayBetweenMove: " + earliestLastTimeMove);
				for (Node node : nodes) {
					
					for (ServerType st : allServers) {
						if (st.getFrameworkID().equals(node.getName())) {
							ManagedElementSet<VirtualMachine> vm = src
									.getRunnings(node);
							List<VirtualMachineType> vmModel = org.f4g.optimizer.utils.Utils
									.getVMs(st);
							for (VirtualMachineType vmt : vmModel) {
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
					vms.add(vm.get(vmt.getFrameworkID()));
								}
							}
						}
					}					
				}
				if (vms.size() != 0) {
					log.debug("Adding F4GDelayBetweenMove constraint");
					v.addConstraint(new F4GDelayBetweenMove(vms));
				}
			} catch (DatatypeConfigurationException e1) {
				log.error("Exception", e1);
			}
			
		} else {
			log.debug("No model datetime is set");
		}
	}

	
	private void addVMPaybackTimeConstraint(Cluster c, int myPaybackTime) {
		
		ManagedElementSet<Node> nodes = Utils.getNodesFromCluster(c, src);
		ManagedElementSet<VirtualMachine> vms = Utils.getVMsFromNodes(nodes, src);

		if(myPaybackTime > 0) {
			v.addConstraint(new F4GVMPaybackTimeConstraint(vms, myPaybackTime, model, SLAvms, powerCalculator, costEstimator ));
		}			
	}

	private void addPeriodVMThreshold(ManagedElementSet<Node> nodes, List<PeriodType> periods, float overbooking) {
		
		if(model.getDatetime() != null) {
			LoadType loadType = SLAReader.getVMSlotsThreshold(model.getDatetime().toGregorianCalendar().getTime(), periods);					
			if(nodes.size() !=0 && 
			   loadType != null && loadType.getLowVMSlotsThreshold() > 0 ) {
				v.addConstraint(new SpareCPUs(nodes, loadType.getLowVMSlotsThreshold()/100, overbooking));
			}
			   	
		} else {
			log.warn("the model doesn't specify the current time: cannot use the on/off policy");
		}	

	}

		
}
