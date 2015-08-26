//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//


package f4g.schemas.java.constraints.optimizerconstraints;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;
import f4g.schemas.java.metamodel.NrOfCpus;
import f4g.schemas.java.metamodel.RAMSize;
import f4g.schemas.java.metamodel.StorageCapacity;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for capacityType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="capacityType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="vCpus" type="{f4g/schemas/java/MetaModel}NrOfCpus"/&gt;
 *         &lt;element name="vRam" type="{f4g/schemas/java/MetaModel}RAMSize"/&gt;
 *         &lt;element name="vHardDisk" type="{f4g/schemas/java/MetaModel}StorageCapacity"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "capacityType", propOrder = {
    "vCpus",
    "vRam",
    "vHardDisk"
})
public class CapacityType implements Cloneable, Visitable, CopyTo
{

    @XmlElement(required = true)
    @XmlSchemaType(name = "int")
    protected NrOfCpus vCpus;
    @XmlElement(required = true)
    @XmlSchemaType(name = "double")
    protected RAMSize vRam;
    @XmlElement(required = true)
    @XmlSchemaType(name = "double")
    protected StorageCapacity vHardDisk;

    /**
     * Default no-arg constructor
     * 
     */
    public CapacityType() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public CapacityType(final NrOfCpus vCpus, final RAMSize vRam, final StorageCapacity vHardDisk) {
        this.vCpus = vCpus;
        this.vRam = vRam;
        this.vHardDisk = vHardDisk;
    }

    /**
     * Gets the value of the vCpus property.
     * 
     * @return
     *     possible object is
     *     {@link NrOfCpus }
     *     
     */
    public NrOfCpus getVCpus() {
        return vCpus;
    }

    /**
     * Sets the value of the vCpus property.
     * 
     * @param value
     *     allowed object is
     *     {@link NrOfCpus }
     *     
     */
    public void setVCpus(NrOfCpus value) {
        this.vCpus = value;
    }

    /**
     * Gets the value of the vRam property.
     * 
     * @return
     *     possible object is
     *     {@link RAMSize }
     *     
     */
    public RAMSize getVRam() {
        return vRam;
    }

    /**
     * Sets the value of the vRam property.
     * 
     * @param value
     *     allowed object is
     *     {@link RAMSize }
     *     
     */
    public void setVRam(RAMSize value) {
        this.vRam = value;
    }

    /**
     * Gets the value of the vHardDisk property.
     * 
     * @return
     *     possible object is
     *     {@link StorageCapacity }
     *     
     */
    public StorageCapacity getVHardDisk() {
        return vHardDisk;
    }

    /**
     * Sets the value of the vHardDisk property.
     * 
     * @param value
     *     allowed object is
     *     {@link StorageCapacity }
     *     
     */
    public void setVHardDisk(StorageCapacity value) {
        this.vHardDisk = value;
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
        if (draftCopy instanceof CapacityType) {
            final CapacityType copy = ((CapacityType) draftCopy);
            if (this.vCpus!= null) {
                NrOfCpus sourceVCpus;
                sourceVCpus = this.getVCpus();
                NrOfCpus copyVCpus = ((NrOfCpus) strategy.copy(LocatorUtils.property(locator, "vCpus", sourceVCpus), sourceVCpus));
                copy.setVCpus(copyVCpus);
            } else {
                copy.vCpus = null;
            }
            if (this.vRam!= null) {
                RAMSize sourceVRam;
                sourceVRam = this.getVRam();
                RAMSize copyVRam = ((RAMSize) strategy.copy(LocatorUtils.property(locator, "vRam", sourceVRam), sourceVRam));
                copy.setVRam(copyVRam);
            } else {
                copy.vRam = null;
            }
            if (this.vHardDisk!= null) {
                StorageCapacity sourceVHardDisk;
                sourceVHardDisk = this.getVHardDisk();
                StorageCapacity copyVHardDisk = ((StorageCapacity) strategy.copy(LocatorUtils.property(locator, "vHardDisk", sourceVHardDisk), sourceVHardDisk));
                copy.setVHardDisk(copyVHardDisk);
            } else {
                copy.vHardDisk = null;
            }
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new CapacityType();
    }

}