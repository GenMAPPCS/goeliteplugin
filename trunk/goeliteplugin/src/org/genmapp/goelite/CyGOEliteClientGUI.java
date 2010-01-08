package org.genmapp.goelite;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cytoscape.data.webservice.CyWebServiceEvent;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;

import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;

public class CyGOEliteClientGUI extends JPanel implements ActionListener
{
	static final long serialVersionUID = 0;
	private CyGOEliteClient client = null;
	JLabel textLabel = null;
	public CyGOEliteClientGUI( CyGOEliteClient client_ )
	{
		client_ = client;
		textLabel = new JLabel();
		textLabel.setText( "Nothing yet");
		add( textLabel );
		
	  String vSpecies[] = { new String( "Mm" ), new String( "Hs" ) };
	  String vGeneSystems[] = { new String( "Ensembl" ), new String( "EntrezGene" ) };
	  String vPruningAlgorithms[] = { new String( "z-score" ), new String( "gene number" ), 
			new String( "combination" ) };

      LayoutProperties layoutProperties = new LayoutProperties( "Go-elite" );

	    layoutProperties.add( new Tunable("group", "group",
	    		Tunable.GROUP, new Integer( 10 ) ) );
	    layoutProperties.add(new Tunable("num_cpus", "Number of CPUs",
	            Tunable.INTEGER, new Integer( 1 ) ) );

	    layoutProperties.add(new Tunable("species", "Species to analyze",
	            Tunable.LIST, new Integer(0),
	            (Object) vSpecies, (Object) null, 0));
	    layoutProperties.add( new Tunable( "input_denom_file", "Input denominator file",
	    		Tunable.STRING, new String( "" ) ) );
	    layoutProperties.add( new Tunable( "input_gene_list_file", "Input gene list file",
	    		Tunable.STRING, new String( "" ) ) );
	    layoutProperties.add(new Tunable("gene_system", "Primary gene system",
	            Tunable.LIST, new Integer(0),
	            (Object) vGeneSystems, (Object) null, 0));
	    layoutProperties.add( new Tunable( "num_permutations", "Number of permutations for over-representation analysis",
	    		Tunable.INTEGER, new Integer( 2000 ) ) );
	    layoutProperties.add( new Tunable( "go_pruning_algorithm", "GO pruning algorithm",
	    		Tunable.LIST, new Integer( 0 ),
	    		(Object) vPruningAlgorithms, (Object) null, 0 ) );
	    layoutProperties.add(new Tunable( "z-score_thresh", "Z-score threshold",
	            Tunable.DOUBLE, new Double( 1.96 ) ) );
	    layoutProperties.add(new Tunable("p-value_thresh", "P-value threshold",
	            Tunable.DOUBLE, new Double( 0.05 ) ) );
	    layoutProperties.add(new Tunable("min_num_genes_changed", "Minimum number of genes changed",
	            Tunable.INTEGER, new Integer( 3 ) ) );

	    JPanel panel = layoutProperties.getTunablePanel();
	    add( panel );
	    panel.setSize( 300, 500 );
	    panel.setVisible( true );
	    
	    JButton launchButton = new JButton( "Launch" );
	    add( launchButton );
	    launchButton.addActionListener( this );
	}
	public void setResults( JobSubOutputType results_ )
	{
		String resultsAsText = results_.toString();
		textLabel.setText( resultsAsText );
	}
	
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
	
	public void actionPerformed( ActionEvent evt_ )
	{
		try
		{
			// XXX - begin dummy section
			JobInputType launchJobInput = new JobInputType(); // XXXX - should receive from plugin
			   launchJobInput.setArgList("--species Mm --denom denom.txt --input probesets.txt --mod EntrezGene --permutations 2 --method z-score --pval 0.05 --num 3");
			
			   try
			   {
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
			   }
			   catch( Exception e )
			   {
				   System.out.println( "Exception: " + e );
			   }
			// XXX - end of dummy section
			   
			   
			WebServiceClientManager.getCyWebServiceEventSupport().fireCyWebServiceEvent(
					new CyWebServiceEvent<JobInputType>(
							client.getClientID(),
							null,
							launchJobInput
					)
			);
		}
		catch( Exception e )
		{
			System.out.println( "Exception " + e );
		}
		
	}
}
