
package f4g.schemas.java.metamodel;

public enum ServerStatus {

    ON, OFF, STANDBY, POWERING_ON, POWERING_OFF;

    public String value() {
	return name();
    }

    public static ServerStatus fromValue(String v) {
	return valueOf(v);
    }

}
