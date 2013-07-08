package f4g.commons.power;

import f4g.commons.com.util.PowerData;
import f4g.schemas.java.metamodel.CPUArchitectureType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.SiteType;
import f4g.schemas.java.metamodel.DatacenterType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.MainboardType;
import f4g.schemas.java.metamodel.OperatingSystemTypeType;
import f4g.schemas.java.metamodel.RAIDType;
import f4g.schemas.java.metamodel.HardDiskType;
import f4g.schemas.java.metamodel.SolidStateDiskType;
import f4g.schemas.java.metamodel.CPUType;
import f4g.schemas.java.metamodel.CoreType;
import f4g.schemas.java.metamodel.FanType;
import f4g.schemas.java.metamodel.RackType;
import f4g.schemas.java.metamodel.NASType;
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
	public PowerData computePowerFIT4Green(FIT4GreenType model);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green site 
	 * 
	 * @param site
	 * @return a data structure containing the power consumption in Watts of a FIT4Green site 
	 */
	public PowerData computePowerSite(SiteType site);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green datacenter 
	 * 
	 * @param datacenter
	 * @return a data structure containing the power consumption in Watts of a FIT4Green datacenter 
	 */
	public PowerData computePowerDatacenter(DatacenterType datacenter);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green rack 
	 * 
	 * @param rack
	 * @return a data structure containing the power consumption in Watts of a FIT4Green rack 
	 */
	public PowerData computePowerRack(RackType rack);
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green server 
	 * 
	 * @param server
	 * @return a data structure containing the power consumption in Watts of a FIT4Green server 
	 */
	public PowerData computePowerServer(ServerType server);
	
	/**
	 * 
	 * Computes the current power consumption of a server's mainboard 
	 * 
	 * @param mainboard, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a server's mainboard 
	 */
	public PowerData computePowerMainboard(MainboardType mainboard,OperatingSystemTypeType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of a RAID device 
	 * 
	 * @param raid
	 * @return a data structure containing the power consumption in Watts of a RAID device  
	 */
	public PowerData computePowerRAID(RAIDType raid);
	
	/**
	 * 
	 * Computes the current power consumption of a hard disk 
	 * 
	 * @param hardDisk
	 * @return a data structure containing the power consumption in Watts of a hard disk   
	 */
	public PowerData computePowerHardDisk(HardDiskType hardDisk);
	
	/**
	 * 
	 * Computes the current power consumption of a solid state disk 
	 * 
	 * @param hardDisk
	 * @return a data structure currently containing a value of zero for the power consumption of a solid state disk   
	 */
	public PowerData computePowerSolidStateDisk(SolidStateDiskType ssdisk);
	
	/**
	 * 
	 * Computes the current power consumption of a central processing unit
	 * 
	 * @param cpu, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a central processing unit  
	 */
	public PowerData computePowerCPU(CPUType cpu,OperatingSystemTypeType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of a core of a specific CPU
	 * 
	 * @param core, numberOfCores (single, dual or quad -cores), operatingSystem 
	 *         
	 * @return a data structure containing the power consumption in Watts of a core of a specific CPU  
	 */
	public PowerData computePowerCore(CoreType myCore, CPUType cpu,OperatingSystemTypeType operatingSystem);
	
	/**
	 * 
	 * Computes the current power consumption of the RAMs of a server
	 * 
	 * @param mainboard 
	 *         
	 * @return a data structure containing the power consumption in Watts of a core of a specific CPU  
	 */
	public PowerData computePowerMainboardRAMs(MainboardType mainboard);
	
	
	/**
	 * computes the current power consumption of FANs
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param fan
	 * @return
	 *
	 * @author nasirali
	 */
	public PowerData computePowerFAN(FanType fan);
	
	/**
	 * compute the current power consumption of SAN
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return
	 *
	 * @author nasirali
	 */
	public PowerData computePowerSAN(RackType obj);
	
	/**
	 * This method computes only the idle power of SAN.
	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return
	 *
	 * @author nasirali
	 */
	public PowerData computePowerIdleSAN(RackType obj);

/**
	 * compute the current power consumption of NAS

	 * {To be completed; use html notation if necessary}
	 * 
	 * @param obj
	 * @return

	 *
	 * @author basmadji
	 */
	public PowerData computePowerNAS(NASType obj);
	
	/**
	 * This method computes only the idle power of NAS.
	 * {To be completed; use html notation if necessary}
	 * 

	 * @param obj
	 * @return
	 *
	 * @author basmadji

	 */
	public PowerData computePowerIdleNAS(NASType obj);
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
	boolean dispose();
	
	public void setSimulationFlag(boolean f);
	public boolean getSimulationFlag();
	
}
