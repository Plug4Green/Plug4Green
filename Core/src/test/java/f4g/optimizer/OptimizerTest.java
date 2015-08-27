package f4g.optimizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import f4g.commons.com.util.PowerData;
import f4g.commons.controller.IController;
import f4g.optimizer.cloud.OptimizerEngineCloud;
import f4g.commons.optimizer.ICostEstimator;
import f4g.optimizer.utils.Utils;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.actions.*;
import f4g.schemas.java.allocation.AllocationRequest;
import f4g.schemas.java.allocation.CloudVmAllocation;
import f4g.schemas.java.allocation.ObjectFactory;
import f4g.schemas.java.constraints.optimizerconstraints.*;
import f4g.schemas.java.constraints.optimizerconstraints.ClusterType.Cluster;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.schemas.java.constraints.optimizerconstraints.SLAType.SLA;
import f4g.schemas.java.metamodel.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.jscience.physics.amount.*;
import org.jscience.economics.money.*;
import org.junit.Before;

import javax.measure.quantity.*;

import static javax.measure.unit.SI.*;

/**
 * {To be completed; use html notation, if necessary}
 * @author  cdupont
 */
public class OptimizerTest {

	public Logger log;  
	
	//this actionRequest is filled by the MockController after being called. 
	protected ActionRequest actionRequest = null;
	protected final Semaphore actionRequestAvailable = new Semaphore(10);
	
	/**
	 * @uml.property  name="optimizer"
	 * @uml.associationEnd  
	 */
	OptimizerEngineCloud optimizer = null;

	protected XMLGregorianCalendar begin;

	protected XMLGregorianCalendar end;
	
	ModelGenerator modelGenerator;
	
	//protected abstract OptimizerEngine getOptimizer();
	
	/**
	 * Mocked controller to be passed to the optimizer
	 *
	 * @author cdupont
	 */
	protected class MockController implements IController{

		@Override
		public boolean executeActionList(ActionRequest myActionRequest) {
			actionRequest = myActionRequest;
			actionRequestAvailable.release();
			for (JAXBElement<? extends AbstractBaseAction> action : myActionRequest.getActionList().getAction()){

				if (action.getValue() instanceof PowerOnAction) {
					PowerOnAction on = (PowerOnAction)action.getValue();
					log.debug("executeActionList: power ON on :" + on.getNodeName());
				}
				if (action.getValue() instanceof PowerOffAction) {
					PowerOffAction off = (PowerOffAction)action.getValue();
					log.debug("executeActionList: power OFF on :" + off.getNodeName());
				}
				if (action.getValue() instanceof MoveVMAction) {
					MoveVMAction move = (MoveVMAction)action.getValue();
					log.debug("executeActionList: move VM " + move.getVirtualMachine() + " from " + move.getSourceNodeController() + " to " + move.getDestNodeController());
				}
				if (action.getValue() instanceof LiveMigrateVMAction) {
					LiveMigrateVMAction move = (LiveMigrateVMAction)action.getValue();
					log.debug("executeActionList: live migrate VM " + move.getVirtualMachine() + " from " + move.getSourceNodeController() + " to " + move.getDestNodeController());
				}

			}
			log.debug("executeActionList: ComputedPowerBefore = " + myActionRequest.getComputedPowerBefore().getValue());
			log.debug("executeActionList: ComputedPowerAfter = " + myActionRequest.getComputedPowerAfter().getValue());
			
			return true;
		}

		@Override
		public boolean dispose() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.f4g.controller.IController#setActionsApproved(boolean)
		 */
		@Override
		public void setActionsApproved(boolean actionsApproved) {
			// TODO Auto-generated method stub
			
		}
		
		/* (non-Javadoc)
		 * @see org.f4g.controller.IController#setActionsApproved(boolean)
		 */
		@Override
		public void setApprovalSent(boolean actionsApproved) {
			// TODO Auto-generated method stub
			
		}	
				
	}
	
	/**
	 * Mocked power calculator to be passed to the optimizer
	 *
	 * @author cdupont
	 */
	protected class MockPowerCalculator implements IPowerCalculator{

