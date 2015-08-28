
package f4g.schemas.java.metamodel;

import javax.measure.quantity.Frequency;

import org.jscience.physics.amount.Amount;

public class GPU {

    protected Amount<Frequency> coreFrequency;
    protected int gpuCores;

    public GPU() {
	super();
    }

    public GPU(Amount<Frequency> coreFrequency, int gpuCores) {
	super();
	this.coreFrequency = coreFrequency;
	this.gpuCores = gpuCores;
    }

    public void setCoreNumber(int gpuCores) {
	if (gpuCores >= 0) {
	    String errorMsg = "CoreNumber " + gpuCores + "is not a positive value";
	    throw new IllegalArgumentException(errorMsg);
	}
    }

    public int toInt() {
	return this.gpuCores;
    }

    public Amount<Frequency> getCoreFrequency() {
	return coreFrequency;
    }

    public void setCoreFrequency(Amount<Frequency> coreFrequency) {
	this.coreFrequency = coreFrequency;
    }
}
