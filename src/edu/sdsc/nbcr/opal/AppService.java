/**
 * AppService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package edu.sdsc.nbcr.opal;

public interface AppService extends javax.xml.rpc.Service {
    public java.lang.String getAppServicePortAddress();

    public edu.sdsc.nbcr.opal.AppServicePortType getAppServicePort() throws javax.xml.rpc.ServiceException;

    public edu.sdsc.nbcr.opal.AppServicePortType getAppServicePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
