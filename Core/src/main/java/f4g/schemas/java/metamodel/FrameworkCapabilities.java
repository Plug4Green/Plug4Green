
package f4g.schemas.java.metamodel;

public class FrameworkCapabilities {

    protected NodeActions node;
    protected VMActions vm;

    public FrameworkCapabilities() {
	super();
    }

    public FrameworkCapabilities(final NodeActions node, final VMActions vm) {
	this.node = node;
	this.vm = vm;
    }

    public NodeActions getNode() {
	return node;
    }

    public void setNode(NodeActions value) {
	this.node = value;
    }

    public VMActions getVm() {
	return vm;
    }

    public void setVmActions(VMActions value) {
	this.vm = value;
    }

}
