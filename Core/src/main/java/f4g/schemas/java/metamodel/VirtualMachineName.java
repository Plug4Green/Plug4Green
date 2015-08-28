package f4g.schemas.java.metamodel;

import java.util.Objects;

public class VirtualMachineName {

    private String name;

    public VirtualMachineName() {
	super();
    }

    public VirtualMachineName(String name) {
	this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof VirtualMachineName) {
	    final VirtualMachineName other = (VirtualMachineName) obj;
	    return Objects.equals(this.name, other.name);
	}
	return false;
    }

    @Override
    public String toString() {
	return name;
    }

}
