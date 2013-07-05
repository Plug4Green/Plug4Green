package org.f4g.power;
import java.util.*;

import javax.measure.Measure;
import javax.measure.quantity.Power;

import org.f4g.power.PoweredComponent;

import org.apache.log4j.Logger;
import org.f4g.schema.metamodel.NetworkPortType;
import org.f4g.schema.metamodel.NetworkNodeStatusType;
import org.apache.log4j.Logger;
import org.f4g.schema.metamodel.BoxNetworkType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.NetworkNodeStatusType;

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

public class PoweredNetworkNode extends NetworkNodeType implements PoweredComponent{	
	
    NetworkNodeType node;
	private boolean simulationFlag;
    //	private JXPathContext netContext;
    
	/**
	 * PoweredNetworkNode constructor
	 * 
	 */
    public PoweredNetworkNode(NetworkNodeType n)
    {  
        this.node = n;
		this.simulationFlag = true;       
    }
    
    public PoweredNetworkNode(NetworkNodeType n, boolean flag)
    {  
        this.node = n;
		this.simulationFlag = flag;        
    }
    
    //    public PoweredNetworkNode(JXPathContext ctx, NetworkNodeType n, boolean flag)
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
        
        if( node.getStatus() == NetworkNodeStatusType.ON ) {
            
            double totalPps = 0;
            
            Iterator<NetworkPortType> itr = node.getNetworkPort().iterator();
            while(itr.hasNext()) {
                NetworkPortType port = itr.next();
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