		PowerCalculatorTraverser traverser = new PowerCalculatorTraverser();

		@Override public PowerData computePowerServer(Server server) {
			return traverser.calculatePower(server);
		}
		
		/*@Override public PowerData computePowerFIT4Green(FIT4Green model) {
			return traverser.calculatePower(model);
		}
		
		@Override public boolean dispose() {
			return true;
		}


		@Override public PowerData computePowerCPU(CPU cpu,
				OperatingSystemType operatingSystem) {
			return traverser.calculatePower(cpu);
		}


		@Override public PowerData computePowerDatacenter(Datacenter datacenter) {
			return traverser.calculatePower(datacenter);
		}

		@Override public PowerData computePowerFAN(Fan fan) {
			return traverser.calculatePower(fan);
		}

		
		@Override public PowerData computePowerHardDisk(HardDisk hardDisk) {
			return traverser.calculatePower(hardDisk);
		}

		@Override public PowerData computePowerMainboard(Mainboard mainboard,
				OperatingSystemType operatingSystem) {
			return traverser.calculatePower(mainboard);
		}

		@Override public PowerData computePowerMainboardRAMs(Mainboard mainboard) {
			return traverser.calculatePower(mainboard);
		}

		@Override public PowerData computePowerRAID(RAID raid) {
			return traverser.calculatePower(raid);
		}

		@Override public PowerData computePowerRack(Rack rack) {
			return traverser.calculatePower(rack);
		}


		@Override public PowerData computePowerSite(Site site) {
			return traverser.calculatePower(site);
		}

		@Override public PowerData computePowerSolidStateDisk(SolidStateDisk ssdisk) {
			return traverser.calculatePower(ssdisk);
		}

		@Override
		public PowerData computePowerCore(Core myCore, CPU cpu,
				OperatingSystemType operatingSystem) {
			return traverser.calculatePower(myCore);
		}

		@Override
		public PowerData computePowerSAN(Rack obj) {
			return traverser.calculatePower(obj);
		}

		@Override
		public PowerData computePowerIdleSAN(Rack obj) {
			return traverser.calculatePower(obj);
		}

		@Override
		public boolean getSimulationFlag() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setSimulationFlag(boolean f) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public PowerData computePowerIdleNAS(NAS obj) {
			return traverser.calculatePower(obj);
		}

		@Override
		public PowerData computePowerNAS(NAS obj) {
			return traverser.calculatePower(obj);
		}*/
	}
	
	/**
	 * Mocked cost estimator to be passed to the optimizer
	 *
	 * @author cdupont
	 */
	protected class MockNetworkCost implements ICostEstimator {


		@Override
		public boolean dispose() {
			return false;
		}

		@Override
		public Amount<Duration> moveDownTimeCost(NetworkNode fromServer,
				NetworkNode toServer, VirtualMachine VM,
				FIT4Green model) {
			return null;
		}

		@Override
		public Amount<Energy> moveEnergyCost(NetworkNode fromServer,
				NetworkNode toServer, VirtualMachine VM,
				FIT4Green model) {
			
			return Amount.valueOf(100, JOULE);
		}


		@Override
		public Amount<Money> moveFinancialCost(NetworkNode fromServer,
				NetworkNode toServer, VirtualMachine VM,
				FIT4Green model) {
			return null;
		}
		
	}
	
