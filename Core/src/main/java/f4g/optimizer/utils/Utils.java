package f4g.optimizer.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import f4g.optimizer.utils.OptimizerRackServer;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.utils.OptimizerServer.CreationImpossible;
import f4g.optimizer.utils.OptimizerBladeServer;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.metamodel.*;
import f4g.commons.util.Util;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.btrplace.model.Node;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType;

public class Utils {
	
	static Logger log = Logger.getLogger(Utils.class.getName()); 
 

	public static Datacenter getFirstDatacenter(FIT4Green model) {
		if(model != null)
			try {
				return (Datacenter)JXPathContext.newContext(model).getValue("site[1]/datacenter[1]", Datacenter.class);
			} catch (JXPathNotFoundException e) {
				return null;
			}				
		else
			return null;
		
	}
	
	public static Datacenter getServerDatacenter(Server server, FIT4Green model) {
		
		return getServerDatacenterbyName(server.getFrameworkID(), model);
	}
	
	public static Datacenter getServerDatacenterbyName(String server, FIT4Green model) {
		
		for(Site site : model.getSite()) {
			for(Datacenter dc : site.getDatacenter()) {
				for(Server myServer : getAllServers(site)) {
					if (myServer.getFrameworkID().equals(server)) {
						return dc;
					}
				}
			}
		}
		log.error("DC not found for server " + server);
		return null;
	}
	
	public static Site getServerSite(Server server, FIT4Green model) {
		
		for(Site site : model.getSite()) {
			for(Server myServer : getAllServers(site)) {
				if (myServer.getFrameworkID().equals(server.getFrameworkID())) {
					return site;
				}
			}
		}
		log.error("site not found for server " + server.getFrameworkID());
		return null;
	}
		
	public static Site getNetworkNodeSite(NetworkNode node, FIT4Green model) {
				
		for(Site site : model.getSite()) {
			for(NetworkNode myNode : getAllNetworkNodes(site)) {
				if (myNode.getFrameworkID().equals(node.getFrameworkID())) {
					return site;
				}
			}
		}
		log.error("site not found for node " + node.getFrameworkID());
		return null;
    }
    
    
	public static List<Datacenter> getAllDatacenters(FIT4Green model) {
		List<Datacenter> DCs = new ArrayList<Datacenter>();
		
		for(Site s : model.getSite()) {
			for(Datacenter dc : s.getDatacenter()) {
				DCs.add(dc);
			}
		}
		return DCs;
		
	}
	
	public static long getMemory(Server server) {
		int memory=0;
		for(Mainboard mainboard : server.getMainboard())
	    	for(RAMStick RAMStick : mainboard.getRAMStick())
	    		memory += RAMStick.getSize().getValue();
		return memory;
	}
	
//	public static int getDiskIO(Server server) {
//		int diskIO=0;
//		for(Mainboard mainboard : server.getMainboard())
//	    	for(RAMStick RAMStick : mainboard.getHardDisk().get(0).get)
//	    		diskIO += RAMStick.getSize();
//		return diskIO;
//	}

	public static  double getStorage(Server server) {
		int storage = 0;
		for(StorageUnit storageUnit : getAllStorages(server))
	   		storage += storageUnit.getStorageCapacity().getValue();
		return storage;
	}
	
	public static  double getNetworkBandwich(Server server) {
		double bandwidth = 0;
		for(NIC NIC : getAllNIC(server))
			bandwidth += NIC.getProcessingBandwidth().getValue();
		return bandwidth;
	}
	
	public static int getNbCores(Server server) {
		int cores=0;
		for(Mainboard mainboard : server.getMainboard())
	    	for(CPU CPU : mainboard.getCPU())
	    		cores += CPU.getCore().size();
		return cores;
	}
	
	public static List<StorageUnit> getAllStorages(Server server){
		
		List<StorageUnit> storage = new ArrayList<StorageUnit>();
		
		for(Mainboard mainboard : server.getMainboard()){
	    	for(StorageUnit HD : mainboard.getHardDisk())
	    		storage.add(HD);
	    	for(StorageUnit SSD : mainboard.getSolidStateDisk())
	    		storage.add(SSD);
	    	for(RAID raid : mainboard.getHardwareRAID())
	    		for(StorageUnit HD : raid.getHardDisk())
		    		storage.add(HD);	    		
		}		
		return storage;
	}
	
	
	public static List<NIC> getAllNIC(Server server){
		
		List<NIC> NICs = new ArrayList<NIC>();
		
		for(Mainboard mainboard : server.getMainboard()){
	    	for(NIC NIC : mainboard.getEthernetNIC())
	    		NICs.add(NIC);
	    	for(NIC NIC : mainboard.getFiberchannelNIC())
	    		NICs.add(NIC);
		}

		return NICs;
	}
	
