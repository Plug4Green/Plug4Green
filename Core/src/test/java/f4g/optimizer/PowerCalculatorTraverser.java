/**
* ============================== Header ============================== 
* file:          testPrintTraverser.java
* project:       FIT4Green/Optimizer
* created:       20 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-03-26 16:03:46 +0200 (lun, 26 mar 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1242 $
* 
* short description:
*   This is an example of usage of JAXB-Visitor.
*   It make a traversal an aggregate values at each levels.
* ============================= /Header ==============================
*/
package f4g.optimizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import com.massfords.humantask.DepthFirstTraverserImpl;
import com.massfords.humantask.TraversingVisitor;
import com.massfords.humantask.TraversingVisitorProgressMonitor;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;

import f4g.commons.com.util.PowerData;

import f4g.schemas.java.ENTITIES;
import f4g.schemas.java.ENTITY;
import f4g.schemas.java.IDREFS;
import f4g.schemas.java.Language;
import f4g.schemas.java.NCName;
import f4g.schemas.java.NMTOKEN;
import f4g.schemas.java.NMTOKENS;
import f4g.schemas.java.Name;
import f4g.schemas.java.NegativeInteger;
import f4g.schemas.java.NonNegativeInteger;
import f4g.schemas.java.NonPositiveInteger;
import f4g.schemas.java.PositiveInteger;
import f4g.schemas.java.UnsignedLong;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.actions.ActionRequest.ActionList;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.StandByAction;
import f4g.schemas.java.actions.StartJobAction;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.AllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocationResponse;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.HpcClusterAllocationResponse;
import f4g.schemas.java.allocation.HpcClusterAllocation;
import f4g.schemas.java.allocation.TraditionalVmAllocationResponse;
import f4g.schemas.java.allocation.TraditionalVmAllocation;
import f4g.schemas.java.commontypes.ActionResultCode;
import f4g.schemas.java.commontypes.ActionResultDescription;
import f4g.schemas.java.commontypes.JobID;
import f4g.schemas.java.commontypes.Minutes;
import f4g.schemas.java.commontypes.Node10;
import f4g.schemas.java.commontypes.Operator;
import f4g.schemas.java.commontypes.ProcessorFrequency;
import f4g.schemas.java.commontypes.Reason;
import f4g.schemas.java.commontypes.RequestDateTime;
import f4g.schemas.java.commontypes.Seconds;
import f4g.schemas.java.commontypes.Timeperiod;
import f4g.schemas.java.commontypes.Version;
import f4g.schemas.java.commontypes.VirtualMachineDump;
import f4g.schemas.java.commontypes.VirtualMachineID;
import f4g.schemas.java.constraints.optimizerconstraints.Ban;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraint;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraint.PlacementConstraint;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType.SLA;
import f4g.schemas.java.constraints.optimizerconstraints.Capacity;
import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.Constraint;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType.MaxPowerServer;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoad;
import f4g.schemas.java.constraints.optimizerconstraints.FIT4GreenOptimizerConstraint;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.Fence;
import f4g.schemas.java.constraints.optimizerconstraints.Gather;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.CompPowerGHz;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.GPUFreqGHz;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.HDDCapacity;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.MemorySpaceGB;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.NbOfCores;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.NbOfGPUCores;
import f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.RAIDLevel;
import f4g.schemas.java.constraints.optimizerconstraints.Load;
import f4g.schemas.java.constraints.optimizerconstraints.Lonely;
import f4g.schemas.java.constraints.optimizerconstraints.NodeController;
import f4g.schemas.java.constraints.optimizerconstraints.Period;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.Bandwidth;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerAvgVCPUperCore;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerAvgVRAMperPhyRAM;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerCPULoad;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVMperServer;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVRAMperPhyRAM;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore;
import f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualLoadPerCore;
import f4g.schemas.java.constraints.optimizerconstraints.Root;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType;
import f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType.DedicatedServer;
import f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType.SecureAccessPossibility;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType;
import f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType.ServerGroup;
import f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs;
import f4g.schemas.java.constraints.optimizerconstraints.SpareNodes;
import f4g.schemas.java.constraints.optimizerconstraints.Split;
import f4g.schemas.java.constraints.optimizerconstraints.Spread;
import f4g.schemas.java.constraints.optimizerconstraints.VMGroup;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType.VMFlavor;
import f4g.schemas.java.constraints.placement.DC;
import f4g.schemas.java.constraints.placement.FIT4GreenConstraint;
import f4g.schemas.java.constraints.placement.OneOf;
import f4g.schemas.java.constraints.placement.TS;
import f4g.schemas.java.loadpatterns.LoadPattern;
import f4g.schemas.java.loadpatterns.LoadPatterns;
import f4g.schemas.java.metamodel.*;


