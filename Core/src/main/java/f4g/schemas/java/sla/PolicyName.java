package f4g.schemas.java.sla;

import java.util.Objects;

public class PolicyName {

    private final String name;

    public PolicyName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PolicyName) {
            final PolicyName other = (PolicyName) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
