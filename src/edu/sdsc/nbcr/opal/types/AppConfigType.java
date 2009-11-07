
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AppConfigType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AppConfigType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="metadata" type="{http://nbcr.sdsc.edu/opal/types}AppMetadataType"/>
 *         &lt;element name="binaryLocation" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="defaultArgs" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="parallel" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AppConfigType", propOrder = {
    "metadata",
    "binaryLocation",
    "defaultArgs",
    "parallel"
})
public class AppConfigType {

    @XmlElement(required = true)
    protected AppMetadataType metadata;
    @XmlElement(required = true)
    protected String binaryLocation;
    protected String defaultArgs;
    protected boolean parallel;

    /**
     * Gets the value of the metadata property.
     * 
     * @return
     *     possible object is
     *     {@link AppMetadataType }
     *     
     */
    public AppMetadataType getMetadata() {
        return metadata;
    }

    /**
     * Sets the value of the metadata property.
     * 
     * @param value
     *     allowed object is
     *     {@link AppMetadataType }
     *     
     */
    public void setMetadata(AppMetadataType value) {
        this.metadata = value;
    }

    /**
     * Gets the value of the binaryLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBinaryLocation() {
        return binaryLocation;
    }

    /**
     * Sets the value of the binaryLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBinaryLocation(String value) {
        this.binaryLocation = value;
    }

    /**
     * Gets the value of the defaultArgs property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultArgs() {
        return defaultArgs;
    }

    /**
     * Sets the value of the defaultArgs property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultArgs(String value) {
        this.defaultArgs = value;
    }

    /**
     * Gets the value of the parallel property.
     * 
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Sets the value of the parallel property.
     * 
     */
    public void setParallel(boolean value) {
        this.parallel = value;
    }

}
