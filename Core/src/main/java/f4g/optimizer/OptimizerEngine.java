/**
* ============================== Header ============================== 
* file:          OptimizerEngine.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate$ by $LastChangedBy$
* revision:      $LastChangedRevision$
* 
* short description:
*   This class contains all common code between the 3 optimization engines.
*   
* ============================= /Header ==============================
*/

package f4g.optimizer;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.metamodel.*;
import org.apache.log4j.Logger;

import f4g.commons.controller.IController;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.actions.AbstractBaseAction;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.rits.cloning.Cloner;


/**
 * The Optimizer Engine.
 * This class contains all common code between the 3 optimization engines.
 * 
 * @author cdupont
 *
 */
public abstract class OptimizerEngine implements Runnable  {

	private Federation globalOptimizationRequest;
	private Thread engineThread;
	
	public Logger log;  
	
	protected IController controller = null;
	protected IPowerCalculator powerCalculator = null;
	protected ICostEstimator costEstimator = null;

	/**
	 * optimization objective: either Power or CO2
	 */
	protected OptimizationObjective optiObjective = null;

	
	/**
	 * constructor
	 */
	public OptimizerEngine(IController controller, IPowerCalculator powerCalculator, ICostEstimator costEstimator) {
		
		this.controller = controller;
		this.powerCalculator = powerCalculator;
		this.costEstimator = costEstimator;
		log = Logger.getLogger(OptimizerEngine.class.getName()); 

		//default objective is Power.
		//it can be overriden with the setter.
		optiObjective = OptimizationObjective.Power;
	}
	
	
	/**
	 * Handles a request for resource allocation
	 * 
	 * @param allocationRequest Data structure describing the resource allocation request 
	 * @return A data structure representing the result of the allocation
	 */
	public abstract AllocationResponse allocateResource(AllocationRequest allocationRequest, Federation model);
	
	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model the f4g model
	 * @return true if successful, false otherwise
	 */
	public void performGlobalOptimization(Federation model) {
		
		log.debug("OptimizerEngine: performGlobalOptimization: starting global optimization thread run.");
		
		globalOptimizationRequest = model;
		
		engineThread = new Thread(this);
		engineThread.start();

	}

	/**
	 * Thread running.
	 * This function is called whenever you start the thread.
	 * It handle the global optimization.  
	 * 
	 * @param none
	 * @return none
	 */
	@Override
	public void run() {
		
		log.debug("OptimizerEngine: run: starting global optimization calculation.");

		runGlobalOptimization(globalOptimizationRequest);
		
		log.debug("OptimizerEngine: run: optimization calculation finished.");
	    
	}
	
	/**
	 * Perform the actual optimization calculus when then thread is run
	 * 
	 * @param model the f4g model
	 * @return the f4g model
	 */
	public abstract void runGlobalOptimization(Federation model);
	


	/**
	 * performs the moves in a data center
	 * 
	 */
	protected Federation performMoves(List<AbstractBaseAction> moves, Federation federation) {

		Cloner cloner = new Cloner();
		Federation newfederation = cloner.deepClone(federation);
		
		for (AbstractBaseAction move : moves){
			ServerName source = new ServerName();
			ServerName dest = new ServerName();
			VirtualMachineName virtualMachine  = new VirtualMachineName();
			
			if (move instanceof MoveVMAction) {
				source = ((MoveVMAction)move).getSourceServer();
				dest = ((MoveVMAction)move).getDestServer();
				virtualMachine = ((MoveVMAction)move).getVirtualMachine();
			} else if (move instanceof LiveMigrateVMAction) {
				source = ((LiveMigrateVMAction)move).getSourceServer();
				dest = ((LiveMigrateVMAction)move).getDestServer();
				virtualMachine = ((LiveMigrateVMAction)move).getVirtualMachine();
			}	
			
			Server oldServer = Utils.findServerByName(newfederation, source);
			Server newServer = Utils.findServerByName(newfederation, dest);
			
			VirtualMachine VM = Utils.findVirtualMachineByName(oldServer.getVMs(), virtualMachine);
					
			//remove the VM from source server
			List<VirtualMachine> oldServerVMs = oldServer.getVMs();
			if(oldServerVMs != null)
				oldServerVMs.remove(VM);
			else
				log.error("performMoves: No hypervisor found in the source server!");

			//add VM in the destination server
			List<VirtualMachine> newServerVMs = newServer.getVMs();
			if(newServerVMs != null)
				newServerVMs.add(VM);
			else
				log.error("performMoves: No hypervisor found in the destination server!");
		}
		
		return newfederation;
	
	}
	
