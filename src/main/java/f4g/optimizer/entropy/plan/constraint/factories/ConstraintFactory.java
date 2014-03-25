package f4g.optimizer.entropy.plan.constraint.factories;

import org.apache.log4j.Logger;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import f4g.optimizer.entropy.NamingService;
import f4g.optimizer.entropy.configuration.F4GConfigurationAdapter;

public class ConstraintFactory {

	public Logger log;  
	protected NamingService<Node> nodeNames;
	protected NamingService<VM> vmNames;
	protected Mapping map;
    
	public ConstraintFactory(Model model) {
		this.nodeNames = (NamingService<Node>) model.getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.NODE_NAMING_SERVICE);
		this.vmNames = (NamingService<VM>) model.getView(NamingService.VIEW_ID_BASE + F4GConfigurationAdapter.VM_NAMING_SERVICE);
		this.map = model.getMapping();
		
	}

}