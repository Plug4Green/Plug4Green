package f4g.f4gGui.gui.shared;

import java.io.Serializable;

public class Status implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public boolean isRunning = false;
	public String statusMessage = "";
	public double powerConsumption = 0.0;
	public boolean error = false;
	public String errorMessage = null;
	public boolean isObjectivePower = true;
}