	protected Federation performOnOffs(List<PowerOnAction> ons, List<PowerOffAction> offs, Federation federation){
		return performOffs(offs, performOns(ons, federation));
	}
	
	
	
	/**
	 * performs the switchs on in a data center
	 * 
	 */
	protected Federation performOns(List<PowerOnAction> ons, Federation federation) {

		Cloner cloner=new Cloner();
		Federation newFederation = cloner.deepClone(federation);
		
		for (PowerOnAction on : ons){
			
			Server server = Utils.findServerByName(newFederation, on.getServerName());
			server.setStatus(ServerStatus.ON);
		}
		return newFederation;
	}

	
	/**
	 * performs the switchs off in a data center
	 * 
	 */
	protected Federation performOffs(List<PowerOffAction> offs, Federation federation) {

		Cloner cloner=new Cloner();
		Federation newFederation = cloner.deepClone(federation);
		
		for (PowerOffAction off : offs){
			
			Server server = Utils.findServerByName(newFederation, off.getNodeName());
			server.setStatus(ServerStatus.OFF);
		}
		return newFederation;
	}
	
	
	/**
	 * create an action list from switchs on, off and moves.
	 * 
	 */
	ActionRequest.ActionList createActionList(List<PowerOnAction> ons, List<PowerOffAction> offs, List<MoveVMAction> moves){
		//Create action list
		ActionRequest.ActionList actionList = new ActionRequest.ActionList();
		actionList.getAction();
		ObjectFactory actionFactory = new ObjectFactory();
		
		for (PowerOffAction off : offs)
			actionList.getAction().add(actionFactory.createPowerOff(off));
		
		for (PowerOnAction on : ons)
			actionList.getAction().add(actionFactory.createPowerOn(on));
		
		for (MoveVMAction move : moves)
			actionList.getAction().add(actionFactory.createMoveVM(move));
		
		return actionList;
		
	}

	/**
	 * finds a server name by its ID
	 * 
	 */
	protected Server findServerByName(Datacenter datacenter, final ServerName name) {
		
		Iterator<Server> it = datacenter.getServers().iterator();
		Predicate<Server> isID = new Predicate<Server>() {
	        @Override public boolean apply(Server s) {
	            return s.getFrameworkID().equals(name);
	        }               
	    };
	
		return Iterators.find(it, isID);		
	}
	


	/**
	 * fills an action request.
	 */
	protected ActionRequest getActionRequest(ActionRequest.ActionList actionList, Federation fedBefore, Federation fedAfter) {
		
		//Create action requests
		ActionRequest actionRequest = new ActionRequest();
		
//		double powerBefore = powerCalculator.computePowerFIT4Green(fedBefore).getActualConsumption();
//		double powerAfter = powerCalculator.computePowerFIT4Green(fedAfter).getActualConsumption();
//		double powerSaved = powerAfter - powerBefore;
//		log.debug("powerSaved: " + powerSaved);
		
		try {
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
			actionRequest.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
			actionRequest.setActionList(actionList);
//			actionRequest.setComputedPowerBefore(new Power(powerBefore));
//			actionRequest.setComputedPowerAfter(new Power(powerAfter));
		} catch (DatatypeConfigurationException e) {
			log.debug("Error in date");
		}
		return actionRequest;
	}

	/**
	 *  get the optimization objective
	 */
	public OptimizationObjective getOptiObjective() {
		return optiObjective;
	}
	
	/**
	 *  set the optimization objective (Power or CO2)
	 */
	public void setOptiObjective(OptimizationObjective optiObjective) {
		this.optiObjective = optiObjective;
	}

	/**
	 *  get the controller
	 */
	public IController getController() {
		return controller;
	}


	/**
	 *  set the controller
	 */
	public void setController(IController controller) {
		this.controller = controller;
	}

	/**
	 *  get the power calculator
	 */
	public IPowerCalculator getPowerCalculator() {
		return powerCalculator;
	}

	/**
	 *  set the power calculator
	 */
	public void setPowerCalculator(IPowerCalculator powerCalculator) {
		this.powerCalculator = powerCalculator;
	}
	
	/**
	 *  get the cost estimator
	 */
	public ICostEstimator getCostEstimator() {
		return costEstimator;
	}

	/**
	 *  set the cost estimator
	 */
	public void setCostEstimator(ICostEstimator costEstimator) {
		this.costEstimator = costEstimator;
	}

}
