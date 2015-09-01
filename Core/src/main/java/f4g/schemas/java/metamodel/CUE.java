
package f4g.schemas.java.metamodel;

import org.jscience.physics.amount.Amount;

import javax.measure.quantity.Dimensionless;

public class CUE {

    protected Amount<Dimensionless> value;

    /**
     * Default no-arg constructor
     * 
     */
    public CUE() {
	super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public CUE(final Amount<Dimensionless> value) {
	this.value = value;
    }

    /**
     * Gets the value of the value property.
     * 
     */
    public Amount<Dimensionless> getValue() {
	return value;
    }

    /**
     * Sets the value of the value property.
     * 
     */
    public void setValue(Amount<Dimensionless> value) {
	this.value = value;
    }
}
