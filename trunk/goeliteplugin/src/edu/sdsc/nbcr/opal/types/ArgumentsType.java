
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArgumentsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArgumentsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="flags" type="{http://nbcr.sdsc.edu/opal/types}FlagsArrayType" minOccurs="0"/>
 *         &lt;element name="taggedParams" type="{http://nbcr.sdsc.edu/opal/types}ParamsArrayType" minOccurs="0"/>
 *         &lt;element name="untaggedParams" type="{http://nbcr.sdsc.edu/opal/types}ParamsArrayType" minOccurs="0"/>
 *         &lt;element name="implicitParams" type="{http://nbcr.sdsc.edu/opal/types}ImplicitParamsArrayType" minOccurs="0"/>
 *         &lt;element name="groups" type="{http://nbcr.sdsc.edu/opal/types}GroupsArrayType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArgumentsType", propOrder = {
    "flags",
    "taggedParams",
    "untaggedParams",
    "implicitParams",
    "groups"
})
public class ArgumentsType {

    protected FlagsArrayType flags;
    protected ParamsArrayType taggedParams;
    protected ParamsArrayType untaggedParams;
    protected ImplicitParamsArrayType implicitParams;
    protected GroupsArrayType groups;

    /**
     * Gets the value of the flags property.
     * 
     * @return
     *     possible object is
     *     {@link FlagsArrayType }
     *     
     */
    public FlagsArrayType getFlags() {
        return flags;
    }

    /**
     * Sets the value of the flags property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlagsArrayType }
     *     
     */
    public void setFlags(FlagsArrayType value) {
        this.flags = value;
    }

    /**
     * Gets the value of the taggedParams property.
     * 
     * @return
     *     possible object is
     *     {@link ParamsArrayType }
     *     
     */
    public ParamsArrayType getTaggedParams() {
        return taggedParams;
    }

    /**
     * Sets the value of the taggedParams property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParamsArrayType }
     *     
     */
    public void setTaggedParams(ParamsArrayType value) {
        this.taggedParams = value;
    }

    /**
     * Gets the value of the untaggedParams property.
     * 
     * @return
     *     possible object is
     *     {@link ParamsArrayType }
     *     
     */
    public ParamsArrayType getUntaggedParams() {
        return untaggedParams;
    }

    /**
     * Sets the value of the untaggedParams property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParamsArrayType }
     *     
     */
    public void setUntaggedParams(ParamsArrayType value) {
        this.untaggedParams = value;
    }

    /**
     * Gets the value of the implicitParams property.
     * 
     * @return
     *     possible object is
     *     {@link ImplicitParamsArrayType }
     *     
     */
    public ImplicitParamsArrayType getImplicitParams() {
        return implicitParams;
    }

    /**
     * Sets the value of the implicitParams property.
     * 
     * @param value
     *     allowed object is
     *     {@link ImplicitParamsArrayType }
     *     
     */
    public void setImplicitParams(ImplicitParamsArrayType value) {
        this.implicitParams = value;
    }

    /**
     * Gets the value of the groups property.
     * 
     * @return
     *     possible object is
     *     {@link GroupsArrayType }
     *     
     */
    public GroupsArrayType getGroups() {
        return groups;
    }

    /**
     * Sets the value of the groups property.
     * 
     * @param value
     *     allowed object is
     *     {@link GroupsArrayType }
     *     
     */
    public void setGroups(GroupsArrayType value) {
        this.groups = value;
    }

}
