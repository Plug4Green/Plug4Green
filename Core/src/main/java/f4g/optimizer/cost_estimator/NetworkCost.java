/**
 * f4gcost_estimator/NetworkCost.java
 *
 * Ricardo Lent
 */

package f4g.optimizer.cost_estimator;
import java.util.*;

import org.jscience.economics.money.Currency;
import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;

import javax.measure.quantity.*;

import static javax.measure.unit.SI.*;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.NetworkNode;
import f4g.schemas.java.metamodel.NetworkPort;
import f4g.schemas.java.metamodel.VirtualMachine;
import f4g.schemas.java.metamodel.Site;
import f4g.commons.optimizer.ICostEstimator;
import f4g.commons.power.IPowerCalculator;
import f4g.powerCalculator.power.PoweredNetworkNode;
import f4g.commons.optimizer.OptimizationObjective;
import f4g.optimizer.utils.Utils;
import f4g.optimizer.cloud.SLAReader;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.commons.util.Util;


/**
 * class NetworkCost: estimates network energy costs for both inter and intra site
 * 
 */

public class NetworkCost implements ICostEstimator {
    
    public class Protocol extends Object {
        public static final int SSH = 0;
        public static final int HTTP = 1;
        public static final int UDT = 2;
    }
    
    private int defaultNetworkProto = NetworkCost.Protocol.SSH;
    private int defaultPacketSize = 512;
    private OptimizationObjective optiObjective = OptimizationObjective.Power;
    private VMFlavorType currentVMFlavor;
   
    
    public NetworkCost(OptimizationObjective optObj, VMFlavorType vm) {
        super();
        optiObjective = optObj;
        currentVMFlavor = vm;
    }

