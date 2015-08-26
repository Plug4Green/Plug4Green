//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//


package f4g.schemas.java.constraints.optimizerconstraints;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for VMFlavorType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VMFlavorType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="VMFlavor" maxOccurs="unbounded"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *                   &lt;element name="capacity" type="{f4g/schemas/java/constraints/OptimizerConstraints}capacityType"/&gt;
 *                   &lt;element name="expectedLoad" type="{f4g/schemas/java/constraints/OptimizerConstraints}expectedLoad"/&gt;
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
@XmlType(name = "VMFlavorType", propOrder = {
    "vmFlavor"
})
public class VMFlavorType implements Cloneable, Visitable, CopyTo
{

    @XmlElement(name = "VMFlavor", required = true)
    protected List<VMFlavorType.VMFlavor> vmFlavor;

    /**
     * Default no-arg constructor
     * 
     */
    public VMFlavorType() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public VMFlavorType(final List<VMFlavorType.VMFlavor> vmFlavor) {
        this.vmFlavor = vmFlavor;
    }

    /**
     * Gets the value of the vmFlavor property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vmFlavor property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVMFlavor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VMFlavorType.VMFlavor }
     * 
     * 
     */
    public List<VMFlavorType.VMFlavor> getVMFlavor() {
        if (vmFlavor == null) {
            vmFlavor = new ArrayList<VMFlavorType.VMFlavor>();
        }
        return this.vmFlavor;
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
        if (draftCopy instanceof VMFlavorType) {
            final VMFlavorType copy = ((VMFlavorType) draftCopy);
            if ((this.vmFlavor!= null)&&(!this.vmFlavor.isEmpty())) {
                List<VMFlavorType.VMFlavor> sourceVMFlavor;
                sourceVMFlavor = (((this.vmFlavor!= null)&&(!this.vmFlavor.isEmpty()))?this.getVMFlavor():null);
                @SuppressWarnings("unchecked")
                List<VMFlavorType.VMFlavor> copyVMFlavor = ((List<VMFlavorType.VMFlavor> ) strategy.copy(LocatorUtils.property(locator, "vmFlavor", sourceVMFlavor), sourceVMFlavor));
                copy.vmFlavor = null;
                if (copyVMFlavor!= null) {
                    List<VMFlavorType.VMFlavor> uniqueVMFlavorl = copy.getVMFlavor();
                    uniqueVMFlavorl.addAll(copyVMFlavor);
                }
            } else {
                copy.vmFlavor = null;
            }
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new VMFlavorType();
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
     *         &lt;element name="capacity" type="{f4g/schemas/java/constraints/OptimizerConstraints}capacityType"/&gt;
     *         &lt;element name="expectedLoad" type="{f4g/schemas/java/constraints/OptimizerConstraints}expectedLoad"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "capacity",
        "expectedLoad"
    })
    public static class VMFlavor implements Cloneable, Visitable, CopyTo
    {

        @XmlElement(required = true)
        protected String name;
        @XmlElement(required = true)
        protected CapacityType capacity;
        @XmlElement(required = true)
        protected ExpectedLoad expectedLoad;

        /**
         * Default no-arg constructor
         * 
         */
        public VMFlavor() {
            super();
        }

        /**
         * Fully-initialising value constructor
         * 
         */
        public VMFlavor(final String name, final CapacityType capacity, final ExpectedLoad expectedLoad) {
            this.name = name;
            this.capacity = capacity;
            this.expectedLoad = expectedLoad;
        }

        /**
         * Gets the value of the name property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Gets the value of the capacity property.
         * 
         * @return
         *     possible object is
         *     {@link CapacityType }
         *     
         */
        public CapacityType getCapacity() {
            return capacity;
        }

        /**
         * Sets the value of the capacity property.
         * 
         * @param value
         *     allowed object is
         *     {@link CapacityType }
         *     
         */
        public void setCapacity(CapacityType value) {
            this.capacity = value;
        }

        /**
         * Gets the value of the expectedLoad property.
         * 
         * @return
         *     possible object is
         *     {@link ExpectedLoad }
         *     
         */
        public ExpectedLoad getExpectedLoad() {
            return expectedLoad;
        }

        /**
         * Sets the value of the expectedLoad property.
         * 
         * @param value
         *     allowed object is
         *     {@link ExpectedLoad }
         *     
         */
        public void setExpectedLoad(ExpectedLoad value) {
            this.expectedLoad = value;
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
            if (draftCopy instanceof VMFlavorType.VMFlavor) {
                final VMFlavorType.VMFlavor copy = ((VMFlavorType.VMFlavor) draftCopy);
                if (this.name!= null) {
                    String sourceName;
                    sourceName = this.getName();
                    String copyName = ((String) strategy.copy(LocatorUtils.property(locator, "name", sourceName), sourceName));
                    copy.setName(copyName);
                } else {
                    copy.name = null;
                }
                if (this.capacity!= null) {
                    CapacityType sourceCapacity;
                    sourceCapacity = this.getCapacity();
                    CapacityType copyCapacity = ((CapacityType) strategy.copy(LocatorUtils.property(locator, "capacity", sourceCapacity), sourceCapacity));
                    copy.setCapacity(copyCapacity);
                } else {
                    copy.capacity = null;
                }
                if (this.expectedLoad!= null) {
                    ExpectedLoad sourceExpectedLoad;
                    sourceExpectedLoad = this.getExpectedLoad();
                    ExpectedLoad copyExpectedLoad = ((ExpectedLoad) strategy.copy(LocatorUtils.property(locator, "expectedLoad", sourceExpectedLoad), sourceExpectedLoad));
                    copy.setExpectedLoad(copyExpectedLoad);
                } else {
                    copy.expectedLoad = null;
                }
            }
            return draftCopy;
        }

        public Object createNewInstance() {
            return new VMFlavorType.VMFlavor();
        }

    }

}