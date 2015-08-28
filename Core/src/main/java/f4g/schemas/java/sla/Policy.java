package f4g.schemas.java.sla;

import javax.measure.quantity.Duration;
import org.jscience.physics.amount.Amount;

public class Policy {

    protected PolicyName name;
    protected Amount<Duration> delayBetweenMove;
    protected Amount<Duration> delayBetweenOnOff;
    protected Integer spareNodes;
    protected Integer spareCores;

    public Policy(PolicyName name, Amount<Duration> delayBetweenMove, Amount<Duration> delayBetweenOnOff, Integer spareNodes, Integer spareCores) {
        this.name = name;
        this.delayBetweenMove = delayBetweenMove;
        this.delayBetweenOnOff = delayBetweenOnOff;
        this.spareNodes = spareNodes;
        this.spareCores = spareCores;
    }

    public Policy() {
    }

    public Amount<Duration> getDelayBetweenMove() {
        return delayBetweenMove;
    }

    public void setDelayBetweenMove(Amount<Duration> delayBetweenMove) {
        this.delayBetweenMove = delayBetweenMove;
    }

    public Amount<Duration> getDelayBetweenOnOff() {
        return delayBetweenOnOff;
    }

    public void setDelayBetweenOnOff(Amount<Duration> delayBetweenOnOff) {
        this.delayBetweenOnOff = delayBetweenOnOff;
    }


    public Integer getSpareNodes() {
        return spareNodes;
    }

    public void setSpareNodes(Integer spareNodes) {
        this.spareNodes = spareNodes;
    }

    public Integer getSpareCores() {
        return spareCores;
    }

    public void setSpareCores(Integer spareCores) {
        this.spareCores = spareCores;
    }

    public PolicyName getName() {
        return name;
    }

    public void setName(PolicyName name) {
        this.name = name;
    }
}
