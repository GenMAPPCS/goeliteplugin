
package edu.sdsc.nbcr.opal.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GroupsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroupsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="elements" type="{http://www.w3.org/2001/XMLSchema}IDREFS"/>
 *         &lt;element name="required" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="exclusive" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="semanticType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="textDesc" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroupsType", propOrder = {
    "name",
    "elements",
    "required",
    "exclusive",
    "semanticType",
    "textDesc"
})
public class GroupsType {

    @XmlElement(required = true)
    protected String name;
    @XmlList
    @XmlElement(required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    protected List<Object> elements;
    protected Boolean required;
    protected Boolean exclusive;
    protected String semanticType;
    protected String textDesc;

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
     * Gets the value of the elements property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the elements property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getElements().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getElements() {
        if (elements == null) {
            elements = new ArrayList<Object>();
        }
        return this.elements;
    }

    /**
     * Gets the value of the required property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRequired() {
        return required;
    }

    /**
     * Sets the value of the required property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRequired(Boolean value) {
        this.required = value;
    }

    /**
     * Gets the value of the exclusive property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isExclusive() {
        return exclusive;
    }

    /**
     * Sets the value of the exclusive property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setExclusive(Boolean value) {
        this.exclusive = value;
    }

    /**
     * Gets the value of the semanticType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSemanticType() {
        return semanticType;
    }

    /**
     * Sets the value of the semanticType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSemanticType(String value) {
        this.semanticType = value;
    }

    /**
     * Gets the value of the textDesc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextDesc() {
        return textDesc;
    }

    /**
     * Sets the value of the textDesc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextDesc(String value) {
        this.textDesc = value;
    }

}
