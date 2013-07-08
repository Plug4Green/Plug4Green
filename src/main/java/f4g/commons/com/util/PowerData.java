package f4g.commons.com.util;

/**
 * Mock implementation representing the information on the power consumption
 * 
 * !THIS IS ONLY A SAMPLE IMPLEMENTATION!
 * 
 * @author FIT4Green
 *
 */
public class PowerData {

	private double actualConsumption = 0.0;
	
	public PowerData() {
		
	}
	public double getActualConsumption() {
		return actualConsumption;
	}

	public void setActualConsumption(double actualConsumption) {
		this.actualConsumption = actualConsumption;
	}
}
