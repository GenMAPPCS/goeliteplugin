/*
 Copyright 2010 Alexander Pico
 Licensed under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License. 
 You may obtain a copy of the License at 
 	
 	http://www.apache.org/licenses/LICENSE-2.0 
 	
 Unless required by applicable law or agreed to in writing, software 
 distributed under the License is distributed on an "AS IS" BASIS, 
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 See the License for the specific language governing permissions and 
 limitations under the License. 
 */

/*
 * 
 * 1. Understand layoutProperties and how to expose args from this class
 * 2. Refactor InputDialog:  separate Dialog from PluginClient
 * 
 * PluginClient
 * - int launchJob() { return( id ) 
 * - int getStatus( int id ) { return( status ) 
 * - String[][] getResults( int id ) { return files }
 * 
 * InputDialog
 * - display
 * - actionPerformed
 * --- SwingWorker::doInBkgd
 * --- SwingWorker::done
 * 
 */

package org.genmapp.goelite;

import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobOutputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;
import edu.sdsc.nbcr.opal.types.OutputFileType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;
import giny.model.Node;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingworker.SwingWorker;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;

import org.pathvisio.cytoscape.GpmlPlugin;
import org.pathvisio.model.Pathway;
import org.pathvisio.wikipathways.WikiPathwaysClient;
import org.pathvisio.wikipathways.webservice.WSPathway;

//import org.pathvisio.wikipathways.WikiPathwaysClient;
//import org.pathvisio.wikipathways.webservice.WSPathway;

/*
 * This class's constructor gets trigged by Cytoscape immediately upon loading
 * 
 * The basic use case is quite linear for this plugin:
 * - user clicks on "Plugins->RUN Go-Elite" 
 * - dialog box pops up with a bunch of input parameters
 * - user fills it out
 * - OPAL webservice computes a result based on the user input
 * - results are shown in the Results Panel of the cytoscape application
 * : the dialog disappears immediately after the user hits "submit"
 * : while processing occurs, the results tab gives some indicator of the current progress
 * : when complete, separate results tabs are populated with the results if the run was successful + the log file is printed to the status window
 * 
 * Another way to trigger its functionality is to use CriteriaMapper to select criteria.
 * 
 * A third way is to use CyCommands.
 *  
 *
 * Note:  we use the layoutProperites class to pass around the input arguments/types to the GOElite web service
 * 		most of the arguments are embedded as Tunables.  2 however, are not:
 * 		- denom_file_name
 * 		- probeset_file_name
 */
public class GOElitePlugin extends CytoscapePlugin {
	static boolean bResultsMasterPanelAlreadyAdded = false; // used for results
	// panel in
	// Cytoscape window
	CloseableTabbedPane resultsMasterPanel = null;

	public static String[] getCriteriaSets(CyNetwork network,
			TextArea debugWindow) {
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		String[] a = { "" };

		debugWindow.append("getCriteriaSets start for network "
				+ network.getIdentifier());
		debugWindow.append("cyattributes "
				+ networkAttributes.getAttributeNames().length);
		if (networkAttributes.hasAttribute(network.getIdentifier(),
				"__criteria")) {
			debugWindow.append("__criteria found");
			ArrayList<String> temp = (ArrayList<String>) networkAttributes
					.getListAttribute(network.getIdentifier(), "__criteria");
			ArrayList<String> full = new ArrayList<String>();
			for (String s : temp) {
				full.add(s);
			}
			debugWindow.append("a found " + a.length);
			return full.toArray(a);

		}
		debugWindow.append("getCriteriaSets end (" + a.length + ")");
		return (a);
	}

	// for a given criteriaSet, return its criteria
	public static String[] getCriteria(String criteriaSet, CyNetwork network,
			TextArea debugWindow) {
		ArrayList<String> criteriaNames = new ArrayList<String>();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		ArrayList<String> temp = ((ArrayList<String>) networkAttributes
				.getListAttribute(network.getIdentifier(), criteriaSet));
		debugWindow.append("criteria for " + criteriaSet + " found "
				+ temp.size());

		// split first on "comma", then on ":"
		boolean isFirst = true;
		for (String criterion : temp) {
			// skip the first entry, it's not actually a criterion
			if (isFirst) {
				isFirst = false;
				continue;
			}

			String[] tokens = criterion.split(":");
			debugWindow.append("tokens[1]: " + tokens[1]);
			criteriaNames.add(tokens[1]);
		}
		return ((String[]) criteriaNames.toArray(new String[criteriaNames
				.size()]));
	}

