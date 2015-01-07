

package f4g.optimizer.entropy.plan.search_heuristic;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import java.util.List;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import org.btrplace.configuration.*;

import org.apache.log4j.Logger;
import f4g.optimizer.entropy.configuration.F4GNode;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.commons.util.StaticPowerCalculation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import choco.kernel.solver.search.ValSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;



/**
 * A heuristic to try to assign the virtual machines to migrate
 * or to resume to its current (or previous) location.
 * If it is not possible, it consider the current residual capacity of the nodes
 * to choose the one to test.
 *
 * @author Fabien Hermenier
 */
public class ConsolidateValSelector implements ValSelector<IntDomainVar> {
    
	public Logger log;
    
    ReconfigurationProblem pb;
    IntDomainVar[] hosters;
    ManagedElementSet<Node> nodes;
    ManagedElementSet<VirtualMachine> vms;
    
    /**
     * Build a selector for a specific solver.
     *
     * @param s    the solver
     * @param pCpu the pack constraint for the CPU resources
     * @param pMem the pack constraint for the Mem resources
     * @param o    the option to customize the heuristic
     */
    public ConsolidateValSelector(ReconfigurationProblem myPb, ManagedElementSet<Node> myNodes) {
    	log = Logger.getLogger(this.getClass().getName());
		pb = myPb;
		Configuration src = pb.getSourceConfiguration();
		nodes = myNodes;
		
		
		vms = myPb.getSourceConfiguration().getRunnings(myNodes);
		hosters = new IntDomainVar[vms.size()];
		for(int i = 0; i < vms.size(); i++) {
			VirtualMachine vm = vms.get(i);
			hosters[i] = myPb.getAssociatedAction(vm).getDemandingSlice().hoster();
		}
		
	    Collections.sort(nodes, new Utils.NodeCompareName());
	    		
    }
    

    //Get the hoster for a VM mimimises the remaining space (to pack them)
    @Override
    public int getBestVal(IntDomainVar hoster) {
		
		//int indexDest = worstFitCPUOnlinesFirst(var);
		int indexDest = worstFitCPULowPIdleFirst(hoster);
		return indexDest;
    }


    private void setUp() {
        ManagedElementSet<Node> allNodes = new SimpleManagedElementSet<Node>();
        allNodes.addAll(nodes);
    }


   private int worstFitCPUOnlinesFirst(IntDomainVar v) {
        DisposableIntIterator ite = v.getDomain().getIterator();
        int maxIdx = ite.next();
        int maxCPU = pb.getUsedCPU(pb.getNode(maxIdx)).getInf();
        while (ite.hasNext()) {
            int possible = ite.next();
            int cpu = pb.getUsedCPU(pb.getNode(possible)).getInf();
            if (cpu > maxCPU) {
                maxIdx = possible;
                maxCPU = cpu;
            } else if (cpu == maxCPU) { //Same value, so if the node was already online in the original configuration, it is better
                Node n = pb.getNode(possible);
                if (pb.getSourceConfiguration().isOnline(n)) {
                    maxIdx = possible;
                }
            }
        }
        ite.dispose();
        return maxIdx;
    }
   
   private int worstFitCPULowPIdleFirst(IntDomainVar v) {
       DisposableIntIterator ite = v.getDomain().getIterator();
       int maxIdx = ite.next();
       int maxCPU = pb.getUsedCPU(pb.getNode(maxIdx)).getInf();
       while (ite.hasNext()) {
           int possible = ite.next();
           int cpu = pb.getUsedCPU(pb.getNode(possible)).getInf();
           if (cpu > maxCPU) {
               maxIdx = possible;
               maxCPU = cpu;
           } else if (cpu == maxCPU) { //Same value, so if the node has less power idle, it is better
               F4GNode newNode = (F4GNode) pb.getNode(possible);
               F4GNode oldNode = (F4GNode) pb.getNode(maxIdx);
               if (newNode.getPIdle() < oldNode.getPIdle()) {
                   maxIdx = possible;
               }
           }
       }
       ite.dispose();
       return maxIdx;
   }

