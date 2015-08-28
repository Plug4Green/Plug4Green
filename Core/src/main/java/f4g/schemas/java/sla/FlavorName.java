package f4g.schemas.java.sla;

import java.util.Objects;

public class FlavorName {

    private final String name;

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     */
    public FlavorName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlavorName) {
            final FlavorName other = (FlavorName) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
