
package f4g.schemas.java.metamodel;

public enum ServerRole {

    HPC_COMPUTE_NODE, HPC_RESOURCE_MANAGEMENT, CLOUD_CONTROLLER, CLOUD_CLUSTER_CONTROLLER, CLOUD_NODE_CONTROLLER, TRADITIONAL_HOST, OTHER;

    public String value() {
	return name();
    }

    public static ServerRole fromValue(String v) {
	return valueOf(v);
    }

}
