/**
 * ============================== Header ============================== 
 * file:          F4GHardwareConstraint.java
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

import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.DatacenterType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.SiteType;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.Fence;


/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author ts
 */
public class F4GHardwareConstraint extends F4GConstraint {

	private Fence fence;
	private FIT4GreenType model;

	/**
	 * 
	 */
	public F4GHardwareConstraint(int i, Object o, ManagedElementSet<Node> nodes,
			ManagedElementSet<VirtualMachine> vms, FIT4GreenType model) {
		super(); 
		this.model = model;
		
		ManagedElementSet<Node> fulfillingNodes = new SimpleManagedElementSet<Node>();
		switch (i) {
		case 1: //CPUFrequency
			for (Node n : nodes){
				if (n.getCPUCapacity() >= (int) (Double.parseDouble(o.toString())/ 1000000)){ 
					fulfillingNodes.add(n);
				}
			}
			break;
		case 2: //GPUFrequency				
			fulfillingNodes.addAll(getNodesForGPUFrequencyConstraint((int) Double.parseDouble(o.toString()), nodes));
			break;
		case 3: //RaidLevel				
			fulfillingNodes.addAll(getNodesForRAIDConstraint( (int) Integer.parseInt(o.toString()), nodes)); 
			break;
		case 4: //CPUCores
			for (Node n : nodes){				
				if (n.getNbOfCPUs() >= (int) Integer.parseInt(o.toString())){ 
					fulfillingNodes.add(n);
				}				
			}
			break;
		case 5: //Bandwidth		
			int bw = (int) Double.parseDouble(o.toString());			
			fulfillingNodes.addAll(getNodesForBandwidthConstraint(bw, nodes)); 
			break;
		default:
			break;
		}
		if (fulfillingNodes.size() > 0)
			this.fence = new Fence(vms, fulfillingNodes);
	}
	
	private ManagedElementSet<Node> getNodesForRAIDConstraint(int raidlevel, ManagedElementSet<Node> nodes){
		ManagedElementSet<Node> n = new SimpleManagedElementSet<Node>();
		int i = 0;
		int j = 0;
		
		boolean correctRaid = true;
		boolean isServerForConstraint = false;
		for (SiteType st : model.getSite())
			for (DatacenterType dt : st.getDatacenter())
				//get all nodes in a DC 				
				for(ServerType server : Utils.getAllServers(dt)) { 
					i = 0; 
					j = 0;
					correctRaid = true;
					isServerForConstraint = false;
					//Check if the server is part of the cluster/constraint
					while (!isServerForConstraint){
						if (nodes.size() > i && server.getFrameworkID().equals(nodes.get(i).getName().toString())){
							isServerForConstraint = true;
							
							//Check if HardwareRaid is correct
							for(MainboardType mt : server.getMainboard()){
								if (mt.getHardwareRAID().size() > j+1 && (mt.getHardwareRAID().get(j++) != null) && mt.getHardwareRAID().size() > j && mt.getHardwareRAID().get(j++).getLevel().getValue() != raidlevel)
									correctRaid = false; 
							}
							//If server has correct HardwareRaid, add it to nodes
							if (correctRaid){
								if (nodes.size() > i)
									n.add(nodes.get(i));
							}
						}
						i++;							
					}						
				}
		return n;		
	}
	
