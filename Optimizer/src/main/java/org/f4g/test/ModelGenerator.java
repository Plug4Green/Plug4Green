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
*   You can run the local main, with argument "config/f4gconfig.properties". A new file f4gmodel_instance_populated.xml
*   will be created in resources directory.
*   This file can then be used for test purpose.   
* ============================= /Header ==============================
*/

package org.f4g.test;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.lang.Math;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.f4g.schema.metamodel.*;
import org.f4g.optimizer.utils.AggregatedUsage;
import org.f4g.optimizer.utils.Utils;

import org.f4g.util.Util;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.f4g.optimizer.utils.Utils;

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
    
    public static BitErrorRateType defaultBitErrorRate = new BitErrorRateType(0.0);          // default values for a typical Ethernet link
    public static PropagationDelayType defaultPropagationDelay = new PropagationDelayType(0.0);
    public static NetworkTrafficType defaultLineCapacity = new NetworkTrafficType(8.0);      //  1 Byte/sec
    public static PowerType defaultPortPowerIdle = new PowerType(  0 );
    public static PowerType defaultPortPowerMax = new PowerType( 1.0 );
    public static NetworkPortBufferSizeType defaultBufferSize = new NetworkPortBufferSizeType(50);
    public static PowerType defaultSwitchPowerIdle = new PowerType( 48.0 );             // small Cisco switch
    public static PowerType defaultSwitchPowerMax = new PowerType( 50.0 );
    public static PowerType defaultRouterPowerIdle = new PowerType( 12.0 );             // small Cisco router
    public static PowerType defaultRouterPowerMax = new PowerType( 13.0 );
    public static BandwidthType defaultRouterProcessingBandwidth = new BandwidthType(1000000000);
    public static BandwidthType defaultSwitchProcessingBandwidth = new BandwidthType(1000000000);
    



	
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
			System.out.println("Please provide the path of config file as an argument (usually config/f4gconfig.properties).");
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
	public FIT4GreenType createFIT4GreenType(){
		
			
		FIT4GreenType fIT4Green = new FIT4GreenType();
		try {
			fIT4Green.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2012, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 2 ));
		} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
		}
		SiteType site = new SiteType();
				
		site.setComputedPower(new PowerType(0.0));
		site.setCUE(new CUEType(10.0));
		site.setPUE(new PUEType(1.0));
				
		fIT4Green.getSite().add(site);
		return fIT4Green;
		
	}
	
	/**
	 * create a F4G type (everything but servers)
	 *
	 * @author cdupont
	 */
	public FIT4GreenType createFIT4GreenType2Sites(){
		
			
		FIT4GreenType fIT4Green = new FIT4GreenType();
		try {
			fIT4Green.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(/*year*/2012, /*month*/ 04, /*day*/ 04, /*hour*/ 8,  /*minute*/ 00, /*second*/ 00, /*millisecond*/ 00, /*timezone*/ 2 ));
		} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
		}
		SiteType site1 = new SiteType();
				
		site1.setComputedPower(new PowerType(0.0));
		site1.setCUE(new CUEType(30.0));
		site1.setPUE(new PUEType(1.0));
				
		fIT4Green.getSite().add(site1);
	
		SiteType site2 = new SiteType();
		
		site2.setComputedPower(new PowerType(0.0));
		site2.setCUE(new CUEType(12.0));
		site2.setPUE(new PUEType(2.0));
				
		fIT4Green.getSite().add(site2);
		
		return fIT4Green;
		
	}
	
	/**
	 * create a datacenter
	 *
	 * @author cdupont
	 */
	public DatacenterType createDatacenterType(String FWName){
			
		DatacenterType datacenter = new DatacenterType();
		RackType rack = new RackType();
		NetworkLoadType networkLoad = new NetworkLoadType();
		VMActionsType vMActions = new VMActionsType();
		
		vMActions.setIntraMoveVM(true);
		vMActions.setInterMoveVM(false);
		vMActions.setIntraLiveMigrate(false);
		vMActions.setInterLiveMigrate(false);
		
		NodeActionsType nodeActions = new NodeActionsType();
		nodeActions.setPowerOff(true);
		nodeActions.setPowerOn(true);		
		
		FrameworkCapabilitiesType frameworkCapability = new FrameworkCapabilitiesType();
		frameworkCapability.setFrameworkName(FWName);
		frameworkCapability.setId(FWName);
		frameworkCapability.setVm(vMActions);
		frameworkCapability.setNode(nodeActions);
		frameworkCapability.setStatus(FrameworkStatusType.RUNNING);
		
		datacenter.setComputingStyle(DCComputingStyleType.CLOUD);
		datacenter.setComputedPower(new PowerType(0.0));
		datacenter.setNetworkLoad(networkLoad);
	
		rack.setComputedPower(new PowerType(0.0));
		
		datacenter.getFrameworkCapabilities().add(frameworkCapability);
		datacenter.getRack().add(rack);

		return datacenter;
		
	}

	/**
	 * create a F4G type with servers and VMs
	 *
	 * @author cdupont
	 */
	public FIT4GreenType createPopulatedFIT4GreenType() {
		
		FIT4GreenType fIT4Green = createFIT4GreenType();
		DatacenterType DC1 = createDatacenterType("DC1");
		fIT4Green.getSite().get(0).getDatacenter().add(DC1);


//		
//		for(int i=0; i<MAX_NB_SERVERS; i++)
//			DC1.getRack().get(0).getRackableServer()
//				.add( createRandomServer(DC1.getFrameworkCapabilities().get(0), SERVER_FRAMEWORK_ID * i));


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
	public FIT4GreenType createPopulatedFIT4GreenType2DC() {
		
		FIT4GreenType fIT4Green = createFIT4GreenType();
		DatacenterType DC1 = createDatacenterType("DC1");
		DatacenterType DC2 = createDatacenterType("DC2");
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
	public FIT4GreenType createPopulatedFIT4GreenType2Sites() {
		
		FIT4GreenType fIT4Green = createFIT4GreenType2Sites();
		DatacenterType DC1 = createDatacenterType("DC1");
		DatacenterType DC2 = createDatacenterType("DC2");
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
	public RackableServerType createRandomServer(FrameworkCapabilitiesType frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		RackableServerType        rackableServer        = new RackableServerType();
		CoolingSystemType         coolingSystem         = new CoolingSystemType();
		NativeOperatingSystemType nativeOperatingSystem = new NativeOperatingSystemType();
		MainboardType             mainboard             = new MainboardType();
		RAMStickType              RAMStick              = new RAMStickType();
		HardDiskType              hardDisk              = new HardDiskType();
		NICType                   NIC                   = new NICType();
		HostedHypervisorType      hostedHypervisor      = new HostedHypervisorType();
		PSUType                   PSU                   = new PSUType();
		
		//same frequencies and powers for every CPU and cores.
		double CPUfrequency = genRandomDouble(MIN_FREQUENCY,      MAX_FREQUENCY,      random);
		double CPUpowerIdle = genRandomDouble(MIN_CPU_POWER_IDLE, MAX_CPU_POWER_IDLE, random);
		double CPUpowerMax  = genRandomDouble(MIN_CPU_POWER_IDLE, MAX_CPU_POWER_IDLE, random) + CPUpowerIdle;
		
		//number of cores (even number)
		int nbCorePerCPU = genRandomInteger(MIN_CORE, MAX_CORE, random);
		int nbCPU = genRandomInteger(MIN_CPU, MAX_CPU, random);
		
		for (int i=0; i < nbCPU; i++ ){
			CPUType CPU = new CPUType();
			CPU.setPowerIdle(new PowerType(CPUpowerIdle));
			CPU.setPowerMax(new PowerType(CPUpowerMax));
			CPU.setArchitecture(CPUArchitectureType.AMD);
			CPU.setTransistorNumber(new NrOfTransistorsType(NUMBER_OF_TRANSISTORS));
			CPU.setDVFS(true);
			
			for (int j=0; j< nbCorePerCPU; j++ ){
				CoreType core = new CoreType();
				core.setFrequency(new FrequencyType(CPUfrequency));
				core.setCoreLoad(new CoreLoadType(0.1));
				core.setVoltage(new VoltageType(1.0));
				core.setLastPstate(new NrOfPstatesType(0));
				core.setTotalPstates(new NrOfPstatesType(0));
				CPU.getCore().add(core);
				
			}
			mainboard.getCPU().add(CPU);
		}
		
		NIC           .setProcessingBandwidth(new BandwidthType(genRandomDouble( MIN_NIC_BANDWITCH,          MAX_NIC_BANDWITCH, random)));
		NIC           .setPowerIdle(          new PowerType(genRandomDouble( MIN_NIC_POWER_IDLE,         MAX_NIC_POWER_IDLE, random)));
		NIC           .setPowerMax(           new PowerType(genRandomDouble( MIN_NIC_POWER_DELTA_MAX,    MAX_NIC_POWER_DELTA_MAX, random) + NIC.getPowerIdle().getValue()));
		RAMStick      .setSize(               new RAMSizeType(genRandomInteger(MIN_RAM_SIZE,               MAX_RAM_SIZE, random)));
		hardDisk      .setStorageCapacity(    new StorageCapacityType(genRandomInteger(MIN_STORAGE_SIZE,           MAX_STORAGE_SIZE, random)));
		//coolingSystem .setNumberOfFans(       genRandomInteger(MIN_FAN,                    MAX_FAN, random));
		mainboard.setPowerIdle(               new PowerType(genRandomDouble( MIN_SERVER_POWER_IDLE,      MAX_SERVER_POWER_IDLE, random)));
		mainboard.setPowerMax(                new PowerType(genRandomDouble( MIN_SERVER_POWER_DELTA_MAX, MAX_SERVER_POWER_DELTA_MAX, random) + mainboard.getPowerIdle().getValue()));
		
		//PSU.setCertification(PSUCertificationType.CERT_1);
		
		hardDisk.setMaxReadRate(new IoRateType(0));
		hardDisk.setMaxWriteRate(new IoRateType(0));
		hardDisk.setReadRate(new IoRateType(0));
		hardDisk.setWriteRate(new IoRateType(0));
		hardDisk.setPowerIdle(new PowerType(0));
		hardDisk.setPowerMax(new PowerType(0));
		hardDisk.setRpm(new RPMType(0));
		
		NetworkPortType netPort = new NetworkPortType();
		netPort.setPowerIdle(new PowerType(1.0));
		netPort.setPowerMax(new PowerType(2.0));
		netPort.setLineCapacity(new NetworkTrafficType(1.0));
		netPort.setId("id" + String.valueOf(NIC_FRAMEWORK_ID + id + 10));
		netPort.setPortID("PortID");
		
		NIC.setFrameworkRef(frameworkCapabilitie);
		//NIC.setSwitchingType(NNSwitchingType.TYPE_1);
		NIC.setFrameworkID("id" + String.valueOf(NIC_FRAMEWORK_ID + id));
		NIC.setID("id" + String.valueOf(NIC_FRAMEWORK_ID + id));
		NIC.setForwardFlag(false);
		NIC.getNetworkPort().add(netPort);
		NIC.setStatus(NetworkNodeStatusType.ON);
		RAMStick.setType(RAMTypeType.DDR);
		RAMStick.setVendor(RAMTypeVendorType.GENERIC);
		RAMStick.setVoltage(new VoltageType(0));
		RAMStick.setFrequency(new FrequencyType(0));
		RAMStick.setBufferType(BufferTypeType.FULLY_BUFFERED);
		
		mainboard.getRAMStick().add(RAMStick);
		mainboard.getHardDisk().add(hardDisk);
		mainboard.getEthernetNIC().add(NIC);
		mainboard.setFrameworkID("id" + String.valueOf(MAINBOARD_FRAMEWORK_ID + id));
	
		hostedHypervisor.setHypervisorName(HostedHypervisorNameType.VM_WARE);
		hostedHypervisor.setFrameworkID("id" + String.valueOf(HYPERVISOR_FRAMEWORK_ID + id));
		
		nativeOperatingSystem.setName(OperatingSystemTypeType.LINUX);
		nativeOperatingSystem.getHostedHypervisor().add(hostedHypervisor);
		
		PSU.setLoad(new PSULoadType(50));
		PSU.setEfficiency(new EfficiencyType(50));
		
		//rackableServer.setPSU(PSU);
		rackableServer.setFrameworkRef((Object)frameworkCapabilitie);
		rackableServer.setName(ServerRoleType.CLOUD_NODE_CONTROLLER);
		rackableServer.setStatus(ServerStatusType.ON);
		rackableServer.setFrameworkID("id" + String.valueOf(id));
		rackableServer.setNativeOperatingSystem(nativeOperatingSystem);
		//rackableServer.setCoolingSystem(coolingSystem);
		rackableServer.getMainboard().add(mainboard);
		rackableServer.setName(ServerRoleType.CLOUD_NODE_CONTROLLER);
		rackableServer.getPSU().add(PSU);

		int max_nb_VM = Math.min(nbCorePerCPU*nbCPU, MAX_NB_VIRTUAL_MACHINES);
		int min_nb_VM = Math.min(MIN_NB_VIRTUAL_MACHINES, max_nb_VM);
		int nb_VM = genRandomInteger(min_nb_VM, max_nb_VM, random);
		//int nb_VM = genRandomInteger(MIN_NB_VIRTUAL_MACHINES, MAX_NB_VIRTUAL_MACHINES, random);
		
		//adding a number of VMs (max one VM per core)
		for(int i=0; i<nb_VM; i++){
			VirtualMachineType VM = createVirtualMachineType(rackableServer, frameworkCapabilitie, id + VM_FRAMEWORK_ID + i);
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
	public VirtualMachineType createVirtualMachineType(ServerType runningOn, FrameworkCapabilitiesType frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		VirtualMachineType virtualMachine = new VirtualMachineType();
		HostedOperatingSystemType hostedOperatingSystem = new HostedOperatingSystemType();
		hostedOperatingSystem.setName(OperatingSystemTypeType.LINUX);
				
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

        
			virtualMachine.setNumberOfCPUs(          new NrOfCpusType(genRandomInteger(MIN_NB_CPU,        max_nb_cpu,        random)));
			virtualMachine.setActualCPUUsage(        new CpuUsageType(genRandomDouble( MIN_CPU_USAGE,     MAX_CPU_USAGE,     random)));
			virtualMachine.setActualNetworkUsage(    new NetworkUsageType(genRandomDouble( MIN_NETWORK_USAGE, max_network_usage, random)));  //TODO correct types
			virtualMachine.setActualStorageUsage(    new StorageUsageType(genRandomDouble( MIN_STORAGE_USAGE, max_storage_usage, random)));
			virtualMachine.setActualMemoryUsage(     new MemoryUsageType(genRandomDouble( MIN_MEMORY_USAGE,  max_memory_usage,  random)));
			virtualMachine.setActualDiskIORate(      new IoRateType(0.0)); //TODO correct 
		
		} else {
			
			virtualMachine.setCloudVmType(VM_TYPE);
			
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
	public FIT4GreenType addRandomServersToModel(FIT4GreenType FIT4Green){
		
		FrameworkCapabilitiesType frameworkCapability = FIT4Green.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0);
		
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
		
		FIT4GreenType model = getModel(fromModelFile);
		FIT4GreenType populatedF4G = addRandomServersToModel(model);
		
	}
	
	/**
	 * create a populated model and save it.
	 *
	 * @author cdupont
	 */
	public void createPopulatedModel(String toModelFile) {
		

		FIT4GreenType populatedF4G = createPopulatedFIT4GreenType();
		
		
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
	public boolean validate(FIT4GreenType FIT4Green){
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    
		try {
			Schema schema = schemaFactory.newSchema(new File("../Schemas/src/main/schema/MetaModel.xsd"));
			Marshaller marshaller = Util.getJaxbContext().createMarshaller();
		    marshaller.setSchema(schema);
		    JAXBElement<FIT4GreenType> element = (new ObjectFactory()).createFIT4Green(FIT4Green);
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
	public FIT4GreenType getModel(String modelPathName) {
		
		InputStream isModel = null;
		try {
			isModel = new FileInputStream(modelPathName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("modelPathName: " + modelPathName + ", isModel: " + isModel);
		
		JAXBElement<?> poElement = null;
		try {
			// create an Unmarshaller
			Unmarshaller u = Util.getJaxbContext().createUnmarshaller();

			// ****** VALIDATION ******
			SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			try {
				Schema schema = sf.newSchema(new File("../FIT4Green/Schemas/src/main/schema/MetaModel.xsd"));
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
			
			return (FIT4GreenType) poElement.getValue();
			

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
	public BoxSwitchType createRandomBoxSwitch(FrameworkCapabilitiesType frameworkCapabilitie, int id) {
		
		Random random = new Random();
		
		BoxSwitchType node = new BoxSwitchType();
		node.setFrameworkID("id" + String.valueOf(id));
		node.setID("id" + String.valueOf(id));
		node.setForwardFlag(true);
		node.setStatus(NetworkNodeStatusType.ON);		
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
	public BoxRouterType createRandomBoxRouter(FrameworkCapabilitiesType frameworkCapabilitie, int id){
		
		Random random = new Random();
		
		BoxRouterType node = new BoxRouterType();
		node.setFrameworkID("id" + String.valueOf(id));
		node.setID("id" + String.valueOf(id));
		node.setForwardFlag(true);
		node.setStatus(NetworkNodeStatusType.ON);				
        node.setPowerIdle( defaultRouterPowerIdle );
        node.setPowerMax( defaultRouterPowerMax );
        node.setProcessingBandwidth( defaultRouterProcessingBandwidth );

		return node;
		
	}
	
    
    /**
     * Connect two NetworkNodeType using defaul values
	 *
	 * @author rlent
     */
    public static void connectNetDevsFullDuplex(NetworkNodeType node0, NetworkNodeType node1) 
    {
        // 1. Create a link to connect the nodes
        
        LinkType link = new LinkType(defaultPropagationDelay, defaultBitErrorRate);
        
        // 2. Create a network port for node 0 
        
        NetworkPortType port0 = new NetworkPortType();      
        port0.setLineCapacity( defaultLineCapacity );
        port0.setPowerIdle( defaultPortPowerIdle );
        port0.setPowerMax( defaultPortPowerMax );
        port0.setLink( link );
        
        // 3. Create a network port for node 1 
        
        NetworkPortType port1 = new NetworkPortType();
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
     * Connect a ServerType to a NetworkNodeType using defaul values
	 *
	 * @author rlent
     */
    public static void connectServerToNetDevFullDuplex(ServerType server, NetworkNodeType node) 
    {

//        for (int i = 0; i < allServers.size(); i++) {
//        	ServerType server = allServers.get(i);
//            
//            System.out.println("-> " + server.getFrameworkID() );
//            
//            MainboardType amainboard = server.getMainboard().get(0); 
//            NICType anic = amainboard.getEthernetNIC().get(0);
//            
//        }

        // 1. Create a link to connect the nodes
        
        LinkType link = new LinkType(defaultPropagationDelay, defaultBitErrorRate);
        
        // 2. Create a network port for server 
        
        NetworkNodeType nic0 = server.getMainboard().get(0).getEthernetNIC().get(0);    // first server's NIC is a NetworkNode
        NetworkPortType port0 = nic0.getNetworkPort().get(0);      
//        NetworkPortType port0 = new NetworkPortType();      
        port0.setLineCapacity( defaultLineCapacity );
        port0.setPowerIdle( defaultPortPowerIdle );
        port0.setPowerMax( defaultPortPowerMax );
        port0.setLink( link );
        
        // 3. Create a network port for node 
        
        NetworkPortType port1 = new NetworkPortType();
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



