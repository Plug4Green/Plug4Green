package org.f4g.entropy.plan.objective;

import choco.Choco;
import choco.cp.solver.constraints.reified.FastIFFEq;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.cp.solver.variables.integer.BoolVarNot;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.BootableNodeActionModel2;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.actionModel.ShutdownableNodeActionModel;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.vjob.VJob;
import org.f4g.entropy.configuration.F4GNode;
import org.f4g.entropy.plan.constraint.Cardinalities;
import org.f4g.entropy.plan.constraint.PackingBasedCardinalities;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.util.StaticPowerCalculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static choco.cp.solver.CPSolver.mult;
import static choco.cp.solver.constraints.integer.bool.BooleanFactory.or;

public class PowerObjective extends Objective {

   
    FIT4GreenType model;
    IPowerCalculator powerCalculator;
    VMTypeType vmTypes;

    //objective: either Power or CO2
	OptimizationObjective optiObjective;
	StaticPowerCalculation powerCalculation;

	//number of switches
	int numberOfSwitches = 0;
	
	//index of the switch attached to a node i
	// row i represents the set of switches attached to server i 
	List<Integer> nodeSwitches[];
	
	//power for each switch
	int powerPerSwitch[];
    
    
    int reconfTime = 1000; // seconds (must be multiple of 1000s)
    int EnergyOn = 1; //KJoule, sample value
    int EnergyOff = 1; //KJoule, sample value
    int EnergyMove = 1; //KJoule, sample value
	private List<ServerType> allServers;

    private List<SConstraint> qualityConstraints;

    /**
     * Make a new constraint.
     *
     * @param vms A non-empty set of virtual machines
     */
    public PowerObjective(FIT4GreenType myModel, VMTypeType myVMs, IPowerCalculator myPowerCalculator, OptimizationObjective myOptiObjective) {
    	
    	model = myModel;
    	powerCalculator = myPowerCalculator;
    	vmTypes = myVMs;
    	optiObjective = myOptiObjective;
    	this.qualityConstraints = new ArrayList<SConstraint>();
    	powerCalculation = new StaticPowerCalculation(myVMs);

        // initialize data structures
        
    	allServers = Utils.getAllServers(model);
        int numberOfServers = allServers.size();

        List<NetworkNodeType> networkNodes = Utils.getAllNetworkDeviceNodes(model);
        numberOfSwitches = networkNodes.size();

        powerPerSwitch = new int[numberOfSwitches];
        nodeSwitches = new ArrayList[numberOfServers];
        
        // setup mapping from switch frameworkid -> index in networkNodes list
        
        HashMap<String, Integer> frameid2index = new HashMap();
		for (int i=0; i<numberOfSwitches; i++) 
                frameid2index.put(networkNodes.get(i).getFrameworkID(), i);

        // get power of each switch: initialize powerPerSwitch

		for (int i=0; i<numberOfSwitches; i++) {
            NetworkNodeType sw = networkNodes.get(i);
        	double ue;
        	if(optiObjective == OptimizationObjective.Power) 
           		ue = Utils.getNetworkSite(sw, model).getPUE().getValue();
        	else
        		ue = Utils.getNetworkSite(sw, model).getCUE().getValue();

            // powerPerSwitch[i] = (int) ((new PoweredNetworkNode(sw)).computePower() * ue);
            powerPerSwitch[i] = (int) (sw.getPowerIdle().getValue() * ue);
		}

        // populate nodeSwitches
        
        for(int i=0; i<numberOfServers; ++i) {
            List<NetworkNodeType> swList = Utils.getAttachedSwitches(allServers.get(i), model);
            nodeSwitches[i] = new ArrayList();
            for( NetworkNodeType sw : swList ) 
                nodeSwitches[i].add( frameid2index.get(sw.getFrameworkID()));
        }

    }

    

