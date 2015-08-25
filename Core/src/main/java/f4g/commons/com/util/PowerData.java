package f4g.commons.com.util;

/**
 * Implementation representing the information on the power consumption
 *
 *
 */
public class PowerData {

	private double actualConsumption = 0.0;
	
	public PowerData() {}
	public double getActualConsumption() {
		return actualConsumption;
	}

	public void setActualConsumption(double actualConsumption) {
		this.actualConsumption = actualConsumption;
	}
}
