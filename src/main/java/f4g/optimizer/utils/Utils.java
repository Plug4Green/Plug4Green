/**
* ============================== Header ============================== 
* file:          Utils.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-04-27 14:52:52 +0200 (vie, 27 abr 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1383 $
* 
* short description:
*   utility fonctions to fetch the model 
*   
* ============================= /Header ==============================
*/

package f4g.optimizer.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import f4g.optimizer.utils.OptimizerRackServer;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import f4g.optimizer.utils.OptimizerServer.CreationImpossible;
import f4g.optimizer.utils.OptimizerBladeServer;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.metamodel.*;
import f4g.commons.util.Util;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType;

public class Utils {
	
	static Logger log = Logger.getLogger(Utils.class.getName()); 
 

	public static DatacenterType getFirstDatacenter(FIT4GreenType model) {
		if(model != null)
			try {
				return (DatacenterType)JXPathContext.newContext(model).getValue("site[1]/datacenter[1]", DatacenterType.class);
			} catch (JXPathNotFoundException e) {
				return null;
			}				
		else
			return null;
		
	}
	
	public static DatacenterType getServerDatacenter(ServerType server, FIT4GreenType model) {
		
		return getServerDatacenterbyName(server.getFrameworkID(), model);
	}
	
	public static DatacenterType getServerDatacenterbyName(String server, FIT4GreenType model) {
		
		for(SiteType site : model.getSite()) {
			for(DatacenterType dc : site.getDatacenter()) {
				for(ServerType myServer : getAllServers(site)) {
					if (myServer.getFrameworkID().equals(server)) {
						return dc;
					}
				}
			}
		}
		log.error("DC not found for server " + server);
		return null;
	}
	
	public static SiteType getServerSite(ServerType server, FIT4GreenType model) {
		
		for(SiteType site : model.getSite()) {
			for(ServerType myServer : getAllServers(site)) {
				if (myServer.getFrameworkID().equals(server.getFrameworkID())) {
					return site;
				}
			}
		}
		log.error("site not found for server " + server.getFrameworkID());
		return null;
	}
		
	public static SiteType getNetworkNodeSite(NetworkNodeType node, FIT4GreenType model) {
				
		for(SiteType site : model.getSite()) {
			for(NetworkNodeType myNode : getAllNetworkNodes(site)) {
				if (myNode.getFrameworkID().equals(node.getFrameworkID())) {
					return site;
				}
			}
		}
		log.error("site not found for node " + node.getFrameworkID());
		return null;
    }
    
    
	public static List<DatacenterType> getAllDatacenters(FIT4GreenType model) {
		List<DatacenterType> DCs = new ArrayList<DatacenterType>();
		
		for(SiteType s : model.getSite()) {
			for(DatacenterType dc : s.getDatacenter()) {
				DCs.add(dc);
			}
		}
		return DCs;
		
	}
	
	public static long getMemory(ServerType server) {
		int memory=0;
		for(MainboardType mainboard : server.getMainboard())
	    	for(RAMStickType RAMStick : mainboard.getRAMStick())
	    		memory += RAMStick.getSize().getValue();
		return memory;
	}
	
//	public static int getDiskIO(ServerType server) {
//		int diskIO=0;
//		for(MainboardType mainboard : server.getMainboard())
//	    	for(RAMStickType RAMStick : mainboard.getHardDisk().get(0).get)
//	    		diskIO += RAMStick.getSize();
//		return diskIO;
//	}

	public static  double getStorage(ServerType server) {
		int storage = 0;
		for(StorageUnitType storageUnit : getAllStorages(server))
	   		storage += storageUnit.getStorageCapacity().getValue();
		return storage;
	}
	
	public static  double getNetworkBandwich(ServerType server) {
		double bandwidth = 0;
		for(NICType NIC : getAllNIC(server))
			bandwidth += NIC.getProcessingBandwidth().getValue();
		return bandwidth;
	}
	
	public static int getNbCores(ServerType server) {
		int cores=0;
		for(MainboardType mainboard : server.getMainboard())
	    	for(CPUType CPU : mainboard.getCPU())
	    		cores += CPU.getCore().size();
		return cores;
	}
	
