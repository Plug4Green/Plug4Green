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
package org.f4g.util;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.log4j.Logger;
import org.f4g.com.AbstractCom;
import org.f4g.schema.metamodel.CPUType;
import org.f4g.schema.metamodel.CoreLoadType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.CpuUsageType;
import org.f4g.schema.metamodel.HardDiskType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NetworkUsageType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.StorageUsageType;
import org.f4g.schema.metamodel.VirtualMachineType;

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
		if ((parent instanceof ServerType) && name.equals("measuredPower")) {
			log.debug("Setting new measured power!");
			((ServerType) parent).setMeasuredPower(new PowerType());
			return true;
		} else if ((parent instanceof CoreType) && name.equals("coreLoad")) {
			((CoreType) parent).setCoreLoad(new CoreLoadType());
			return true;
		} else if ((parent instanceof HardDiskType) && name.equals("readRate")) {
			((HardDiskType) parent).setReadRate(new IoRateType());
			return true;
		}  else if ((parent instanceof HardDiskType) && name.equals("writeRate")) {
			((HardDiskType) parent).setWriteRate(new IoRateType());
			return true;
		} else
		if ((parent instanceof MainboardType)) {
			if(name.equals("CPU")) {
				((MainboardType) parent).getCPU().add(new CPUType());
				return true;
			} else if (name.equals("memoryUsage")) {
				((MainboardType) parent).setMemoryUsage(new MemoryUsageType());
				return true;
			} else if (name.equals("hardDisk")) {
				((MainboardType) parent).getHardDisk().add(new HardDiskType());
				return true;
			}
		} else 
		if (parent instanceof VirtualMachineType){
			if(name.equals("actualCPUUsage")) {
				((VirtualMachineType) parent).setActualCPUUsage(new CpuUsageType());
				return true;
			} else if (name.equals("actualStorageUsage")) {
				((VirtualMachineType) parent).setActualStorageUsage(new StorageUsageType());
				return true;
			} else if (name.equals("actualNetworkUsage")) {
				((VirtualMachineType) parent).setActualNetworkUsage(new NetworkUsageType());
				return true;
			} else if (name.equals("actualDiskIORate")) {
				((VirtualMachineType) parent).setActualDiskIORate(new IoRateType());
				return true;
			} else if (name.equals("actualMemoryUsage")) {
				((VirtualMachineType) parent).setActualMemoryUsage(new MemoryUsageType());
				return true;
			} 
		}
		return false;
	}
}
