/**
 *  Copyright (c) 1999-2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.f4g.entropy.plan.search_heuristic;


import java.util.Collections;

import org.apache.log4j.Logger;
import org.f4g.entropy.configuration.F4GNode;
import org.f4g.entropy.configuration.F4GNodeComparator;
import org.f4g.entropy.configuration.F4GResourcePicker;

import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.branch.AbstractLargeIntBranchingStrategy;
import choco.kernel.solver.search.IntBranchingDecision;
import choco.kernel.solver.variables.integer.IntDomainVar;
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
        targetServer = pb.getEnvironment().makeInt(nodes.size()-1);
        
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
		
		while(sourceServer.get() != targetServer.get()) {
			Node srcNode = nodes.get(sourceServer.get());
			
			ManagedElementSet<VirtualMachine> sourceVMs = pb.getSourceConfiguration().getRunnings(srcNode);
			
			for (VirtualMachine vm : sourceVMs) {
	        	int indexVM = pb.getVirtualMachine(vm);
	            if (!hosters[indexVM].isInstantiated()) {
	            	//log.debug("Selected VM " + vm.getName());
	                return hosters[indexVM];
	            }
	        }
			sourceServer.increment();
			//log.debug("sourceServer incremented: " + sourceServer.get());
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
    				//log.debug("Selected Server " + dstNode.getName());
        			return indexDest;
    			}
    		}
			targetServer.decrement();
			//log.debug("targetServer decrement: " + targetServer.get());
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
	}

	@Override
	public String getDecisionLogMessage(IntBranchingDecision decision) {
		return getDefaultAssignMsg(decision);
	}

}