	/**
	 * Construction of the optimizer
	 *
	 * @author cdupont
	 */
	@Before
	protected void setUp() throws Exception {

		begin = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2010, 1, 1, 0);
		end = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2020, 1, 1, 0);
		
		Properties log4jProperties = new Properties();
		if(System.getProperty("log4j.configuration") != null){
			PropertyConfigurator.configure(System.getProperty("log4j.configuration"));				
		} else {
			InputStream isLog4j = this.getClass().getClassLoader().getResourceAsStream("optimizer/log4j.properties");
			log4jProperties.load(isLog4j);
			PropertyConfigurator.configure(log4jProperties);
			System.out.println("logger f4g:" + log4jProperties.getProperty("log4j.logger.org.f4g"));
		}
			
		log = Logger.getLogger(this.getClass().getName()); 
		modelGenerator = new ModelGenerator();
		
		actionRequestAvailable.acquire();	    
	}
		
	/**
	 * helper function
	 */
	protected AllocationRequest createAllocationRequestCloud(String VMFlavor) {
		
		AllocationRequest request = new AllocationRequest();
		
		CloudVmAllocation alloc = new CloudVmAllocation();
		alloc.setVm(VMFlavor);
		alloc.setImageId("");
		alloc.getClusterId().add("c1");
		
		
		//Simulates a CloudVmAllocation operation
		JAXBElement<CloudVmAllocation>  operationType = (new ObjectFactory()).createCloudVmAllocation(alloc);
	
		request.setRequest(operationType);
		
		return request;
	}

	
	protected ClusterType createDefaultCluster(int NumberOfNodes, List<SLA> sla, List<Policy> policy) {
	
		List<String> nodeName = new ArrayList<String>();
		for(int i=1; i<=NumberOfNodes; i++){
			nodeName.add("id" + i*100000 );	
		}		
		List<Cluster> cluster = new ArrayList<ClusterType.Cluster>();

		BoundedSLAsType bSlas = null;
		if(sla != null) {
			bSlas = new BoundedSLAsType();
			bSlas.getSLA().add(new BoundedSLAsType.SLA(sla.get(0)));	
		} 		
		
		BoundedPoliciesType bPolicies = null;
		
		cluster.add(new Cluster("c1", new NodeController(nodeName) , bSlas, bPolicies, "id"));
		return new ClusterType(cluster);
	}
	

	protected FederationType makeSimpleFed(PolicyType policies, FIT4Green f4g) {
				
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(policies.getPolicy().get(0));
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);
		
		if(f4g != null) {
			//add all servers in one cluster
	
			BoundedClustersType bcls = new BoundedClustersType();
			ClusterType.Cluster c = new ClusterType.Cluster();
			c.setNodeController(new NodeController());
			c.setName("c1");
			for(Server s : Utils.getAllServers(f4g)) {
			    c.getNodeController().getNodeName().add(s.getFrameworkID());
			}
			BoundedClustersType.Cluster bcl = new BoundedClustersType.Cluster();
			bcl.setIdref(c);
			bcls.getCluster().add(bcl);
			fed.setBoundedCluster(bcls);
		}	
		
		return fed;

	}
	
	List <MoveVMAction> getMoves() {
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ActionRequest.ActionList response = actionRequest.getActionList();
		
		List <MoveVMAction> moves = new ArrayList<MoveVMAction>();
			
		for (JAXBElement<? extends AbstractBaseAction> action : response.getAction()){
			if (action.getValue() instanceof MoveVMAction) 
				moves.add((MoveVMAction)action.getValue());
		}
		return moves;
	}
	
	List <LiveMigrateVMAction> getLiveMigrate() {
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ActionRequest.ActionList response = actionRequest.getActionList();
		
		List <LiveMigrateVMAction> moves = new ArrayList<LiveMigrateVMAction>();
			
		for (JAXBElement<? extends AbstractBaseAction> action : response.getAction()){
			if (action.getValue() instanceof LiveMigrateVMAction) 
				moves.add((LiveMigrateVMAction)action.getValue());
		}
		return moves;
	}
	
	List <PowerOffAction> getPowerOffs() {
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ActionRequest.ActionList response = actionRequest.getActionList();
		
		List <PowerOffAction> powerOffs = new ArrayList<PowerOffAction>();
		
		for (JAXBElement<? extends AbstractBaseAction> action : response.getAction()){
			if (action.getValue() instanceof PowerOffAction) 
				powerOffs.add((PowerOffAction)action.getValue());
		}
		return powerOffs;
	}
	
	List <PowerOnAction> getPowerOns() {
		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ActionRequest.ActionList response = actionRequest.getActionList();
		
		List <PowerOnAction> powerOns = new ArrayList<PowerOnAction>();
		
		for (JAXBElement<? extends AbstractBaseAction> action : response.getAction()){
			if (action.getValue() instanceof PowerOnAction) 
				powerOns.add((PowerOnAction)action.getValue());
		}
		return powerOns;
	}
	
}
