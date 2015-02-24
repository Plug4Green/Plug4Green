package f4g.commons.com.util;

/**
 * Sample implementation of a Com operation 
 * 
 * @author FIT4Green
 *
 */
public class ComOperation {

	public static final String VM_ON_HYPERVISOR_PATH = "./nativeHypervisor/virtualMachine/frameworkID";
	public static final String VM_ON_OS_PATH = "./nativeOperatingSystem/hostedHypervisor/virtualMachine/frameworkID";

	public static String TYPE_ADD = "ADD";
	public static String TYPE_REMOVE = "REMOVE";
	public static String TYPE_UPDATE = "UPDATE";
	
	//Defines the operation type
	private String type = null;
	
	//Define the xpath expression
	private String expression = null;

	//Define the String value to set
	private String value = null;
	
	//Define the Object value to set
	private Object objValue = null;
	
	private boolean isObjectValue = false;
	

	public ComOperation(){
		
	}
	
	public ComOperation(String type, String expression){
		this.type = type;
		this.expression = expression;
	}
	
	public ComOperation(String type, String expression, String value){
		this(type, expression);
		this.value = value;
	}
	
	public ComOperation(String type, String expression, Object value){
		this(type, expression);
		setObjValue(value);
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Object getObjValue() {
		return objValue;
	}

	public void setObjValue(Object objValue) {
		isObjectValue = true;
		this.objValue = objValue;
	}

	public boolean isObjectValue() {
		return isObjectValue;
	}

}