	private ManagedElementSet<Node> getNodesForBandwidthConstraint(int bandwidth, ManagedElementSet<Node> nodes){
		ManagedElementSet<Node> n = new SimpleManagedElementSet<Node>();
		int i = 0;
		int j = 0;
		boolean correctBW = true;
		boolean isServerForConstraint = false;
		for (SiteType st : model.getSite()){
			
			for (DatacenterType dt : st.getDatacenter()){
				//get all nodes in a DC 		
				
				List<ServerType> test = Utils.getAllServers(dt);
				
				
				for(ServerType server : Utils.getAllServers(dt)) { 
					i = 0; 
					j = 0;					
					correctBW = true;
					isServerForConstraint = false;
					//Check if the server is part of the cluster/constraint
					while (!isServerForConstraint){						
					
						if (server.getFrameworkID().equals(nodes.get(i).getName())){
							isServerForConstraint = true;
							
							try {
								//Check if Bandwidth is correct
								for(MainboardType mt : server.getMainboard()){								    
									if (mt.getEthernetNIC().get(j++).getProcessingBandwidth().getValue() < bandwidth)
										correctBW = false; 
								}
							} catch (Exception e) {
							}
							
							//If server has correct Bandwidth, add it to nodes
							if (correctBW){
								if (nodes.size() > i)
									n.add(nodes.get(i));
							}
						}
						i++;							
					}						
				}}}
		System.out.println(n.toString());
		return n;		
	}
	
	private ManagedElementSet<Node> getNodesForGPUFrequencyConstraint(double GPUFreq, ManagedElementSet<Node> nodes){
		ManagedElementSet<Node> n = new SimpleManagedElementSet<Node>();
		int i = 0;
		int j = 0;
		boolean correctGPUF = true;
		boolean isServerForConstraint = false;
		for (SiteType st : model.getSite())
			for (DatacenterType dt : st.getDatacenter())
				//get all nodes in a DC
				for(ServerType server : Utils.getAllServers(dt)) { 
					i = 0;
					j = 0;
					correctGPUF = true;			
					isServerForConstraint = false;
					//Check if the server is part of the cluster/constraint
					while (!isServerForConstraint && i < nodes.size()){
						j = 0;
						if (nodes.size() > i && server.getFrameworkID().equals(nodes.get(i).getName().toString())){
							isServerForConstraint = true;
							
							//Check if GPU frequencies are correct
							for(MainboardType mt : server.getMainboard()){								
								if (mt.getGPU().size() > j && mt.getGPU().get(j).getCoreFrequency() != null && mt.getGPU().size() > j && mt.getGPU().get(j).getCoreFrequency().getValue() != (int) GPUFreq)
									correctGPUF = false;
								j++;								
							} 
							
							//If server has correct GPU Frequencies, add it to nodes
							if (correctGPUF){
								if (nodes.size() > i)
									n.add(nodes.get(i));
							}
						}
						i++;							
					}						
				}
		System.out.println(n);
		return n;
		//model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getEnclosure().get(0).getBladeServer().get(0).getMainboard().get(0).getHardwareRAID().get(0).getLevel();
	}

	/**
	 * Get the virtual machines involved in the constraint.
	 * 
	 * @return a set of VMs. should not be empty
	 */
	@Override
	public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
		return fence.getAllVirtualMachines();
	}

	/**
	 * Get the set of nodes involved in the constraint.
	 * 
	 * @return a set of nodes, should not be empty
	 */
	public ManagedElementSet<Node> getNodes() {
		return fence.getNodes();
	}

	/**
	 * Get the set of virtual machines involved in the constraint.
	 * 
	 * @return a set of virtual machines, should not be empty
	 */
	public ManagedElementSet<VirtualMachine> getVirtualMachines() {
		return fence.getVirtualMachines();
	}

	@Override
	public String toString() {
		return fence.toString();
	}

	@Override
	public boolean equals(Object o) {
		return fence.equals(o);
	}

	@Override
	public int hashCode() {
		return fence.hashCode();
	}

	@Override
	public void inject(ReconfigurationProblem core) {
		if (fence != null)
		fence.inject(core);
	}

	/**
	 * Check that the constraint is satified in a configuration.
	 * 
	 * @param cfg
	 *            the configuration to check
	 * @return true if the running VMs are hosted on more than one group
	 */
	@Override
	public boolean isSatisfied(Configuration cfg) {
		if (fence != null)
		return fence.isSatisfied(cfg);
		else return true;
	}

	@Override
	public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
		return fence.getMisPlaced(cfg);
	}
	
	public boolean isCorrect(){
		return (fence != null);
	}

}
