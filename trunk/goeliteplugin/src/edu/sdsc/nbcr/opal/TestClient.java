package edu.sdsc.nbcr.opal;
import java.util.ArrayList;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceFactory;
import javax.xml.rpc.ServiceException;
import edu.sdsc.nbcr.opal.types.*;
import java.io.*;

public class TestClient {
	public static byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);
	
	    // Get the size of the file
	    long length = file.length();
	
	    // You cannot create an array using a long type.
	    // It needs to be an int type.
	    // Before converting to an int type, check
	    // to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }
	
	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];
	
	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }
	
	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }
	
	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}
	
	// replaces DOS-style carriage-returns with spaces: needed when sending a text file from DOS -> UNIX
	public static byte[] replaceCR( byte[] bytes )
	{
		byte[] newBytes = new byte[ bytes.length ];
		
		int j = 0;
		for( int i = 0; i < bytes.length; i++ )
		{
			if ( '\r' != bytes[ i ] ) { newBytes[ j ] = bytes[ i ]; j++; }
		}
		return( newBytes );
	}
	public static void main( String [] args ) throws javax.xml.rpc.ServiceException,java.rmi.RemoteException, java.io.IOException
	{
	    AppServiceLocator findService = new AppServiceLocator();
	    System.out.println( "" + findService );
	    findService.setAppServicePortEndpointAddress(
	    		"http://webservices.cgl.ucsf.edu/opal/services/GOEliteService" 
	    );
	    System.out.println( "" + findService );
	    
	
	    AppServicePortType service = findService.getAppServicePort(); 
	    System.out.println( "" + service );
	    JobInputType launchJobInput = new JobInputType();
	    launchJobInput.setArgList("--species Mm --denom denom.txt --input probesets.txt --mod EntrezGene --permutations 2 --method z-score --pval 0.05 --num 3");

	    
	    String dataFileName = "c://probesets.txt";
	    InputFileType dataFile = new InputFileType();
	    dataFile.setName( "probesets.txt" );
	    byte[] dataFileBytes = getBytesFromFile( new File( dataFileName ) );
	    dataFile.setContents( replaceCR( dataFileBytes ) );
	    
	    String denomFileName = "c://denom.txt";
	    InputFileType denomFile = new InputFileType();
	    denomFile.setName( "denom.txt" );
	    byte[] denomFileBytes = ( getBytesFromFile( new File( denomFileName ) ) );
	    denomFile.setContents( replaceCR(denomFileBytes ) );
	    
	    InputFileType[] list = {dataFile, denomFile};
	    launchJobInput.setInputFile( list );
	    JobSubOutputType output = service.launchJob(launchJobInput);
	    
	    System.out.println( "Job: " + output.getJobID() );
	}
}