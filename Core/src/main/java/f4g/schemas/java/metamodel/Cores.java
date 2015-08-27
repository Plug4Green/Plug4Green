package f4g.schemas.java.metamodel;

import javax.measure.quantity.Frequency;

import org.jscience.physics.amount.Amount;

public class Cores {

    protected int coreNumber;
    protected Amount<Frequency> frequency;

    public Cores(int coreNumber, Amount<Frequency> frequency) {
	this.coreNumber = coreNumber;
	this.frequency = frequency;
    }

    public void setCoreNumber(int coreNumber) {
	if (coreNumber < 1) {
	    String errorMsg = "CoreNumber " + coreNumber + "is not major of zero";
	    throw new IllegalArgumentException(errorMsg);
	}
    }

    public int toInt() {
	return this.coreNumber;
    }

    public Amount<Frequency> getFrequency() {
	return frequency;
    }

    public void setFrequency(Amount<Frequency> frequency) {
	this.frequency = frequency;
    }

}
