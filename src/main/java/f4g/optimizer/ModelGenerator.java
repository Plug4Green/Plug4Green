/**
* ============================== Header ============================== 
* file:          ModelGenerator.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2012-07-05 16:23:09 +0200 (jeu. 05 juil. 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1512 $
* 
* short description:
*   Generate a random Model and eventuallysaves it to file.
*   You can run the local main, with argument "optimizer/f4gconfig.properties". A new file f4gmodel_instance_populated.xml
*   will be created in resources directory.
*   This file can then be used for test purpose.   
* ============================= /Header ==============================
*/

package f4g.optimizer;

import org.apache.log4j.Logger;
import f4g.optimizer.utils.AggregatedUsage;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.*;
import f4g.commons.util.Util;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

public class ModelGenerator {
	
	
	
	
	//bounds to construct a server
	public int MIN_CPU = 1;
	public int MAX_CPU = 1;
	public int MIN_CORE = 6; //6 cores
	public int MAX_CORE = 6;
	public double MIN_FREQUENCY = 1.0;
	public double MAX_FREQUENCY = 1.0;
	public double MIN_CPU_POWER_IDLE = 50.0;
	public double MAX_CPU_POWER_IDLE = 50.0;
	public double MIN_CPU_POWER_DELTA_MAX = 50.0;
	public double MAX_CPU_POWER_DELTA_MAX = 50.0;
	public int NUMBER_OF_TRANSISTORS = 731;
	public int MIN_RAM_SIZE = 50;
	public int MAX_RAM_SIZE = 50;
	public int MIN_STORAGE_SIZE = 10000;
	public int MAX_STORAGE_SIZE = 10000;
	public int MIN_FAN = 2;
	public int MAX_FAN = 2;
	public boolean DVFS = true;
	public int CPU_VOLTAGE = 1;
	
	public double MIN_SERVER_POWER_IDLE = 50.0;
	public double MAX_SERVER_POWER_IDLE = 50.0;
	public double MIN_SERVER_POWER_DELTA_MAX = 50.0;
	public double MAX_SERVER_POWER_DELTA_MAX = 50.0;
	public double MIN_NIC_BANDWITCH = 100000000.0;
	public double MAX_NIC_BANDWITCH = 100000000.0;
	public double MIN_NIC_POWER_IDLE = 1.0;
	public double MAX_NIC_POWER_IDLE = 1.0;
	public double MIN_NIC_POWER_DELTA_MAX = 7.0;
	public double MAX_NIC_POWER_DELTA_MAX = 7.0;
	
	//bounds for a VM
	public double MIN_CPU_USAGE = 0.0;
	public double MAX_CPU_USAGE = 100.0;
	public int    MIN_NB_CPU = 1;
	public int    MAX_NB_CPU = 1;
	public double MIN_NETWORK_USAGE = 10000;
	public double MAX_NETWORK_USAGE = 10000;
	public double MIN_STORAGE_USAGE = 100;
	public double MAX_STORAGE_USAGE = 100;
	public double MIN_MEMORY_USAGE = 1;
	public double MAX_MEMORY_USAGE = 1;
	public String VM_TYPE = "m1.small";
	
	public int MIN_NB_SERVERS = 10;
	public int MAX_NB_SERVERS = 10;
	public int MIN_NB_VIRTUAL_MACHINES = 3;
	public int MAX_NB_VIRTUAL_MACHINES = 3;

	public int VM_FRAMEWORK_ID = 20000;
	public int NIC_FRAMEWORK_ID = 30000;
	public int MAINBOARD_FRAMEWORK_ID = 40000;
	public int HYPERVISOR_FRAMEWORK_ID = 50000;
	//start ranges for framework IDs
	public int SERVER_FRAMEWORK_ID = 100000;
	public int ROUTER_FRAMEWORK_ID = 1000000;
	
	public boolean IS_CLOUD = true;
	
	public Logger log;  
	



    // Network stuff
    // rlent
    
	public int    NB_SWITCHES = 0;
	public int    NB_ROUTERS = 0;
    
    public static BitErrorRate defaultBitErrorRate = new BitErrorRate(0.0);          // default values for a typical Ethernet link
    public static PropagationDelay defaultPropagationDelay = new PropagationDelay(0.0);
    public static NetworkTraffic defaultLineCapacity = new NetworkTraffic(8.0);      //  1 Byte/sec
    public static Power defaultPortPowerIdle = new Power(  0 );
    public static Power defaultPortPowerMax = new Power( 1.0 );
    public static NetworkPortBufferSize defaultBufferSize = new NetworkPortBufferSize(50);
    public static Power defaultSwitchPowerIdle = new Power( 48.0 );             // small Cisco switch
    public static Power defaultSwitchPowerMax = new Power( 50.0 );
    public static Power defaultRouterPowerIdle = new Power( 12.0 );             // small Cisco router
    public static Power defaultRouterPowerMax = new Power( 13.0 );
    public static Bandwidth defaultRouterProcessingBandwidth = new Bandwidth(1000000000);
    public static Bandwidth defaultSwitchProcessingBandwidth = new Bandwidth(1000000000);
    

