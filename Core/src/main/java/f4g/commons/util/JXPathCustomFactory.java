/**
 * ============================== Header ============================== 
 * file:          JXPathCustomFactory.java
 * project:       FIT4Green/Commons
 * created:       Sep 9, 2011 by FIT4Green
 * last modified: $LastChangedDate$ by $LastChangedBy$
 * revision:      $LastChangedRevision$
 * 
 * short description:
 *   {To be completed}
 * ============================= /Header ==============================
 */
package f4g.commons.util;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.log4j.Logger;
import f4g.commons.com.AbstractCom;
import f4g.schemas.java.metamodel.CPU;
import f4g.schemas.java.metamodel.CoreLoad;
import f4g.schemas.java.metamodel.Core;
import f4g.schemas.java.metamodel.CpuUsage;
import f4g.schemas.java.metamodel.HardDisk;
import f4g.schemas.java.metamodel.IoRate;
import f4g.schemas.java.metamodel.Mainboard;
import f4g.schemas.java.metamodel.MemoryUsage;
import f4g.schemas.java.metamodel.NetworkUsage;
import f4g.schemas.java.metamodel.Power;
import f4g.schemas.java.metamodel.Server;
import f4g.schemas.java.metamodel.StorageUsage;
import f4g.schemas.java.metamodel.VirtualMachine;

/**
 *
 * 
 * 
 * @author FIT4Green
 */
public class JXPathCustomFactory extends AbstractFactory {
	static Logger log = Logger.getLogger(JXPathCustomFactory.class.getName()); //

	/**
	 * 
	 */
	public JXPathCustomFactory() {
		//log.debug("****Factory created!");
	}

	public boolean createObject(JXPathContext context, Pointer pointer,
			Object parent, String name, int index) {
		log.debug("Setting new node " + name + " from Factory. Parent Type:" + parent.getClass().getName());
		if ((parent instanceof Server) && name.equals("measuredPower")) {
			log.debug("Setting new measured power!");
			((Server) parent).setMeasuredPower(new Power());
			return true;
		} else if ((parent instanceof Core) && name.equals("coreLoad")) {
			((Core) parent).setCoreLoad(new CoreLoad());
			return true;
		} else if ((parent instanceof HardDisk) && name.equals("readRate")) {
			((HardDisk) parent).setReadRate(new IoRate());
			return true;
		}  else if ((parent instanceof HardDisk) && name.equals("writeRate")) {
			((HardDisk) parent).setWriteRate(new IoRate());
			return true;
		} else
		if ((parent instanceof Mainboard)) {
			if(name.equals("CPU")) {
				((Mainboard) parent).getCPU().add(new CPU());
				return true;
			} else if (name.equals("memoryUsage")) {
				((Mainboard) parent).setMemoryUsage(new MemoryUsage());
				return true;
			} else if (name.equals("hardDisk")) {
				((Mainboard) parent).getHardDisk().add(new HardDisk());
				return true;
			}
		} else 
		if (parent instanceof VirtualMachine){
			if(name.equals("actualCPUUsage")) {
				((VirtualMachine) parent).setActualCPUUsage(new CpuUsage());
				return true;
			} else if (name.equals("actualStorageUsage")) {
				((VirtualMachine) parent).setActualStorageUsage(new StorageUsage());
				return true;
			} else if (name.equals("actualNetworkUsage")) {
				((VirtualMachine) parent).setActualNetworkUsage(new NetworkUsage());
				return true;
			} else if (name.equals("actualDiskIORate")) {
				((VirtualMachine) parent).setActualDiskIORate(new IoRate());
				return true;
			} else if (name.equals("actualMemoryUsage")) {
				((VirtualMachine) parent).setActualMemoryUsage(new MemoryUsage());
				return true;
			} 
		}
		return false;
	}
}
