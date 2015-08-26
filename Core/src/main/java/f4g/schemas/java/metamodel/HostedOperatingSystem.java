//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.10 at 04:34:50 PM CEST 
//


package f4g.schemas.java.metamodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import com.massfords.humantask.Visitable;
import com.massfords.humantask.Visitor;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.CopyTo;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for HostedOperatingSystem complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HostedOperatingSystem"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{f4g/schemas/java/MetaModel}OperatingSystem"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{f4g/schemas/java/MetaModel}SoftwareApplication" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HostedOperatingSystem", propOrder = {
    "softwareApplication"
})
public class HostedOperatingSystem
    extends OperatingSystem
    implements Cloneable, Visitable, CopyTo
{

    @XmlElement(name = "SoftwareApplication", namespace = "f4g/schemas/java/MetaModel")
    protected List<SoftwareApplication> softwareApplication;

    /**
     * Default no-arg constructor
     * 
     */
    public HostedOperatingSystem() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public HostedOperatingSystem(final OperatingSystemType name, final MemoryUsage systemRAMBaseUsage, final List<FileSystem> fileSystem, final List<SoftwareRAID> softwareRAID, final List<SoftwareNetwork> softwareNetwork, final QName jaxbElementName, final List<SoftwareApplication> softwareApplication) {
        super(name, systemRAMBaseUsage, fileSystem, softwareRAID, softwareNetwork, jaxbElementName);
        this.softwareApplication = softwareApplication;
    }

    /**
     * Gets the value of the softwareApplication property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the softwareApplication property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSoftwareApplication().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SoftwareApplication }
     * 
     * 
     */
    public List<SoftwareApplication> getSoftwareApplication() {
        if (softwareApplication == null) {
            softwareApplication = new ArrayList<SoftwareApplication>();
        }
        return this.softwareApplication;
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
        super.copyTo(locator, draftCopy, strategy);
        if (draftCopy instanceof HostedOperatingSystem) {
            final HostedOperatingSystem copy = ((HostedOperatingSystem) draftCopy);
            if ((this.softwareApplication!= null)&&(!this.softwareApplication.isEmpty())) {
                List<SoftwareApplication> sourceSoftwareApplication;
                sourceSoftwareApplication = (((this.softwareApplication!= null)&&(!this.softwareApplication.isEmpty()))?this.getSoftwareApplication():null);
                @SuppressWarnings("unchecked")
                List<SoftwareApplication> copySoftwareApplication = ((List<SoftwareApplication> ) strategy.copy(LocatorUtils.property(locator, "softwareApplication", sourceSoftwareApplication), sourceSoftwareApplication));
                copy.softwareApplication = null;
                if (copySoftwareApplication!= null) {
                    List<SoftwareApplication> uniqueSoftwareApplicationl = copy.getSoftwareApplication();
                    uniqueSoftwareApplicationl.addAll(copySoftwareApplication);
                }
            } else {
                copy.softwareApplication = null;
            }
        }
        return draftCopy;
    }

    public Object createNewInstance() {
        return new HostedOperatingSystem();
    }

}