	public static List<StorageUnitType> getAllStorages(ServerType server){
		
		List<StorageUnitType> storage = new ArrayList<StorageUnitType>();
		
		for(MainboardType mainboard : server.getMainboard()){
	    	for(StorageUnitType HD : mainboard.getHardDisk())
	    		storage.add(HD);
	    	for(StorageUnitType SSD : mainboard.getSolidStateDisk())
	    		storage.add(SSD);
	    	for(RAIDType raid : mainboard.getHardwareRAID())
	    		for(StorageUnitType HD : raid.getHardDisk())
		    		storage.add(HD);	    		
		}		
		return storage;
	}
	
	
	public static List<NICType> getAllNIC(ServerType server){
		
		List<NICType> NICs = new ArrayList<NICType>();
		
		for(MainboardType mainboard : server.getMainboard()){
	    	for(NICType NIC : mainboard.getEthernetNIC())
	    		NICs.add(NIC);
	    	for(NICType NIC : mainboard.getFiberchannelNIC())
	    		NICs.add(NIC);
		}

		return NICs;
	}
	
	public static List<VirtualMachineType> getVMs(ServerType server){
		if(server.getStatus() == ServerStatusType.ON) {
			if(server.getNativeHypervisor() != null)
				return server.getNativeHypervisor().getVirtualMachine(); 
			else if (server.getNativeOperatingSystem() != null && server.getNativeOperatingSystem().getHostedHypervisor().size() != 0)
				return server.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
			else 
				return new ArrayList<VirtualMachineType>();
		} else
			return new ArrayList<VirtualMachineType>();
		
	}
	
	public static List<VirtualMachineType> getAllVMs(DatacenterType dc){
		List<VirtualMachineType> vms = new ArrayList<VirtualMachineType>();
		for(ServerType s : getAllServers(dc)) {
				vms.addAll(getVMs(s));
		}
		return vms;
	}
	
	public static List<VirtualMachineType> getAllVMs(SiteType s){
		List<VirtualMachineType> vms = new ArrayList<VirtualMachineType>();
		for(DatacenterType dc : s.getDatacenter()) {
				vms.addAll(getAllVMs(dc));
		}
		return vms;
	}
	
	public static List<VirtualMachineType> getAllVMs(FIT4GreenType f){
		List<VirtualMachineType> vms = new ArrayList<VirtualMachineType>();
		for(SiteType s : f.getSite()) {
				vms.addAll(getAllVMs(s));
		}
		return vms;
	}
	public static List<FanType> getAllFans(ServerType server){
		
		List<FanType> fans = new ArrayList<FanType>();
		
		//FIXME finish
		
		return fans;
		
		
	}
	
	public static List<BoxNetworkType> getAllBoxNetwork(DatacenterType datacenter){
		
		List<BoxNetworkType> boxNetworks = new ArrayList<BoxNetworkType>();
		
		boxNetworks.addAll(datacenter.getBoxRouter());
		boxNetworks.addAll(datacenter.getBoxSwitch());
		
		return boxNetworks;
		
	}

	/**
	 * retrieve all servers
	 */
	public static List<ServerType> getAllServers(FIT4GreenType f4g) {
		List<ServerType> servers = new ArrayList<ServerType>();
		for(SiteType s : f4g.getSite()){
			servers.addAll(getAllServers(s));
		}
		return servers;
		
	}
	
	/**
	 * retrieve all servers in a site
	 */
	public static List<ServerType> getAllServers(SiteType site) {
		List<ServerType> servers = new ArrayList<ServerType>();
		for(DatacenterType dc : site.getDatacenter()){
			servers.addAll(getAllServers(dc));
		}
		return servers;
		
	}
	/**
	 * retrieve all servers in a datacenter
	 */
	public static List<ServerType> getAllServers(DatacenterType datacenter) {
		List<ServerType> servers = new ArrayList<ServerType>();
		//log.debug("adding " + datacenter.getTowerServer().size() + " TowerServer");
		servers.addAll(datacenter.getTowerServer());
		
		for(RackType rack : datacenter.getRack()){
			//log.debug("adding " + rack.getRackableServer().size() + " RackableServer");
			servers.addAll(rack.getRackableServer());

			for(EnclosureType enclosure : rack.getEnclosure()){
				//log.debug("adding " + enclosure.getBladeServer().size() + " BladeServer");
				servers.addAll(enclosure.getBladeServer());
			}
		}
		return servers;
	}
	
	
	public static ArrayList<CoreType> getAllCores(MainboardType mainboard){
		
		ArrayList<CoreType> cores = new ArrayList<CoreType>();
		
		for(CPUType CPU : mainboard.getCPU())
			for(CoreType core : CPU.getCore())
				cores.add(core);							
				
		return cores;
	}
	

