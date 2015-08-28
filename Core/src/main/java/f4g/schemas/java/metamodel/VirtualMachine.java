//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//

package f4g.schemas.java.metamodel;

import javax.measure.quantity.DataAmount;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import f4g.schemas.java.sla.FlavorName;

//TODO: implement CopyTo if needed
public class VirtualMachine implements Cloneable /* , CopyTo */
{

    protected VirtualMachineName name;
    protected Amount<DataAmount> actualCPUUsage;
    protected Amount<DataAmount> actualStorageUsage;
    protected Amount<DataAmount> actualMemoryUsage;
    protected DateTime startTimestamp;
    protected DateTime lastMigrationTimestamp;
    protected FlavorName flavorName;

    
    public VirtualMachine(VirtualMachineName name, Amount<DataAmount> actualCPUUsage, Amount<DataAmount> actualStorageUsage,
	    Amount<DataAmount> actualMemoryUsage, DateTime startTimestamp, DateTime lastMigrationTimestamp, 
	    FlavorName flavorName) {
	super();
	this.name = name;
	this.actualCPUUsage = actualCPUUsage;
	this.actualStorageUsage = actualStorageUsage;
	this.actualMemoryUsage = actualMemoryUsage;
	this.startTimestamp = startTimestamp;
	this.lastMigrationTimestamp = lastMigrationTimestamp;
	this.flavorName = flavorName;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return possible object is {@link VirtualMachineName }
     * 
     */
    public VirtualMachineName getName() {
	return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *            allowed object is {@link VirtualMachineName }
     * 
     */
    public void setName(VirtualMachineName value) {
	this.name = value;
    }

    public Amount<DataAmount> getActualCPUUsage() {
	return actualCPUUsage;
    }

    public void setActualCPUUsage(Amount<DataAmount> actualCPUUsage) {
	this.actualCPUUsage = actualCPUUsage;
    }

    public Amount<DataAmount> getActualStorageUsage() {
	return actualStorageUsage;
    }

    public void setActualStorageUsage(Amount<DataAmount> actualStorageUsage) {
	this.actualStorageUsage = actualStorageUsage;
    }

    public Amount<DataAmount> getActualMemoryUsage() {
	return actualMemoryUsage;
    }

    public void setActualMemoryUsage(Amount<DataAmount> actualMemoryUsage) {
	this.actualMemoryUsage = actualMemoryUsage;
    }

    public DateTime getStartTimestamp() {
	return startTimestamp;
    }

    public void setStartTimestamp(DateTime startTimestamp) {
	this.startTimestamp = startTimestamp;
    }

    public DateTime getLastMigrationTimestamp() {
	return lastMigrationTimestamp;
    }

    public void setLastMigrationTimestamp(DateTime lastMigrationTimestamp) {
	this.lastMigrationTimestamp = lastMigrationTimestamp;
    }

    /*
     * public Object copyTo(Object target) { final CopyStrategy strategy =
     * JAXBCopyStrategy.INSTANCE; return copyTo(null, target, strategy); }
     * 
     * public Object copyTo(ObjectLocator locator, Object target, CopyStrategy
     * strategy) { final Object draftCopy = ((target ==
     * null)?createNewInstance():target); if (draftCopy instanceof
     * VirtualMachine) { final VirtualMachine copy = ((VirtualMachine)
     * draftCopy); if (this.name!= null) { String sourceName; sourceName =
     * this.getName(); String copyName = ((String)
     * strategy.copy(LocatorUtils.property(locator, "name", sourceName),
     * sourceName)); copy.setName(copyName); } else { copy.name = null; } if
     * (this.numberOfCPUs!= null) { NrOfCpus sourceNumberOfCPUs;
     * sourceNumberOfCPUs = this.getNumberOfCPUs(); NrOfCpus copyNumberOfCPUs =
     * ((NrOfCpus) strategy.copy(LocatorUtils.property(locator, "numberOfCPUs",
     * sourceNumberOfCPUs), sourceNumberOfCPUs));
     * copy.setNumberOfCPUs(copyNumberOfCPUs); } else { copy.numberOfCPUs =
     * null; } if (this.actualCPUUsage!= null) { CpuUsage sourceActualCPUUsage;
     * sourceActualCPUUsage = this.getActualCPUUsage(); CpuUsage
     * copyActualCPUUsage = ((CpuUsage)
     * strategy.copy(LocatorUtils.property(locator, "actualCPUUsage",
     * sourceActualCPUUsage), sourceActualCPUUsage));
     * copy.setActualCPUUsage(copyActualCPUUsage); } else { copy.actualCPUUsage
     * = null; } if (this.actualStorageUsage!= null) { StorageUsage
     * sourceActualStorageUsage; sourceActualStorageUsage =
     * this.getActualStorageUsage(); StorageUsage copyActualStorageUsage =
     * ((StorageUsage) strategy.copy(LocatorUtils.property(locator,
     * "actualStorageUsage", sourceActualStorageUsage),
     * sourceActualStorageUsage));
     * copy.setActualStorageUsage(copyActualStorageUsage); } else {
     * copy.actualStorageUsage = null; } if (this.actualDiskIORate!= null) {
     * IoRate sourceActualDiskIORate; sourceActualDiskIORate =
     * this.getActualDiskIORate(); IoRate copyActualDiskIORate = ((IoRate)
     * strategy.copy(LocatorUtils.property(locator, "actualDiskIORate",
     * sourceActualDiskIORate), sourceActualDiskIORate));
     * copy.setActualDiskIORate(copyActualDiskIORate); } else {
     * copy.actualDiskIORate = null; } if (this.actualMemoryUsage!= null) {
     * MemoryUsage sourceActualMemoryUsage; sourceActualMemoryUsage =
     * this.getActualMemoryUsage(); MemoryUsage copyActualMemoryUsage =
     * ((MemoryUsage) strategy.copy(LocatorUtils.property(locator,
     * "actualMemoryUsage", sourceActualMemoryUsage), sourceActualMemoryUsage));
     * copy.setActualMemoryUsage(copyActualMemoryUsage); } else {
     * copy.actualMemoryUsage = null; } if (this.actualNetworkUsage!= null) {
     * NetworkUsage sourceActualNetworkUsage; sourceActualNetworkUsage =
     * this.getActualNetworkUsage(); NetworkUsage copyActualNetworkUsage =
     * ((NetworkUsage) strategy.copy(LocatorUtils.property(locator,
     * "actualNetworkUsage", sourceActualNetworkUsage),
     * sourceActualNetworkUsage));
     * copy.setActualNetworkUsage(copyActualNetworkUsage); } else {
     * copy.actualNetworkUsage = null; } if (this.hostedOperatingSystem!= null)
     * { HostedOperatingSystem sourceHostedOperatingSystem;
     * sourceHostedOperatingSystem = this.getHostedOperatingSystem();
     * HostedOperatingSystem copyHostedOperatingSystem =
     * ((HostedOperatingSystem) strategy.copy(LocatorUtils.property(locator,
     * "hostedOperatingSystem", sourceHostedOperatingSystem),
     * sourceHostedOperatingSystem));
     * copy.setHostedOperatingSystem(copyHostedOperatingSystem); } else {
     * copy.hostedOperatingSystem = null; } if (this.frameworkID!= null) {
     * String sourceFrameworkID; sourceFrameworkID = this.getFrameworkID();
     * String copyFrameworkID = ((String)
     * strategy.copy(LocatorUtils.property(locator, "frameworkID",
     * sourceFrameworkID), sourceFrameworkID));
     * copy.setFrameworkID(copyFrameworkID); } else { copy.frameworkID = null; }
     * if (this.cloudVmImage!= null) { String sourceCloudVmImage;
     * sourceCloudVmImage = this.getCloudVmImage(); String copyCloudVmImage =
     * ((String) strategy.copy(LocatorUtils.property(locator, "cloudVmImage",
     * sourceCloudVmImage), sourceCloudVmImage));
     * copy.setCloudVmImage(copyCloudVmImage); } else { copy.cloudVmImage =
     * null; } if (this.cloudVm!= null) { String sourceCloudVm; sourceCloudVm =
     * this.getCloudVm(); String copyCloudVm = ((String)
     * strategy.copy(LocatorUtils.property(locator, "cloudVm", sourceCloudVm),
     * sourceCloudVm)); copy.setCloudVm(copyCloudVm); } else { copy.cloudVm =
     * null; } if (this.startTimestamp!= null) { XMLGregorianCalendar
     * sourceStartTimestamp; sourceStartTimestamp = this.getStartTimestamp();
     * XMLGregorianCalendar copyStartTimestamp = ((XMLGregorianCalendar)
     * strategy.copy(LocatorUtils.property(locator, "startTimestamp",
     * sourceStartTimestamp), sourceStartTimestamp));
     * copy.setStartTimestamp(copyStartTimestamp); } else { copy.startTimestamp
     * = null; } if (this.endTimestamp!= null) { XMLGregorianCalendar
     * sourceEndTimestamp; sourceEndTimestamp = this.getEndTimestamp();
     * XMLGregorianCalendar copyEndTimestamp = ((XMLGregorianCalendar)
     * strategy.copy(LocatorUtils.property(locator, "endTimestamp",
     * sourceEndTimestamp), sourceEndTimestamp));
     * copy.setEndTimestamp(copyEndTimestamp); } else { copy.endTimestamp =
     * null; } if (this.lastMigrationTimestamp!= null) { XMLGregorianCalendar
     * sourceLastMigrationTimestamp; sourceLastMigrationTimestamp =
     * this.getLastMigrationTimestamp(); XMLGregorianCalendar
     * copyLastMigrationTimestamp = ((XMLGregorianCalendar)
     * strategy.copy(LocatorUtils.property(locator, "lastMigrationTimestamp",
     * sourceLastMigrationTimestamp), sourceLastMigrationTimestamp));
     * copy.setLastMigrationTimestamp(copyLastMigrationTimestamp); } else {
     * copy.lastMigrationTimestamp = null; } if (this.frameworkRef!= null) {
     * Object sourceFrameworkRef; sourceFrameworkRef = this.getFrameworkRef();
     * Object copyFrameworkRef = ((Object)
     * strategy.copy(LocatorUtils.property(locator, "frameworkRef",
     * sourceFrameworkRef), sourceFrameworkRef));
     * copy.setFrameworkRef(copyFrameworkRef); } else { copy.frameworkRef =
     * null; } } return draftCopy; }
     * 
     * public Object createNewInstance() { return new VirtualMachine(); }
     */
}
