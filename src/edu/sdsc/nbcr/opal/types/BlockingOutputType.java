
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BlockingOutputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BlockingOutputType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="status" type="{http://nbcr.sdsc.edu/opal/types}StatusOutputType"/>
 *         &lt;element name="jobOut" type="{http://nbcr.sdsc.edu/opal/types}JobOutputType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BlockingOutputType", propOrder = {
    "status",
    "jobOut"
})
public class BlockingOutputType {

    @XmlElement(required = true)
    protected StatusOutputType status;
    @XmlElement(required = true)
    protected JobOutputType jobOut;

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link StatusOutputType }
     *     
     */
    public StatusOutputType getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatusOutputType }
     *     
     */
    public void setStatus(StatusOutputType value) {
        this.status = value;
    }

    /**
     * Gets the value of the jobOut property.
     * 
     * @return
     *     possible object is
     *     {@link JobOutputType }
     *     
     */
    public JobOutputType getJobOut() {
        return jobOut;
    }

    /**
     * Sets the value of the jobOut property.
     * 
     * @param value
     *     allowed object is
     *     {@link JobOutputType }
     *     
     */
    public void setJobOut(JobOutputType value) {
        this.jobOut = value;
    }

}
