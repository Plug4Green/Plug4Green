package f4g.commons.optimizer;


import javax.measure.quantity.Duration;
import javax.measure.quantity.Energy;

import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.NetworkNodeType;
import f4g.schemas.java.metamodel.VirtualMachineType;
import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;


/**
 * Interface for the f4g Cost Estimator component
 * 
 */
public interface ICostEstimator {


	/**
	 * Calculates the energy used for moving a VM from one server to another
	 * 
	 * @param the origin server, the destination server, the VM to move, the complete model
	 * @return the energy
	 */
	public Amount<Energy> moveEnergyCost(NetworkNodeType fromServer, NetworkNodeType toServer, VirtualMachineType VM, FIT4GreenType model);

	/**
	 * Calculates the financial cost of moving a VM from one server to another
	 * 
	 * @param the origin server, the destination server, the VM to move, the complete model
	 * @return the money amount
	 */
	public Amount<Money> moveFinancialCost(NetworkNodeType fromServer, NetworkNodeType toServer, VirtualMachineType VM, FIT4GreenType model);
	
	
	/**
	 * Calculates the down time necessary for moving a VM from one server to another
	 * 
	 * @param the origin server, the destination server, the VM to move, the complete model
	 * @return the duration
	 */
	public Amount<Duration> moveDownTimeCost(NetworkNodeType fromServer, NetworkNodeType toServer, VirtualMachineType VM, FIT4GreenType model);

	
	/**
	 * 
	 * This method is called by the core component responsible for starting up and shutting
	 * down the F4G plugin. It must implement all the operations needed to dispose the component
	 * in a clean way (e.g. stopping dependent threads, closing connections, sockets, file handlers, etc.)
	 * 
	 * @return
	 *
	 * @author FIT4Green
	 */
	boolean dispose();

}
