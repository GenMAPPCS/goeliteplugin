package org.genmapp.goelite;

import javax.swing.JOptionPane;

import java.awt.Color;
import java.awt.Component;
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
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.generated.Child;
import cytoscape.generated.Network;

import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

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
	InputDataType inputDataType = InputDataType.FILE;
	String criteriaAllStringValue = "-all-"; // selection value for the criteria
	// "all"
	JButton launchButton = null;
	JButton generateInputFileButton = null;
	JTextArea debugWindow = null;
	public final static long serialVersionUID = 0;
	AppServicePortType service = null;
	LayoutProperties layoutProperties = null;
	JLabel inputDenomFilenameDescriptor = null,
			inputNumeratorFilenameDescriptor = null,
			inputCriteriaKeyAttributeDescriptor = null;
	JLabel inputDenomFilenameLabel, inputNumeratorFilenameLabel;
	JTextArea inputNumerFileJTextArea = null, inputDenomFileJTextArea = null;
	JButton inputNumerFileBrowseButton = null,
			inputDenomFileBrowseButton = null;
	static File browseButtonLastFileSelected = null;
	JComboBox inputCriteriaSetComboBox = null, inputCriteriaComboBox = null,
			inputCriteriaKeyAttributeComboBox = null;
	JLabel inputCriteriaDescriptor = null, inputCriteriaSetDescriptor = null;
	
	JComboBox inputNetworkNumeratorComboBox = null, inputNetworkDenominatorComboBox = null,
		inputNetworkKeyAttributeComboBox = null;
	JLabel inputNetworkNumeratorDescriptor = null, inputNetworkDenominatorDescriptor = null, 	inputNetworkKeyAttributeDescriptor = null;

	String resultName = null;
	
	// prepare input files
	String geneListFilePath = "";
	String denomFilePath = "";
	JTabbedPane resultsAnalysisNamePanel = null;
	
	class InputDialogWorker extends SwingWorker<StatusOutputType, Void>
	{
	
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
		boolean bRunGONotPathwayAnalysis = false;
		public InputDialogWorker( boolean bRunGONotPathwayAnalysis_, CloseableTabbedPane resultsAnalysisTypePanel_, 
				int numPermutations_, String zScorePruningMethod_ )
		{
			bRunGONotPathwayAnalysis = bRunGONotPathwayAnalysis_;

			// this stuff is common to both pathway and GO analyses for the given job
			// **** Prepare results pane 			
			resultsAnalysisTypePanel = resultsAnalysisTypePanel_;
			overrideNumPermutations = numPermutations_;
			overrideZScorePruningMethod = zScorePruningMethod_;
		}
		
		public StatusOutputType doInBackground() {
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

				// make sure everything is in String format
				args.put(WebService.ARG_SPECIES, vSpecies[(new Integer(
						layoutProperties.getValue("species_idx_code"))
						.intValue())]);
				args.put(WebService.ARG_MOD_ID_SYSTEM,
						vGeneSystemMODS[new Integer(layoutProperties
								.getValue("gene_system_idx_code"))
								.intValue()]);
				args.put(WebService.ARG_ID_SYSTEM,
						vGeneSystemCodes[new Integer(layoutProperties
								.getValue("gene_system_idx_code"))
								.intValue()]
								+ " ("
								+ vGeneSystems[new Integer(layoutProperties
										.getValue("gene_system_idx_code"))
										.intValue()] + ")");
				int numPermutations = ( overrideNumPermutations == -1 ?
						new Integer( layoutProperties.getValue("num_permutations") ).intValue() : overrideNumPermutations );
				args.put(WebService.ARG_NUM_PERMUTATIONS, 
						"" + numPermutations );
				args.put(WebService.ARG_PRUNING_ALGORITHM,
						overrideZScorePruningMethod.length() == 0 ?
						vPruningAlgorithms[new Integer(layoutProperties
								.getValue("go_pruning_algorithm"))
								.intValue()] :
						  overrideZScorePruningMethod );
				args.put(WebService.ARG_ANALYSIS_TYPE,
						( bRunGONotPathwayAnalysis ? "GeneOntology" : "Pathways" ) );
				args.put(WebService.ARG_PVAL_THRESH, new String(
						layoutProperties.getValue("p-value_thresh")));
				args.put(WebService.ARG_MIN_GENES_CHANGED, new String(
								layoutProperties
										.getValue("min_num_genes_changed")));
				args.put(WebService.ARG_ZSCORE_THRESH, new String(
						layoutProperties.getValue("z-score_thresh")));
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
			
			System.out.println("done!");
			debugWindow.append("done! status = " + status + "\n");
			try {
			
			

				// print results in results panel
				if (status.getCode() == 8) {
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
					
					resultsWrapperPanel.add( resultsScrollPane );
					JPanel optionsPanel = new JPanel();
					optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.X_AXIS ));
					optionsPanel.add( new JLabel( "# Permutations" ) );
					rerunAnalysisNumPermutations = new JTextField( "0", 3 );
					optionsPanel.add( rerunAnalysisNumPermutations );
					
					optionsPanel.add( new JLabel( "z-score pruning method:" ) );
					rerunAnalysisZScorePruningMethod = new JComboBox( vPruningAlgorithms );
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


	
	static String[] vSpecies = {};
	static String vGeneSystems[] = {new String("Ensembl"),
			new String("EntrezGene"), new String("SGD"),
			new String("TubercuList"), new String("Affymetrix")};
	// MOD for mapping to GO
	static String vGeneSystemMODS[] = {new String("Ensembl"),
			new String("EntrezGene"), new String("Ensembl"),
			new String("Ensembl"), new String("Ensembl")};
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

	private JTable pathwayResultsTable;

	static Map<Integer, CyNetwork> networkMap = new HashMap<Integer, CyNetwork>();

	public InputDialog() {

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

		layoutProperties = new LayoutProperties("go-elite");
		String systemCodeInData = Cytoscape.getNetworkAttributes().getStringAttribute( 
				Cytoscape.getCurrentNetwork().getIdentifier(),	"SystemCode" );
		int systemCodeDefaultIdx = 0;
		if ( systemCodeInData != null )
		{
			for( int i =0; i < vGeneSystems.length; i++ )
			{
				if ( vGeneSystems[ i ].equals( systemCodeInData ) )
				{
					systemCodeDefaultIdx = i;
				}
			}
		}
		layoutProperties.add(new Tunable("species_idx_code",
				"Species to analyze", Tunable.LIST, new Integer(
						defaultSpeciesIdx), (Object) getSpeciesCodes(),
				(Object) null, 0));
		layoutProperties.add(new Tunable("gene_system_idx_code",
				"Primary identifier", Tunable.LIST, new Integer(0),
				(Object) vGeneSystems, (Object) null, 0));
		layoutProperties.add(new Tunable("num_permutations",
				"Number of permutations", Tunable.INTEGER, new Integer(0)));
		layoutProperties.add(new Tunable("go_pruning_algorithm",
				"GO pruning algorithm", Tunable.LIST, new Integer(0),
				(Object) vPruningAlgorithms, (Object) null, 0));
		layoutProperties.add(new Tunable("z-score_thresh", "Z-score threshold",
				Tunable.DOUBLE, new Double(1.96)));
		layoutProperties.add(new Tunable("p-value_thresh", "P-value threshold",
				Tunable.DOUBLE, new Double(0.05)));
		layoutProperties.add(new Tunable("min_num_genes_changed",
				"Minimum genes changed", Tunable.INTEGER, new Integer(3)));

		final JPanel panel = layoutProperties.getTunablePanel();

		JPanel inputSourcePanel = new JPanel();
		inputSourcePanel.add(new JLabel("Input Source "));

		ButtonGroup inputButtonsGroup = new ButtonGroup();
		final JRadioButton inputSourceFileRadioButton = new JRadioButton("File");
		final JRadioButton inputSourceCriteriaRadioButton = new JRadioButton(
				"Criteria");
		final JRadioButton inputSourceNetworkRadioButton = new JRadioButton( "Network" );
		inputSourcePanel.add(inputSourceFileRadioButton);
		inputSourcePanel.add(inputSourceCriteriaRadioButton);
		inputSourcePanel.add(inputSourceNetworkRadioButton );
		inputButtonsGroup.add(inputSourceFileRadioButton);
		inputButtonsGroup.add(inputSourceCriteriaRadioButton);
		inputButtonsGroup.add(inputSourceNetworkRadioButton);
		
		final JPanel inputSourceExpandingPanel = new JPanel();
		inputSourceExpandingPanel.setLayout(new BoxLayout(
				inputSourceExpandingPanel, BoxLayout.PAGE_AXIS));

		inputSourceExpandingPanel.setSize(300, 500);

		ActionListener inputSourceActionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				if (e.getSource() == inputSourceFileRadioButton) {

					inputDataType = InputDataType.FILE;
					
					inputSourceExpandingPanel.removeAll();
					inputNumeratorFilenameDescriptor = new JLabel("Numerator");
					inputSourceExpandingPanel
							.add(inputNumeratorFilenameDescriptor);
					inputNumerFileJTextArea = new JTextArea("");
					Dimension d = inputNumerFileJTextArea.getPreferredSize();
					inputNumerFileJTextArea.setMinimumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputNumerFileJTextArea);
					inputNumerFileBrowseButton = new JButton("Browse...");
					d = inputNumerFileBrowseButton.getPreferredSize();
					inputNumerFileBrowseButton.setMinimumSize(new Dimension(
							300, d.height));
					inputSourceExpandingPanel.add(inputNumerFileBrowseButton);

					inputDenomFilenameDescriptor = new JLabel("Denominator");
					inputSourceExpandingPanel.add(inputDenomFilenameDescriptor);
					inputDenomFileJTextArea = new JTextArea("");
					d = inputDenomFileJTextArea.getPreferredSize();
					inputDenomFileJTextArea.setMinimumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputDenomFileJTextArea);
					inputDenomFileBrowseButton = new JButton("Browse...");
					d = inputDenomFileBrowseButton.getPreferredSize();
					inputDenomFileBrowseButton.setMinimumSize(new Dimension(
							300, d.height));
					inputSourceExpandingPanel.add(inputDenomFileBrowseButton);

					inputNumerFileBrowseButton.addActionListener(this);
					inputDenomFileBrowseButton.addActionListener(this);

				} else if (e.getSource() == inputSourceCriteriaRadioButton) {
					inputDataType = InputDataType.CRITERIA;
					inputSourceExpandingPanel.removeAll();
					String[] criteriaSet = GOElitePlugin
							.getCriteriaSets(debugWindow);
					String[] criteria = {};
					inputCriteriaSetDescriptor = new JLabel("Criteria Set:");
					inputSourceExpandingPanel.add(inputCriteriaSetDescriptor);
					inputCriteriaSetComboBox = new JComboBox();
					if (criteriaSet.length > 0) {
						inputCriteriaSetComboBox.addItem("");
					}
					for (String c : criteriaSet) {
						inputCriteriaSetComboBox.addItem(c);
					}
					Dimension d = inputCriteriaSetComboBox.getPreferredSize();
					inputCriteriaSetComboBox.setMaximumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputCriteriaSetComboBox);

					inputCriteriaDescriptor = new JLabel("Criteria: ( 0 )");
					inputSourceExpandingPanel.add(inputCriteriaDescriptor);
					inputCriteriaComboBox = new JComboBox(criteria);
					d = inputCriteriaComboBox.getPreferredSize();
					inputCriteriaComboBox.setMaximumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputCriteriaComboBox);

					inputCriteriaSetComboBox.addActionListener(this);
					inputCriteriaComboBox.addActionListener(this);

					debugWindow.append("preparing attribute names\n");
					CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
					debugWindow.append("2\n");
					String[] attributeNames = nodeAttributes
							.getAttributeNames();
					debugWindow.append("3\n");

					for (String n : attributeNames) {
						debugWindow.append("name: " + n + "\n");
					}
					inputCriteriaKeyAttributeDescriptor = new JLabel(
							"Key attribute:");
					inputCriteriaKeyAttributeComboBox = new JComboBox();
					inputCriteriaKeyAttributeComboBox.insertItemAt("ID", 0);
					for (String n : attributeNames) {
						if (CyAttributes.TYPE_STRING == nodeAttributes
								.getType(n)) {
							inputCriteriaKeyAttributeComboBox.addItem(n);
						}
					}
					inputCriteriaKeyAttributeComboBox.setSelectedIndex(0);

					inputSourceExpandingPanel
							.add(inputCriteriaKeyAttributeDescriptor);
					inputSourceExpandingPanel
							.add(inputCriteriaKeyAttributeComboBox);
				} else if ( e.getSource() == inputSourceNetworkRadioButton )
				{
					debugWindow.append( "1\n");
					inputDataType = InputDataType.NETWORK;
					
					inputSourceExpandingPanel.removeAll();
					ArrayList< String > numerators = new ArrayList< String >();
					numerators.add( "selected nodes" );
					inputNetworkNumeratorDescriptor = new JLabel("Network Numerator:");
					inputSourceExpandingPanel.add(inputNetworkNumeratorDescriptor);

					inputNetworkNumeratorComboBox = new JComboBox();

					Dimension d = inputNetworkNumeratorComboBox.getPreferredSize();
					inputNetworkNumeratorComboBox.setMaximumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputNetworkNumeratorComboBox);

					inputNetworkDenominatorDescriptor = new JLabel("Denominator:");
					inputSourceExpandingPanel.add(inputNetworkDenominatorDescriptor);
					String [] availableNetworks = new String[ Cytoscape.getNetworkSet().size() ];
					int i = 0;
					for ( CyNetwork n : Cytoscape.getNetworkSet() )
					{
						availableNetworks[ i ] = n.getIdentifier() + " " + n.getTitle();
						i++;
					}

					inputNetworkDenominatorComboBox = new JComboBox( availableNetworks );
					d = inputNetworkDenominatorComboBox.getPreferredSize();
					inputNetworkDenominatorComboBox.setMaximumSize(new Dimension(300,
							d.height));
					inputSourceExpandingPanel.add(inputNetworkDenominatorComboBox);

					debugWindow.append( "6\n");
					for (String n : numerators ) 
					{
						inputNetworkNumeratorComboBox.addItem( n );
					}
					debugWindow.append( "7\n");

					
					inputNetworkNumeratorComboBox.addActionListener(this);
					inputNetworkDenominatorComboBox.addActionListener(this);

					inputNetworkKeyAttributeDescriptor = new JLabel(
							"Key attribute:");
					inputNetworkKeyAttributeComboBox = new JComboBox();
					inputNetworkKeyAttributeComboBox.insertItemAt("ID", 0);
					inputNetworkKeyAttributeComboBox.setSelectedIndex(0);

					inputSourceExpandingPanel
							.add(inputNetworkKeyAttributeDescriptor);
					inputSourceExpandingPanel
							.add(inputNetworkKeyAttributeComboBox);
					
				} else if (e.getSource() == inputNumerFileBrowseButton
						|| e.getSource() == inputDenomFileBrowseButton) {
					JFileChooser chooser = (null != browseButtonLastFileSelected
							? new JFileChooser(browseButtonLastFileSelected)
							: new JFileChooser());
					int returnVal = chooser.showOpenDialog(panel);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						browseButtonLastFileSelected = chooser
								.getSelectedFile();
						try {
							if (e.getSource() == inputNumerFileBrowseButton) {
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								if (new File(filePath).exists()) {
									inputNumerFileJTextArea.setText(filePath);
									inputNumerFileJTextArea.setColumns(20);
									inputNumeratorFilenameDescriptor
											.setText("Numerator: ("
													+ Utilities
															.countLinesInFile(filePath)
													+ ")");
									inputNumeratorFilenameDescriptor
											.setAlignmentX(Component.CENTER_ALIGNMENT);
								}
							} else {
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								if (new File(filePath).exists()) {
									inputDenomFileJTextArea.setText(filePath);
									inputDenomFileJTextArea.setColumns(20);
									inputDenomFilenameDescriptor
											.setText("Denominator: ("
													+ Utilities
															.countLinesInFile(filePath)
													+ ")");
									inputDenomFilenameDescriptor
											.setAlignmentX(Component.CENTER_ALIGNMENT);
								}
							}
						} catch (java.io.IOException exception) {

						} 
					}
					pack();
				} else if (e.getSource() == inputCriteriaSetComboBox) {
					debugWindow
							.append("event caught: inputCriteriaSetComboBox ");
					// based on selection, update criteria choices
					String selectedCriteriaSet = (String) inputCriteriaSetComboBox
							.getSelectedItem();
					String[] newCriteria = GOElitePlugin
							.getCriteria((String) inputCriteriaSetComboBox
									.getSelectedItem(), debugWindow);
					inputCriteriaComboBox.removeAllItems();
					inputCriteriaComboBox.addItem(criteriaAllStringValue);
					for (int i = 0; i < newCriteria.length; i++) {
						inputCriteriaComboBox.addItem(newCriteria[i]);
					}
					pack();
				} 
				else if ( e.getSource() == inputNetworkDenominatorComboBox )
				{
					debugWindow.append( "7\n");

					String selectedNetworkID = ( String ) inputNetworkDenominatorComboBox.getSelectedItem();
					CyNetwork selectedNetwork = Cytoscape.getNetwork( selectedNetworkID );
					ArrayList< String > numerators = new ArrayList< String >();
					numerators.add( "selected nodes" );
					
					// add subnetworks of selected denominator network if applicable
					if ( null != selectedNetwork )
					{
						debugWindow.append( "8\n" );
						List<Child> children = ((Network) selectedNetwork).getChild();
						for (Child ch : children) {
							debugWindow.append( "9\n");

						    numerators.add( ch.getId() );
						}
					}
					inputNetworkNumeratorComboBox.removeAllItems();
					for (String n : numerators ) 
					{
						inputNetworkNumeratorComboBox.addItem( n );
					}										
					debugWindow.append( "10\n");

				}
				if (e.getSource() == inputCriteriaSetComboBox
						|| e.getSource() == inputCriteriaComboBox) {
					debugWindow
							.append("event caught: inputCriteriaSetComboBox or inputCriteriaComboBox\n ");
					try {
						// update the denominator label to count up number of
						// hits
						String selectedCriteriaSet = (String) inputCriteriaSetComboBox
								.getSelectedItem();
						String selectedCriteria = (String) inputCriteriaComboBox
								.getSelectedItem();

						debugWindow.append("counting hits for criteria "
								+ selectedCriteriaSet + " \\ "
								+ selectedCriteria + "\n");
						long numHits = 0;
						long numTotal = 0;
						if (((String) selectedCriteria)
								.equals(criteriaAllStringValue)) {
							inputCriteriaDescriptor.setText("Criteria: ( - )");

						} else {
							long[] nums = GOElitePlugin
									.generateInputFileFromNetworkCriteria(
											"",
											"",
											(String) selectedCriteriaSet,
											(String) inputCriteriaComboBox
													.getSelectedItem(),
											true,
											false,
											(String) inputCriteriaKeyAttributeComboBox
													.getSelectedItem(),
											debugWindow);
							numHits = nums[0];
							numTotal = nums[1];
							debugWindow.append("setting label: " + numHits
									+ " out of " + numTotal + "\n:");
							inputCriteriaDescriptor.setText("Criteria: ("
									+ numHits + "/" + numTotal + ")");
						}

					} catch (java.io.IOException except) {
						Utilities
								.showError(
										"I/O Error: Couldn't generate temporary files for criteriaset/criteria.  Check disk space/permissions",
										except);
					}
				}

				panel.revalidate();
			}
		};

		inputSourceFileRadioButton.addActionListener(inputSourceActionListener);
		inputSourceCriteriaRadioButton
				.addActionListener(inputSourceActionListener);
		inputSourceFileRadioButton.setSelected(true);
		inputSourceNetworkRadioButton
		.addActionListener(inputSourceActionListener);

		// invoke action to update panels
		inputSourceActionListener.actionPerformed(new ActionEvent(
				inputSourceFileRadioButton, 0, null));
		panel.add(inputSourcePanel);
		panel.add(inputSourceExpandingPanel);

		launchButton = new JButton("Run analysis");
		panel.add(launchButton);
		launchButton.addActionListener(this);

		generateInputFileButton = new JButton("Generate Input File");
		// panel.add( generateInputFileButton );
		generateInputFileButton.addActionListener(this);

		debugWindow = new JTextArea("", 5, 40);
		panel.add(new JScrollPane(debugWindow));
		panel.setVisible(true);

		panel.setMinimumSize(new Dimension(300, 600));
		panel.setPreferredSize(new Dimension(300, 600));
		// panel.setMaximumSize(new Dimension(200,800));

		this.add(panel);
		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setLocationRelativeTo(Cytoscape.getDesktop());
		this.pack();

	}


	public void actionPerformed(ActionEvent evt_) 
	{

		debugWindow.append("evt source: " + evt_.getSource() + "\n");
		debugWindow.append("launchButton: " + launchButton + "\n");
		debugWindow.append("genInputFileButton: " + generateInputFileButton
				+ "\n");

		if (evt_.getSource() != launchButton) {
			debugWindow.append("early return\n ");
			return;
		}
		
		debugWindow.append("launched a job request ");
		String pluginDir = null;
		try 
		{
			debugWindow.append("1\n");
			layoutProperties.updateValues(); // must do this to refresh contents
			// of the Tunables before we
			// read from them


			if ( InputDataType.NETWORK == inputDataType || InputDataType.CRITERIA == inputDataType ) {
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

				String systemCode = vGeneSystemCodes[new Integer(
						layoutProperties.getValue("gene_system_idx_code"))
						.intValue()];
				String systemCodeInData = Cytoscape.getNetworkAttributes().getStringAttribute( Cytoscape.getCurrentNetwork().getIdentifier(),
						"SystemCode" );
				if ( !systemCode.equals( systemCodeInData ) && !systemCodeInData.equals( "MIXED" ) )
				{
// XXX
					// user error: abort
			//		JOptionPane.showMessageDialog( null, "The primary identifier system code you selected [" + systemCode + "] does not match the data [ " + 
			//				systemCodeInData + "]" );
			//		return;
				}
				
				debugWindow.append("4\n");
			
				if ( InputDataType.CRITERIA == inputDataType )
				{
					String selectedCriteriaSet = (String) inputCriteriaSetComboBox
					.getSelectedItem();
					String selectedCriteria = (String) inputCriteriaComboBox
							.getSelectedItem();
					String[] criteriaList = new String[]{selectedCriteria};
		
					if (((String) inputCriteriaComboBox.getSelectedItem())
							.compareTo(criteriaAllStringValue) == 0) {
						debugWindow.append("-all- found: get list of criteria ");
						// user chose "-all-" criteria: launch a series of jobs, one
						// per criteria in the criteriaSet
						criteriaList = GOElitePlugin.getCriteria(
								selectedCriteriaSet, debugWindow);
					}

					for (String criteria : criteriaList) {
						String geneListFileName = criteria;
						geneListFilePath = pluginDir + "/" + geneListFileName;
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
								(String) inputCriteriaKeyAttributeComboBox
										.getSelectedItem(), debugWindow);
	
						String denomFileName = criteria + "_denom";
						denomFilePath = pluginDir + "/" + denomFileName;
						debugWindow.append("generating input denominator\n");
						denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
								+ denomFileName + ".txt");
						GOElitePlugin.generateInputFileFromNetworkCriteria(
								denomFilePath, systemCode,
								(String) selectedCriteriaSet, (String) criteria,
								false, true,
								(String) inputCriteriaKeyAttributeComboBox
										.getSelectedItem(), debugWindow);
	
						launchJob(geneListFilePath, denomFilePath);
					}
				} 
				else if ( InputDataType.NETWORK == inputDataType )
				{
					debugWindow.append("5\n");


					// this is the network the user selected as the denominator
					String networkIDPlusTitle = ( String ) inputNetworkDenominatorComboBox.getSelectedItem();
					String networkID = networkIDPlusTitle.split( "\\s+" )[ 0 ];
					CyNetwork network = Cytoscape.getNetwork( networkID );

					String geneListFileName = network.getTitle();
					geneListFilePath = pluginDir + "/" + geneListFileName;
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
							true, ( String )inputNetworkKeyAttributeComboBox.getSelectedItem(), debugWindow);
					CyLogger.getLogger().debug( "6" );
					debugWindow.append("file generated for " + geneListFilePath + "\n");

					
					// for denominator, use all nodes in the selected network
					Set< Node > denomNodes = new java.util.HashSet<Node>( network.nodesList() );
					String denomFileName =  "network_denom";
					denomFilePath = pluginDir + ")/" + denomFileName;
					debugWindow.append("generating in)put denominator\n");
					denomFilePath = Utilities.generateUniqueFilename(pluginDir + "/"
							+ denomFileName);
					GOElitePlugin.generateInputFileFromNodeSet(
							denomFilePath, systemCode,
							denomNodes,
							true, ( String ) inputNetworkKeyAttributeComboBox.getSelectedItem(),
							debugWindow);
					debugWindow.append("file generated for " + denomFilePath + "\n");

					launchJob(geneListFilePath, denomFilePath);
					debugWindow.append("launchJob() done\n");
				}		
			}
			else 
			{
				geneListFilePath = inputNumerFileJTextArea.getText();
				denomFilePath = inputDenomFileJTextArea.getText();

				launchJob(geneListFilePath, denomFilePath);

			}
			debugWindow.append("2>" + service + "\n");
			//this.dispose();  // close the InputDialog at this point
			
		} catch (IOException e) {
			if (pluginDir == null || pluginDir.length() == 0) {
				Utilities
						.showError(
								"Could not get canonical path from plugin manager directory",
								e);
			} else {
				Utilities.showError(
						"Could not generate input files from network criteria:"
								+ e.getMessage(), e);
			}
		}
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
			resultsAnalysisNamePanel = new CloseableTabbedPane(); 
			CloseableTabbedPane pathwayPanel = new CloseableTabbedPane();
			CloseableTabbedPane GOPanel = new CloseableTabbedPane();
			resultsAnalysisNamePanel.add( "GO", GOPanel );
			resultsAnalysisNamePanel.add( "Pathway", pathwayPanel );
			resultsMasterPanel.add( resultName, resultsAnalysisNamePanel );  // should be name of network/file
			CyLogger.getLogger().debug( "3" );
		
	    	debugWindow.append( "cytopanel contents:\n");
	    	for( int i =0; i<resultsMasterPanel.getTabCount();i++ )
	    	{
	    		debugWindow.append( resultsMasterPanel.getTitleAt(i) + "\n" );
	    	}

	    	CommandHandler.updateResultsPanel(resultName, false, "GO-Elite Results", resultName, geneListFilePath );

        
     		InputDialogWorker pathwayAnalysisWorker = 
     			new InputDialogWorker( false, pathwayPanel, overrideNumPermutations, overrideZScorePruningMethod );
     		InputDialogWorker GOAnalysisWorker = 
     			new InputDialogWorker( true, GOPanel, overrideNumPermutations, overrideZScorePruningMethod );
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
     			new InputDialogWorker( bRunGONotPathway,
     					analysisTypePanelToReuse, overrideNumPermutations, overrideZScorePruningMethod );

    		CyLogger.getLogger().debug( "executing analysisWorker\n");

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
	/*
	 * Listener for row selection in Pathway results table. Useful for
	 * responding to clicks on pathway results.
	 */
	ListSelectionListener lsl = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();

			int rowIndex = lsm.getLeadSelectionIndex();
			boolean isAdjusting = e.getValueIsAdjusting();

			boolean loaded = false;
			CyNetwork n = networkMap.get(rowIndex);
			if (n != null) {

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
						pathwayResultsTable.clearSelection();
					}
				} else {
					networkMap.remove(n);
				}
			}

			if (isAdjusting && !loaded) {
				String value = (String) pathwayResultsTable.getValueAt(
						rowIndex, 0);
				Pattern pat = Pattern.compile(":WP");
				String[] terms = pat.split(value);
				String wp = "WP" + terms[1];
				// System.out.println(wp);
				// Get the instance of the GPML plugin
				GpmlPlugin gp = GpmlPlugin.getInstance();

				// if GPML plugin is loaded, then attempt load pathway
				if (null != gp) {
					Task task = new LoadPathwayTask(gp, wp, rowIndex, pathwayResultsTable);
					JTaskConfig config = new JTaskConfig();
					config.setModal(false);
					config.setOwner(Cytoscape.getDesktop());
					config.setAutoDispose(true);
					TaskManager.executeTask(task, config);

				}

			}

		}
	};

}