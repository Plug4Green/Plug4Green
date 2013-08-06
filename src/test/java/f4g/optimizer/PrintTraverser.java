/**
* ============================== Header ============================== 
* file:          testPrintTraverser.java
* project:       FIT4Green/Optimizer
* created:       20 déc. 2010 by cdupont
* last modified: $LastChangedDate: 2011-09-19 13:16:40 +0200 (lun, 19 sep 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 786 $
* 
* short description:
*   This is an example of usage of JAXB-Visitor.
*   It traverse the F4G tree and displays each nodes.
* ============================= /Header ==============================
*/
package f4g.optimizer;


import f4g.schemas.java.metamodel.*;


public class PrintTraverser {
	
	//prints any F4G type tree
	public void print(Visitable aVisitable) {
		
		Visitor printer = new BaseVisitor();
		TraversingVisitorProgressMonitor monitor = new PrintMonitor();
		TraversingVisitor tv = new TraversingVisitor( new DepthFirstTraverserImpl(), printer );

		tv.setProgressMonitor(monitor);
		aVisitable.accept(tv);
	}
	
	public class PrintMonitor implements TraversingVisitorProgressMonitor {
		
		@Override
		public void traversed(Visitable aVisitable) {
			
		}
		
		@Override
		public void visited(Visitable aVisitable) {
			System.out.println(aVisitable.getClass().getName());
			
		}
		
	}
	
	public static void main(String[] args) {
		
		SiteType site = new SiteType();
		
		DatacenterType dataCenter1 = new DatacenterType();
		DatacenterType dataCenter2 = new DatacenterType();
		site.getDatacenter().add(dataCenter1);
		site.getDatacenter().add(dataCenter2);
	
		PrintTraverser test = new PrintTraverser();
		test.print(site);
	
	}
	
}
