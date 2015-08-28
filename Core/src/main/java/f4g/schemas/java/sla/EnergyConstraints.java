package f4g.schemas.java.sla;

import javax.measure.quantity.Power;
import org.jscience.physics.amount.Amount;

public class EnergyConstraints
{

    protected Amount<Power> maxPowerServer;

    public EnergyConstraints() {}

    public EnergyConstraints(final Amount<Power> maxPowerServer) {
        this.maxPowerServer = maxPowerServer;
    }

    public Amount<Power> getMaxPowerServer() {
        return maxPowerServer;
    }

    public void setMaxPowerServer(Amount<Power> maxPowerServer) {
        this.maxPowerServer = maxPowerServer;
    }
}
