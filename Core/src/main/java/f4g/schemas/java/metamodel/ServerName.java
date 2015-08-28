package f4g.schemas.java.metamodel;

import java.util.Objects;


public class ServerName {
    private final String name;

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     */
    public ServerName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServerName) {
            final ServerName other = (ServerName) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
