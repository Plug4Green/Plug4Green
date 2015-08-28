
package f4g.schemas.java.actions;

import org.joda.time.DateTime;

public abstract class AbstractBaseAction implements Cloneable /* , CopyTo */
{
    protected ActionId id;
    protected Boolean forwarded;
    protected DateTime forwardedAt;

    public AbstractBaseAction() {
	super();
    }

    public AbstractBaseAction(ActionId id, Boolean forwarded, DateTime forwardedAt) {
	this.id = id;
	this.forwarded = forwarded;
	this.forwardedAt = forwardedAt;
    }

    public ActionId getID() {
	return id;
    }

    public void setID(ActionId value) {
	this.id = value;
    }

    public Boolean isForwarded() {
	return forwarded;
    }

    public void setForwarded(Boolean value) {
	this.forwarded = value;
    }

    public DateTime getForwardedAt() {
	return forwardedAt;
    }

    public void setForwardedAt(DateTime value) {
	this.forwardedAt = value;
    }
}