	// produces a probeset/denominator file that can be sent to the webservice
	// for GO-Elite analysis
	// bWriteMode = if false, then does not write the file to disk; useful for
	// counting number of hits it would write
	public static long[] generateInputFileFromNetworkCriteria(
			String pathToFile, String systemCode, String criteriaSetName,
			String criteriaLabel, boolean bAcceptTrueValuesOnly,
			boolean bWriteMode, TextArea debugWindow)
			throws java.io.IOException {
		FileWriter fw = null;
		PrintWriter out = null;

		if (bWriteMode) {
			debugWindow.append("opening filewriter\n");
			fw = new FileWriter(pathToFile, false);
			debugWindow.append("filewriter opened\n");
			out = new PrintWriter(fw);
			debugWindow.append("2\n");

			out.write("id\tsystemCode");
			debugWindow.append("3\n");
			out.println();
			debugWindow.append("4\n");
		}
		// / for every node,
		// get all nodes that pass the test
		String nodeAttributeCriteriaLabel = criteriaSetName + "_"
				+ criteriaLabel;
		List<Node> nodeList = Cytoscape.getCyNodesList();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		debugWindow.append("bAcceptTrueValuesOnly: " + bAcceptTrueValuesOnly);
		boolean bFirstValue = true;
		long numHits = 0;
		long numTotal = 0;
		debugWindow.append("Checking all nodes: " + nodeList.size() + " for "
				+ nodeAttributeCriteriaLabel + "\n");
		for (Node node : nodeList) {
			boolean value = false;
			if (nodeAttributes.hasAttribute(node.getIdentifier(),
					nodeAttributeCriteriaLabel)) {
				numTotal++;
				//debugWindow.append(!bAcceptTrueValuesOnly+" or "+nodeAttributes
				// .getBooleanAttribute( node.getIdentifier(),
				// nodeAttributeCriteriaLabel)+"\n");
				if (!bAcceptTrueValuesOnly
						|| nodeAttributes.getBooleanAttribute(node
								.getIdentifier(), nodeAttributeCriteriaLabel)) {
					if (node.getIdentifier().length() > 0) {
						if (bWriteMode) {
							if (!bFirstValue) {
								out.println();
							} // opal server barfs on empty lines in numerator
							// file
							out.write(node.getIdentifier() + "\t" + systemCode);
						}
						bFirstValue = false;
						numHits++;
					}
				}
			}
		}
		long[] nums = { numHits, numTotal };
		if (bWriteMode) {
			out.close();
			fw.close();
		}
		debugWindow.append("done writing input file: " + numHits + " hits\n");
		return (nums);
	}

	public GOElitePlugin() {
		JMenuItem item = new JMenuItem("Run GO-Elite");
		JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
				.getMenu("Plugins");
		item.addActionListener(new GOElitePluginCommandListener(this));
		pluginMenu.add(item);

		LayoutProperties layoutProperties = new LayoutProperties("goelite");
		new GOEliteCommandHandler(layoutProperties);
	}
}

/*
 * Registers CyCommands to Cytoscape, and handles CyCommand events/requests
 */
class GOEliteCommandHandler extends AbstractCommandHandler {

	protected static final String GETDATA = "get data";
	protected static final String LAUNCH = "launch";
	protected static final String OPENDIALOG = "open dialog";
	protected static final String STATUS = "status";

	protected static final String ARG_ID = "id"; // argument name
	protected static final String ARG_STATUS_CODE = "status_code";
	protected static final String ARG_STATUS_MSG = "status_msg";
	protected static final String ARG_RESULTS_FILE = "results_file";
	protected static final String ARG_LOG_FILE = "log_file";

	LayoutProperties props = null;

	public GOEliteCommandHandler(LayoutProperties props) {
		super(CyCommandManager.reserveNamespace("goelite"));

		// *** functions definitions for the plugin to expose to the world
		// GETDATA:
		// get data id="id"
		addDescription(GETDATA, "");
		addArgument(GETDATA, ARG_ID);

		// LAUNCH
		// to get the arguments, we just use the properties passed in from the
		// GOEliteInputDialogBox
		// however, we must also manually add two params that are not in the
		// tunable list
		addDescription(LAUNCH, "");
		for (String t : props.getTunableList()) {
			addArgument(LAUNCH, t);
		}
		addArgument(LAUNCH, GOEliteInputDialog.ARG_GENELIST_FILE);
		addArgument(LAUNCH, GOEliteInputDialog.ARG_DENOM_FILE);

		// OPENDIALOG
		addDescription(OPENDIALOG, "");
		addArgument(OPENDIALOG);

		// STATUS
		addDescription(STATUS, "");
		addArgument(STATUS, ARG_ID);
	}

	public CyCommandResult execute(String command, Collection<Tunable> args)
			throws CyCommandException {
		return execute(command, createKVMap(args));
	}

	public CyCommandResult execute(String command, Map<String, Object> args)
			throws CyCommandException {
		CyCommandResult result = new CyCommandResult();

		if (LAUNCH.equals(command)) {
			String jobId = GOEliteService.launchJob(args, null);

			result.addMessage("job id = " + jobId);
			result.addResult(ARG_ID, jobId);
		} else if (OPENDIALOG.equals(command)) {
			GOEliteInputDialog dialog = new GOEliteInputDialog(
					new LayoutProperties("goelite"));
			dialog.setVisible(true);

			result.addMessage("Opened dialog");
		} else if (STATUS.equals(command)) {
			String jobId = getArg(command, ARG_ID, args);

			StatusOutputType status = GOEliteService.getStatus(jobId, null);
			result.addMessage("GOElite status for run id " + jobId + " = "
					+ status.getMessage());
			result.addResult(ARG_STATUS_CODE, status.getCode());
			result.addResult(ARG_STATUS_MSG, status.getMessage());
		} else if (GETDATA.equals(command)) {
			// returns the result file URLs off the server
			String jobId = getArg(command, ARG_ID, args);
			Vector<URL> vURL = GOEliteService.getResults(jobId, null);
			result.addMessage("URL found");
			result.addResult(ARG_RESULTS_FILE, vURL.get(0));
			result.addResult(ARG_LOG_FILE, vURL.get(1));
		}
		return (result);
	}
}

