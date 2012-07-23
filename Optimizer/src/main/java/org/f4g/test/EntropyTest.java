/**
* ============================== Header ============================== 
* file:          OptimizerTest.java
* project:       FIT4Green/Optimizer
* created:       10 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2012-05-01 00:59:19 +0200 (mar, 01 may 2012) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 1406 $
* 
* short description:
*   Optimizer mother class for tests
* ============================= /Header ==============================
*/
package org.f4g.test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.ResourcePicker;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.SimpleNode;
import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import entropy.configuration.VirtualMachineComparator;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Offline;
import entropy.vjob.VJob;
import junit.framework.TestCase;



/**
 * {To be completed; use html notation, if necessary}
 * @author  cdupont
 */
public class EntropyTest extends TestCase {

	public Logger log;  
	
	/**
	 * Construction of the optimizer
	 *
	 * @author cdupont
	 */
	protected void setUp() throws Exception {
		super.setUp();

		log = Logger.getLogger(this.getClass().getName()); 
		
	}



	/**
	 * Destruction
	 * 
	 * @author cdupont
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		
	}

	
	  public void testOfflineOnOfflineNodes() {
	        ManagedElementSet<Node> ns1 = new SimpleManagedElementSet<Node>();
	        ManagedElementSet<Node> ns2 = new SimpleManagedElementSet<Node>();
	        Configuration src = new SimpleConfiguration();
	        for (int i = 0; i < 4; i++) {
	            Node n = new SimpleNode("N" + i, 8, 800, 24576);
	            ns1.add(n);
	            src.addOffline(n);
	        }
	        for (int i = 0; i < 4; i++) {
	        	Node n = new SimpleNode("N" + i, 8, 800, 24576);
	            ns2.add(n);
	            src.addOffline(n);
	        }
	        Offline c1 = new Offline(ns1);
	        VJob v = new DefaultVJob("V1");
	        v.addConstraint(c1);
	        
	        Offline c2 = new Offline(ns1);
	        v.addConstraint(c2);

	        List<VJob> vjobs = new ArrayList<VJob>();
	        vjobs.add(v);
	        try {

	            ChocoCustomRP planner = new ChocoCustomRP(new MockDurationEvaluator(2, 5, 1, 1, 7, 14, 7, 2, 4));
	            planner.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsFastBP());
	            planner.setRepairMode(false);


	            TimedReconfigurationPlan p = planner.compute(src,
	                    src.getAllVirtualMachines(),
	                    new SimpleManagedElementSet<VirtualMachine>(),
	                    src.getSleepings(),
	                    new SimpleManagedElementSet<VirtualMachine>(),
	                    new SimpleManagedElementSet<Node>(),
	                    new SimpleManagedElementSet<Node>(),
	                    vjobs);
	            assertTrue(p.getActions().isEmpty());
	            assertEquals(p.getDuration(), 0);
	        } catch (Exception e) {
	            fail(e.getMessage());
	        }

	    }
	
	  public void testContains() {
		  ManagedElementSet<VirtualMachine> s1 = new SimpleManagedElementSet<VirtualMachine>();
		  VirtualMachine vm = new SimpleVirtualMachine("i-c2b4711d", 1, 40, 1024);
		  s1.add(vm);
		  assertTrue(s1.contains(vm));
		  ManagedElementSet<VirtualMachine> s2 = s1.clone();
		  Collections.sort(s2, new VirtualMachineComparator(false, ResourcePicker.VMRc.nbOfCPUs));
		  assertTrue(s1.contains(vm));
		  
	  }
	  
}
