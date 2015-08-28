package f4g.schemas.java.actions;

import java.util.Objects;

public class ActionId {
    private final String name;

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id
     *            string identifier
     */
    public ActionId(String name) {
	this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof ActionId) {
	    final ActionId other = (ActionId) obj;
	    return Objects.equals(this.name, other.name);
	}
	return false;
    }

    @Override
    public String toString() {
	return name;
    }
}
