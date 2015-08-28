package f4g.schemas.java.metamodel;

import java.util.Objects;


public class VirtualMachineName {
    
    private final String name;

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     */
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