// in terms of design, layoutProperites is created in the GOElitePlugin and
// passed into this class
// so that the GOEliteCommandListener can tap into the object and infer what
// arguments to receive from the Tunable list;
// at that point in time, this GOEliteInputDialog class
// does need to be instantiated yet. LayoutProperties contains the arguments
// selected by the user.
class GOEliteInputDialog extends JDialog implements ActionListener {
	static CloseableTabbedPane resultsMasterPanel = null;

	String criteriaAllStringValue = "-all-"; // selection value for the criteria
	// "all"
	JButton launchButton = null;
	JButton generateInputFileButton = null;
	TextArea debugWindow = null;
	public final static long serialVersionUID = 0;
	AppServicePortType service = null;
	String jobID = null;
	LayoutProperties layoutProperties = null;
	JLabel inputDenomFilenameDescriptor = null,
			inputNumeratorFilenameDescriptor = null;
	JLabel inputDenomFilenameLabel, inputNumeratorFilenameLabel;
	JTextArea inputNumerFileTextArea = null, inputDenomFileTextArea = null;
	JButton inputNumerFileBrowseButton = null,
			inputDenomFileBrowseButton = null;
	JComboBox inputCriteriaSetComboBox = null, inputCriteriaComboBox = null;
	JLabel inputCriteriaDescriptor = null, inputCriteriaSetDescriptor = null;
	static String vSpecies[] = { new String("Hs"), new String("Mm"),
			new String("Sc") };
	static String vGeneSystems[] = { new String("Ensembl"),
			new String("EntrezGene"), new String("SGD"),
			new String("Affymetrix") };
	static String vGeneSystemMODS[] = { new String("Ensembl"),
			new String("EntrezGene"), new String("Ensembl"),
			new String("EntrezGene") }; // MOD for mapping to GO
	static String vGeneSystemCodes[] = { new String("En"), new String("L"),
			new String("D"), new String("X") }; // Code for input files
	static String vPruningAlgorithms[] = { new String("z-score"),
			new String("gene number"), new String("combination") };

	boolean bIsInputAFile = true;

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

	static boolean bResultsMasterPanelAlreadyAdded = false; // used for results
	// panel in
	// Cytoscape window

	public static String ARG_GENELIST_FILE = "numerator file";
	public static String ARG_DENOM_FILE = "denominator file";

