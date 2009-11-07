
package edu.sdsc.nbcr.opal.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for JobInputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="JobInputType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="argList" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="numProcs" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="inputFile" type="{http://nbcr.sdsc.edu/opal/types}InputFileType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JobInputType", propOrder = {
    "argList",
    "numProcs",
    "inputFile"
})
public class JobInputType {

    protected String argList;
    protected Integer numProcs;
    protected List<InputFileType> inputFile;

    /**
     * Gets the value of the argList property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArgList() {
        return argList;
    }

    /**
     * Sets the value of the argList property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArgList(String value) {
        this.argList = value;
    }

    /**
     * Gets the value of the numProcs property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumProcs() {
        return numProcs;
    }

    /**
     * Sets the value of the numProcs property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumProcs(Integer value) {
        this.numProcs = value;
    }

    /**
     * Gets the value of the inputFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link InputFileType }
     * 
     * 
     */
    public List<InputFileType> getInputFile() {
        if (inputFile == null) {
            inputFile = new ArrayList<InputFileType>();
        }
        return this.inputFile;
    }

}
