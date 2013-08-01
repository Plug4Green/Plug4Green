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


import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;
import f4g.commons.controller.IController;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.DatacenterType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.AllocationRequestType;
import f4g.schemas.java.AllocationResponseType;


import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.rits.cloning.Cloner;
import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;
import javax.measure.quantity.*;
import static javax.measure.unit.SI.*;


/**
 * The Optimizer Engine.
 * This class contains all common code between the 3 optimization engines.
 * 
 * @author cdupont
 *
 */
public abstract class OptimizerEngine implements Runnable  {

	private FIT4GreenType globalOptimizationRequest;
	private Thread engineThread;
	
	public Logger log;  
	
	/**
	 * access to controller
	 */
	protected IController controller = null;
	

	/**
	 * access to power calculator
	 */
	protected IPowerCalculator powerCalculator = null;



	/**
	 * access to cost estimator
	 */
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
	public abstract AllocationResponseType allocateResource(AllocationRequestType allocationRequest, FIT4GreenType model);
	
	/**
	 * Handles a request for a global optimization
	 * 
	 * @param model the f4g model
	 * @return true if successful, false otherwise
	 */
	public void performGlobalOptimization(FIT4GreenType model) {
		
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
	public abstract void runGlobalOptimization(FIT4GreenType model);
	


	/**
	 * performs the moves in a data center
	 * 
	 */
	protected FIT4GreenType performMoves(List<AbstractBaseActionType> moves, FIT4GreenType federation) {

		Cloner cloner = new Cloner();
		FIT4GreenType newfederation = cloner.deepClone(federation);
		
		for (AbstractBaseActionType move : moves){
			String source = "";
			String dest = "";
			String virtualMachine = "";
			
			if (move instanceof MoveVMActionType) {
				source = ((MoveVMActionType)move).getSourceNodeController();	
				dest = ((MoveVMActionType)move).getDestNodeController();
				virtualMachine = ((MoveVMActionType)move).getVirtualMachine();
			} else if (move instanceof LiveMigrateVMActionType) {
				source = ((LiveMigrateVMActionType)move).getSourceNodeController();	
				dest = ((LiveMigrateVMActionType)move).getDestNodeController();
				virtualMachine = ((LiveMigrateVMActionType)move).getVirtualMachine();
			}	
			
			ServerType oldServer = Utils.findServerByName(newfederation, source);
			ServerType newServer = Utils.findServerByName(newfederation, dest);
			
			VirtualMachineType VM = Utils.findVirtualMachineByName(Utils.getVMs(oldServer), virtualMachine);
					
			//remove the VM from source server
			List<VirtualMachineType> oldServerVMs = getVMList(oldServer);
			if(oldServerVMs != null)
				oldServerVMs.remove(VM);
			else
				log.error("performMoves: No hypervisor found in the source server!");

			//add VM in the destination server
			List<VirtualMachineType> newServerVMs = getVMList(newServer);
			if(newServerVMs != null)
				newServerVMs.add(VM);
			else
				log.error("performMoves: No hypervisor found in the destination server!");
		}
		
		return newfederation;
	
	}
	
	protected FIT4GreenType performOnOffs(List<PowerOnActionType> ons, List<PowerOffActionType> offs, FIT4GreenType federation){
		return performOffs(offs, performOns(ons, federation));
	}
	
	
	
	/**
	 * performs the switchs on in a data center
	 * 
	 */
	protected FIT4GreenType performOns(List<PowerOnActionType> ons, FIT4GreenType federation) {

		Cloner cloner=new Cloner();
		FIT4GreenType newFederation = cloner.deepClone(federation);
		
		for (PowerOnActionType on : ons){
			
			ServerType server = Utils.findServerByName(newFederation, on.getNodeName());
			server.setStatus(ServerStatusType.ON);
		}
		return newFederation;
	}

	
	/**
	 * performs the switchs off in a data center
	 * 
	 */
	protected FIT4GreenType performOffs(List<PowerOffActionType> offs, FIT4GreenType federation) {

		Cloner cloner=new Cloner();
		FIT4GreenType newFederation = cloner.deepClone(federation);
		
		for (PowerOffActionType off : offs){
			
			ServerType server = Utils.findServerByName(newFederation, off.getNodeName());
			server.setStatus(ServerStatusType.OFF);
		}
		return newFederation;
	}
	
	
	/**
	 * create an action list from switchs on, off and moves.
	 * 
	 */
	ActionRequestType.ActionList createActionList(List<PowerOnActionType> ons, List<PowerOffActionType> offs, List<MoveVMActionType> moves){
		//Create action list
		ActionRequestType.ActionList actionList = new ActionRequestType.ActionList();
		actionList.getAction();
		
		org.f4g.schema.actions.ObjectFactory actionFactory = new org.f4g.schema.actions.ObjectFactory();
		
		
		for (PowerOffActionType off : offs)
			actionList.getAction().add(actionFactory.createPowerOff(off));
		
		for (PowerOnActionType on : ons)
			actionList.getAction().add(actionFactory.createPowerOn(on));
		
		for (MoveVMActionType move : moves)
			actionList.getAction().add(actionFactory.createMoveVM(move));
		
		return actionList;
		
	}
	
	/**
	 * returns the first list or VMs available in the server.
	 * //TODO fix: how to determine where to add the VM?
	 */
	public static List<VirtualMachineType> getVMList(ServerType server){
			
		if (server.getNativeHypervisor() != null)
			return server.getNativeHypervisor().getVirtualMachine();
		
		else if (server.getNativeOperatingSystem() != null &&
				server.getNativeOperatingSystem().getHostedHypervisor().size() != 0) 
			return server.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine();

		else
			return null;
	}
	
	/**
	 * finds a server name by its ID
	 * 
	 */
	protected ServerType findServerByName(DatacenterType datacenter, final String frameWorkID) {
		
		Iterator<ServerType> it = Utils.getAllServers(datacenter).iterator();
		Predicate<ServerType> isID = new Predicate<ServerType>() {
	        @Override public boolean apply(ServerType s) {
	            return s.getFrameworkID().equals(frameWorkID);
	        }               
	    };
	
		return Iterators.find(it, isID);		
	}
	


	/**
	 * fills an action request.
	 */
	protected ActionRequestType getActionRequest(ActionRequestType.ActionList actionList, FIT4GreenType fedBefore, FIT4GreenType fedAfter) {
		
		//Create action requests
		ActionRequestType actionRequest = new ActionRequestType();
		
		double powerBefore = powerCalculator.computePowerFIT4Green(fedBefore).getActualConsumption();
		double powerAfter = powerCalculator.computePowerFIT4Green(fedAfter).getActualConsumption();
		double powerSaved = powerAfter - powerBefore;
		log.debug("powerSaved: " + powerSaved);
		
		try {
			GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
			actionRequest.setDatetime(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
			actionRequest.setActionList(actionList);
			actionRequest.setComputedPowerBefore(new PowerType(powerBefore));
			actionRequest.setComputedPowerAfter(new PowerType(powerAfter));
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