	public GOEliteInputDialog(LayoutProperties _layoutProperties) {
		layoutProperties = _layoutProperties;

		layoutProperties.add(new Tunable("species_idx_code",
				"Species to analyze", Tunable.LIST, new Integer(0),
				(Object) vSpecies, (Object) null, 0));
		layoutProperties.add(new Tunable("gene_system_idx_code",
				"Primary gene system", Tunable.LIST, new Integer(0),
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
				"Minimum number of genes changed", Tunable.INTEGER,
				new Integer(3)));

		final JPanel panel = layoutProperties.getTunablePanel();

		JPanel inputSourcePanel = new JPanel();
		inputSourcePanel.add(new JLabel("Input Source "));

		ButtonGroup inputButtonsGroup = new ButtonGroup();
		final JRadioButton inputSourceFileRadioButton = new JRadioButton("File");
		final JRadioButton inputSourceCriteriaRadioButton = new JRadioButton(
				"Criteria");
		inputSourcePanel.add(inputSourceFileRadioButton);
		inputSourcePanel.add(inputSourceCriteriaRadioButton);
		inputButtonsGroup.add(inputSourceFileRadioButton);
		inputButtonsGroup.add(inputSourceCriteriaRadioButton);
		final JPanel inputSourceExpandingPanel = new JPanel();
		inputSourceExpandingPanel.setLayout(new BoxLayout(
				inputSourceExpandingPanel, BoxLayout.PAGE_AXIS));

		inputSourceExpandingPanel.setSize(800, 500);

		ActionListener inputSourceActionListener = new ActionListener() {
			public boolean isInputAFile() {
				return (bIsInputAFile);
			}

			public void actionPerformed(ActionEvent e) {

				if (e.getSource() == inputSourceFileRadioButton) {

					bIsInputAFile = true;
					inputSourceExpandingPanel.removeAll();
					inputNumeratorFilenameDescriptor = new JLabel("Numerator");
					inputSourceExpandingPanel
							.add(inputNumeratorFilenameDescriptor);
					inputNumerFileTextArea = new JTextArea("");
					inputNumerFileTextArea.setColumns(20);
					inputSourceExpandingPanel.add(inputNumerFileTextArea);
					inputNumerFileBrowseButton = new JButton("Browse...");
					inputSourceExpandingPanel.add(inputNumerFileBrowseButton);

					inputDenomFilenameDescriptor = new JLabel("Denominator");
					inputSourceExpandingPanel.add(inputDenomFilenameDescriptor);
					inputDenomFileTextArea = new JTextArea("");
					inputDenomFileTextArea.setColumns(20);
					inputSourceExpandingPanel.add(inputDenomFileTextArea);
					inputDenomFileBrowseButton = new JButton("Browse...");
					inputSourceExpandingPanel.add(inputDenomFileBrowseButton);

					inputNumerFileBrowseButton.addActionListener(this);
					inputDenomFileBrowseButton.addActionListener(this);

				} else if (e.getSource() == inputSourceCriteriaRadioButton) {
					bIsInputAFile = false;
					inputSourceExpandingPanel.removeAll();
					String[] criteriaSet = GOElitePlugin.getCriteriaSets(
							Cytoscape.getCurrentNetwork(), debugWindow);
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
					inputCriteriaSetComboBox.setMaximumSize(new Dimension(400,
							d.height));
					inputSourceExpandingPanel.add(inputCriteriaSetComboBox);

					inputCriteriaDescriptor = new JLabel("Criteria: ( 0 )");
					inputSourceExpandingPanel.add(inputCriteriaDescriptor);
					inputCriteriaComboBox = new JComboBox(criteria);
					d = inputCriteriaComboBox.getPreferredSize();
					inputCriteriaComboBox.setMaximumSize(new Dimension(400,
							d.height));
					inputSourceExpandingPanel.add(inputCriteriaComboBox);

					inputCriteriaSetComboBox.addActionListener(this);
					inputCriteriaComboBox.addActionListener(this);

				} else if (e.getSource() == inputNumerFileBrowseButton
						|| e.getSource() == inputDenomFileBrowseButton) {
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showOpenDialog(panel);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						try {
							if (e.getSource() == inputNumerFileBrowseButton) {
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								inputNumerFileTextArea.setText(filePath);
								inputNumerFileTextArea.setColumns(20);
								inputNumeratorFilenameDescriptor
										.setText("Numerator: ("
												+ Utilities
														.countLinesInFile(filePath)
												+ ")");
								inputNumeratorFilenameDescriptor
										.setAlignmentX(Component.CENTER_ALIGNMENT);

							} else {
								String filePath = chooser.getSelectedFile()
										.getCanonicalPath();
								inputDenomFileTextArea.setText(filePath);
								inputDenomFileTextArea.setColumns(20);
								inputDenomFilenameDescriptor
										.setText("Denominator: ("
												+ Utilities
														.countLinesInFile(filePath)
												+ ")");
								inputDenomFilenameDescriptor
										.setAlignmentX(Component.CENTER_ALIGNMENT);
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
									.getSelectedItem(), Cytoscape
									.getCurrentNetwork(), debugWindow);
					inputCriteriaComboBox.removeAllItems();
					inputCriteriaComboBox.addItem(criteriaAllStringValue);
					for (int i = 0; i < newCriteria.length; i++) {
						inputCriteriaComboBox.addItem(newCriteria[i]);
					}
					pack();
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
									.generateInputFileFromNetworkCriteria("",
											"", (String) selectedCriteriaSet,
											(String) inputCriteriaComboBox
													.getSelectedItem(), true,
											false, debugWindow);
							numHits = nums[0];
							numTotal = nums[1];
							debugWindow.append("setting label: " + numHits
									+ " out of " + numTotal + "\n:");
							inputCriteriaDescriptor.setText("Criteria: ("
									+ numHits + "/" + numTotal + ")");
						}

					} catch (java.io.IOException except) {
						debugWindow
								.append("exception: couldn't count hits for criteriaset/criteria");
					}
				}

				panel.revalidate();
			}
		};

		inputSourceFileRadioButton.addActionListener(inputSourceActionListener);
		inputSourceCriteriaRadioButton
				.addActionListener(inputSourceActionListener);
		inputSourceFileRadioButton.setSelected(true);

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

		debugWindow = new TextArea("", 5, 40);
		panel.add(debugWindow);
		panel.setSize(750, 2000);
		panel.setVisible(true);

