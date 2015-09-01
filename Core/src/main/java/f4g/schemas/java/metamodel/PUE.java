
package f4g.schemas.java.metamodel;

import org.jscience.physics.amount.Amount;

import javax.measure.quantity.Dimensionless;

public class PUE {

    protected Amount<Dimensionless> value;

    public PUE() {
	super();
    }

    public PUE(final Amount<Dimensionless> value) {
	this.value = value;
    }

    public Amount<Dimensionless> getValue() {
	return value;
    }

    public void setValue(Amount<Dimensionless> value) {
	this.value = value;
    }

}
