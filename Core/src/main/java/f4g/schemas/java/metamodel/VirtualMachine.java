
package f4g.schemas.java.metamodel;

import javax.measure.quantity.DataAmount;
import javax.measure.quantity.Dimensionless;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import f4g.schemas.java.sla.FlavorName;

public class VirtualMachine {

    protected VirtualMachineName name;
    protected Amount<Dimensionless> actualCPUUsage;
    protected Amount<DataAmount> actualStorageUsage;
    protected Amount<DataAmount> actualMemoryUsage;
    protected DateTime startTimestamp;
    protected DateTime lastMigrationTimestamp;
    protected FlavorName flavorName;

    public VirtualMachine() {
        super();
    }

    public VirtualMachine(VirtualMachineName name, Amount<Dimensionless> actualCPUUsage,
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

    public Amount<Dimensionless> getActualCPUUsage() {
        return actualCPUUsage;
    }

    public void setActualCPUUsage(Amount<Dimensionless> actualCPUUsage) {
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


    public FlavorName getFlavorName() {
        return flavorName;
    }

    public void setFlavorName(FlavorName flavorName) {
        this.flavorName = flavorName;
    }

}