		add(panel);

	}

	// keeps adding numeric suffixes until the name is unique
	String generateUniqueFilename(String filenameBase) {
		int cntr = 0;
		String x = filenameBase + ".txt";
		while (new File(x).exists()) {
			x = filenameBase + "_" + cntr + ".txt";
			cntr++;
		}
		return (x);
	}

	public void actionPerformed(ActionEvent evt_) {
		debugWindow.append("evt source: " + evt_.getSource() + "\n");
		debugWindow.append("launchButton: " + launchButton + "\n");
		debugWindow.append("genInputFileButton: " + generateInputFileButton
				+ "\n");

		if (evt_.getSource() != launchButton) {
			debugWindow.append("early return\n ");
			return;
		}

		debugWindow.append("launched a job request ");
		try {
			layoutProperties.updateValues(); // must do this to refresh contents
			// of the Tunables before we
			// read from them

			// prepare input files
			String geneListFilePath = "";
			String denomFilePath = "";

			if (!bIsInputAFile) {
				// criteriaSet/criteria were selected
				String pluginDir = PluginManager.getPluginManager()
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

				String selectedCriteriaSet = (String) inputCriteriaSetComboBox
						.getSelectedItem();
				String selectedCriteria = (String) inputCriteriaComboBox
						.getSelectedItem();
				String[] criteriaList = new String[] { selectedCriteria };

				if (((String) inputCriteriaComboBox.getSelectedItem())
						.compareTo(criteriaAllStringValue) == 0) {
					debugWindow.append("-all- found: get list of criteria ");
					// user chose "-all-" criteria: launch a series of jobs, one
					// per criteria in the criteriaSet
					criteriaList = GOElitePlugin.getCriteria(
							selectedCriteriaSet, Cytoscape.getCurrentNetwork(),
							debugWindow);
				}

				String systemCode = vGeneSystemCodes[new Integer(
						layoutProperties.getValue("gene_system_idx_code"))
						.intValue()];
				for (String criteria : criteriaList) {
					String geneListFileName = criteria;
					geneListFilePath = pluginDir + "/" + geneListFileName;
					geneListFilePath = generateUniqueFilename(geneListFilePath);

					debugWindow.append("launching job for criteria: "
							+ criteria);
					// if criteria selected, generate files first

					debugWindow.append("generating input numerator\n");
					GOElitePlugin.generateInputFileFromNetworkCriteria(
							geneListFilePath, systemCode,
							(String) selectedCriteriaSet, (String) criteria,
							true, true, debugWindow);

					String denomFileName = criteria + "_denom";
					denomFilePath = pluginDir + "/" + denomFileName;
					debugWindow.append("generating input denominator\n");
					denomFilePath = generateUniqueFilename(pluginDir + "/"
							+ denomFileName);
					GOElitePlugin.generateInputFileFromNetworkCriteria(
							denomFilePath, systemCode,
							(String) selectedCriteriaSet, (String) criteria,
							false, true, debugWindow);

					launchJob(geneListFilePath, denomFilePath);
				}
			} else {
				geneListFilePath = inputNumerFileTextArea.getText();
				denomFilePath = inputDenomFileTextArea.getText();

				launchJob(geneListFilePath, denomFilePath);

			}
			debugWindow.append("2>" + service + "\n");

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	void launchJob(final String geneListFilePath, final String denomFilePath) {
		debugWindow.append("launchJob start\n");
		SwingWorker<StatusOutputType, Void> worker = new SwingWorker<StatusOutputType, Void>() {
			edu.sdsc.nbcr.opal.types.StatusOutputType status = null;
			JTabbedPane resultsParentPanel = null;
			TextArea statusWindow = null;
			AppServicePortType service = null;

			public StatusOutputType doInBackground() {
				try {
					JobInputType launchJobInput = new JobInputType();

					debugWindow.append("doInBkgd\n");

					// **** Prepare results pane / status window
					statusWindow = new TextArea("", 20, 80,
							TextArea.SCROLLBARS_VERTICAL_ONLY);
					CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
							SwingConstants.EAST);

					if (resultsMasterPanel == null) {
						resultsMasterPanel = new CloseableTabbedPane();
					}
					resultsParentPanel = new JTabbedPane();

					JPanel statusPanel = new JPanel();
					statusPanel.setLayout(new BoxLayout(statusPanel,
							BoxLayout.PAGE_AXIS));
					JLabel jobUrlLabel = new JLabel("Job Server Url:");
					jobUrlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

					statusPanel.add(jobUrlLabel);
					statusPanel.add(statusWindow);

					resultsParentPanel.addTab("Status", statusPanel);
					String geneListFileName = new File(geneListFilePath)
							.getName();
					resultsMasterPanel.addTab(geneListFileName,
							resultsParentPanel);
					if (!bResultsMasterPanelAlreadyAdded) {
						cytoPanel.add("GO-Elite Results", resultsMasterPanel);
						bResultsMasterPanelAlreadyAdded = true;
					}
					cytoPanel.setState(CytoPanelState.DOCK);

					// CytoPanel -> resultsMasterPanel "GO-Elite Results" ->
					// resultsParentPanel ("Status"|"Pathway"|"GO")

					service = GOEliteService.getService();
					Map<String, Object> args = new HashMap<String, Object>();

					// make sure everything is in String format
					args.put("species", vSpecies[new Integer(layoutProperties
							.getValue("species_idx_code")).intValue()]);
					args.put("mapping through", vGeneSystemMODS[new Integer(
							layoutProperties.getValue("gene_system_idx_code"))
							.intValue()]);
					args.put("system code", vGeneSystemCodes[new Integer(
							layoutProperties.getValue("gene_system_idx_code"))
							.intValue()]
							+ " ("
							+ vGeneSystems[new Integer(layoutProperties
									.getValue("gene_system_idx_code"))
									.intValue()] + ")");
					args.put("permutations", new String(layoutProperties
							.getValue("num_permutations")));
					args.put("pruning algorithm",
							vPruningAlgorithms[new Integer(layoutProperties
									.getValue("go_pruning_algorithm"))
									.intValue()]);
					args.put("p-value threshold", new String(layoutProperties
							.getValue("p-value_thresh")));
					args.put("min genes changed", new String(layoutProperties
							.getValue("min_num_genes_changed")));
					args.put("z-score threshold", new String(layoutProperties
							.getValue("z-score_thresh")));
					args.put(GOEliteInputDialog.ARG_GENELIST_FILE,
							geneListFilePath);
					args.put(GOEliteInputDialog.ARG_DENOM_FILE, denomFilePath);

					jobID = GOEliteService.launchJob(args, service,
							statusWindow);

					String serverUrl = "http://webservices.rbvi.ucsf.edu:8080/";
					statusWindow.append(serverUrl + jobID + "\n\n");
					jobUrlLabel.setText("Job: " + jobID);
					debugWindow.append("Job Server Url!!!: " + serverUrl
							+ jobID);
					jobUrlLabel.repaint();

					// 8 is the code for completion
					while (status == null || 8 != status.getCode()) {
						Thread.sleep(5000);
						status = GOEliteService.getStatus(jobID, service);
						debugWindow.append("[" + status.getCode() + "] "
								+ status.getMessage() + "\n");
						statusWindow.append("[" + status.getCode() + "] "
								+ status.getMessage() + "\n");
						statusWindow.repaint();
					}
				} catch (Exception e) {

				}
				return (status);
			}

			public Vector<String> getFileContents(URL u)
					throws MalformedURLException, IOException {
				Vector<String> contents = new Vector<String>();

				InputStream is = null;
				DataInputStream dis = null;

				// ----------------------------------------------//
				// Step 3: Open an input stream from the
				// url. //
				// ----------------------------------------------//

				is = u.openStream(); // throws an
				// IOException
				//-------------------------------------------------------------/
				// /

				// Step 4: //
				//-------------------------------------------------------------/
				// /
				// Convert the InputStream to a buffered
				// DataInputStream. //
				// Buffering the stream makes the reading
				// faster; the //
				// readLine() method of the DataInputStream
				// makes the reading //
				// easier. //
				//-------------------------------------------------------------/
				// /

				dis = new DataInputStream(new BufferedInputStream(is));

				//------------------------------------------------------------//
				// Step 5: //
				//------------------------------------------------------------//
				// Now just read each record of the input
				// stream, and print //
				// it out. Note that it's assumed that this
				// problem is run //
				// from a command-line, not from an
				// application or applet. //
				//------------------------------------------------------------//

				String s = null;
				while ((s = dis.readLine()) != null) {
					contents.add(s);
				}

				return (contents);
			}

			@Override
			public void done() {
				System.out.println("done!");
				debugWindow.append("done!\n");
				try {

					// print results in results panel
					if (status.getCode() == 8) {
						debugWindow.append("getting results!\n");

						Vector<URL> vResultURL = GOEliteService.getResults(
								jobID, service);
						debugWindow.append("results fetched: "
								+ vResultURL.size() + " URLs\n");

						Vector<String> logFileContents = null;
						Vector<String> GONameResultsColumnNames = new Vector<String>();
						Vector<String> pathwayResultsColumnNames = new Vector<String>();
						Vector<Vector> GONameResultsColumnData = new Vector<Vector>();
						Vector<Vector> pathwayResultsColumnData = new Vector<Vector>();

						// process each output file that's sitting on server
						for (int i = 0; i < vResultURL.size(); i++) {
							URL u = vResultURL.get(i);

							InputStream is = null;
							DataInputStream dis;
							String s;

							debugWindow.append(u + "\n");
							if (u.getFile().contains(
									"pruned-results_z-score_elite.txt")) {
								debugWindow.append("parsing results table\n");
								Vector<String> fileContents = getFileContents(u);
								Enumeration<String> contents = fileContents
										.elements();

								boolean processingGONameResultsNotPathwayResults = true;

								while (contents.hasMoreElements()) {

									String line = (String) contents
											.nextElement();
									System.out.println(line);
									Vector<String> columnsAsVector = new Vector<String>();
									String[] columnsData = (line).split("\t");

									if (columnsData.length < 2) {
										continue;
									} // ignore blank lines

									if (columnsData[2].contains("MAPP")) {
										processingGONameResultsNotPathwayResults = false;
									}

									// is this a column header?
									if (processingGONameResultsNotPathwayResults
											&& GONameResultsColumnNames.size() == 0) {
										GONameResultsColumnNames.addAll(Arrays
												.asList(columnsData));
										continue;
									} else if (!processingGONameResultsNotPathwayResults
											&& pathwayResultsColumnNames.size() == 0) {
										pathwayResultsColumnNames.addAll(Arrays
												.asList(columnsData));
										continue;
									}

									// it's a data line
									if (processingGONameResultsNotPathwayResults) {
										GONameResultsColumnData
												.add(new Vector<String>(Arrays
														.asList(columnsData)));
									} else {
										Pattern pat = Pattern.compile(":WP");
										String[] terms = pat
												.split(columnsData[2]);
										String wp = "WP" + terms[1];
										System.out.println(wp);
										// Get the instance of the GPML plugin
										GpmlPlugin gp = GpmlPlugin
												.getInstance();

										if (null != gp) {
											// GpmlPlugin isloaded;
											// make pathway links
											System.out.println("gp:" + gp);
											// Get the wikipathways client
											WSPathway r = gp
													.getWikiPathwaysClient()
													.getStub().getPathway(wp);
											// Load and import the pathway
											Pathway p = WikiPathwaysClient
													.toPathway(r);
											// Load the pathway in a new network
											CyNetwork n = gp.load(p, true);
										}
										// TODO: parse WP ids here to create
										// pathway import links
										pathwayResultsColumnData
												.add(new Vector<String>(Arrays
														.asList(columnsData)));
									}
								}
							} else if (u.getFile().contains(
									"GO-Elite_report.log")) {
								logFileContents = getFileContents(u);
								continue;
							} else {
								continue;
							}
						} // ... end of "for"

						// populate tables
						CytoPanel cytoPanel = Cytoscape.getDesktop()
								.getCytoPanel(SwingConstants.EAST);

						JTable GONameResultsTable = new JTable(
								GONameResultsColumnData,
								GONameResultsColumnNames);
						JTable pathwayResultsTable = new JTable(
								pathwayResultsColumnData,
								pathwayResultsColumnNames);

						// hide some columns
						int[] GONameColumnsToHide = { 0, 1, 5, 6, 7, 11, 12, 13 };
						int[] pathwayColumnsToHide = GONameColumnsToHide; // hide
						// the
						// same
						// columns

						for (int j = GONameColumnsToHide.length - 1; j >= 0; j--) {
							TableColumn column = GONameResultsTable
									.getColumnModel().getColumn(
											GONameColumnsToHide[j]);
							debugWindow.append("removing column: " + column);
							GONameResultsTable.removeColumn(column);
						}
						for (int j = pathwayColumnsToHide.length - 1; j >= 0; j--) {
							TableColumn column = pathwayResultsTable
									.getColumnModel().getColumn(
											pathwayColumnsToHide[j]);
							pathwayResultsTable.removeColumn(column);
						}

						for (int j = 0; j < logFileContents.size(); j++) {
							statusWindow.append(logFileContents.elementAt(j)
									+ "\n");
						}

						resultsParentPanel.addTab("GO", new JScrollPane(
								GONameResultsTable));
						resultsParentPanel.addTab("Pathway", new JScrollPane(
								pathwayResultsTable));

					} // if... end
				} catch (Exception e) {
					System.out.println("Exception " + e);
				} // try...catch...end

			} // done() end
		};

		debugWindow.append("executing worker");
		worker.execute();
		debugWindow.append("done executing worker");
	}

	// public String loadNetwork(String id, WikiPathwaysClient client) {
	// CyNetwork net = null;
	// try {
	// WSPathway wspath = this.client.getPathway(id);
	//			
	// System.out.println(
	// "Trying to load the pathway '"
	// + id + ":" + wspath.getName());
	//			
	// Pathway wikiPathway = WikiPathwaysClient.toPathway(wspath);
	// GpmlPlugin gp = GpmlPlugin.getInstance();
	//
	// System.out.println(
	// "Trying to load the pathway '"
	// + id + ":" + wspath.getName());
	//			
	//			 
	// // ASSUME: load() causes the current network to change.
	// net = gp.load(wikiPathway, true);
	// if (net == null) {System.out.println(
	// "GPMLPlugin returned null loading the pathway '"
	// + id + ":" + wspath.getName());
	// }
	//
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// return null;
	// }
	//
	//	
	// return net.getIdentifier();
	//
	// }

}

