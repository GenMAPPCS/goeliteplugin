package org.genmapp.goelite;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.plugin.CytoscapePlugin;
import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobOutputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;
import edu.sdsc.nbcr.opal.types.OutputFileType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;
/*
 * 
 * The use case is quite linear for this plugin:
 * - user clicks on "Plugins->RUN Go-Elite"
 * - dialog box pops up with a bunch of input parameters
 * - user fills it out
 * - OPAL webservice computes a result based on the user input
 * - results are shown in the Results Panel of the cytoscape application
 * : the dialog disappears immediately after the user hits "submit"
 * : while processing occurs, the results tab gives some indicator of the current progress
 * : upon processing completion, 
 * 
 * Outputs:
 * - tree of terms and scores
 * - Jpanel, with list of pathways
 */
public class GOElitePlugin extends CytoscapePlugin 
{
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
	
	public GOElitePlugin() 
    { 
      System.out.println( "GO Elite Plugin start" );
	  JMenuItem item = new JMenuItem("Run GO-Elite");
	  JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar().getMenu("Plugins");
	  item.addActionListener(new GoElitePluginCommandListener( this ));

	  pluginMenu.add(item);

	   
    }

	class GOEliteInputDialog extends JDialog implements ActionListener
	{
		JButton launchButton = null;
		public final static long serialVersionUID = 0;
		AppServicePortType service = null;
		String jobID = null;
		LayoutProperties layoutProperties = null;
		String vSpecies[] = { new String( "Mm" ), new String( "Hs" ) };
		String vGeneSystems[] = { new String( "Ensembl" ), new String( "EntrezGene" ) };
		String vPruningAlgorithms[] = { new String( "z-score" ), new String( "gene number" ), 
				new String( "combination" ) };
		
