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
package f4g.optimizer.entropy.plan.constraint.factories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.metamodel.Datacenter;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.VirtualMachine;



/**
 * This class creates all the constraints linked to the model
 * 
 */
public class ModelConstraintFactory extends ConstraintFactory {
    
	protected FIT4Green F4GModel;
	VMFlavorType currentVMFlavor;
	Map<String, Integer> VMCPUConstraints;

	/**
	 * Constructor needing an instance of the SLAReader and an entropy
	 * configuration element.
	 */
	public ModelConstraintFactory(Model model, FIT4Green F4GModel, VMFlavorType vm, Map<String, Integer> VMCPUConstraints) {
		super(model);
		log = Logger.getLogger(this.getClass().getName()); 
		this.F4GModel = F4GModel;
		this.currentVMFlavor = vm;
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
		
		for(Server s : Utils.getAllServers(F4GModel)) {
	
			Node n = nodeNames.getElement(s.getFrameworkID());
			if(n!=null)	 {
				switch(s.getName()) {          
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
		List<Datacenter> dcs = Utils.getAllDatacenters(F4GModel);
		for(Datacenter dc : dcs) {
			i++;
			
			//Get all VMs of the DC
			Set<VM> vms = new HashSet<VM>();
			for(VirtualMachine vm : Utils.getAllVMs(dc)) {
				VM myVM = vmNames.getElement(vm.getFrameworkID());
				if(myVM != null) {
					vms.add(myVM);
				}				
			}
						
			//get all nodes of the DC
			Set<Node> nodes = new HashSet<Node>();
			for(Server s : Utils.getAllServers(dc)) {
				Node n = nodeNames.getElement(s.getFrameworkID());
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
		for(VirtualMachine VM : Utils.getAllVMs(F4GModel)) {
		
			VM vm = vmNames.getElement(VM.getFrameworkID());	
			int VMconsumption;
			if(VMCPUConstraints.containsKey(VM.getFrameworkID())) {
				VMconsumption = VMCPUConstraints.get(VM.getFrameworkID());
			} else {
				VMconsumption = F4GConfigurationAdapter.getVMCPUConsumption(vm, VM, currentVMFlavor);
			}			
			
			v.add(new Preserve(vm, F4GConfigurationAdapter.SHAREABLE_RESOURCE_CPU, VMconsumption));
			
		}
		return v;
	}
	
}
