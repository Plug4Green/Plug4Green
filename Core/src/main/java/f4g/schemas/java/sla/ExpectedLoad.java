package f4g.schemas.java.sla;

import javax.measure.quantity.DataAmount;
import javax.measure.quantity.DataRate;
import javax.measure.quantity.Dimensionless;
import org.jscience.physics.amount.Amount;

public class ExpectedLoad
{
    protected Amount<Dimensionless> vCPULoad;
    protected Amount<DataAmount> vRAMUsage;
    protected Amount<DataRate> vDiskLoad;
    protected Amount<DataRate> vNetworkLoad;

    public ExpectedLoad() {
    }

    public ExpectedLoad(Amount<Dimensionless> vCPULoad, Amount<DataAmount> vRAMUsage, Amount<DataRate> vDiskLoad, Amount<DataRate> vNetworkLoad) {
        this.vCPULoad = vCPULoad;
        this.vRAMUsage = vRAMUsage;
        this.vDiskLoad = vDiskLoad;
        this.vNetworkLoad = vNetworkLoad;
    }

    public Amount<Dimensionless> getvCPULoad() {
        return vCPULoad;
    }

    public void setvCPULoad(Amount<Dimensionless> vCPULoad) {
        this.vCPULoad = vCPULoad;
    }

    public Amount<DataAmount> getvRAMUsage() {
        return vRAMUsage;
    }

    public void setvRAMUsage(Amount<DataAmount> vRAMUsage) {
        this.vRAMUsage = vRAMUsage;
    }

    public Amount<DataRate> getvDiskLoad() {
        return vDiskLoad;
    }

    public void setvDiskLoad(Amount<DataRate> vDiskLoad) {
        this.vDiskLoad = vDiskLoad;
    }

    public Amount<DataRate> getvNetworkLoad() {
        return vNetworkLoad;
    }

    public void setvNetworkLoad(Amount<DataRate> vNetworkLoad) {
        this.vNetworkLoad = vNetworkLoad;
    }

}
