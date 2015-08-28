//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//

package f4g.schemas.java.actions;

import java.util.ArrayList;
import java.util.List;

import javax.measure.quantity.Power;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;

import javassist.compiler.ast.Visitor;

/**
 * <p>
 * Java class for ActionRequest complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ActionRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Datetime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *         &lt;element name="IsAutomatic" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="OperatorName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ComputedPowerBefore" type="{f4g/schemas/java/MetaModel}Power" minOccurs="0"/&gt;
 *         &lt;element name="ComputedPowerAfter" type="{f4g/schemas/java/MetaModel}Power" minOccurs="0"/&gt;
 *         &lt;element name="ActionList"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element ref="{f4g/schemas/java/Actions}Action" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionRequest", propOrder = { "datetime", "isAutomatic", "operatorName", "computedPowerBefore",
	"computedPowerAfter", "actionList" })
public class ActionRequest implements Cloneable, Named, Visitable, CopyTo {

    @XmlElement(name = "Datetime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar datetime;
    @XmlElement(name = "IsAutomatic")
    protected Boolean isAutomatic;
    @XmlElement(name = "OperatorName")
    protected String operatorName;
    @XmlElement(name = "ComputedPowerBefore")
    @XmlSchemaType(name = "double")
    protected Power computedPowerBefore;
    @XmlElement(name = "ComputedPowerAfter")
    @XmlSchemaType(name = "double")
    protected Power computedPowerAfter;
    @XmlElement(name = "ActionList", required = true)
    protected ActionRequest.ActionList actionList;
    @XmlTransient
    private QName jaxbElementName;

    /**
     * Default no-arg constructor
     * 
     */
    public ActionRequest() {
	super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public ActionRequest(final XMLGregorianCalendar datetime, final Boolean isAutomatic, final String operatorName,
	    final Power computedPowerBefore, final Power computedPowerAfter, final ActionRequest.ActionList actionList,
	    final QName jaxbElementName) {
	this.datetime = datetime;
	this.isAutomatic = isAutomatic;
	this.operatorName = operatorName;
	this.computedPowerBefore = computedPowerBefore;
	this.computedPowerAfter = computedPowerAfter;
	this.actionList = actionList;
	this.jaxbElementName = jaxbElementName;
    }

    /**
     * Gets the value of the datetime property.
     * 
     * @return possible object is {@link XMLGregorianCalendar }
     * 
     */
    public XMLGregorianCalendar getDatetime() {
	return datetime;
    }

    /**
     * Sets the value of the datetime property.
     * 
     * @param value
     *            allowed object is {@link XMLGregorianCalendar }
     * 
     */
    public void setDatetime(XMLGregorianCalendar value) {
	this.datetime = value;
    }

    /**
     * Gets the value of the isAutomatic property.
     * 
     * @return possible object is {@link Boolean }
     * 
     */
    public Boolean isIsAutomatic() {
	return isAutomatic;
    }

    /**
     * Sets the value of the isAutomatic property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setIsAutomatic(Boolean value) {
	this.isAutomatic = value;
    }

    /**
     * Gets the value of the operatorName property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getOperatorName() {
	return operatorName;
    }

    /**
     * Sets the value of the operatorName property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setOperatorName(String value) {
	this.operatorName = value;
    }

    /**
     * Gets the value of the computedPowerBefore property.
     * 
     * @return possible object is {@link Power }
     * 
     */
    public Power getComputedPowerBefore() {
	return computedPowerBefore;
    }

    /**
     * Sets the value of the computedPowerBefore property.
     * 
     * @param value
     *            allowed object is {@link Power }
     * 
     */
    public void setComputedPowerBefore(Power value) {
	this.computedPowerBefore = value;
    }

    /**
     * Gets the value of the computedPowerAfter property.
     * 
     * @return possible object is {@link Power }
     * 
     */
    public Power getComputedPowerAfter() {
	return computedPowerAfter;
    }

    /**
     * Sets the value of the computedPowerAfter property.
     * 
     * @param value
     *            allowed object is {@link Power }
     * 
     */
    public void setComputedPowerAfter(Power value) {
	this.computedPowerAfter = value;
    }

    /**
     * Gets the value of the actionList property.
     * 
     * @return possible object is {@link ActionRequest.ActionList }
     * 
     */
    public ActionRequest.ActionList getActionList() {
	return actionList;
    }

    /**
     * Sets the value of the actionList property.
     * 
     * @param value
     *            allowed object is {@link ActionRequest.ActionList }
     * 
     */
    public void setActionList(ActionRequest.ActionList value) {
	this.actionList = value;
    }

    public void setJAXBElementName(QName name) {
	this.jaxbElementName = name;
    }

    public QName getJAXBElementName() {
	return this.jaxbElementName;
    }

    public void afterUnmarshal(Unmarshaller u, Object parent) {
	if (parent instanceof JAXBElement) {
	    this.jaxbElementName = ((JAXBElement) parent).getName();
	}
    }

    public void accept(Visitor aVisitor) {
	aVisitor.visit(this);
    }

    public Object clone() {
	return copyTo(createNewInstance());
    }

    public Object copyTo(Object target) {
	final CopyStrategy strategy = JAXBCopyStrategy.INSTANCE;
	return copyTo(null, target, strategy);
    }

    public Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy) {
	final Object draftCopy = ((target == null) ? createNewInstance() : target);
	if (draftCopy instanceof ActionRequest) {
	    final ActionRequest copy = ((ActionRequest) draftCopy);
	    if (this.datetime != null) {
		XMLGregorianCalendar sourceDatetime;
		sourceDatetime = this.getDatetime();
		XMLGregorianCalendar copyDatetime = ((XMLGregorianCalendar) strategy
			.copy(LocatorUtils.property(locator, "datetime", sourceDatetime), sourceDatetime));
		copy.setDatetime(copyDatetime);
	    } else {
		copy.datetime = null;
	    }
	    if (this.isAutomatic != null) {
		Boolean sourceIsAutomatic;
		sourceIsAutomatic = this.isIsAutomatic();
		Boolean copyIsAutomatic = ((Boolean) strategy
			.copy(LocatorUtils.property(locator, "isAutomatic", sourceIsAutomatic), sourceIsAutomatic));
		copy.setIsAutomatic(copyIsAutomatic);
	    } else {
		copy.isAutomatic = null;
	    }
	    if (this.operatorName != null) {
		String sourceOperatorName;
		sourceOperatorName = this.getOperatorName();
		String copyOperatorName = ((String) strategy
			.copy(LocatorUtils.property(locator, "operatorName", sourceOperatorName), sourceOperatorName));
		copy.setOperatorName(copyOperatorName);
	    } else {
		copy.operatorName = null;
	    }
	    if (this.computedPowerBefore != null) {
		Power sourceComputedPowerBefore;
		sourceComputedPowerBefore = this.getComputedPowerBefore();
		Power copyComputedPowerBefore = ((Power) strategy.copy(
			LocatorUtils.property(locator, "computedPowerBefore", sourceComputedPowerBefore),
			sourceComputedPowerBefore));
		copy.setComputedPowerBefore(copyComputedPowerBefore);
	    } else {
		copy.computedPowerBefore = null;
	    }
	    if (this.computedPowerAfter != null) {
		Power sourceComputedPowerAfter;
		sourceComputedPowerAfter = this.getComputedPowerAfter();
		Power copyComputedPowerAfter = ((Power) strategy.copy(
			LocatorUtils.property(locator, "computedPowerAfter", sourceComputedPowerAfter),
			sourceComputedPowerAfter));
		copy.setComputedPowerAfter(copyComputedPowerAfter);
	    } else {
		copy.computedPowerAfter = null;
	    }
	    if (this.actionList != null) {
		ActionRequest.ActionList sourceActionList;
		sourceActionList = this.getActionList();
		ActionRequest.ActionList copyActionList = ((ActionRequest.ActionList) strategy
			.copy(LocatorUtils.property(locator, "actionList", sourceActionList), sourceActionList));
		copy.setActionList(copyActionList);
	    } else {
		copy.actionList = null;
	    }
	}
	return draftCopy;
    }

    public Object createNewInstance() {
	return new ActionRequest();
    }

    /**
     * <p>
     * Java class for anonymous complex type.
     * 
     * <p>
     * The following schema fragment specifies the expected content contained
     * within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element ref="{f4g/schemas/java/Actions}Action" maxOccurs="unbounded" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = { "action" })
    public static class ActionList implements Cloneable, Visitable, CopyTo {

	@XmlElementRef(name = "Action", namespace = "f4g/schemas/java/Actions", type = JAXBElement.class, required = false)
	protected List<JAXBElement<? extends AbstractBaseAction>> action;

	/**
	 * Default no-arg constructor
	 * 
	 */
	public ActionList() {
	    super();
	}

	/**
	 * Fully-initialising value constructor
	 * 
	 */
	public ActionList(final List<JAXBElement<? extends AbstractBaseAction>> action) {
	    this.action = action;
	}

	/**
	 * Gets the value of the action property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list
	 * will be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the action property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getAction().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link JAXBElement }{@code <}{@link StandByAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link PowerOffAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link MoveVMAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link LiveMigrateVMAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link StartJobAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link AbstractBaseAction }{@code >}
	 * {@link JAXBElement }{@code <}{@link PowerOnAction }{@code >}
	 * 
	 * 
	 */
	public List<JAXBElement<? extends AbstractBaseAction>> getAction() {
	    if (action == null) {
		action = new ArrayList<JAXBElement<? extends AbstractBaseAction>>();
	    }
	    return this.action;
	}

	public void accept(Visitor aVisitor) {
	    aVisitor.visit(this);
	}

	public Object clone() {
	    return copyTo(createNewInstance());
	}

	public Object copyTo(Object target) {
	    final CopyStrategy strategy = JAXBCopyStrategy.INSTANCE;
	    return copyTo(null, target, strategy);
	}

	public Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy) {
	    final Object draftCopy = ((target == null) ? createNewInstance() : target);
	    if (draftCopy instanceof ActionRequest.ActionList) {
		final ActionRequest.ActionList copy = ((ActionRequest.ActionList) draftCopy);
		if ((this.action != null) && (!this.action.isEmpty())) {
		    List<JAXBElement<? extends AbstractBaseAction>> sourceAction;
		    sourceAction = (((this.action != null) && (!this.action.isEmpty())) ? this.getAction() : null);
		    @SuppressWarnings("unchecked")
		    List<JAXBElement<? extends AbstractBaseAction>> copyAction = ((List<JAXBElement<? extends AbstractBaseAction>>) strategy
			    .copy(LocatorUtils.property(locator, "action", sourceAction), sourceAction));
		    copy.action = null;
		    if (copyAction != null) {
			List<JAXBElement<? extends AbstractBaseAction>> uniqueActionl = copy.getAction();
			uniqueActionl.addAll(copyAction);
		    }
		} else {
		    copy.action = null;
		}
	    }
	    return draftCopy;
	}

	public Object createNewInstance() {
	    return new ActionRequest.ActionList();
	}

    }

}
