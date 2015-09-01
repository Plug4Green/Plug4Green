package f4g.schemas.java.actions;

import java.util.Objects;

public class ActionId {
    private String name;

    public ActionId() {
	super();
    }

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