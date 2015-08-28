package f4g.schemas.java.sla;


public class SLA {

    protected SLAName name;
    protected HardwareConstraints hardwareConstraints;
    protected QoSConstraints qosConstraints;
    protected SecurityConstraints securityConstraints;
    protected EnergyConstraints energyConstraints;

    public SLA() {
    }

    public SLA(SLAName name,
               HardwareConstraints hardwareConstraints,
               QoSConstraints qosConstraints,
               SecurityConstraints securityConstraints,
               EnergyConstraints energyConstraints) {
        this.name = name;
        this.hardwareConstraints = hardwareConstraints;
        this.qosConstraints = qosConstraints;
        this.securityConstraints = securityConstraints;
        this.energyConstraints = energyConstraints;
    }

    public SLAName getName() {
        return name;
    }

    public void setName(SLAName name) {
        this.name = name;
    }

    public HardwareConstraints getHardwareConstraints() {
        return hardwareConstraints;
    }

    public void setHardwareConstraints(HardwareConstraints hardwareConstraints) {
        this.hardwareConstraints = hardwareConstraints;
    }

    public QoSConstraints getQosConstraints() {
        return qosConstraints;
    }

    public void setQosConstraints(QoSConstraints qosConstraints) {
        this.qosConstraints = qosConstraints;
    }

    public SecurityConstraints getSecurityConstraints() {
        return securityConstraints;
    }

    public void setSecurityConstraints(SecurityConstraints securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    public EnergyConstraints getEnergyConstraints() {
        return energyConstraints;
    }

    public void setEnergyConstraints(EnergyConstraints energyConstraints) {
        this.energyConstraints = energyConstraints;
    }

}
