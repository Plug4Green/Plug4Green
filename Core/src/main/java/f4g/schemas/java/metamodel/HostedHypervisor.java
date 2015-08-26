//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//


package f4g.schemas.java.metamodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import com.massfords.humantask.Named;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for HostedHypervisor complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HostedHypervisor"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="hypervisorName" type="{f4g/schemas/java/MetaModel}HostedHypervisorName"/&gt;
 *         &lt;element name="frameworkID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element ref="{f4g/schemas/java/MetaModel}VirtualMachine" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HostedHypervisor", propOrder = {
    "hypervisorName",
    "frameworkID",
    "virtualMachine"
})
public class HostedHypervisor implements Cloneable, Named, Visitable, CopyTo
{

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected HostedHypervisorName hypervisorName;
    @XmlElement(required = true)
    protected String frameworkID;
    @XmlElement(name = "VirtualMachine", namespace = "f4g/schemas/java/MetaModel")
    protected List<VirtualMachine> virtualMachine;
    @XmlTransient
    private QName jaxbElementName;

    /**
     * Default no-arg constructor
     * 
     */
    public HostedHypervisor() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public HostedHypervisor(final HostedHypervisorName hypervisorName, final String frameworkID, final List<VirtualMachine> virtualMachine, final QName jaxbElementName) {
        this.hypervisorName = hypervisorName;
        this.frameworkID = frameworkID;
        this.virtualMachine = virtualMachine;
        this.jaxbElementName = jaxbElementName;
    }

    /**
     * Gets the value of the hypervisorName property.
     * 
     * @return
     *     possible object is
     *     {@link HostedHypervisorName }
     *     
     */
    public HostedHypervisorName getHypervisorName() {
        return hypervisorName;
    }

    /**
     * Sets the value of the hypervisorName property.
     * 
     * @param value
     *     allowed object is
     *     {@link HostedHypervisorName }
     *     
     */
    public void setHypervisorName(HostedHypervisorName value) {
        this.hypervisorName = value;
    }

    /**
     * Gets the value of the frameworkID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFrameworkID() {
        return frameworkID;
    }

    /**
     * Sets the value of the frameworkID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrameworkID(String value) {
        this.frameworkID = value;
    }

    /**
     * Gets the value of the virtualMachine property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the virtualMachine property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVirtualMachine().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VirtualMachine }
     * 
     * 
     */
    public List<VirtualMachine> getVirtualMachine() {
        if (virtualMachine == null) {
            virtualMachine = new ArrayList<VirtualMachine>();
        }
        return this.virtualMachine;
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
        final Object draftCopy = ((target == null)?createNewInstance():target);
        if (draftCopy instanceof HostedHypervisor) {
            final HostedHypervisor copy = ((HostedHypervisor) draftCopy);
            if (this.hypervisorName!= null) {
                HostedHypervisorName sourceHypervisorName;
                sourceHypervisorName = this.getHypervisorName();
                HostedHypervisorName copyHypervisorName = ((HostedHypervisorName) strategy.copy(LocatorUtils.property(locator, "hypervisorName", sourceHypervisorName), sourceHypervisorName));
                copy.setHypervisorName(copyHypervisorName);
            } else {
                copy.hypervisorName = null;
            }
            if (this.frameworkID!= null) {
                String sourceFrameworkID;
                sourceFrameworkID = this.getFrameworkID();
                String copyFrameworkID = ((String) strategy.copy(LocatorUtils.property(locator, "frameworkID", sourceFrameworkID), sourceFrameworkID));
                copy.setFrameworkID(copyFrameworkID);
            } else {
                copy.frameworkID = null;
            }
            if ((this.virtualMachine!= null)&&(!this.virtualMachine.isEmpty())) {
                List<VirtualMachine> sourceVirtualMachine;
                sourceVirtualMachine = (((this.virtualMachine!= null)&&(!this.virtualMachine.isEmpty()))?this.getVirtualMachine():null);
                @SuppressWarnings("unchecked")
                List<VirtualMachine> copyVirtualMachine = ((List<VirtualMachine> ) strategy.copy(LocatorUtils.property(locator, "virtualMachine", sourceVirtualMachine), sourceVirtualMachine));
                copy.virtualMachine = null;
                if (copyVirtualMachine!= null) {
                    List<VirtualMachine> uniqueVirtualMachinel = copy.getVirtualMachine();
                    uniqueVirtualMachinel.addAll(copyVirtualMachine);
                }
            } else {
                copy.virtualMachine = null;
            }
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new HostedHypervisor();
    }

}