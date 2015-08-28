
package f4g.schemas.java.metamodel;

import java.util.ArrayList;
import java.util.List;

import javax.measure.quantity.Energy;

import org.jscience.physics.amount.Amount;

public class Datacenter {

    protected DatacenterName name;
    protected PUE pue;
    protected CUE cue;
    protected Amount<Energy> eVMMigration;
    protected List<FrameworkCapabilities> frameworkCapabilities;
    protected List<Server> servers;

    public Datacenter() {
	super();
    }

    public Datacenter(DatacenterName name, PUE pue, CUE cue, Amount<Energy> eVMMigration,
	    final List<FrameworkCapabilities> frameworkCapabilities, List<Server> servers) {
	this.name = name;
	this.pue = pue;
	this.cue = cue;
	this.eVMMigration = eVMMigration;
	this.frameworkCapabilities = frameworkCapabilities;
	this.servers = servers;
    }

    public DatacenterName getName() {
	return name;
    }

    public void setName(DatacenterName value) {
	this.name = value;
    }

    public List<FrameworkCapabilities> getFrameworkCapabilities() {
	if (frameworkCapabilities == null) {
	    frameworkCapabilities = new ArrayList<FrameworkCapabilities>();
	}
	return this.frameworkCapabilities;
    }

    public void setFrameworkCapabilities(List<FrameworkCapabilities> frameworkCapabilities) {
	this.frameworkCapabilities = frameworkCapabilities;
    }

    public PUE getPue() {
	return pue;
    }

    public void setPue(PUE pue) {
	this.pue = pue;
    }

    public CUE getCue() {
	return cue;
    }

    public void setCue(CUE cue) {
	this.cue = cue;
    }

    public Amount<Energy> getEvMigration() {
	return eVMMigration;
    }

    public void setEvMigration(Amount<Energy> evMigration) {
	this.eVMMigration = evMigration;
    }

    public List<Server> getServers() {
	return servers;
    }

    public void setServers(List<Server> servers) {
	this.servers = servers;
    }
}