public class PowerCalculatorTraverser {
	
	//calculate the aggregated power consumption for any F4G type
	public PowerData calculatePower(Visitable aVisitable) {
		
		//getting a visitor that will compute consuption for each element
        CalculatePowerConsumptionVisitor powerVisitor = new CalculatePowerConsumptionVisitor();
        //getting a monitor that will aggregate consumption
        GetAggregatePowerConsumptionMonitor monitor = new GetAggregatePowerConsumptionMonitor(powerVisitor);
	    
        //setting a Depth first traversal
        TraversingVisitor viz = new TraversingVisitor(new DepthFirstTraverserImpl(), powerVisitor);
	    viz.setProgressMonitor(monitor);

	    //calculate everything
		aVisitable.accept(viz);
		
		//retrieve our level
        Map<Object, Double> counts = monitor.getPowerLevels();
        Double count = counts.get(aVisitable);
        
        //for(Entry<Object, Double> e : counts.entrySet()) {
        //        System.out.printf("%-30s= %3f\n", e.getKey().getClass().getSimpleName() , e.getValue());
        //}
        
        PowerData power = new PowerData();
        power.setActualConsumption(count);
        return power;

	}
	
	/**
	 * calculate power consumption of each node
	 *
	 */
	public class CalculatePowerConsumptionVisitor implements Visitor {

		private double powerConsumption = 0.0;

		public void setPower(double power){
			powerConsumption = power;
		}
		
