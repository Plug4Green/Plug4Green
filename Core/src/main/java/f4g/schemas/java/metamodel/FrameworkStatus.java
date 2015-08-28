
package f4g.schemas.java.metamodel;

public enum FrameworkStatus {

    STARTING, STOPPING, POWERING_ON, POWERING_OFF;

    public String value() {
	return name();
    }

    public static FrameworkStatus fromValue(String v) {
	return valueOf(v);
    }
}
