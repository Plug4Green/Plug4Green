/**
* ============================== Header ============================== 
* file:          F4GRoot.java
* project:       FIT4Green/Optimizer
* created:       07.10.2011 by ts
* last modified: $LastChangedDate$ by $LastChangedBy$
* revision:      $LastChangedRevision$
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.entropy.plan.constraint;

import static javax.measure.units.SI.*;

import javax.measure.quantities.*;
import org.apache.log4j.Logger;
import org.f4g.optimizer.ICostEstimator;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType.VMType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.util.StaticPowerCalculation;
import org.f4g.util.Util;
import org.jscience.physics.measures.Measure;

import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.vjob.*;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GVMPaybackTimeConstraint extends F4GConstraint {

	
	Logger log;
    private ManagedElementSet<VirtualMachine> vms;
    private Measure<Duration> paybackTime;
    FIT4GreenType model;
    StaticPowerCalculation powerCalculation;
    IPowerCalculator powerCalculator;
    private ICostEstimator netCost; 
    VMTypeType SLAVMs;
    
    /**
     * Make a new constraint.
     *
     * @param v the VMs to consider.
     */
    public F4GVMPaybackTimeConstraint(ManagedElementSet<VirtualMachine> MyVMs, int myPaybackTime, FIT4GreenType myModel, 
    		VMTypeType mySLAVMs, IPowerCalculator myPowerCalculator, ICostEstimator myNetCost) {
    	log = Logger.getLogger(this.getClass().getName());
    	vms = MyVMs;
    	paybackTime = Measure.valueOf(myPaybackTime*60, SECOND);
    	model = myModel;
    	powerCalculation = new StaticPowerCalculation(mySLAVMs);
    	powerCalculator = myPowerCalculator;
    	netCost = myNetCost;
    	SLAVMs = mySLAVMs;
    }

    /**
     * injects the constraint into the engine 
     */
    @Override
    public void inject(ReconfigurationProblem core) {
    	log.debug("injecting VM Payback Time Constraint with " + paybackTime.doubleValue(SECOND));
        try {
			for (VirtualMachine vm : vms) {
				
				Node origin = core.getSourceConfiguration().getLocation(vm);
				ManagedElementSet<Node> nodes = core.getSourceConfiguration().getAllNodes();
				
			    VirtualMachineActionModel a = core.getAssociatedAction(vm);
			    if (a != null) {                
			        IntDomainVar hoster = a.getDemandingSlice().hoster();                               
			        
			        for(Node dest : nodes) {
			        	int host = nodes.indexOf(dest);
			        	if(origin != dest && hoster.canBeInstantiatedTo(host)) {
			        		Measure<Power> Psaved = powerSavedByMove(vm, origin, dest);
			        		log.debug("VM " + vm.getName() + " origin " + origin.getName() + " dest " + dest.getName());
			        		log.debug("Psaved=" + Psaved.doubleValue(WATT));
			        		Measure<Energy> Emove = energyCostOfMove(vm, origin, dest);
			        		log.debug("Emove=" + Emove.doubleValue(JOULE));
			        		
			        		Measure<Energy> ePayback = (Measure<Energy>) Psaved.times(paybackTime);
			        		
			        		if(ePayback.isLessThan(Emove)) {
			        			log.debug("move forbidden, suppressed from search space");
			        			try {
									hoster.remVal(host);
								} catch (ContradictionException e1) {
									e1.printStackTrace();
								}
			        		}
			        	}
			        }               
			        
			    }
			}
		} catch (Exception e) {
		}
    }

    /**
     * Power saved by the move 
     */
    public Measure<Power> powerSavedByMove(VirtualMachine vm, Node from, Node to) {
    	ServerType fromServer = Utils.findServerByName(model, from.getName());
    	ServerType toServer = Utils.findServerByName(model, to.getName());
    	int numberOfVMs = Utils.getVMs(fromServer).size();
    	log.debug("Number of VMs on origin server:" + numberOfVMs);
    	VirtualMachineType myVM = Utils.findVirtualMachineByName(model, vm.getName());
    	    	
    	VMType slaVM = Util.findVMByName(myVM.getCloudVmType(), SLAVMs);
		double PBefore = powerCalculation.computePowerForVM(fromServer, slaVM, powerCalculator);
		double POriginServer = powerCalculation.computePowerIdle(fromServer, powerCalculator);
		double PAfter = powerCalculation.computePowerForVM(toServer, slaVM, powerCalculator);
		
		//We consider in the calculus of the power saved that the server will be switched off.
		return Measure.valueOf(POriginServer/numberOfVMs + PBefore - PAfter, WATT);
    }
    
    public Measure<Energy> energyCostOfMove(VirtualMachine vm, Node from, Node to) {
    	ServerType fromServer = Utils.findServerByName(model, from.getName());
    	ServerType toServer = Utils.findServerByName(model, to.getName());
    	
    	if(fromServer.getMainboard().size() == 0 || fromServer.getMainboard().get(0).getEthernetNIC().size() == 0) {
			log.error("F4GVMPaybackTime constraint activated but servers " + fromServer.getFrameworkID() + " does not have a EthernetNIC");
    		throw(new NullPointerException());
    	}    	
    	if(toServer.getMainboard().size() == 0 || toServer.getMainboard().get(0).getEthernetNIC().size() == 0) {
			log.error("F4GVMPaybackTime constraint activated but servers " + toServer.getFrameworkID() + " does not have a EthernetNIC");
    		throw(new NullPointerException());
    	}
    	
    	NetworkNodeType fromNode = fromServer.getMainboard().get(0).getEthernetNIC().get(0);
    	NetworkNodeType toNode = toServer.getMainboard().get(0).getEthernetNIC().get(0);
    	
    	VirtualMachineType myVM = Utils.findVirtualMachineByName(model, vm.getName());
    	    	
		return netCost.moveEnergyCost(fromNode, toNode, myVM, model);
    }
    
    
    /**
     * Entailed method
     *
     * @param configuration the configuration to check
     * @return {@code true}
     */
    @Override
    public boolean isSatisfied(Configuration configuration) {
        return true;
    }

    /**
     * Get the VMs involved in the constraint.
     *
     * @return a set of virtual machines, should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return new SimpleManagedElementSet<Node>();
    }

    /**
     * Entailed method. No VMs may be misplaced without consideration of the reconfiguration plan.
     *
     * @param configuration the configuration to check
     * @return an empty set
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    @Override
    public String toString() {
        return new StringBuilder("F4GVMPaybackTimeConstraint(").append(getAllVirtualMachines()).append(")").toString();
    }

	
}