		public GOEliteInputDialog( )
		{

	        layoutProperties = new LayoutProperties( "Go-elite" );

	        // XXX - Not supported by webservice
		    //layoutProperties.add( new Tunable("group", "group",
		    //		Tunable.GROUP, new Integer( 10 ) ) );
		    //layoutProperties.add(new Tunable("num_cpus", "Number of CPUs",
		    //       Tunable.INTEGER, new Integer( 1 ) ) );

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
		    layoutProperties.add( new Tunable( "num_permutations", "Number of permutations",
		    		Tunable.INTEGER, new Integer( 2 ) ) );
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
		    
		    launchButton = new JButton( "Launch" );		    
		    panel.add( launchButton );
		    launchButton.addActionListener( this );

		    panel.setSize( 350, 500 );
		    panel.setVisible( true );
		}
		public void actionPerformed( ActionEvent evt_ )
		{
			if ( evt_.getSource() != launchButton ) { return; }

			// The User just launched a job request
			// spawn worker thread			
			SwingWorker worker = new SwingWorker<StatusOutputType, Void>() 
			{
				edu.sdsc.nbcr.opal.types.StatusOutputType status = null;
					    @Override
			    public StatusOutputType doInBackground() 
			    {
					// fire off webservice request
			    	try
			    	{
					    AppServiceLocator findService = new AppServiceLocator();
					    System.out.println( "" + findService );
					    findService.setAppServicePortEndpointAddress(
					    		"http://webservices.cgl.ucsf.edu/opal/services/GOEliteService" 
					    );
					    System.out.println( "1>" + findService );
					    
					
					    service = findService.getAppServicePort(); 
					    System.out.println( "2>" + service );
					    JobInputType launchJobInput = new JobInputType();
		
					    layoutProperties.updateValues();  // must do this to refresh contents of the Tunables before we read from them
					    
	
					    // Process the gene list file
					    String geneListFilePath = layoutProperties.getValue( "input_gene_list_file" );
					    System.out.println( "file: " + geneListFilePath );
						File geneListFile = new File( geneListFilePath ); 
					    System.out.println( "2" );
					    InputFileType geneListOpalFile = new InputFileType();			    
						geneListOpalFile.setName( geneListFile.getName() );  	// extract the name portion of the full path
					    byte[] geneListFileBytes = getBytesFromFile( geneListFile );
					    geneListOpalFile.setContents( replaceCR( geneListFileBytes ) );
					    					    
					    String denomFilePath = layoutProperties.getValue( "input_denom_file" );
					    System.out.println( "file: " + denomFilePath );
						File denomFile = new File( denomFilePath ); 
						System.out.println( "2" );
					    InputFileType denomOpalFile = new InputFileType();	
					    System.out.println( denomFile.getName() );
						denomOpalFile.setName( denomFile.getName() );  	// extract the name portion of the full path
					    byte[] denomFileBytes = getBytesFromFile( denomFile );
					    denomOpalFile.setContents( replaceCR( denomFileBytes ) );
					    
					    // web service arguments are sent in as one long string
					    // Example: "--species Mm --denom denom.txt --input probesets.txt --mod EntrezGene 
					    //   --permutations 2 --method z-score --pval 0.05 --num 3"
					    String argList = "--species " + vSpecies[ new Integer( layoutProperties.getValue( "species" ) ).intValue() ] + " " +
					    				 "--denom " + denomFile.getName() + " " +
					    				 "--input " + geneListFile.getName() + " " +
					    				 "--mod " + vGeneSystems[ new Integer( layoutProperties.getValue( "gene_system" ) ).intValue() ] + " " +
					    				 "--permutations " + layoutProperties.getValue( "num_permutations" ) + " " +
					    				 "--method " + vPruningAlgorithms[ new Integer( layoutProperties.getValue( "go_pruning_algorithm" ) ).intValue() ] + " " +
					    				 "--pval " + layoutProperties.getValue( "p-value_thresh" ) + " " +
					    				 "--num " + layoutProperties.getValue( "min_num_genes_changed" ) + " "  +
					    				 "--zscore " + layoutProperties.getValue( "z-score_thresh" ) + " ";
					    System.out.println ( "argList: " + argList );				 
					    launchJobInput.setArgList( argList );
					    
					    InputFileType[] list = {geneListOpalFile, denomOpalFile};
					    launchJobInput.setInputFile( list );
					    JobSubOutputType output = service.launchJob(launchJobInput);
					    
					    jobID = output.getJobID();
					    System.out.println( "Job: " + jobID );
	
					    // 8 is the code for completion
				    	while( status == null || 8 != status.getCode() )
				    	{
				    		Thread.sleep( 5000 );
				    		status = service.queryStatus( output.getJobID() );
				    		System.out.println( "[" + status.getCode() + "] " + status.getMessage() ); 
				    	 }
			    	}
			    	catch( Exception e )
			    	{
			    		System.out.println ( "Exception: " + e );
			    	}
			    	return status;
			    }

			    @Override
			    public void done() 
			    {			      
			    	System.out.println( "done!" );
			        try {
			            StatusOutputType status = get();
			            if ( status.getCode() == 8 )
			            {
			                 JobOutputType outputs = service.getOutputs( jobID );
			                 OutputFileType[] files = outputs.getOutputFile();
			                 for ( int i =0; i < files.length; i++ )
			                 {
		                	     URL u;
						         InputStream is = null;
						         DataInputStream dis;
						         String s;
						         System.out.println( files[ i ].getName() );
						         if ( files[ i ].getName().contains( "GO-Elite_results" )  )
						         {
						        	 // dig into the results folder to get the file we care about
							         u = new URL( files[ i ].getUrl().toString() +
							        		 "/pruned-results_z-score_elite.txt" );
						         }
						         else
						         {
						        	 continue;
						         }
						         System.out.println( "Output URL: " + u );

						         try
			                	 {
								    //----------------------------------------------//
						            // Step 3:  Open an input stream from the url.  //
						            //----------------------------------------------//
	
						            is = u.openStream();         // throws an IOException
	
						            //-------------------------------------------------------------//
						            // Step 4:                                                     //
						            //-------------------------------------------------------------//
						            // Convert the InputStream to a buffered DataInputStream.      //
						            // Buffering the stream makes the reading faster; the          //
						            // readLine() method of the DataInputStream makes the reading  //
						            // easier.                                                     //
						            //-------------------------------------------------------------//
	
						            dis = new DataInputStream(new BufferedInputStream(is));
	
						            //------------------------------------------------------------//
						            // Step 5:                                                    //
						            //------------------------------------------------------------//
						            // Now just read each record of the input stream, and print   //
						            // it out.  Note that it's assumed that this problem is run   //
						            // from a command-line, not from an application or applet.    //
						            //------------------------------------------------------------//
	
						            while ((s = dis.readLine()) != null) {
						               System.out.println(s);
						            }

						         } catch (MalformedURLException mue) {
	
						            System.out.println("Ouch - a MalformedURLException happened.");
						            mue.printStackTrace();
						            System.exit(1);
	
						         } catch (IOException ioe) {
	
						            System.out.println("Oops- an IOException happened.");
						            ioe.printStackTrace();
						            System.exit(1);
	
						         } finally {
	
						            //---------------------------------//
						            // Step 6:  Close the InputStream  //
						            //---------------------------------//
	
						            try {
						               is.close();
						            } catch (IOException ioe) {
						               // just going to ignore this one
						            }
	
						         } // end of 'finally' clause
			                 
					      }  
			            }
			        } catch (InterruptedException ignore) {}
			        
			        catch (java.util.concurrent.ExecutionException e) {
			            String why = null;
			            Throwable cause = e.getCause();
			            if (cause != null) {
			                why = cause.getMessage();
			            } else {
			                why = e.getMessage();
			            }
			            System.err.println("Error retrieving file: " + why);
			        }
			        catch( Exception e ) { System.out.println( "Exception: " + e ); }
			    }
			};

			worker.execute();
		}
		
	}
	// Handles the top-level menu selection event from Cytoscape
	class GoElitePluginCommandListener implements ActionListener {
		GOElitePlugin plugin = null;
		public GoElitePluginCommandListener( GOElitePlugin plugin_ ) {
			plugin = plugin_;
		}

		public void actionPerformed(ActionEvent evt_ ) 
		{
			  try
			  {
				  CyGOEliteClient client = new CyGOEliteClient( plugin );
				  WebServiceClientManager.registerClient( client );
				  
				  // pop up dialog
				  GOEliteInputDialog dialog = new GOEliteInputDialog();
				  dialog.setSize( new Dimension( 300, 300 ) );
				  dialog.setVisible( true );
		      }
		      catch( Exception e )
		      {
		    	  System.out.println( "Exception: " + e );
		      }
		      
		}		  
	}
}

