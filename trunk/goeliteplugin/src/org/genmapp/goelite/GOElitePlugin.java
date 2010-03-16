package org.genmapp.goelite;

import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.io.FileWriter;
import javax.swing.BorderFactory; 
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;
import java.awt.TextArea;
import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobOutputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;
import edu.sdsc.nbcr.opal.types.OutputFileType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import edu.sdsc.nbcr.opal.*;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import org.jdesktop.swingworker.SwingWorker;  // Part of JDK 1.6, use backported version for now

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
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
		TextArea debugWindow = null;
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
		    		Tunable.STRING, new String( "c:\\denom.txt" ) ) );
		    layoutProperties.add( new Tunable( "input_gene_list_file", "Input gene list file",
		    		Tunable.STRING, new String( "c:\\probesets.txt" ) ) );
		    layoutProperties.add(new Tunable("gene_system", "Primary gene system",
		            Tunable.LIST, new Integer(0),
		            (Object) vGeneSystems, (Object) null, 0));
		    layoutProperties.add( new Tunable( "num_permutations", "Number of permutations",
		    		Tunable.INTEGER, new Integer( 1 ) ) );
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
		    
		    launchButton = new JButton( "Run analysis" );		    
		    panel.add( launchButton );
		    launchButton.addActionListener( this );

		    debugWindow = new TextArea( "", 5, 40 );
		    panel.add( debugWindow );
		    panel.setSize( 350, 500 );
		    panel.setVisible( true );
		}
		public void actionPerformed( ActionEvent evt_ )
		{
			debugWindow.append( "evt source: " + evt_.getSource() );
			//if ( evt_.getSource() != launchButton ) { return; }
			
			// The User just launched a job request
			// spawn worker thread			
			SwingWorker<StatusOutputType, Void> worker = new SwingWorker<StatusOutputType, Void>() 
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
					    debugWindow.append( "1>" + findService  + "\n" );
					
					    service = findService.getAppServicePort(); 
					    System.out.println( "2>" + service );
					    debugWindow.append( "2>" + service  + "\n" );
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
					    debugWindow.append( "argList" + argList + "\n" );
					    launchJobInput.setArgList( argList );
					    
					    InputFileType[] list = {geneListOpalFile, denomOpalFile};
					    launchJobInput.setInputFile( list );
					    JobSubOutputType output = service.launchJob(launchJobInput);
					    
					    jobID = output.getJobID();
					    System.out.println( "Job: " + jobID );
					    debugWindow.append( "Job: " + jobID  + "\n" );

					    // 8 is the code for completion
				    	while( status == null || 8 != status.getCode() )
				    	{
				    		Thread.sleep( 5000 );
				    		status = service.queryStatus( output.getJobID() );
				    		System.out.println( "[" + status.getCode() + "] " + status.getMessage() ); 
				    		debugWindow.append( "[" + status.getCode() + "] " + status.getMessage() + "\n" );
				    	 }
			    	}
			    	catch( Exception e )
			    	{
			    		System.out.println ( "Exception: " + e );
			    	}
			    	return status;
			    }

			    public Vector<String> getFileContents(URL u) throws MalformedURLException, IOException
			    {
					Vector<String> contents = new Vector<String>();

					InputStream is = null;
					DataInputStream dis = null;
					
					// ----------------------------------------------//
					// Step 3: Open an input stream from the
					// url. //
					// ----------------------------------------------//

					is = u.openStream(); // throws an
											// IOException
					// -------------------------------------------------------------//

					// Step 4: //
					// -------------------------------------------------------------//
					// Convert the InputStream to a buffered
					// DataInputStream. //
					// Buffering the stream makes the reading
					// faster; the //
					// readLine() method of the DataInputStream
					// makes the reading //
					// easier. //
					// -------------------------------------------------------------//

					dis = new DataInputStream(
							new BufferedInputStream(is));

					// ------------------------------------------------------------//
					// Step 5: //
					// ------------------------------------------------------------//
					// Now just read each record of the input
					// stream, and print //
					// it out. Note that it's assumed that this
					// problem is run //
					// from a command-line, not from an
					// application or applet. //
					// ------------------------------------------------------------//

					String s = null;
					while ((s = dis.readLine()) != null) {
						contents.add( s );
					}

					return (contents);
				}

			    @Override
			    public void done() 
			    {			      
			    	System.out.println( "done!" );
			    	debugWindow.append( "done!\n" );
			    	try
			    	{
	
				    	// print results in results panel
						StatusOutputType status = get();
						if (status.getCode() == 8) 
						{
							JobOutputType outputs = service.getOutputs(jobID);
							OutputFileType[] files = outputs.getOutputFile();
							Vector< String > logFileContents = null; 
						    Vector< String > GONameResultsColumnNames = new Vector< String >();
						    Vector< String > pathwayResultsColumnNames = new Vector< String >();
						    Vector< Vector > GONameResultsColumnData = new Vector< Vector >();
						    Vector< Vector > pathwayResultsColumnData = new Vector< Vector >();
							
							// process each output file that's sitting on server
							for (int i = 0; i < files.length; i++) 
							{
								URL u = null;
								InputStream is = null;
								DataInputStream dis;
								String s;
								System.out.println(files[i].getName());
						    	debugWindow.append( files[i].getName() + "\n" );
								if (files[i].getName().contains( "GO-Elite_results" ) ) 
								{
									// dig into the results folder to get the
									// file we care about
							    	debugWindow.append( "results found\n" );
									u = new URL(
											files[i].getUrl().toString()
													+ "/pruned-results_z-score_elite.txt");
									
							    	debugWindow.append( "Output URL: " + u + "\n" );
									
									Vector<String> fileContents = getFileContents( u );
									Enumeration< String > contents = fileContents.elements();
									
									boolean processingGONameResultsNotPathwayResults = true;
								    
									while( contents.hasMoreElements() )
									{
										
										String line = ( String )contents.nextElement() ;
										System.out.println( line );
										Vector<String> columnsAsVector = new Vector< String >();
										String [] columnsData = ( line ).split( "\t" ); 
										
										if ( columnsData.length < 2 ) { continue; } // ignore blank lines

										if ( columnsData[ 2 ].contains( "MAPP" )) 
										{
											processingGONameResultsNotPathwayResults = false;
										}
										
										
										// is this a column header?
										if ( processingGONameResultsNotPathwayResults && GONameResultsColumnNames.size() == 0 )
										{
											GONameResultsColumnNames.addAll( Arrays.asList( columnsData ) );
											continue;
										}
										else if ( !processingGONameResultsNotPathwayResults && pathwayResultsColumnNames.size() == 0 )
										{
											pathwayResultsColumnNames.addAll( Arrays.asList( columnsData ) );
											continue;
										}
									
										// it's a data line
										if ( processingGONameResultsNotPathwayResults )
										{
											GONameResultsColumnData.add( new Vector< String >( Arrays.asList( columnsData ) ) );										
										}
										else
										{
											pathwayResultsColumnData.add( new Vector< String >( Arrays.asList( columnsData ) ) );										
										}
									}
								} 
								else if ( files[ i ].getName().contains( "GO-Elite_report.log" ) )
								{
									logFileContents = getFileContents( new URL( files[ i ].getUrl().toString() ) );
									continue;
								}	
								else
								{
									continue;
								}
							} // ... end of "for"

							
							// populate tables
							CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
							
					    	JTabbedPane resultsParentPanel = new JTabbedPane();						    	
							JTable GONameResultsTable = new JTable( GONameResultsColumnData, GONameResultsColumnNames );
							JTable pathwayResultsTable = new JTable( pathwayResultsColumnData, pathwayResultsColumnNames );
							
					    	// hide some columns
					    	int [] GONameColumnsToHide = { 0, 1, 5, 6, 7, 11, 12, 13 };
					    	int [] pathwayColumnsToHide = GONameColumnsToHide;  // hide the same columns
					    	
							for ( int j = GONameColumnsToHide.length - 1; j >= 0; j-- )
							{
								TableColumn column = GONameResultsTable.getColumnModel().getColumn( GONameColumnsToHide[ j ] );
								debugWindow.append( "removing column: " + column );
								GONameResultsTable.removeColumn( column );
							}
							for ( int j = pathwayColumnsToHide.length - 1; j >= 0; j-- )
							{
								TableColumn column = pathwayResultsTable.getColumnModel().getColumn( pathwayColumnsToHide[ j ] );
								pathwayResultsTable.removeColumn( column );
							}
							
					    	JTextArea logFileOutput = new JTextArea( ( logFileContents != null ? logFileContents.toString() : "Log file not found on server" ) );
					    	resultsParentPanel.addTab( "GO", new JScrollPane( GONameResultsTable ) );
							resultsParentPanel.addTab( "Pathway", new JScrollPane( pathwayResultsTable ) );
							resultsParentPanel.addTab( "Log File", new JScrollPane( logFileOutput ) );
							cytoPanel.add( "GO-Elite Outputs", resultsParentPanel );
							
						} // if... end
			    	}
					catch( Exception e )
					{
						System.out.println( "Exception " + e );
					}  // try...catch...end		
					
			    } // done() end
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

