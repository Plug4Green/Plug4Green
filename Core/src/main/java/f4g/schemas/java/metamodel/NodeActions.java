
package f4g.schemas.java.metamodel;

public class NodeActions {

    protected boolean powerOn;
    protected boolean powerOff;
    protected boolean standBy;

    public NodeActions() {
	super();
    }

    public NodeActions(final boolean powerOn, final boolean powerOff, final boolean standBy) {
	this.powerOn = powerOn;
	this.powerOff = powerOff;
	this.standBy = standBy;
    }

    public boolean isPowerOn() {
	return powerOn;
    }

    public void setPowerOn(boolean value) {
	this.powerOn = value;
    }

    public boolean isPowerOff() {
	return powerOff;
    }

    public void setPowerOff(boolean value) {
	this.powerOff = value;
    }

    public boolean isStandBy() {
	return standBy;
    }

    public void setStandBy(boolean value) {
	this.standBy = value;
    }

}