	/**
	 * retrieve all optimizer servers in a datacenter for Cloud
	 * @param vmTypes 
	 * 
	 */
	public static ArrayList<IOptimizerServer> getAllOptimizerServersCloud(DatacenterType datacenter, VMTypeType vmTypes) {
		ArrayList<IOptimizerServer> servers = new ArrayList<IOptimizerServer>();

		//creating optimizer servers from towers, rackables and blades
		for(TowerServerType tower : datacenter.getTowerServer()) {
			try {
				if(tower.getName() == ServerRoleType.CLOUD_NODE_CONTROLLER)
					servers.add(new OptimizerTowerServer(tower, vmTypes));
			} catch (CreationImpossible e) {
				log.warn("Creation of an optimizer server impossible for server " + tower.getFrameworkID());
			}
		}
					
		for(RackType rack : datacenter.getRack()) {
			for(RackableServerType rackable : rack.getRackableServer()) {
				try {
					if(rackable.getName() == ServerRoleType.CLOUD_NODE_CONTROLLER)
						servers.add(new OptimizerRackServer(rackable, vmTypes));
				} catch (CreationImpossible e) {
					log.warn("Creation of an optimizer server impossible for server " + rackable.getFrameworkID());
				}
			}
			for(EnclosureType enclosure : rack.getEnclosure()) {
				for(BladeServerType blade : enclosure.getBladeServer()) {
					try {
						if(blade.getName() == ServerRoleType.CLOUD_NODE_CONTROLLER)
							servers.add(new OptimizerBladeServer(blade, vmTypes));
					} catch (CreationImpossible e) {
						log.warn("Creation of an optimizer server impossible for server " + blade.getFrameworkID());
					}
				}
			}
		}

		return servers;
	}

	/**
	 * retrieve all optimizer servers in a datacenter for Tradi
	 * 
	 */
	public static ArrayList<IOptimizerServer> getAllOptimizerServersTradi(DatacenterType datacenter) {
		ArrayList<IOptimizerServer> servers = new ArrayList<IOptimizerServer>();

		//creating optimizer servers from towers, rackables and blades
		for(TowerServerType tower : datacenter.getTowerServer()) {
			try {
				servers.add(new OptimizerTowerServer(null, tower));
			} catch (CreationImpossible e) {
				log.warn("Creation of an optimizer server impossible for server " + tower.getFrameworkID());
			}
		}
			
		for(RackType rack : datacenter.getRack()) {
			for(RackableServerType rackable : rack.getRackableServer()) {
				try {
					servers.add(new OptimizerRackServer(null, rackable));
				} catch (CreationImpossible e) {
					log.warn("Creation of an optimizer server impossible for server " + rackable.getFrameworkID());
				}
			}
			for(EnclosureType enclosure : rack.getEnclosure()) {
				for(BladeServerType blade : enclosure.getBladeServer()) {
					try {
						servers.add(new OptimizerBladeServer(null, blade));
					} catch (CreationImpossible e) {
						log.warn("Creation of an optimizer server impossible for server " + blade.getFrameworkID());
					}
				}
			}
		}

		return servers;
	}



	/**
	 * retrieve all network nodes in a site
	 * 
	 */
	public static List<NetworkNodeType> getAllNetworkNodes(SiteType site) {
    
		List<NetworkNodeType> nodes = new ArrayList<NetworkNodeType>();
		for(DatacenterType dc : site.getDatacenter()){
			nodes.addAll(getAllNetworkNodes(dc));
		}
		return nodes;
		
	}


