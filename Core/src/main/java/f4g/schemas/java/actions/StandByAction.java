
package f4g.schemas.java.actions;

import org.joda.time.DateTime;

import f4g.schemas.java.metamodel.ServerName;

public class StandByAction extends AbstractBaseAction {

    protected ServerName serverName;

    public StandByAction(ActionId id, Boolean forwarded, DateTime forwardedAt, ServerName servername) {
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
