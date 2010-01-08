/**
 * AppConfigType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package edu.sdsc.nbcr.opal.types;

public class AppConfigType  implements java.io.Serializable {
    private edu.sdsc.nbcr.opal.types.AppMetadataType metadata;

    private java.lang.String binaryLocation;

    private java.lang.String defaultArgs;

    private boolean parallel;

    public AppConfigType() {
    }

    public AppConfigType(
           edu.sdsc.nbcr.opal.types.AppMetadataType metadata,
           java.lang.String binaryLocation,
           java.lang.String defaultArgs,
           boolean parallel) {
           this.metadata = metadata;
           this.binaryLocation = binaryLocation;
           this.defaultArgs = defaultArgs;
           this.parallel = parallel;
    }


    /**
     * Gets the metadata value for this AppConfigType.
     * 
     * @return metadata
     */
    public edu.sdsc.nbcr.opal.types.AppMetadataType getMetadata() {
        return metadata;
    }


    /**
     * Sets the metadata value for this AppConfigType.
     * 
     * @param metadata
     */
    public void setMetadata(edu.sdsc.nbcr.opal.types.AppMetadataType metadata) {
        this.metadata = metadata;
    }


    /**
     * Gets the binaryLocation value for this AppConfigType.
     * 
     * @return binaryLocation
     */
    public java.lang.String getBinaryLocation() {
        return binaryLocation;
    }


    /**
     * Sets the binaryLocation value for this AppConfigType.
     * 
     * @param binaryLocation
     */
    public void setBinaryLocation(java.lang.String binaryLocation) {
        this.binaryLocation = binaryLocation;
    }


    /**
     * Gets the defaultArgs value for this AppConfigType.
     * 
     * @return defaultArgs
     */
    public java.lang.String getDefaultArgs() {
        return defaultArgs;
    }


    /**
     * Sets the defaultArgs value for this AppConfigType.
     * 
     * @param defaultArgs
     */
    public void setDefaultArgs(java.lang.String defaultArgs) {
        this.defaultArgs = defaultArgs;
    }


    /**
     * Gets the parallel value for this AppConfigType.
     * 
     * @return parallel
     */
    public boolean isParallel() {
        return parallel;
    }


    /**
     * Sets the parallel value for this AppConfigType.
     * 
     * @param parallel
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AppConfigType)) return false;
        AppConfigType other = (AppConfigType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.metadata==null && other.getMetadata()==null) || 
             (this.metadata!=null &&
              this.metadata.equals(other.getMetadata()))) &&
            ((this.binaryLocation==null && other.getBinaryLocation()==null) || 
             (this.binaryLocation!=null &&
              this.binaryLocation.equals(other.getBinaryLocation()))) &&
            ((this.defaultArgs==null && other.getDefaultArgs()==null) || 
             (this.defaultArgs!=null &&
              this.defaultArgs.equals(other.getDefaultArgs()))) &&
            this.parallel == other.isParallel();
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getMetadata() != null) {
            _hashCode += getMetadata().hashCode();
        }
        if (getBinaryLocation() != null) {
            _hashCode += getBinaryLocation().hashCode();
        }
        if (getDefaultArgs() != null) {
            _hashCode += getDefaultArgs().hashCode();
        }
        _hashCode += (isParallel() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AppConfigType.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://nbcr.sdsc.edu/opal/types", "AppConfigType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("metadata");
        elemField.setXmlName(new javax.xml.namespace.QName("", "metadata"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://nbcr.sdsc.edu/opal/types", "AppMetadataType"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("binaryLocation");
        elemField.setXmlName(new javax.xml.namespace.QName("", "binaryLocation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("defaultArgs");
        elemField.setXmlName(new javax.xml.namespace.QName("", "defaultArgs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parallel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parallel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
