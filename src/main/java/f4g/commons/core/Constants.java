/**
* ============================== Header ============================== 
* file:          Constants.java
* project:       FIT4Green/Commons
* created:       18 nov 2010 by FIT4Green
* 
* $LastChangedDate: 2011-09-07 16:08:44 +0200 (wo, 07 sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 716 $
* 
* short description:
*   Contains all the constants global to the FIT4Green framework.
* ============================= /Header ==============================
*/
package f4g.commons.core;

/**
 * Contains all the constants global to the FIT4Green framework
 * @author Vasiliki Georgiadou, Corentin Dupont
 *
 */
public class Constants {

	
	public static final String F4G_MODEL_FILE_PATH = "f4gModelFilePath";
	public static final String SLA_CLUSTER_FILE_PATH = "SlaClusterFilePath";
	
	public static final String AUTOMATIC_STARTUP = "automaticStartup";
	
	// TODO Once statistics implemented and tested, these parameters are obsolete
	public static final String RECORDER_IS_ON = "Record";	
	public static final String RECORDER_FILE_PATH = "RecorderFilePath";	
	
	public static final String OPERATION_MODE = "operationMode";
	
	public static final String GLOBAL_OPTIMIZATION_PERIOD = "globalOptimizationPeriod";
	public static final String GLOBAL_OPTIMIZATION_DELAY = "globalOptimizationDelay";
	
	public static final String COMPUTE_POWER_PERIOD = "computePowerPeriod";
	
	public static final String DB_URL = "url";
	public static final String ACTIONS_DB_NAME = "actionsDB";
	public static final String MODELS_DB_NAME = "modelsDB";
	public static final String MAX_SIZE = "maxSize";
	
	public static final String METAMODEL_FILE_NAME            = "MetaModel.xsd";
	public static final String METAMODEL_PACKAGE_NAME          = "f4g.schemas.java.metamodel";
	public static final String ACTIONS_FILE_NAME               = "Actions.xsd";
	public static final String ACTIONS_PACKAGE_NAME     	   = "f4g.schemas.java.actions";
	public static final String ALLOCATION_FILE_NAME            = "AllocationService.xsd";
	public static final String ALLOCATION_PACKAGE_NAME         = "f4g.schemas.java.allocation";
	
	//XPATH EXPRESSIONS
	public static final String FRAMEWORK_ID_X_PATH_EXPR = "//frameworkID";
	public static final String FRAMEWORK_NAME_X_PATH_EXPR = "//frameworkName";
		
}
