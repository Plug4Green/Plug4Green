package f4g.schemas.java.sla;

import java.util.List;

public class Policies {

    List<Policy> policies;

    public Policies(List<Policy> policies) {
        this.policies = policies;
    }

    public Policies() {
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }
}
