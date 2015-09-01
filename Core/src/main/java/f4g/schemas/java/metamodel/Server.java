
package f4g.schemas.java.metamodel;

import java.util.List;

import javax.measure.quantity.DataAmount;
import javax.measure.quantity.Power;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

public class Server {

    protected ServerName serverName;
    protected ServerRole serverRole;
    protected ServerStatus status;
    protected Amount<Power> idlePower;
    protected Amount<Power> maxPower;
    protected DateTime lastOnOff;
    protected Amount<DataAmount> ramSize;
    protected Amount<DataAmount> storageCapacity;
    protected Cores cores;
    protected GPU gpu;
    protected List<VirtualMachine> VMs;

    public Server() {}

    public Server(ServerRole serverRole,
                  ServerStatus status,
                  ServerName serverName,
                  Amount<Power> idlePower,
                  Amount<Power> maxPower,
                  DateTime lastOnOff,
                  Amount<DataAmount> ramSize,
                  Amount<DataAmount> storageCapacity,
                  Cores cores,
                  GPU gpu,
                  List<VirtualMachine> vMs) {
        super();
        this.serverRole = serverRole;
        this.status = status;
        this.serverName = serverName;
        this.idlePower = idlePower;
        this.maxPower = maxPower;
        this.lastOnOff = lastOnOff;
        this.ramSize = ramSize;
        this.storageCapacity = storageCapacity;
        this.cores = cores;
        this.gpu = gpu;
        this.VMs = vMs;
    }

    public ServerName getServerName() {
        return serverName;
    }

    public void setServerName(ServerName serverName) {
        this.serverName = serverName;
    }

    public ServerRole getServerRole() {
        return serverRole;
    }

    public void setServerRole(ServerRole serverRole) {
        this.serverRole = serverRole;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public Amount<Power> getIdlePower() {
        return idlePower;
    }

    public void setIdlePower(Amount<Power> idlePower) {
        this.idlePower = idlePower;
    }

    public Amount<Power> getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(Amount<Power> maxPower) {
        this.maxPower = maxPower;
    }

    public DateTime getLastOnOff() {
        return lastOnOff;
    }

    public void setLastOnOff(DateTime lastOnOff) {
        this.lastOnOff = lastOnOff;
    }

    public Amount<DataAmount> getRamSize() {
        return ramSize;
    }

    public void setRamSize(Amount<DataAmount> ramSize) {
        this.ramSize = ramSize;
    }

    public Amount<DataAmount> getStorageCapacity() {
        return storageCapacity;
    }

    public void setStorageCapacity(Amount<DataAmount> storageCapacity) {
        this.storageCapacity = storageCapacity;
    }

    public Cores getCores() {
        return cores;
    }

    public void setCores(Cores cores) {
        this.cores = cores;
    }

    public GPU getGpu() {
        return gpu;
    }

    public void setGpu(GPU gpu) {
        this.gpu = gpu;
    }

    public List<VirtualMachine> getVMs() {
        return VMs;
    }

    public void setVMs(List<VirtualMachine> VMs) {
        this.VMs = VMs;
    }
}
