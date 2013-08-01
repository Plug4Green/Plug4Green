package f4g.optimizer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import f4g.schemas.java.constraints.optimizerconstraints.VMTypeType;
import f4g.optimizer.cost_estimator.NetworkCost;

import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.metamodel.NetworkNodeType;
import f4g.optimizer.utils.Utils;
import f4g.schemas.java.metamodel.PowerType;
import f4g.schemas.java.metamodel.MemoryUsageType;

import f4g.schemas.java.metamodel.CpuUsageType;
import f4g.schemas.java.metamodel.IoRateType;
import f4g.schemas.java.metamodel.NetworkUsageType;
import f4g.schemas.java.metamodel.NrOfCpusType;
import f4g.schemas.java.metamodel.RAMSizeType;
import f4g.schemas.java.metamodel.StorageCapacityType;
import f4g.schemas.java.actions.AbstractBaseActionType;
import f4g.schemas.java.actions.MoveVMActionType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.constraints.optimizerconstraints.BoundedPoliciesType;
import f4g.schemas.java.constraints.optimizerconstraints.CapacityType;
import f4g.schemas.java.constraints.optimizerconstraints.ExpectedLoadType;
import f4g.schemas.java.constraints.optimizerconstraints.FederationType;
import f4g.schemas.java.constraints.optimizerconstraints.LoadType;
import f4g.schemas.java.constraints.optimizerconstraints.PeriodType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType;
import f4g.schemas.java.constraints.optimizerconstraints.PolicyType.Policy;
import f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;


public class OptimizerNetworkTest extends OptimizerTest {
    
//	private Logger log; 
    FIT4GreenType model;
    List<ServerType> allServers;
    List<NetworkNodeType> allSwitches;
        
        
	protected void setUp() throws Exception {
		super.setUp();

		SLAGenerator slaGenerator = new SLAGenerator();
		
		PeriodType period = new PeriodType(begin, end, null, null, new LoadType(null, null));
		
		PolicyType.Policy pol = new Policy();
		pol.getPeriodVMThreshold().add(period);

		List<Policy> polL = new LinkedList<Policy>();
		polL.add(pol);

		PolicyType policies = new PolicyType(polL);
		policies.getPolicy().add(pol);
		
		FederationType fed = new FederationType();
		BoundedPoliciesType.Policy bpol = new BoundedPoliciesType.Policy(pol);
		BoundedPoliciesType bpols = new BoundedPoliciesType();
		bpols.getPolicy().add(bpol);		
		fed.setBoundedPolicies(bpols);
		
		optimizer = new OptimizerEngineCloudTraditional(new MockController(), new MockPowerCalculator(), new NetworkCost(), 
				        slaGenerator.createVirtualMachineType(), policies, fed);

    }
    
	protected void tearDown() throws Exception {
		super.tearDown();
		optimizer = null;
	}
        
     

        
    public void checkNetworkConnections() {
    
        // verify who is connected to switch0 
        assertEquals(2, allSwitches.get(0).getNetworkPort().size());
        String n0 = (String) allSwitches.get(0).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch0's port 0
        String n1 = (String) allSwitches.get(0).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch0's port 1
        assertEquals("id2000000", n0);        // should be switch1
        assertEquals("id3000000", n1);        // should be switch2
        
        // verify who is connected to switch1
        assertEquals(5, allSwitches.get(1).getNetworkPort().size());
        String n2 = (String) allSwitches.get(1).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch1's port 0
        String s0 = (String) allSwitches.get(1).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch1's port 1
        String s1 = (String) allSwitches.get(1).getNetworkPort().get(2).getNetworkPortRef();    // node connected to switch1's port 2
        String s2 = (String) allSwitches.get(1).getNetworkPort().get(3).getNetworkPortRef();    // node connected to switch1's port 2
        String s3 = (String) allSwitches.get(1).getNetworkPort().get(4).getNetworkPortRef();    // node connected to switch1's port 2
        assertEquals("id1000000", n2);               // should be switch0
        assertEquals("id100000", s0);               // should be server0
        assertEquals("id200000", s1);          // should be server1
        assertEquals("id300000", s2);          // should be server2
        assertEquals("id400000", s3);          // should be server3
        
    
        // verify who is connected to switch2
        assertEquals(5, allSwitches.get(2).getNetworkPort().size());
        String n3 = (String) allSwitches.get(2).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch2's port 0
        String s4 = (String) allSwitches.get(2).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch2's port 1
        String s5 = (String) allSwitches.get(2).getNetworkPort().get(2).getNetworkPortRef();    // node connected to switch2's port 2
        String s6 = (String) allSwitches.get(2).getNetworkPort().get(3).getNetworkPortRef();    // node connected to switch2's port 2
        String s7 = (String) allSwitches.get(2).getNetworkPort().get(4).getNetworkPortRef();    // node connected to switch2's port 2
        assertEquals("id1000000", n3);             // should be connected to switch0
        assertEquals("id500000", s4);        // should be connected to server2
        assertEquals("id600000", s5);        // should be connected to server3
        assertEquals("id700000", s6);        // should be connected to server2
        assertEquals("id800000", s7);        // should be connected to server3
         
    }


    
    
	
	/**
	 * Test global optimization with router/switch turning on/off
     * Based on OptimizerGlobalTest/testGlobalConstraintOnCPUUsage
	 * @author rlent
	 */
     
