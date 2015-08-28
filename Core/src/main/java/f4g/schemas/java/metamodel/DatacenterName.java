package f4g.schemas.java.metamodel;

import java.util.Objects;

public class DatacenterName {

    private String name;

    public DatacenterName() {
	super();
    }

    public DatacenterName(String name) {
	this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof DatacenterName) {
	    final DatacenterName other = (DatacenterName) obj;
	    return Objects.equals(this.name, other.name);
	}
	return false;
    }

    @Override
    public String toString() {
	return name;
    }

}
