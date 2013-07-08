/**
 * ============================== Header ============================== 
 * file:          SLAReader.java
 * project:       FIT4Green/Optimizer
 * created:       06 Dec. 2011 by ts
 * last modified: 
 * revision:      
 * 
 * short description:
 *   Reads and validate the Placement Constraint instance file.
 * ============================= /Header ==============================
 */
package f4g.optimizer.CloudTraditional;

import java.io.FileReader;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import f4g.schemas.java.constraints.placement.Ban;
import f4g.schemas.java.constraints.placement.Capacity;
import f4g.schemas.java.constraints.placement.ConstraintType;
import f4g.schemas.java.constraints.placement.FIT4GreenConstraintType;
import f4g.schemas.java.constraints.placement.Fence;
import f4g.schemas.java.constraints.placement.Gather;
import f4g.schemas.java.constraints.placement.Lonely;
import f4g.schemas.java.constraints.placement.OneOf;
import f4g.schemas.java.constraints.placement.Root;
import f4g.schemas.java.constraints.placement.Split;
import f4g.schemas.java.constraints.placement.Spread;

/**
 * {To be completed; use html notation, if necessary}
 * 
 * 
 * @author Ts
 */
public class ConstraintReader {

	public Logger log;
	public ConstraintType CP;

	/**
	 * get Placement constraints from file input TODO: To be set.
	 * 
	 * @param CPPathName
	 *            the path, where the constraint file can be found at.
	 * 
	 * @author Ts
	 */
	public ConstraintReader(String CPPathName) {
		log = Logger.getLogger(ConstraintReader.class.getName());
		CP = readCP(CPPathName);
		if (CP == null)
			log.error("No Placement constraint file found or wrong instance");
	}

	/**
	 * loads a Constraint file
	 * 
	 * @author Ts
	 */
	private ConstraintType readCP(String CPPathName) {
		
		InputStream cpStream = this.getClass().getClassLoader()
		.getResourceAsStream(CPPathName);

		try {
			JAXBContext context = JAXBContext
					.newInstance("org.f4g.schema.constraints.placement");
			Unmarshaller um = context.createUnmarshaller();
			@SuppressWarnings("unchecked")
			JAXBElement<FIT4GreenConstraintType> s = (JAXBElement<FIT4GreenConstraintType>) um
					.unmarshal(cpStream);	
					//.unmarshal(new FileReader(CPPathName));
			return s.getValue().getDataCentre().getTargetSys().getConstraint();
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	public List<Ban> getAllBan() {
		return CP.getBan();
	}

	public List<Capacity> getAllCapacity() {
		return CP.getCapacity();
	}

	public List<Fence> getAllFence() {
		return CP.getFence();
	}

	public List<Gather> getAllGather() {
		return CP.getGather();
	}

	public List<Lonely> getAllLonely() {
		return CP.getLonely();
	}

	public List<OneOf> getAllOneOf() {
		return CP.getOneOf();
	}

	public List<Root> getAllRoot() {
		return CP.getRoot();
	}

	public List<Split> getAllSplit() {
		return CP.getSplit();
	}

	public List<Spread> getAllSpread() {
		return CP.getSpread();
	}

}
