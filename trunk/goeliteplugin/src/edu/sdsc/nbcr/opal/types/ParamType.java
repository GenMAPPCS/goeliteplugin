
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ParamType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ParamType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="INT"/>
 *     &lt;enumeration value="BOOL"/>
 *     &lt;enumeration value="FLOAT"/>
 *     &lt;enumeration value="STRING"/>
 *     &lt;enumeration value="FILE"/>
 *     &lt;enumeration value="URL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ParamType")
@XmlEnum
public enum ParamType {

    INT,
    BOOL,
    FLOAT,
    STRING,
    FILE,
    URL;

    public String value() {
        return name();
    }

    public static ParamType fromValue(String v) {
        return valueOf(v);
    }

}