    //get the best destination node for a VM
    private Node bestDestinationNode(final IntDomainVar place, final VirtualMachine myVM) {
    	//log.debug("bestDestinationNode");
        final Node originalNode = pb.getSourceConfiguration().getLocation(myVM);
                
        Predicate<Node> isOn = new Predicate<Node>() { 
		    @Override public boolean apply(Node n) { return pb.getSourceConfiguration().isOnline(n); }};
		    
		Predicate<Node> isNotFull = new Predicate<Node>() { 
			    @Override public boolean apply(Node n) { return  Utils.getRemainingSpace(n, nodes.indexOf(n), pb.getSourceConfiguration(), vms, hosters) > 0; }};
		    
        Predicate<Node> isIntanciable = new Predicate<Node>() { 
		    @Override public boolean apply(Node n) { return place.canBeInstantiatedTo(nodes.indexOf(n)); }};
		
		Predicate<Node> isTarget = new Predicate<Node>() { 
		        @Override public boolean apply(Node n) { return Utils.isTargetFast(pb, n, nodes, hosters); }};
		        
		Predicate<Node> isOrigin = new Predicate<Node>() { 
			        @Override public boolean apply(Node n) { return originalNode == n; }};
		        
//		Collection<Node> ons = Collections2.filter(nodes, isOn);
//		log.debug("ons: " + ons);
//		Collection<Node> notfulls = Collections2.filter(ons, isNotFull);
//		log.debug("notfulls: " + notfulls);
		Collection<Node> instanciables = Collections2.filter(nodes, isIntanciable);
		//log.debug("instanciables: " + instanciables);
		//if(instanciables.size() == 0) {
        if(instanciables.isEmpty()) {
			log.debug("No possible target for VM");
        	return null;
		}
		
		Collection<Node> targets = Collections2.filter(instanciables, isTarget);
		//log.debug("targets:" + targets);
		Collection<Node> notTargets = Collections2.filter(instanciables, Predicates.not(isTarget));

		Collection<Node> originInTarget = Collections2.filter(targets, isOrigin);
		Collection<Node> notOriginInTarget = Collections2.filter(targets, Predicates.not(isOrigin));
		assert(originInTarget.size() <= 1);
        //Comparator<Node> cmpRemSpace = new Utils.NodeCompareRemainingSpace(pb.getSourceConfiguration(), vms, nodes, hosters);
        Comparator<Node> cmpRemSpace = new Utils.NodeCompareRemainingSpaceFast(pb, pb.getSourceConfiguration(), vms, nodes, hosters);
        Comparator<Node> cmpTarget = new Utils.NodeCompareSelectTargets(pb, nodes, hosters);
        List<Comparator<Node>> comparators = new ArrayList<Comparator<Node>>();
        //First criteria: if a server has already been choosen as a target, its better
        comparators.add(cmpTarget);
        //Second criteria: ON servers are best
        comparators.add(new NodeCompareOn()); //Already online nodes plz
        //Third criteria: not full servers are best
        //comparators.add(new NodeCompareNotFull());
        comparators.add(new NodeCompareNotFullFast(pb));                //Useless, cause the non possible nodes will not be in the VM domain
        //Fourth criteria: select the server with the less remaining space
        comparators.add(cmpRemSpace);
        
        Utils.MultiComparator<Node> cmp = new Utils.MultiComparator<Node>(comparators);

        Node bestNode = Collections.max(instanciables, cmp);
        //log.debug("Choosen Node " + bestNode.getName());
        return bestNode;
        
    }
    
    public class NodeCompareOn implements Comparator<Node> {
	   	@Override
        public int compare(Node n1, Node n2) {
    		boolean o1 = pb.getSourceConfiguration().isOnline(n1);
    		boolean o2 = pb.getSourceConfiguration().isOnline(n2);
			if (o1 && !o2 ) {
				return 1;
			} else if (o1 == o2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    
    public class NodeCompareNotFull implements Comparator<Node> {
	   	@Override
        public int compare(Node n1, Node n2) {
    		boolean nf1 = Utils.getRemainingSpace(n1, nodes.indexOf(n1), pb.getSourceConfiguration(), vms, hosters) > 0;
    		boolean nf2 = Utils.getRemainingSpace(n2, nodes.indexOf(n2), pb.getSourceConfiguration(), vms, hosters) > 0;
			if (nf1 && !nf2 ) {
				return 1;
			} else if (nf1 == nf2) {
				return 0;
			} else {
				return -1;
			}
        }
    }

    public class NodeCompareNotFullFast implements Comparator<Node> {

        private ReconfigurationProblem rp;
        public NodeCompareNotFullFast(ReconfigurationProblem rp) {
            this.rp = rp;
        }
        @Override
        public int compare(Node n1, Node n2) {
            boolean nf1 = Utils.getRemainingSpaceFast(rp, n1, pb.getSourceConfiguration(), vms, hosters) > 0;
            boolean nf2 = Utils.getRemainingSpaceFast(rp, n2, pb.getSourceConfiguration(), vms, hosters) > 0;
            if (nf1 && !nf2 ) {
                return 1;
            } else if (nf1 == nf2) {
                return 0;
            } else {
                return -1;
            }
        }
    }

	private VirtualMachine getVM(IntDomainVar hoster) {
		int indexVM = -1;
		for(int i = 0; i < hosters.length; i++) {
			if(hosters[i] == hoster){
				indexVM = i;
				break;				
			}
		}
		//log.debug("setBranch index VM" + indexVM);
		VirtualMachine myVM = vms.get(indexVM);
		return myVM;
	}
    
	 
	
    
}

