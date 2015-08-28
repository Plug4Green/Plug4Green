package f4g.schemas.java.sla;

import java.util.List;

public class SLAs {

    protected List<SLA> slas;

    public SLAs(List<SLA> slas) {
        this.slas = slas;
    }

    public SLAs() {
    }

    public List<SLA> getSlas() {
        return slas;
    }

    public void setSlas(List<SLA> slas) {
        this.slas = slas;
    }

}
