
package f4g.schemas.java.metamodel;

import java.util.List;

import javax.measure.quantity.Power;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

public class Server {

    protected ServerRole name;
    protected ServerStatus status;
    protected ServerName serverName;
    protected Amount<Power> idlePower;
    protected Amount<Power> maxPower;
    protected DateTime lastOnOffTimestamp;
    protected RAMSize ramSize;
    protected StorageCapacity storageCapacity;
    protected Cores cores;
    protected GPU gpu;
    protected List<VirtualMachine> VMs;

    public Server() {
	super();
    }

    public Server(ServerRole name, ServerStatus status, ServerName frameworkID, Amount<Power> idlePower,
	    Amount<Power> maxPower, DateTime lastOnOffTimestamp, RAMSize ramSize, StorageCapacity storageCapacity,
	    Cores cores, GPU gpu, List<VirtualMachine> vMs) {
	super();
	this.name = name;
	this.status = status;
	this.serverName = frameworkID;
	this.idlePower = idlePower;
	this.maxPower = maxPower;
	this.lastOnOffTimestamp = lastOnOffTimestamp;
	this.ramSize = ramSize;
	this.storageCapacity = storageCapacity;
	this.cores = cores;
	this.gpu = gpu;
	this.VMs = vMs;
    }

    public ServerRole getName() {
	return name;
    }

    public void setName(ServerRole value) {
	this.name = value;
    }

    public ServerStatus getStatus() {
	return status;
    }

    public void setStatus(ServerStatus value) {
	this.status = value;
    }

    public ServerName getFrameworkID() {
	return serverName;
    }

    public void setFrameworkID(ServerName value) {
	this.serverName = value;
    }

    public Amount<Power> getIdlePower() {
	return idlePower;
    }

    public void setIdlePower(Amount<Power> value) {
	this.idlePower = value;
    }

    public Amount<Power> getMaxPower() {
	return maxPower;
    }

    public void setMeasuredPower(Amount<Power> value) {
	this.maxPower = value;
    }

    public RAMSize getRAMSize() {
	return this.ramSize;
    }

    public void setRAMSize(RAMSize value) {
	this.ramSize = value;
    }

    public StorageCapacity getStorageCapacity() {
	return this.storageCapacity;
    }

    public StorageCapacity setStorageCapacity(StorageCapacity value) {
	return this.storageCapacity = value;
    }

    public void setCoreNumber(Cores value) {
	this.cores = value;
    }

    public DateTime getLastOnOffTimestamp() {
	return lastOnOffTimestamp;
    }

    public void setLastOnOffTimestamp(DateTime value) {
	this.lastOnOffTimestamp = value;
    }
}
