
package f4g.schemas.java.metamodel;

import javax.measure.quantity.DataAmount;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import f4g.schemas.java.sla.FlavorName;

public class VirtualMachine {

    protected VirtualMachineName name;
    protected Amount<DataAmount> actualCPUUsage;
    protected Amount<DataAmount> actualStorageUsage;
    protected Amount<DataAmount> actualMemoryUsage;
    protected DateTime startTimestamp;
    protected DateTime lastMigrationTimestamp;
    protected FlavorName flavorName;

    public VirtualMachine() {
	super();
    }

    public VirtualMachine(VirtualMachineName name, Amount<DataAmount> actualCPUUsage,
	    Amount<DataAmount> actualStorageUsage, Amount<DataAmount> actualMemoryUsage, DateTime startTimestamp,
	    DateTime lastMigrationTimestamp, FlavorName flavorName) {
	super();
	this.name = name;
	this.actualCPUUsage = actualCPUUsage;
	this.actualStorageUsage = actualStorageUsage;
	this.actualMemoryUsage = actualMemoryUsage;
	this.startTimestamp = startTimestamp;
	this.lastMigrationTimestamp = lastMigrationTimestamp;
	this.flavorName = flavorName;
    }

    public VirtualMachineName getName() {
	return name;
    }

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
}
