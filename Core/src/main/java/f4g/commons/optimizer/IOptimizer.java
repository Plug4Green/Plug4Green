package f4g.commons.optimizer;

import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.metamodel.Federation;

/**
 * Interface for the f4g Optimizer component
 * 
 * @author FIT4Green
 *
 */
public interface IOptimizer {


	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest Data structure describing the resource allocation request 
	 * @return A data structure representing the result of the allocation
	 */
	public AllocationResponse allocateResource(AllocationRequest allocationRequest, Federation fed);

	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model the f4g model
	 * @return true if successful, false otherwise
	 */
	public boolean performGlobalOptimization(Federation model);
	
	/**
	 * 
	 * This method is called by the core component responsible for starting up and shutting
	 * down the F4G plugin. It must implement all the operations needed to dispose the component
	 * in a clean way (e.g. stopping dependent threads, closing connections, sockets, file handlers, etc.)
	 * 
	 * @return
	 *
	 * @author FIT4Green
	 */
	boolean dispose();

	/**
	 * 
	 * This method allows to set the optimization objective: either Power or CO2
	 *
	 * @author cdupont
	 */
	public void setOptimizationObjective(OptimizationObjective optiObjective);
	

}
