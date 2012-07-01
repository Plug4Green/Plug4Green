

package org.f4g.entropy.plan.search_heuristic;


import java.util.Comparator;
import java.util.List;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;



public class Utils {
    

    //Compare two nodes and returns the one with less remaining space on CPU
    public static class NodeCompareRemainingSpace implements Comparator<Node> {
    	Configuration src;
    	List<VirtualMachine> vms;
    	IntDomainVar[] hosters;
    	List<Node> nodes;
    	NodeCompareRemainingSpace(Configuration mySrc, List<VirtualMachine> myVms, List<Node> myNodes, IntDomainVar[] myHosters) {
    		src = mySrc;
    		vms = myVms;
    		hosters = myHosters;
    		nodes = myNodes;
    	}
    	
    	@Override
        public int compare(Node n1, Node n2) {
    		int remainingSpace1 = getRemainingSpace(n1, nodes.indexOf(n1), src, vms, hosters);
    		int remainingSpace2 = getRemainingSpace(n2, nodes.indexOf(n2), src, vms, hosters);
			if (remainingSpace1 < remainingSpace2 ) {
				return 1;
			} else if (remainingSpace1 == remainingSpace2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    
    //Select the node wich hosts the VM
    public static class NodeCompareOriginal implements Comparator<Node> {

    	Configuration src;
    	VirtualMachine vm;    	
    	NodeCompareOriginal(VirtualMachine myVM, Configuration mySrc) {
    		vm = myVM;
    		src = mySrc;
    	}
    	
    	@Override
        public int compare(Node n1, Node n2) {
    		Node originalNode = src.getLocation(vm);
    		boolean isOriginalNode1 = originalNode == n1;
    		boolean isOriginalNode2 = originalNode == n2;
			if (isOriginalNode1 && !isOriginalNode2 ) {
				return 1;
			} else if (isOriginalNode1 == isOriginalNode2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    
    //Compare two nodes and returns the one with less initial VMs
    public static class NodeCompareNbVMs implements Comparator<Node> {
    	    	
    	Configuration src;
  	
    	NodeCompareNbVMs(Configuration mySrc) {
    		src = mySrc;
    	}
    	
    	@Override
        public int compare(Node n1, Node n2) {
    		int nbVMs1 = src.getRunnings(n1).size();
    		int nbVMs2 = src.getRunnings(n2).size();
			if (nbVMs1 > nbVMs2 ) {
				return 1;
			} else if (nbVMs1 == nbVMs2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
	
    public static class NodeCompareNbCPU implements Comparator<Node> {
		@Override
        public int compare(Node n1, Node n2) {
			if (n1.getNbOfCPUs() > n2.getNbOfCPUs() ) {
				return 1;
			} else if (n1.getNbOfCPUs() == n2.getNbOfCPUs()) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    
    //nodes that are already a target for VMs are better
    public static class NodeCompareSelectTargets implements Comparator<Node> {
    	
    	List<Node> nodes; 
    	IntDomainVar[] hosters;
    	NodeCompareSelectTargets(List<Node> myNodes, IntDomainVar[] myHosters) {
    		nodes = myNodes;
    		hosters = myHosters;
    	}
    	
		@Override
        public int compare(Node n1, Node n2) {
			boolean target1 = isTarget(n1, nodes, hosters);
			boolean target2 = isTarget(n2, nodes, hosters);
			
			if (target1 && !target2 ) {
				return 1;
			} else if (target1 == target2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    
    public static class NodeCompareName implements Comparator<Node> {
	   	@Override
        public int compare(Node n1, Node n2) {return n1.getName().compareTo(n2.getName());}
    }
    

    //Compare two nodes and returns the one with less remaining space on CPU
    public static class VMCompareRemainingSpaceOnOrigin implements Comparator<VirtualMachine> {
    	Configuration src;
    	List<VirtualMachine> vms;
    	IntDomainVar[] hosters;
    	List<Node> nodes;
    	VMCompareRemainingSpaceOnOrigin(Configuration mySrc, List<VirtualMachine> myVms, List<Node> myNodes, IntDomainVar[] myHosters) {
    		src = mySrc;
    		vms = myVms;
    		hosters = myHosters;
    		nodes = myNodes;
    	}
    	
    	@Override
        public int compare(VirtualMachine vm1, VirtualMachine vm2) {
    		Node originNode1 = src.getLocation(vm1);
    		Node originNode2 = src.getLocation(vm2);
    		int remainingSpace1 = getRemainingSpace(originNode1, nodes.indexOf(originNode1), src, vms, hosters);
    		int remainingSpace2 = getRemainingSpace(originNode2, nodes.indexOf(originNode2), src, vms, hosters);
			if (remainingSpace1 > remainingSpace2 ) {
				return 1;
			} else if (remainingSpace1 == remainingSpace2) {
				return 0;
			} else {
				return -1;
			}
        }
    }
    

    //allows to build a comparator out of several comparators
    public static class MultiComparator<T> implements Comparator<T> {
        private List<Comparator<T>> comparators;

        public MultiComparator(List<Comparator<T>> comparators) {
            this.comparators = comparators;
        }

        public int compare(T o1, T o2) {
            for (Comparator<T> comparator : comparators) {
                int comparison = comparator.compare(o1, o2);
                if (comparison != 0) return comparison;
            }
            return 0;
        }
    }


	//compute the remaining space on the destination (initial VM + arriving VMs)
	public static int getRemainingSpace(Node node, int indexNode, Configuration src, List<VirtualMachine> vms, IntDomainVar[] hosters) {
		int consummedSpace = 0;
		ManagedElementSet<VirtualMachine> initialVMsOnNode = src.getRunnings(node);
		
		for(VirtualMachine vm : vms) {

			IntDomainVar hoster = hosters[vms.indexOf(vm)];
			if(hoster.isInstantiated()) {
				if(hoster.isInstantiatedTo(indexNode)) {
					consummedSpace += vm.getCPUConsumption();
				}
			} else {
				if(initialVMsOnNode.contains(vm)) {
					consummedSpace += vm.getCPUConsumption();
				}
			}
		}
		int remainingSpace = node.getCPUCapacity() - consummedSpace;
		return remainingSpace;
	}
	
	public static boolean isTarget(Node n, List<Node> nodes, IntDomainVar[] hosters) {
		int index = nodes.indexOf(n);
		for(IntDomainVar h : hosters) {
			if(h.isInstantiatedTo(index)) {
				return true;				
			}
		}
		return false;
	}

    
}

