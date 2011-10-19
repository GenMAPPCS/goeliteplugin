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
	
	InputDataType mode = InputDataType.FILE;
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
        
    public InputDialog()
    {
    	setTitle( "GO-Elite" );
    	JDialog pane = this;
    	CyLogger.getLogger().debug( "addComponentToPane 1" );   	
    	
    	debugWindow = new JTextArea("", 5, 40);
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

    	int idx = 0;
		int defaultSpeciesIdx = 0;
		ArrayList<String> arrayListSpecies = new ArrayList<String>();
		for (String line : lines) {
			String[] s = line.split("\t");
		
			// format: genus \t species \t common \t two-letter
			String latinName = (s[0] + " " + s[1]);
			String twoLetterCode = s[3];
		
			if (latinName.equals(defaultSpecies)) {
				defaultSpeciesIdx = idx;
			}
		
			// you can also use this list to populate your vSpecies[] array
			arrayListSpecies.add(twoLetterCode);
			idx++;
		}
		vSpecies = arrayListSpecies.toArray(vSpecies);

    	CyLogger.getLogger().debug( "addComponentToPane 18" );

		String systemCodeInData = Cytoscape.getNetworkAttributes().getStringAttribute( 
				Cytoscape.getCurrentNetwork().getIdentifier(),	"SystemCode" );
		int systemCodeDefaultIdx = 0;
		if ( systemCodeInData != null )
		{
			for( int ii =0; ii < vGeneSystems.length; ii++ )
			{
				if ( vGeneSystems[ ii ].equals( systemCodeInData ) )
				{
					systemCodeDefaultIdx = ii;
				}
			}
		}

    	
    	
    	// Top-level button group for switching modes
    	JPanel modeSelectionPanel = new JPanel(); //use FlowLayout
        modeSelectionPanel.setBorder( new TitledBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ), "", 
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION ) );
       
        addRadioButton( InputDataType.FILE, modeSelectionPanel );
        addRadioButton( InputDataType.CRITERIA, modeSelectionPanel );
        addRadioButton( InputDataType.NETWORK, modeSelectionPanel );
        
    	CyLogger.getLogger().debug( "addComponentToPane 2" );
        
        // Create the "cards".
        
        // ****  create file card *****
        JPanel fileCard = new JPanel();
        fileCard.setLayout( new BoxLayout( fileCard, BoxLayout.Y_AXIS ) );

        // init data-holding components
        fileSpeciesToAnalyzeComboBox = new JComboBox( vSpecies );
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
        criteriaSpeciesToAnalyzeComboBox = new JComboBox( vSpecies );
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
		criteriaPrimaryIDColumnComboBox.insertItemAt("ID", 0);
		for (String n : attributeNames) {
			if (CyAttributes.TYPE_STRING == nodeAttributes
					.getType(n)) {
				criteriaPrimaryIDColumnComboBox.addItem(n);
			}
		}
		
    	CyLogger.getLogger().debug( "addComponentToPane 4" );
		String[] criteriaSet = GOElitePlugin
				.getCriteriaSets(debugWindow);
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
    		for( String c : GOElitePlugin.getCriteria( cs, debugWindow ) )
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
        
        Box criteriaSpeciesParamBox = Box.createHorizontalBox();
        criteriaSpeciesParamBox.add( new JLabel( "Species" ) );
        criteriaSpeciesParamBox.add( criteriaSpeciesToAnalyzeComboBox );
        
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
        		TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION) );
        
        Box criteriaNumerPanel = Box.createHorizontalBox();
        Box criteriaDenomPanel = Box.createHorizontalBox();
        
    	CyLogger.getLogger().debug( "addComponentToPane 7" );

    	criteriaNumerFileDescriptor = new JLabel( "Criteria Set" );
        criteriaNumerPanel.add( criteriaNumerFileDescriptor );
        criteriaNumerPanel.add( Box.createHorizontalGlue() );        
        criteriaNumerPanel.add( criteriaInputCriteriaSetComboBox );
    
        criteriaDenomFileDescriptor = new JLabel( "Criteria ( 0 / 0 )" ) ;
        criteriaDenomPanel.add(  criteriaDenomFileDescriptor );
        criteriaDenomPanel.add( Box.createHorizontalGlue() );        
        criteriaDenomPanel.add( criteriaInputCriteriaComboBox );
                
        criteriaInputSelectPanel.add( criteriaSpeciesParamBox ); 
        criteriaInputSelectPanel.add( criteriaPrimaryIDParamBox );
        criteriaInputSelectPanel.add( criteriaNumerPanel );
        criteriaInputSelectPanel.add( criteriaDenomPanel );        

    	CyLogger.getLogger().debug( "addComponentToPane 8" );

        // add the panels to the card
        criteriaCard.add( criteriaInputSelectPanel );
        
        
        // ****  create network card *****
        JPanel networkCard = new JPanel();
        networkCard.setLayout( new BoxLayout( networkCard, BoxLayout.Y_AXIS ) );

        networkSpeciesToAnalyzeComboBox = new JComboBox( vSpecies );
		networkPrimaryIDColumnComboBox = new JComboBox();
		networkPrimaryIDColumnComboBox.insertItemAt("ID", 0);
		for (String n : attributeNames) {
			if (CyAttributes.TYPE_STRING == nodeAttributes
					.getType(n)) {
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
        //pane.add( new JScrollPane( debugWindow ), BorderLayout.EAST );
        
    	CyLogger.getLogger().debug( "addComponentToPane 13" );

	
		
        // actionlisteners
		ActionListener inputSourceActionListener = new ActionListener() 
		{

			public void actionPerformed(ActionEvent e) 
			{
				repaint();
				debugWindow.append( "actionPerformed..." );

				if ( e.getSource() == criteriaPrimaryIDColumnComboBox )
				{
					updatePrimaryIDSystemComboBoxes( ( String ) criteriaPrimaryIDColumnComboBox.getSelectedItem(), 
							criteriaPrimaryIDSystemComboBox );
				}
				else if ( e.getSource() == networkPrimaryIDColumnComboBox )
				{
					updatePrimaryIDSystemComboBoxes( ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), 
							networkPrimaryIDSystemComboBox );
				}
				
				if (e.getSource() == criteriaInputCriteriaSetComboBox ) 
				{
					debugWindow.append( "getting criteria...\n" );
					String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox
						.getSelectedItem();
					String[] newCriteria = GOElitePlugin
						.getCriteria((String) criteriaInputCriteriaSetComboBox
							.getSelectedItem(), debugWindow);
					debugWindow.append( "removeAllItems\n" + criteriaInputCriteriaComboBox );
					
					// JComboBox removeall() was acting up, so I create a dummy and swap models instead
					JComboBox dummy = new JComboBox();
					dummy.addItem( criteriaAllStringValue );
					
					for ( int i = 0; i < newCriteria.length; i++ )
					{
						dummy.addItem( newCriteria[ i ] );
					}
					
					criteriaInputCriteriaComboBox.setModel( dummy.getModel() );
					debugWindow.append( "repainting combobox...\n" );
				}
				if (e.getSource() == criteriaInputCriteriaSetComboBox
						|| e.getSource() == criteriaInputCriteriaComboBox) 
				{
					debugWindow
						.append("event caught: inputCriteriaSetComboBox or inputCriteriaComboBox\n ");
					try 
					{
						// update the denominator label to count up number of
						// hits
						String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox
								.getSelectedItem();
						String selectedCriteria = (String) criteriaInputCriteriaComboBox
								.getSelectedItem();
	
						debugWindow.append("counting hits for criteria "
								+ selectedCriteriaSet + " \\ "
								+ selectedCriteria + "\n");
						long numHits = 0;
						long numTotal = 0;
						if (((String) selectedCriteria)
								.equals(criteriaAllStringValue)) {
							criteriaDenomFileDescriptor.setText("Criteria: ( - )");
	
						} else {
							
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
											debugWindow);
							numHits = nums[0];
							numTotal = nums[1];
							debugWindow.append("setting label: " + numHits
									+ " out of " + numTotal + "\n:");
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
													+ Utilities
															.countLinesInFile(filePath)
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
													+ Utilities
															.countLinesInFile(filePath)
													+ ")");
								}
							}
						} 
						catch (java.io.IOException exception) 
						{
							CyLogger.getLogger().error( ""+exception );
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
			
					
					
					// user hit "Run analysis button" 
					InputDialog.this.dispose();
							
					debugWindow.append("launchButton: " + launchButton + "\n");
					debugWindow.append("genInputFileButton: " + generateInputFileButton
							+ "\n");
								
					debugWindow.append("launched a job request\n");
					String pluginDir = null;
					try 
					{
						debugWindow.append("1\n");
			
						if ( InputDataType.NETWORK == mode || InputDataType.CRITERIA == mode ) {
							debugWindow.append("2\n");
			
							pluginDir = PluginManager.getPluginManager()
									.getPluginManageDirectory().getCanonicalPath()
									+ "/GOEliteData";
							boolean pluginDirExists = new File(pluginDir).exists();
							if (!pluginDirExists) {
								debugWindow.append("plugin dir " + pluginDir
										+ " does not exist: creating...");
			
								boolean success = new File(pluginDir).mkdir();
								if (!success) {
									debugWindow.append("couldn't create plugin dir "
											+ pluginDir);
								}
							}
							debugWindow.append("3\n");
			
							/*
							String systemCode = vGeneSystemCodes[new Integer(
									layoutProperties.getValue("gene_system_idx_code"))
									.intValue()];
							String systemCodeInData = Cytoscape.getNetworkAttributes().getStringAttribute( Cytoscape.getCurrentNetwork().getIdentifier(),
									"SystemCode" );
							if ( !systemCode.equals( systemCodeInData ) && !systemCodeInData.equals( "MIXED" ) )
							{
								// user error: abort
								JOptionPane.showMessageDialog( null, "The primary identifier system code you selected [" + systemCode + "] does not match the data [ " + 
										systemCodeInData + "]" );
								return;
							}
							*/
							debugWindow.append("4\n");
						
							if ( InputDataType.CRITERIA == mode )
							{
								String selectedCriteriaSet = (String) criteriaInputCriteriaSetComboBox.getSelectedItem();
								String selectedCriteria = (String) criteriaInputCriteriaComboBox.getSelectedItem();
								String[] criteriaList = new String[]{selectedCriteria};
								String systemCode = ( String ) criteriaPrimaryIDSystemComboBox.getSelectedItem();
								 
								if (((String) criteriaInputCriteriaComboBox.getSelectedItem())
										.compareTo(criteriaAllStringValue) == 0) {
									debugWindow.append("-all- found: get list of criteria ");
									// user chose "-all-" criteria: launch a series of jobs, one
									// per criteria in the criteriaSet
									criteriaList = GOElitePlugin.getCriteria(selectedCriteriaSet, debugWindow);
								}
			
								for (String criteria : criteriaList) {
									String geneListFileName = criteria;
									String geneListFilePath = pluginDir + "/" + geneListFileName;
									geneListFilePath = Utilities.generateUniqueFilename(geneListFilePath);
									CyLogger.getLogger().debug( "geneListFileName " + geneListFileName );
									CyLogger.getLogger().debug( "geneListFilePath " + geneListFilePath );
									debugWindow.append("launching job for criteria: "
											+ criteria);
									// if criteria selected, generate files first
				
									debugWindow.append("generating input numerator\n");
									GOElitePlugin.generateInputFileFromNetworkCriteria(
											geneListFilePath, systemCode,
											(String) selectedCriteriaSet, (String) criteria,
											true, true,
											(String) criteriaPrimaryIDColumnComboBox
													.getSelectedItem(), debugWindow);
				
									String denomFileName = criteria + "_denom";
									String denomFilePath = pluginDir + "/" + denomFileName;
									debugWindow.append("generating input denominator\n");
									denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
											+ denomFileName + ".txt");
									GOElitePlugin.generateInputFileFromNetworkCriteria(
											denomFilePath, systemCode,
											(String) selectedCriteriaSet, (String) criteria,
											false, true,
											(String) criteriaPrimaryIDColumnComboBox
													.getSelectedItem(), debugWindow);
				
									launchJob(geneListFilePath, denomFilePath);
								}
							} 
							else if ( InputDataType.NETWORK == mode )
							{
								debugWindow.append("5\n");
			
								String systemCode = ( String ) networkPrimaryIDSystemComboBox.getSelectedItem();
			
								// this is the network the user selected as the denominator
								String networkTitle = ( String ) networkInputDenominatorComboBox .getSelectedItem();
								String networkID = getNetwork( networkTitle ).getIdentifier();
								CyNetwork network = Cytoscape.getNetwork( networkID );
			
								String geneListFileName = network.getTitle();
								String geneListFilePath = pluginDir + "/" + geneListFileName;
								geneListFilePath = Utilities.generateUniqueFilename(geneListFilePath);
			
								debugWindow.append("launching job for network: "
											+ networkID );
								// if criteria selected, generate files first
								debugWindow.append("generating input numerator\n");
			
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
										true, ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), debugWindow);
								CyLogger.getLogger().debug( "6" );
								debugWindow.append("file generated for " + geneListFilePath + "\n");
			
								
								// for denominator, use all nodes in the selected network
								Set< Node > denomNodes = new java.util.HashSet<Node>( network.nodesList() );
								String denomFileName =  "network_denom";
								String denomFilePath = pluginDir + ")/" + denomFileName;
								debugWindow.append("generating in)put denominator\n");
								denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
										+ denomFileName);
								GOElitePlugin.generateInputFileFromNodeSet(
										denomFilePath, systemCode,
										denomNodes,
										true, ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(),
										debugWindow);
								debugWindow.append("file generated for " + denomFilePath + "\n");
			
								launchJob(geneListFilePath, denomFilePath);
								debugWindow.append("launchJob() done\n");
							}		
						}
						else 
						{
							String geneListFilePath = fileNumerFilename.getText();
							String denomFilePath = fileDenomFilename.getText();
			
							launchJob(geneListFilePath, denomFilePath);
			
						}
						debugWindow.append("2>" + service + "\n");
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
				criteriaPrimaryIDSystemComboBox );
		updatePrimaryIDSystemComboBoxes( ( String ) networkPrimaryIDColumnComboBox.getSelectedItem(), 
				networkPrimaryIDSystemComboBox );
		
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

		//pane.add(new JScrollPane(debugWindow), BorderLayout.SOUTH );
		pane.setVisible(true);
    	CyLogger.getLogger().debug( "addComponentToPane end" );

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
	JTextArea debugWindow = null;
	public final static long serialVersionUID = 0;
	AppServicePortType service = null;
	
	String resultName = null;
	
	// prepare input files
	JTabbedPane resultsAnalysisNamePanel = null;
	
	class InputDialogWorker extends SwingWorker<StatusOutputType, Void>
	{
		String geneListFilePath = "";
		String denomFilePath = "";
		JButton rerunAnalysisButton = null, exportButton = null;
		JComboBox rerunAnalysisZScorePruningMethod = null;
		JTextField rerunAnalysisNumPermutations = null;
		edu.sdsc.nbcr.opal.types.StatusOutputType status = null;
		CloseableTabbedPane resultsAnalysisTypePanel = null;
		JTextArea statusWindow = null, stdoutWindow = null,
				stderrWindow = null;
		AppServicePortType service = null;
		String jobID = null;
		JTable resultsTable = null; 
		
		// go-elite user parameters:  these override the ones selected in the dialog 
		int overrideNumPermutations = -1;   // -1 will default to using the dialog selection
		String overrideZScorePruningMethod = "";
		
		// defaults
		int defaultMinNumGenesChanged = 3, defaultNumPermutations = 0;
		String defaultZScorePruningMethod = "z-score";
		double defaultPValueThresh = 0.05;
		double defaultZScoreThresh = 1.96;
		
		boolean bRunGONotPathwayAnalysis = false;
		
		
		public InputDialogWorker( String geneListFilePath_, String denomFilePath_, boolean bRunGONotPathwayAnalysis_, CloseableTabbedPane resultsAnalysisTypePanel_, 
				int numPermutations_, String zScorePruningMethod_ )
		{
			geneListFilePath = geneListFilePath_;  denomFilePath = denomFilePath_;
			
			bRunGONotPathwayAnalysis = bRunGONotPathwayAnalysis_;

			// this stuff is common to both pathway and GO analyses for the given job
			// **** Prepare results pane 			
			resultsAnalysisTypePanel = resultsAnalysisTypePanel_;
			overrideNumPermutations = numPermutations_;
			overrideZScorePruningMethod = zScorePruningMethod_;
		}
		
		public StatusOutputType doInBackground() 
		{
			try {
				JobInputType launchJobInput = new JobInputType();

				debugWindow.append("doInBkgd\n");
				
				JPanel statusPanel = new JPanel();
				debugWindow.append("s0\n");

				statusPanel.setLayout(new BoxLayout(statusPanel,
						BoxLayout.PAGE_AXIS));
				debugWindow.append("s1\n");

				statusWindow = new JTextArea("", 15, 80);
				JScrollPane statusScroll = new JScrollPane(statusWindow);
				statusPanel.add(statusScroll);
				debugWindow.append("s2\n");


				resultsAnalysisTypePanel.addTab("Status", statusPanel);
				resultsMasterPanel.setSelectedComponent( resultsAnalysisNamePanel );
				resultsAnalysisNamePanel.setSelectedComponent( resultsAnalysisTypePanel );
				resultsAnalysisTypePanel.setSelectedComponent( statusPanel );
				
				debugWindow.append( "WebService.getService calledn" );
				service = WebService.getService();
				debugWindow.append( "WebService.getService returned = " + service );
				if ( null == service ) { CyLogger.getLogger().error( "Couldn't get service object from WebService" ); }
				
				Map<String, String> args = new HashMap<String, String>();
				
				String species = "";
				debugWindow.append( "species" );

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

				// make sure everything is in String format
				args.put(WebService.ARG_SPECIES, species);
			
				
				debugWindow.append( "modid" );

				// parallel arrays: translate the selected id system into the appropritate master-key id system
				int geneSystemSelectedIndex = -1;
				// first determine the current system code
				
				String geneSystemSelected = "";
				// XXXX - need to select based on type: FILE must read from disk
				if ( mode == InputDataType.NETWORK )
				{
					geneSystemSelectedIndex = networkPrimaryIDSystemComboBox.getSelectedIndex();
					geneSystemSelected = vGeneSystemMODS[ geneSystemSelectedIndex ]; 
				}
				else if ( mode == InputDataType.CRITERIA )
				{
					geneSystemSelectedIndex = criteriaPrimaryIDSystemComboBox.getSelectedIndex();
					geneSystemSelected = vGeneSystemMODS[ geneSystemSelectedIndex ];
				}
				else if ( mode == InputDataType.FILE )
				{
					geneSystemSelected = ( String ) fileMODIDSystemComboBox.getSelectedItem();
				}
					
				args.put( WebService.ARG_MOD_ID_SYSTEM,
						geneSystemSelected );
				
				debugWindow.append( "numperm" );

				int numPermutations = ( overrideNumPermutations == -1 ?
						defaultNumPermutations : overrideNumPermutations );
				args.put(WebService.ARG_NUM_PERMUTATIONS, 
						"" + numPermutations );
				
				
				debugWindow.append( "pruning" );

				args.put(WebService.ARG_PRUNING_ALGORITHM,
						overrideZScorePruningMethod.length() == 0 ?
						  defaultZScorePruningMethod :
						  overrideZScorePruningMethod );
				
				debugWindow.append( "analysis" );

				args.put(WebService.ARG_ANALYSIS_TYPE,
						( bRunGONotPathwayAnalysis ? "GeneOntology" : "Pathways" ) );
				args.put(WebService.ARG_PVAL_THRESH, "" + defaultPValueThresh );
				args.put(WebService.ARG_MIN_GENES_CHANGED, "" + defaultMinNumGenesChanged );
				args.put(WebService.ARG_ZSCORE_THRESH, "" + defaultZScoreThresh );
				args.put(WebService.ARG_GENELIST_FILE, geneListFilePath);
				args.put(WebService.ARG_DENOM_FILE, denomFilePath);

				debugWindow.append( "WebService.launchJob called\n" );
				jobID = WebService.launchJob(args, service, statusWindow);
				debugWindow.append( "WebService.launchJob returned with id = " + jobID + "\n" );
				String serverUrl = WebService.OUTPUT_HEAD_URL;
				statusWindow.append("Link to webservice job:\n "
						+ serverUrl + jobID + "\n\n");
				debugWindow.append("Link to webservice job: " + serverUrl
						+ jobID);

				// 8 is the code for completion
				statusWindow.append("Status:\n");
				// scroll to bottom to show status
				statusWindow.setCaretPosition(statusWindow.getDocument()
						.getLength());
				while (status == null || 2 == status.getCode()) {
					Thread.sleep(5000);
					status = WebService.getStatus(jobID, service);
					debugWindow.append("[" + status.getCode() + "] "
							+ status.getMessage() + "\n");
					statusWindow.append("[" + status.getCode() + "] "
							+ status.getMessage() + "\n");
					// Determine position of scroll bar
					JScrollBar bar = statusScroll.getVerticalScrollBar();
					boolean autoScroll = ((bar.getValue() + bar
							.getVisibleAmount()) == bar.getMaximum());
					// If still at bottom, then continue to auto scroll;
					// otherwise, assume user is purposefully scrolling text
					// area and leave them alone
					if (autoScroll)
						statusWindow.setCaretPosition(statusWindow
								.getDocument().getLength());

				}
			} catch (javax.xml.rpc.ServiceException e) {
				Utilities.showError("Could not retrieve webservice", e);
			} catch (java.lang.InterruptedException e) {
				// Thread.sleep() was interrupted, ignore
			}

			return (status);
		}

		@SuppressWarnings("serial")
		@Override
		public void done() {
	    	CommandHandler.updateResultsPanel(resultName, true, "GO-Elite Results", resultName, geneListFilePath );
			
			System.out.println("done!");
			debugWindow.append("done! status = " + status + "\n");
			try {
			
			

				// print results in results panel
				// status.getCode() == 8  means success
				// grab all the available results, regardless of return code
				if ( true ) {
					debugWindow.append("getting results!!!\n");

					String geneListFilePrefix = "";
					Pattern p = Pattern.compile( "(.+(\\\\|/))*(.+)\\.txt" );
					Matcher m = p.matcher( geneListFilePath );
					if ( m.matches() )
					{
					  geneListFilePrefix = m.group( 3 );
					  debugWindow.append( "prefix = " + geneListFilePrefix + "\n" );
					}
					else
					{
						// throw error
						CyLogger.getLogger().error( "gene list file must end in .txt" );
						return;
					}
					
					URL[] vResultURL = WebService.getResults(jobID, geneListFilePrefix,
							service, debugWindow );
					debugWindow.append("results fetched: "
							+ vResultURL.length + " URLs\n");

					// ******  grab results off server and put into simple data structures *****
					Vector<String> logFileContents = new Vector<String>();
					Vector<String> stdoutFileContents = new Vector<String>();
					Vector<String> stderrFileContents = new Vector<String>();
					Vector<String> GONameResultsColumnNames = new Vector<String>();
					Vector<String> pathwayResultsColumnNames = new Vector<String>();
					Vector<Vector> GONameResultsRowData = new Vector<Vector>();
					Vector<Vector> pathwayResultsRowData = new Vector<Vector>();
					HashMap< String, Boolean > GOIDsToHighlight = new HashMap< String, Boolean >();
					HashMap< String, Boolean > pathwayIDsToHighlight = new HashMap< String, Boolean >();
					Vector< Boolean > vbGORowsToHighlight = new Vector< Boolean >(); // parallel array with GONameResultsRowData[]
					Vector< Boolean > vbPathwayRowsToHighlight = new Vector< Boolean >();
					// process each output file that's sitting on server
					InputStream is = null;
					DataInputStream dis;
					String s;
					
				    URL u = vResultURL[ WebService.ReturnTypes.RESULT_PRUNED_GO_AND_PATHWAY.ordinal() ];
					debugWindow.append(u + "\n");
					if ( null != u ) 
					{
						debugWindow.append("parsing pruned results\n");
						statusWindow
								.append("\nParsing pruned results table...\n");
						Vector<String> fileContents = Utilities
								.getFileContents(u);
						Enumeration<String> contents = fileContents
								.elements();

						boolean processingGONameResultsNotPathwayResults = bRunGONotPathwayAnalysis;

						boolean bIsFirstLine = true;

						// the same filename)) could potentially hold both go or pathway results, but for us, we are only running one mode
						//    per job so we know which result type to expect from this file
						// header = 1st line:  rest = data  
						//   may be some blank lines at end?
						while (contents.hasMoreElements()) {

							String line = (String) contents
									.nextElement();
							if ( bIsFirstLine ) { bIsFirstLine = false; continue; }
						
							Vector<String> columnsAsVector = new Vector<String>();
							String[] rowData = (line).split("\t");
							if ( rowData.length < 3 ) { continue; }
							
							// it's a data line
							if (processingGONameResultsNotPathwayResults) 
							{
								String GONameAndID = rowData[ 2 ];
								CyLogger.getLogger().debug( "try and highlight: " + GONameAndID );

								Pattern pat = Pattern.compile( ".+\\((.+)\\)");
								Matcher match = pat.matcher( GONameAndID );
								if ( match.matches() )
								{
								  String GOID = match.group( 1 );
								  CyLogger.getLogger().debug( "highlight: " + GOID );
								  
								  GOIDsToHighlight.put( GOID, Boolean.TRUE );
								}
								else
								{
									CyLogger.getLogger().error( "Couldn't parse: " + GONameAndID + " for line " + line );
								}
							} 
							else 
							{
								// pathway info is given as
								// 3rd column = MAPP name
								// ( text name ):( pathway id )
								String[] pathwayNameAndID = rowData[ 2 ].split( ":" );
								String pathwayID = pathwayNameAndID[ 1 ];
								pathwayIDsToHighlight.put( pathwayID, Boolean.TRUE );
								CyLogger.getLogger().debug( "highlight pathway: " + pathwayID );
							}
						}
					} 

					u = vResultURL[ WebService.ReturnTypes.RESULT_FULL_GO.ordinal() ];
					debugWindow.append(u + "\n");
					if ( null != u ) 
					{
						
						debugWindow.append("parsing full go results\n");
						Vector<String> fileContents = Utilities
								.getFileContents(u);
						Enumeration<String> contents = fileContents
								.elements();

						// the results file is arranged so that GO results are reported above the Pathway results
						// When we see a header row that has "GOID" as its 1st column, we know that we've switched to Pathway results
						boolean bStartProcessing = false;
						long GORowsToHighlight = 0;
						while (contents.hasMoreElements()) 
						{

							String line = (String) contents
									.nextElement();
							// System.out.println(line);
							Vector<String> columnsAsVector = new Vector<String>();
							String[] rowData = (line).split("\t");

							if ( !bStartProcessing && rowData[ 0 ].contains("GOID") ) 
							{
								bStartProcessing = true;
								GONameResultsColumnNames.addAll(Arrays
										.asList(rowData));
								continue;
							}
							if ( !bStartProcessing ) { continue; }
							
							if ( GOIDsToHighlight.containsKey( rowData[ 0 ] ) && 
							     ( Boolean.TRUE == GOIDsToHighlight.get( rowData[ 0 ] ) ) )
							{
								GORowsToHighlight++;
								vbGORowsToHighlight.add( Boolean.TRUE );
							}
							else
							{
								vbGORowsToHighlight.add( Boolean.FALSE );
							}
							
							GONameResultsRowData
										.add(new Vector<String>(Arrays
												.asList(rowData)));
							
						}
						CyLogger.getLogger().debug( "GORowsToHighlight " + GORowsToHighlight );
					}
					
					u = vResultURL[ WebService.ReturnTypes.RESULT_FULL_PATHWAY.ordinal() ];
					debugWindow.append(u + "\n");
					if ( null != u ) 
					{
						
						debugWindow.append("parsing full pathway results\n");
						Vector<String> fileContents = Utilities
								.getFileContents(u);
						Enumeration<String> contents = fileContents
								.elements();

						// the results file is arranged so that there is a lengthy header
						// the header ends / column names start with "^MAPP Name"
						boolean bStartProcessing = false;
						long pathwayRowsToHighlight = 0;
						while (contents.hasMoreElements()) 
						{

							
							String line = (String) contents
									.nextElement();
							// System.out.println(line);
							Vector<String> columnsAsVector = new Vector<String>();
							String[] rowData = (line).split("\t");

							if ( !bStartProcessing && rowData[ 0 ].contains("MAPP Name") ) 
							{
								bStartProcessing = true;
								pathwayResultsColumnNames.addAll(Arrays
										.asList(rowData));
								continue;
							}
							if ( !bStartProcessing ) { continue; }
							
							// Pathway ID is buried in the MAPPName   ( text name ):( id )
							// Triacylglyceride Synthesis:WP386

							CyLogger.getLogger().debug( "rowdata[0] " + rowData[0] );

							String[] pathwayTextNameAndID = rowData[ 0 ].split( ":" );
							String pathwayID = pathwayTextNameAndID[1];
							CyLogger.getLogger().debug( "pathwayTextNameAndID " + pathwayID);

							if ( pathwayIDsToHighlight.containsKey( pathwayID ) && 
							     ( Boolean.TRUE == pathwayIDsToHighlight.get( pathwayID ) ) )
							{
								pathwayRowsToHighlight++;
								vbPathwayRowsToHighlight.add( Boolean.TRUE );
							}
							else
							{
								vbPathwayRowsToHighlight.add( Boolean.FALSE );
							}
							
							pathwayResultsRowData
										.add(new Vector<String>(Arrays
												.asList(rowData)));
							
						}
						CyLogger.getLogger().debug( "pathwayRowsToHighlight " + pathwayRowsToHighlight );
					}
					
					u = vResultURL[ WebService.ReturnTypes.RESULT_LOG.ordinal() ];
					if ( null != u )
					{
						logFileContents = Utilities.getFileContents(u);
					} 
					
					u = vResultURL[ WebService.ReturnTypes.RESULT_STDOUT.ordinal() ];
					if ( null != u ) 
					{
						stdoutFileContents = Utilities.getFileContents( u );
					} 
					
					u = vResultURL[ WebService.ReturnTypes.RESULT_STDERR.ordinal() ];
					if ( null != u ) 
					{
						stderrFileContents = Utilities.getFileContents( u );
					}

					
						
					
					// *****  process results *****							
					if ( GONameResultsRowData.size() == 0 && bRunGONotPathwayAnalysis )
					{
						statusWindow.append( "No GO results found\n" );
					}
					if ( pathwayResultsRowData.size() == 0 && !bRunGONotPathwayAnalysis )
					{
						statusWindow.append( "No Pathway results found\n" );
					}
					else
					{
					}
					debugWindow.append( "populating tables\n");

					// actionlistener to handle rerunning of analysis from results panel
					ActionListener rerunAnalysisActionListener = new ActionListener( ) 
					{
						
						public void actionPerformed( ActionEvent e )
						{
							// edit # of permutations
							int numPermutations = new Integer( rerunAnalysisNumPermutations.getText() ).intValue();
							String zScorePruningMethod = ( String ) rerunAnalysisZScorePruningMethod.getSelectedItem();
							CyLogger.getLogger().debug( "override z-score pruning: " + zScorePruningMethod );
							
							// rerun the analysis with new permutations #
							launchJob(geneListFilePath, denomFilePath, numPermutations, zScorePruningMethod, resultsAnalysisTypePanel,
									bRunGONotPathwayAnalysis );

						}
					};
					debugWindow.append( "1\n");

					class ExportResultsActionListener implements ActionListener
					{
						JTable results = null;  
						public ExportResultsActionListener( JTable results_ ) { results = results_; }
						
						public void actionPerformed( ActionEvent e )
						{
							// dump this to disk
							// dialog box: save file
							JFileChooser chooser = (null != browseButtonLastFileSelected
									? new JFileChooser(browseButtonLastFileSelected)
									: new JFileChooser());
							int returnVal = chooser.showOpenDialog( resultsMasterPanel );

							try
							{
								if (returnVal == JFileChooser.APPROVE_OPTION) 
								{
									File exportFile = chooser
											.getSelectedFile();
								
									FileWriter fw = new FileWriter( exportFile );
									String header = "";
									for( int i = 0; i < results.getColumnCount(); i++ )
									{
										
										header += ( i > 0 ? "\t" : "" ) + results.getColumnName( i );
										
									}
									header += "\n";
									fw.write( header );
									
									// write data
									for( int row = 0; row < results.getRowCount(); row++ )
									{
										String data = "";
										for( int col = 0; col < results.getColumnCount(); col++ )
										{
											data += ( col > 0 ? "\t" : "" ) + ( String ) results.getValueAt( row, col );
										}
										data += "\n";
										fw.write( data );
									}
									fw.close();
								}
								else
								{
									// aborted
									return; 
								}
							}
							catch ( java.io.IOException ex )
							{
								CyLogger.getLogger().error( "couldn't export GO-Elite results to file: " + ex );
							}

							
						}
					};
					// populate tables
					CytoPanel cytoPanel = Cytoscape.getDesktop()
							.getCytoPanel(SwingConstants.EAST);

					debugWindow.append( "2\n");

					if ( bRunGONotPathwayAnalysis )
					{
						debugWindow.append("GONameResults...\n");
						/*
						 * 	Process GO Results Table
						 */
						if (GONameResultsRowData.size() > 0) 
						{
							debugWindow.append("processing GOName Results\n");
	
							resultsTable = new JTable(
									GONameResultsRowData,
									GONameResultsColumnNames) {
								public boolean isCellEditable(int rowIndex,
										int vColIndex) {
									return false;
								}
							};
							debugWindow.append( "Num rows to highlight GO:" + vbGORowsToHighlight.size() );
							long  hits = 0;
							for( int i =0 ; i< vbGORowsToHighlight.size(); i++ )
							{
								if ( vbGORowsToHighlight.get( i ) == Boolean.TRUE ) { hits++; }
							}
							CyLogger.getLogger().debug( "vbGORowsToHighlight: " + hits + " / " + vbGORowsToHighlight.size() );
							
							resultsTable.setDefaultRenderer( Object.class, new CustomTableCellRenderer( vbGORowsToHighlight ) );

							// hide some columns
							// the indices are adjusted to account for the fact that they shift while deleting is going on
							//int[] GONameColumnsToHide = {0, 1, 5, 6, 7, 11, 12,
							//		13};
							int [] GONameColumnsToHide = {}; //
							
							for (int j = GONameColumnsToHide.length - 1; j >= 0; j--) 
							{
								TableColumn column = resultsTable
										.getColumnModel().getColumn(
												GONameColumnsToHide[j]);
								CyLogger.getLogger().debug( "hiding: " + GONameResultsColumnNames.get( j ) );
								resultsTable.removeColumn(column);
							}

						}
					}
					
					else
					{
						debugWindow.append( "3\n");
	
						/*
						 * Listener for row selection in Pathway results table. Useful for
						 * responding to clicks on pathway results.
						 */
						ListSelectionListener lsl = new ListSelectionListener() {
							public void valueChanged(ListSelectionEvent e) {
								CyLogger.getLogger().debug( "valueChanged:" );
								ListSelectionModel lsm = (ListSelectionModel) e.getSource();

								int rowIndex = lsm.getLeadSelectionIndex();
								boolean isAdjusting = e.getValueIsAdjusting();

								boolean loaded = false;
								CyNetwork n = networkMap.get(rowIndex);
								if (n != null) {
									CyLogger.getLogger().debug( "n != null" );

									// verify that network still exists and has not been destroyed
									if (Cytoscape.getNetworkSet().contains(n)) {
										// check to see if network has view; if not then destroy and reload!
										if (!Cytoscape.getNetworkViewMap().containsValue(Cytoscape.getNetworkView(n.getIdentifier()))){
											Cytoscape.destroyNetwork(n);
											networkMap.remove(n);
										} else {
											loaded = true;
											Cytoscape.getDesktop().setFocus(n.getIdentifier());
											// then clear selection to allow refocus
											resultsTable.clearSelection();
										}
									} else {
										networkMap.remove(n);
									}
								}

								if (isAdjusting && !loaded) {
									CyLogger.getLogger().debug( "isAdjusting && !loaded" );

									CyLogger.getLogger().debug( "isAdjusting && !loaded: rowIndex = " + rowIndex );

									String value = (String) resultsTable.getValueAt(
											rowIndex, 0);
									CyLogger.getLogger().debug( "value: " + value );
									Pattern pat = Pattern.compile(":WP");
									String[] terms = pat.split(value);
									String wp = "WP" + terms[1];
									// System.out.println(wp);
									CyLogger.getLogger().debug( "wp: " + wp );
									
									
									// Get the instance of the GPML plugin
									GpmlPlugin gp = GpmlPlugin.getInstance();
									CyLogger.getLogger().debug( "gpml plugin: " + gp );

									// if GPML plugin is loaded, then attempt load pathway
									if (null != gp) {
										Task task = new LoadPathwayTask(gp, wp, rowIndex, resultsTable);
										JTaskConfig config = new JTaskConfig();
										config.setModal(false);
										config.setOwner(Cytoscape.getDesktop());
										config.setAutoDispose(true);

										CyLogger.getLogger().debug( "executing task: " + task );

										TaskManager.executeTask(task, config);

									}

								}

							}
						};

						/*
						 * Process Pathway Results Table
						 */
						if (pathwayResultsRowData.size() > 0) {
							debugWindow.append("PathwayResults...\n");
							debugWindow.append("processing pathwayResults\n");
							resultsTable = new JTable(
									pathwayResultsRowData,
									pathwayResultsColumnNames) 
							{
								public boolean isCellEditable(int rowIndex,
										int vColIndex) 
								{
									return false;
								}
							};
							
							resultsTable.setDefaultRenderer( Object.class, new CustomTableCellRenderer( vbPathwayRowsToHighlight ) );
	
							// hide the same columns
							//int[] pathwayColumnsToHide = {0, 1, 5, 6, 7, 11,
							//		12, 13};
							int [] pathwayColumnsToHide = {};
							
							for (int j = pathwayColumnsToHide.length - 1; j >= 0; j--) {
								TableColumn column = resultsTable
										.getColumnModel().getColumn(
												pathwayColumnsToHide[j]);
								resultsTable.removeColumn(column);
							}
							debugWindow.append( "hiding columns for pathway results");
							// support row selection
							//pathwayResultsTable.setCellSelectionEnabled(true);
							ListSelectionModel tableRowSelectionModel = resultsTable
									.getSelectionModel();
							tableRowSelectionModel
									.addListSelectionListener(lsl);
							tableRowSelectionModel
									.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							resultsTable
									.setSelectionModel(tableRowSelectionModel);
							debugWindow.append( "set selection model for pathway results");
	
							// render cells to appear like hyperlinks
							
							debugWindow.append( "1\n");
							ClickableRenderer cr = new ClickableRenderer();
							cr.setToolTipText("Click to load pathway");
							debugWindow.append( "2\n");
							resultsTable.getColumnModel().getColumn(0)
									.setCellRenderer(cr);
							debugWindow.append( "3\n");
							resultsTable
									.addMouseMotionListener(new MouseMotionAdapter() {
										public void mouseMoved(MouseEvent e) {
											Point p = new Point(e.getX(), e
													.getY());
											resultsTable
													.setCursor(resultsTable
															.columnAtPoint(p) == 0
															? new Cursor(
																	Cursor.HAND_CURSOR)
															: new Cursor(
																	Cursor.DEFAULT_CURSOR));
										}
									});
	
							debugWindow.append( "4\n");	
						}
					}
					
					debugWindow.append("scrollpane...\n");

					// Schematic for nested panel:
					// GO-Elite-Results   ( resultsMasterPanel )
					// + mynetwork_0  ( resultsAnalysisNamePanel  )
					// +++ GO   ( resultsAnalysisTypePanel -- thread 1 )
					// +++++ Status   
					// +++++ Results  ( resultsWrapperPanel -- thread 1 )
					// +++ Pathway  ( resultsAnalysisTypePanel -- thread 2)
					// +++++ Status
					// +++++ Results  ( resultsWrapperPanel -- thread 2 )
					
					JScrollPane resultsScrollPane = new JScrollPane( resultsTable );

					JPanel resultsWrapperPanel = new JPanel();
					resultsWrapperPanel.setLayout( new BoxLayout( resultsWrapperPanel, BoxLayout.Y_AXIS ) );
					debugWindow.append("scrollpane 1...\n");
					
					
					if ( bRunGONotPathwayAnalysis && ( GONameResultsRowData.size() == 0 ) ||
							( !bRunGONotPathwayAnalysis && ( pathwayResultsRowData.size() == 0 ) ) )
					{
						CyLogger.getLogger().debug( "no results found" );
						
						JPanel noResultsMsg = new JPanel();
						noResultsMsg.setBackground( Color.white );
						noResultsMsg.add( new JLabel( "No results found" ) );
						noResultsMsg.setMinimumSize( new Dimension( 100, 300 ) );
						
						
						resultsWrapperPanel.add( noResultsMsg );
						resultsWrapperPanel.add( Box.createVerticalGlue() );
					}
					else
					{
						resultsWrapperPanel.add( resultsScrollPane );
					}
					
					
					JPanel optionsPanel = new JPanel();
					optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.X_AXIS ));
					optionsPanel.add( new JLabel( "# Permutations" ) );
					rerunAnalysisNumPermutations = new JTextField( "0", 3 );
					rerunAnalysisNumPermutations.setMaximumSize( rerunAnalysisNumPermutations.getPreferredSize() );
					optionsPanel.add( rerunAnalysisNumPermutations );
					
					optionsPanel.add( new JLabel( "z-score pruning method:" ) );
					rerunAnalysisZScorePruningMethod = new JComboBox( vPruningAlgorithms );
					rerunAnalysisZScorePruningMethod.setMaximumSize( rerunAnalysisZScorePruningMethod.getPreferredSize());
					optionsPanel.add( rerunAnalysisZScorePruningMethod );
					debugWindow.append("scrollpane 2...\n");

					rerunAnalysisButton = new JButton( "Rerun" );
					rerunAnalysisButton.addActionListener( rerunAnalysisActionListener );
					optionsPanel.add( rerunAnalysisButton );
					debugWindow.append("scrollpane 3...\n");

					exportButton = new JButton( "Export" );
					ExportResultsActionListener exportResultsActionListener = new ExportResultsActionListener( resultsTable );
					exportButton.addActionListener( exportResultsActionListener );
					optionsPanel.add( exportButton );
					
					resultsWrapperPanel.add( optionsPanel );
					resultsAnalysisTypePanel.addTab("Results", resultsWrapperPanel );
					
					debugWindow.append("setselectedcomponent resultsWrapperPanel...\n");
					
					resultsMasterPanel.setSelectedComponent( resultsAnalysisNamePanel );					
					resultsAnalysisNamePanel.setSelectedComponent( resultsAnalysisTypePanel );
                    resultsAnalysisTypePanel.setSelectedComponent( resultsWrapperPanel );
                    
					CommandHandler.changeResultStatus(resultName, true); 
					debugWindow.append("change result tab idx...\n");
					CommandHandler.changeResultTabIndex( resultName, bRunGONotPathwayAnalysis ? "GO" : "Pathway" ); 
					debugWindow.append("log file...\n");

					/*
					 * Process log file
					 */
					for (int j = 0; j < logFileContents.size(); j++) {
						statusWindow.append(logFileContents.elementAt(j)
								+ "\n");
					}
					
					debugWindow.append("stdout...\n");

					/*
					 * Process stdout and stderr file
					 */
					if (stdoutFileContents.size() > 0) {
						debugWindow.append("process stdout\n");

						JPanel stdoutPanel = new JPanel();
						stdoutPanel.setLayout(new BoxLayout(stdoutPanel,
								BoxLayout.PAGE_AXIS));
						stdoutWindow = new JTextArea("", 15, 80);
						JScrollPane stdoutScroll = new JScrollPane(
								stdoutWindow);
						stdoutPanel.add(stdoutScroll);

						resultsAnalysisTypePanel.addTab("Stdout", stdoutPanel);

						for (int j = 0; j < stdoutFileContents.size(); j++) {
							stdoutWindow.append(stdoutFileContents
									.elementAt(j)
									+ "\n");
						}	     
					}

					debugWindow.append("stderr...\n");

					if (stderrFileContents.size() > 0) {
						debugWindow.append("process stderr\n");

						JPanel stderrPanel = new JPanel();
						stderrPanel.setLayout(new BoxLayout(stderrPanel,
								BoxLayout.PAGE_AXIS));
						stderrWindow = new JTextArea("", 15, 80);
						JScrollPane stderrScroll = new JScrollPane(
								stderrWindow);
						stderrPanel.add(stderrScroll);

						resultsAnalysisTypePanel.addTab("Stderr", stderrPanel);

						for (int j = 0; j < stderrFileContents.size(); j++) {
							stderrWindow.append(stderrFileContents
									.elementAt(j)
									+ "\n");
						}
			            CommandHandler.changeResultStatus(resultName, false ); 
		                CommandHandler.changeResultTabIndex(resultName, "Stderr" ); 
						resultsAnalysisTypePanel.setSelectedComponent( stderrPanel );                    

					}

				} // if... end
				
			} catch (java.net.MalformedURLException e) {
				debugWindow.append("error:" + e );
				statusWindow.append("exception: " + e );

				Utilities.showError(
						"malformed URL while fetching results: ", e);
			} catch (java.io.IOException e) {
				debugWindow.append("error:" + e);
				statusWindow.append( "exception: " + e );
				Utilities.showError(
						"IO exception while fetching results: ", e);
			} catch( Exception e  )
			{
				debugWindow.append( "Exception: " + e + "\n");
				statusWindow.append( "Exception: " + e );
			}
			// try...catch...end

		} 
	};
	
	
	@SuppressWarnings("unchecked")
	public Set<String> guessIdType( String sampleID )
	{
		CyLogger.getLogger().debug( "guessIDType: " + sampleID );
		Set<String> idTypes = new HashSet<String>();
		if ( sampleID == null ) { return idTypes; }
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", sampleID );
		
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping", "guess id type", args);
			 idTypes = (Set<String>) result.getResult();
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

	private void updatePrimaryIDSystemComboBoxes( String primaryIDCol, JComboBox comboBox ) 
	{
		CyLogger.getLogger().debug( "updatePrimaryIDSystemComboBoxes " + primaryIDCol + " " + comboBox );
		
		// look in current network, grab first row of node attributes
		// look for 
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		
		// get a node, any node from the current network
		if ( Cytoscape.getCurrentNetwork().nodesList().size() == 0 ) { return; }
		
		
		Node node = ( Node ) Cytoscape.getCurrentNetwork().nodesList().get( 0 );
		CyLogger.getLogger().debug( "got node: " + node.getIdentifier() );
		
		String sampleID = "";
		if ( primaryIDCol.equals( "ID" ) )
		{
			// "ID" is not technically stored the same way as the other attributes, it is not a 'node attribute' tho
			//    it is listed in the node attributes spreadsheet
			sampleID = node.getIdentifier();
		}
		else
		{
			sampleID = ( String ) nodeAttributes.getAttribute( node.getIdentifier(), primaryIDCol );
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
	
	static String vGeneSystems[] = {new String("Ensembl Yeast"),
			new String("Entrez Gene"), new String("SGD"),
			new String("Tuberculist"), new String("Affy"), new String( "???" ) };
	
	// MOD for mapping to GO
	static String vGeneSystemMODS[] = {new String("Ensembl"),
			new String("EntrezGene"), new String("Ensembl"),
			new String("Ensembl"), new String("Ensembl")};
	
	static String vMODIDSystems[] = {new String( "Ensembl"), new String( "EntrezGene" )};
	// Code for input files
	static String vGeneSystemCodes[] = {new String("En"), new String("L"),
			new String("D"), new String("Tb"), new String("X")};
	static String vPruningAlgorithms[] = {new String("z-score"),
			new String("\"gene number\""), new String("combination")};
	
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

	static Map<Integer, CyNetwork> networkMap = new HashMap<Integer, CyNetwork>();




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
	void launchJob(final String geneListFilePath, final String denomFilePath,
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

		
		debugWindow.append("launchJob start\n");
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
		  debugWindow.append( "prefix = " + geneListFilePrefix + "\n" );
		}
		else
		{
			// throw error
			CyLogger.getLogger().error( "gene list file must end in .txt" );
			return;
		}

        resultName = geneListFilePrefix; 
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
     			new InputDialogWorker( geneListFilePath, denomFilePath, false, pathwayPanel, overrideNumPermutations, overrideZScorePruningMethod );
     		InputDialogWorker GOAnalysisWorker = 
     			new InputDialogWorker( geneListFilePath, denomFilePath, true, GOPanel, overrideNumPermutations, overrideZScorePruningMethod );
     		debugWindow.append("executing pathway worker\n");
     		pathwayAnalysisWorker.execute();
     		debugWindow.append("executing GO Analysis worker\n");
     		GOAnalysisWorker.execute();

     		debugWindow.append("done executing workers\n");
        }
        else
        {
        	// just reruns one of either GO/Pathway analysis
    		analysisTypePanelToReuse.removeAll();
    		CyLogger.getLogger().debug( "creating analysisWorker: " + bRunGONotPathway + " " + analysisTypePanelToReuse + " " + 
    				overrideNumPermutations + " " + overrideZScorePruningMethod + "\n");
    		InputDialogWorker analysisWorker = 
     			new InputDialogWorker( geneListFilePath, denomFilePath, bRunGONotPathway,
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
	 public class CustomTableCellRenderer extends DefaultTableCellRenderer
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
			 return cell;
		 }
	 }

}