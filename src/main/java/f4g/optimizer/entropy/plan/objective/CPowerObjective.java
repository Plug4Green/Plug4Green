package f4g.optimizer.entropy.plan.objective;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Constraint;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.actionModel.NodeActionModel;
import btrplace.solver.choco.constraint.CObjective;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;
import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.objective.api.PowerObjective;
import f4g.optimizer.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;


import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.Solver;

import solver.variables.VariableFactory;
import solver.constraints.IntConstraintFactory;
import static solver.variables.VariableFactory.*;
import static solver.constraints.IntConstraintFactory.*;

public class CPowerObjective implements CObjective {
  
    //objective: either Power or CO2
	//OptimizationObjective optiObjective;
	    
    int reconfTime = 1000; // seconds (must be multiple of 1000s)
    int EnergyOn = 1; //KJoule, sample value
    int EnergyOff = 1; //KJoule, sample value
    int EnergyMove = 1; //KJoule, sample value
    
    class SwitchOnOffs {
       BoolVar[] switchOns;
       BoolVar[] switchOffs;
    }
    
    /**
     * Make a new constraint.
     *
     * @param vms A non-empty set of virtual machines
     */
    public CPowerObjective() {
    }
    


	@Override
    public boolean inject(ReconfigurationProblem rp) {

    	Solver solver = rp.getSolver();
    	IntVar reconfEnergy = VariableFactory.bounded("reconfEnergy", 0, Integer.MAX_VALUE / 100, solver); //In Watts
    	IntVar globalPower = VariableFactory.bounded("globalPower", 0, Integer.MAX_VALUE / 100, solver); //In Watts 
    	IntVar stableEnergy = VariableFactory.bounded("stableEnergy", 0, Integer.MAX_VALUE / 100, solver);  //in KJ
    	
    	//sum the power Idle, the power for the VMs and extra power for the network
    	IntVar[] pows = new  IntVar[]{getPVMs(rp)}; //, getPIdle(rp)}; //, getPNetwork(m)
        solver.post(sum(pows, globalPower));
        
        solver.post(times(globalPower, fixed((int)reconfTime/1000, solver), stableEnergy));
    	//this equation represents the energy spent between two reconfigurations (that we'll try to minimize).
    	IntVar[] energies = new  IntVar[]{stableEnergy}; //, getEMove(rp), getEOnOff(rp)};
    	solver.post(sum(energies, reconfEnergy));
     
        rp.setObjective(true, reconfEnergy);
        return true;
    }
  	
    public IntVar getPIdle(ReconfigurationProblem rp) {

    	PowerView powerIdleView = (PowerView) rp.getSourceModel().getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_IDLES);
    	
