package org.genmapp.goelite;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.genmapp.goelite.InputDialog.CustomTableCellRenderer;
import org.genmapp.goelite.InputDialog.InputDataType;
import org.jdesktop.swingworker.SwingWorker;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.logger.CyLogger;
import cytoscape.view.cytopanels.CytoPanel;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;

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

	// go-elite user parameters:  these override the ones selected in the dialog 
	int overrideNumPermutations = -1;   // -1 will default to using the dialog selection
	String overrideZScorePruningMethod = "";
	String resultName = "";

	// defaults
	int defaultMinNumGenesChanged = 3, defaultNumPermutations = 0;
	String defaultZScorePruningMethod = "z-score";
	double defaultPValueThresh = 0.05;
	double defaultZScoreThresh = 1.96;
	InputDialog inputDialog = null;
	boolean bRunGONotPathwayAnalysis = false;
	URL[] vResultURL = null;

	// must synchronize access to:
	// resultsAnalysisTypePanel
	//resultsMasterPanel.setSelectedComponent( resultsAnalysisNamePanel );
	//resultsAnalysisNamePanel.setSelectedComponent( resultsAnalysisTypePanel );
	//vSpecies? ( or make it final )

	public InputDialogWorker( InputDialog inputDialog_, String resultName_, String geneListFilePath_, String denomFilePath_, boolean bRunGONotPathwayAnalysis_, CloseableTabbedPane resultsAnalysisTypePanel_, 
			int numPermutations_, String zScorePruningMethod_ )
	{
		inputDialog = inputDialog_;
		geneListFilePath = geneListFilePath_;  denomFilePath = denomFilePath_; resultName = resultName_;

		CyLogger.getLogger().debug( geneListFilePath + " constructor called");

		bRunGONotPathwayAnalysis = bRunGONotPathwayAnalysis_;

		// this stuff is common to both pathway and GO analyses for the given job
		// **** Prepare results pane 			
		resultsAnalysisTypePanel = resultsAnalysisTypePanel_;
		if ( resultsAnalysisTypePanel == null )
		{
			CyLogger.getLogger().error( "resultsAnalysisTypePanel is null ( constructor )" );
		}
		overrideNumPermutations = numPermutations_;
		overrideZScorePruningMethod = zScorePruningMethod_;
		CyLogger.getLogger().debug( geneListFilePath + " constructor done ");

	}

	// this stuff in here will run in a separate thread:  beware thread-safety issues!
	// Note: e.g., CyLogger doesn't appear to be thread-safe, hence the commenting out
	public StatusOutputType doInBackground() 
	{
		{
			try {

				JobInputType launchJobInput = new JobInputType();

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() start ");

				JPanel statusPanel = new JPanel();

				statusPanel.setLayout(new BoxLayout(statusPanel,
						BoxLayout.PAGE_AXIS));

				statusWindow = new JTextArea("", 15, 80);
				JScrollPane statusScroll = new JScrollPane(statusWindow);
				statusPanel.add(statusScroll);

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 1 ");
				resultsAnalysisTypePanel.addTab("Status", statusPanel);

				Runnable setSelectedComponentForResultsMasterPanel =  
					new Runnable() {
					public void run() 
					{ 
						InputDialog.getResultsMasterPanel().setSelectedComponent( InputDialog.getResultsAnalysisNamePanel() );
						InputDialog.getResultsAnalysisNamePanel().setSelectedComponent( resultsAnalysisTypePanel );
					}
				};

				resultsAnalysisTypePanel.setSelectedComponent( statusPanel );

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 2 ");

				int numTries = 0;
				while( service == null )
				{
					try
					{
						service = WebService.getService();
					}
					catch( Exception e )
					{
						CyLogger.getLogger().debug( geneListFilePath + " Exception caught and handled: " + e );
						numTries++;
						if ( numTries > 5 )
						{
							CyLogger.getLogger().error( geneListFilePath + " couldn't find service after 5 tries, exiting " );
							return null;
						}
					}
				}
				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 3 " + service );

				if ( null == service ) { CyLogger.getLogger().error( "Couldn't get service object from WebService" ); }

				Map<String, String> args = new HashMap<String, String>();

				String species = inputDialog.getSelectedSpecies();

				// make sure everything is in String format
				args.put(WebService.ARG_SPECIES, species);


				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 4 ");

				// To find the MODIDsystem ( the 'universal' system used to translate the primary ID system to 
				//    other systems, we use Ensembl for everything except for the special cases listed below

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 5 ");

				args.put( WebService.ARG_MOD_ID_SYSTEM,
						inputDialog.getSelectedModGeneSystem() );


				int numPermutations = ( overrideNumPermutations == -1 ?
						defaultNumPermutations : overrideNumPermutations );
				args.put(WebService.ARG_NUM_PERMUTATIONS, 
						"" + numPermutations );

				args.put(WebService.ARG_PRUNING_ALGORITHM,
						overrideZScorePruningMethod.length() == 0 ?
								defaultZScorePruningMethod :
									overrideZScorePruningMethod );

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() 6 ");

				args.put(WebService.ARG_ANALYSIS_TYPE,
						( bRunGONotPathwayAnalysis ? "GeneOntology" : "Pathways" ) );
				args.put(WebService.ARG_PVAL_THRESH, "" + defaultPValueThresh );
				args.put(WebService.ARG_MIN_GENES_CHANGED, "" + defaultMinNumGenesChanged );
				args.put(WebService.ARG_ZSCORE_THRESH, "" + defaultZScoreThresh );
				args.put(WebService.ARG_GENELIST_FILE, geneListFilePath);
				args.put(WebService.ARG_DENOM_FILE, denomFilePath);

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() launching job ");
				jobID = WebService.launchJob(args, service, statusWindow);
				CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() job launched ");
				String serverUrl = WebService.OUTPUT_HEAD_URL;
				statusWindow.append("Link to webservice job:\n "
						+ serverUrl + jobID + "\n\n");
				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() about to start loop ");

				// 8 is the code for completion
				statusWindow.append("Status:\n");
				// scroll to bottom to show status
				statusWindow.setCaretPosition(statusWindow.getDocument()
						.getLength());

				while (status == null || 2 == status.getCode()) {
					Thread.sleep(5000);
					status = WebService.getStatus(jobID, service);
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

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() getting results ");

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
					return( null );
				}

				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() getting results" );
				URL[] vResultURL = WebService.getResults(jobID, geneListFilePrefix,
						service, statusWindow );
				//CyLogger.getLogger().debug( geneListFilePath + " doInBkgd() got results" );

				// ******  grab results off server and put into simple data structures *****
				// process each output file that's sitting on server
				//synchronized( InputDialog.class )
				
					InputStream is = null;
					DataInputStream dis;
					String s;
	
					URL u = vResultURL[ WebService.ReturnTypes.RESULT_PRUNED_GO_AND_PATHWAY.ordinal() ];
					if ( null != u ) 
					{
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
					//			CyLogger.getLogger().debug( "highlight pathway: " + pathwayID );
							}
						}
					} 
	
					u = vResultURL[ WebService.ReturnTypes.RESULT_FULL_GO.ordinal() ];
					if ( null != u ) 
					{
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
						//CyLogger.getLogger().debug( "GORowsToHighlight " + GORowsToHighlight );
					}
	
					u = vResultURL[ WebService.ReturnTypes.RESULT_FULL_PATHWAY.ordinal() ];
					if ( null != u ) 
					{	
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
							// System.out.println(ine);
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
	
							//CyLogger.getLogger().debug( "rowdata[0] " + rowData[0] );
	
							String[] pathwayTextNameAndID = rowData[ 0 ].split( ":" );
							String pathwayID = pathwayTextNameAndID[1];
							//CyLogger.getLogger().debug( "pathwayTextNameAndID " + pathwayID);
	
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
						//CyLogger.getLogger().debug( "pathwayRowsToHighlight " + pathwayRowsToHighlight );
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
					//	CyLogger.getLogger().debug( geneListFilePath + " No GO results found\n" );
					}
					if ( pathwayResultsRowData.size() == 0 && !bRunGONotPathwayAnalysis )
					{
					//	CyLogger.getLogger().debug( geneListFilePath + "No Pathway results found\n" );
					}
					else
					{
					//	CyLogger.getLogger().debug( geneListFilePath + " Results were found" );
					}			

				
			} catch (java.net.MalformedURLException e) {
				statusWindow.append("exception: " + e );

				Utilities.showError(
						"malformed URL while fetching results: ", e);
			} catch (java.lang.InterruptedException e) {
				// Thread.sleep() was interrupted, ignore
				CyLogger.getLogger().error( "InterruptedException: " + e );
			} catch( IOException e )
			{
				CyLogger.getLogger().error( "IOException: " + e );
			} catch( Exception e )
			{
				CyLogger.getLogger().error( geneListFilePath + " Exception: " + e + stack2string( e ) );
			}


			return (status);
		}
	}


	@SuppressWarnings("serial")
	@Override
	public void done() 
	{
		CommandHandler.updateResultsPanel(resultName, true, "GO-Elite Results", resultName, geneListFilePath );

		System.out.println("done!");
		try 
		{

			// print results in results panel
			// status.getCode() == 8  means success
			// grab all the available results, regardless of return code
			if ( true ) 
			{

				// populate tables
				CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);

				if ( bRunGONotPathwayAnalysis )
				{
					//
					// 	Process GO Results Table
					//

					if (GONameResultsRowData.size() > 0) 
					{
						// filter rows for z-score
						// z-score has index 13

						int rowsDeleted = 0;
						for ( int i = 0; i < GONameResultsRowData.size(); i++ )
						{
							String x = ( String ) GONameResultsRowData.get( i ).get( 13 );
							if ( new Double( x ).doubleValue() < 0 )
							{
								// delete row
								GONameResultsRowData.remove( i );

								// adjust for deletion
								i--;
							}
						}
						
						resultsTable = new JTable(
								GONameResultsRowData,
								GONameResultsColumnNames) 
						{
							public boolean isCellEditable(int rowIndex,
									int vColIndex) 
							{
								return false;
							}
						};
						long  hits = 0;
						for( int i =0 ; i< vbGORowsToHighlight.size(); i++ )
						{
							if ( vbGORowsToHighlight.get( i ) == Boolean.TRUE ) { hits++; }
						}
						CyLogger.getLogger().debug( "vbGORowsToHighlight: " + hits + " / " + vbGORowsToHighlight.size() );

						resultsTable.setDefaultRenderer( Object.class, new CustomTableCellRenderer( vbGORowsToHighlight ) );

						// hide some columns
						// actual columns = 3-7, 11-12							
						int [] GONameColumnsToHide = { 3, 4, 5, 6, 7, 11, 12 }; 

						// remove in reverse order so the indices don't need adjustment
						for (int j = GONameColumnsToHide.length - 1; j >= 0; j--) 
						{
							TableColumn column = resultsTable
							.getColumnModel().getColumn( GONameColumnsToHide[ j ] );
							CyLogger.getLogger().debug( "hiding: " + GONameResultsColumnNames.get( GONameColumnsToHide[ j ] ) );
							resultsTable.removeColumn(column);
						}
						resultsTable.addMouseMotionListener( new MouseMotionAdapter() 
						{
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
						}
						);

					}
				
				}
				else							
				{

					/*
					 * Process Pathway Results Table
					 */
					if (pathwayResultsRowData.size() > 0) 
					
					{
						// filter rows for z-score
						// z-score has index 6							
						int rowsDeleted = 0;
						for ( int i = 0; i < pathwayResultsRowData.size(); i++ )
						{
							String x = ( String ) pathwayResultsRowData.get( i ).get( 6 );
							if ( new Double( x ).doubleValue() < 0 )
							{
								// delete row
								pathwayResultsRowData.remove( i );

								// adjust for deletion
								i--;
							}
						}


						resultsTable = new JTable(
								pathwayResultsRowData,
								pathwayResultsColumnNames); 
	//							{
	//								public boolean isCellEditable(int rowIndex,
	//										int vColIndex) 
	//								{
	//									return false;
	//								}
	//							};

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
						// support row selection
						//pathwayResultsTable.setCellSelectionEnabled(true);

						ListSelectionModel tableRowSelectionModel = resultsTable.getSelectionModel();


						// truly this belongs to the resultsTable, but we are lazy and not overriding JTable so we simply store pass it along
						//    to each worker class that needs it.
						HashMap< Integer, CyNetwork > networkMap = new HashMap< Integer, CyNetwork >();

						PathwayListSelectionListener lsl = new PathwayListSelectionListener( resultsTable, networkMap );

						tableRowSelectionModel.addListSelectionListener(lsl);
						tableRowSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						resultsTable.setSelectionModel(tableRowSelectionModel);

						// render cells to appear like hyperlinks

						ClickableRenderer cr = new ClickableRenderer( networkMap );
						cr.setToolTipText("Click to load pathway");
						resultsTable.getColumnModel().getColumn(0)    //XXX seems to cause hanging / poor swing performance, who knows why?
							.setCellRenderer(cr);

						resultsTable.addMouseMotionListener( new MouseMotionAdapter() 
						{
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
						}
						);

					}
				}


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
				

				if ( bRunGONotPathwayAnalysis && ( GONameResultsRowData.size() == 0 ) ||
						( !bRunGONotPathwayAnalysis && ( pathwayResultsRowData.size() == 0 ) ) )
				{
					CyLogger.getLogger().debug( geneListFilePath + " no results found in done()" );

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
				rerunAnalysisNumPermutations.setMinimumSize( rerunAnalysisNumPermutations.getPreferredSize() );
				optionsPanel.add( rerunAnalysisNumPermutations );

				optionsPanel.add( new JLabel( "z-score pruning method:" ) );
				rerunAnalysisZScorePruningMethod = new JComboBox( InputDialog.vPruningAlgorithms );
				rerunAnalysisZScorePruningMethod.setMaximumSize( rerunAnalysisZScorePruningMethod.getPreferredSize());
				optionsPanel.add( rerunAnalysisZScorePruningMethod );

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
						inputDialog.launchJob(geneListFilePath, denomFilePath, numPermutations, zScorePruningMethod, resultsAnalysisTypePanel,
								bRunGONotPathwayAnalysis );

					}
				};

				rerunAnalysisButton = new JButton( "Rerun" );
				rerunAnalysisButton.addActionListener( rerunAnalysisActionListener );
				optionsPanel.add( rerunAnalysisButton );

				exportButton = new JButton( "Export" );
				ExportResultsActionListener exportResultsActionListener = new ExportResultsActionListener( resultsTable );
				exportButton.addActionListener( exportResultsActionListener );
				optionsPanel.add( exportButton );

				resultsWrapperPanel.add( optionsPanel );
				resultsAnalysisTypePanel.addTab("Results", resultsWrapperPanel );

				if ( resultsAnalysisTypePanel==null )
				{
					CyLogger.getLogger().error( "resultsAnalysisTypePanel is null (1) " );
				}
				InputDialog.getResultsMasterPanel().setSelectedComponent( InputDialog.getResultsAnalysisNamePanel() );
				if ( resultsAnalysisTypePanel==null )
				{
					CyLogger.getLogger().error( "resultsAnalysisTypePanel is null (2) " );
				}
				//resultsAnalysisNamePanel.setSelectedComponent( resultsAnalysisTypePanel );
				resultsAnalysisTypePanel.setSelectedComponent( resultsWrapperPanel );

				CommandHandler.changeResultStatus(resultName, true); 
				CommandHandler.changeResultTabIndex( resultName, bRunGONotPathwayAnalysis ? "GO" : "Pathway" ); 

				/*
				 * Process log file
				 */
				for (int j = 0; j < logFileContents.size(); j++) {
					statusWindow.append(logFileContents.elementAt(j)
							+ "\n");
				}

				/*
				 * Process stdout and stderr file
				 */
				if (stdoutFileContents.size() > 0) 
				{

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

				if (stderrFileContents.size() > 0) 
				{

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
			}
		} catch( Exception e  )
		{
			statusWindow.append( "Exception: " + e );

			CyLogger.getLogger().error( "Exception: " + e + stack2string( e ) );
		}
		// try...catch...end

	} 
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


	class ExportResultsActionListener implements ActionListener
	{
		JTable results = null;  
		public ExportResultsActionListener( JTable results_ ) { results = results_; }

		public void actionPerformed( ActionEvent e )
		{
			// dump this to disk
			// dialog box: save file
			JFileChooser chooser = ( new JFileChooser());
			int returnVal = chooser.showSaveDialog( inputDialog.getResultsMasterPanel() );

			try
			{
				if (returnVal == JFileChooser.APPROVE_OPTION) 
				{
					File exportFile = chooser
					.getSelectedFile();

					if ( !exportFile.getName().contains( ".txt" ) )
					{
						exportFile = new File( exportFile.getAbsolutePath() + ".txt " );
					}

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
	}
}
