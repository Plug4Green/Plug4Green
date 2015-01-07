
package f4g.optimizer.entropy.plan.action;

import org.btrplace.plan.event.Action;

/**
 * Interface that define a driver to perform an action.
 * 
 * @author Fabien Hermenier
 * 
 */
public abstract class Driver {
	
	/**
	 * The action to execute.
	 */
	private Action action;
	
	/**
	 * Get the action to execute.
	 * @return an action
	 */
	public Action getAction() {
		return this.action;
	}
	
	/**
	 * Create and configure the driver to execute an action.
	 * @param a the action to execute	 
	 */
	public Driver(Action a) {
		this.action = a;
	}
	
	/**
	 * Execute the action.
	 * @throws DriverException if an error occurred during the execution
	 */
	public abstract void execute();
	
	/**
	 * Textual representation of the driver.
	 * @return a String!
	 */
	public abstract String toString();
}
