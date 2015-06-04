/*

 * VxipmiRunner.java 

 * Created on 2011-09-20

 *

 * Copyright (c) Verax Systems 2011.

 * All rights reserved.

 *

 * This software is furnished under a license. Use, duplication,

 * disclosure and all other uses are restricted to the rights

 * specified in the written license agreement.

 */

package f4g.com.opennebula;


import java.net.InetAddress;
import com.veraxsystems.vxipmi.api.async.ConnectionHandle;
import com.veraxsystems.vxipmi.api.sync.IpmiConnector;
import com.veraxsystems.vxipmi.coding.commands.IpmiVersion;
import com.veraxsystems.vxipmi.coding.commands.PrivilegeLevel;
import com.veraxsystems.vxipmi.coding.commands.chassis.ChassisControl;
import com.veraxsystems.vxipmi.coding.commands.chassis.ChassisControlResponseData;
import com.veraxsystems.vxipmi.coding.commands.chassis.GetChassisStatus;
import com.veraxsystems.vxipmi.coding.commands.chassis.GetChassisStatusResponseData;
import com.veraxsystems.vxipmi.coding.commands.chassis.PowerCommand;
import com.veraxsystems.vxipmi.coding.commands.session.SetSessionPrivilegeLevel;
import com.veraxsystems.vxipmi.coding.protocol.AuthenticationType;
import com.veraxsystems.vxipmi.coding.security.CipherSuite;

public class Ipmi {

        private IpmiConnector connector;
	private ConnectionHandle handle;
	private CipherSuite cs;

	// Create the connector, specify port that will be used to communicate
       	// with the remote host. The UDP layer starts listening at this port, so
        // no 2 connectors can work at the same time on the same port.
	public void init(String hostIP){
		try{
	        connector = new IpmiConnector(6000);

        // Create the connection and get the handle, specify IP address of the
        // remote host. The connection is being registered in ConnectionManager,
        // the handle will be needed to identify it among other connections
        // (target IP address isn't enough, since we can handle multiple
        // connections to the same host)
        	handle = connector.createConnection(InetAddress.getByName(hostIP));

        // Get available cipher suites list via getAvailableCipherSuites and
        // pick one of them that will be used further in the session.
	        cs = connector.getAvailableCipherSuites(handle).get(0);

        // Provide chosen cipher suite and privilege level to the remote host.
        // From now on, your connection handle will contain these information.
        	connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.Administrator);

        // Start the session, provide username and password, and optionally the
        // BMC key (only if the remote host has two-key authentication enabled,
        // otherwise this parameter should be null)
	        connector.openSession(handle, "ipmi", "1pm1t3st", null);

        // Set session privilege level to administrator, as ChassisControl command requires this level
	        connector.sendMessage(handle, new SetSessionPrivilegeLevel(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus, PrivilegeLevel.Administrator));
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public String getStatus(){
		String status=null;
		try{
	        // Send some message and read the response
        	GetChassisStatusResponseData rd = (GetChassisStatusResponseData) connector.sendMessage(handle, new GetChassisStatus(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));

        	if (rd.isPowerOn()) status="up";
		else status="down";
		} catch (Exception e){
			e.printStackTrace();
		}
		return status;
	}

	public void powerOn(){
		try{
		        ChassisControl chassisControl = null;
			chassisControl = new ChassisControl(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus, PowerCommand.PowerUp);
	        	ChassisControlResponseData data = (ChassisControlResponseData) connector.sendMessage(handle, chassisControl);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

        public void powerOff(){
		try{
	                ChassisControl chassisControl = null;
        	        chassisControl = new ChassisControl(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus, PowerCommand.PowerDown);
	        	ChassisControlResponseData data = (ChassisControlResponseData) connector.sendMessage(handle, chassisControl);
		} catch (Exception e){
			e.printStackTrace();
		}
        }

	public void endSession(){
		try{
	        // Close the session
	        connector.closeSession(handle);

	        // Close connection manager and release the listener port.
        	connector.tearDown();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
