/**
* ============================== Header ============================== 
* file:          F4GConstraint.java
* project:       FIT4Green/Optimizer
* created:       06.10.2011 by ts
* last modified: $LastChangedDate: 2010-11-26 11:33:26 +0100 (Fr, 26 Nov 2010) $ by $LastChangedBy: corentin.dupont@create-net.org $
* revision:      $LastChangedRevision: 150 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.optimizer.entropy.plan.constraint;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.protobuf.PBVJob.vjob.Constraint;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author ts
 */
public class F4GConstraint implements PlacementConstraint {


	@Override
	public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
		return new SimpleManagedElementSet<VirtualMachine>();
	}

	@Override
	public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration arg0) {
		return null;
	}


	@Override
	public ManagedElementSet<Node> getNodes() {
		return null;
	}

	@Override
	public void inject(ReconfigurationProblem arg0) {

	}

	@Override
	public boolean isSatisfied(Configuration arg0) {
		return false;
	}

	/* (non-Javadoc)
	 * @see entropy.vjob.PlacementConstraint#getType()
	 */
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see entropy.vjob.PlacementConstraint#toProtobuf()
	 */
	@Override
	public Constraint toProtobuf() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see entropy.vjob.PlacementConstraint#toXML()
	 */
	@Override
	public String toXML() {
		// TODO Auto-generated method stub
		return null;
	}

}
