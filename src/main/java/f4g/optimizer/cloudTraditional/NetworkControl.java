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

import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.NetworkNodeType;
import f4g.schemas.java.metamodel.NetworkPortType;
import f4g.schemas.java.metamodel.NetworkNodeStatusType;
import f4g.schemas.java.actions.ObjectFactory;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.ActionRequestType.ActionList;
import f4g.schemas.java.actions.AbstractBaseActionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import com.rits.cloning.Cloner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import javax.xml.bind.JAXBElement;


public class NetworkControl {

	public static ActionList getOnOffActions(FIT4GreenType federation, FIT4GreenType model) {

		ActionList actionList = new ActionList();
        actionList.getAction();     // initialize
		ObjectFactory objectFactory = new ObjectFactory();
        List<NetworkNodeType> allSwitches = Utils.getAllNetworkDeviceNodes(federation);

        for( NetworkNodeType node : allSwitches ) {
            boolean should_be_on = false; 
            boolean access_sw = false;
            for( NetworkPortType port : node.getNetworkPort() ) {
                if( port.getNetworkPortRef() != null ) {
      
                    try {
                        ServerType server = Utils.findServerByName(federation, (String) port.getNetworkPortRef());
                        access_sw = true;
                        if( server.getStatus() == ServerStatusType.ON) {
                            should_be_on = true;
                            break;
                        }
                    }
                    catch (NoSuchElementException e) {
                    }
                     
                }
            }
            if( access_sw && should_be_on && node.getStatus() != NetworkNodeStatusType.ON ) {
                    // switch on
                    PowerOnActionType action = new PowerOnActionType();
                    action.setNodeName(node.getID());  
            		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) node.getFrameworkRef();
            		action.setFrameworkName(fc.getFrameworkName());
                    actionList.getAction().add(objectFactory.createPowerOn(action));
            }
            else            
                if( access_sw && ! should_be_on && node.getStatus() == NetworkNodeStatusType.ON ) {
                    // switch off
                    PowerOffActionType action = new PowerOffActionType();
                    action.setNodeName(node.getID());  
            		FrameworkCapabilitiesType fc = (FrameworkCapabilitiesType) node.getFrameworkRef();         		
            		action.setFrameworkName(fc.getFrameworkName());
                    actionList.getAction().add(objectFactory.createPowerOff(action));                
               }
        }

		return actionList;
		
	}
    
    
    public static FIT4GreenType performOnOffs(FIT4GreenType federation, ActionList actionList) {
        
        Cloner cloner=new Cloner();
        FIT4GreenType newFederation = cloner.deepClone(federation);
        
        for (JAXBElement<? extends AbstractBaseActionType> action : actionList.getAction()) {
            
            if( action.getValue() instanceof PowerOnActionType) {
                String nodeName = ((PowerOnActionType)action.getValue()).getNodeName();
                NetworkNodeType node = Utils.findNetworkNodeByName(newFederation, nodeName);
                node.setStatus(NetworkNodeStatusType.ON);
            }
            else{
                String nodeName = ((PowerOffActionType)action.getValue()).getNodeName();
                NetworkNodeType node = Utils.findNetworkNodeByName(newFederation, nodeName);
                node.setStatus(NetworkNodeStatusType.OFF);
            }
            
        }
        
        return newFederation;
    }

    
  //  protected static NetworkNodeType findNetworkNodeByName(FIT4GreenType f4g, final String frameWorkID) {
    
  //      Iterator<NetworkNodeType> it = Utils.getAllNetworkDeviceNodes(f4g).iterator();
  //      Predicate<NetworkNodeType> isID = new Predicate<NetworkNodeType>() {
  //          @Override public boolean apply(NetworkNodeType s) {
  //              return s.getFrameworkID().equals(frameWorkID);
  //          }
  //      };
  //      return Iterators.find(it, isID);
  //  }
    
    



    
    
    

}
