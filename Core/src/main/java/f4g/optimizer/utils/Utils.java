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
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import f4g.optimizer.btrplace.NamingService;
import f4g.optimizer.utils.OptimizerServer.CreationImpossible;
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
 

	public static Datacenter getFirstDatacenter(Federation fed) {
		return fed.getDatacenter().get(0);
	}
	
	public static Datacenter getServerDatacenter(Server server, Federation fed) {
		
		return getServerDatacenterbyName(server.getName(), fed);
	}
	
	public static Datacenter getServerDatacenterbyName(ServerName server, Federation fed) {

		for(Datacenter dc : fed.getDatacenter()) {
			for(Server myServer : getAllServers(site)) {
				if (myServer.getFrameworkID().equals(server)) {
					return dc;
				}
			}
		}

		log.error("DC not found for server " + server);
		return fed.getDatacenter().stream().fi;
	}

	
	public static List<VirtualMachine> getAllVMs(Datacenter dc){

		return dc.getServers().stream().flatMap(l -> l.getVMs().stream()).collect(Collectors.toList());
	}

	
	public static List<VirtualMachine> getAllVMs(Federation f4g){
		return f4g.getDatacenter().stream()
				.flatMap(l -> l.getServers().stream())
				.flatMap(l -> l.getVMs().stream())
				.collect(Collectors.toList());
	}

	/**
	 * retrieve all servers
	 */
	public static List<Server> getAllServers(Federation f4g) {

		return f4g.getDatacenter().stream().flatMap(l -> l.getServers().stream()).collect(Collectors.toList());
		
	}

	
	/**
	 * finds a workload name by its ID
	 * 
	 */
	public static VirtualMachine findVirtualMachineByName(List<VirtualMachine> VMs, final VirtualMachineName name) {
		
		Iterator<VirtualMachine> it = VMs.iterator();
		Predicate<VirtualMachine> isID = new Predicate<VirtualMachine>() {
	        @Override public boolean apply(VirtualMachine s) {
	            return s.getName().equals(name);
	        }               
	    };
	
		return Iterators.find(it, isID);		
	}
	
	public static VirtualMachine findVirtualMachineByName(Federation fed, final VirtualMachineName name) {
		return findVirtualMachineByName(Utils.getAllVMs(fed), name);
	}

	/**
	 * finds a server name by its ID
	 * throws NoSuchElementException if not found
	 */
	public static Server findServerByName(Federation f4g, final ServerName name) throws NoSuchElementException{
		
		Iterator<Server> it = Utils.getAllServers(f4g).iterator();
		Predicate<Server> isID = new Predicate<Server>() {
	        @Override public boolean apply(Server s) {
	            return s.getFrameworkID().equals(name);
	        }               
	    };
        
		return Iterators.find(it, isID);
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
