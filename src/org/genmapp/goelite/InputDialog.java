package org.genmapp.goelite;

import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cytoscape.CytoscapeInit;
import cytoscape.command.CyCommandManager;
import cytoscape.data.CyAttributes;
import cytoscape.generated.Child;
import cytoscape.generated.Network;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import cytoscape.command.CyCommandResult;
import cytoscape.command.CyCommandException;

import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;
import giny.model.Node;

import java.util.ArrayList;

/* This is the actual JDialog subclass that presents the main user interface.  We use
 *   CardLayout because of the three distinct views that need to be instantly switchable
 *   ( CardLayout seems tailor-made for this )
 *   
 *   The design is to have most of the UI stuff here, but the heavy lifting is actually
 *      done by InputDialogWorker.   The Worker thread, which is (mostly) thread-safe, allows for 
 *      multiple GOElite tasks to work at the same time ( though there may still be some intermittent issues ).
 *         
 *   The code here mostly deals with window-dressing/UI except for the method launchJob()
 *      which creates InputDialogWorkers and executes them simultaneously.
 */

/**
 * In terms of design, layoutProperites is created in the GOElitePlugin and
 * passed into this class so that the GOEliteCommandListener can tap into the
 * object and infer what arguments to receive from the Tunable list; at that
 * point in time, this InputDialog class does need to be instantiated yet.
 * LayoutProperties contains the arguments selected by the user.
 * 
 */
public class InputDialog extends JDialog implements ActionListener {
	static CloseableTabbedPane resultsMasterPanel = null;

	enum InputDataType { FILE, CRITERIA, NETWORK };
	
	InputDataType mode = InputDataType.CRITERIA;
	JTextField fileNumerFilename, fileDenomFilename;
	JLabel fileNumerFileDescriptor, fileDenomFileDescriptor;
	JLabel criteriaNumerFileDescriptor, criteriaDenomFileDescriptor;
	JLabel networkInputNumeratorLabel, networkInputDenominatorLabel;
    JPanel cards; //a panel that uses CardLayout    
    JPanel fileInputSelectPanel, networkInputSelectPanel, criteriaInputSelectPanel;
    JButton fileInputNumerBrowseButton = null, fileInputDenomBrowseButton = null;
    JComboBox fileSpeciesToAnalyzeComboBox, criteriaSpeciesToAnalyzeComboBox, networkSpeciesToAnalyzeComboBox;
    JComboBox criteriaPrimaryIDColumnComboBox, networkPrimaryIDColumnComboBox; 
    JComboBox criteriaInputCriteriaSetComboBox, criteriaInputCriteriaComboBox;
    JComboBox networkInputNumeratorComboBox, networkInputDenominatorComboBox;
    JComboBox criteriaPrimaryIDSystemComboBox, networkPrimaryIDSystemComboBox, fileMODIDSystemComboBox;
	JButton runButton = null, cancelButton = null;
    Box fileNumerPanel = null, fileDenomPanel = null;

    String[] networkInputNumerators = { "selected nodes" };
    String[] networkInputDenominators = { "" };
    static synchronized JTabbedPane getResultsMasterPanel() { return( resultsMasterPanel ); }
    static synchronized JTabbedPane getResultsAnalysisNamePanel() { return( resultsAnalysisNamePanel ); }

    public InputDialog()
    {
    	setTitle( "GO-Elite" );
    	JDialog pane = this;
    	CyLogger.getLogger().debug( "addComponentToPane 1" );   	
    	
    	inputDataTypeButtonGroup = new ButtonGroup();

		String defaultSpecies = CytoscapeInit.getProperties().getProperty(
		"defaultSpeciesName");

		String bridgedbSpecieslist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";
		Vector<String> lines = null;
		try {
			lines = Utilities.getFileContents(new URL(bridgedbSpecieslist));
		} catch (java.net.MalformedURLException e) {
			Utilities.showError("Malformed URL in species list", e);
			return;
		
		} catch (java.io.IOException e) { 
			Utilities.showError("Could not retrieve species list from URL", e);
			return;
		}

		String defaultLatinNameSpecies = CytoscapeInit.getProperties().getProperty("defaultSpeciesName");
				   
    	int idx = 0;
		int defaultSpeciesIdx = 0;
		ArrayList<String> arrayListSpecies = new ArrayList<String>();
		ArrayList<String> arrayListSpeciesLatin = new ArrayList<String>();
		for (String line : lines) {
			String[] s = line.split("\t");
		
			// format: genus \t species \t common \t two-letter
			String latinName = (s[0] + " " + s[1]);
			if ( defaultLatinNameSpecies.equals( latinName ) )
			{
				defaultSpeciesIdx = idx;
			}
			String twoLetterCode = s[3];
		
			if (latinName.equals(defaultSpecies)) {
				defaultSpeciesIdx = idx;
			}
		
			arrayListSpecies.add(twoLetterCode);
			arrayListSpeciesLatin.add( latinName );
			idx++;
		}
		vSpecies = arrayListSpecies.toArray(vSpecies);
		vSpeciesLatin = arrayListSpeciesLatin.toArray(vSpeciesLatin);
		
    	CyLogger.getLogger().debug( "addComponentToPane 18" );
    	
		CyLogger.getLogger().debug( "getting supported gene systems for current species" );
		Set<String> idTypes = new HashSet<String>();
		Map<String, Object> args = new HashMap<String, Object>();
		
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping", "get source id types", args);
			 idTypes = (Set<String>) result.getResult();
			 CyLogger.getLogger().debug("cycommand returned with result: " + idTypes );
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CyLogger.getLogger().error( "CyCommandException: " + e );
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CyLogger.getLogger().error( "CyCommandException: " + e );			
		}
		ArrayList< String > geneSystems = new ArrayList< String >( idTypes );		
		
    	// grab supported gene systems from master list --- this is for all species so the vGeneSystems array
		//     will link to a subset of this
    	String geneSystemList = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/datasources.txt";
    	lines = null;
		try 
		{
			lines = Utilities.getFileContents(new URL(geneSystemList));
		} catch (java.net.MalformedURLException e) {
			Utilities.showError("Malformed URL in species list", e);
			return;
		
		} catch (java.io.IOException e) { 
			Utilities.showError("Could not retrieve species list from URL", e);
			return;
		
		}
    	CyLogger.getLogger().debug( "addComponentToPane 19" );

		
		ArrayList< String > systemCodes = new ArrayList< String >();
		for ( int i = 0; i < geneSystems.size(); i++ )
		{
			systemCodes.add( null );
		}
		for (String line : lines) 
		{
			String[] s = line.split("\t");

			// format: <gene_system_name>\t<gene_system_id>]\t......\n";
			for( int i = 0; i < geneSystems.size(); i++ )
			{
				if ( geneSystems.get( i ).equals( s[ 0 ] ) )
				{
					if ( geneSystems.get( i ).contains( "Ensembl" ) )
					{
						CyLogger.getLogger().debug( "geneSystem " + geneSystems.get( i ) + " -> En" );
						systemCodes.set( i, "En" );
					}
					else
					{
					  systemCodes.set( i, s[ 1 ] );					
					}
				}
			}
		}
		for( int i = 0; i < systemCodes.size(); i++ )
		{
			if ( systemCodes.get( i )  == null )
			{
				geneSystems.remove( i );
				systemCodes.remove( i );
				i--;
			}
		}
		// at this point, you have geneSystems and systemCodes as parallel arraylists, and only
		//    those geneSystems that have matching systemCodes are allowed to exist
		
