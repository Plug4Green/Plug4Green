/**
* ============================== Header ============================== 
* file:          Wol.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Functions to use powerOnLan and ACPI standby/hibernate
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Functions to use powerOnLan and ACPI standby/hibernate
 * 
 *
 * @author Daniel Brinkers
 */
public class Wol {
	/**
	 * 
	 * Wake up a certain node 
	 * 
	 * @param macAddress of the node to wake up
	 * @throws IOException
	 *
	 * @author Daniel Brinkers
	 */
	public static void wake(String macAddress) throws IOException{
		byte[] mac = new byte[6];
		byte[] pack = new byte[102];
		String[] macStringArray = macAddress.split(":");
		for(int i=0; i<6; ++i){
			mac[i] = (byte) Integer.parseInt(macStringArray[i], 16);
			pack[i] = (byte) 0xff;
		}
		for(int i=0; i<16; ++i){
			for(int ii=0; ii<6; ++ii){
				pack[6+i*6+ii] = mac[ii];
			}
		}
		//InetAddress broadcastInetAddress = NetworkInterface.getNetworkInterfaces().nextElement().getInterfaceAddresses().iterator().next().getBroadcast();
		InetAddress broadcastInetAddress = InetAddress.getByName("255.255.255.255");
		DatagramPacket datagramPacket = new DatagramPacket(pack, 102, broadcastInetAddress, 42);
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(datagramPacket);
        datagramSocket.close();
	}
	/**
	 * 
	 * Send a node into standby state
	 * 
	 * @param InetAddress address of the node
	 * @throws IOException
	 *
	 * @author Daniel Brinkers
	 */
	public static void standby(InetAddress InetAddress) throws IOException{
		//FIXME path
		String cmd = "ssh " + InetAddress.getHostAddress() + "\"echo mem > /home/root/sys_power_state &\"";
		Process p = Runtime.getRuntime().exec(cmd);
		
		int ret = 0;
		while(true){
			try{
				ret = p.exitValue();
				break;
			}catch (IllegalThreadStateException e){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {;
				}
			}
		}
		System.out.println("Exit: " + ret);
	}
	/**
	 * 
	 * Send a node into hibernate state
	 * 
	 * @param InetAddress address of the node
	 * @throws IOException
	 *
	 * @author Daniel Brinkers
	 */
	public static void hibernate(InetAddress InetAddress) throws IOException, InterruptedException{
		String cmd = "ssh " + InetAddress.getHostAddress() + "\"cat echo > /home/root/sys_power_state &\"";
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
		System.out.println("Exit: " + p.exitValue());
	}
}
