
package f4g.optimizer.entropy.plan.search_heuristic;


import java.util.Collections;

import org.apache.log4j.Logger;
import f4g.optimizer.entropy.configuration.F4GNode;
import f4g.optimizer.entropy.configuration.F4GNodeComparator;
import f4g.optimizer.entropy.configuration.F4GResourcePicker;
 
import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.branch.AbstractLargeIntBranchingStrategy;
import choco.kernel.solver.search.IntBranchingDecision;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.cp.solver.variables.integer.BooleanVarImpl;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;

public class SimpleVMPacking extends AbstractLargeIntBranchingStrategy {
	
	public Logger log;
    
    ReconfigurationProblem pb;
    IntDomainVar[] hosters;
    ManagedElementSet<Node> nodes;
    IStateInt sourceServer;
    IStateInt targetServer;

	public SimpleVMPacking(ReconfigurationProblem myPb, ManagedElementSet<Node> myNodes) {

    	log = Logger.getLogger(this.getClass().getName());
		pb = myPb;
		nodes = myNodes.clone();

		//first criteria: select first the servers with few VMs to empty them
        F4GNodeComparator cmp = new F4GNodeComparator(true, F4GResourcePicker.NodeRc.numberVMsInitial, (Configuration) myPb.getSourceConfiguration());
        //second criteria: select first the servers with a big power idle (to free them and switch them off)
        cmp.appendCriteria(false, F4GResourcePicker.NodeRc.powerIdle);
        
        Collections.sort(nodes, cmp);
        sourceServer = pb.getEnvironment().makeInt(0);
        if(nodes.size() != 0) {
        	targetServer = pb.getEnvironment().makeInt(nodes.size()-1);
        } else {
        	targetServer = pb.getEnvironment().makeInt(0);
        }
        for(Node n : nodes) {
        	log.debug("node " + n.getName() + " contains " + myPb.getSourceConfiguration().getRunnings(n).size() + " VMs and consumes " + ((F4GNode)n).getPIdle() + " Watts");
        }

        ManagedElementSet<VirtualMachine> allVMS = pb.getSourceConfiguration().getAllVirtualMachines();
        hosters = new IntDomainVar[allVMS.size()];
        for(int i = 0; i < allVMS.size(); i++) {
        	VirtualMachine vm = pb.getVirtualMachine(i);
        	hosters[i] = myPb.getAssociatedAction(vm).getDemandingSlice().hoster();
        }
    }

	/**
	 * Select the VM
	 */
	@Override
	public Object selectBranchingObject() throws ContradictionException {
		if(nodes.size()==0)
			return null;
		
		while(sourceServer.get() != targetServer.get()) {
			Node srcNode = nodes.get(sourceServer.get());
			
			ManagedElementSet<VirtualMachine> sourceVMs = pb.getSourceConfiguration().getRunnings(srcNode);
			
			for (VirtualMachine vm : sourceVMs) {
	        	int indexVM = pb.getVirtualMachine(vm);
	            if (!hosters[indexVM].isInstantiated()) {
	            	log.debug("Selected VM " + vm.getName());
	                return hosters[indexVM];
	            }
	        }
			sourceServer.add(1);
			log.debug("sourceServer incremented: " + sourceServer.get());
		}
		log.debug("Packing finished");
		return null;
	}

	/**
	 * Select the Node
	 */
    public int getBestVal(IntDomainVar hoster) {
    	
    	while(sourceServer.get() != targetServer.get()) {
    		Node dstNode = nodes.get(targetServer.get());
			int indexDest = pb.getNode(dstNode);
			if(hoster.canBeInstantiatedTo(indexDest)) {
    			if(canContain(dstNode, getVM(hoster))) {
    				log.debug("Selected Server " + dstNode.getName());
        			return indexDest;
    			}
    		}
			targetServer.add(-1); //TODO check if equivalent to decrement
			log.debug("targetServer decrement: " + targetServer.get());
		}

    	log.debug("No more target servers");
    	//returning the current server which should be the source.
		return pb.getNode(nodes.get(targetServer.get()));
		
    }
    
    /**
	 * return wheras the server can contain the VM according to the current situation
	 * and supposing that the VMs initialy present on the server won't move
	 */
    private boolean canContain(Node n, VirtualMachine vm) {
    	
    	int cpuRemaining = F4GResourcePicker.get((F4GNode)n, F4GResourcePicker.NodeRc.cpuRemaining, pb.getSourceConfiguration());
    	int memoryRemaining = F4GResourcePicker.get((F4GNode)n, F4GResourcePicker.NodeRc.memoryRemaining, pb.getSourceConfiguration());

    	cpuRemaining -= pb.getUsedCPU(n).getInf();
    	memoryRemaining -= pb.getUsedMem(n).getInf();
    	//log.debug("cpuRemaining " + n.getName() + " = " + cpuRemaining);
    	//log.debug("memoryRemaining " + n.getName() + " = " + memoryRemaining);
    	if(cpuRemaining >= vm.getCPUDemand() &&
    		memoryRemaining >= vm.getMemoryDemand()) {
    		return true;
    	} else {
    		return false;
    	}
    		
    }
    
    /**
	 * Get the VM corresponding to the hoster
	 */
    private VirtualMachine getVM(IntDomainVar hoster) {

		for(int i = 0; i < hosters.length; i++) {
			if(hosters[i] == hoster){
				VirtualMachine myVM = pb.getVirtualMachine(i);
				return myVM;			
			}
		}
		return null;
	}

    @Override
	public boolean finishedBranching(final IntBranchingDecision decision) {
		return decision.getBranchingIntVar().getDomainSize() == 0;
	}

	
	@Override
	public void setFirstBranch(final IntBranchingDecision decision) {
		decision.setBranchingValue(getBestVal(decision.getBranchingIntVar()));
	}
	
	@Override
	public void setNextBranch(final IntBranchingDecision decision) {
		decision.setBranchingValue(getBestVal(decision.getBranchingIntVar()));
	}

	@Override
	public void goDownBranch(final IntBranchingDecision decision) throws ContradictionException {
		decision.setIntVal();
	}

	@Override
	public void goUpBranch(final IntBranchingDecision decision) throws ContradictionException {
		decision.remIntVal();
		//targetServer.increment();
		log.debug("go up branch targetServer= " + targetServer.get());
		log.debug("go up branch sourceServer= " + sourceServer.get());
		
	}

	@Override
	public String getDecisionLogMessage(IntBranchingDecision decision) {
		return getDefaultAssignMsg(decision);
	}

}
