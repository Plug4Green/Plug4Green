package f4g.schemas.java.sla;

import javax.measure.quantity.DataAmount;
import javax.measure.quantity.Frequency;

import org.jscience.physics.amount.Amount;

public class HardwareConstraints {

    protected Amount<DataAmount> hddCapacity;
    protected Integer nbOfCores;
    protected Amount<Frequency> coresFrequency;
    protected Amount<DataAmount> RAMSpace;
    protected Integer nbOfGPUCores;
    protected Amount<Frequency> gpuFrequency;
    protected Integer raidLevel;

    public HardwareConstraints() {
    }

    public HardwareConstraints(final Amount<DataAmount> hddCapacity,
                               final Integer nbOfCores,
                               final Amount<Frequency> coresFrequency,
                               final Amount<DataAmount> RAMSpace,
                               final Integer nbOfGPUCores,
                               final Amount<Frequency> gpuFrequency,
                               final Integer raidLevel) {
        this.hddCapacity = hddCapacity;
        this.nbOfCores = nbOfCores;
        this.coresFrequency = coresFrequency;
        this.RAMSpace = RAMSpace;
        this.nbOfGPUCores = nbOfGPUCores;
        this.gpuFrequency = gpuFrequency;
        this.raidLevel = raidLevel;
    }

    public Amount<DataAmount> getHddCapacity() {
        return hddCapacity;
    }

    public void setHddCapacity(Amount<DataAmount> hddCapacity) {
        this.hddCapacity = hddCapacity;
    }

    public Integer getNbOfCores() {
        return nbOfCores;
    }

    public void setNbOfCores(Integer nbOfCores) {
        this.nbOfCores = nbOfCores;
    }

    public Amount<Frequency> getCoresFrequency() {
        return coresFrequency;
    }

    public void setCoresFrequency(Amount<Frequency> coresFrequency) {
        this.coresFrequency = coresFrequency;
    }

    public Amount<DataAmount> getRAMSpace() {
        return RAMSpace;
    }

    public void setRAMSpace(Amount<DataAmount> RAMSpace) {
        this.RAMSpace = RAMSpace;
    }

    public Integer getNbOfGPUCores() {
        return nbOfGPUCores;
    }

    public void setNbOfGPUCores(Integer nbOfGPUCores) {
        this.nbOfGPUCores = nbOfGPUCores;
    }

    public Amount<Frequency> getGpuFrequency() {
        return gpuFrequency;
    }

    public void setGpuFrequency(Amount<Frequency> gpuFrequency) {
        this.gpuFrequency = gpuFrequency;
    }

    public Integer getRaidLevel() {
        return raidLevel;
    }

    public void setRaidLevel(Integer raidLevel) {
        this.raidLevel = raidLevel;
    }
}
