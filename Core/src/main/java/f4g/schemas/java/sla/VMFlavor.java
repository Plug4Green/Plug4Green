package f4g.schemas.java.sla;

public class VMFlavor
{

    protected FlavorName name;
    protected Capacity capacity;
    protected ExpectedLoad expectedLoad;

    public VMFlavor(FlavorName name, Capacity capacity, ExpectedLoad expectedLoad) {
        this.name = name;
        this.capacity = capacity;
        this.expectedLoad = expectedLoad;
    }

    public VMFlavor() {
    }

    public FlavorName getName() {
        return name;
    }

    public void setName(FlavorName name) {
        this.name = name;
    }

    public Capacity getCapacity() {
        return capacity;
    }

    public void setCapacity(Capacity capacity) {
        this.capacity = capacity;
    }

    public ExpectedLoad getExpectedLoad() {
        return expectedLoad;
    }

    public void setExpectedLoad(ExpectedLoad expectedLoad) {
        this.expectedLoad = expectedLoad;
    }


}