	public static List<VirtualMachine> getVMs(Server server){
		if(server.getStatus() == ServerStatus.ON) {
			if(server.getNativeHypervisor() != null)
				return server.getNativeHypervisor().getVirtualMachine(); 
			else if (server.getNativeOperatingSystem() != null && server.getNativeOperatingSystem().getHostedHypervisor().size() != 0)
				return server.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();
			else 
				return new ArrayList<VirtualMachine>();
		} else
			return new ArrayList<VirtualMachine>();
		
	}
	
	public static List<VirtualMachine> getAllVMs(Datacenter dc){
		List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
		for(Server s : getAllServers(dc)) {
				vms.addAll(getVMs(s));
		}
		return vms;
	}
	
	public static List<VirtualMachine> getAllVMs(Site s){
		List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
		for(Datacenter dc : s.getDatacenter()) {
				vms.addAll(getAllVMs(dc));
		}
		return vms;
	}
	
	public static List<VirtualMachine> getAllVMs(FIT4Green f){
		List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
		for(Site s : f.getSite()) {
				vms.addAll(getAllVMs(s));
		}
		return vms;
	}
	public static List<Fan> getAllFans(Server server){
		
		List<Fan> fans = new ArrayList<Fan>();
		
		//FIXME finish
		
		return fans;
		
		
	}
	
	public static List<BoxNetwork> getAllBoxNetwork(Datacenter datacenter){
		
		List<BoxNetwork> boxNetworks = new ArrayList<BoxNetwork>();
		
		boxNetworks.addAll(datacenter.getBoxRouter());
		boxNetworks.addAll(datacenter.getBoxSwitch());
		
		return boxNetworks;
		
	}

	/**
	 * retrieve all servers
	 */
	public static List<Server> getAllServers(FIT4Green f4g) {
		List<Server> servers = new ArrayList<Server>();
		for(Site s : f4g.getSite()){
			servers.addAll(getAllServers(s));
		}
		return servers;
		
	}
	
	/**
	 * retrieve all servers in a site
	 */
	public static List<Server> getAllServers(Site site) {
		List<Server> servers = new ArrayList<Server>();
		for(Datacenter dc : site.getDatacenter()){
			servers.addAll(getAllServers(dc));
		}
		return servers;
		
	}
	/**
	 * retrieve all servers in a datacenter
	 */
	public static List<Server> getAllServers(Datacenter datacenter) {
		List<Server> servers = new ArrayList<Server>();
		//log.debug("adding " + datacenter.getTowerServer().size() + " TowerServer");
		servers.addAll(datacenter.getTowerServer());
		
		for(Rack rack : datacenter.getRack()){
			//log.debug("adding " + rack.getRackableServer().size() + " RackableServer");
			servers.addAll(rack.getRackableServer());

			for(Enclosure enclosure : rack.getEnclosure()){
				//log.debug("adding " + enclosure.getBladeServer().size() + " BladeServer");
				servers.addAll(enclosure.getBladeServer());
			}
		}
		return servers;
	}
	
	
	public static ArrayList<Core> getAllCores(Mainboard mainboard){
		
		ArrayList<Core> cores = new ArrayList<Core>();
		
		for(CPU CPU : mainboard.getCPU())
			for(Core core : CPU.getCore())
				cores.add(core);							
				
		return cores;
	}
	

