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
package org.f4g.test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.f4g.com.util.PowerData;

import org.f4g.schema.metamodel.*;


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
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ENTITIES)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.ENTITIES aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.ENTITY)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.ENTITY aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.IDREFS)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.IDREFS aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Language)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.Language aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NCName)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NCName aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NMTOKEN)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NMTOKEN aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NMTOKENS)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NMTOKENS aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.Name)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.Name aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NegativeInteger)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NegativeInteger aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NonNegativeInteger)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NonNegativeInteger aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.NonPositiveInteger)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.NonPositiveInteger aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.PositiveInteger)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.PositiveInteger aBean) {
			setPower(0.0);
			
		}

		/* (non-Javadoc)
		 * @see org.f4g.schema.metamodel.Visitor#visit(org.f4g.schema.metamodel.UnsignedLong)
		 */
		@Override
		public void visit(org.f4g.schema.metamodel.UnsignedLong aBean) {
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