    	Solver solver = rp.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, rp.getNodes()); 
    	int[] powerIdles =  ArrayUtils.toPrimitive(powerIdleView.getPowers(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PIdleTotal = bounded("PIdleTotal", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(scalar(getStates(rp), powerIdles, PIdleTotal));
        
        return PIdleTotal; 
    }


    public IntVar getPVMs(ReconfigurationProblem rp) {

    	PowerView powerPerVMView = (PowerView) rp.getSourceModel().getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_PER_VM);

    	Solver solver = rp.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, rp.getNodes()); 
    	int[] powerperVMs =  ArrayUtils.toPrimitive(powerPerVMView.getPowers(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PowerperVMsTotal = bounded("PowerperVMsTotal", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(scalar(rp.getNbRunningVMs(), powerperVMs, PowerperVMsTotal));
        
    	System.out.print(PowerperVMsTotal);
        return PowerperVMsTotal; 
    }

    public IntVar getEMove(ReconfigurationProblem rp) {
              
    	Solver solver = rp.getSolver();
    	int[] EMoveperVMs = new int[rp.getVMs().length];
        Arrays.fill(EMoveperVMs, EnergyMove); 
        
    	IntVar EMove = bounded("EMove", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(scalar(getMoves(rp), EMoveperVMs, EMove));
        return EMove;               
    }
    
    //energy spent to switch on and off machines
    public IntVar getEOnOff(ReconfigurationProblem rp) {
        
    	Solver solver = rp.getSolver();
    	int[] EOn = new int[rp.getNodes().length];
        Arrays.fill(EOn, EnergyOff); 
        int[] EOff = new int[rp.getNodes().length];
        Arrays.fill(EOff, EnergyOn); 
    	
        Pair<BoolVar[], BoolVar[]> switchs = getSwitchOnOffs(rp);
        
		IntVar EOnTot = bounded("EOnTot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(scalar(switchs.getFirst(), EOn, EOnTot));
    	IntVar EOffTot = VariableFactory.bounded("EOffTot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(scalar(switchs.getSecond(), EOff, EOffTot));
    	IntVar ETot = bounded("ETot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(IntConstraintFactory.sum(new IntVar[]{EOnTot,EOffTot}, ETot));
    	return ETot;
    }
    
    
    public Pair<BoolVar[], BoolVar[]> getSwitchOnOffs(ReconfigurationProblem rp) {
    	
    	BoolVar[] switchOns  = new BoolVar[rp.getNodes().length];
        BoolVar[] switchOffs = new BoolVar[rp.getNodes().length];
        
        int i = 0;
		for (Node node : rp.getNodes()) {
			BoolVar futurState = rp.getNodeAction(node).getState();
			//if the node if offline, it will be switched on if the future state is on, and vice-versa
			if(rp.getSourceModel().getMapping().isOffline(node)) {
				switchOns[i] = futurState;
				switchOffs[i] = zero(rp.getSolver());
			} else	{
				switchOffs[i] = not(futurState);
				switchOns[i] = zero(rp.getSolver());
			}
		   i++;
		}
		return new Pair(switchOns, switchOffs); 
    	
    }
    
    public BoolVar[] getStates(ReconfigurationProblem rp) {

		BoolVar[] states = new BoolVar[rp.getNodes().length];
    	int i = 0;
		for (NodeActionModel action : rp.getNodeActions()) {     
		   states[i] = action.getState();
		   i++;
		}
		return states;
	}
    
    public BoolVar[] getMoves(ReconfigurationProblem rp) {
    	Solver solver = rp.getSolver();
    	BoolVar moves[] = new BoolVar[rp.getVMs().length];
        int i = 0;
        for (VM vm : rp.getVMs()) {        	
          	//A boolean variable to indicate whether the node is used or not
            moves[i] = bool("moves(" + vm.id() + ")", solver);
            IntVar hoster = rp.getVMAction(vm).getDSlice().getHoster();
            moves[i] = arithm(hoster, "=", rp.getCurrentVMLocation(i)).reif();
            i++;
        }
        return moves;
	}   
    
    //Computes the power of the network
//    public IntVar getPNetwork(ReconfigurationProblem m) {
//    	
//    	IntDomainVar PNetwork = m.createBoundIntVar("PNetwork",0, Choco.MAX_UPPER_BOUND);
//    	
//		//For each switch, we define a boolean domain denoting if the switch is on or off
//        IntDomainVar[] switchON = new IntDomainVar[numberOfSwitches];
//                
//    	for (int i = 0; i < numberOfSwitches; i++) {
//    		//create the boolean domain variables
//    		switchON[i]  = m.createBooleanVar("switchON(" + i + ")");
//    	}
//    	
//    	//get all the Entropy nodes
//        ManagedElementSet<Node> nodes = m.getSourceConfiguration().getAllNodes();
//        
//    	for (int i = 0; i < nodes.size(); ++i) {    		
//    		for (int j=0; j<nodeSwitches[i].size(); ++j) {
//        		//get the index of the switch attached to this server
//        		int indexSwitch = nodeSwitches[i].get(j);        			        			
//        		ManageableNodeActionModel action = (ManageableNodeActionModel) m.getAssociatedAction(nodes.get(i));    
//        		m.post(ReifiedFactory.builder(switchON[indexSwitch], or(m.getEnvironment(), switchON[indexSwitch], action.getState()), m));  
//    		}
//    	}       
//    	
//    	//the power of all the switch is egal to the scalar product of these two vector
//        //TODO quality oriented constraint
//        SConstraint s = m.eq(m.scalar(switchON, powerPerSwitch), PNetwork);
//        qualityConstraints.add(s);
//        //m.post(s);
//    	return PNetwork;
//    }
 

	@Override
	public Set<VM> getMisPlacedVMs(Model m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void postCostConstraints() {
		// TODO Auto-generated method stub
		
	}
	
	 /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return PowerObjective.class;
        }

        @Override
        public CPowerObjective build(Constraint cstr) {
            return new CPowerObjective();
        }
    }
}
