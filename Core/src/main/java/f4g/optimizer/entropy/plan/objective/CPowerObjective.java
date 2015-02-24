package f4g.optimizer.entropy.plan.objective;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.SliceUtils;
import org.btrplace.scheduler.choco.constraint.mttr.*;
import org.btrplace.scheduler.choco.transition.NodeTransition;
import org.btrplace.scheduler.choco.constraint.CObjective;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.TransitionUtils;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.variables.VF;

import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;
import f4g.optimizer.entropy.plan.objective.api.PowerObjective;
import f4g.optimizer.utils.Pair;

import java.util.*;

import org.apache.commons.lang.ArrayUtils;

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
    	IntVar[] pows = new  IntVar[]{getPVMs(rp), getPIdle(rp)}; //, getPNetwork(m)
        solver.post(ICF.sum(pows, globalPower));
        
        solver.post(ICF.times(globalPower, VF.fixed((int)reconfTime/1000, solver), stableEnergy));
    	//this equation represents the energy spent between two reconfigurations (that we'll try to minimize).
    	IntVar[] energies = new  IntVar[]{stableEnergy, getEMove(rp), getEOnOff(rp)};
    	solver.post(ICF.sum(energies, reconfEnergy));
     
        rp.setObjective(true, reconfEnergy);
        
        injectPlacementHeuristic(rp, reconfEnergy);
        postCostConstraints();

        return true;
    }
  	
    public IntVar getPIdle(ReconfigurationProblem rp) {

    	PowerView powerIdleView = (PowerView) rp.getSourceModel().getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_IDLES);
    	
    	Solver solver = rp.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, rp.getNodes()); 
    	int[] powerIdles =  ArrayUtils.toPrimitive(powerIdleView.getPowers(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PIdleTotal = VF.bounded("PIdleTotal", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.scalar(getStates(rp), powerIdles, PIdleTotal));
        
        return PIdleTotal; 
    }


    public IntVar getPVMs(ReconfigurationProblem rp) {

    	PowerView powerPerVMView = (PowerView) rp.getSourceModel().getView(PowerView.VIEW_ID_BASE + F4GConfigurationAdapter.VIEW_POWER_PER_VM);

    	Solver solver = rp.getSolver();
    	List<Node> nodes = new ArrayList<Node>();
    	Collections.addAll(nodes, rp.getNodes()); 
    	int[] powerperVMs =  ArrayUtils.toPrimitive(powerPerVMView.getPowers(nodes).toArray(new Integer[nodes.size()]));
    	
    	IntVar PowerperVMsTotal = VF.bounded("PowerperVMsTotal", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.scalar(rp.getNbRunningVMs(), powerperVMs, PowerperVMsTotal));
        
    	System.out.print(PowerperVMsTotal);
        return PowerperVMsTotal; 
    }

    public IntVar getEMove(ReconfigurationProblem rp) {
              
    	Solver solver = rp.getSolver();
    	int[] EMoveperVMs = new int[rp.getVMs().length];
        Arrays.fill(EMoveperVMs, EnergyMove); 
        
    	IntVar EMove = VF.bounded("EMove", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.scalar(getMoves(rp), EMoveperVMs, EMove));
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
        
		IntVar EOnTot = VF.bounded("EOnTot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.scalar(switchs.getFirst(), EOn, EOnTot));
    	IntVar EOffTot = VariableFactory.bounded("EOffTot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.scalar(switchs.getSecond(), EOff, EOffTot));
    	IntVar ETot = VF.bounded("ETot", 0, Integer.MAX_VALUE / 100, solver);
    	solver.post(ICF.sum(new IntVar[]{EOnTot,EOffTot}, ETot));
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
				switchOffs[i] = VF.zero(rp.getSolver());
			} else	{
				switchOffs[i] = VF.not(futurState);
				switchOns[i] = VF.zero(rp.getSolver());
			}
		   i++;
		}
		return new Pair(switchOns, switchOffs); 
    	
    }
    
    public BoolVar[] getStates(ReconfigurationProblem rp) {

		BoolVar[] states = new BoolVar[rp.getNodes().length];
    	int i = 0;
		for (NodeTransition action : rp.getNodeActions()) {     
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
          	//A boolean variable to indicate whether the VM moves
            moves[i] = VF.bool("moves(" + vm.id() + ")", solver);
            IntVar hoster = rp.getVMAction(vm).getDSlice().getHoster();
            moves[i] = ICF.arithm(hoster, "!=", rp.getCurrentVMLocation(i)).reif();
            i++;
        }
        return moves;
	}

    private void injectHeuristic(ReconfigurationProblem p) {

        List<AbstractStrategy> strats = new ArrayList<>();


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

    private void injectPlacementHeuristic(ReconfigurationProblem p, IntVar cost) {

        Model mo = p.getSourceModel();
        Mapping map = mo.getMapping();

        OnStableNodeFirst schedHeuristic = new OnStableNodeFirst(p, this);

        //Get the VMs to place
        Set<VM> onBadNodes = new HashSet<>(p.getManageableVMs());

        //Get the VMs that runs and have a pretty low chances to move
        Set<VM> onGoodNodes = map.getRunningVMs(map.getOnlineNodes());
        onGoodNodes.removeAll(onBadNodes);

        VMTransition[] goodActions = p.getVMActions(onGoodNodes);
        VMTransition[] badActions = p.getVMActions(onBadNodes);

        Solver s = p.getSolver();

        //Get the VMs to move for exclusion issue
        Set<VM> vmsToExclude = new HashSet<>(p.getManageableVMs());
        for (Iterator<VM> ite = vmsToExclude.iterator(); ite.hasNext(); ) {
            VM vm = ite.next();
            if (!(map.isRunning(vm) && p.getFutureRunningVMs().contains(vm))) {
                ite.remove();
            }
        }
        List<AbstractStrategy> strategies = new ArrayList<>();

        Map<IntVar, VM> pla = VMPlacementUtils.makePlacementMap(p);
        if (!vmsToExclude.isEmpty()) {
            List<VMTransition> actions = new LinkedList<>();
            //Get all the involved slices
            for (VM vm : vmsToExclude) {
                if (p.getFutureRunningVMs().contains(vm)) {
                    actions.add(p.getVMAction(vm));
                }
            }
            IntVar[] scopes = SliceUtils.extractHoster(TransitionUtils.getDSlices(actions));

            strategies.add(new IntStrategy(scopes, new MovingVMs(p, map, actions), new RandomVMPlacement(p, pla, true)));
        }

        placeVMs(p, strategies, badActions, schedHeuristic, pla);
        placeVMs(p, strategies, goodActions, schedHeuristic, pla);

        //VMs to run
/*        Set<VM> vmsToRun = new HashSet<>(map.getReadyVMs());
        vmsToRun.removeAll(p.getFutureReadyVMs());

        VMTransition[] runActions = p.getVMActions(vmsToRun);

        placeVMs(strategies, runActions, schedHeuristic, pla);
  */

        if (p.getNodeActions().length > 0) {
            //Boot some nodes if needed
            strategies.add(new IntStrategy(TransitionUtils.getStarts(p.getNodeActions()), new InputOrder<>(), new IntDomainMin()));
        }

        ///SCHEDULING PROBLEM
        MovementGraph gr = new MovementGraph(p);
        strategies.add(new IntStrategy(SliceUtils.extractStarts(TransitionUtils.getDSlices(p.getVMActions())), new StartOnLeafNodes(p, gr), new IntDomainMin()));
        strategies.add(new IntStrategy(schedHeuristic.getScope(), schedHeuristic, new IntDomainMin()));

        //At this stage only it matters to plug the cost constraints
        strategies.add(new IntStrategy(new IntVar[]{p.getEnd(), cost}, new InputOrder<>(), new IntDomainMin()));

        s.getSearchLoop().set(new StrategiesSequencer(s.getEnvironment(), strategies.toArray(new AbstractStrategy[strategies.size()])));
    }

    /*
     * Try to place the VMs associated on the actions in a random node while trying first to stay on the current node
     */
    private void placeVMs(ReconfigurationProblem rp, List<AbstractStrategy> strategies, VMTransition[] actions, OnStableNodeFirst schedHeuristic, Map<IntVar, VM> map) {
        if (actions.length > 0) {
            IntVar[] hosts = SliceUtils.extractHoster(TransitionUtils.getDSlices(actions));
            if (hosts.length > 0) {
                strategies.add(new IntStrategy(hosts, new HostingVariableSelector(schedHeuristic), new RandomVMPlacement(rp, map, true)));
            }
        }
    }
}
