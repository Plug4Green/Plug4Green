
package f4g.schemas.java.metamodel;

public class VMActions {

    protected boolean interLiveMigrate;
    protected boolean intraLiveMigrate;
    protected boolean interMoveVM;
    protected boolean intraMoveVM;

    public VMActions() {
	super();
    }

    public VMActions(final boolean interLiveMigrate, final boolean intraLiveMigrate, final boolean interMoveVM,
	    final boolean intraMoveVM) {
	this.interLiveMigrate = interLiveMigrate;
	this.intraLiveMigrate = intraLiveMigrate;
	this.interMoveVM = interMoveVM;
	this.intraMoveVM = intraMoveVM;
    }

    public boolean isInterLiveMigrate() {
	return interLiveMigrate;
    }

    public void setInterLiveMigrate(boolean value) {
	this.interLiveMigrate = value;
    }

    public boolean isIntraLiveMigrate() {
	return intraLiveMigrate;
    }

    public void setIntraLiveMigrate(boolean value) {
	this.intraLiveMigrate = value;
    }

    public boolean isInterMoveVM() {
	return interMoveVM;
    }

    public void setInterMoveVM(boolean value) {
	this.interMoveVM = value;
    }

    public boolean isIntraMoveVM() {
	return intraMoveVM;
    }

    public void setIntraMoveVM(boolean value) {
	this.intraMoveVM = value;
    }

}
