package f4g.schemas.java.sla;

import javax.measure.quantity.DataAmount;
import org.jscience.physics.amount.Amount;

public class Capacity
{
    protected Integer nbOfVCPUs;
    protected Amount<DataAmount> VRamSize;
    protected Amount<DataAmount> HDDSize;

    public Capacity() {}

    public Capacity(Integer nbOfVCPUs,
                    Amount<DataAmount> VRamSize,
                    Amount<DataAmount> HDDSize) {
        this.nbOfVCPUs = nbOfVCPUs;
        this.VRamSize = VRamSize;
        this.HDDSize = HDDSize;
    }

    public Integer getNbOfVCPUs() {
        return nbOfVCPUs;
    }

    public void setNbOfVCPUs(Integer nbOfVCPUs) {
        this.nbOfVCPUs = nbOfVCPUs;
    }

    public Amount<DataAmount> getVRamSize() {
        return VRamSize;
    }

    public void setVRamSize(Amount<DataAmount> VRamSize) {
        this.VRamSize = VRamSize;
    }

    public Amount<DataAmount> getHDDSize() {
        return HDDSize;
    }

    public void setHDDSize(Amount<DataAmount> HDDSize) {
        this.HDDSize = HDDSize;
    }
}