    @Override
    public void makeObjective(ReconfigurationProblem m) {
        
    	IntDomainVar reconfEnergy = m.createBoundIntVar("reconfEnergy", 0, Choco.MAX_UPPER_BOUND); //In Watts
    	IntDomainVar globalPower = m.createBoundIntVar("globalPower", 0, Choco.MAX_UPPER_BOUND); // in Watts
    	IntDomainVar stableEnergy = m.createBoundIntVar("stableEnergy", 0, Choco.MAX_UPPER_BOUND); //in KJ
    	
    	//sum the power Idle, the power for the VMs and extra power for the network
        IntExp c = m.sum(getPIdle(m), getPVMs(m), getPNetwork(m));
        SConstraint s = m.eq(c, globalPower);
        //TODO quality constraint ?
    	//m.post(s);
    	m.post(m.eq(mult((int)reconfTime/1000, globalPower), stableEnergy));
    	//this equation represents the energy spent between two reconfigurations (that we'll try to minimize).
        m.post(m.eq(m.sum(stableEnergy, getEMove(m), getEOnOff(m)), reconfEnergy));
     
        objective = reconfEnergy;

    }
    
  
	public IntExp getPIdle(ReconfigurationProblem m) {
             
    	IntDomainVar Pidle = m.createBoundIntVar("Pidle",0, Choco.MAX_UPPER_BOUND);
    	
        ManagedElementSet<Node> nodes = m.getSourceConfiguration().getAllNodes();
        IntDomainVar[] PIdleServer = new IntDomainVar[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
        	
        	F4GNode f4gNode = (F4GNode)nodes.get(i);		
               	
            PIdleServer[i] = m.createEnumIntVar("IdlePowerServer" + i, new int[]{0, f4gNode.getPIdle()}); 
                        
            if(m.getFutureOnlines().contains(nodes.get(i))) {
                m.post(m.eq(f4gNode.getPIdle(), PIdleServer[i])); 
            } else if (m.getFutureOfflines().contains(nodes.get(i))) {
            	m.post(m.eq(0, PIdleServer[i]));
            } else {
                ManageableNodeActionModel action = (ManageableNodeActionModel) m.getAssociatedAction(nodes.get(i));
                m.post(new FastIFFEq(action.getState(), PIdleServer[i], f4gNode.getPIdle())); 
            }
                     
        }
        IntExp e = m.sum(PIdleServer);
        //TODO quality constraint ?
        SConstraint s = m.eq(e, Pidle);
        qualityConstraints.add(s);
        //m.post(s);
        
        return Pidle;        
    }


    public IntExp getPVMs(ReconfigurationProblem m) {
                
    	IntDomainVar PVMs = m.createBoundIntVar("PVMs",0, Choco.MAX_UPPER_BOUND);
        ManagedElementSet<Node> nodes = m.getSourceConfiguration().getAllNodes();

        //For each node, we define a set denoting the VMs it may hosts
        IntDomainVar[] cards = new IntDomainVar[nodes.size()];
                    
        int[] nodesEnergyPerVM = new int[allServers.size()];

        //Cardinalities c = OccurencesBasedCardinalities.getInstances();
        Cardinalities c = PackingBasedCardinalities.getInstances();
        if (c == null) {
            c = new PackingBasedCardinalities(m, 50);
        }
        Plan.logger.debug(nodes.toString());
        for (int i = 0; i < nodes.size(); i++) {
        	F4GNode f4gNode = (F4GNode)nodes.get(i);		
 
        	//cards[i] = m.getSetModel(nodes.get(i)).getCard();
            cards[i] = c.getCardinality(nodes.get(i));
            nodesEnergyPerVM[i] = f4gNode.getPperVM();
        }

        //TODO quality oriented constraints
        SConstraint s = m.eq(m.scalar(cards, nodesEnergyPerVM), PVMs);
        qualityConstraints.add(s);
        //m.post(s);
        return PVMs;
        
    }
    
    //Computes the power of the network
    public IntExp getPNetwork(ReconfigurationProblem m) {
    	
    	IntDomainVar PNetwork = m.createBoundIntVar("PNetwork",0, Choco.MAX_UPPER_BOUND);
    	
		//For each switch, we define a boolean domain denoting if the switch is on or off
        IntDomainVar[] switchON = new IntDomainVar[numberOfSwitches];
                
    	for (int i = 0; i < numberOfSwitches; i++) {
    		//create the boolean domain variables
    		switchON[i]  = m.createBooleanVar("switchON(" + i + ")");
    	}
    	
    	//get all the Entropy nodes
        ManagedElementSet<Node> nodes = m.getSourceConfiguration().getAllNodes();
        
    	for (int i = 0; i < nodes.size(); ++i) {    		
    		for (int j=0; j<nodeSwitches[i].size(); ++j) {
        		//get the index of the switch attached to this server
        		int indexSwitch = nodeSwitches[i].get(j);        			        			
        		ManageableNodeActionModel action = (ManageableNodeActionModel) m.getAssociatedAction(nodes.get(i));    
        		m.post(ReifiedFactory.builder(switchON[indexSwitch], or(m.getEnvironment(), switchON[indexSwitch], action.getState()), m));  
    		}
    	}       
    	
    	//the power of all the switch is egal to the scalar product of these two vector
        //TODO quality oriented constraint
        SConstraint s = m.eq(m.scalar(switchON, powerPerSwitch), PNetwork);
        qualityConstraints.add(s);
        //m.post(s);
    	return PNetwork;
    }
 

