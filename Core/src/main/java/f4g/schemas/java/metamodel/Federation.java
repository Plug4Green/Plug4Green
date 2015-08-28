
package f4g.schemas.java.metamodel;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class Federation {

    protected List<Datacenter> datacenters;
    protected DateTime currentTime;

    public Federation() {
	super();
    }

    public Federation(List<Datacenter> datacenters, DateTime datetime) {
	this.datacenters = datacenters;
	this.currentTime = datetime;
    }

    public List<Datacenter> getDatacenter() {
	if (datacenters == null) {
	    datacenters = new ArrayList<Datacenter>();
	}
	return datacenters;
    }

    public DateTime getcurrentTime() {
	return currentTime;
    }

    public void setcurrentTime(DateTime value) {
	this.currentTime = value;
    }

}
