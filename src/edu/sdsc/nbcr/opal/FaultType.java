/**
 * This class was generated by Apache CXF 2.2.4
 * Fri Nov 06 13:47:24 PST 2009
 * Generated source version: 2.2.4
 * 
 */

package edu.sdsc.nbcr.opal;

import javax.xml.ws.WebFault;

@WebFault(name = "opalFaultOutput", targetNamespace = "http://nbcr.sdsc.edu/opal/types")
public class FaultType extends Exception {
    public static final long serialVersionUID = 20091106134724L;
    
    private edu.sdsc.nbcr.opal.types.FaultType opalFaultOutput;

    public FaultType() {
        super();
    }
    
    public FaultType(String message) {
        super(message);
    }
    
    public FaultType(String message, Throwable cause) {
        super(message, cause);
    }

    public FaultType(String message, edu.sdsc.nbcr.opal.types.FaultType opalFaultOutput) {
        super(message);
        this.opalFaultOutput = opalFaultOutput;
    }

    public FaultType(String message, edu.sdsc.nbcr.opal.types.FaultType opalFaultOutput, Throwable cause) {
        super(message, cause);
        this.opalFaultOutput = opalFaultOutput;
    }

    public edu.sdsc.nbcr.opal.types.FaultType getFaultInfo() {
        return this.opalFaultOutput;
    }
}
