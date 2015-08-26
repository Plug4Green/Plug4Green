//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//


package f4g.schemas.java;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for nonPositiveInteger simple type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;simpleType name="nonPositiveInteger"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer"&gt;
 *     &lt;maxInclusive value="0"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "nonPositiveInteger", propOrder = {
    "value"
})
public class NonPositiveInteger implements Cloneable, Visitable, CopyTo
{

    @XmlValue
    @XmlSchemaType(name = "nonPositiveInteger")
    protected BigInteger value;

    /**
     * Default no-arg constructor
     * 
     */
    public NonPositiveInteger() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public NonPositiveInteger(final BigInteger value) {
        this.value = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setValue(BigInteger value) {
        this.value = value;
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
        if (draftCopy instanceof NonPositiveInteger) {
            final NonPositiveInteger copy = ((NonPositiveInteger) draftCopy);
            if (this.value!= null) {
                BigInteger sourceValue;
                sourceValue = this.getValue();
                BigInteger copyValue = ((BigInteger) strategy.copy(LocatorUtils.property(locator, "value", sourceValue), sourceValue));
                copy.setValue(copyValue);
            } else {
                copy.value = null;
            }
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new NonPositiveInteger();
    }

}