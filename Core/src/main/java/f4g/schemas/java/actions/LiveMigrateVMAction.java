
package f4g.schemas.java.actions;

import org.joda.time.DateTime;

import f4g.schemas.java.metamodel.ServerName;
import f4g.schemas.java.metamodel.VirtualMachineName;

public class LiveMigrateVMAction extends AbstractBaseAction
	implements Cloneable /* , CopyTo */
{

    protected VirtualMachineName virtualMachineId;
    protected ServerName srcServer;
    protected ServerName dstServer;

    public LiveMigrateVMAction(ActionId id, Boolean forwarded, DateTime forwardedAt, VirtualMachineName virtualMachine,
	    ServerName srcServer, String dstServer) {
	super(id, forwarded, forwardedAt);
	this.virtualMachineId = virtualMachine;
	this.srcServer = srcServer;
	this.srcServer = srcServer;
    }

    public VirtualMachineName getVirtualMachine() {
	return virtualMachineId;
    }

    public void setVirtualMachine(VirtualMachineName value) {
	this.virtualMachineId = value;
    }

    public ServerName getSourceServer() {
	return srcServer;
    }

    public void setSourceServer(ServerName value) {
	this.srcServer = value;
    }

    public ServerName getDestServer() {
	return dstServer;
    }

    public void setDestServer(ServerName value) {
	this.dstServer = value;
    }
}
