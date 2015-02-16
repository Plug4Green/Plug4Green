package f4g.powerCalculator.power;
import java.util.*;

import javax.measure.Measure;
import javax.measure.quantity.Power;

import f4g.powerCalculator.power.PoweredComponent;

import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.NetworkPort;
import f4g.schemas.java.metamodel.NetworkNodeStatus;
import org.apache.log4j.Logger;
import f4g.schemas.java.metamodel.BoxNetwork;
import f4g.schemas.java.metamodel.NetworkNode;
import f4g.schemas.java.metamodel.NetworkNodeStatus;

import org.jscience.physics.amount.*;
import org.jscience.physics.model.*;
import org.jscience.economics.money.*;
import javax.measure.quantity.*;
import static javax.measure.unit.SI.*;

/**
 * PoweredNetworkNode Class 
 * 
 * @author rlent
 *
 */

public class PoweredNetworkNode extends NetworkNode implements PoweredComponent{	
	
    NetworkNode node;
	private boolean simulationFlag;
    //	private JXPathContext netContext;
    
	/**
	 * PoweredNetworkNode constructor
	 * 
	 */
    public PoweredNetworkNode(NetworkNode n)
    {  
        this.node = n;
		this.simulationFlag = true;       
    }
    
    public PoweredNetworkNode(NetworkNode n, boolean flag)
    {  
        this.node = n;
		this.simulationFlag = flag;        
    }
    
    //    public PoweredNetworkNode(JXPathContext ctx, NetworkNode n, boolean flag)
    //    {  
    //        this.node = n;
    //        this.netContext = ctx;
    //		this.simulationFlag = flag;        
    //    }
    
	/**
	 * calculates the power consumption of a network device
	 * 
	 * @param none
	 * @return class Measure     .doubleValue(WATT);
	 */
	@Override
	public double computePower() {
        
		// Power is due to switching fabric (if router/switch) + all ports
		// If NIC, there is no switching fabric and just one port
		
		Amount<Power> networkNodePower = Amount.valueOf(0.0, WATT);
        
        if( node.getStatus() == NetworkNodeStatus.ON ) {
            
            double totalPps = 0;
            
            Iterator<NetworkPort> itr = node.getNetworkPort().iterator();
            while(itr.hasNext()) {
                NetworkPort port = itr.next();
                double Ti = 0.0;
                double To = 0.0;
                if(port.getTrafficIn() != null) Ti = port.getTrafficIn().getValue();
                if(port.getTrafficOut() != null) To = port.getTrafficOut().getValue();
                double pps = Ti+To; 
                
                if( node.isForwardFlag() ) {  		// Switch/router
                    double bandwidth = port.getLineCapacity().getValue();		// in pps
                    networkNodePower = networkNodePower.plus( trafficToPower(port.getPowerIdle().getValue(), port.getPowerMax().getValue(), pps, bandwidth) ); 
                }
                
                totalPps += pps;
            }
            
            // compute power due to switching operations (if router/switch) or NIC port
            
            double bandwidth = node.getProcessingBandwidth().getValue();
            networkNodePower = networkNodePower.plus( trafficToPower(node.getPowerIdle().getValue(), node.getPowerMax().getValue(), totalPps, bandwidth) ); 
        }
        
        return networkNodePower.doubleValue(WATT);
	}
    
    
	/**
	 * power consumption model
	 * 
	 * @param model parameters and operating point (in packets per second)
	 * @return class Measure  
	 */
    public static Amount<Power> trafficToPower(double pidle, double pmax, double pps, double bandwidth)
    {
        return Amount.valueOf( pidle + (pmax-pidle)*pps/bandwidth, WATT );
    }
    
}