    public String schema_location =  "../Schemas/src/main/schema/MetaModel.xsd";

	public ModelGenerator() {
		
		log = Logger.getLogger(ModelGenerator.class.getName()); 
		
	}
	

	public void setCPU(int mCPU) {
		MAX_CPU = mCPU;
		MIN_CPU = mCPU;
	}

	public void setCORE(int mCORE) {
		MAX_CORE = mCORE;
		MIN_CORE = mCORE;
	}

	public void setFREQUENCY(double mFREQUENCY) {
		MAX_FREQUENCY = mFREQUENCY;
		MIN_FREQUENCY = mFREQUENCY;
	}

	public void setCPU_POWER_IDLE(double mCPUPOWERIDLE) {
		MAX_CPU_POWER_IDLE = mCPUPOWERIDLE;
		MIN_CPU_POWER_IDLE = mCPUPOWERIDLE;
	}

	public void setCPU_POWER_DELTA_MAX(double mCPUPOWERDELTAMAX) {
		MAX_CPU_POWER_DELTA_MAX = mCPUPOWERDELTAMAX;
		MIN_CPU_POWER_DELTA_MAX = mCPUPOWERDELTAMAX;
	}

	public void setRAM_SIZE(int mRAMSIZE) {
		MAX_RAM_SIZE = mRAMSIZE;
		MIN_RAM_SIZE = mRAMSIZE;
	}

	public void setSTORAGE_SIZE(int mSTORAGESIZE) {
		MAX_STORAGE_SIZE = mSTORAGESIZE;
		MIN_STORAGE_SIZE = mSTORAGESIZE;
	}

	public void setFAN(int mFAN) {
		MAX_FAN = mFAN;
		MIN_FAN = mFAN;
	}

	public void setSERVER_POWER_IDLE(double mSERVERPOWERIDLE) {
		MAX_SERVER_POWER_IDLE = mSERVERPOWERIDLE;
		MIN_SERVER_POWER_IDLE = mSERVERPOWERIDLE;
	}

	public void setSERVER_POWER_DELTA_MAX(double mSERVERPOWERDELTAMAX) {
		MAX_SERVER_POWER_DELTA_MAX = mSERVERPOWERDELTAMAX;
		MIN_SERVER_POWER_DELTA_MAX = mSERVERPOWERDELTAMAX;
	}

	public void setNIC_BANDWITCH(double mNICBANDWITCH) {
		MAX_NIC_BANDWITCH = mNICBANDWITCH;
		MIN_NIC_BANDWITCH = mNICBANDWITCH;
	}

	public void setNIC_POWER_IDLE(double mNICPOWERIDLE) {
		MAX_NIC_POWER_IDLE = mNICPOWERIDLE;
		MIN_NIC_POWER_IDLE = mNICPOWERIDLE;
	}

	public void setNIC_POWER_DELTA_MAX(double mNICPOWERDELTAMAX) {
		MAX_NIC_POWER_DELTA_MAX = mNICPOWERDELTAMAX;
		MIN_NIC_POWER_DELTA_MAX = mNICPOWERDELTAMAX;
	}

	public void setCPU_USAGE(double mCPUUSAGE) {
		MAX_CPU_USAGE = mCPUUSAGE;
		MIN_CPU_USAGE = mCPUUSAGE;
	}

	public void setNB_CPU(int mNBCPU) {
		MAX_NB_CPU = mNBCPU;
		MIN_NB_CPU = mNBCPU;
	}

	public void setNETWORK_USAGE(double mNETWORKUSAGE) {
		MAX_NETWORK_USAGE = mNETWORKUSAGE;
		MIN_NETWORK_USAGE = mNETWORKUSAGE;
	}

	public void setSTORAGE_USAGE(double mSTORAGEUSAGE) {
		MAX_STORAGE_USAGE = mSTORAGEUSAGE;
		MIN_STORAGE_USAGE = mSTORAGEUSAGE;
	}

	public void setMEMORY_USAGE(double mMEMORYUSAGE) {
		MAX_MEMORY_USAGE = mMEMORYUSAGE;
		MIN_MEMORY_USAGE = mMEMORYUSAGE;
	}

	public void setVM_TYPE(String vMTYPE) {
		VM_TYPE = vMTYPE;
	}

	public void setNB_SERVERS(int mNBSERVERS) {
		MAX_NB_SERVERS = mNBSERVERS;
		MIN_NB_SERVERS = mNBSERVERS;
	}

