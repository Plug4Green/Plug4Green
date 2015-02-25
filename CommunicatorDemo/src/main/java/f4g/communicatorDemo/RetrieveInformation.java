/**
* ============================== Header ============================== 
* file:          RetrieveInformation.java
* project:       FIT4Green/CommunicatorDemo
* created:       28/06/2012 by jclegea
* 
* $LastChangedDate: 2012-06-29 14:23:43 +0200 (vie, 29 jun 2012) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 1509 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorDemo;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.commons.util.Util;

/**
 * Retrieve the information from the Datacenter
 * 
 *
 * @author jclegea
 */
public class RetrieveInformation {
	static Logger log = Logger.getLogger(RetrieveInformation.class.getName());
	
	private static Properties configurationDemo_ = null;
	private String exampleDataCenter_ = null;
	private boolean isConnected_ = false;
	
	FIT4Green datacenterExample_ = null;
	
	public boolean init(){
		try {
			configurationDemo_ = new Properties();
			InputStream configInputStream = this.getClass().getClassLoader().getResourceAsStream("communicatorDemo/ComDemo.properties");
			configurationDemo_.load(configInputStream);
			exampleDataCenter_ = configurationDemo_.getProperty("exampleModel");
			
		} catch (IOException e) {			
			log.error("Error reading ComDemo properties", e);
		}
		
		return true;
	}
	
	/**
	 * 
	 * Connect to the Data Center
	 * 
	 * @return true if the connection is succesfull, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean connect(){
		
			
		if(!isConnect()){
			log.debug("Connecting with Data Center...");
			isConnected_ = true;			
		}
		
		return isConnected_;
	}
	
	/**
	 * 
	 * test whether the connection with the datacenter is up and running
	 * 
	 * @return true if is connected, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean isConnect(){
		
		return isConnected_;
	}
	
	/**
	 * 
	 * Set whatever is neccessary to start using the data from the datacenter
	 * 
	 * @return true if the data is loaded correctly, false otherwise
	 *
	 * @author jclegea
	 */
	public boolean startRetrieveInformation(){
		return loadModel(exampleDataCenter_);
	}
	
	
	/**
	 * 
	 * finish the connection with the datacenter
	 * 
	 * @return the status of the connection
	 *
	 * @author jclegea
	 */
	public boolean disconnect(){
		isConnected_ = false;
		
		return isConnected_;
	}
	
	
	/**
	 * Loads the FIT4Green model from an XML file and transforms it into an object 
	 * hierarchy representation.
	 * 
	 * @param modelPathName path to the XML model file
	 * @return true if success, false otherwise
	 * 
	 * @author jclegea
	 */	
	public boolean loadModel(String datacenterExamplePathName) {
		
		InputStream isModel = 
			this.getClass().getClassLoader().getResourceAsStream(datacenterExamplePathName);
		
		log.debug("datacenterExample PathName: " + datacenterExamplePathName + ", isModel: " + isModel);
		
		JAXBElement<?> poElement = null;
		try {
			// create an Unmarshaller
			Unmarshaller u = Util.getJaxbContext().createUnmarshaller();

			// ****** VALIDATION ******
			URL url = 
				this.getClass().getClassLoader().getResource("schema/MetaModel.xsd");
			
			log.debug("URL: " + url);
			
			SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			try {
				Schema schema = sf.newSchema(url);
				u.setSchema(schema);
				u.setEventHandler(new ValidationEventHandler() {
					// allow unmarshalling to continue even if there are errors
					@Override
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
			// objects composed of classes from the "f4gschema" package.
			poElement = (JAXBElement<?>) u.unmarshal(isModel);
			
			datacenterExample_ = (FIT4Green) poElement.getValue();
			

		} catch (JAXBException je) {
			log.error(je);
			return false;
		}
		
		return true;
	}
	

///////////////////////////////////////////////////////////////////////////////////////////
// SECTION of funcions to get information from the datacenter hypervisor  
// in our case is from XML they are quite simple and they are not individually commented.
///////////////////////////////////////////////////////////////////////////////////////////
	
	// get the structure of the DC 
	public int getSiteListSize(){		
		return datacenterExample_.getSite().size();
	}
	
	public int getDatacenterListSize(int siteIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().size();
	}
	
	public int getRackListSize(int siteIndex, int datacenterIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().size();
	}
	
	public int getEnclosureListSize(int siteIndex,int datacenterIndex, int rackIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().size();
	}
	
	public int getHostListSize(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().size();
	}
	
	public int getVmListSize(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex, int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().size();
	}
	
	
	// get dynamic data from hosts
	public String getHostName(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getFrameworkID(); 
	}
	
	public String getPowerState(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getStatus().value();
	}
	
	public double getMeasuredPower(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMeasuredPower().getValue();
	}
	
	public double getHostMemoryUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getMemoryUsage().getValue();
	}
		
	// get information from Hard disks 
	
	public int getHDListSize(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getHardwareRAID().get(0).getHardDisk().size();
	}
	
	public double getHDReadRate(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int HDIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getHardwareRAID().get(0).getHardDisk().get(HDIndex).getReadRate().getValue();
	}
	
	public double getHDWriteRate(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int HDIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getHardwareRAID().get(0).getHardDisk().get(HDIndex).getReadRate().getValue();
	}
	
	public String getHDFramework(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int HDIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getHardwareRAID().get(0).getHardDisk().get(HDIndex).getFrameworkID();
	}
	
	// get information from CPUs
	public int getHostCPUListSize(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().size();
	}
	
	public double getHostCPUUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int CPUIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().get(CPUIndex).getCpuUsage().getValue();
	}
	
	public int getHostCoreListSize(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int CPUIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().get(CPUIndex).getCore().size();
	}
		
	public double getHostCoreLoad(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int CPUIndex, int COREIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().get(CPUIndex).getCore().get(COREIndex).getCoreLoad().getValue();
	}
	
	public double getHostCoreTotalPSates(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int CPUIndex, int COREIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().get(CPUIndex).getCore().get(COREIndex).getTotalPstates().getValue();
	}
	
	public double getHostCoreLastPSate(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int CPUIndex, int COREIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getMainboard().get(0).getCPU().get(CPUIndex).getCore().get(COREIndex).getLastPstate().getValue();
	}

	// Get dynamic data from virtual machines 
	
	public String getVmName(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){		
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getName();
	}
	
	public int getVmNumCpus(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getNumberOfCPUs().getValue();
	}
	
	public double getActualVMCPUUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getActualCPUUsage().getValue(); 
	}
	
	public double getActualVMDiskIORate(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getActualDiskIORate().getValue(); 
	}
	
	public double getActualVMMemoryUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getActualMemoryUsage().getValue(); 
	}
	
	public double getActualVMNetworkUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getActualNetworkUsage().getValue(); 
	}
	
	public double getActualVMStorageUsage(int siteIndex,int datacenterIndex, int rackIndex, int enclosureIndex,int hostIndex, int vmIndex){
		return datacenterExample_.getSite().get(siteIndex).getDatacenter().get(datacenterIndex).getRack().get(rackIndex).getEnclosure().get(enclosureIndex).getBladeServer().get(hostIndex).getNativeHypervisor().getVirtualMachine().get(vmIndex).getActualStorageUsage().getValue(); 
	}
	
	
}
