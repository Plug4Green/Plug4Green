package f4g.optimizer.entropy.plan.objective;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.actionModel.NodeActionModel;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import solver.constraints.IntConstraintFactory;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import solver.Solver;

public class PowerObjective2 implements btrplace.solver.choco.constraint.CObjective {
  
	PowerView powers;
	
    //objective: either Power or CO2
	OptimizationObjective optiObjective;
	    
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
    public PowerObjective2(PowerView powers, OptimizationObjective optiObjective) {
    	
    	this.powers = powers;    	
    	this.optiObjective = optiObjective;
    }
    

    @Override
    public boolean inject(ReconfigurationProblem m) {

    	Solver solver = m.getSolver();
    	IntVar reconfEnergy = VariableFactory.bounded("reconfEnergy", 0, Integer.MAX_VALUE / 100, solver); //In Watts
    	IntVar globalPower = VariableFactory.bounded("globalPower", 0, Integer.MAX_VALUE / 100, solver); //In Watts 
    	IntVar stableEnergy = VariableFactory.bounded("stableEnergy", 0, Integer.MAX_VALUE / 100, solver);  //in KJ
    	
    	//sum the power Idle, the power for the VMs and extra power for the network
    	IntVar[] pows = new  IntVar[]{getPIdle(m), getPVMs(m)}; //, getPNetwork(m)
        solver.post(IntConstraintFactory.sum(pows, globalPower));
        
    	solver.post(IntConstraintFactory.arithm(stableEnergy, "=", globalPower, "*", (int)reconfTime/1000));
    	//this equation represents the energy spent between two reconfigurations (that we'll try to minimize).
    	IntVar[] energies = new  IntVar[]{stableEnergy, getEMove(m), getEOnOff(m)};
    	solver.post(IntConstraintFactory.sum(energies, reconfEnergy));
     
        m.setObjective(true, reconfEnergy);
        return true;
    }
  	
    public IntVar getPIdle(ReconfigurationProblem m) {

    	Solver solver = m.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, m.getNodes()); 
    	int[] powerIdles =  ArrayUtils.toPrimitive(powers.getPowerIdles(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PIdleTotal = VariableFactory.bounded("PIdleTotal", 0, Integer.MAX_VALUE / 100, solver);
        IntConstraintFactory.scalar(getStates(m), powerIdles, PIdleTotal);
        
        return PIdleTotal; 
    }


    public IntVar getPVMs(ReconfigurationProblem m) {

    	Solver solver = m.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, m.getNodes()); 
    	int[] powerperVMs =  ArrayUtils.toPrimitive(powers.getPowerperVMs(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PowerperVMsTotal = VariableFactory.bounded("PowerperVMsTotal", 0, Integer.MAX_VALUE / 100, solver);
        IntConstraintFactory.scalar(m.getNbRunningVMs(), powerperVMs, PowerperVMsTotal);
        
        return PowerperVMsTotal; 
    }

    public IntVar getEMove(ReconfigurationProblem m) {
              
    	Solver solver = m.getSolver();
    	int[] EMoveperVMs = new int[m.getVMs().length];
        Arrays.fill(EMoveperVMs, EnergyMove); 
        
    	IntVar EMove = VariableFactory.bounded("EMove", 0, Integer.MAX_VALUE / 100, solver);
    	IntConstraintFactory.scalar(getMoves(m), EMoveperVMs, EMove);
        return EMove;               
    }
    
    //energy spent to switch on and off machines
    public IntVar getEOnOff(ReconfigurationProblem m) {
        
    	Solver solver = m.getSolver();
    	int[] EOn = new int[m.getNodes().length];
        Arrays.fill(EOn, EnergyOff); 
        int[] EOff = new int[m.getNodes().length];
        Arrays.fill(EOff, EnergyOn); 
    	
        Pair<BoolVar[], BoolVar[]> switchs = getSwitchOnOffs(m);
        
		IntVar EOnTot = VariableFactory.bounded("EOnTot", 0, Integer.MAX_VALUE / 100, solver);
    	IntConstraintFactory.scalar(switchs.getFirst(), EOn, EOnTot);
    	IntVar EOffTot = VariableFactory.bounded("EOffTot", 0, Integer.MAX_VALUE / 100, solver);
    	IntConstraintFactory.scalar(switchs.getSecond(), EOff, EOffTot);
        
        return IntConstraintFactory.arithm(EOnTot, "+", EOffTot).reif();
    }
    
    
    public Pair<BoolVar[], BoolVar[]> getSwitchOnOffs(ReconfigurationProblem m) {
    	
    	BoolVar[] switchOns  = new BoolVar[m.getNodes().length];
        BoolVar[] switchOffs = new BoolVar[m.getNodes().length];
        
        int i = 0;
		for (Node node : m.getNodes()) {
			BoolVar futurState = m.getNodeAction(node).getState();
			//if the node if offline, it will be switched on if the future state is on, and vice-versa
			if(m.getSourceModel().getMapping().isOffline(node)) {
				switchOns[i] = futurState;
			} else	{
				switchOffs[i] = VariableFactory.not(futurState);
			}
		   i++;
		}
		return new Pair(switchOns, switchOffs); 
    	
    }
    
    public BoolVar[] getStates(ReconfigurationProblem m) {

		BoolVar[] states = new BoolVar[m.getNodes().length];
    	int i = 0;
		for (NodeActionModel action : m.getNodeActions()) {     
		   states[i] = action.getState();
		   i++;
		}
		return states;
	}
    
    public BoolVar[] getMoves(ReconfigurationProblem m) {
    	Solver solver = m.getSolver();
    	BoolVar moves[] = new BoolVar[m.getVMs().length];
        int i = 0;
        for (VM vm : m.getVMs()) {        	
          	//A boolean variable to indicate whether the node is used or not
            moves[i] = VariableFactory.bool("moves(" + vm.id() + ")", solver);
            IntVar hoster = m.getVMAction(vm).getDSlice().getHoster();
            moves[i] = IntConstraintFactory.arithm(hoster, "=", m.getCurrentVMLocation(i)).reif();
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
}
