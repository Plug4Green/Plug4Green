package f4g.schemas.java.sla;

import java.util.Objects;

public class SLAName {

    private final String name;

    public SLAName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SLAName) {
            final SLAName other = (SLAName) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
