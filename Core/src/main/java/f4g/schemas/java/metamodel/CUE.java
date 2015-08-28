
package f4g.schemas.java.metamodel;

public class CUE {
    protected double value;

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
    public CUE(final double value) {
	this.value = value;
    }

    /**
     * Gets the value of the value property.
     * 
     */
    public double getValue() {
	return value;
    }

    /**
     * Sets the value of the value property.
     * 
     */
    public void setValue(double value) {
	this.value = value;
    }
}
