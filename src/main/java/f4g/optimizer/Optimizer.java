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

package org.f4g.optimizer;

import java.util.Map;
import org.apache.log4j.Logger;
import org.f4g.schema.metamodel.*;
import org.f4g.controller.IController;
import org.f4g.core.IMain;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.optimizer.OptimizerEngine;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.power.IPowerCalculator;
import org.f4g.optimizer.HPC.OptimizerEngineHPC;
import org.f4g.optimizer.utils.Utils;
import org.f4g.optimizer.OptimizationObjective;

import java.util.HashMap;
import org.f4g.schema.allocation.*;

/**
 * implementation of the Optimizer
 *
 */
public class Optimizer implements IOptimizer{
	
	public Logger log;  
	IController controller = null;
	IPowerCalculator powerCalculator = null;


	//the three engines for each computing styles are held here
	private Map<DCComputingStyleType, OptimizerEngine> engines;

	public enum CloudTradCS {
		CLOUD,
		TRADITIONAL
	}

		
	public Optimizer(IMain main) {
		
		initialize(main.getController(), main.getPowerCalculator(), new NetworkCost());
	}
	
	public Optimizer(IController myController, IPowerCalculator myPowerCalculator, ICostEstimator costEstimator) {
		
		initialize(myController, myPowerCalculator, costEstimator);
	}

		
	public void initialize(IController myController, IPowerCalculator myPowerCalculator, ICostEstimator costEstimator) {
		
		this.controller = myController;
		this.powerCalculator = myPowerCalculator;
		log = Logger.getLogger(Optimizer.class.getName());
		
		log.debug("Initializing engines...");
		engines = new HashMap<DCComputingStyleType, OptimizerEngine> ();
		
				
		//initialization of the three engines
		engines.put(DCComputingStyleType.SUPER,       new OptimizerEngineHPC(controller, powerCalculator, costEstimator));
		engines.put(DCComputingStyleType.TRADITIONAL, new OptimizerEngineCloudTraditional(controller, powerCalculator, costEstimator, CloudTradCS.TRADITIONAL));
		engines.put(DCComputingStyleType.CLOUD,       new OptimizerEngineCloudTraditional(controller, powerCalculator, costEstimator, CloudTradCS.CLOUD));
		 
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
	public AllocationResponseType allocateResource(AllocationRequestType allocationRequest, FIT4GreenType model){
		
		DatacenterType myDC = Utils.getFirstDatacenter(model);
		
		if(myDC!=null &&
		   myDC.getComputingStyle() != null) {
			
			//choose the engine corresponding to computing style.
			return engines.get(myDC.getComputingStyle() ).allocateResource(allocationRequest, model);
		
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
	public boolean performGlobalOptimization(FIT4GreenType model) {
		
		log.debug("Optimizer: performGlobalOptimization");
		
		DatacenterType myDC = Utils.getFirstDatacenter(model);
		
		if(myDC!=null &&
		   myDC.getComputingStyle() != null) {
			
			//choose the engine corresponding to computing style.
			engines.get(myDC.getComputingStyle() ).performGlobalOptimization(model);
		
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
		for (DCComputingStyleType style : DCComputingStyleType.values()) {
			engines.get(style).setOptiObjective(optiObjective);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.f4g.optimizer.IOptimizer#dispose()
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
