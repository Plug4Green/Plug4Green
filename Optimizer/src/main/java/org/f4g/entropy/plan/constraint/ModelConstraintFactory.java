/**
 * ============================== Header ============================== 
 * file:          ModelConstraintFactory.java
 * project:       FIT4Green/Optimizer
 * created:       09.10.2011 by ts
 * last modified: $LastChangedDate: 2012-06-13 16:36:15 +0200 (mié, 13 jun 2012) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 1493 $
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package org.f4g.entropy.plan.constraint;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.DatacenterType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.FrameworkCapabilitiesType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.Ban;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Fence;
import entropy.vjob.Offline;
import entropy.vjob.Online;
import entropy.vjob.Root;
import entropy.vjob.VJob;

/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class ModelConstraintFactory {

	public Logger log;  
	private Configuration src;
	private FIT4GreenType model;

	
	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public ModelConstraintFactory(Configuration mySrc,
			FIT4GreenType myModel) {
		log = Logger.getLogger(this.getClass().getName()); 
		src = mySrc;
		model = myModel;
	}

	public List<VJob> getModelConstraints() {
		List<VJob> vs = new ArrayList<VJob>();
		VJob myVJob = getNodeTypeConstraints();
		if(myVJob.getConstraints().size() != 0)
			vs.add(myVJob);
		
		VJob myVJob2 = getFrameworkCapabilitiesConstraints();
		if(myVJob2.getConstraints().size() != 0)
			vs.add(myVJob2);
		
		return vs;
		
	}
	
	public VJob getNodeTypeConstraints() {
		VJob v = new DefaultVJob("modelVJob");
		
		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
		vms.addAll(src.getAllVirtualMachines());
		ManagedElementSet<Node> onlines = new SimpleManagedElementSet<Node>();
		ManagedElementSet<Node> offlines = new SimpleManagedElementSet<Node>();
		ManagedElementSet<Node> empties = new SimpleManagedElementSet<Node>();
		
		for(ServerType s : Utils.getAllServers(model)) {
	
			Node n = src.getAllNodes().get(s.getFrameworkID());
			if(n!=null)	 {
				switch(s.getName()) {          
			    case CLOUD_CONTROLLER          : {
			    	log.debug("Cloud controller " + n.getName());
			    	onlines.add(n); 
			    	empties.add(n);
			    	break;
			    }
			    case CLOUD_CLUSTER_CONTROLLER  : {
			    	log.debug("Cloud cluster controller " + n.getName());
			    	onlines.add(n);
			    	empties.add(n);
			    	break;
			    }
			    case CLOUD_NODE_CONTROLLER     : break;
			    case TRADITIONAL_HOST          : break;
			    case OTHER                     : break;
			    default                        : break;
				}
			}
			
			//Entropy sees the nodes in transition as offline and mustn't switch them on.
			if(s.getStatus() == ServerStatusType.POWERING_OFF) {
				offlines.add(n);
			}
			if(s.getStatus() == ServerStatusType.POWERING_ON) {
				onlines.add(n);
				empties.add(n);
			}
		}
		if(onlines.size() != 0) {
			v.addConstraint(new Online(onlines));	
		}
		
		if(offlines.size() != 0) {
			v.addConstraint(new Offline(offlines));	
		}
		
		if(empties.size() != 0 && vms.size() != 0) {
			v.addConstraint(new Ban(vms, empties));	
		}
		
		return v;
	}

	public VJob getFrameworkCapabilitiesConstraints() {
		VJob v = new DefaultVJob("modelVJob");
		int i = 0;
		List<DatacenterType> dcs = Utils.getAllDatacenters(model);
		for(DatacenterType dc : dcs) {
			i++;
			
			//Get all VMs of the DC
			ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
			for(VirtualMachineType vm : Utils.getAllVMs(dc)) {
				VirtualMachine myVM = src.getAllVirtualMachines().get(vm.getFrameworkID());
				if(myVM != null) {
					vms.add(myVM);
				}				
			}
						
			//get all nodes of the DC
			ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
			for(ServerType s : Utils.getAllServers(dc)) {
				Node n = src.getAllNodes().get(s.getFrameworkID());
				if(n!=null){
					nodes.add(n);
				}
				
			}
			
			for(FrameworkCapabilitiesType fc : dc.getFrameworkCapabilities()) {
				
				if (fc.getVm() != null) {
					//for CP point of view, live migrate and moveVM are considered the same
					//migration inside the DC
					if (fc.getVm().isIntraLiveMigrate()
							|| fc.getVm().isIntraMoveVM()) {
						log.debug("VMs are allowed to move inside DC #" + i);

						//migration between the DCs
						if (fc.getVm().isInterLiveMigrate()
								|| fc.getVm().isInterMoveVM()) {
							log.debug("VMs of DC #" + i
									+ " are allowed to move to another DC");
						} else {
							log.debug("VMs of DC #" + i
									+ " are NOT allowed to move to another DC");
							if (dcs.size() >= 2) {
								if (vms.size() > 0 && nodes.size() > 0) {
									v.addConstraint(new Fence(vms, nodes));
								}
							}

						}

					} else {
						log.debug("VMs are NOT allowed to move in DC #" + i);
						v.addConstraint(new Root(vms));
					}
				}
				
				//node framework capabilities
				if (fc.getNode() != null) {

					if(!fc.getNode().isPowerOff()) {
						
						ManagedElementSet<Node> onNodes = new SimpleManagedElementSet<Node>();
						for(Node n : nodes) {
							if(src.isOnline(n)) {
								onNodes.add(n);
							}
						}	
						//keep ON nodes ON
						if(onNodes.size() != 0) {
							v.addConstraint(new Online(onNodes));							
						}
						
					}
					if(!fc.getNode().isPowerOn()) {
						ManagedElementSet<Node> offNodes = new SimpleManagedElementSet<Node>();
						for(Node n : nodes) {
							if(src.isOffline(n)) {
								offNodes.add(n);
							}
						}	
						//keep OFF nodes OFF
						if(offNodes.size() != 0) {
							v.addConstraint(new Offline(offNodes));
						}
					}
					
				}
			}
		}
		
		return v;
	}
}
