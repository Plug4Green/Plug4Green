package f4g.schemas.java.sla;

public class SecurityConstraints {

    protected Boolean dedicatedServer;
    protected Boolean secureAccessPossibility;

    public SecurityConstraints() {
    }

    public SecurityConstraints(final Boolean dedicatedServer,
                               final Boolean secureAccessPossibility) {
        this.dedicatedServer = dedicatedServer;
        this.secureAccessPossibility = secureAccessPossibility;
    }

    public Boolean getDedicatedServer() {
        return dedicatedServer;
    }

    public void setDedicatedServer(Boolean dedicatedServer) {
        this.dedicatedServer = dedicatedServer;
    }

    public Boolean getSecureAccessPossibility() {
        return secureAccessPossibility;
    }

    public void setSecureAccessPossibility(Boolean secureAccessPossibility) {
        this.secureAccessPossibility = secureAccessPossibility;
    }


}
