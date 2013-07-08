/**
* ============================== Header ============================== 
* file:          DatabaseConfiguration.java
* project:       FIT4Green/Commons
* created:       Sep 30, 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-09-22 17:04:35 +0200 (Thu, 22 Sep 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 807 $
* 
* short description:
*   Collects configuration data for couchDB
* ============================= /Header ==============================
*/
package f4g.commons.core;

/**
 * Collects configuration data for couchDB
 *
 * @author Vasiliki Georgiadou
 */
public class DatabaseConfiguration {
	
	private String databaseUrl;
	private String actionsDatabaseName;
	private String modelsDatabaseName;
	
	public DatabaseConfiguration() {
		this.databaseUrl = "";
		this.actionsDatabaseName ="";
		this.modelsDatabaseName = "";
	}
	
	
	public String getDatabaseUrl() {
		return this.databaseUrl;
	}
	public String getActionsDatabaseName() {
		return actionsDatabaseName;
	}
	public String getModelsDatabaseName() {
		return modelsDatabaseName;
	}

	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
	}

	public void setActionsDatabaseName(String actionsDatabaseName) {
		this.actionsDatabaseName = actionsDatabaseName;
	}

	public void setModelsDatabaseName(String modelsDatabaseName) {
		this.modelsDatabaseName = modelsDatabaseName;
	}

}
