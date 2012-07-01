/**
* ============================== Header ============================== 
* file:          F4GResourceMaxConstraint.java
* project:       FIT4Green/Optimizer
* created:       09.10.2011 by ts
* last modified: $LastChangedDate: 2010-11-26 11:33:26 +0100 (Fr, 26 Nov 2010) $ by $LastChangedBy: corentin.dupont@create-net.org $
* revision:      $LastChangedRevision: 150 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.entropy.plan.constraint;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;


/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GResourceMaxConstraint extends F4GConstraint {
	
	private ManagedElementSet<Node> nodes;	
	private int i;
	private Object o;
	private ManagedElementSet<VirtualMachine> vms;

		
	/**
	 * 
	 */
	public F4GResourceMaxConstraint(int i, Object o, ManagedElementSet<Node> nodes, ManagedElementSet<VirtualMachine> vms) {
		super();
		this.nodes = nodes;
		this.i = i;
		this.o = o;
		this.vms = vms;
	}
	
	@Override
	public void inject(ReconfigurationProblem core) {
		
	}
	
	

}
