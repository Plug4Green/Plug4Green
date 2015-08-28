package f4g.schemas.java.metamodel;

import java.util.Objects;

public class ServerName {
    private String name;

    public ServerName() {
	super();
    }

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
