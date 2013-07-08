package f4g.f4gGui.gui.shared;

import java.io.Serializable;

public class ConfigurationData implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String urlDB;
	private String actionsDB;
	private String modelsDB;
	
	
	public String getUrlDB() {
		return urlDB;
	}
	public void setUrlDB(String urlDB) {
		this.urlDB = urlDB;
	}
	public String getActionsDB() {
		return actionsDB;
	}
	public void setActionsDB(String actionsDB) {
		this.actionsDB = actionsDB;
	}
	public String getModelsDB() {
		return modelsDB;
	}
	public void setModelsDB(String modelsDB) {
		this.modelsDB = modelsDB;
	}
	
}