	/**
	 * retrieve all optimizer servers in a datacenter for Cloud
	 * @param vms 
	 * 
	 */
	public static ArrayList<IOptimizerServer> getAllOptimizerServersCloud(Datacenter datacenter, VMFlavorType vms) {
		ArrayList<IOptimizerServer> servers = new ArrayList<IOptimizerServer>();

		//creating optimizer servers from towers, rackables and blades
		for(TowerServer tower : datacenter.getTowerServer()) {
			try {
				if(tower.getName() == ServerRole.CLOUD_NODE_CONTROLLER)
					servers.add(new OptimizerTowerServer(tower, vms));
			} catch (CreationImpossible e) {
				log.warn("Creation of an optimizer server impossible for server " + tower.getFrameworkID());
			}
		}
					
		for(Rack rack : datacenter.getRack()) {
			for(RackableServer rackable : rack.getRackableServer()) {
				try {
					if(rackable.getName() == ServerRole.CLOUD_NODE_CONTROLLER)
						servers.add(new OptimizerRackServer(rackable, vms));
				} catch (CreationImpossible e) {
					log.warn("Creation of an optimizer server impossible for server " + rackable.getFrameworkID());
				}
			}
			for(Enclosure enclosure : rack.getEnclosure()) {
				for(BladeServer blade : enclosure.getBladeServer()) {
					try {
						if(blade.getName() == ServerRole.CLOUD_NODE_CONTROLLER)
							servers.add(new OptimizerBladeServer(blade, vms));
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
	public static ArrayList<IOptimizerServer> getAllOptimizerServersTradi(Datacenter datacenter) {
		ArrayList<IOptimizerServer> servers = new ArrayList<IOptimizerServer>();

		//creating optimizer servers from towers, rackables and blades
		for(TowerServer tower : datacenter.getTowerServer()) {
			try {
				servers.add(new OptimizerTowerServer(null, tower));
			} catch (CreationImpossible e) {
				log.warn("Creation of an optimizer server impossible for server " + tower.getFrameworkID());
			}
		}
			
		for(Rack rack : datacenter.getRack()) {
			for(RackableServer rackable : rack.getRackableServer()) {
				try {
					servers.add(new OptimizerRackServer(null, rackable));
				} catch (CreationImpossible e) {
					log.warn("Creation of an optimizer server impossible for server " + rackable.getFrameworkID());
				}
			}
			for(Enclosure enclosure : rack.getEnclosure()) {
				for(BladeServer blade : enclosure.getBladeServer()) {
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
	public static List<NetworkNode> getAllNetworkNodes(Site site) {
    
		List<NetworkNode> nodes = new ArrayList<NetworkNode>();
		for(Datacenter dc : site.getDatacenter()){
			nodes.addAll(getAllNetworkNodes(dc));
		}
		return nodes;
		
	}


	/**
	 * retrieve all network nodes in a datacenter
	 * 
	 */
	public static List<NetworkNode> getAllNetworkNodes(Datacenter datacenter) {
		
		ArrayList<NetworkNode> networkNodes = new ArrayList<NetworkNode>();
		
		//add NICs from all servers
		for(Server server : Utils.getAllServers(datacenter))
			for(Mainboard mainboard : server.getMainboard()) {
				for(NIC NIC : mainboard.getEthernetNIC())
					networkNodes.add(NIC);
				for(NIC NIC : mainboard.getFiberchannelNIC())
					networkNodes.add(NIC);
			}
				
		//add Racked Network
		for(Rack rack : datacenter.getRack())
			networkNodes.addAll(rack.getRackableRouter());
		
		//add Racked Network
		for(Rack rack : datacenter.getRack())
			networkNodes.addAll(rack.getRackableSwitch());
		
		//add NICs for enclosures
		for(Rack rack : datacenter.getRack())
			for(Enclosure enclosure : rack.getEnclosure()) {
				for(NIC NIC : enclosure.getEthernetNIC())
					networkNodes.add(NIC);
				for(NIC NIC : enclosure.getFiberchannelNIC())
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
	public static void addVM(final OptimizerWorkload WL, final Server server){

		Server toServer = server;
		
		List<VirtualMachine> VMs = getVMs(toServer);
		
		VMs.add(WL);
		
		final int nbAskedCores = WL.getNumberOfCPUs().getValue();
		final Double percentageCores = WL.getActualCPUUsage().getValue();
		log.debug("addVM: percentageCores=" + percentageCores);
		
		//for now, only first mainboard is treated
		Mainboard myMainboard = toServer.getMainboard().get(0);
		
		//get all cores in mainboard
		ArrayList<Core> cores = Utils.getAllCores(myMainboard);
				
		//get only the not loaded ones
		Collection<Core> freeCores = Collections2.filter( cores, new Predicate<Core>() { 
	        @Override public boolean apply(Core core) { return core.getCoreLoad() != null &&
	        	                                                   core.getCoreLoad().getValue() <=  1 - percentageCores; }               
	    });		

		log.debug("freeCores.size=" + freeCores.size());
		if(freeCores.size() >= nbAskedCores)
		{
			//take first cores
			Iterator<Core> corestoFill = Iterators.limit(freeCores.iterator(), nbAskedCores);
						
			while(corestoFill.hasNext()) { 
				log.debug("core");
				Core core = corestoFill.next();
				core.setCoreLoad(new CoreLoad(core.getCoreLoad().getValue() + percentageCores));
			} 
			
			//increase core load
//			Collections2.transform(corestoFill, new Function<Core, Core>(){
//	            @Override public Core apply(final Core input){
//	            	log.debug("increase core");
//	            	input.setCoreLoad(input.getCoreLoad() + percentageCores);
//	            	
//	                return input;
//	            }
//	        });
			
		}
		
		//increment the memory usage on the mainboard
		if(myMainboard.getMemoryUsage() != null)
			myMainboard.setMemoryUsage(new MemoryUsage(myMainboard.getMemoryUsage().getValue() + WL.getActualMemoryUsage().getValue()));

		// commented because 1. disk usage as no real impact on energy consumption 2. these is no disk usage field in HD.
//		List<StorageUnit> storages = Utils.getAllStorages(toServer);
//		
//		//get the disks big enough
//		List<StorageUnit> freeDisks = (List<StorageUnit>) Collections2.filter( storages, new Predicate<StorageUnit>() { 
//	        @Override public boolean apply(StorageUnit storage) { return storage.getStorageCapacity() <=  WL.getActualStorageUsage(); }               
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
	public static VirtualMachine findVirtualMachineByName(List<VirtualMachine> VMs, final String frameworkID) {
		
		Iterator<VirtualMachine> it = VMs.iterator();
		Predicate<VirtualMachine> isID = new Predicate<VirtualMachine>() {
	        @Override public boolean apply(VirtualMachine s) {
	            return s.getFrameworkID().equals(frameworkID);
	        }               
	    };
	
		return Iterators.find(it, isID);		
	}
	
	public static VirtualMachine findVirtualMachineByName(FIT4Green model, final String frameworkID) {
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
    public static List<NetworkNode> getAllNetworkDeviceNodes(FIT4Green f4g) {
        List<NetworkNode> nodes = new ArrayList<NetworkNode>();
        for(Site s : f4g.getSite()){
            nodes.addAll(getAllNetworkDeviceNodes(s));
        }
        return nodes;
        
    }
    
    /**
     * retrieve all network devices (only switches/routers) in a site
     *
     * @author rlent
     */
    public static List<NetworkNode> getAllNetworkDeviceNodes(Site site) {
        
        List<NetworkNode> nodes = new ArrayList<NetworkNode>();
        for(Datacenter dc : site.getDatacenter()){
            nodes.addAll(getAllNetworkDeviceNodes(dc));
        }
        return nodes;
        
    }
    
    
    /**
     * retrieve all network devices (only switches/routers) in a datacentre
     *
     * @author rlent
     */
    public static List<NetworkNode> getAllNetworkDeviceNodes(Datacenter datacenter) {
        
        ArrayList<NetworkNode> networkNodes = new ArrayList<NetworkNode>();
        
        // add Racked Network
        for(Rack rack : datacenter.getRack())
            networkNodes.addAll(rack.getRackableRouter());
        
        // add Racked Network
        for(Rack rack : datacenter.getRack())
            networkNodes.addAll(rack.getRackableSwitch());
        
        // add BoxRouters
        networkNodes.addAll(datacenter.getBoxRouter());
        
        // add BoxSwitches
        networkNodes.addAll(datacenter.getBoxSwitch());
        
        return networkNodes;
    }
    
    
    public static Site getNetworkSite(NetworkNode netdev, FIT4Green model) {
        
        for(Site site : model.getSite()) {
            
            for(NetworkNode mynetdev : getAllNetworkDeviceNodes(site))
                if (mynetdev.getFrameworkID().equals(netdev.getFrameworkID()))
                    return site;
            
        }
        log.error("site not found for netdev " + netdev.getFrameworkID());
        return null;
    }
    
    public static List<NetworkNode> getAttachedSwitches(Server server, FIT4Green model) {
        
        ArrayList<NetworkNode> swlist = new ArrayList<NetworkNode>();
        
        for(Mainboard mainboard : server.getMainboard()) {
            for (NIC nic : mainboard.getEthernetNIC()) {
                for (NetworkPort port : nic.getNetworkPort()) {
                    try {
                        String swid = (String) port.getNetworkPortRef();
                        NetworkNode sw = findNetworkNodeByName(model, swid);
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
	public static NetworkNode findNetworkNodeByName(FIT4Green f4g, final String frameWorkID) {
		
		Iterator<NetworkNode> it = Utils.getAllNetworkDeviceNodes(f4g).iterator();
		Predicate<NetworkNode> isID = new Predicate<NetworkNode>() {
	        @Override public boolean apply(NetworkNode s) {
	            return s.getFrameworkID().equals(frameWorkID);
	        }               
	    };
	
		return Iterators.find(it, isID);
	}
	
	
    
    

	/**
	 * finds a server name by its ID
	 * throws NoSuchElementException if not found
	 */
	public static Server findServerByName(FIT4Green f4g, final String frameWorkID) throws NoSuchElementException{
		
		Iterator<Server> it = Utils.getAllServers(f4g).iterator();
		Predicate<Server> isID = new Predicate<Server>() {
	        @Override public boolean apply(Server s) {
	            return s.getFrameworkID().equals(frameWorkID);
	        }               
	    };
        
		return Iterators.find(it, isID);
	}
	

	/**
	 * get the servers in a cluster
	 * 
	 */
	public static List<Server> getNodesInCluster(Cluster c, FIT4Green model) {
		List<Server> servers = new ArrayList<Server>();
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
	public static Set<Node> getNodesFromCluster(Cluster c, NamingService<Node> ns) {
		Set<Node> nodes = new HashSet<Node>();
		for (String nodeName : c.getNodeController().getNodeName()) {
			try {
				Node n = ns.getElement(nodeName);
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
	public static Set<Node> getNodesFromFederation(FederationType fed, NamingService<Node> ns) {
		Set<Node> nodes = new HashSet<Node>();
		if (fed.getBoundedCluster() != null) {
			for (BoundedClustersType.Cluster bc : fed.getBoundedCluster().getCluster()) {
				ClusterType.Cluster c = bc.getIdref();
				nodes.addAll(getNodesFromCluster(c, ns));
			}
		}
		return nodes;
		
	}
	
	/**
	 * get the nodes in a cluster
	 * 
	 */
//	public static ManagedElementSet<VirtualMachine> getVMsFromNodes(ManagedElementSet<Node> nodes, Configuration src) {
//		// get all VMs for these nodes
//		ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
//		for (Node node : nodes) {
//			try {
//				ManagedElementSet<VirtualMachine> vm = src.getRunnings(node);
//				vms.addAll(vm);
//			} catch (Exception e) {
//			}
//
//		}
//		return vms;
//	}
//	
	public static List<Server> getAllServersInFederation(FederationType fed, final FIT4Green f4g) {
		List<Server> serversInFed = new ArrayList<Server>();
		for (BoundedClustersType.Cluster bc : fed.getBoundedCluster().getCluster()) {
			ClusterType.Cluster c = bc.getIdref();
			serversInFed.addAll(getServersInCluster(c, f4g));
		}
		return serversInFed;
		
	}
	
	public static List<Server> getServersInCluster(ClusterType.Cluster cluster, final FIT4Green f4g) {
		List<Server> serversInCluster = new ArrayList<Server>();

		for (String name : cluster.getNodeController().getNodeName()) {
			try {
				Server s = Utils.findServerByName(f4g, name);
				serversInCluster.add(s);
			} catch (NoSuchElementException e) {
				log.warn("Server " + name + " from cluster " + cluster.getName() + " not found");
			}
			
		}
		return serversInCluster;
	}
	
	public static boolean isOffServers(FIT4Green f4g) {
		for(Server s : getAllServers(f4g)) {
			if(s.getStatus() == ServerStatus.OFF) {
				return true;
			}
		}
		return false;
		
	}
	
	//TODO simplified versions of accessors
	public static Frequency getCPUFrequency(Server server) {
		return server.getMainboard().get(0).getCPU().get(0).getCore().get(0).getFrequency();
	}
	
	public static Optional<Frequency> getGPUFrequency(Server server) {
		if(server.getMainboard().get(0).getGPU() != null) {
			return Optional.of(server.getMainboard().get(0).getGPU().get(0).getCoreFrequency());
		} else {
			return Optional.absent();
		}
		
	}
	
	public static StorageCapacity getHDDCapacty(Server server) {
		return server.getMainboard().get(0).getHardDisk().get(0).getStorageCapacity();
	}
	
	public static Optional<RAIDLevel> getRAIDLevel(Server server) {
		if(server.getMainboard().get(0).getHardwareRAID().get(0) != null) {
			return Optional.of(server.getMainboard().get(0).getHardwareRAID().get(0).getLevel());
		} else {
			return Optional.absent();
		}
	}
	
	public static Optional<Bandwidth> getBandwidth(Server server) {
		if(server.getMainboard().get(0).getEthernetNIC().get(0).getProcessingBandwidth() != null) {
			return Optional.of(server.getMainboard().get(0).getEthernetNIC().get(0).getProcessingBandwidth());
		} else {
			return Optional.absent();
		}
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
