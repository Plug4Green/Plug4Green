package f4g.schemas.java.sla;

import java.util.List;

public class VMFlavors
{

    protected List<VMFlavor> vmFlavors;

    public VMFlavors() {
    }

    public VMFlavors(List<VMFlavor> vmFlavors) {
        this.vmFlavors = vmFlavors;
    }

    public List<VMFlavor> getVmFlavors() {
        return vmFlavors;
    }

    public void setVmFlavors(List<VMFlavor> vmFlavors) {
        this.vmFlavors = vmFlavors;
    }

}
