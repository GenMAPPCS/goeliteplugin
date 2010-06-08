package org.genmapp.goelite;

import giny.model.Node;
import java.util.List;
import java.util.ArrayList;
import cytoscape.layout.LayoutProperties;
import cytoscape.plugin.PluginManager;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.io.FileWriter;
import java.awt.Component;
import javax.swing.BorderFactory; 
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.event.ActionEvent;
import java.awt.Color;
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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import edu.sdsc.nbcr.opal.*;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
	static boolean bResultsMasterPanelAlreadyAdded = false;  // used for results panel in Cytoscape window
	CloseableTabbedPane resultsMasterPanel = null;
	
	public long countLinesInFile( String filename )
	{
	    // Count the number of lines in the specified file, and
	    // print the number to standard output.  If an error occurs
	    // while processing the file, print an error message instead.
	    // Two try...catch statements are used so I can give a
	    // different error message in each case.
		long lineCount = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader( filename ) );
			String line = "";
			while ((  line = br.readLine()) != null) {
		        lineCount++;
			}
		}
		catch( Exception e )
		{
			return( -1 );
		}
		return( lineCount );
   }  // end countLines()

	public String [] getCriteriaSets( CyNetwork network, TextArea debugWindow )
	{
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		String[] a = {""};
		
	 	debugWindow.append( "getCriteriaSets start for network " + network.getIdentifier() );
	 	debugWindow.append( "cyattributes " + networkAttributes.getAttributeNames().length );
		if ( networkAttributes.hasAttribute(network.getIdentifier(),
					"__criteria")) {
				debugWindow.append( "__criteria found");
				ArrayList<String> temp = (ArrayList<String>) networkAttributes
						.getListAttribute(network.getIdentifier(), "__criteria");
				ArrayList<String> full = new ArrayList<String>();
				for (String s : temp) {
					full.add(s);
				}
				debugWindow.append( "a found " + a.length );
				return full.toArray(a);
			
				}
		debugWindow.append( "getCriteriaSets end (" + a.length + ")" );
		return( a );
	}
	// for a given criteriaSet, return its criteria
	public String [] getCriteria( String criteriaSet, CyNetwork network, TextArea debugWindow )
	{
		ArrayList<String> criteriaNames = new ArrayList<String>();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		ArrayList<String> temp = ((ArrayList<String>) networkAttributes.getListAttribute( network.getIdentifier(), criteriaSet ) );
		debugWindow.append( "criteria for " + criteriaSet + " found " + temp.size() );
		
		// split first on "comma", then on ":"
		boolean isFirst = true;
		for ( String criterion : temp )	
		{
			// skip the first entry, it's not actually a criterion
			if ( isFirst ) { isFirst = false; continue; }
			
			String[] tokens = criterion.split( ":" );
			debugWindow.append( "tokens[1]: " + tokens[ 1 ] );
			criteriaNames.add( tokens[ 1 ] );
		}
		return( ( String [] ) criteriaNames.toArray ( new String [ criteriaNames.size() ] ) );
	}
	
	// produces a probeset/denominator file that can be sent to the webservice for GO-Elite analysis
	public static void generateInputFileFromNetworkCriteria( String pathToFile, String systemCode, String fileName, String criteriaSetName, 
			String criteriaLabel, boolean bAcceptTrueValuesOnly,
			TextArea debugWindow ) throws java.io.IOException
	{
		debugWindow.append( "opening filewriter\n" );
		FileWriter fw = new FileWriter( pathToFile, false );
		debugWindow.append( "filewriter opened\n" );
		PrintWriter out = new PrintWriter( fw );
		debugWindow.append( "2\n" );

		out.write( "id\tsystemCode" );
		debugWindow.append( "3\n" );
		out.println();
		debugWindow.append( "4\n" );
		
		/// for every node, 
		// get all nodes that pass the test
		String nodeAttributeCriteriaLabel = criteriaSetName + "_" + criteriaLabel;
		List<Node> nodeList = Cytoscape.getCyNodesList();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		debugWindow.append( "bAcceptTrueValuesOnly: " + bAcceptTrueValuesOnly );
		boolean bFirstValue = true;
		for(Node node: nodeList)
	    {
			boolean value = false;
			if ( nodeAttributes.hasAttribute( node.getIdentifier(), nodeAttributeCriteriaLabel ) )
			{
				if ( !bAcceptTrueValuesOnly || nodeAttributes.getBooleanAttribute( node.getIdentifier(), nodeAttributeCriteriaLabel ) )				
				{
					if ( node.getIdentifier().length() > 0 )
					{
					  if ( !bFirstValue ) { out.println( ); } // opal server barfs on empty lines in numerator file
					  out.write( node.getIdentifier() + "\t" + systemCode );
					  bFirstValue = false;
					}
				}
			}
	    }
		out.close();
		debugWindow.append( "done writing input file\n" );
		
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
	
	public GOElitePlugin() 
	{ 
		System.out.println( "GO Elite Plugin start" );
		JMenuItem item = new JMenuItem("Run GO-Elite");
		JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar().getMenu("Plugins");
		item.addActionListener(new GoElitePluginCommandListener( this ));
		pluginMenu.add(item);

		// LayoutProperties layoutProperties = initialize();
		//new GOEliteCommandHandler(layoutProperties);
	
	}
/*
	protected static final String GETDATA "get data";
	protected static final String ID "id";
	protected static final String LAUNCH "launch";
	protected static final String OPENDIALOG "open dialog";
	protected static final String STATUS "status";

	class GOEliteCommandHandler extends AbstractCommandHandler {
		public GOEliteCommandHandler(LayoutProperties props) {
			super(CyCommandManager.reserveNamespace("goelite"));
			// GETDATA:
			//	get data id="id"
			addDescription(GETDATA, "");
			addArgument(GETDATA, ID);

			// LAUNCH
			addDescription(LAUNCH, "");
			for (Tunable t: props) {
				addArgument(LAUNCH, t);
			}

			// OPENDIALOG
			addDescription(OPENDIALOG, "");
			addArgument(OPENDIALOG);

			// STATUS
			addDescription(STATUS, "");
			addArgument(STATUS, ID);
		}

		public CyCommandResult execute(String command, Collection<Tunable>args) throws CyCommandException {
			CyCommandResult result = new CyCommandResult();
			if (LAUNCH.equals(command)) {
				// Launch
				// Get id
				results.addMessage("GOElite id = "+id);
				results.addResult("id", id)
			} else if (OPENDIALOG.equals(command)) {
				GOEliteInputDialog dialog = new GOEliteInputDialog(props);
				dialog.setVisible(true);
			} else if (STATUS.equals(command)) {
				String id = getArg(command, ID, args);
			} else if (GETDATA.equals(command)) {
				String id = getArg(command, ID, args);
			}
		}

		public CyCommandResult execute(String command, Map<String, Object>args) throws CyCommandException {
			return execute(command, createTunableCollection(args));
		}

		
	}
*/
	class GOEliteInputDialog extends JDialog implements ActionListener
	{
		String criteriaAllStringValue = "-all-";  // selection value for the criteria "all" 
		JButton launchButton = null;
		JButton generateInputFileButton = null;
		TextArea debugWindow = null;
		public final static long serialVersionUID = 0;
		AppServicePortType service = null;
		String jobID = null;
		LayoutProperties layoutProperties = null;
		String vSpecies[] = { new String( "Mm" ), new String( "Hs" ) };
		String vGeneSystems[] = { new String( "Ensembl" ), new String( "EntrezGene" ) };
		String vGeneSystemCodes[] = { new String ( "En" ), new String( "L" ) };
		String vPruningAlgorithms[] = { new String( "z-score" ), new String( "gene number" ), 
				new String( "combination" ) };
		JLabel inputDenomFilenameDescriptor = null, inputNumeratorFilenameDescriptor = null;
		JLabel inputDenomFilenameLabel, inputNumeratorFilenameLabel;
    	JTextArea inputNumerFileTextArea= null, inputDenomFileTextArea = null;
    	JButton inputNumerFileBrowseButton = null, inputDenomFileBrowseButton = null;
    	JComboBox inputCriteriaSetComboBox = null, inputCriteriaComboBox = null;
    	boolean bIsInputAFile = true;

		
		//public GOEliteInputDialog(LayoutProperties layoutProperties )
    	public GOEliteInputDialog()
		{
	        layoutProperties = new LayoutProperties( "Go-elite" );

		    layoutProperties.add(new Tunable("species", "Species to analyze",
		            Tunable.LIST, new Integer(0),
		            (Object) vSpecies, (Object) null, 0)); 
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

		    final JPanel panel = layoutProperties.getTunablePanel();

		    JPanel inputSourcePanel = new JPanel( );
		    inputSourcePanel.add( new JLabel( "Input Source " ) );
		    
		    ButtonGroup inputButtonsGroup = new ButtonGroup();
		    final JRadioButton inputSourceFileRadioButton = new JRadioButton( "File" );
		    final JRadioButton inputSourceCriteriaRadioButton = new JRadioButton( "Criteria" );
		    inputSourcePanel.add( inputSourceFileRadioButton );
		    inputSourcePanel.add( inputSourceCriteriaRadioButton );
		    inputButtonsGroup.add( inputSourceFileRadioButton );
		    inputButtonsGroup.add( inputSourceCriteriaRadioButton );
		    final JPanel inputSourceExpandingPanel = new JPanel();
    		inputSourceExpandingPanel.setLayout( new BoxLayout( inputSourceExpandingPanel, BoxLayout.PAGE_AXIS ) );

		    inputSourceExpandingPanel.setSize( 500, 500 );
		    inputSourcePanel.add( inputSourceExpandingPanel );
		    
		    ActionListener inputSourceActionListener = new ActionListener()
		    {
		    	public boolean isInputAFile() { return( bIsInputAFile ); }
		    	public void actionPerformed( ActionEvent e )
		    	{
		    		if ( e.getSource() == inputSourceFileRadioButton )
		    		{
		    			bIsInputAFile = true;
		    			inputSourceExpandingPanel.removeAll();
		    			inputNumeratorFilenameDescriptor = new JLabel( "Numerator" );
		    			inputSourceExpandingPanel.add( inputNumeratorFilenameDescriptor);
		    			inputNumerFileTextArea = new JTextArea( "" );
		    			inputNumerFileTextArea.setColumns( 10 );
		    			inputSourceExpandingPanel.add( inputNumerFileTextArea );
		    			inputNumerFileBrowseButton = new JButton( "Browse..." );
		    			inputSourceExpandingPanel.add( inputNumerFileBrowseButton );
		    			
		    			inputDenomFilenameDescriptor = new JLabel( "Denominator" );
		    			inputSourceExpandingPanel.add( inputDenomFilenameDescriptor );
		    			inputDenomFileTextArea = new JTextArea( "" );
		    			inputDenomFileTextArea.setColumns( 10 );
		    			inputSourceExpandingPanel.add( inputDenomFileTextArea );
		    			inputDenomFileBrowseButton = new JButton( "Browse..." );
		    			inputSourceExpandingPanel.add( inputDenomFileBrowseButton );
		    			
		    			inputNumerFileBrowseButton.addActionListener( this );
		    			inputDenomFileBrowseButton.addActionListener( this );
		    		}
		    		else if ( e.getSource() == inputSourceCriteriaRadioButton )
		    		{
		    			bIsInputAFile = false;
		    			inputSourceExpandingPanel.removeAll();
		    			String [] criteriaSet = getCriteriaSets( Cytoscape.getCurrentNetwork(), debugWindow );
		    			String [] criteria = {};
		    			inputSourceExpandingPanel.add( new JLabel( "Criteria set" ) );
		    			inputCriteriaSetComboBox = new JComboBox();
		    			if ( criteriaSet.length > 0 )
		    			{
		    				inputCriteriaSetComboBox.addItem( "" );
		    			}
		    			for ( String c  : criteriaSet )		    			
		    			{
		    				inputCriteriaSetComboBox.addItem( c );
		    			}
		    			inputSourceExpandingPanel.add( inputCriteriaSetComboBox );
		    			inputCriteriaComboBox = new JComboBox( criteria );
		    			inputSourceExpandingPanel.add( new JLabel( "Criteria" ) );
		    			inputSourceExpandingPanel.add( inputCriteriaComboBox );
		    			inputCriteriaSetComboBox.addActionListener( this );
		    			
		    			
		    		}
		    		else if ( e.getSource() == inputNumerFileBrowseButton || e.getSource() == inputDenomFileBrowseButton )
		    		{
		    			 JFileChooser chooser = new JFileChooser();
		    			 int returnVal = chooser.showOpenDialog( panel );
		    			 
		    			 if( returnVal == JFileChooser.APPROVE_OPTION ) 
		    			 {
		    				 try
		    				 {
			    				 if ( e.getSource() == inputNumerFileBrowseButton )
			    				 {
			    					 String filePath = chooser.getSelectedFile().getCanonicalPath();
			    					 inputNumerFileTextArea.setText( filePath );
			    					 inputNumerFileTextArea.setColumns( 10 );
				                     inputNumeratorFilenameDescriptor.setText( "Numerator: (" + countLinesInFile( filePath ) + ")" );
				                     inputNumeratorFilenameDescriptor.setAlignmentX( Component.CENTER_ALIGNMENT );
				 		    		   
			    				 }
			    				 else
			    				 {
			    					 String filePath = chooser.getSelectedFile().getCanonicalPath(); 
			    					 inputDenomFileTextArea.setText( filePath );
			    					 inputDenomFileTextArea.setColumns( 10 );
			    					 inputDenomFilenameDescriptor.setText( "Denominator: (" + countLinesInFile( filePath ) + ")" );
				                     inputDenomFilenameDescriptor.setAlignmentX( Component.CENTER_ALIGNMENT );
			    				 }
		    				 }
		    				 catch( java.io.IOException exception )
		    				 {
		    			
		    				 }
		    			 }	    			 
                         pack();
		    		}
		    		else if ( e.getSource() == inputCriteriaSetComboBox )
		    		{
		    			// based on selection, update criteria choices
		    			String [] newCriteria = getCriteria( (String) inputCriteriaSetComboBox.getSelectedItem(), Cytoscape.getCurrentNetwork(), debugWindow );
		    			inputCriteriaComboBox.removeAllItems();
		    			inputCriteriaComboBox.addItem( criteriaAllStringValue );
		    			for ( int i = 0; i < newCriteria.length; i++ )
		    			{
		    				inputCriteriaComboBox.addItem( newCriteria[ i ] );
		    			}
		    		}
		    		else if ( e.getSource() == inputCriteriaComboBox )
		    		{
		    			
		    		}
		    		panel.revalidate();
		    	}
		    };
		    inputSourceFileRadioButton.addActionListener( inputSourceActionListener );
		    inputSourceCriteriaRadioButton.addActionListener( inputSourceActionListener );
		    inputSourceFileRadioButton.setSelected( true );
		    
		    // invoke action to update panels
		    inputSourceActionListener.actionPerformed( new ActionEvent( inputSourceFileRadioButton, 0, null ) );
		    panel.add( inputSourcePanel );

		    launchButton = new JButton( "Run analysis" );		    
		    panel.add( launchButton );
		    launchButton.addActionListener( this );

		    generateInputFileButton = new JButton( "Generate Input File" );
		    //panel.add( generateInputFileButton );
		    generateInputFileButton.addActionListener( this );
		    
		    debugWindow = new TextArea( "", 5, 40 );
		    //panel.add( debugWindow );
		    panel.setSize( 750, 2000 );
		    panel.setVisible( true );

		    add( panel );
		    
		}
    	String generateUniqueFileName( String filenameBase )
    	{
    		return( filenameBase );
    	}
		public void actionPerformed( ActionEvent evt_ )
		{
			debugWindow.append( "evt source: " + evt_.getSource() + "\n" );
			debugWindow.append( "launchButton: " + launchButton + "\n" );
			debugWindow.append( "genInputFileButton: " + generateInputFileButton + "\n" );
			
			if ( evt_.getSource() == generateInputFileButton ) 
			{
				try
				{
					debugWindow.append( "run generateInputFile...\n " );
				    generateInputFileFromNetworkCriteria( "c:\\dummy\\dummy.txt", "En", "dummy.txt", "test", "gal1", true, debugWindow );
				}
				catch( Exception e )
				{
				}
			}
			
			if ( evt_.getSource() != launchButton ){ debugWindow.append( "early return\n " ); return; }
			
			debugWindow.append( "launched a job request ");
			// The User just launched a job request
			
			
			
			// spawn worker thread, one per process
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
			    
			    // prepare input files
			    String geneListFilePath = "";
			    String denomFilePath = "";

			    if ( !bIsInputAFile )
			    {
			    	// criteriaSet/criteria were selected
			    	String pluginDir = PluginManager.getPluginManager().getPluginManageDirectory().getCanonicalPath();
			    	
			    	String selectedCriteriaSet = ( String ) inputCriteriaSetComboBox.getSelectedItem();
			    	String selectedCriteria = ( String ) inputCriteriaSetComboBox.getSelectedItem();
			    	String [] criteriaList = new String[] { selectedCriteria };
			    	
			    	if (((String)inputCriteriaComboBox.getSelectedItem()).compareTo( criteriaAllStringValue ) == 0)
			    	{
			    		debugWindow.append( "-all- found: get list of criteria ");
			    		// user chose "-all-" criteria: launch a series of jobs, one per criteria in the criteriaSet
			    		criteriaList = getCriteria( selectedCriteriaSet, Cytoscape.getCurrentNetwork(), debugWindow );
			    	}
			    	
			    	String systemCode = vGeneSystemCodes[ new Integer( layoutProperties.getValue( "gene_system" ) ).intValue() ] ; 
		    		for ( String criteria : criteriaList )
		    		{
		    			String geneListFileName = criteria + ".txt";
		    			
			    		debugWindow.append( "launching job for criteria: " + criteria );
					    // if criteria selected, generate files first
				    	geneListFilePath = generateUniqueFileName( pluginDir + "\\" + geneListFileName );
				    	    
					    debugWindow.append( "generating input numerator\n" );
				    	generateInputFileFromNetworkCriteria( geneListFilePath, systemCode, geneListFileName, 
				    		(String) selectedCriteriaSet, (String) criteria, 
							true, debugWindow );
				    	
				    	String denomFileName = criteria + "_denom.txt";
					    debugWindow.append( "generating input denominator\n" );
				    	denomFilePath = generateUniqueFileName( pluginDir + "\\" + denomFileName );
				    	generateInputFileFromNetworkCriteria( denomFilePath, systemCode, denomFileName,
				    			(String) selectedCriteriaSet, (String) criteria, 
								false, debugWindow );	
				    	
				    	launchJob( geneListFilePath, denomFilePath, launchJobInput );
		    		}				    								    	
			    }
			    else
			    {
			    	geneListFilePath = inputNumerFileTextArea.getText();
			    	denomFilePath = inputDenomFileTextArea.getText();
			    	
			    	launchJob( geneListFilePath, denomFilePath, launchJobInput );
			    	
			    }
			    debugWindow.append( "2>" + service  + "\n" );
			    
			}
	    	catch( Exception e )
	    	{
	    		System.out.println ( "Exception: " + e );
	    	}
		}
	    void launchJob( final String geneListFilePath, final String denomFilePath, final JobInputType launchJobInput  )
	    {    	
		    debugWindow.append( "launchJob start\n" );
		    SwingWorker<StatusOutputType, Void> worker = new SwingWorker<StatusOutputType, Void>() 
			{
				edu.sdsc.nbcr.opal.types.StatusOutputType status = null;
				JTabbedPane resultsParentPanel = null; 
				TextArea statusWindow = null; 

				public StatusOutputType doInBackground()
		    	{
					try
					{
				    // 
				    // *** Parse dialog box inputs
				    // Process the gene list file					    
				    
				    debugWindow.append( "numer file: " + geneListFilePath );
				    File geneListFile = new File( geneListFilePath ); 
				    InputFileType geneListOpalFile = new InputFileType();			    
					geneListOpalFile.setName( geneListFile.getName() );  	// extract the name portion of the full path
				    byte[] geneListFileBytes = getBytesFromFile( geneListFile );
				    geneListOpalFile.setContents( replaceCR( geneListFileBytes ) );
				    debugWindow.append( "2> geneListFileBytes ( total size ): " + geneListFileBytes.length + "\n" );
				    					    
				    debugWindow.append( "denom file: " + denomFilePath );
				    System.out.println( "file: " + denomFilePath );
					File denomFile = new File( denomFilePath ); 
					System.out.println( "2" );
				    InputFileType denomOpalFile = new InputFileType();	
				    System.out.println( denomFile.getName() );
					denomOpalFile.setName( denomFile.getName() );  	// extract the name portion of the full path
				    byte[] denomFileBytes = getBytesFromFile( denomFile );
				    debugWindow.append( "2> denomFileBytes ( total size ): " + denomFileBytes.length + "\n" );
				    denomOpalFile.setContents( replaceCR( denomFileBytes ) );

				    // **** Prepare results pane / status window
		    		statusWindow = new TextArea( "", 20, 80, TextArea.SCROLLBARS_VERTICAL_ONLY );
		    		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);

		    		if ( resultsMasterPanel == null )
		    		{
		    			resultsMasterPanel = new CloseableTabbedPane();
		    		}
		    		resultsParentPanel = new JTabbedPane();
		    		
		    		JPanel statusPanel = new JPanel();
		    		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS ) );
		    		JLabel jobUrlLabel = new JLabel( "Job Server Url:" );
		    		jobUrlLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
		    		
		    		statusPanel.add( jobUrlLabel );
		    		statusPanel.add( statusWindow );
		    		resultsParentPanel.addTab( "Status", statusPanel );
		    		resultsMasterPanel.addTab( geneListFilePath, resultsParentPanel );
		    		if ( !bResultsMasterPanelAlreadyAdded )
		    		{ 
		    			cytoPanel.add( "GO-Elite Results", resultsMasterPanel );
		    			bResultsMasterPanelAlreadyAdded = true;
		    		}
		    		// CytoPanel -> resultsMasterPanel "GO-Elite Results" -> 
		    		//   resultsParentPanel "Status", "Pathway", "GO"

		    		// *** Launch webservice
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
				    
				    // *** Wait for results, update running status
				    InputFileType[] list = {geneListOpalFile, denomOpalFile};
				    launchJobInput.setInputFile( list );
				    debugWindow.append( "launching job---" );
				    JobSubOutputType output = service.launchJob(launchJobInput);
				    debugWindow.append( "---job launched" );
								    
				    jobID = output.getJobID();
				    System.out.println( "Job: " + jobID );
				    debugWindow.append( "Job: " + jobID + "\n" );
				    statusWindow.append( "Job: " + jobID  + "\n" );
				    String serverUrl = "http://webservices.rbvi.ucsf.edu:8080/";
				    jobUrlLabel.setText( "Job Server Url: " + serverUrl + jobID );
				    debugWindow.append( "Job Server Url: " + serverUrl + jobID );
				    jobUrlLabel.repaint();
				    
				    // 8 is the code for completion
			    	while( status == null || 8 != status.getCode() )
			    	{
			    		Thread.sleep( 5000 );
			    		status = service.queryStatus( output.getJobID() );
					    debugWindow.append( "[" + status.getCode() + "] " + status.getMessage() + "\n" );
			    		System.out.println( "[" + status.getCode() + "] " + status.getMessage() ); 
			    		statusWindow.append( "[" + status.getCode() + "] " + status.getMessage() + "\n" );
			    		statusWindow.repaint();
			    	}
					}
					catch( Exception e )
					{
						
					}
			    	return( status );
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
							
							for ( int j = 0; j < logFileContents.size(); j++ )
							{
								statusWindow.append( logFileContents.elementAt( j ) + "\n" );
							}
							
					    	resultsParentPanel.addTab( "GO", new JScrollPane( GONameResultsTable ) );
							resultsParentPanel.addTab( "Pathway", new JScrollPane( pathwayResultsTable ) );
						
							
						} // if... end
			    	}
					catch( Exception e )
					{
						System.out.println( "Exception " + e );
					}  // try...catch...end		
					
			    } // done() end
			};

			debugWindow.append( "executing worker" );
			worker.execute(); 
			debugWindow.append( "done executing worker" );
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
				  //CyGOEliteClient client = new CyGOEliteClient( plugin );
				  //WebServiceClientManager.registerClient( client );
				  
				  // pop up dialog
				  GOEliteInputDialog dialog = new GOEliteInputDialog();
				  dialog.setSize( new Dimension( 350, 500 ) );
				  dialog.setVisible( true );
					FileWriter fw = new FileWriter( "c:\\dummy\\debug.txt", false );
					PrintWriter out = new PrintWriter( fw );
					out.write( "YaY");
					out.println();
					out.close();
		      }
		      catch( Exception e )
		      {
		    	  System.out.println( "Exception: " + e );
		      }
		      
		}		  
	}

}

