
package edu.sdsc.nbcr.opal.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for JobOutputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="JobOutputType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="stdOut" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="stdErr" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="outputFile" type="{http://nbcr.sdsc.edu/opal/types}OutputFileType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JobOutputType", propOrder = {
    "stdOut",
    "stdErr",
    "outputFile"
})
public class JobOutputType {

    @XmlSchemaType(name = "anyURI")
    protected String stdOut;
    @XmlSchemaType(name = "anyURI")
    protected String stdErr;
    protected List<OutputFileType> outputFile;

    /**
     * Gets the value of the stdOut property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStdOut() {
        return stdOut;
    }

    /**
     * Sets the value of the stdOut property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStdOut(String value) {
        this.stdOut = value;
    }

    /**
     * Gets the value of the stdErr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStdErr() {
        return stdErr;
    }

    /**
     * Sets the value of the stdErr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStdErr(String value) {
        this.stdErr = value;
    }

    /**
     * Gets the value of the outputFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the outputFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutputFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OutputFileType }
     * 
     * 
     */
    public List<OutputFileType> getOutputFile() {
        if (outputFile == null) {
            outputFile = new ArrayList<OutputFileType>();
        }
        return this.outputFile;
    }

}