    public IntExp getEMove(ReconfigurationProblem m) {
              
    	IntDomainVar EMove = m.createBoundIntVar("EMove",0, Choco.MAX_UPPER_BOUND);
        ManagedElementSet<VirtualMachine> vms = m.getFutureRunnings();
        
        IntDomainVar moves[] = new IntDomainVar[vms.size()];
        
        for (int i = 0; i < vms.size(); i++) {
        	
        	VirtualMachine vm = vms.get(i);
        	//A boolean variable to indicate whether the node is used or not
            moves[i] = m.createBooleanVar("moves(" + vm.getName() + ")");
                        
            Slice t = m.getAssociatedAction(vm).getDemandingSlice();
            if (t != null) {                    
               try {
                  Configuration src = m.getSourceConfiguration();
                  Node node = src.getLocation(vm);
                  if(node != null){
                	  //moves[i] is true if the futur hoster of a VM is not the current one.
                      m.post(ReifiedFactory.builder(moves[i], m.neq(t.hoster(), m.getNode(node)), m));
                  } else {
                	  //if the VM is not hosted yet, we don't consider it as a move.
                	  m.post(m.eq(moves[i],0));
                  }
                       
                } catch (Exception e) {
                  VJob.logger.error(e.getMessage(), e);
                }
            }
        }
        
        IntDomainVar NbMoves = m.createBoundIntVar("NbMoves",0, vms.size());
        //TODO quality oriented constraint ?
        SConstraint s = m.eq(m.sum(moves), NbMoves);
        //m.post(s);
        SConstraint s2 = m.eq(mult(EnergyMove, NbMoves), EMove);
        //m.post(s2);
        qualityConstraints.add(s2);
        qualityConstraints.add(s);
        return EMove;               
    }
    
    //energy spent to switch on and off machines
    public IntExp getEOnOff(ReconfigurationProblem m) {
        
    	IntDomainVar EOnOff = m.createBoundIntVar("EOnOff", 0, Choco.MAX_UPPER_BOUND);
    	ManagedElementSet<Node> nodes = m.getSourceConfiguration().getAllNodes();
        IntDomainVar[] EOnOffServer = new IntDomainVar[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
        	Node n = nodes.get(i);
        	//TODO: correct to have 2 possible values for EnergyOn & EnergyOff
        	EOnOffServer[i] = m.createEnumIntVar("EOnOffServer" + i, new int[]{0, 1}); 
            
        	//first forced cases
            if(m.getFutureOnlines().contains(n)) {
            	if(m.getSourceConfiguration().isOffline(n)) {
            		m.post(m.eq(EnergyOn, EOnOffServer[i])); 
            	} else {
            		m.post(m.eq(0, EOnOffServer[i])); 
            	}   
            } else if (m.getFutureOfflines().contains(n)) {
            	if(m.getSourceConfiguration().isOnline(n)) {
            		m.post(m.eq(EnergyOff, EOnOffServer[i])); 
            	} else {
            		m.post(m.eq(0, EOnOffServer[i])); 
            	}   
            } else { //the state is managed by the engine           	
                ManageableNodeActionModel action = (ManageableNodeActionModel) m.getAssociatedAction(nodes.get(i));
                if (action instanceof BootableNodeActionModel2) {
                	m.post(new FastIFFEq(action.getState(), EOnOffServer[i], EnergyOn)); 
                	//m.post(m.eq(EOnOffServer[i], mult(EnergyOn, action.getState()))); 
                } else if (action instanceof ShutdownableNodeActionModel) {
                	IntDomainVar isOffline = new BoolVarNot(m, "offline(" + n.getName() + ")", action.getState());
                	m.post(new FastIFFEq(isOffline, EOnOffServer[i], EnergyOff));
                	//m.post(m.eq(EOnOffServer[i], mult(EnergyOff, isOffline))); 
                }
            }                     
        }
        //TODO quality oriented constraint
        SConstraint s = m.eq(m.sum(EOnOffServer), EOnOff);
        qualityConstraints.add(s);
        //m.post(s);
        return EOnOff;
    }

    public List<SConstraint> getQualityOrientedConstraints() {
        return this.qualityConstraints;
    }
}