// Handles the top-level menu selection event from Cytoscape
class GOElitePluginCommandListener implements ActionListener {
	GOElitePlugin plugin = null;

	public GOElitePluginCommandListener(GOElitePlugin plugin_) {
		plugin = plugin_;
	}

	public void actionPerformed(ActionEvent evt_) {
		try {
			// pop up dialog
			LayoutProperties layoutProperties = new LayoutProperties("goelite");
			GOEliteInputDialog dialog = new GOEliteInputDialog(layoutProperties);
			dialog.setSize(new Dimension(350, 500));
			dialog.setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Exception: " + e);
			System.out.println("Exception: " + e);
		}

	}
}

// random file utilites
class Utilities {
	// replaces DOS-style carriage-returns with spaces: needed when sending a
	// text file from DOS -> UNIX
	public static byte[] replaceCR(byte[] bytes) {
		byte[] newBytes = new byte[bytes.length];

		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			if ('\r' != bytes[i]) {
				newBytes[j] = bytes[i];
				j++;
			}
		}
		return (newBytes);
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
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	public static long countLinesInFile(String filename) {
		// Count the number of lines in the specified file, and
		// print the number to standard output. If an error occurs
		// while processing the file, print an error message instead.
		// Two try...catch statements are used so I can give a
		// different error message in each case.
		long lineCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				lineCount++;
			}
		} catch (Exception e) {
			return (-1);
		}
		return (lineCount);
	} // end countLines()

} // end class Utilities