	public void testGlobalConstraintOnCPUUsageWithNetwork() {

		//generate one VM per server
		//VMs ressource usage is 0
		ModelGenerator modelGenerator = new ModelGenerator();

		modelGenerator.setNB_SERVERS(8); //8
		modelGenerator.setNB_VIRTUAL_MACHINES(1);
		
		//servers settings
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(1);

		modelGenerator.setVM_TYPE("CPU_constraint");
		
		VMTypeType.VMType type1 = new VMTypeType.VMType();
		type1.setName("CPU_constraint");
		type1.setCapacity(new CapacityType(new NrOfCpusType(1), new RAMSizeType(12), new StorageCapacityType(1)));
		type1.setExpectedLoad(new ExpectedLoadType(new CpuUsageType(50), new MemoryUsageType(0), new IoRateType(0), new NetworkUsageType(0)));
				
		optimizer.getVmTypes().getVMType().add(type1);
		

        // Network settings
        modelGenerator.setNB_SWITCHES(3);
        modelGenerator.setNB_ROUTERS(0);
        ModelGenerator.defaultSwitchPowerIdle = new PowerType( 100.0 );
        ModelGenerator.defaultSwitchPowerMax = new PowerType( 100.0 );


        // Populate model
		FIT4GreenType modelManyServersNoLoad = modelGenerator.createPopulatedFIT4GreenType();
        
        allServers = Utils.getAllServers(modelManyServersNoLoad);
        allSwitches = Utils.getAllNetworkDeviceNodes(modelManyServersNoLoad.getSite().get(0));


        // Create Topology:
        //            server0 ---\
        //               ...       switch1 ---\
        //            server3 ---/             \
        //                                       switch0
        //            server4 ---\             /
        //               ...       switch2 ---/
        //            server7 ---/             

        ModelGenerator.connectNetDevsFullDuplex(allSwitches.get(0), allSwitches.get(1));
        ModelGenerator.connectNetDevsFullDuplex(allSwitches.get(0), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(0), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(1), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(2), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(3), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(4), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(5), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(6), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(7), allSwitches.get(2));
    
        checkNetworkConnections();
		
		optimizer.runGlobalOptimization(modelManyServersNoLoad);

		try {
			actionRequestAvailable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		List <MoveVMActionType> moves = new ArrayList<MoveVMActionType>();
		List <PowerOffActionType> powerOffs = new ArrayList<PowerOffActionType>();


		if(actionRequest!=null) {
			for (JAXBElement<? extends AbstractBaseActionType> action : actionRequest.getActionList().getAction()){
				if (action.getValue() instanceof MoveVMActionType) 
					moves.add((MoveVMActionType)action.getValue());
				if (action.getValue() instanceof PowerOffActionType) {
                    PowerOffActionType a = (PowerOffActionType)action.getValue();
                    System.out.println(">>>>>" + a.getNodeName() );
					powerOffs.add(a);
               }
			}	
		}
		
        System.out.println("Moves: " + moves.size());
        System.out.println("Poweroffs: " + powerOffs.size());
		
	}
	    
    
}


