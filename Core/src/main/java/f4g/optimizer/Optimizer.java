/**
* ============================== Header ============================== 
* file:          Optimizer.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate$ by $LastChangedBy$
* revision:      $LastChangedRevision$
* 
* short description:
*   Entry point of the optimizer.
* ============================= /Header ==============================
*/

package f4g.optimizer;
import org.apache.log4j.Logger;

import f4g.schemas.java.metamodel.*;
import f4g.schemas.java.allocation.*;
import f4g.commons.controller.IController;
import f4g.commons.core.IMain;
import f4g.optimizer.cost_estimator.NetworkCost;
import f4g.optimizer.OptimizerEngine;
import f4g.optimizer.cloudTraditional.OptimizerEngineCloudTraditional;
import f4g.commons.power.IPowerCalculator;
import f4g.optimizer.utils.Utils;
import f4g.commons.optimizer.*;



/**
 * implementation of the Optimizer
 *
 */
public class Optimizer implements IOptimizer{
	
    	private final Logger log = Logger.getLogger(getClass()); 
	IController controller = null;
	IPowerCalculator powerCalculator = null;

	private OptimizerEngine engine;

		
	public Optimizer(IMain main) {
		
		initialize(main.getController(), main.getPowerCalculator(), new NetworkCost());
	}
	
	public Optimizer(IController myController, IPowerCalculator myPowerCalculator, ICostEstimator costEstimator) {
		
		initialize(myController, myPowerCalculator, costEstimator);
	}

		
	public void initialize(IController myController, IPowerCalculator myPowerCalculator, ICostEstimator costEstimator) {
		
		this.controller = myController;
		this.powerCalculator = myPowerCalculator;
				
				
		//initialization of the three engines
		engine = new OptimizerEngineCloudTraditional(controller, powerCalculator, costEstimator);
		 
		//default objective to power
		setOptimizationObjective(OptimizationObjective.Power);
	}
	



	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest Data structure describing the resource allocation request 
	 * @return A data structure representing the result of the allocation
	 */
	@Override
	public AllocationResponse allocateResource(AllocationRequest allocationRequest, FIT4Green model){
		
		Datacenter myDC = Utils.getFirstDatacenter(model);
		
		if(myDC!=null) {
			
			//choose the engine corresponding to computing style.
			return engine.allocateResource(allocationRequest, model);
		
		} else {
			log.error("performGlobalOptimization: no datacenter or no computing style inside the model");
			return null;
		}
		
				
	}
	
	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model the f4g model
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean performGlobalOptimization(FIT4Green model) {
		
		log.debug("Optimizer: performGlobalOptimization");
		
		Datacenter myDC = Utils.getFirstDatacenter(model);
		
		if(myDC!=null) {
			
			//choose the engine corresponding to computing style.
			engine.performGlobalOptimization(model);
		
			return true;
		} else {
			log.error("performGlobalOptimization: no datacenter or no computing style inside the model");
			return false;
		}
			
		
	}

	
	/**
	 * set the optimization objective for the 3 computing styles
	 */
	public void setOptimizationObjective(OptimizationObjective optiObjective) {
			engine.setOptiObjective(optiObjective);
	}
	
	/* (non-Javadoc)
	 * @see f4g.optimizer.IOptimizer#dispose()
	 */
	@Override
	public boolean dispose() {

		//optimizer is pure: nothing to do.
		return true;
	}		
	

	public IPowerCalculator getPowerCalculator() {
		return powerCalculator;
	}

	public void setPowerCalculator(IPowerCalculator powerCalculator) {
		this.powerCalculator = powerCalculator;
	}
}
