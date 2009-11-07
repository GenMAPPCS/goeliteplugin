
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IOType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="IOType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="INPUT"/>
 *     &lt;enumeration value="OUTPUT"/>
 *     &lt;enumeration value="INOUT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "IOType")
@XmlEnum
public enum IOType {

    INPUT,
    OUTPUT,
    INOUT;

    public String value() {
        return name();
    }

    public static IOType fromValue(String v) {
        return valueOf(v);
    }

}