	public void setNB_VIRTUAL_MACHINES(int mNBVIRTUALMACHINES) {
		MAX_NB_VIRTUAL_MACHINES = mNBVIRTUALMACHINES;
		MIN_NB_VIRTUAL_MACHINES = mNBVIRTUALMACHINES;
	}

	
	/**
	 * Launcher method
	 * @param args
	 */
	public static void main(String[] args) {
		
		ModelGenerator test = new ModelGenerator();	
		if(args.length == 0){
			System.out.println("Please provide the path of config file as an argument (usually optimizer/f4gconfig.properties).");
			System.exit(1);
		}
		
		boolean res = Utils.initLogger(args[0]);
		
		if(!res){
			System.exit(1);
		}
		
		Logger.getLogger(ModelGenerator.class.getName()).debug("writing test XML");

		//populate an existing model
		test.populateModel("resources/unittest_f4gmodel_instance.xml", "unittest_f4gmodel_instance_populated.xml");
		
		//create a new model
		//test.createPopulatedModel("f4gmodel_instance_populated.xml");
		
	}
	
	
	/**
	 * create a F4G type (everything but servers)
	 *
	 * @author cdupont
	 */
	public FIT4Green createFIT4Green(){
		
			
		FIT4Green fIT4Green = new FIT4Green();
		try {
			fIT4Green.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2012, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 2 ));
		} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
		}
		Site site = new Site();
				
		site.setComputedPower(new Power(0.0));
		site.setCUE(new CUE(10.0));
		site.setPUE(new PUE(1.0));
				
		fIT4Green.getSite().add(site);
		return fIT4Green;
		
	}
	
	/**
	 * create a F4G type (everything but servers)
	 *
	 * @author cdupont
	 */
	public FIT4Green createFIT4Green2Sites(){
		
			
		FIT4Green fIT4Green = new FIT4Green();
		try {
			fIT4Green.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2012, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 2 ));
		} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
		}
		Site site1 = new Site();
				
		site1.setComputedPower(new Power(0.0));
		site1.setCUE(new CUE(30.0));
		site1.setPUE(new PUE(1.0));
				
		fIT4Green.getSite().add(site1);
	
		Site site2 = new Site();
		
		site2.setComputedPower(new Power(0.0));
		site2.setCUE(new CUE(12.0));
		site2.setPUE(new PUE(2.0));
				
		fIT4Green.getSite().add(site2);
		
		return fIT4Green;
		
	}
	
	/**
	 * create a datacenter
	 *
	 * @author cdupont
	 */
	public Datacenter createDatacenter(String FWName){
			
		Datacenter datacenter = new Datacenter();
		Rack rack = new Rack();
		NetworkLoad networkLoad = new NetworkLoad();
		VMActions vMActions = new VMActions();
		
		vMActions.setIntraMoveVM(true);
		vMActions.setInterMoveVM(false);
		vMActions.setIntraLiveMigrate(false);
		vMActions.setInterLiveMigrate(false);
		
		NodeActions nodeActions = new NodeActions();
		nodeActions.setPowerOff(true);
		nodeActions.setPowerOn(true);		
		
		FrameworkCapabilities frameworkCapability = new FrameworkCapabilities();
		frameworkCapability.setFrameworkName(FWName);
		frameworkCapability.setId(FWName);
		frameworkCapability.setVm(vMActions);
		frameworkCapability.setNode(nodeActions);
		frameworkCapability.setStatus(FrameworkStatus.RUNNING);
		
		datacenter.setComputingStyle(DCComputingStyle.CLOUD);
		datacenter.setComputedPower(new Power(0.0));
		datacenter.setNetworkLoad(networkLoad);
	
		rack.setComputedPower(new Power(0.0));
		
		datacenter.getFrameworkCapabilities().add(frameworkCapability);
		datacenter.getRack().add(rack);

		return datacenter;
		
	}

	/**
	 * create a F4G type with servers and VMs
	 *
	 * @author cdupont
	 */
	public FIT4Green createPopulatedFIT4Green() {
		
		FIT4Green fIT4Green = createFIT4Green();
		Datacenter DC1 = createDatacenter("DC1");
		fIT4Green.getSite().get(0).getDatacenter().add(DC1);

        int frameworkid = SERVER_FRAMEWORK_ID;
		
		for(int i=0; i<MAX_NB_SERVERS; i++) {
			DC1.getRack().get(0).getRackableServer()
				.add( createRandomServer(DC1.getFrameworkCapabilities().get(0), frameworkid));
            frameworkid+= SERVER_FRAMEWORK_ID;
        }

        
        // also add network devices
		frameworkid = ROUTER_FRAMEWORK_ID;
		for(int i=0; i<NB_SWITCHES; i++) {
			DC1.getBoxSwitch()
				.add( createRandomBoxSwitch(DC1.getFrameworkCapabilities().get(0), frameworkid));
            frameworkid+= ROUTER_FRAMEWORK_ID;
        }

		for(int i=0; i<NB_ROUTERS; i++) {
			DC1.getBoxRouter()
				.add( createRandomBoxRouter(DC1.getFrameworkCapabilities().get(0), frameworkid));
            frameworkid+= ROUTER_FRAMEWORK_ID;
        }
		


		
		return fIT4Green;
	}
		
	/**
	 * create a F4G type with servers and VMs
	 *
	 * @author cdupont
	 */
	public FIT4Green createPopulatedFIT4Green2DC() {
		
		FIT4Green fIT4Green = createFIT4Green();
		Datacenter DC1 = createDatacenter("DC1");
		Datacenter DC2 = createDatacenter("DC2");
		fIT4Green.getSite().get(0).getDatacenter().add(DC1);
		fIT4Green.getSite().get(0).getDatacenter().add(DC2);
		
		for(int i=0; i<MAX_NB_SERVERS; i++)
			DC1.getRack().get(0).getRackableServer()
				.add( createRandomServer(DC1.getFrameworkCapabilities().get(0), SERVER_FRAMEWORK_ID * i));
		
		for(int i=0; i<MAX_NB_SERVERS; i++)
			DC2.getRack().get(0).getRackableServer()
				.add( createRandomServer(DC2.getFrameworkCapabilities().get(0), 1000000 + SERVER_FRAMEWORK_ID * i));
		
		return fIT4Green;
	}
	
	/**
	 * create a F4G type with servers and VMs
	 *
	 * @author cdupont
	 */
	public FIT4Green createPopulatedFIT4Green2Sites() {
		
		FIT4Green fIT4Green = createFIT4Green2Sites();
		Datacenter DC1 = createDatacenter("DC1");
		Datacenter DC2 = createDatacenter("DC2");
		fIT4Green.getSite().get(0).getDatacenter().add(DC1);
		fIT4Green.getSite().get(1).getDatacenter().add(DC2);
		
		for(int i=0; i<MAX_NB_SERVERS; i++)
			DC1.getRack().get(0).getRackableServer()
				.add( createRandomServer(DC1.getFrameworkCapabilities().get(0), SERVER_FRAMEWORK_ID * i));
		
		for(int i=0; i<MAX_NB_SERVERS; i++)
			DC2.getRack().get(0).getRackableServer()
				.add( createRandomServer(DC1.getFrameworkCapabilities().get(0), 1000000 + SERVER_FRAMEWORK_ID * i));
		
		return fIT4Green;
	}
	
	
	/**
	 * create a server with VMs
	 *
	 * @author cdupont
	 */
	public RackableServer createRandomServer(FrameworkCapabilities frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		RackableServer        rackableServer        = new RackableServer();
		CoolingSystem         coolingSystem         = new CoolingSystem();
		NativeOperatingSystem nativeOperatingSystem = new NativeOperatingSystem();
		Mainboard             mainboard             = new Mainboard();
		RAMStick              RAMStick              = new RAMStick();
		HardDisk              hardDisk              = new HardDisk();
		NIC                   NIC                   = new NIC();
		HostedHypervisor      hostedHypervisor      = new HostedHypervisor();
		PSU                   PSU                   = new PSU();
		
		//same frequencies and powers for every CPU and cores.
		double CPUfrequency = genRandomDouble(MIN_FREQUENCY,      MAX_FREQUENCY,      random);
		double CPUpowerIdle = genRandomDouble(MIN_CPU_POWER_IDLE, MAX_CPU_POWER_IDLE, random);
		double CPUpowerMax  = genRandomDouble(MIN_CPU_POWER_IDLE, MAX_CPU_POWER_IDLE, random) + CPUpowerIdle;
		
		//number of cores (even number)
		int nbCorePerCPU = genRandomInteger(MIN_CORE, MAX_CORE, random);
		int nbCPU = genRandomInteger(MIN_CPU, MAX_CPU, random);
		
		for (int i=0; i < nbCPU; i++ ){
			CPU CPU = new CPU();
			CPU.setPowerIdle(new Power(CPUpowerIdle));
			CPU.setPowerMax(new Power(CPUpowerMax));
			CPU.setArchitecture(CPUArchitecture.AMD);
			CPU.setTransistorNumber(new NrOfTransistors(NUMBER_OF_TRANSISTORS));
			CPU.setDVFS(true);
			
			
			for (int j=0; j< nbCorePerCPU; j++ ){
				Core core = new Core();
				core.setFrequency(new Frequency(CPUfrequency));
				//core.setFrequencyMin(new Frequency(MIN_FREQUENCY));
				//core.setFrequencyMax(new Frequency(MAX_FREQUENCY));
				core.setCoreLoad(new CoreLoad(0.1));
				core.setVoltage(new Voltage(CPU_VOLTAGE));
				core.setLastPstate(new NrOfPstates(0));
				core.setTotalPstates(new NrOfPstates(0));
				CPU.getCore().add(core);
				
			}
			mainboard.getCPU().add(CPU);
		}
		
		NIC           .setProcessingBandwidth(new Bandwidth(genRandomDouble( MIN_NIC_BANDWITCH,          MAX_NIC_BANDWITCH, random)));
		NIC           .setPowerIdle(          new Power(genRandomDouble( MIN_NIC_POWER_IDLE,         MAX_NIC_POWER_IDLE, random)));
		NIC           .setPowerMax(           new Power(genRandomDouble( MIN_NIC_POWER_DELTA_MAX,    MAX_NIC_POWER_DELTA_MAX, random) + NIC.getPowerIdle().getValue()));
		RAMStick      .setSize(               new RAMSize(genRandomInteger(MIN_RAM_SIZE,               MAX_RAM_SIZE, random)));
		hardDisk      .setStorageCapacity(    new StorageCapacity(genRandomInteger(MIN_STORAGE_SIZE,           MAX_STORAGE_SIZE, random)));
		//coolingSystem .setNumberOfFans(       genRandomInteger(MIN_FAN,                    MAX_FAN, random));
		mainboard.setPowerIdle(               new Power(genRandomDouble( MIN_SERVER_POWER_IDLE,      MAX_SERVER_POWER_IDLE, random)));
		mainboard.setPowerMax(                new Power(genRandomDouble( MIN_SERVER_POWER_DELTA_MAX, MAX_SERVER_POWER_DELTA_MAX, random) + mainboard.getPowerIdle().getValue()));
		
		//PSU.setCertification(PSUCertificationType.CERT_1);
		
		hardDisk.setMaxReadRate(new IoRate(0));
		hardDisk.setMaxWriteRate(new IoRate(0));
		hardDisk.setReadRate(new IoRate(0));
		hardDisk.setWriteRate(new IoRate(0));
		hardDisk.setPowerIdle(new Power(0));
		hardDisk.setPowerMax(new Power(0));
		hardDisk.setRpm(new RPM(0));
		
		NetworkPort netPort = new NetworkPort();
		netPort.setPowerIdle(new Power(1.0));
		netPort.setPowerMax(new Power(2.0));
		netPort.setLineCapacity(new NetworkTraffic(1.0));
		netPort.setId("id" + String.valueOf(NIC_FRAMEWORK_ID + id + 10));
		netPort.setPortID("PortID");
		
		NIC.setFrameworkRef(frameworkCapabilitie);
		//NIC.setSwitchingType(NNSwitchingType.TYPE_1);
		NIC.setFrameworkID("id" + String.valueOf(NIC_FRAMEWORK_ID + id));
		NIC.setID("id" + String.valueOf(NIC_FRAMEWORK_ID + id));
		NIC.setForwardFlag(false);
		NIC.getNetworkPort().add(netPort);
		NIC.setStatus(NetworkNodeStatus.ON);
		RAMStick.setType(RAMTypeType.DDR);
		RAMStick.setVendor(RAMTypeVendorType.GENERIC);
		RAMStick.setVoltage(new Voltage(0));
		RAMStick.setFrequency(new Frequency(0));
		RAMStick.setBufferType(BufferTypeType.FULLY_BUFFERED);
		
		mainboard.getRAMStick().add(RAMStick);
		mainboard.getHardDisk().add(hardDisk);
		mainboard.getEthernetNIC().add(NIC);
		mainboard.setFrameworkID("id" + String.valueOf(MAINBOARD_FRAMEWORK_ID + id));
	
		hostedHypervisor.setHypervisorName(HostedHypervisorName.VM_WARE);
		hostedHypervisor.setFrameworkID("id" + String.valueOf(HYPERVISOR_FRAMEWORK_ID + id));
		
		nativeOperatingSystem.setName(OperatingSystemType.LINUX);
		nativeOperatingSystem.getHostedHypervisor().add(hostedHypervisor);
		
		PSU.setLoad(new PSULoad(50));
		PSU.setEfficiency(new Efficiency(50));
		
		//rackableServer.setPSU(PSU);
		rackableServer.setFrameworkRef((Object)frameworkCapabilitie);
		rackableServer.setName(ServerRole.CLOUD_NODE_CONTROLLER);
		rackableServer.setStatus(ServerStatus.ON);
		rackableServer.setFrameworkID("id" + String.valueOf(id));
		rackableServer.setNativeOperatingSystem(nativeOperatingSystem);
		//rackableServer.setCoolingSystem(coolingSystem);
		rackableServer.getMainboard().add(mainboard);
		rackableServer.setName(ServerRole.CLOUD_NODE_CONTROLLER);
		rackableServer.getPSU().add(PSU);

		int max_nb_VM = Math.min(nbCorePerCPU*nbCPU, MAX_NB_VIRTUAL_MACHINES);
		int min_nb_VM = Math.min(MIN_NB_VIRTUAL_MACHINES, max_nb_VM);
		int nb_VM = genRandomInteger(min_nb_VM, max_nb_VM, random);
		//int nb_VM = genRandomInteger(MIN_NB_VIRTUAL_MACHINES, MAX_NB_VIRTUAL_MACHINES, random);
		
		//adding a number of VMs (max one VM per core)
		for(int i=0; i<nb_VM; i++){
			VirtualMachine VM = createVirtualMachine(rackableServer, frameworkCapabilitie, id + VM_FRAMEWORK_ID + i);
			if(VM != null){
				hostedHypervisor.getVirtualMachine().add(VM);
			} else {
				break;
			}								
		} 
							
		return rackableServer;
		
	}
	
	/**
	 * create a VM
	 *
	 * @author cdupont
	 */
	public VirtualMachine createVirtualMachine(Server runningOn, FrameworkCapabilities frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		VirtualMachine virtualMachine = new VirtualMachine();
		HostedOperatingSystem hostedOperatingSystem = new HostedOperatingSystem();
		hostedOperatingSystem.setName(OperatingSystemType.LINUX);
				
		if(! IS_CLOUD) {
			
			AggregatedUsage aggregatedUsageOnServer = AggregatedUsage.getAggregatedUsage(runningOn.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine());
			
			//compute the remainings on the server
			//double remainingCPU = 1.0 - aggregatedUsageOnServer.aggregatedCPUUsage;
			double remainingNetwork = Utils.getNetworkBandwich(runningOn) - aggregatedUsageOnServer.aggregatedNetworkUsage;
			double remainingStorage = Utils.getStorage(runningOn) - aggregatedUsageOnServer.aggregatedStorageUsage;
			double remainingMemory  = Utils.getMemory(runningOn)  - aggregatedUsageOnServer.aggregatedMemoryUsage;
			int    remainingNbCPU   = (int)Utils.getNbCores(runningOn) - (int)aggregatedUsageOnServer.aggregatedNbCores;
									
			//compute the maxs for the WL
			int    max_nb_cpu        = Math.min(remainingNbCPU,   MAX_NB_CPU);  
			//double max_cpu_usage     = Math.min(remainingCPU,     MAX_CPU_USAGE);  
			double max_network_usage = Math.min(remainingNetwork, MAX_NETWORK_USAGE);
			double max_storage_usage = Math.min(remainingStorage, MAX_STORAGE_USAGE);
			double max_memory_usage  = Math.min(remainingMemory,  MAX_MEMORY_USAGE);
			
			
			if(MIN_NB_CPU        > max_nb_cpu)        { log.debug("No more Cores.");       return null; }
			//if(MIN_CPU_USAGE     > max_cpu_usage)     { log.debug("CPU usage exhausted."); return null; }
			if(MIN_NETWORK_USAGE > max_network_usage) { log.debug("network exhausted.");   return null; }
			if(MIN_STORAGE_USAGE > max_storage_usage) { log.debug("storage exhausted.");   return null; }
			if(MIN_MEMORY_USAGE  > max_memory_usage)  { log.debug("memory exhausted.");	   return null; }

        
			virtualMachine.setNumberOfCPUs(          new NrOfCpus(genRandomInteger(MIN_NB_CPU,        max_nb_cpu,        random)));
			virtualMachine.setActualCPUUsage(        new CpuUsage(genRandomDouble( MIN_CPU_USAGE,     MAX_CPU_USAGE,     random)));
			virtualMachine.setActualNetworkUsage(    new NetworkUsage(genRandomDouble( MIN_NETWORK_USAGE, max_network_usage, random)));  //TODO correct types
			virtualMachine.setActualStorageUsage(    new StorageUsage(genRandomDouble( MIN_STORAGE_USAGE, max_storage_usage, random)));
			virtualMachine.setActualMemoryUsage(     new MemoryUsage(genRandomDouble( MIN_MEMORY_USAGE,  max_memory_usage,  random)));
			virtualMachine.setActualDiskIORate(      new IoRate(0.0)); //TODO correct 
		
		} else {
			
			virtualMachine.setCloudVm(VM_TYPE);
			
			virtualMachine.setNumberOfCPUs(       null);    
			virtualMachine.setActualCPUUsage(     null);   
			virtualMachine.setActualNetworkUsage( null);
			virtualMachine.setActualStorageUsage( null);   
			virtualMachine.setActualMemoryUsage(  null);   
			virtualMachine.setActualDiskIORate(   null);
			
		}
					
		virtualMachine.setName("virtualMachine");
		virtualMachine.setFrameworkRef((Object)frameworkCapabilitie);
		virtualMachine.setFrameworkID("id" + String.valueOf(id));
		
		virtualMachine.setHostedOperatingSystem(hostedOperatingSystem);
		virtualMachine.setName("none");

		
		return virtualMachine;
	}
	
	
	/**
	 * add servers to an existing model
	 *
	 * @author cdupont
	 */
	public FIT4Green addRandomServersToModel(FIT4Green FIT4Green){
		
		FrameworkCapabilities frameworkCapability = FIT4Green.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0);
		
		for(int i=0; i<MAX_NB_SERVERS; i++)
			FIT4Green.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer().add( createRandomServer(frameworkCapability, i));
		
		return FIT4Green;
	}

	


	/**
	 * compute a random int value uniformly distributed between aStart (inclusive) and aEnd (inclusive)
	 *
	 * @author cdupont
	 */
	public int genRandomInteger(int aStart, int aEnd, Random aRandom)
	{
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range
	    int range = aEnd - aStart + 1;
	    // compute a fraction of the range, 0 <= frac < range
	    int fraction = aRandom.nextInt(range);

	    return (aStart + fraction);    
	}

	
	/**
	 * compute a random double value uniformly distributed between aStart (inclusive) and aEnd (exclusive)
	 *
	 * @author cdupont
	 */
	public double genRandomDouble(double aStart, double aEnd, Random aRandom)
	{
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range, casting to long to avoid overflow problems
	    double range = aEnd - aStart;
	    // compute a fraction of the range, 0 <= frac < range
	    double fraction = range * aRandom.nextDouble();
 
	    return fraction + aStart; 
	}
	

	/**
	 * load a model from file, populate it and then save it.
	 *
	 * @author cdupont
	 */
	public void populateModel(String fromModelFile, String toModelFile) {
		
		FIT4Green model = getModel(fromModelFile);
		FIT4Green populatedF4G = addRandomServersToModel(model);
		
	}
	
	/**
	 * create a populated model and save it.
	 *
	 * @author cdupont
	 */
	public void createPopulatedModel(String toModelFile) {
		

		FIT4Green populatedF4G = createPopulatedFIT4Green();
		
		
//		if(validate(populatedF4G)){
//			log.debug("Saving populated model under: " + toModelFile);
//			(new Recorder()).recordModel(populatedF4G); 
//		} else {
//			log.debug("populated model could not be validated");
//		}
		
	}

	/**
	 * validate a model against a schema. 
	 *
	 * @author cdupont
	 */
	public boolean validate(FIT4Green FIT4Green){
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    
		try {
			Schema schema = schemaFactory.newSchema(new File(schema_location));
			Marshaller marshaller = Util.getJaxbContext().createMarshaller();
		    marshaller.setSchema(schema);
		    JAXBElement<FIT4Green> element = (new ObjectFactory()).createFIT4Green(FIT4Green);
	   		marshaller.marshal (element, new DefaultHandler());
			return true;
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("JAXBException Message: " + e.toString());
			log.error("JAXBException getLocalizedMessage: " + e.getLocalizedMessage());
			return false;
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			log.error("SAXException getMessage: " + e1.getMessage());
			log.error("SAXException getLocalizedMessage: " + e1.getLocalizedMessage());
			return false;
		} 
	    
	}
	
	/**
	 * load a model. 
	 *
	 * @author cdupont
	 */
	public FIT4Green getModel(String modelPathName) {
		
		InputStream isModel = null;
		if((new File(modelPathName)).exists()) {
			try {
				isModel = new FileInputStream(modelPathName);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			isModel = this.getClass().getClassLoader().getResourceAsStream(modelPathName);
		}
			
		log.debug("modelPathName: " + modelPathName + ", isModel: " + isModel);
		
		JAXBElement<?> poElement = null;
		try {
			// create an Unmarshaller
			Unmarshaller u = Util.getJaxbContext().createUnmarshaller();

			// ****** VALIDATION ******
			SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			try {
				Schema schema = sf.newSchema(new File(schema_location));
				u.setSchema(schema);
				u.setEventHandler(new ValidationEventHandler() {
					// allow unmarshalling to continue even if there are errors
					public boolean handleEvent(ValidationEvent ve) {
						// ignore warnings
						if (ve.getSeverity() != ValidationEvent.WARNING) {
							ValidationEventLocator vel = ve.getLocator();
							log.warn("Line:Col["
									+ vel.getLineNumber() + ":"
									+ vel.getColumnNumber() + "]:"
									+ ve.getMessage());
						}
						return true;
					}
				});
			} catch (org.xml.sax.SAXException se) {
				log.error("Unable to validate due to following error: ", se );
			}
			// *********************************

			// unmarshal an XML document into a tree of Java content
			// objects composed of classes from the "org.f4g.schema" package.
			poElement = (JAXBElement<?>) u.unmarshal(isModel);
			
			return (FIT4Green) poElement.getValue();
			

		} catch (JAXBException je) {
			log.error(je);
			return null;
		}
		
	}

    // =====================================================================================================
    // Network stuff
    // rlent
    
	/**
	 * create a network switch
	 *
	 * @author rlent
	 */
	public BoxSwitch createRandomBoxSwitch(FrameworkCapabilities frameworkCapabilitie, int id) {
		
		Random random = new Random();
		
		BoxSwitch node = new BoxSwitch();
		node.setFrameworkID("id" + String.valueOf(id));
		node.setID("id" + String.valueOf(id));
		node.setForwardFlag(true);
		node.setStatus(NetworkNodeStatus.ON);		
        node.setPowerIdle( defaultSwitchPowerIdle  );
        node.setPowerMax( defaultSwitchPowerMax );
        node.setProcessingBandwidth( defaultSwitchProcessingBandwidth );

		return node;
	}
	
	/**
	 * create a network router
	 *
	 * @author rlent
	 */
	public BoxRouter createRandomBoxRouter(FrameworkCapabilities frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		BoxRouter node = new BoxRouter();
		node.setFrameworkID("id" + String.valueOf(id));
		node.setID("id" + String.valueOf(id));
		node.setForwardFlag(true);
		node.setStatus(NetworkNodeStatus.ON);				
        node.setPowerIdle( defaultRouterPowerIdle );
        node.setPowerMax( defaultRouterPowerMax );
        node.setProcessingBandwidth( defaultRouterProcessingBandwidth );

		return node;
		
	}
	
    
    /**
     * Connect two NetworkNode using defaul values
	 *
	 * @author rlent
     */
    public static void connectNetDevsFullDuplex(NetworkNode node0, NetworkNode node1) 
    {
        // 1. Create a link to connect the nodes
        
        Link link = new Link(defaultPropagationDelay, defaultBitErrorRate, null);
        
        // 2. Create a network port for node 0 
        
        NetworkPort port0 = new NetworkPort();      
        port0.setLineCapacity( defaultLineCapacity );
        port0.setPowerIdle( defaultPortPowerIdle );
        port0.setPowerMax( defaultPortPowerMax );
        port0.setLink( link );
        
        // 3. Create a network port for node 1 
        
        NetworkPort port1 = new NetworkPort();
        port1.setLineCapacity( defaultLineCapacity );
        port1.setPowerIdle( defaultPortPowerIdle );
        port1.setPowerMax( defaultPortPowerMax );
        port1.setLink( link );
        
        // 4. Reference network ports to each other 
        
        port0.setNetworkPortRef( node1.getFrameworkID() );
        port1.setNetworkPortRef( node0.getFrameworkID() );
        
        // 5. Attach ports to each of node
        
        node0.getNetworkPort().add( port0 );
        node1.getNetworkPort().add( port1 );        
    }


    /**
     * Connect a Server to a NetworkNode using defaul values
	 *
	 * @author rlent
     */
    public static void connectServerToNetDevFullDuplex(Server server, NetworkNode node) 
    {

//        for (int i = 0; i < allServers.size(); i++) {
//        	Server server = allServers.get(i);
//            
//            System.out.println("-> " + server.getFrameworkID() );
//            
//            Mainboard amainboard = server.getMainboard().get(0); 
//            NIC anic = amainboard.getEthernetNIC().get(0);
//            
//        }

        // 1. Create a link to connect the nodes
        
        Link link = new Link(defaultPropagationDelay, defaultBitErrorRate, null);
        
        // 2. Create a network port for server 
        
        NetworkNode nic0 = server.getMainboard().get(0).getEthernetNIC().get(0);    // first server's NIC is a NetworkNode
        NetworkPort port0 = nic0.getNetworkPort().get(0);      
//        NetworkPort port0 = new NetworkPort();      
        port0.setLineCapacity( defaultLineCapacity );
        port0.setPowerIdle( defaultPortPowerIdle );
        port0.setPowerMax( defaultPortPowerMax );
        port0.setLink( link );
        
        // 3. Create a network port for node 
        
        NetworkPort port1 = new NetworkPort();
        port1.setLineCapacity( defaultLineCapacity );
        port1.setPowerIdle( defaultPortPowerIdle );
        port1.setPowerMax( defaultPortPowerMax );
        port1.setLink( link );
        
        // 4. Reference network ports to each other 
        
        port0.setNetworkPortRef( node.getFrameworkID() );
        port1.setNetworkPortRef( server.getFrameworkID() );
        
        // 5. Attach ports to each of node
        
        // server.getMainboard().get(0).getEthernetNIC().get(0).getNetworkPort().add( port0 );
        node.getNetworkPort().add( port1 );   
        
        // 6. NICs' share Server's ID
        
         nic0.setFrameworkID(server.getFrameworkID());
    }

	public void setNB_SWITCHES(int v) {
        NB_SWITCHES = v;
    }
	public void setNB_ROUTERS(int v) {
        NB_ROUTERS = v;
    }
	
}