	/**
	 * retrieve all network nodes in a datacenter
	 * 
	 */
	public static List<NetworkNodeType> getAllNetworkNodes(DatacenterType datacenter) {
		
		ArrayList<NetworkNodeType> networkNodes = new ArrayList<NetworkNodeType>();
		
		//add NICs from all servers
		for(ServerType server : Utils.getAllServers(datacenter))
			for(MainboardType mainboard : server.getMainboard()) {
				for(NICType NIC : mainboard.getEthernetNIC())
					networkNodes.add(NIC);
				for(NICType NIC : mainboard.getFiberchannelNIC())
					networkNodes.add(NIC);
			}
				
		//add Racked Network
		for(RackType rack : datacenter.getRack())
			networkNodes.addAll(rack.getRackableRouter());
		
		//add Racked Network
		for(RackType rack : datacenter.getRack())
			networkNodes.addAll(rack.getRackableSwitch());
		
		//add NICs for enclosures
		for(RackType rack : datacenter.getRack())
			for(EnclosureType enclosure : rack.getEnclosure()) {
				for(NICType NIC : enclosure.getEthernetNIC())
					networkNodes.add(NIC);
				for(NICType NIC : enclosure.getFiberchannelNIC())
					networkNodes.add(NIC);
			}
				
		
		//add BoxRouters
		networkNodes.addAll(datacenter.getBoxRouter());
		
		//add BoxSwitchs
		networkNodes.addAll(datacenter.getBoxSwitch());
		
		return networkNodes;
	}
	
	
	/* 
	 * Add a VM to the server, simulating every load increases
	 */
	public static void addVM(final OptimizerWorkload WL, final ServerType server, final AlgoType algoType){

		ServerType toServer = server;
		
		List<VirtualMachineType> VMs = getVMs(toServer);
		
		VMs.add(WL);
		
		final int nbAskedCores = WL.getNumberOfCPUs().getValue();
		final Double percentageCores = WL.getActualCPUUsage().getValue();
		log.debug("addVM: percentageCores=" + percentageCores);
		
		//for now, only first mainboard is treated
		MainboardType myMainboard = toServer.getMainboard().get(0);
		
		//get all cores in mainboard
		ArrayList<CoreType> cores = Utils.getAllCores(myMainboard);
				
		//get only the not loaded ones
		Collection<CoreType> freeCores = Collections2.filter( cores, new Predicate<CoreType>() { 
	        @Override public boolean apply(CoreType core) { return core.getCoreLoad() != null &&
	        	                                                   core.getCoreLoad().getValue() <=  1 - percentageCores; }               
	    });		

		log.debug("freeCores.size=" + freeCores.size());
		if(freeCores.size() >= nbAskedCores)
		{
			//take first cores
			Iterator<CoreType> corestoFill = Iterators.limit(freeCores.iterator(), nbAskedCores);
						
			while(corestoFill.hasNext()) { 
				log.debug("core");
				CoreType core = corestoFill.next();
				core.setCoreLoad(new CoreLoadType(core.getCoreLoad().getValue() + percentageCores));
			} 
			
			//increase core load
//			Collections2.transform(corestoFill, new Function<CoreType, CoreType>(){
//	            @Override public CoreType apply(final CoreType input){
//	            	log.debug("increase core");
//	            	input.setCoreLoad(input.getCoreLoad() + percentageCores);
//	            	
//	                return input;
//	            }
//	        });
			
		}
		
		//increment the memory usage on the mainboard
		if(myMainboard.getMemoryUsage() != null)
			myMainboard.setMemoryUsage(new MemoryUsageType(myMainboard.getMemoryUsage().getValue() + WL.getActualMemoryUsage().getValue()));

		// commented because 1. disk usage as no real impact on energy consumption 2. these is no disk usage field in HD.
//		List<StorageUnitType> storages = Utils.getAllStorages(toServer);
//		
//		//get the disks big enough
//		List<StorageUnitType> freeDisks = (List<StorageUnitType>) Collections2.filter( storages, new Predicate<StorageUnitType>() { 
//	        @Override public boolean apply(StorageUnitType storage) { return storage.getStorageCapacity() <=  WL.getActualStorageUsage(); }               
//	    });		
//		
//		//increment first disk size
//		if(freeDisks.size() != 0)
//			freeDisks.get(0).setStorageCapacity( freeDisks.get(0).getComputedPower() + WL.getActualStorageUsage());
		
		
	}
	
	
	/**
	 * using JAXB to make a deep cloner (can be time consuming).
	 */
	public static <T> JAXBElement<T> cloner(JAXBElement<T> element) {
        
		java.io.StringWriter sw = new StringWriter();
		JAXBElement<T> jbe = null;
		try {
				        
	        Marshaller marshaller = Util.getJaxbContext().createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
	        marshaller.marshal(element, sw);

	        ByteArrayInputStream input = new ByteArrayInputStream (sw.toString().getBytes()); 
			Unmarshaller unmarshaller =  Util.getJaxbContext().createUnmarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			unmarshaller.setSchema(null);
	        jbe = (JAXBElement<T>) unmarshaller.unmarshal(input);
	        
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

        return jbe;
        
	}
	
	
	/**
	 * finds a workload name by its ID
	 * 
	 */
	public static VirtualMachineType findVirtualMachineByName(List<VirtualMachineType> VMs, final String frameworkID) {
		
		Iterator<VirtualMachineType> it = VMs.iterator();
		Predicate<VirtualMachineType> isID = new Predicate<VirtualMachineType>() {
	        @Override public boolean apply(VirtualMachineType s) {
	            return s.getFrameworkID().equals(frameworkID);
	        }               
	    };
	
		return Iterators.find(it, isID);		
	}
	
	public static VirtualMachineType findVirtualMachineByName(FIT4GreenType model, final String frameworkID) {
		return findVirtualMachineByName(Utils.getAllVMs(model), frameworkID);
	}
	
	public static String getClusterId(String nodeName, ClusterType clusters){
		
		String clusterName = "";
		Boolean found = false;
		if(clusters != null) {
			for(Cluster c : clusters.getCluster()) {
				for(String n : c.getNodeController().getNodeName()) {
					if(n.equals(nodeName)) {
						clusterName = c.getName();
						found = true;
					}
						
				}
			}
		} else {
			log.warn("clusters not defined");
			return "";
		}
		
		if(found) {
			return clusterName;
		} else {
			log.warn("Node " + nodeName + " not found in clusters");
			return "";
		}
			
				
	}
    
    /**
     * retrieve all network devices
     */
    public static List<NetworkNodeType> getAllNetworkDeviceNodes(FIT4GreenType f4g) {
        List<NetworkNodeType> nodes = new ArrayList<NetworkNodeType>();
        for(SiteType s : f4g.getSite()){
            nodes.addAll(getAllNetworkDeviceNodes(s));
        }
        return nodes;
        
    }
    
    /**
     * retrieve all network devices (only switches/routers) in a site
     *
     * @author rlent
     */
    public static List<NetworkNodeType> getAllNetworkDeviceNodes(SiteType site) {
        
        List<NetworkNodeType> nodes = new ArrayList<NetworkNodeType>();
        for(DatacenterType dc : site.getDatacenter()){
            nodes.addAll(getAllNetworkDeviceNodes(dc));
        }
        return nodes;
        
    }
    
    
    /**
     * retrieve all network devices (only switches/routers) in a datacentre
     *
     * @author rlent
     */
    public static List<NetworkNodeType> getAllNetworkDeviceNodes(DatacenterType datacenter) {
        
        ArrayList<NetworkNodeType> networkNodes = new ArrayList<NetworkNodeType>();
        
        // add Racked Network
        for(RackType rack : datacenter.getRack())
            networkNodes.addAll(rack.getRackableRouter());
        
        // add Racked Network
        for(RackType rack : datacenter.getRack())
            networkNodes.addAll(rack.getRackableSwitch());
        
        // add BoxRouters
        networkNodes.addAll(datacenter.getBoxRouter());
        
        // add BoxSwitches
        networkNodes.addAll(datacenter.getBoxSwitch());
        
        return networkNodes;
    }
    
    
    public static SiteType getNetworkSite(NetworkNodeType netdev, FIT4GreenType model) {
        
        for(SiteType site : model.getSite()) {
            
            for(NetworkNodeType mynetdev : getAllNetworkDeviceNodes(site))
                if (mynetdev.getFrameworkID().equals(netdev.getFrameworkID()))
                    return site;
            
        }
        log.error("site not found for netdev " + netdev.getFrameworkID());
        return null;
    }
    
    public static List<NetworkNodeType> getAttachedSwitches(ServerType server, FIT4GreenType model) {
        
        ArrayList<NetworkNodeType> swlist = new ArrayList<NetworkNodeType>();
        
        for(MainboardType mainboard : server.getMainboard()) {
            for (NICType nic : mainboard.getEthernetNIC()) {
                for (NetworkPortType port : nic.getNetworkPort()) {
                    try {
                        String swid = (String) port.getNetworkPortRef();
                        NetworkNodeType sw = findNetworkNodeByName(model, swid);
                        swlist.add(sw);
                    }
                    catch (NoSuchElementException e) {
                    }                    
                 }
            }
        }
        return swlist;
    }



	/**
	 * finds a network device by ID
	 * 
	 */
	public static NetworkNodeType findNetworkNodeByName(FIT4GreenType f4g, final String frameWorkID) {
		
		Iterator<NetworkNodeType> it = Utils.getAllNetworkDeviceNodes(f4g).iterator();
		Predicate<NetworkNodeType> isID = new Predicate<NetworkNodeType>() {
	        @Override public boolean apply(NetworkNodeType s) {
	            return s.getFrameworkID().equals(frameWorkID);
	        }               
	    };
	
		return Iterators.find(it, isID);
	}
	
	
    
    

	/**
	 * finds a server name by its ID
	 * throws NoSuchElementException if not found
	 */
	public static ServerType findServerByName(FIT4GreenType f4g, final String frameWorkID) throws NoSuchElementException{
		
		Iterator<ServerType> it = Utils.getAllServers(f4g).iterator();
		Predicate<ServerType> isID = new Predicate<ServerType>() {
	        @Override public boolean apply(ServerType s) {
	            return s.getFrameworkID().equals(frameWorkID);
	        }               
	    };
        
		return Iterators.find(it, isID);
	}
	

	/**
	 * get the servers in a cluster
	 * 
	 */
	public static List<ServerType> getNodesInCluster(Cluster c, FIT4GreenType model) {
		List<ServerType> servers = new ArrayList<ServerType>();
		for (String nodeName : c.getNodeController().getNodeName()) {
			try {
				servers.add(Utils.findServerByName(model, nodeName));
			} catch (NoSuchElementException e) {
				log.warn("Server name " + nodeName + " from cluster " + c.getName() + "not found in model instance");
			}			
		}
		return servers;
	}

	/**
	 * get the nodes in a cluster
	 * 
	 */
	public static ManagedElementSet<Node> getNodesFromCluster(Cluster c, Configuration src) {
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
		return nodes;
	}
	
	/**
	 * get the nodes in a federation
	 * 
	 */
	public static ManagedElementSet<Node> getNodesFromFederation(FederationType fed, Configuration src) {
		ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
		if (fed.getBoundedCluster() != null) {
			for (BoundedClustersType.Cluster bc : fed.getBoundedCluster().getCluster()) {
				ClusterType.Cluster c = bc.getIdref();
				nodes.addAll(getNodesFromCluster(c, src));
			}
		}
		return nodes;
		
	}
	
	/**
	 * get the nodes in a cluster
	 * 
	 */
	public static ManagedElementSet<VirtualMachine> getVMsFromNodes(ManagedElementSet<Node> nodes, Configuration src) {
		// get all VMs for these nodes
		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
		for (Node node : nodes) {
			try {
				ManagedElementSet<VirtualMachine> vm = src.getRunnings(node);
				vms.addAll(vm);
			} catch (Exception e) {
			}

		}
		return vms;
	}
	
	public static List<ServerType> getAllServersInFederation(FederationType fed, final FIT4GreenType f4g) {
		List<ServerType> serversInFed = new ArrayList<ServerType>();
		for (BoundedClustersType.Cluster bc : fed.getBoundedCluster().getCluster()) {
			ClusterType.Cluster c = bc.getIdref();
			serversInFed.addAll(getServersInCluster(c, f4g));
		}
		return serversInFed;
		
	}
	
	public static List<ServerType> getServersInCluster(ClusterType.Cluster cluster, final FIT4GreenType f4g) {
		List<ServerType> serversInCluster = new ArrayList<ServerType>();

		for (String name : cluster.getNodeController().getNodeName()) {
			try {
				ServerType s = Utils.findServerByName(f4g, name);
				serversInCluster.add(s);
			} catch (NoSuchElementException e) {
				log.warn("Server " + name + " from cluster " + cluster.getName() + " not found");
			}
			
		}
		return serversInCluster;
	}
	
	public static boolean isOffServers(FIT4GreenType f4g) {
		for(ServerType s : getAllServers(f4g)) {
			if(s.getStatus() == ServerStatusType.OFF) {
				return true;
			}
		}
		return false;
		
	}
	

	public static boolean initLogger(String pathName) {
				
		try {
			File dir1 = new File(".");
			System.out.println("Current dir : " + dir1.getCanonicalPath());
			Properties log4jProperties = new Properties();
			if(System.getProperty("log4j.configuration") != null){
				PropertyConfigurator.configure(System.getProperty("log4j.configuration"));				
			} else {
				InputStream isLog4j = new FileInputStream(pathName);
				log4jProperties.load(isLog4j);
				PropertyConfigurator.configure(log4jProperties);				
			}
			log.info("Loading configuration...");
			
			
		} catch (IOException e) {
			System.out.println(e);
			return false;
		}
		
		return true;
	}
	
}
