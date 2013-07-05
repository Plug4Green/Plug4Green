package org.f4g.com.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Container for a set of ComOperation objects
 * @author FIT4Green
 *
 */
public class ComOperationCollector {

	ArrayList operations = null;
	
	public ComOperationCollector() {
		operations = new ArrayList();
	}
	
	public ComOperationCollector(Collection c) {
		this.operations = new ArrayList(c);
	}
	
	public boolean add(ComOperation operation){
		return operations.add(operation);
	}
	
	public boolean remove(ComOperation operation){
		return operations.remove(operation);
	}
	
	public ArrayList getOperations() {
		return operations;
	}

}