    //TODO: remove this (broken) constructor
    public NetworkCost() {
        super();
    }
	public boolean dispose() {
        return true;
    }
    
    
	/**
	 * Calculates the energy used for moving a VM from one server to another
     *
     * Assumes:
     *
     *     - end hosts have a single network port
     *
     *     - 
     *
	 * 
	 * @param the origin server, the destination server, the VM to move, the complete model
	 * @return the energy
	 */
	public Amount<Energy> moveEnergyCost(NetworkNode srcServer, NetworkNode dstServer, VirtualMachine VM, FIT4Green model) 
    {  
        ArrayList<NetworkNode> route = calculateRoute(srcServer, dstServer, model);   
        double throughput = estimateThroughput(route);   
        double nbytes = 0.;
        
        VMFlavorType.VMFlavor SLA_VM = null;
		if(VM.getActualStorageUsage() == null || VM.getActualMemoryUsage() == null) {
			SLA_VM = Util.findVMByName(VM.getCloudVm(), currentVMFlavor);	
            nbytes = SLA_VM.getExpectedLoad().getVRamUsage().getValue() + SLA_VM.getCapacity().getVHardDisk().getValue();       // check units 
        }
        else {
            nbytes = VM.getActualStorageUsage().getValue() + VM.getActualMemoryUsage().getValue();
        }

		Amount<Duration> xfer_time = estimateXferTime(throughput, nbytes);   
		Amount<Power> totalPower = calculatePowerNetwork(route, throughput, model).plus( calculatePowerEndHosts(srcServer, dstServer, throughput, model) );

        return Amount.valueOf(totalPower.doubleValue(WATT) * xfer_time.doubleValue(SECOND), JOULE);        
    }
    
    
    public Amount<Duration> 
    moveDownTimeCost(NetworkNode srcServer, NetworkNode dstServer, VirtualMachine VM, FIT4Green model)
    {
        ArrayList<NetworkNode> route = calculateRoute(srcServer, dstServer, model);
        double throughput = estimateThroughput(route);
        double nbytes = 0.;
        
        VMFlavorType.VMFlavor SLA_VM = null;
		if(VM.getActualStorageUsage() == null || VM.getActualMemoryUsage() == null) {
//		if(VM.getCloudVmType() != null) {
			SLA_VM = Util.findVMByName(VM.getCloudVm(), currentVMFlavor);	
            nbytes = SLA_VM.getExpectedLoad().getVRamUsage().getValue() + SLA_VM.getExpectedLoad().getVDiskLoad().getValue();
		}
        else {
            nbytes = VM.getActualStorageUsage().getValue() + VM.getActualMemoryUsage().getValue();
        }
     
        return estimateXferTime(throughput, nbytes);
    }
    
    
    public Amount<Money> moveFinancialCost(NetworkNode srcServer, NetworkNode dstServer, VirtualMachine VM, FIT4Green model) {
        
    	Amount<Money> money = Amount.valueOf(0.0, Currency.EUR);
        return money;
    }
    
    
    // ========================================================================================================
    
    
    protected Amount<Duration> 
    estimateXferTime(double throughput, double nbytes) 
    {
        double xfer_time = 0.0;
        
        if( throughput > 0.0 )
                xfer_time = (nbytes * estimateProtocolOverhead()) / throughput;
                
        return Amount.valueOf(xfer_time, SECOND);    
    }
    
    
    protected Amount<Power> 
    calculatePowerEndHosts(NetworkNode srcServer, NetworkNode dstServer, double throughput, FIT4Green model)
    {
    	Amount<Power> total = Amount.valueOf(0.0, WATT);
        List<NetworkPort> sportlst = srcServer.getNetworkPort();
        List<NetworkPort> dportlst = dstServer.getNetworkPort();
        
        if( sportlst.size() > 0 && dportlst.size() > 0 ) {
            
            NetworkPort sport = sportlst.get(0);
            NetworkPort dport = dportlst.get(0);            
            
            Site ssite = null, dsite = null;
            
            if(model != null) {
                ssite = Utils.getNetworkNodeSite(srcServer, model);
                dsite = Utils.getNetworkNodeSite(dstServer, model);
            }
            
            // the power consumption at the end hosts for 
            
            
            double s_idle   = srcServer.getPowerIdle().getValue();
            double s_max    = srcServer.getPowerMax().getValue();
            double s_ppsmax = srcServer.getProcessingBandwidth().getValue();
            double d_idle   = dstServer.getPowerIdle().getValue();
            double d_max    = dstServer.getPowerMax().getValue();
            double d_ppsmax = dstServer.getProcessingBandwidth().getValue();
            
            double spower = PoweredNetworkNode.trafficToPower(s_idle, s_max, throughput, s_ppsmax).doubleValue(WATT) - s_idle;
            double dpower = PoweredNetworkNode.trafficToPower(d_idle, d_max, throughput, d_ppsmax).doubleValue(WATT) - d_idle;
            
            if( ssite != null ) {
                double f;
            	if(optiObjective == OptimizationObjective.Power) 
            		f = ssite.getPUE().getValue();
            	else 
            		f = ssite.getCUE().getValue();
                spower = spower * f;
            }
            
            if( dsite != null ) {
                double f;
            	if(optiObjective == OptimizationObjective.Power) 
            		f = dsite.getPUE().getValue();
            	else 
            		f = dsite.getCUE().getValue();
                dpower = dpower * f;
            }
            
            total = Amount.valueOf(spower + dpower, WATT);
        }
        
        return total;
    }
    
    
    
