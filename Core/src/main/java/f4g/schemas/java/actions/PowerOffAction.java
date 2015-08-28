
package f4g.schemas.java.actions;

import org.joda.time.DateTime;

import f4g.schemas.java.metamodel.ServerName;

public class PowerOffAction extends AbstractBaseAction {

    protected ServerName serverName;

    public PowerOffAction() {
	super();
    }

    public PowerOffAction(ActionId id, Boolean forwarded, DateTime forwardedAt, ServerName servername) {
	super(id, forwarded, forwardedAt);
	this.serverName = servername;
    }

    public ServerName getNodeName() {
	return serverName;
    }

    public void setNodeName(ServerName value) {
	this.serverName = value;
    }

}