    	CyLogger.getLogger().debug( "addComponentToPane 20" );
		
		
    	// add two special cases
		geneSystems.add( "<gene symbol>" );
		geneSystems.add( "???" );
		
		systemCodes.add( "Sy" );
		systemCodes.add( "--" );
		
		vGeneSystems = geneSystems.toArray( vGeneSystems );
		vGeneSystemCodes = systemCodes.toArray( vGeneSystemCodes );
    	CyLogger.getLogger().debug( "addComponentToPane 21" );

		
		String systemCodeInData = Cytoscape.getNetworkAttributes().getStringAttribute( 
				Cytoscape.getCurrentNetwork().getIdentifier(),	"SystemCode" );
		int systemCodeDefaultIdx = 0;
		if ( systemCodeInData != null )
		{
			for( int ii =0; ii < vGeneSystems.length; ii++ )
			{
				if ( vGeneSystemCodes[ ii ].equals( systemCodeInData ) )
				{
					systemCodeDefaultIdx = ii;
				}
			}
		}

    	CyLogger.getLogger().debug( "addComponentToPane 22" );

    	
    	// Top-level button group for switching modes
    	JPanel modeSelectionPanel = new JPanel(); //use FlowLayout
        modeSelectionPanel.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "", 
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION ) );
       
        addRadioButton( InputDataType.CRITERIA, modeSelectionPanel );
        addRadioButton( InputDataType.NETWORK, modeSelectionPanel );
        addRadioButton( InputDataType.FILE, modeSelectionPanel );
        
    	CyLogger.getLogger().debug( "addComponentToPane 2" );
        
        // Create the "cards".
        
        // ****  create file card *****
        JPanel fileCard = new JPanel();
        fileCard.setLayout( new BoxLayout( fileCard, BoxLayout.Y_AXIS ) );

        // init data-holding components
        fileSpeciesToAnalyzeComboBox = new JComboBox( vSpeciesLatin );
        fileSpeciesToAnalyzeComboBox.setSelectedIndex( defaultSpeciesIdx );
		
        fileMODIDSystemComboBox = new JComboBox( vMODIDSystems );        
        
        Box fileSpeciesParamBox = Box.createHorizontalBox();
        fileSpeciesParamBox.add( new JLabel( "Species to analyze" ) );
        fileSpeciesParamBox.add( Box.createHorizontalGlue() );        
        fileSpeciesParamBox.add( fileSpeciesToAnalyzeComboBox );
        fileSpeciesToAnalyzeComboBox.setMaximumSize( new Dimension( 30, 30 ) );
        
        Box fileMODIDSystemBox = Box.createHorizontalBox();
        fileMODIDSystemBox.add( new JLabel( "Mod ID System") );
        fileMODIDSystemBox.add( Box.createHorizontalGlue() );
        fileMODIDSystemBox.add( fileMODIDSystemComboBox );

        // input panel
        fileInputSelectPanel = new JPanel();
        fileInputSelectPanel.setLayout( new BoxLayout( fileInputSelectPanel, BoxLayout.Y_AXIS ));
        fileInputSelectPanel.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "Input",
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION) );
        
        fileNumerPanel = Box.createHorizontalBox();
        fileDenomPanel = Box.createHorizontalBox();
        fileNumerFilename = new JTextField( 20 );
        fileDenomFilename = new JTextField( 20 );
        
        fileNumerFileDescriptor = new JLabel( "Numerator ( 0 )" );
        fileNumerPanel.add(  fileNumerFileDescriptor );
        fileNumerPanel.add( Box.createHorizontalGlue() );
        fileNumerPanel.add( fileNumerFilename );
        fileInputNumerBrowseButton = new JButton( "Browse..." );
        fileNumerPanel.add( fileInputNumerBrowseButton );
    
        fileDenomFileDescriptor = new JLabel( "Denominator ( 0 )" );
        fileDenomPanel.add(  fileDenomFileDescriptor );
        fileNumerPanel.add( Box.createHorizontalGlue() );        
        fileDenomPanel.add( fileDenomFilename );
        fileInputDenomBrowseButton = new JButton( "Browse..." );
        fileDenomPanel.add( fileInputDenomBrowseButton );
        
 
        fileInputSelectPanel.add( fileSpeciesParamBox );
        fileInputSelectPanel.add( fileMODIDSystemBox );
        fileInputSelectPanel.add( fileNumerPanel );
        fileInputSelectPanel.add( fileDenomPanel );        

        // add the panels to the card
        fileCard.add( fileInputSelectPanel );
    	CyLogger.getLogger().debug( "addComponentToPane 3" );

        
        // ****  create criteria card *****
        JPanel criteriaCard = new JPanel();
        criteriaCard.setLayout( new BoxLayout( criteriaCard, BoxLayout.Y_AXIS ) );

        // init data-holding components
        criteriaSpeciesToAnalyzeComboBox = new JComboBox( vSpeciesLatin );
        criteriaSpeciesToAnalyzeComboBox.setSelectedIndex( defaultSpeciesIdx );
        criteriaPrimaryIDSystemComboBox = new JComboBox( vGeneSystems );
        criteriaInputCriteriaSetComboBox = new JComboBox();
		criteriaPrimaryIDColumnComboBox = new JComboBox();
        criteriaInputCriteriaComboBox = new JComboBox();
        criteriaInputCriteriaComboBox.addItem( criteriaAllStringValue );
        
        CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		String[] attributeNames = nodeAttributes
				.getAttributeNames();
		JLabel criteriaPrimaryIDColumnLabel = new JLabel(
				"Primary ID Column:");
        criteriaPrimaryIDColumnComboBox.addItem( "ID" );

		for (String n : attributeNames) {
			if ( CyAttributes.TYPE_STRING == nodeAttributes
					.getType(n) && nodeAttributes.getUserVisible( n ) ) {
				criteriaPrimaryIDColumnComboBox.addItem(n);
			}
		}
		
    	CyLogger.getLogger().debug( "addComponentToPane 4" );
		String[] criteriaSet = GOElitePlugin
				.getCriteriaSets();
    	CyLogger.getLogger().debug( "addComponentToPane 4.1" );

		String[] criteria = {};
    	CyLogger.getLogger().debug( "addComponentToPane 4.2" );

		for (String c : criteriaSet) {
			criteriaInputCriteriaSetComboBox.addItem(c);
		}
		if ( criteriaInputCriteriaSetComboBox.getItemCount() > 0 )
		{
			criteriaInputCriteriaSetComboBox.setSelectedIndex(0);
		}
		CyLogger.getLogger().debug( "addComponentToPane 4.3" );
    	if ( null != criteriaInputCriteriaSetComboBox.getSelectedItem() )
    	{
    		CyLogger.getLogger().debug( "addComponentToPane 4.3.1" );
        		String cs = ( String ) criteriaInputCriteriaSetComboBox.getSelectedItem();
        		CyLogger.getLogger().debug( "addComponentToPane 4.3.2" );
            if ( cs.length() > 0 )
            {
    		for( String c : GOElitePlugin.getCriteria( cs ) )
    		{   
    			criteriaInputCriteriaComboBox.addItem( c );
    		}
            }
    	}
    	CyLogger.getLogger().debug( "addComponentToPane 4.4" );

		criteriaPrimaryIDColumnComboBox .setSelectedIndex(0);
    	//CyLogger.getLogger().debug( "addComponentToPane 4.5" );


    	CyLogger.getLogger().debug( "addComponentToPane 5" );
        // params panel
        Box criteriaParamsBox = Box.createVerticalBox();
        criteriaParamsBox.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "Params",
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION) );
        

        Box criteriaPrimaryIDParamBox = Box.createHorizontalBox();
        criteriaPrimaryIDParamBox.add( new JLabel( "Primary ID Column" ) );
        criteriaPrimaryIDParamBox.add( Box.createHorizontalGlue() );        
        criteriaPrimaryIDParamBox.add( criteriaPrimaryIDColumnComboBox );
        criteriaPrimaryIDParamBox.add( new JLabel( "System" ) );
        criteriaPrimaryIDParamBox.add( Box.createHorizontalGlue() );        
        criteriaPrimaryIDParamBox.add( criteriaPrimaryIDSystemComboBox );
        
        criteriaPrimaryIDSystemComboBox.setMaximumSize( new Dimension( 30, 30 ) );
        criteriaPrimaryIDColumnComboBox.setMaximumSize( new Dimension( 30, 30 ) );
        
       
        // input panel
    	CyLogger.getLogger().debug( "addComponentToPane 6" );

        criteriaInputSelectPanel = new JPanel();
        criteriaInputSelectPanel.setLayout( new BoxLayout( criteriaInputSelectPanel, BoxLayout.Y_AXIS ));
        criteriaInputSelectPanel.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "Input",
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION ) );
        
        Box criteriaNumerPanel = Box.createHorizontalBox();
        Box criteriaDenomPanel = Box.createHorizontalBox();
        
    	CyLogger.getLogger().debug( "addComponentToPane 7" );

    	criteriaNumerFileDescriptor = new JLabel( "Criteria Set" );
        criteriaNumerPanel.add( criteriaNumerFileDescriptor );
        criteriaNumerPanel.add( Box.createHorizontalGlue() );        
        criteriaNumerPanel.add( criteriaInputCriteriaSetComboBox );
    
        criteriaDenomFileDescriptor = new JLabel( "Criteria: ( - / - )" ) ;
        criteriaDenomPanel.add(  criteriaDenomFileDescriptor );
        criteriaDenomPanel.add( Box.createHorizontalGlue() );        
        criteriaDenomPanel.add( criteriaInputCriteriaComboBox );
                
        criteriaInputSelectPanel.add( criteriaPrimaryIDParamBox );
        criteriaInputSelectPanel.add( criteriaNumerPanel );
        criteriaInputSelectPanel.add( criteriaDenomPanel );        

    	CyLogger.getLogger().debug( "addComponentToPane 8" );

        // add the panels to the card
        criteriaCard.add( criteriaInputSelectPanel );
        
        
        // ****  create network card *****
        JPanel networkCard = new JPanel();
        networkCard.setLayout( new BoxLayout( networkCard, BoxLayout.Y_AXIS ) );

        networkSpeciesToAnalyzeComboBox = new JComboBox( vSpeciesLatin );
        networkSpeciesToAnalyzeComboBox.setSelectedIndex( defaultSpeciesIdx );
		
        networkPrimaryIDColumnComboBox = new JComboBox();
        networkPrimaryIDColumnComboBox.addItem( "ID" );
		for (String n : attributeNames) {
			if (CyAttributes.TYPE_STRING == nodeAttributes
					.getType(n) && nodeAttributes.getUserVisible( n ) ) 
			{
				networkPrimaryIDColumnComboBox.addItem(n);
			}
		}
		networkPrimaryIDColumnComboBox.setSelectedIndex(0);
		networkPrimaryIDColumnComboBox.setMaximumSize( new Dimension( 30, 30 ) );
        
    	CyLogger.getLogger().debug( "addComponentToPane 9" );

		String [] availableNetworks = new String[ Cytoscape.getNetworkSet().size() ];
		int i = 0;
		for ( CyNetwork n : Cytoscape.getNetworkSet() )
		{
			availableNetworks[ i ] = n.getTitle();
			i++;
		}
		
        // init data-holding components
        networkPrimaryIDSystemComboBox = new JComboBox( vGeneSystems );
        networkInputNumeratorComboBox = new JComboBox( networkInputNumerators );
        networkInputDenominatorComboBox = new JComboBox( availableNetworks );

        if ( Cytoscape.getCurrentNetwork() != null )
        {
        	CyLogger.getLogger().debug( "cytoscape.currentnetwork " + Cytoscape.getCurrentNetwork() + " " + Cytoscape.getCurrentNetwork().getTitle());
        	for( int j = 0; j < networkInputDenominatorComboBox.getItemCount(); j++ )
        	{
        		String x = ( String ) networkInputDenominatorComboBox.getItemAt( j );
        		if ( x.equals( Cytoscape.getCurrentNetwork().getTitle() ) )
        		{
        			networkInputDenominatorComboBox.setSelectedIndex( j );
        		}
        	}
        }
        // params panel
        Box networkSpeciesParamBox = Box.createHorizontalBox();
        networkSpeciesParamBox.add( new JLabel( "Species" ) );
        networkSpeciesParamBox.add( networkSpeciesToAnalyzeComboBox );
        
        Box networkPrimaryIDParamBox = Box.createHorizontalBox();
        networkPrimaryIDParamBox.add( new JLabel( "Primary ID Column" ) );
        networkPrimaryIDParamBox.add( Box.createHorizontalGlue() );
        networkPrimaryIDParamBox.add( networkPrimaryIDColumnComboBox );        
        networkPrimaryIDParamBox.add( new JLabel( "System" ) );
        networkPrimaryIDParamBox.add( Box.createHorizontalGlue() );
        networkPrimaryIDParamBox.add( networkPrimaryIDSystemComboBox );        

        networkPrimaryIDSystemComboBox.setMaximumSize( new Dimension( 30, 30 ) );

    	CyLogger.getLogger().debug( "addComponentToPane 10" );
             
        // input panel
    	
		Object selectedNetworkObj = getNetwork(  ( String ) 
				networkInputDenominatorComboBox.getSelectedItem() );
		if ( null != selectedNetworkObj )
		{
			networkInputDenominatorLabel = new JLabel( "Denominator: (" + 
					( ( CyNetwork ) selectedNetworkObj ).nodesList().size() + ")" );
			networkInputNumeratorLabel = new JLabel( "Numerator: (" +
					( ( CyNetwork ) selectedNetworkObj ).getSelectedNodes().size() + ")" );
		}
		else
		{
			networkInputDenominatorLabel = new JLabel( "Denominator: ( 0 )" );
			networkInputNumeratorLabel = new JLabel( "Numerator: ( 0 )" );
		}

    	networkInputSelectPanel = new JPanel();
        networkInputSelectPanel.setLayout( new BoxLayout( networkInputSelectPanel, BoxLayout.Y_AXIS ));
        networkInputSelectPanel.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "Input",
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION) );

        
        Box networkNumerPanel = Box.createHorizontalBox();
        Box networkDenomPanel = Box.createHorizontalBox();
        
        
        networkNumerPanel.add( networkInputNumeratorLabel );
        networkNumerPanel.add( Box.createHorizontalGlue() );        
        networkNumerPanel.add( networkInputNumeratorComboBox );
        networkInputNumeratorComboBox.setMaximumSize( new Dimension( 300, 30 ) );
 
				  

        
        networkDenomPanel.add(  networkInputDenominatorLabel );
        networkDenomPanel.add( Box.createHorizontalGlue() );        
        networkDenomPanel.add( networkInputDenominatorComboBox );
        networkInputDenominatorComboBox.setMaximumSize( new Dimension( 300, 30 ) );
                
        networkInputSelectPanel.add( networkSpeciesParamBox );
        networkInputSelectPanel.add( networkPrimaryIDParamBox );
        networkInputSelectPanel.add( networkNumerPanel );
        networkInputSelectPanel.add( networkDenomPanel );        

    	CyLogger.getLogger().debug( "addComponentToPane 11" );

        // add the panels to the card
        networkCard.add( networkInputSelectPanel );
                
        //Create the panel that contains the "cards".
        cards = new JPanel(new CardLayout());
        cards.add(fileCard, "File" );
        cards.add(criteriaCard, "Criteria" );
        cards.add(networkCard, "Network" );

        CyLogger.getLogger().debug( "addComponentToPane 12" );

        setMode( mode );
        
        runButton = new JButton( "Run" );
        cancelButton = new JButton( "Cancel" );
        JPanel menuButtonsBox = new JPanel();
        menuButtonsBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        menuButtonsBox.add( runButton );
        menuButtonsBox.add( cancelButton );
        
        pane.add(modeSelectionPanel, BorderLayout.PAGE_START);
        pane.add(cards, BorderLayout.CENTER);
        pane.add(menuButtonsBox, BorderLayout.SOUTH );
        
    	CyLogger.getLogger().debug( "addComponentToPane 13" );

	
		
        // actionlisteners
		ActionListener inputSourceActionListener = new ActionListener() 
		{

			public void actionPerformed(ActionEvent e) 
			{
				repaint();

				if ( e.getSource() == criteriaPrimaryIDColumnComboBox )
				{
					updatePrimaryIDSystemComboBoxes( ( String ) criteriaPrimaryIDColumnComboBox.getSelectedItem(), 
							criteriaPrimaryIDSystemComboBox, criteriaPrimaryIDColumnComboBox.getSelectedIndex() == 0  );
				}
				else if ( e.getSource() == networkPrimaryIDColumnComboBox )
				{
					updatePrimaryIDSystemComboBoxes( ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), 
							networkPrimaryIDSystemComboBox, networkPrimaryIDColumnComboBox.getSelectedIndex() == 0  );
				}
				
				if (e.getSource() == criteriaInputCriteriaSetComboBox ) 
				{
					String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox
						.getSelectedItem();
					String[] newCriteria = GOElitePlugin
						.getCriteria((String) criteriaInputCriteriaSetComboBox
							.getSelectedItem() );
					
					// JComboBox removeall() was acting up, so I create a dummy and swap models instead
					JComboBox dummy = new JComboBox();
					dummy.addItem( criteriaAllStringValue );
					
					for ( int i = 0; i < newCriteria.length; i++ )
					{
						dummy.addItem( newCriteria[ i ] );
					}
					
					criteriaInputCriteriaComboBox.setModel( dummy.getModel() );
				}
				if (e.getSource() == criteriaInputCriteriaSetComboBox
						|| e.getSource() == criteriaInputCriteriaComboBox) 
				{
					CyLogger.getLogger().debug( "criteria section");
					try 
					{
						// update the denominator label to count up number of
						// hits
						String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox
								.getSelectedItem();
						String selectedCriteria = (String) criteriaInputCriteriaComboBox
								.getSelectedItem();
	
						long numHits = 0;
						long numTotal = 0;
						if (((String) selectedCriteria)
								.equals(criteriaAllStringValue)) {
							criteriaDenomFileDescriptor.setText("Criteria: ( - / - )");
	
						} else {
							CyLogger.getLogger().debug( "criteria section 2");
							
							long[] nums = GOElitePlugin
									.generateInputFileFromNetworkCriteria(
											"",
											( String ) criteriaPrimaryIDSystemComboBox.getSelectedItem(),
											(String) selectedCriteriaSet,
											(String) criteriaInputCriteriaComboBox
													.getSelectedItem(),
											true,
											false,
											(String) criteriaPrimaryIDColumnComboBox
													.getSelectedItem(),
													0 == criteriaPrimaryIDColumnComboBox.getSelectedIndex()
											);
							numHits = nums[0];
							numTotal = nums[1];
							CyLogger.getLogger().debug( "numHits " + numHits + " numTotal " + numTotal );
							criteriaDenomFileDescriptor.setText("Criteria: ("
									+ numHits + "/" + numTotal + ") ");
						}

					} catch (java.io.IOException except) {
						Utilities
								.showError(
										"I/O Error: Couldn't generate temporary files for criteriaset/criteria.  Check disk space/permissions",
										except);
					}
				}
				else if ( e.getSource() == networkInputDenominatorComboBox )
				{
					CyLogger.getLogger().debug( "network denom update" );
					Object selectedNetworkObj = getNetwork(  ( String ) 
							networkInputDenominatorComboBox.getSelectedItem() );
							  
					if ( null != selectedNetworkObj )
					{
						CyLogger.getLogger().debug( "network denom update: " + ( ( CyNetwork ) selectedNetworkObj ).nodesList().size() + " nodes " );
						networkInputDenominatorLabel.setText(  "Denominator: (" + 
								( ( CyNetwork ) selectedNetworkObj ).nodesList().size() + ")" );
						networkInputNumeratorLabel.setText( "Numerator: (" +
								( ( CyNetwork ) selectedNetworkObj ).getSelectedNodes().size() + ")" );
					}
					else
					{
						networkInputDenominatorLabel.setText( "Denominator: ( 0 )" );
						networkInputNumeratorLabel.setText( "Numerator: ( 0 )" );
					}
				}
				else if ( e.getSource() == cancelButton )
				{
					// hide the window
					InputDialog.this.dispose();
					
				}
				else if ( e.getSource() == fileInputDenomBrowseButton || e.getSource() == fileInputNumerBrowseButton )
				{
					CyLogger.getLogger().debug( "file browse" );
					JFileChooser chooser = (null != browseButtonLastFileSelected
							? new JFileChooser(browseButtonLastFileSelected)
							: new JFileChooser());
					int returnVal = chooser.showOpenDialog( null );
	
					if (returnVal == JFileChooser.APPROVE_OPTION) 
					{
						CyLogger.getLogger().debug( "approved" );

						browseButtonLastFileSelected = chooser
								.getSelectedFile();
						try {
							if (e.getSource() == fileInputNumerBrowseButton ) {
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								CyLogger.getLogger().debug( "path: " + filePath );

								if (new File(filePath).exists()) {
									CyLogger.getLogger().debug( "exists" );

									fileNumerFilename.setText(filePath);
									fileNumerFileDescriptor
											.setText("Numerator: ("
													+ ( Utilities.countLinesInFile(filePath) - 1 )
													+ ")");									

								}
							} 
							else 
							{
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								if (new File(filePath).exists()) {
									fileDenomFilename.setText(filePath);
									fileDenomFileDescriptor
											.setText("Denominator: ("
													+ ( Utilities.countLinesInFile(filePath) - 1 )
													+ ")");
								}
							}
						} 
						catch (java.io.IOException e2 ) 
						{
							CyLogger.getLogger().error( "Exception: " + e + stack2string( e2 ) );
						} 
					}
				}
				else if ( e.getSource() == runButton )
				{		
					// sanity checks before running
					if ( InputDataType.CRITERIA == mode )
					{
						// ensure a primary id system is selected
						if ( null == criteriaPrimaryIDSystemComboBox.getSelectedItem()  || ( ( String ) criteriaPrimaryIDSystemComboBox.getSelectedItem() ).equals( "???" ) )
						{
							CyLogger.getLogger().error( "Please select a primary ID system" );
							return;
						}
						if ( null == criteriaInputCriteriaComboBox.getSelectedItem()  || ( ( String ) criteriaInputCriteriaComboBox.getSelectedItem() ).equals( "" ) ) 
						{
							CyLogger.getLogger().error( "Please select a valid crtieria" );
							return;
						}
						if ( null == criteriaInputCriteriaSetComboBox.getSelectedItem() || ( ( String ) criteriaInputCriteriaSetComboBox.getSelectedItem() ).equals( "" ) )
						{
							CyLogger.getLogger().error( "Please select a valid crtieriaset" );
							return;
						}
					}
					else if ( InputDataType.NETWORK == mode )
					{
						// ensure a primary id system is selected
						if ( null == networkPrimaryIDSystemComboBox.getSelectedItem()  || ( ( String ) networkPrimaryIDSystemComboBox.getSelectedItem() ).equals( "???" ) )
						{
							CyLogger.getLogger().error( "Please select a primary ID system" );
							return;
						}
					}
					
					// user hit "Run analysis button" 
					InputDialog.this.dispose();
							
					String pluginDir = null;
					try 
					{			
						if ( InputDataType.NETWORK == mode || InputDataType.CRITERIA == mode ) {
				
							pluginDir = PluginManager.getPluginManager()
									.getPluginManageDirectory().getCanonicalPath()
									+ "/GOEliteData";
							boolean pluginDirExists = new File(pluginDir).exists();
							if (!pluginDirExists) {
			
								boolean success = new File(pluginDir).mkdir();
								if (!success) {
									CyLogger.getLogger().error( "couldn't create plugin dir " + pluginDir );
								}
							}
										
							if ( InputDataType.CRITERIA == mode )
							{
								String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox.getSelectedItem();
								String selectedCriteria = (String) criteriaInputCriteriaComboBox.getSelectedItem();
								String[] criteriaList = new String[]{selectedCriteria};
								String systemCode = vGeneSystemCodes[ criteriaPrimaryIDSystemComboBox.getSelectedIndex() ];
								 
								if (((String) criteriaInputCriteriaComboBox.getSelectedItem())
										.compareTo(criteriaAllStringValue) == 0) {
									// user chose "-all-" criteria: launch a series of jobs, one
									// per criteria in the criteriaSet
									criteriaList = GOElitePlugin.getCriteria(selectedCriteriaSet);
								}
			
								for (String criteria : criteriaList) {
									String geneListFileName = criteria;
									String geneListFilePath = pluginDir + "/" + geneListFileName;
									geneListFilePath = Utilities.generateUniqueFilename(geneListFilePath);
									CyLogger.getLogger().debug( "geneListFileName " + geneListFileName );
									CyLogger.getLogger().debug( "geneListFilePath " + geneListFilePath );
									// if criteria selected, generate files first
				
									GOElitePlugin.generateInputFileFromNetworkCriteria(
											geneListFilePath, systemCode,
											(String) selectedCriteriaSet, (String) criteria,
											true, true,
											(String) criteriaPrimaryIDColumnComboBox
													.getSelectedItem(), 
											0 == criteriaPrimaryIDColumnComboBox.getSelectedIndex() );
				
									String denomFileName = criteria + "_denom";
									String denomFilePath = pluginDir + "/" + denomFileName;
									denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
											+ denomFileName + ".txt");
									GOElitePlugin.generateInputFileFromNetworkCriteria(
											denomFilePath, systemCode,
											(String) selectedCriteriaSet, (String) criteria,
											false, true,
											(String) criteriaPrimaryIDColumnComboBox
													.getSelectedItem(), 
											0 == criteriaPrimaryIDColumnComboBox.getSelectedIndex());
				
									launchJob(geneListFilePath, denomFilePath);
								}
							} 
							else if ( InputDataType.NETWORK == mode )
							{
								
								String systemCode = vGeneSystemCodes[ networkPrimaryIDSystemComboBox.getSelectedIndex() ];
			
								// this is the network the user selected as the denominator
								String networkTitle = ( String ) networkInputDenominatorComboBox .getSelectedItem();
								String networkID = getNetwork( networkTitle ).getIdentifier();
								CyNetwork network = Cytoscape.getNetwork( networkID );
			
								String geneListFileName = network.getTitle();
								String geneListFilePath = pluginDir + "/" + geneListFileName;
								geneListFilePath = Utilities.generateUniqueFilename(geneListFilePath);
								// if criteria selected, generate files first
								
								Set< Node > numeratorNodes = new java.util.HashSet<Node>();
								CyLogger.getLogger().debug( "3" );
								// get the numerator nodes: right now, we only support "selected nodes"
								Set selectedNodes = network.getSelectedNodes();
								CyLogger.getLogger().debug( "4" );
			
								numeratorNodes = selectedNodes;
								CyLogger.getLogger().debug( "5" );
			
								GOElitePlugin.generateInputFileFromNodeSet(
										geneListFilePath, systemCode,
										numeratorNodes,
										true, ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), 
										0 == networkPrimaryIDColumnComboBox.getSelectedIndex() );
								CyLogger.getLogger().debug( "6" );
			
								
								// for denominator, use all nodes in the selected network
								Set< Node > denomNodes = new java.util.HashSet<Node>( network.nodesList() );
								String denomFileName =  "network_denom";
								String denomFilePath = pluginDir + ")/" + denomFileName;
								denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
										+ denomFileName);
								GOElitePlugin.generateInputFileFromNodeSet(
										denomFilePath, systemCode,
										denomNodes,
										true, ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(),
										0 == networkPrimaryIDColumnComboBox.getSelectedIndex() );
								launchJob(geneListFilePath, denomFilePath);
							}		
						}
						else 
						{
							String geneListFilePath = fileNumerFilename.getText();
							String denomFilePath = fileDenomFilename.getText();
			
							launchJob(geneListFilePath, denomFilePath);
			
						}

					} catch (IOException except ) {
						CyLogger.getLogger().error( except.toString() );
						if (pluginDir == null || pluginDir.length() == 0) {
							Utilities
									.showError(
											"Could not get canonical path from plugin manager directory",
											except );
						} else {
							Utilities.showError(
									"Could not generate input files from network criteria:"
											+ except.getMessage(), except );
						}
					}
				}
			}
		};

    	CyLogger.getLogger().debug( "addComponentToPane 15" );

    	CyLogger.getLogger().debug( "addComponentToPane 16" );

		
    	CyLogger.getLogger().debug( "addComponentToPane 17" );

    	CyLogger.getLogger().debug( "addComponentToPane 19" );

		criteriaPrimaryIDSystemComboBox.setSelectedIndex( systemCodeDefaultIdx );
		networkPrimaryIDSystemComboBox.setSelectedIndex( systemCodeDefaultIdx );
		updatePrimaryIDSystemComboBoxes( ( String ) criteriaPrimaryIDColumnComboBox.getSelectedItem(), 
				criteriaPrimaryIDSystemComboBox, criteriaPrimaryIDColumnComboBox.getSelectedIndex() == 0  );
		updatePrimaryIDSystemComboBoxes( ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), 
				networkPrimaryIDSystemComboBox, networkPrimaryIDColumnComboBox.getSelectedIndex() == 0 );
		
		// Add action listeners
		criteriaInputCriteriaSetComboBox.addActionListener( inputSourceActionListener );
		criteriaInputCriteriaComboBox.addActionListener( inputSourceActionListener );
		cancelButton.addActionListener( inputSourceActionListener );
		fileInputDenomBrowseButton.addActionListener( inputSourceActionListener );
		fileInputNumerBrowseButton.addActionListener( inputSourceActionListener );
		runButton.addActionListener( inputSourceActionListener );
		criteriaPrimaryIDColumnComboBox.addActionListener( inputSourceActionListener );
		networkPrimaryIDColumnComboBox.addActionListener( inputSourceActionListener );
		networkInputDenominatorComboBox.addActionListener( inputSourceActionListener );
		
    	CyLogger.getLogger().debug( "addComponentToPane 20" );

		pane.setVisible(true);
    	CyLogger.getLogger().debug( "addComponentToPane end" );

    }

    
    public String getSelectedModGeneSystem()
    {
	    String modGeneSystemSelected = "Ensembl";
		if ( mode == InputDataType.NETWORK )
		{
			CyLogger.getLogger().debug( "networkPrimaryIDSystemComboBox: " + ( String ) networkPrimaryIDSystemComboBox.getSelectedItem() );
			if ( ( ( String ) networkPrimaryIDSystemComboBox.getSelectedItem() ).equals( "Entrez Gene" ) )
			{
				CyLogger.getLogger().debug( "EntrezGene" );
				modGeneSystemSelected = "EntrezGene";
			}
			CyLogger.getLogger().debug( "Ensembl" );
		}
		else if ( mode == InputDataType.CRITERIA )
		{
			if ( ( ( String ) criteriaPrimaryIDSystemComboBox.getSelectedItem() ).equals( "Entrez Gene" ) )
			{
				modGeneSystemSelected = "EntrezGene";
			}
		}
		else if ( mode == InputDataType.FILE )
		{
			modGeneSystemSelected = ( String ) fileMODIDSystemComboBox.getSelectedItem();
		}
		return( modGeneSystemSelected );
	}

    public CyNetwork getNetwork( String title )
    {
		for ( CyNetwork n : Cytoscape.getNetworkSet() )
		{
			if ( n.getTitle().equals( title ) )
			{
				return( n );
			}
		}
		return( null );
    }
    public String getModeString( InputDataType newMode )
    {
    	String modeLabel = "";
	  	if ( newMode == InputDataType.FILE ) { modeLabel = "File"; }
	  	else if ( newMode == InputDataType.CRITERIA ) { modeLabel = "Criteria"; }
	  	else if ( newMode == InputDataType.NETWORK ) { modeLabel = "Network"; }
	
	  	return( modeLabel );
    }
    public void setMode( InputDataType newMode )
    {	
    	
		CardLayout cl = (CardLayout)(cards.getLayout());
        cl.show(cards, (String)getModeString( newMode ) );
        mode = newMode;
    }

   /**
    * Adds a radio button that sets the font size of the sample text.
    * @param name the string to appear on the button
    * @param size the font size that this button sets
    */
   public void addRadioButton(final InputDataType modeToAdd, Container  pane )
   {
	  String modeLabel = getModeString( modeToAdd );
	  
      JRadioButton button = new JRadioButton( modeLabel, true );
      inputDataTypeButtonGroup.add(button);
      pane.add(button);

      // this listener sets the label font size

      ActionListener listener = new ActionListener()
         {
            public void actionPerformed(ActionEvent event)
            {
               // size refers to the final parameter of the addRadioButton
               // method
               setMode( modeToAdd );
            }
         };

      button.addActionListener(listener);
   }
   public static final int DEFAULT_WIDTH = 400;
   public static final int DEFAULT_HEIGHT = 200;

   private JPanel buttonPanel;
   private ButtonGroup inputDataTypeButtonGroup;
   private JLabel label;

   
   
    
	
	String criteriaAllStringValue = "-all-"; // selection value for the criteria
	// "all"
	JButton launchButton = null;
	JButton generateInputFileButton = null;
	File browseButtonLastFileSelected = null;
	public final static long serialVersionUID = 0;	
	// prepare input files
	static JTabbedPane resultsAnalysisNamePanel = null;
	
	
	public static String stack2string(Exception e) 
	{
		  try {
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    return "------\r\n" + sw.toString() + "------\r\n";
		  }
		  catch(Exception e2) 
		  {
		    return( "bad stack2string" );
		  }
	}
	@SuppressWarnings("unchecked")
	public Set<String> guessIdType( String sampleID )
	{
		CyLogger.getLogger().debug( "guessIDType: " + sampleID );
		Set<String> idTypes = new HashSet<String>();
		if ( sampleID == null ) { return idTypes; }
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", sampleID );
		
		try {
			CyLogger.getLogger().debug( "calling cycommand ipdmapping.guessidtype with args: " + args );
			CyCommandResult result = CyCommandManager.execute("idmapping", "guess id type", args);
			 idTypes = (Set<String>) result.getResult();
			 CyLogger.getLogger().debug("cycommand returned with result: " + idTypes );
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CyLogger.getLogger().debug( "CyCommandException: " + e );
			return null;
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CyLogger.getLogger().debug( "CyCommandException: " + e );
			return null;
		}
		
		return idTypes;
	}

	// bUseCytoscapeID - if true, then the primaryIDCol param does not refer to a nodeAttribute
	//                   but rather the cytoscape ID
	private void updatePrimaryIDSystemComboBoxes( String primaryIDCol, JComboBox comboBox,
			boolean bUseCytoscapeID ) 
	{
		CyLogger.getLogger().debug( "updatePrimaryIDSystemComboBoxes " + primaryIDCol + " " + comboBox );
		
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();		
		String sampleID = "";
		Node sampleNode = GOElitePlugin.getSampleNodeWithAttribute( primaryIDCol, bUseCytoscapeID );
		if ( null == sampleNode ) { return; }
	
		if ( bUseCytoscapeID )
		{
		  sampleID = sampleNode.getIdentifier();
		}
		else
		{
			sampleID = ( String ) nodeAttributes.getAttribute( sampleNode.getIdentifier(), primaryIDCol );
		}
		CyLogger.getLogger().debug( "sampleID: " + sampleID );
		
		// Update ID type display
		Set<String> idTypes = guessIdType( sampleID );
		CyLogger.getLogger().debug( "guessedIDtype: " + idTypes.size() + " found ");
		
		// TODO: just take first guess for now
		String firstType = null;
		if (null != idTypes) {
			for (String t : idTypes) {
				firstType = t;
				break;
			}
		}
		if ( firstType == null ) { firstType = "???"; }
		CyLogger.getLogger().debug( "guess = " + firstType );

		// update the primary ID type combobox with guess
		for (int i = 0; i < comboBox.getItemCount(); i++) 
		{
			if (comboBox.getItemAt(i).equals(firstType)) {
				CyLogger.getLogger().debug( "found in combobox" );

				comboBox.setSelectedIndex(i);
				comboBox.repaint();
			}
		}
	}
	
	static String[] vSpecies = {};
	static String[] vSpeciesLatin = {};
	
	// Due to the various parallel arrays, we cannot use CyThesaurus to populate the list of gene systems internally
	//   ( as CyThesaurus only exposes the gene system names, not the equivalent system codes for them, which is
	//   the actual input into GO elite opal )
	// The spelling of these hard-coded values must match what is given by CyThesaurus in order for our guessID stuff to work
	//  it would be better if CyThesaurus returned an ID system code instead of a the full spelling, and had a 2-way 
	//  translation between code and full spelling
	/*
	Entrez Gene
	Ensembl Yeast
	SGD
	RefSeq
	Description
	Chromosome
	Synonyms
	Type
	GeneOntology
	PDB
	Symbol
	Uniprot/TrEMBL
	Affy
	WikiGenes
	*/
	
	// be sure to add "Sy" and "???"
	static String vGeneSystems[] = {};
	
	
	static String vMODIDSystems[] = {new String( "Ensembl"), new String( "EntrezGene" )};
	
	// Code for input files
	static String vGeneSystemCodes[] = {};
	
	static String vPruningAlgorithms[] = {new String("z-score"),
			new String("\"gene_number\""), new String("combination")};
	
	public synchronized String getSelectedSpecies()
	{
		String species = "";
		if ( mode == InputDataType.FILE )
		{
			species = vSpecies[ fileSpeciesToAnalyzeComboBox.getSelectedIndex() ];
		}
		else if ( mode == InputDataType.CRITERIA )
		{
			species = vSpecies[ criteriaSpeciesToAnalyzeComboBox.getSelectedIndex() ];
		}
		else if ( mode == InputDataType.NETWORK )
		{
			species = vSpecies[ networkSpeciesToAnalyzeComboBox.getSelectedIndex() ];
		}

		return( species );
	}
	public static String[] getSpeciesCodes() {
		return (vSpecies);
	}

	public static String[] getGeneSystems() {
		return (vGeneSystems);
	}

	public static String[] getGeneSystemCodes() {
		return (vGeneSystemCodes);
	}

	public static String[] getPruningAlgorithms() {
		return (vPruningAlgorithms);
	}


	// used for results panel in Cytoscape window
	static boolean bResultsMasterPanelAlreadyAdded = false;




	public void actionPerformed(ActionEvent evt_) 
	{

	}		
	


	
	void launchJob(final String geneListFilePath, final String denomFilePath ) {
		launchJob( geneListFilePath, denomFilePath, -1, "", null, true );
	}
	
	// there are two modes:
	// 1 - run a fresh run ( this fires off GO and Pathway jobs separately )
	// 2 - rerun an analysis ( this fires off one of GO / pathway using the same params as before except with a few overrides explicitly
	//     passed in:  it also reuses an analysisTypePanel for output display )
	public void launchJob(final String geneListFilePath, final String denomFilePath,
			int overrideNumPermutations, String overrideZScorePruningMethod,
			CloseableTabbedPane analysisTypePanelToReuse, boolean bRunGONotPathway ) 
	{
		// sanity check
		if ( !(new File( geneListFilePath ).exists() ) )
		{
			CyLogger.getLogger().error( "Invalid genelist/numerator file" );
			return;
		}
		if ( !(new File( denomFilePath ).exists() ) )
		{
			CyLogger.getLogger().error( "Invalid denominator file" );
			return;
		}

		if ( Utilities.countLinesInFile( geneListFilePath ) == 0 )
		{
			CyLogger.getLogger().error( "genelist/numerator is empty" );
			return;			
		}
		if ( Utilities.countLinesInFile( denomFilePath ) == 0 )
		{
			CyLogger.getLogger().error( "denominator is empty" );
			return;			
		}

		
		CyLogger.getLogger().debug( "1" );
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.EAST);
		CyLogger.getLogger().debug( "2" );
		if (resultsMasterPanel == null) {
			resultsMasterPanel = new CloseableTabbedPane();
		}
		CyLogger.getLogger().debug( "3" );
		
		if (!bResultsMasterPanelAlreadyAdded) {
			cytoPanel.add("GO-Elite Results", resultsMasterPanel);
			bResultsMasterPanelAlreadyAdded = true;
		}
		resultsMasterPanel.setMinimumSize( new Dimension( 400, 0 ) ) ;
		if ( cytoPanel.getState().equals(CytoPanelState.HIDE ))
		{
			cytoPanel.setState(CytoPanelState.DOCK);
		}
		CyLogger.getLogger().debug( "5" );

		// update ResultsPanel in Workspaces
		String geneListFilePrefix = "";
		Pattern p = Pattern.compile( "(.+(\\\\|/))*(.+)\\.txt" );
		Matcher m = p.matcher( geneListFilePath );
		if ( m.matches() )
		{
		  geneListFilePrefix = m.group( 3 );
		}
		else
		{
			// throw error
			CyLogger.getLogger().error( "gene list file must end in .txt" );
			return;
		}

        String resultName = geneListFilePrefix; 
        CyLogger.getLogger().debug( resultName );
		
		// INNER class: SwingWorker - only needed here inside this function
        if ( analysisTypePanelToReuse == null )
        {
        	// run both analyses, GO and Pathway
            CyLogger.getLogger().debug( "5.1" );

			resultsAnalysisNamePanel = new CloseableTabbedPane(); 
			CloseableTabbedPane pathwayPanel = new CloseableTabbedPane();
			CloseableTabbedPane GOPanel = new CloseableTabbedPane();
            CyLogger.getLogger().debug( "5.2 " + GOPanel + " " + pathwayPanel );
			resultsAnalysisNamePanel.add( "GO", GOPanel );
			resultsAnalysisNamePanel.add( "Pathway", pathwayPanel );
			CyLogger.getLogger().debug( "5.3" );
			resultsMasterPanel.add( resultName, resultsAnalysisNamePanel );  // should be name of network/file
			CyLogger.getLogger().debug( "5.4" );
		
	    	
	    	CommandHandler.updateResultsPanel(resultName, false, "GO-Elite Results", resultName, geneListFilePath );
	    	CyLogger.getLogger().debug( "4" );
			
        
     		InputDialogWorker pathwayAnalysisWorker = 
     			new InputDialogWorker( this, resultName, geneListFilePath, denomFilePath, false, pathwayPanel, overrideNumPermutations, overrideZScorePruningMethod );
     		InputDialogWorker GOAnalysisWorker = 
     			new InputDialogWorker( this, resultName, geneListFilePath, denomFilePath, true, GOPanel, overrideNumPermutations, overrideZScorePruningMethod );
     		pathwayAnalysisWorker.execute();
     		GOAnalysisWorker.execute();

        }
        else
        {
        	// just reruns one of either GO/Pathway analysis
    		analysisTypePanelToReuse.removeAll();
    		CyLogger.getLogger().debug( "creating analysisWorker: " + bRunGONotPathway + " " + analysisTypePanelToReuse + " " + 
    				overrideNumPermutations + " " + overrideZScorePruningMethod + "\n");
    		InputDialogWorker analysisWorker = 
     			new InputDialogWorker( this, resultName, geneListFilePath, denomFilePath, bRunGONotPathway,
     					analysisTypePanelToReuse, overrideNumPermutations, overrideZScorePruningMethod );

    		CyLogger.getLogger().debug( "executing analysisWorker\n");

	    	CommandHandler.updateResultsPanel(resultName, false, "GO-Elite Results", resultName, geneListFilePath );
    		analysisWorker.execute();
    		
    		CyLogger.getLogger().debug( "done analysisWorker\n");

    	
        }
	}

	public void resizeColumns(JTable table) 
	{
		CyLogger.getLogger().setDebug( true );
		CyLogger.getLogger().debug( "resizeColumns" );
		CyLogger.getLogger().debug( "table = " + table + " columnCnt = " + table.getColumnCount() + " rowCnt = " + table.getRowCount() );

		for (int i = 0; i < table.getColumnCount(); i++) 
		{
			TableColumn col = table.getColumnModel().getColumn(i);
			int width = 0;

			// cycle through rows to find max
			for (int r = 0; r < table.getRowCount(); r++) {
				CyLogger.getLogger().debug( "(column,row) = " + i + " " + r );
				CyLogger.getLogger().debug( "value = " + table.getValueAt( r, i ) );
				
				javax.swing.table.TableCellRenderer  tcr = table.getCellRenderer(r, i);
				CyLogger.getLogger().debug("1" + tcr );
				if ( tcr == null ) 
				{
					CyLogger.getLogger().debug( "null tcr" );
				}
				
				Component comp = tcr.
						getTableCellRendererComponent(table,
								table.getValueAt(r, i), false, false, r, i);
				CyLogger.getLogger().debug("2");
				if ( comp == null ) 
				{
					CyLogger.getLogger().debug( "null comp" );
				}
				
				CyLogger.getLogger().debug("3");
				// max: header vs. data value width
				width = Math.max(width, comp.getPreferredSize().width);
				CyLogger.getLogger().debug("4");
			}
			// Add margin
			width += 2;
			// Set the width
			CyLogger.getLogger().debug("5");
			col.setPreferredWidth(width);
			CyLogger.getLogger().debug( "end of width" );

		}

	}
	 public static class CustomTableCellRenderer extends DefaultTableCellRenderer
	 {
		 Vector< Boolean > vbHighlightRow= null;
	 
		 CustomTableCellRenderer( Vector< Boolean> vbHighlightRow_ ) { vbHighlightRow = vbHighlightRow_; }

		 public Component getTableCellRendererComponent (JTable table, 
				 Object obj, boolean isSelected, boolean hasFocus, int row, int column) 
		 {
			 Component cell = super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
		  
			 if ( vbHighlightRow.get( row ) == Boolean.TRUE ) 
			 {  
				 cell.setBackground( Color.yellow );
			 }
			 else
			 {
				 cell.setBackground( Color.white );
			 }
			 
			 if ( isSelected )
			 {
				 cell.setForeground( Color.red );
			 }
			 else
			 {
				 cell.setForeground( Color.black );
			 }
			 
			 return cell;
		 }
	 }

}