    protected Amount<Power> 
    calculatePowerNetwork(ArrayList<NetworkNode> route, double throughput, FIT4Green model)
    {
    	Amount<Power> total = Amount.valueOf(0.0, WATT);
        
        if( route.size() > 2 ) {
            // add network cost
            for(int i=1; i<(route.size()-1); i++) {
                NetworkNode node = route.get(i);
                double a = node.getPowerIdle().getValue(), b = node.getPowerMax().getValue(), c = node.getProcessingBandwidth().getValue();                
                Amount<Power> node_pwr = PoweredNetworkNode.trafficToPower( a, b, throughput, c ).minus(PoweredNetworkNode.trafficToPower( a, b, 0, c ));
                
                
                Site site = Utils.getNetworkNodeSite(node, model);
                if( site != null ) {
                    double f;
                    if(optiObjective == OptimizationObjective.Power) 
                        f = site.getPUE().getValue();
                    else 
                        f = site.getCUE().getValue();
                    node_pwr = node_pwr.times(f);
                }
                total = total.plus( node_pwr );
           }
        }
        
        return total;        
    }
    
    
    // ========================================================================================================
    
    
    protected double 
    estimateThroughput(ArrayList<NetworkNode> route)            
    {
        double throughput = 0.;
        if( route.size() <= 0 ) return 0.;
        
        // System.out.println( ">>>>" + route.get(0).getNetworkPort().size() );
        
        throughput = route.get(0).getNetworkPort().get(0).getLineCapacity().getValue() / 8.0;
        
        for(NetworkNode node : route) {
            double prate = node.getNetworkPort().get(0).getLineCapacity().getValue();
            if( prate < throughput ) throughput = prate;                                        // PENDING: Replace with residual bandwidth 
        }
        
        if( defaultNetworkProto != NetworkCost.Protocol.UDT )
            throughput *= 0.75;     // approx for TCP xfers
        
        return throughput;
    }
    
    
    protected double 
    estimateProtocolOverhead() {
        
        double data_pktlen = defaultPacketSize+14+20+20;	// MSS + ETH header + IP header + TCP header    -- in bytes
        // double ack_pktlen = 64;		// in bytes
        
        if( defaultNetworkProto == NetworkCost.Protocol.SSH )
            data_pktlen += 6;                   // from  measurements
        else
            if( defaultNetworkProto == NetworkCost.Protocol.HTTP )
                data_pktlen += 10;              // needs to be validated
            else                
                data_pktlen -= 12;              // replace TCP header with UDP header
        
        return ((double) data_pktlen)/defaultPacketSize;
    }                
    
    
    // ========================================================================================================
    
    protected ArrayList<NetworkNode>
    calculateRoute(NetworkNode srcServer, NetworkNode dstServer, FIT4Green model)
    {  
        boolean found = false;
        LinkedList<NetworkNode> q  = new LinkedList();
        Map<NetworkNode, NetworkNode> predecessor = new HashMap<NetworkNode, NetworkNode>();
        //System.out.println("S: " + srcServer.getFrameworkID() + " " + dstServer.getFrameworkID() );
        // traverse graph
        q.addLast( srcServer );
        predecessor.put(srcServer, srcServer);

       	if(model!=null){
        while( q.size() > 0 ) {          
            NetworkNode node = q.removeFirst();        
            System.out.println( "-> " + node.getFrameworkID() );          
            if( node.getFrameworkID() == dstServer.getFrameworkID() ) {
                System.out.println( "found" );
                found = true;
                break;
            }
            else {
               List<NetworkPort> portlist = node.getNetworkPort();
                if( portlist == null ) {
                    //System.out.println( "no route" );
                    return new ArrayList();     // no route
                }
                for(NetworkPort port : portlist) {                
                    NetworkNode neighbor = new NetworkNode();                  
                    if( port.getNetworkPortRef() != null ) {
                        try {
                            Server srv = (Server) Utils.findServerByName(model, (String) port.getNetworkPortRef());
                            neighbor = (NetworkNode) (srv.getMainboard().get(0).getEthernetNIC().get(0));
                            }
                        catch (NoSuchElementException e) {
                        	neighbor = (NetworkNode) Utils.findNetworkNodeByName(model, (String) port.getNetworkPortRef());
                        }
                        System.out.println( "neighbor: " + neighbor.getFrameworkID() );
                    }
                    else {
                        System.out.println( "no route" );
                        return new ArrayList();     // no route
                    }
                    if( ! predecessor.containsKey(neighbor) ) {
                        q.addLast( neighbor );
                        predecessor.put(neighbor, node);
                    }
                }
                
            }
        }
    }       
        
        // prepare return structure
        
        LinkedList<NetworkNode> route = new LinkedList();
        if( found ) {
            NetworkNode node = dstServer;
            while( ! node.equals( srcServer ) ) {
                route.addFirst(node);
                node = predecessor.get( node );
            }
            route.addFirst(srcServer);          
        }   
        else {  // unknown interconnection network
            route.addLast( srcServer );
            route.addLast( dstServer );
        }
        
        return new ArrayList(route);
    }
    
    
    
    
    
}


