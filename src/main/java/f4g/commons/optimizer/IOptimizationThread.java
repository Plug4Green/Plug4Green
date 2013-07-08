package f4g.commons.optimizer;

import f4g.schemas.java.metamodel.FIT4GreenType;

/**
 * Interface to be implemented by the component performing optimization
 * 
 * @author FIT4Green
 *
 */
public interface IOptimizationThread extends Runnable {

	/**
	 * The optimize() method must:
	 * <ul>calculate which is the best configuration for the system according to the current context
	 * <ul>generate a list of actions to be performed on the system in order to reach such optimal state
	 * <ul>forward the actions to be performed to the Controller
	 */
	abstract void optimize();
	
}
