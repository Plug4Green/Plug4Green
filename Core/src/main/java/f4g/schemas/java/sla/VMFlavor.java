package f4g.schemas.java.sla;

public class VMFlavor
{

    protected String name;
    protected Capacity capacity;
    protected ExpectedLoad expectedLoad;

    public VMFlavor(String name, Capacity capacity, ExpectedLoad expectedLoad) {
        this.name = name;
        this.capacity = capacity;
        this.expectedLoad = expectedLoad;
    }

    public VMFlavor() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
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