		protected double getPowerConsumption() {
            return powerConsumption;
        }

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ATM)
		 */
		@Override
		public void visit(ATM aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BladeServer)
		 */
		@Override
		public void visit(BladeServer aBean) {
			visit((Server)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxNetwork)
		 */
		@Override
		public void visit(BoxNetwork aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxRouter)
		 */
		@Override
		public void visit(BoxRouter aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxSwitch)
		 */
		@Override
		public void visit(BoxSwitch aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CPU)
		 */
		@Override
		public void visit(CPU aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Cache)
		 */
		@Override
		public void visit(Cache aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ClusterManagement)
		 */
		@Override
		public void visit(ClusterManagement aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CoolingSystem)
		 */
		@Override
		public void visit(CoolingSystem aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Core)
		 */
		@Override
		public void visit(Core aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Datacenter)
		 */
		@Override
		public void visit(Datacenter aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.E80211XType)
		 */
		@Override
		public void visit(E80211X aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Enclosure)
		 */
		@Override
		public void visit(Enclosure aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Ethernet)
		 */
		@Override
		public void visit(Ethernet aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FIT4Green)
		 */
		@Override
		public void visit(FIT4Green aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Fan)
		 */
		@Override
		public void visit(Fan aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystem)
		 */
		@Override
		public void visit(FileSystem aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Flow)
		 */
		@Override
		public void visit(Flow aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FrameworkCapabilities)
		 */
		@Override
		public void visit(FrameworkCapabilities aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HardDisk)
		 */
		@Override
		public void visit(HardDisk aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HardwareRAID)
		 */
		@Override
		public void visit(HardwareRAID aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HostedHypervisor)
		 */
		@Override
		public void visit(HostedHypervisor aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HostedOperatingSystem)
		 */
		@Override
		public void visit(HostedOperatingSystem aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobActions)
		 */
		@Override
		public void visit(JobActions aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Job)
		 */
		@Override
		public void visit(Job aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Link)
		 */
		@Override
		public void visit(Link aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Mainboard)
		 */
		@Override
		public void visit(Mainboard aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NIC)
		 */
		@Override
		public void visit(NIC aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NativeHypervisor)
		 */
		@Override
		public void visit(NativeHypervisor aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NativeOperatingSystem)
		 */
		@Override
		public void visit(NativeOperatingSystem aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkLoad)
		 */
		@Override
		public void visit(NetworkLoad aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkNode)
		 */
		@Override
		public void visit(NetworkNode aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPort)
		 */
		@Override
		public void visit(NetworkPort aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Node)
		 */
		@Override
		public void visit(Node aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OperatingSystem)
		 */
		@Override
		public void visit(OperatingSystem aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OpticalFDDI)
		 */
		@Override
		public void visit(OpticalFDDI aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PDU)
		 */
		@Override
		public void visit(PDU aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PSU)
		 */
		@Override
		public void visit(PSU aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Queue)
		 */
		@Override
		public void visit(Queue aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAID)
		 */
		@Override
		public void visit(RAID aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAMStick)
		 */
		@Override
		public void visit(RAMStick aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Rack)
		 */
		@Override
		public void visit(Rack aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableNetwork)
		 */
		@Override
		public void visit(RackableNetwork aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableRouter)
		 */
		@Override
		public void visit(RackableRouter aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableServer)
		 */
		@Override
		public void visit(RackableServer aBean) {
			visit((Server)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableSwitch)
		 */
		@Override
		public void visit(RackableSwitch aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Role)
		 */
		@Override
		public void visit(Role aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SAN)
		 */
		@Override
		public void visit(SAN aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SerialPPP)
		 */
		@Override
		public void visit(SerialPPP aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Server)
		 */
		@Override
		public void visit(Server aBean) {
			if(aBean.getStatus() == ServerStatus.ON) {
				setPower(10.0);	
			}
			else
				setPower(0.0);
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Site)
		 */
		@Override
		public void visit(Site aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareApplication)
		 */
		@Override
		public void visit(SoftwareApplication aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareNetwork)
		 */
		@Override
		public void visit(SoftwareNetwork aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareRAID)
		 */
		@Override
		public void visit(SoftwareRAID aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SolidStateDisk)
		 */
		@Override
		public void visit(SolidStateDisk aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageUnit)
		 */
		@Override
		public void visit(StorageUnit aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.TowerServer)
		 */
		@Override
		public void visit(TowerServer aBean) {
			visit((Server)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Tunnel)
		 */
		@Override
		public void visit(Tunnel aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VMActions)
		 */
		@Override
		public void visit(VMActions aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VPN)
		 */
		@Override
		public void visit(VPN aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VirtualMachine)
		 */
		@Override
		public void visit(VirtualMachine aBean) {
			setPower(1.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.WaterCooler)
		 */
		@Override
		public void visit(WaterCooler aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Bandwidth)
		 */
		@Override
		public void visit(f4g.schemas.java.metamodel.Bandwidth aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BitErrorRate)
		 */
		@Override
		public void visit(BitErrorRate aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CUE)
		 */
		@Override
		public void visit(CUE aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CacheLevel)
		 */
		@Override
		public void visit(CacheLevel aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CoreLoad)
		 */
		@Override
		public void visit(CoreLoad aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CpuUsage)
		 */
		@Override
		public void visit(CpuUsage aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Dimension)
		 */
		@Override
		public void visit(Dimension aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Efficiency)
		 */
		@Override
		public void visit(Efficiency aBean) {
			setPower(0.0);
			
		}

//		/* (non-Javadoc)
//		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.EnergySourceFactorType)
//		 */
//		@Override
//		public void visit(EnergySourceFactorType aBean) {
//			setPower(0.0);
//			
//		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystemFragmentation)
		 */
		@Override
		public void visit(FileSystemFragmentation aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystemSpace)
		 */
		@Override
		public void visit(FileSystemSpace aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Frequency)
		 */
		@Override
		public void visit(Frequency aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IntPercent)
		 */
		@Override
		public void visit(IntPercent aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IoRate)
		 */
		@Override
		public void visit(IoRate aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobLimit)
		 */
		@Override
		public void visit(JobLimit aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobPriority)
		 */
		@Override
		public void visit(JobPriority aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobTime)
		 */
		@Override
		public void visit(JobTime aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LUN)
		 */
		@Override
		public void visit(LUN aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Lithography)
		 */
		@Override
		public void visit(Lithography aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Location)
		 */
		@Override
		public void visit(Location aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LogicalUnit)
		 */
		@Override
		public void visit(LogicalUnit aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.MemoryUsage)
		 */
		@Override
		public void visit(MemoryUsage aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPortBufferOccupancy)
		 */
		@Override
		public void visit(NetworkPortBufferOccupancy aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPortBufferSize)
		 */
		@Override
		public void visit(NetworkPortBufferSize aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkTraffic)
		 */
		@Override
		public void visit(NetworkTraffic aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkUsage)
		 */
		@Override
		public void visit(NetworkUsage aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfCores)
		 */
		@Override
		public void visit(NrOfCores aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfCpus)
		 */
		@Override
		public void visit(NrOfCpus aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfLinks)
		 */
		@Override
		public void visit(NrOfLinks aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfNodes)
		 */
		@Override
		public void visit(NrOfNodes aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPlugs)
		 */
		@Override
		public void visit(NrOfPlugs aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPorts)
		 */
		@Override
		public void visit(NrOfPorts aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPstates)
		 */
		@Override
		public void visit(NrOfPstates aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfTransistors)
		 */
		@Override
		public void visit(NrOfTransistors aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PSULoad)
		 */
		@Override
		public void visit(PSULoad aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PUE)
		 */
		@Override
		public void visit(PUE aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Power)
		 */
		@Override
		public void visit(Power aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PropagationDelay)
		 */
		@Override
		public void visit(PropagationDelay aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.QueuePriority)
		 */
		@Override
		public void visit(QueuePriority aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAIDLevel)
		 */
		@Override
		public void visit(f4g.schemas.java.metamodel.RAIDLevel aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAMSize)
		 */
		@Override
		public void visit(RAMSize aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RPM)
		 */
		@Override
		public void visit(RPM aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageCapacity)
		 */
		@Override
		public void visit(StorageCapacity aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageUsage)
		 */
		@Override
		public void visit(StorageUsage aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SwitchFabric)
		 */
		@Override
		public void visit(SwitchFabric aBean) {
			setPower(0.0);
			
		}

			/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Voltage)
		 */
		@Override
		public void visit(Voltage aBean) {
			setPower(0.0);
			
		}



		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OP)
		 */
		@Override
		public void visit(OP aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StripSize)
		 */
		@Override
		public void visit(StripSize aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NodeActions)
		 */
		@Override
		public void visit(NodeActions aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ApplicationBenchmark)
		 */
		@Override
		public void visit(ApplicationBenchmark aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IntAppRank)
		 */
		@Override
		public void visit(IntAppRank aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.GPU)
		 */
		@Override
		public void visit(GPU aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BlockSize)
		 */
		@Override
		public void visit(BlockSize aBean) {
			setPower(0.0);			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Controller)
		 */
		@Override
		public void visit(Controller aBean) {
			setPower(0.0);			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HitRatio)
		 */
		@Override
		public void visit(HitRatio aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LogicalVolume)
		 */
		@Override
		public void visit(LogicalVolume aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NAS)
		 */
		@Override
		public void visit(NAS aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAIDDisk)
		 */
		@Override
		public void visit(RAIDDisk aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.ENTITIES)
		 */
		@Override
		public void visit(ENTITIES aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.ENTITY)
		 */
		@Override
		public void visit(ENTITY aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.IDREFS)
		 */
		@Override
		public void visit(IDREFS aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.Language)
		 */
		@Override
		public void visit(Language aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NCName)
		 */
		@Override
		public void visit(NCName aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NMTOKEN)
		 */
		@Override
		public void visit(NMTOKEN aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NMTOKENS)
		 */
		@Override
		public void visit(NMTOKENS aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.Name)
		 */
		@Override
		public void visit(Name aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NegativeInteger)
		 */
		@Override
		public void visit(NegativeInteger aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NonNegativeInteger)
		 */
		@Override
		public void visit(NonNegativeInteger aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.NonPositiveInteger)
		 */
		@Override
		public void visit(NonPositiveInteger aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.PositiveInteger)
		 */
		@Override
		public void visit(PositiveInteger aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.UnsignedLong)
		 */
		@Override
		public void visit(UnsignedLong aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.ActionRequest)
		 */
		@Override
		public void visit(ActionRequest aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.ActionRequest.ActionList)
		 */
		@Override
		public void visit(ActionList aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.LiveMigrateVMAction)
		 */
		@Override
		public void visit(LiveMigrateVMAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.MoveVMAction)
		 */
		@Override
		public void visit(MoveVMAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.PowerOffAction)
		 */
		@Override
		public void visit(PowerOffAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.PowerOnAction)
		 */
		@Override
		public void visit(PowerOnAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.StandByAction)
		 */
		@Override
		public void visit(StandByAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.StartJobAction)
		 */
		@Override
		public void visit(StartJobAction aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.AllocationRequest)
		 */
		@Override
		public void visit(AllocationRequest aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.AllocationResponse)
		 */
		@Override
		public void visit(AllocationResponse aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.CloudVmAllocationResponse)
		 */
		@Override
		public void visit(CloudVmAllocationResponse aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.CloudVmAllocation)
		 */
		@Override
		public void visit(CloudVmAllocation aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.HpcClusterAllocationResponse)
		 */
		@Override
		public void visit(HpcClusterAllocationResponse aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.HpcClusterAllocation)
		 */
		@Override
		public void visit(HpcClusterAllocation aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.TraditionalVmAllocationResponse)
		 */
		@Override
		public void visit(TraditionalVmAllocationResponse aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.TraditionalVmAllocation)
		 */
		@Override
		public void visit(TraditionalVmAllocation aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ActionResultCode)
		 */
		@Override
		public void visit(ActionResultCode aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ActionResultDescription)
		 */
		@Override
		public void visit(ActionResultDescription aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.JobID)
		 */
		@Override
		public void visit(JobID aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Minutes)
		 */
		@Override
		public void visit(Minutes aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Node10)
		 */
		@Override
		public void visit(Node10 aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Operator)
		 */
		@Override
		public void visit(Operator aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ProcessorFrequency)
		 */
		@Override
		public void visit(ProcessorFrequency aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Reason)
		 */
		@Override
		public void visit(Reason aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.RequestDateTime)
		 */
		@Override
		public void visit(RequestDateTime aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Seconds)
		 */
		@Override
		public void visit(Seconds aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Timeperiod)
		 */
		@Override
		public void visit(Timeperiod aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.Version)
		 */
		@Override
		public void visit(Version aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.VirtualMachineDump)
		 */
		@Override
		public void visit(VirtualMachineDump aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.VirtualMachineID)
		 */
		@Override
		public void visit(VirtualMachineID aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Ban)
		 */
		@Override
		public void visit(Ban aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType)
		 */
		@Override
		public void visit(BoundedClustersType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType.Cluster)
		 */
		@Override
		public void visit(Cluster aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraint)
		 */
		@Override
		public void visit(BoundedPlacementConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraint.PlacementConstraint)
		 */
		@Override
		public void visit(PlacementConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType)
		 */
		@Override
		public void visit(BoundedPoliciesType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType.Policy)
		 */
		@Override
		public void visit(Policy aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType)
		 */
		@Override
		public void visit(BoundedSLAsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType.SLA)
		 */
		@Override
		public void visit(SLA aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SLAType.SLA)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.SLAType.SLA aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Capacity)
		 */
		@Override
		public void visit(Capacity aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.CapacityType)
		 */
		@Override
		public void visit(CapacityType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ClusterType)
		 */
		@Override
		public void visit(ClusterType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Constraint)
		 */
		@Override
		public void visit(Constraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Constraint.PlacementConstraint)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.Constraint.PlacementConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType)
		 */
		@Override
		public void visit(EnergyConstraintsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType.MaxPowerServer)
		 */
		@Override
		public void visit(MaxPowerServer aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoad)
		 */
		@Override
		public void visit(ExpectedLoad aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.FIT4GreenOptimizerConstraint)
		 */
		@Override
		public void visit(FIT4GreenOptimizerConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.FederationType)
		 */
		@Override
		public void visit(FederationType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Fence)
		 */
		@Override
		public void visit(Fence aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Gather)
		 */
		@Override
		public void visit(Gather aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType)
		 */
		@Override
		public void visit(HardwareConstraintsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.CompPowerGHz)
		 */
		@Override
		public void visit(CompPowerGHz aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.GPUFreqGHz)
		 */
		@Override
		public void visit(GPUFreqGHz aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.HDDCapacity)
		 */
		@Override
		public void visit(HDDCapacity aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.MemorySpaceGB)
		 */
		@Override
		public void visit(MemorySpaceGB aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.NbOfCores)
		 */
		@Override
		public void visit(NbOfCores aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.NbOfGPUCores)
		 */
		@Override
		public void visit(NbOfGPUCores aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.HardwareConstraintsType.RAIDLevel)
		 */
		@Override
		public void visit(RAIDLevel aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Load)
		 */
		@Override
		public void visit(Load aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Lonely)
		 */
		@Override
		public void visit(Lonely aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.NodeController)
		 */
		@Override
		public void visit(NodeController aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Period)
		 */
		@Override
		public void visit(Period aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.PlacementConstraint)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.PlacementConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.PolicyType)
		 */
		@Override
		public void visit(PolicyType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType)
		 */
		@Override
		public void visit(QoSConstraintsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.Bandwidth)
		 */
		@Override
		public void visit(Bandwidth aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerAvgVCPUperCore)
		 */
		@Override
		public void visit(MaxServerAvgVCPUperCore aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerAvgVRAMperPhyRAM)
		 */
		@Override
		public void visit(MaxServerAvgVRAMperPhyRAM aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxServerCPULoad)
		 */
		@Override
		public void visit(MaxServerCPULoad aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVMperServer)
		 */
		@Override
		public void visit(MaxVMperServer aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVRAMperPhyRAM)
		 */
		@Override
		public void visit(MaxVRAMperPhyRAM aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualCPUPerCore)
		 */
		@Override
		public void visit(MaxVirtualCPUPerCore aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.QoSConstraintsType.MaxVirtualLoadPerCore)
		 */
		@Override
		public void visit(MaxVirtualLoadPerCore aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Root)
		 */
		@Override
		public void visit(Root aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SLAType)
		 */
		@Override
		public void visit(SLAType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType)
		 */
		@Override
		public void visit(SecurityConstraintsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType.DedicatedServer)
		 */
		@Override
		public void visit(DedicatedServer aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SecurityConstraintsType.SecureAccessPossibility)
		 */
		@Override
		public void visit(SecureAccessPossibility aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType)
		 */
		@Override
		public void visit(ServerGroupType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ServerGroupType.ServerGroup)
		 */
		@Override
		public void visit(ServerGroup aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SpareCPUs)
		 */
		@Override
		public void visit(SpareCPUs aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.SpareNodes)
		 */
		@Override
		public void visit(SpareNodes aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Split)
		 */
		@Override
		public void visit(Split aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.Spread)
		 */
		@Override
		public void visit(Spread aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.VMGroup)
		 */
		@Override
		public void visit(VMGroup aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType)
		 */
		@Override
		public void visit(VMFlavorType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType.VMFlavor)
		 */
		@Override
		public void visit(VMFlavor aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Ban)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Ban aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Capacity)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Capacity aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Constraint)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.placement.Constraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.DC)
		 */
		@Override
		public void visit(DC aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.FIT4GreenConstraint)
		 */
		@Override
		public void visit(FIT4GreenConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Fence)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Fence aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Gather)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Gather aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Lonely)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Lonely aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.OneOf)
		 */
		@Override
		public void visit(OneOf aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.PlacementConstraint)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.placement.PlacementConstraint aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Root)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Root aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.ServerGroup)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.placement.ServerGroup aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Split)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Split aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.Spread)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.Spread aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.TS)
		 */
		@Override
		public void visit(TS aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.VMGroup)
		 */
		@Override
		public void visit(f4g.schemas.java.constraints.placement.VMGroup aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.DC)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.DC aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.LoadPattern)
		 */
		@Override
		public void visit(LoadPattern aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.LoadPatterns)
		 */
		@Override
		public void visit(LoadPatterns aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.Load)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.Load aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.Period)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.Period aBean) {
			// TODO Auto-generated method stub
			
		}
		
	}
	

	/**
	 * Aggregate power consumption at each levels
	 *
	 */
	public class GetAggregatePowerConsumptionMonitor implements TraversingVisitorProgressMonitor {

		Map<Object,Double> counts = new LinkedHashMap();
	    Stack stack = new Stack();
	    CalculatePowerConsumptionVisitor mVisitor;

	    public GetAggregatePowerConsumptionMonitor(CalculatePowerConsumptionVisitor aVisitor) {
            mVisitor = aVisitor;
        }
        
        public void visited(Visitable aBean) {
        	Double myCount = new Double(mVisitor.getPowerConsumption());
                getPowerLevels().put(aBean, myCount.doubleValue());
                push(aBean);
        }
        
        public void traversed(Visitable aBean) {
                pop();
                aggregatePower(aBean);
        }

        protected void push(Object aBean) {
                stack.push(aBean);
        }
        
        protected Object peek() {
                return stack.peek();
        }
        
        protected Object pop() {
                return stack.pop();
        }

        public Map<Object, Double> getPowerLevels() {
                return counts;
        }

        protected void aggregatePower(Object bean) {
                if (stack.isEmpty())
                        return;
                Object parent = peek();
                Double parentCount = getPowerLevels().get(parent);
                if (parentCount == null)
                        parentCount = new Double(0.0);
                Double myCount = getPowerLevels().get(bean);
                getPowerLevels().put(parent, parentCount.doubleValue() + myCount.intValue());
        }

		
	}
	
	public static void main(String[] args) {
		
		Site site = new Site();
		
		Datacenter dataCenter1 = new Datacenter();
		Datacenter dataCenter2 = new Datacenter();
		site.getDatacenter().add(dataCenter1);
		site.getDatacenter().add(dataCenter2);
	
		PowerCalculatorTraverser test = new PowerCalculatorTraverser();
		
		test.calculatePower(site);
	
	}
	
}
