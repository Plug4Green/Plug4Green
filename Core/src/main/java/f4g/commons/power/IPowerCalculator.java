package f4g.commons.power;

import f4g.commons.com.util.PowerData;
import f4g.schemas.java.metamodel.Server;

/**
 * Interface to be implemented by the Power Calculator component
 * 
 * @author FIT4Green
 *
 */
public interface IPowerCalculator {

	/**
	 * Computes the current power consumption
	 * 
	 * @param model
	 * @return data structure containing the power consumption in Watts of the Fit4Green system 
	 */
	//public PowerData computePowerFIT4Green(FIT4Green model);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green site 
	 * 
	 * @param site
	 * @return a data structure containing the power consumption in Watts of a FIT4Green site 
	 */
	//public PowerData computePowerSite(Site site);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green datacenter 
	 * 
	 * @param datacenter
	 * @return a data structure containing the power consumption in Watts of a FIT4Green datacenter 
	 */
	//public PowerData computePowerDatacenter(Datacenter datacenter);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green rack 
	 * 
	 * @param rack
	 * @return a data structure containing the power consumption in Watts of a FIT4Green rack 
	 */
	//public PowerData computePowerRack(Rack rack);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green server 
	 * 
	 * @param server
	 * @return a data structure containing the power consumption in Watts of a FIT4Green server 
	 */
	public PowerData computePowerServer(Server server);
	
	/**
	 * 
	 * Computes the current power consumption of a server's mainboard 
	 * 
	 * @param mainboard, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a server's mainboard 
	 */
	//public PowerData computePowerMainboard(Mainboard mainboard,OperatingSystemType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of a RAID device 
	 * 
	 * @param raid
	 * @return a data structure containing the power consumption in Watts of a RAID device  
	 */
	//public PowerData computePowerRAID(RAID raid);
	
	/**
	 * 
	 * Computes the current power consumption of a hard disk 
	 * 
	 * @param hardDisk
	 * @return a data structure containing the power consumption in Watts of a hard disk   
	 */
	//public PowerData computePowerHardDisk(HardDisk hardDisk);
	
	/**
	 * 
	 * Computes the current power consumption of a solid state disk 
	 * 
	 * @param hardDisk
	 * @return a data structure currently containing a value of zero for the power consumption of a solid state disk   
	 */
	//public PowerData computePowerSolidStateDisk(SolidStateDisk ssdisk);
	
	/**
	 * 
	 * Computes the current power consumption of a central processing unit
	 * 
	 * @param cpu, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a central processing unit  
	 */
	//public PowerData computePowerCPU(CPU cpu,OperatingSystemType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of a core of a specific CPU
	 * 
	 * @param core, numberOfCores (single, dual or quad -cores), operatingSystem 
	 *         
	 * @return a data structure containing the power consumption in Watts of a core of a specific CPU  
	 */
	//public PowerData computePowerCore(Core myCore, CPU cpu,OperatingSystemType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of the RAMs of a server
	 * 
	 * @param mainboard 
	 *         
	 * @return a data structure containing the power consumption in Watts of a core of a specific CPU  
	 */
	//public PowerData computePowerMainboardRAMs(Mainboard mainboard);
	
	
	/**
	 * computes the current power consumption of FANs
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param fan
	 * @return
	 *
	 * @author nasirali
	 */
	//public PowerData computePowerFAN(Fan fan);
	
	/**
	 * compute the current power consumption of SAN
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return
	 *
	 * @author nasirali
	 */
	//public PowerData computePowerSAN(Rack obj);
	
	/**
	 * This method computes only the idle power of SAN.
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return
	 *
	 * @author nasirali
	 */
	//public PowerData computePowerIdleSAN(Rack obj);

/**
	 * compute the current power consumption of NAS

	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return

	 *
	 * @author basmadji
	 */
	//public PowerData computePowerNAS(NAS obj);
	
	/**
	 * This method computes only the idle power of NAS.
	 * {To be completed; use html notation if necessary}
	 * 

	 * @param obj
	 * @return
	 *
	 * @author basmadji

	 */
	//public PowerData computePowerIdleNAS(NAS obj);
	/**
	 * 
	 * This method is called by the core component responsible for starting up and shutting
	 * down the F4G plugin. It must implement all the operations needed to dispose the component
	 * in a clean way (e.g. stopping dependent threads, closing connections, sockets, file handlers, etc.)
	 * 
	 * @return
	 *
	 * @author FIT4Green
	 */
	//boolean dispose();
	
	//public void setSimulationFlag(boolean f);
	//public boolean getSimulationFlag();
	
}
