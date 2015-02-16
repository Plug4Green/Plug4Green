/**
* ============================== Header ============================== 
* file:          NetworkControl.java
* project:       FIT4Green/Optimizer
* created:       28/03/2012 by rlent
* 
* short description:
*   Determines which router/switches could be switched off
* ============================= /Header ==============================
*/
package f4g.optimizer.cloudTraditional;

import f4g.schemas.java.metamodel.FrameworkCapabilities;
import f4g.schemas.java.metamodel.NetworkNode;
import f4g.schemas.java.metamodel.NetworkPort;
import f4g.schemas.java.metamodel.NetworkNodeStatus;
import f4g.schemas.java.actions.ObjectFactory;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.ActionRequest.ActionList;
import f4g.schemas.java.actions.AbstractBaseAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.FIT4Green;
import f4g.schemas.java.metamodel.ServerStatus;
import f4g.schemas.java.metamodel.Server;
import com.rits.cloning.Cloner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import javax.xml.bind.JAXBElement;


public class NetworkControl {

	public static ActionList getOnOffActions(FIT4Green federation, FIT4Green model) {

		ActionList actionList = new ActionList();
        actionList.getAction();     // initialize
		ObjectFactory objectFactory = new ObjectFactory();
        List<NetworkNode> allSwitches = Utils.getAllNetworkDeviceNodes(federation);

        for( NetworkNode node : allSwitches ) {
            boolean should_be_on = false; 
            boolean access_sw = false;
            for( NetworkPort port : node.getNetworkPort() ) {
                if( port.getNetworkPortRef() != null ) {
      
                    try {
                        Server server = Utils.findServerByName(federation, (String) port.getNetworkPortRef());
                        access_sw = true;
                        if( server.getStatus() == ServerStatus.ON) {
                            should_be_on = true;
                            break;
                        }
                    }
                    catch (NoSuchElementException e) {
                    }
                     
                }
            }
            if( access_sw && should_be_on && node.getStatus() != NetworkNodeStatus.ON ) {
                    // switch on
                    PowerOnAction action = new PowerOnAction();
                    action.setNodeName(node.getID());  
            		FrameworkCapabilities fc = (FrameworkCapabilities) node.getFrameworkRef();
            		action.setFrameworkName(fc.getFrameworkName());
                    actionList.getAction().add(objectFactory.createPowerOn(action));
            }
            else            
                if( access_sw && ! should_be_on && node.getStatus() == NetworkNodeStatus.ON ) {
                    // switch off
                    PowerOffAction action = new PowerOffAction();
                    action.setNodeName(node.getID());  
            		FrameworkCapabilities fc = (FrameworkCapabilities) node.getFrameworkRef();         		
            		action.setFrameworkName(fc.getFrameworkName());
                    actionList.getAction().add(objectFactory.createPowerOff(action));                
               }
        }

		return actionList;
		
	}
    
    
    public static FIT4Green performOnOffs(FIT4Green federation, ActionList actionList) {
        
        Cloner cloner=new Cloner();
        FIT4Green newFederation = cloner.deepClone(federation);
        
        for (JAXBElement<? extends AbstractBaseAction> action : actionList.getAction()) {
            
            if( action.getValue() instanceof PowerOnAction) {
                String nodeName = ((PowerOnAction)action.getValue()).getNodeName();
                NetworkNode node = Utils.findNetworkNodeByName(newFederation, nodeName);
                node.setStatus(NetworkNodeStatus.ON);
            }
            else{
                String nodeName = ((PowerOffAction)action.getValue()).getNodeName();
                NetworkNode node = Utils.findNetworkNodeByName(newFederation, nodeName);
                node.setStatus(NetworkNodeStatus.OFF);
            }
            
        }
        
        return newFederation;
    }

    
  //  protected static NetworkNode findNetworkNodeByName(FIT4Green f4g, final String frameWorkID) {
    
  //      Iterator<NetworkNode> it = Utils.getAllNetworkDeviceNodes(f4g).iterator();
  //      Predicate<NetworkNode> isID = new Predicate<NetworkNode>() {
  //          @Override public boolean apply(NetworkNode s) {
  //              return s.getFrameworkID().equals(frameWorkID);
  //          }
  //      };
  //      return Iterators.find(it, isID);
  //  }
    
    



    
    
    

}
