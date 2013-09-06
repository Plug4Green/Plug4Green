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
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.ActionRequestType.ActionList;
import f4g.schemas.java.actions.LiveMigrateVMActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;
import f4g.schemas.java.actions.StandByActionType;
import f4g.schemas.java.actions.StartJobActionType;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.AllocationResponseType;
import f4g.schemas.java.allocation.CloudVmAllocationResponseType;
import f4g.schemas.java.allocation.CloudVmAllocationType;
import f4g.schemas.java.allocation.HpcClusterAllocationResponseType;
import f4g.schemas.java.allocation.HpcClusterAllocationType;
import f4g.schemas.java.allocation.TraditionalVmAllocationResponseType;
import f4g.schemas.java.allocation.TraditionalVmAllocationType;
import f4g.schemas.java.commontypes.ActionResultCodeType;
import f4g.schemas.java.commontypes.ActionResultDescriptionType;
import f4g.schemas.java.commontypes.JobIDType;
import f4g.schemas.java.commontypes.MinutesType;
import f4g.schemas.java.commontypes.NodeType10;
import f4g.schemas.java.commontypes.OperatorType;
import f4g.schemas.java.commontypes.ProcessorFrequencyType;
import f4g.schemas.java.commontypes.ReasonType;
import f4g.schemas.java.commontypes.RequestDateTimeType;
import f4g.schemas.java.commontypes.SecondsType;
import f4g.schemas.java.commontypes.TimeperiodType;
import f4g.schemas.java.commontypes.VersionType;
import f4g.schemas.java.commontypes.VirtualMachineDumpType;
import f4g.schemas.java.commontypes.VirtualMachineIDType;
import f4g.schemas.java.constraints.optimizerconstraints.Ban;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedClustersType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraintType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraintType.PlacementConstraint;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedSLAsType.SLA;
import f4g.schemas.java.constraints.optimizerconstraints.Capacity;
import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType;
import f4g.schemas.java.constraints.optimizerconstraints.ConstraintType;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType;
import f4g.schemas.java.constraints.optimizerconstraints.EnergyConstraintsType.MaxPowerServer;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoadType;
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
import f4g.schemas.java.constraints.optimizerconstraints.LoadType;
import f4g.schemas.java.constraints.optimizerconstraints.Lonely;
import f4g.schemas.java.constraints.optimizerconstraints.NodeControllerType;
import f4g.schemas.java.constraints.optimizerconstraints.PeriodType;
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
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType.VMType;
import f4g.schemas.java.constraints.placement.DCType;
import f4g.schemas.java.constraints.placement.FIT4GreenConstraintType;
import f4g.schemas.java.constraints.placement.OneOf;
import f4g.schemas.java.constraints.placement.TSType;
import f4g.schemas.java.loadpatterns.LoadPatternType;
import f4g.schemas.java.loadpatterns.LoadPatternsType;
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
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ATMType)
		 */
		@Override
		public void visit(ATMType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BladeServerType)
		 */
		@Override
		public void visit(BladeServerType aBean) {
			visit((ServerType)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxNetworkType)
		 */
		@Override
		public void visit(BoxNetworkType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxRouterType)
		 */
		@Override
		public void visit(BoxRouterType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BoxSwitchType)
		 */
		@Override
		public void visit(BoxSwitchType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CPUType)
		 */
		@Override
		public void visit(CPUType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CacheType)
		 */
		@Override
		public void visit(CacheType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ClusterManagementType)
		 */
		@Override
		public void visit(ClusterManagementType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CoolingSystemType)
		 */
		@Override
		public void visit(CoolingSystemType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CoreType)
		 */
		@Override
		public void visit(CoreType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.DatacenterType)
		 */
		@Override
		public void visit(DatacenterType aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.E80211XType)
		 */
		@Override
		public void visit(E80211XType aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.EnclosureType)
		 */
		@Override
		public void visit(EnclosureType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.EthernetType)
		 */
		@Override
		public void visit(EthernetType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FIT4GreenType)
		 */
		@Override
		public void visit(FIT4GreenType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FanType)
		 */
		@Override
		public void visit(FanType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystemType)
		 */
		@Override
		public void visit(FileSystemType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FlowType)
		 */
		@Override
		public void visit(FlowType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FrameworkCapabilitiesType)
		 */
		@Override
		public void visit(FrameworkCapabilitiesType aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HardDiskType)
		 */
		@Override
		public void visit(HardDiskType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HardwareRAIDType)
		 */
		@Override
		public void visit(HardwareRAIDType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HostedHypervisorType)
		 */
		@Override
		public void visit(HostedHypervisorType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HostedOperatingSystemType)
		 */
		@Override
		public void visit(HostedOperatingSystemType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobActionsType)
		 */
		@Override
		public void visit(JobActionsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobType)
		 */
		@Override
		public void visit(JobType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LinkType)
		 */
		@Override
		public void visit(LinkType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.MainboardType)
		 */
		@Override
		public void visit(MainboardType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NICType)
		 */
		@Override
		public void visit(NICType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NativeHypervisorType)
		 */
		@Override
		public void visit(NativeHypervisorType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NativeOperatingSystemType)
		 */
		@Override
		public void visit(NativeOperatingSystemType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkLoadType)
		 */
		@Override
		public void visit(NetworkLoadType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkNodeType)
		 */
		@Override
		public void visit(NetworkNodeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPortType)
		 */
		@Override
		public void visit(NetworkPortType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NodeType)
		 */
		@Override
		public void visit(NodeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OperatingSystemType)
		 */
		@Override
		public void visit(OperatingSystemType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OpticalFDDIType)
		 */
		@Override
		public void visit(OpticalFDDIType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PDUType)
		 */
		@Override
		public void visit(PDUType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PSUType)
		 */
		@Override
		public void visit(PSUType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.QueueType)
		 */
		@Override
		public void visit(QueueType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAIDType)
		 */
		@Override
		public void visit(RAIDType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAMStickType)
		 */
		@Override
		public void visit(RAMStickType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackType)
		 */
		@Override
		public void visit(RackType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableNetworkType)
		 */
		@Override
		public void visit(RackableNetworkType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableRouterType)
		 */
		@Override
		public void visit(RackableRouterType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableServerType)
		 */
		@Override
		public void visit(RackableServerType aBean) {
			visit((ServerType)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RackableSwitchType)
		 */
		@Override
		public void visit(RackableSwitchType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RoleType)
		 */
		@Override
		public void visit(RoleType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SANType)
		 */
		@Override
		public void visit(SANType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SerialPPPType)
		 */
		@Override
		public void visit(SerialPPPType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ServerType)
		 */
		@Override
		public void visit(ServerType aBean) {
			if(aBean.getStatus() == ServerStatusType.ON) {
				setPower(10.0);	
			}
			else
				setPower(0.0);
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SiteType)
		 */
		@Override
		public void visit(SiteType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareApplicationType)
		 */
		@Override
		public void visit(SoftwareApplicationType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareNetworkType)
		 */
		@Override
		public void visit(SoftwareNetworkType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SoftwareRAIDType)
		 */
		@Override
		public void visit(SoftwareRAIDType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SolidStateDiskType)
		 */
		@Override
		public void visit(SolidStateDiskType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageUnitType)
		 */
		@Override
		public void visit(StorageUnitType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.TowerServerType)
		 */
		@Override
		public void visit(TowerServerType aBean) {
			visit((ServerType)aBean);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.TunnelType)
		 */
		@Override
		public void visit(TunnelType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VMActionsType)
		 */
		@Override
		public void visit(VMActionsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VPNType)
		 */
		@Override
		public void visit(VPNType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VirtualMachineType)
		 */
		@Override
		public void visit(VirtualMachineType aBean) {
			setPower(1.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.WaterCoolerType)
		 */
		@Override
		public void visit(WaterCoolerType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BandwidthType)
		 */
		@Override
		public void visit(BandwidthType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BitErrorRateType)
		 */
		@Override
		public void visit(BitErrorRateType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CUEType)
		 */
		@Override
		public void visit(CUEType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CacheLevelType)
		 */
		@Override
		public void visit(CacheLevelType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CoreLoadType)
		 */
		@Override
		public void visit(CoreLoadType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.CpuUsageType)
		 */
		@Override
		public void visit(CpuUsageType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.DimensionType)
		 */
		@Override
		public void visit(DimensionType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.EfficiencyType)
		 */
		@Override
		public void visit(EfficiencyType aBean) {
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
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystemFragmentationType)
		 */
		@Override
		public void visit(FileSystemFragmentationType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FileSystemSpaceType)
		 */
		@Override
		public void visit(FileSystemSpaceType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.FrequencyType)
		 */
		@Override
		public void visit(FrequencyType aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IntPercentType)
		 */
		@Override
		public void visit(IntPercentType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IoRateType)
		 */
		@Override
		public void visit(IoRateType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobLimitType)
		 */
		@Override
		public void visit(JobLimitType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobPriorityType)
		 */
		@Override
		public void visit(JobPriorityType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.JobTimeType)
		 */
		@Override
		public void visit(JobTimeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LUNType)
		 */
		@Override
		public void visit(LUNType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LithographyType)
		 */
		@Override
		public void visit(LithographyType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LocationType)
		 */
		@Override
		public void visit(LocationType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LogicalUnitType)
		 */
		@Override
		public void visit(LogicalUnitType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.MemoryUsageType)
		 */
		@Override
		public void visit(MemoryUsageType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPortBufferOccupancyType)
		 */
		@Override
		public void visit(NetworkPortBufferOccupancyType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkPortBufferSizeType)
		 */
		@Override
		public void visit(NetworkPortBufferSizeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkTrafficType)
		 */
		@Override
		public void visit(NetworkTrafficType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NetworkUsageType)
		 */
		@Override
		public void visit(NetworkUsageType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfCoresType)
		 */
		@Override
		public void visit(NrOfCoresType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfCpusType)
		 */
		@Override
		public void visit(NrOfCpusType aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfLinksType)
		 */
		@Override
		public void visit(NrOfLinksType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfNodesType)
		 */
		@Override
		public void visit(NrOfNodesType aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPlugsType)
		 */
		@Override
		public void visit(NrOfPlugsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPortsType)
		 */
		@Override
		public void visit(NrOfPortsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfPstatesType)
		 */
		@Override
		public void visit(NrOfPstatesType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NrOfTransistorsType)
		 */
		@Override
		public void visit(NrOfTransistorsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PSULoadType)
		 */
		@Override
		public void visit(PSULoadType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PUEType)
		 */
		@Override
		public void visit(PUEType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PowerType)
		 */
		@Override
		public void visit(PowerType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PropagationDelayType)
		 */
		@Override
		public void visit(PropagationDelayType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.QueuePriorityType)
		 */
		@Override
		public void visit(QueuePriorityType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAIDLevelType)
		 */
		@Override
		public void visit(RAIDLevelType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAMSizeType)
		 */
		@Override
		public void visit(RAMSizeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RPMType)
		 */
		@Override
		public void visit(RPMType aBean) {
			setPower(0.0);
			
		}

		
		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageCapacityType)
		 */
		@Override
		public void visit(StorageCapacityType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StorageUsageType)
		 */
		@Override
		public void visit(StorageUsageType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.SwitchFabricType)
		 */
		@Override
		public void visit(SwitchFabricType aBean) {
			setPower(0.0);
			
		}

			/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.VoltageType)
		 */
		@Override
		public void visit(VoltageType aBean) {
			setPower(0.0);
			
		}



		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.OPType)
		 */
		@Override
		public void visit(OPType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.StripSizeType)
		 */
		@Override
		public void visit(StripSizeType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NodeActionsType)
		 */
		@Override
		public void visit(NodeActionsType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ApplicationBenchmarkType)
		 */
		@Override
		public void visit(ApplicationBenchmarkType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IntAppRankType)
		 */
		@Override
		public void visit(IntAppRankType aBean) {
			setPower(0.0);
			
		}


		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.GPUType)
		 */
		@Override
		public void visit(GPUType aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.BlockSizeType)
		 */
		@Override
		public void visit(BlockSizeType aBean) {
			setPower(0.0);			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ControllerType)
		 */
		@Override
		public void visit(ControllerType aBean) {
			setPower(0.0);			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.HitRatioType)
		 */
		@Override
		public void visit(HitRatioType aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.LogicalVolumeType)
		 */
		@Override
		public void visit(LogicalVolumeType aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NASType)
		 */
		@Override
		public void visit(NASType aBean) {
			setPower(0.0);			
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.RAIDDiskType)
		 */
		@Override
		public void visit(RAIDDiskType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.ActionRequestType)
		 */
		@Override
		public void visit(ActionRequestType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.ActionRequestType.ActionList)
		 */
		@Override
		public void visit(ActionList aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.LiveMigrateVMActionType)
		 */
		@Override
		public void visit(LiveMigrateVMActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.MoveVMActionType)
		 */
		@Override
		public void visit(MoveVMActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.PowerOffActionType)
		 */
		@Override
		public void visit(PowerOffActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.PowerOnActionType)
		 */
		@Override
		public void visit(PowerOnActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.StandByActionType)
		 */
		@Override
		public void visit(StandByActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.actions.StartJobActionType)
		 */
		@Override
		public void visit(StartJobActionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.AllocationRequestType)
		 */
		@Override
		public void visit(AllocationRequestType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.AllocationResponseType)
		 */
		@Override
		public void visit(AllocationResponseType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.CloudVmAllocationResponseType)
		 */
		@Override
		public void visit(CloudVmAllocationResponseType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.CloudVmAllocationType)
		 */
		@Override
		public void visit(CloudVmAllocationType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.HpcClusterAllocationResponseType)
		 */
		@Override
		public void visit(HpcClusterAllocationResponseType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.HpcClusterAllocationType)
		 */
		@Override
		public void visit(HpcClusterAllocationType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.TraditionalVmAllocationResponseType)
		 */
		@Override
		public void visit(TraditionalVmAllocationResponseType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.allocation.TraditionalVmAllocationType)
		 */
		@Override
		public void visit(TraditionalVmAllocationType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ActionResultCodeType)
		 */
		@Override
		public void visit(ActionResultCodeType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ActionResultDescriptionType)
		 */
		@Override
		public void visit(ActionResultDescriptionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.JobIDType)
		 */
		@Override
		public void visit(JobIDType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.MinutesType)
		 */
		@Override
		public void visit(MinutesType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.NodeType10)
		 */
		@Override
		public void visit(NodeType10 aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.OperatorType)
		 */
		@Override
		public void visit(OperatorType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ProcessorFrequencyType)
		 */
		@Override
		public void visit(ProcessorFrequencyType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.ReasonType)
		 */
		@Override
		public void visit(ReasonType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.RequestDateTimeType)
		 */
		@Override
		public void visit(RequestDateTimeType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.SecondsType)
		 */
		@Override
		public void visit(SecondsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.TimeperiodType)
		 */
		@Override
		public void visit(TimeperiodType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.VersionType)
		 */
		@Override
		public void visit(VersionType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.VirtualMachineDumpType)
		 */
		@Override
		public void visit(VirtualMachineDumpType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.commontypes.VirtualMachineIDType)
		 */
		@Override
		public void visit(VirtualMachineIDType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraintType)
		 */
		@Override
		public void visit(BoundedPlacementConstraintType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.BoundedPlacementConstraintType.PlacementConstraint)
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ConstraintType)
		 */
		@Override
		public void visit(ConstraintType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ConstraintType.PlacementConstraint)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.optimizerconstraints.ConstraintType.PlacementConstraint aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoadType)
		 */
		@Override
		public void visit(ExpectedLoadType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.LoadType)
		 */
		@Override
		public void visit(LoadType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.NodeControllerType)
		 */
		@Override
		public void visit(NodeControllerType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.PeriodType)
		 */
		@Override
		public void visit(PeriodType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.VMTypeType)
		 */
		@Override
		public void visit(VMTypeType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.optimizerconstraints.VMTypeType.VMType)
		 */
		@Override
		public void visit(VMType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.ConstraintType)
		 */
		@Override
		public void visit(
				f4g.schemas.java.constraints.placement.ConstraintType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.DCType)
		 */
		@Override
		public void visit(DCType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.FIT4GreenConstraintType)
		 */
		@Override
		public void visit(FIT4GreenConstraintType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.constraints.placement.TSType)
		 */
		@Override
		public void visit(TSType aBean) {
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
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.DCType)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.DCType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.LoadPatternType)
		 */
		@Override
		public void visit(LoadPatternType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.LoadPatternsType)
		 */
		@Override
		public void visit(LoadPatternsType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.LoadType)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.LoadType aBean) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see com.massfords.humantask.Visitor#visit(f4g.schemas.java.loadpatterns.PeriodType)
		 */
		@Override
		public void visit(f4g.schemas.java.loadpatterns.PeriodType aBean) {
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
		
		SiteType site = new SiteType();
		
		DatacenterType dataCenter1 = new DatacenterType();
		DatacenterType dataCenter2 = new DatacenterType();
		site.getDatacenter().add(dataCenter1);
		site.getDatacenter().add(dataCenter2);
	
		PowerCalculatorTraverser test = new PowerCalculatorTraverser();
		
		test.calculatePower(site);
	
	}
	
}
