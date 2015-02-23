package f4g.commons.util;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;


public class Util {
	static Logger log = Logger.getLogger(Util.class.getName()); // 

	private static JAXBContext jc = null;

	public static JAXBContext getJaxbContext() {
		if (jc == null) {
			try {
				// Creates a JAXB context for the f4gschema package
				jc = JAXBContext.newInstance("f4g.schemas.java.metamodel");
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
		return jc;

	}
	
	/**
	 * finds a VM attributes based on its name.
	 * 
	 * @author cdupont
	 */
	public static VMFlavorType.VMFlavor findVMByName(final String VMName,
			VMFlavorType myVMFlavors) throws NoSuchElementException {

		if(VMName == null) 
			throw new NoSuchElementException();
		
		Predicate<VMFlavorType.VMFlavor> isOfName = new Predicate<VMFlavorType.VMFlavor>() {
			@Override
			public boolean apply(VMFlavorType.VMFlavor VM) {
				return VMName.equals(VM.getName());
			}
		};

		return Iterators.find(myVMFlavors.getVMFlavor().iterator(), isOfName);
	}



}