// this class talks to the opal server
class GOEliteService {
	public static AppServicePortType getService()
			throws javax.xml.rpc.ServiceException {
		// We could share the AppServicePortType object amongst multiple
		// requests but KISS for now
		AppServiceLocator findService = new AppServiceLocator();
		findService
				.setAppServicePortEndpointAddress("http://webservices.cgl.ucsf.edu/opal/services/GOEliteService");

		AppServicePortType service = findService.getAppServicePort();
		return (service);
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	public static String launchJob(Map<String, Object> args,
			AppServicePortType service) {
		return (launchJob(args, service, null));
	}

	public static String launchJob(Map<String, Object> args,
			AppServicePortType service, TextArea statusWindow) {
		statusWindow.append("Parameters:\n");
		try {
			if (service == null) {
				service = getService();
			}

			// *** Parse dialog box inputs
			// Process the gene list file
			File geneListFile = new File((String) args
					.get(GOEliteInputDialog.ARG_GENELIST_FILE));
			InputFileType geneListOpalFile = new InputFileType();
			geneListOpalFile.setName(geneListFile.getName()); // extract the
			// name portion
			// of the full
			// path
			byte[] geneListFileBytes = Utilities.getBytesFromFile(geneListFile);
			geneListOpalFile
					.setContents(Utilities.replaceCR(geneListFileBytes));

			File denomFile = new File((String) args
					.get(GOEliteInputDialog.ARG_DENOM_FILE));
			InputFileType denomOpalFile = new InputFileType();
			denomOpalFile.setName(denomFile.getName()); // extract the name
			// portion of the full
			// path
			byte[] denomFileBytes = Utilities.getBytesFromFile(denomFile);
			denomOpalFile.setContents(Utilities.replaceCR(denomFileBytes));

			JobInputType launchJobInput = new JobInputType();

			// *** Launch webservice
			// web service arguments are sent in as one long string
			// Example: "--species Mm --denom denom.txt --input probesets.txt
			// --mod EntrezGene
			// --permutations 2 --method z-score --pval 0.05 --num 3"
			// statusWindow.append( "args count: " + args.size() + "\n" );
			for (String key : args.keySet()) {
				statusWindow.append("  " + key + ": ");
				statusWindow.append(args.get(key) + "\n");
			}
			statusWindow.append("\n");
			String argList = "--species " + (String) args.get("species") + " "
					+ "--denom " + denomOpalFile.getName() + " " + "--input "
					+ geneListFile.getName() + " " + "--mod "
					+ (String) args.get("mapping through") + " "
					+ "--permutations " + (String) args.get("permutations")
					+ " " + "--method "
					+ (String) args.get("pruning algorithm") + " " + "--pval "
					+ (String) args.get("p-value threshold") + " " + "--num "
					+ (String) args.get("min genes changed") + " "
					+ "--zscore " + (String) args.get("z-score threshold")
					+ " ";
			// if ( null != statusWindow ) { statusWindow.append( argList ); }
			launchJobInput.setArgList(argList);

			// *** Wait for results, update running status
			InputFileType[] list = { geneListOpalFile, denomOpalFile };
			launchJobInput.setInputFile(list);
			JobSubOutputType output = service.launchJob(launchJobInput);

			return (output.getJobID());
		} catch (Exception e) {
			return (null);
		}
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	public static StatusOutputType getStatus(String jobID,
			AppServicePortType service) {
		try {
			if (service == null) {
				service = getService();
			}

			return (service.queryStatus(jobID));
		} catch (Exception e) {
			return (null);
		}
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	/*
	 * returns a vector of URLs with the following elements at their respective
	 * indices: 0 - results file url 1 - log file url
	 */
	public static Vector<URL> getResults(String jobID,
			AppServicePortType service) {
		try {
			Vector<URL> vResultURL = new Vector<URL>();
			if (service == null) {
				service = getService();
			}

			JobOutputType outputs = service.getOutputs(jobID);
			OutputFileType[] files = outputs.getOutputFile();

			URL resultsFileURL = null, logFileURL = null;

			// process each output file that's sitting on server
			for (int i = 0; i < files.length; i++) {
				URL u = null;
				System.out.println(files[i].getName());
				if (files[i].getName().contains("GO-Elite_results")) {
					// dig into the results folder to get the
					// file we care about
					u = new URL(files[i].getUrl().toString()
							+ "/pruned-results_z-score_elite.txt");

					resultsFileURL = u;

				} else if (files[i].getName().contains("GO-Elite_report.log")) {
					u = new URL(files[i].getUrl().toString());

					logFileURL = u;
				}
			} // ... end of "for"

			vResultURL.add(resultsFileURL);
			vResultURL.add(logFileURL);

			return (vResultURL);
		} catch (Exception e) {
			return (null);
		}
	}

} // end of class Client
