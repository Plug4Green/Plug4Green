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
package f4g.optimizer.btrplace.plan.constraint.factories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import f4g.schemas.java.metamodel.*;
import f4g.schemas.java.sla.VMFlavors;
import org.apache.log4j.Logger;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Ban;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.constraint.Online;
import org.btrplace.model.constraint.Preserve;
import org.btrplace.model.constraint.Root;
import org.btrplace.model.constraint.SatConstraint;

import f4g.optimizer.btrplace.configuration.F4GConfigurationAdapter;
import f4g.optimizer.utils.Utils;
import org.jscience.physics.amount.Amount;

import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

import static f4g.schemas.java.metamodel.ServerRole.CLOUD_CONTROLLER;


/**
 * This class creates all the constraints linked to the model
 * 
 */
public class ModelConstraintFactory extends ConstraintFactory {

	protected Federation fed;
	protected VMFlavors currentVMFlavors;
	protected Map<String, Amount<Dimensionless>> VMCPUConstraints;

	/**
	 * Constructor needing an instance of the SLAReader and BtrPlace
	 * configuration element.
	 */
	public ModelConstraintFactory(Model model, Federation fed, VMFlavors vmFlavors, Map<String, Amount<Dimensionless>> VMCPUConstraints) {
		super(model);
		log = Logger.getLogger(this.getClass().getName()); 
		this.fed = fed;
		this.currentVMFlavors = vmFlavors;
		this.VMCPUConstraints = VMCPUConstraints;
		
	}

	public List<SatConstraint> getModelConstraints() {
		
		List<SatConstraint> vs = new ArrayList<SatConstraint>();
		vs.addAll(getNodeConstraints());
		vs.addAll(getVMConstraints());
		vs.addAll(getFrameworkCapabilitiesConstraints());
	
		return vs;
		
	}
	
	public List<SatConstraint> getNodeConstraints() {
		List<SatConstraint> v = new ArrayList<SatConstraint>();
		
		Set<VM> vms = new HashSet<VM>();
		vms.addAll(map.getAllVMs());
		Set<Node> onlines = new HashSet<Node>();
		Set<Node> offlines = new HashSet<Node>();
		Set<Node> empties = new HashSet<Node>();
		
		for(Server s : Utils.getAllServers(fed)) {
	
			Node n = nodeNames.getElement(s.getServerName().toString());
			if(n!=null)	 {
				switch(s.getServerRole()) {
			    case CLOUD_CONTROLLER          : {
			    	log.debug("Cloud controller " + n.id());
			    	onlines.add(n); 
			    	empties.add(n);
			    	break;
			    }
			    case CLOUD_CLUSTER_CONTROLLER  : {
			    	log.debug("Cloud cluster controller " + n.id());
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
			if(s.getStatus() == ServerStatus.POWERING_OFF) {
				offlines.add(n);
			}
			if(s.getStatus() == ServerStatus.POWERING_ON) {
				onlines.add(n);
				empties.add(n);
			}
		}
		if(onlines.size() != 0) {
			v.addAll(Online.newOnline(onlines));	
		}
		
		if(offlines.size() != 0) {
			v.addAll(Offline.newOffline(offlines));	
		}
		
		if(empties.size() != 0 && vms.size() != 0) {
			v.addAll(Ban.newBan(vms, empties));	
		}
		
		return v;
	}

	public List<SatConstraint> getFrameworkCapabilitiesConstraints() {
		List<SatConstraint> v = new LinkedList<SatConstraint>();
		int i = 0;
		List<Datacenter> dcs = fed.getDatacenters();
		for(Datacenter dc : dcs) {
			i++;
			
			//Get all VMs of the DC
			Set<VM> vms = new HashSet<VM>();
			for(VirtualMachine vm : Utils.getAllVMs(dc)) {
				VM myVM = vmNames.getElement(vm.getName().toString());
				if(myVM != null) {
					vms.add(myVM);
				}				
			}
						
			//get all nodes of the DC
			Set<Node> nodes = new HashSet<Node>();
			for(Server s : dc.getServers()) {
				Node n = nodeNames.getElement(s.getServerName().toString());
				if(n!=null){
					nodes.add(n);
				}
				
			}
			
			for(FrameworkCapabilities fc : dc.getFrameworkCapabilities()) {
				
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
									v.addAll(Fence.newFence(vms, nodes));
								}
							}

						}

					} else {
						log.debug("VMs are NOT allowed to move in DC #" + i);
						v.addAll(Root.newRoots(vms));
					}
				}
				
				//node framework capabilities
				if (fc.getNode() != null) {

					if(!fc.getNode().isPowerOff()) {
						
						Set<Node> onNodes = new HashSet<Node>();
						for(Node n : nodes) {
							if(map.isOnline(n)) {
								onNodes.add(n);
							}
						}	
						//keep ON nodes ON
						if(onNodes.size() != 0) {
							v.addAll(Online.newOnline(onNodes));							
						}
						
					}
					if(!fc.getNode().isPowerOn()) {
						Set<Node> offNodes = new HashSet<Node>();
						for(Node n : nodes) {
							if(map.isOffline(n)) {
								offNodes.add(n);
							}
						}	
						//keep OFF nodes OFF
						if(offNodes.size() != 0) {
							v.addAll(Offline.newOffline(offNodes));
						}
					}
					
				}
			}
		}
		
		return v;
	}
	
	public List<SatConstraint> getVMConstraints() {
		List<SatConstraint> v = new LinkedList<SatConstraint>();
		for(VirtualMachine P4GVM : Utils.getAllVMs(fed)) {
		
			VM vm = vmNames.getElement(P4GVM.getName().toString());
			Amount<Dimensionless> VMConsumption;
			if(VMCPUConstraints.containsKey(P4GVM.getName().toString())) {
				VMConsumption = VMCPUConstraints.get(P4GVM.getName().toString());
			} else {
				VMConsumption = F4GConfigurationAdapter.getVMCPUConsumption(P4GVM, currentVMFlavors);
			}			
			
			v.add(new Preserve(vm, F4GConfigurationAdapter.SHAREABLE_RESOURCE_CPU, (int) VMConsumption.doubleValue(Unit.ONE)));
			
		}
		return v;
	}
	
}
