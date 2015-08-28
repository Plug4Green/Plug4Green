package f4g.schemas.java.sla;

import javax.measure.quantity.DataRate;
import javax.measure.quantity.Dimensionless;

import org.jscience.physics.amount.Amount;

public class QoSConstraints {

    protected Amount<Dimensionless> maxServerCPULoad;
    protected Amount<Dimensionless> maxVCPUPerCore;
    protected Amount<Dimensionless> maxVRAMPerPhyRAM;
    protected Amount<DataRate> MinBandwidth;
    protected Integer maxVMPerServer;

    public QoSConstraints() {
    }

    public QoSConstraints(Amount<Dimensionless> maxServerCPULoad,
                          Amount<Dimensionless> maxVCPUPerCore,
                          Amount<Dimensionless> maxVRAMPerPhyRAM,
                          Amount<DataRate> MinBandwidth,
                          Integer maxVMPerServer) {
        this.maxServerCPULoad = maxServerCPULoad;
        this.maxVCPUPerCore = maxVCPUPerCore;
        this.maxVRAMPerPhyRAM = maxVRAMPerPhyRAM;
        this.MinBandwidth = MinBandwidth;
        this.maxVMPerServer = maxVMPerServer;
    }

    public Amount<Dimensionless> getMaxServerCPULoad() {
        return maxServerCPULoad;
    }

    public void setMaxServerCPULoad(Amount<Dimensionless> maxServerCPULoad) {
        this.maxServerCPULoad = maxServerCPULoad;
    }

    public Amount<Dimensionless> getMaxVCPUPerCore() {
        return maxVCPUPerCore;
    }

    public void setMaxVCPUPerCore(Amount<Dimensionless> maxVCPUPerCore) {
        this.maxVCPUPerCore = maxVCPUPerCore;
    }

    public Amount<Dimensionless> getMaxVRAMPerPhyRAM() {
        return maxVRAMPerPhyRAM;
    }

    public void setMaxVRAMPerPhyRAM(Amount<Dimensionless> maxVRAMPerPhyRAM) {
        this.maxVRAMPerPhyRAM = maxVRAMPerPhyRAM;
    }

    public Amount<DataRate> getMinBandwidth() {
        return MinBandwidth;
    }

    public void setMinBandwidth(Amount<DataRate> minBandwidth) {
        MinBandwidth = minBandwidth;
    }

    public Integer getMaxVMPerServer() {
        return maxVMPerServer;
    }

    public void setMaxVMPerServer(Integer maxVMPerServer) {
        this.maxVMPerServer = maxVMPerServer;
    }

}
