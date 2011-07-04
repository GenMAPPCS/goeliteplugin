package org.genmapp.goelite;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;

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
			new String("gene number"), new String("combination")};
	static String vAnalysisType[] = {new String("both"),
			new String("Pathways"), new String("GeneOntology")};
	static String vAnalysisTypeDisplay[] = {new String("Pathways + GO"),
			new String("Pathways"), new String("GeneOntology")};

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

	public static String[] getVAnalysis() {
		return vAnalysisType;
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
		layoutProperties.add(new Tunable("analysis_type", "Type of analysis",
				Tunable.LIST, new Integer(0), (Object) vAnalysisTypeDisplay,
				(Object) null, 0));
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
		inputSourcePanel.add(inputSourceFileRadioButton);
		inputSourcePanel.add(inputSourceCriteriaRadioButton);
		inputButtonsGroup.add(inputSourceFileRadioButton);
		inputButtonsGroup.add(inputSourceCriteriaRadioButton);
		final JPanel inputSourceExpandingPanel = new JPanel();
		inputSourceExpandingPanel.setLayout(new BoxLayout(
				inputSourceExpandingPanel, BoxLayout.PAGE_AXIS));

		inputSourceExpandingPanel.setSize(300, 500);

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
					bIsInputAFile = false;
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
		String pluginDir = null;
		try {
			layoutProperties.updateValues(); // must do this to refresh contents
			// of the Tunables before we
			// read from them

			// prepare input files
			String geneListFileName = "";
			String geneListFilePath = "";
			String denomFilePath = "";

			if (!bIsInputAFile) {
				// criteriaSet/criteria were selected
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

				String systemCode = vGeneSystemCodes[new Integer(
						layoutProperties.getValue("gene_system_idx_code"))
						.intValue()];
				for (String criteria : criteriaList) {
					geneListFileName = criteria;
					geneListFilePath = pluginDir + "/" + geneListFileName;
					geneListFilePath = generateUniqueFilename(geneListFilePath);

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
					denomFilePath = generateUniqueFilename(pluginDir + "/"
							+ denomFileName);
					GOElitePlugin.generateInputFileFromNetworkCriteria(
							denomFilePath, systemCode,
							(String) selectedCriteriaSet, (String) criteria,
							false, true,
							(String) inputCriteriaKeyAttributeComboBox
									.getSelectedItem(), debugWindow);

					launchJob(geneListFilePath, denomFilePath);
				}
			} else {
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

	void launchJob(final String geneListFilePath, final String denomFilePath) {
		debugWindow.append("launchJob start\n");
		this.dispose();
		
		// INNER class: SwingWorker - only needed here inside this function
		SwingWorker<StatusOutputType, Void> worker = new SwingWorker<StatusOutputType, Void>() {
			edu.sdsc.nbcr.opal.types.StatusOutputType status = null;
			JTextArea statusWindow = null, stdoutWindow = null,
					stderrWindow = null;
			AppServicePortType service = null;
			JTabbedPane resultsParentPanel = null;
			String jobID = null;
			String geneListFileName = null;
			String resultName = null;

			public StatusOutputType doInBackground() {
				try {
					JobInputType launchJobInput = new JobInputType();

					debugWindow.append("doInBkgd\n");

					// **** Prepare results pane / status window
					CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
							SwingConstants.EAST);

					if (resultsMasterPanel == null) {
						resultsMasterPanel = new CloseableTabbedPane();
					}
					resultsParentPanel = new JTabbedPane();

					JPanel statusPanel = new JPanel();
					statusPanel.setLayout(new BoxLayout(statusPanel,
							BoxLayout.PAGE_AXIS));
					statusWindow = new JTextArea("", 15, 80);
					JScrollPane statusScroll = new JScrollPane(statusWindow);
					statusPanel.add(statusScroll);

					resultsParentPanel.addTab("Status", statusPanel);

					geneListFileName = new File(geneListFilePath)
							.getName();
					resultName = geneListFileName.substring(0, geneListFileName.lastIndexOf("."));
					resultsMasterPanel.addTab(resultName,
							resultsParentPanel);
					if (!bResultsMasterPanelAlreadyAdded) {
						cytoPanel.add("GO-Elite Results", resultsMasterPanel);
						bResultsMasterPanelAlreadyAdded = true;
					}
					if (cytoPanel.getState().equals(CytoPanelState.HIDE))
						cytoPanel.setState(CytoPanelState.DOCK);
					
					//update ResultsPanel in Workspaces
					CommandHandler.updateResultsPanel(resultName, false, "GO-Elite Results", resultName, "Status");

					// CytoPanel -> resultsMasterPanel "GO-Elite Results" ->
					// resultsParentPanel ("Status"|"Pathway"|"GO")
					service = WebService.getService();
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
					args.put(WebService.ARG_NUM_PERMUTATIONS, new String(
							layoutProperties.getValue("num_permutations")));
					args.put(WebService.ARG_PRUNING_ALGORITHM,
							vPruningAlgorithms[new Integer(layoutProperties
									.getValue("go_pruning_algorithm"))
									.intValue()]);
					args.put(WebService.ARG_ANALYSIS_TYPE,
							vAnalysisType[new Integer(layoutProperties
									.getValue("analysis_type")).intValue()]);
					args.put(WebService.ARG_PVAL_THRESH, new String(
							layoutProperties.getValue("p-value_thresh")));
					args
							.put(WebService.ARG_MIN_GENES_CHANGED, new String(
									layoutProperties
											.getValue("min_num_genes_changed")));
					args.put(WebService.ARG_ZSCORE_THRESH, new String(
							layoutProperties.getValue("z-score_thresh")));
					args.put(WebService.ARG_GENELIST_FILE, geneListFilePath);
					args.put(WebService.ARG_DENOM_FILE, denomFilePath);

					jobID = WebService.launchJob(args, service, statusWindow);

					String serverUrl = "http://webservices.rbvi.ucsf.edu:8080/";
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
				debugWindow.append("done!\n");
				try {

					// print results in results panel
					if (status.getCode() == 8) {
						debugWindow.append("getting results!!!\n");

						Vector<URL> vResultURL = WebService.getResults(jobID,
								service, debugWindow);
						debugWindow.append("results fetched: "
								+ vResultURL.size() + " URLs\n");

						Vector<String> logFileContents = new Vector<String>();
						Vector<String> stdoutFileContents = new Vector<String>();
						Vector<String> stderrFileContents = new Vector<String>();
						Vector<String> GONameResultsColumnNames = new Vector<String>();
						Vector<String> pathwayResultsColumnNames = new Vector<String>();
						Vector<Vector> GONameResultsRowData = new Vector<Vector>();
						Vector<Vector> pathwayResultsRowData = new Vector<Vector>();

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
								statusWindow
										.append("\nParsing results table...\n");
								Vector<String> fileContents = Utilities
										.getFileContents(u);
								Enumeration<String> contents = fileContents
										.elements();

								boolean processingGONameResultsNotPathwayResults = true;

								// the results file is arranged so that GO results are reported above the Pathway results
								// When we see a header row that has "MAPP" as its 3rd column, we know that we've switched to Pathway results
								while (contents.hasMoreElements()) {

									String line = (String) contents
											.nextElement();
									// System.out.println(line);
									Vector<String> columnsAsVector = new Vector<String>();
									String[] rowData = (line).split("\t");

									if (rowData.length < 2) {
										continue;
									} // ignore blank lines

									if (rowData[2].contains("MAPP")) 
									{
										debugWindow.append( "switching to pathway results");
										processingGONameResultsNotPathwayResults = false;
									}

									// is this a column header?
									if (processingGONameResultsNotPathwayResults
											&& GONameResultsColumnNames.size() == 0) {
										
										// GO results found: add results
										GONameResultsColumnNames.addAll(Arrays
												.asList(rowData));
										
										continue;
									} else if (!processingGONameResultsNotPathwayResults
											&& pathwayResultsColumnNames.size() == 0) {
										pathwayResultsColumnNames.addAll(Arrays
												.asList(rowData));
										continue;
									}

									// it's a data line
									if (processingGONameResultsNotPathwayResults) {
										GONameResultsRowData
												.add(new Vector<String>(Arrays
														.asList(rowData)));
									} else {
										pathwayResultsRowData
												.add(new Vector<String>(Arrays
														.asList(rowData)));
									}
								}
							} else if (u.getFile().contains(
									"GO-Elite_report.log")) {
								logFileContents = Utilities.getFileContents(u);
								continue;
							} else if (u.getFile().contains("stdout.txt")) {
								debugWindow.append("stdout.txt found\n");

								stdoutFileContents = Utilities
										.getFileContents(u);
								continue;
							} else if (u.getFile().contains("stderr.txt")) {
								debugWindow.append("stderr.txt found\n");

								stderrFileContents = Utilities
										.getFileContents(u);
								continue;
							}
						} // ... end of "for"

								
						if ( GONameResultsRowData.size() == 0 )
						{
							statusWindow.append( "No GO results found\n" );
						}
						if ( pathwayResultsRowData.size() == 0 )
						{
							statusWindow.append( "No Pathway results found\n" );
						}
						else
						{
							debugWindow.append( "pathway results ( " + pathwayResultsRowData.size() + " rows ): " + pathwayResultsRowData.toString() );
						}
						debugWindow.append( "populating tables\n");


						// populate tables
						CytoPanel cytoPanel = Cytoscape.getDesktop()
								.getCytoPanel(SwingConstants.EAST);

						debugWindow.append("GONameResults...\n");

						/*
						 * Process GO Results Table
						 */
						if (GONameResultsRowData.size() > 0) {
							debugWindow.append("processing GOName Results\n");

							JTable goResultsTable = new JTable(
									GONameResultsRowData,
									GONameResultsColumnNames) {
								public boolean isCellEditable(int rowIndex,
										int vColIndex) {
									return false;
								}
							};

							// hide some columns
							int[] GONameColumnsToHide = {0, 1, 5, 6, 7, 11, 12,
									13};
							for (int j = GONameColumnsToHide.length - 1; j >= 0; j--) {
								TableColumn column = goResultsTable
										.getColumnModel().getColumn(
												GONameColumnsToHide[j]);
								goResultsTable.removeColumn(column);
							}

							// resize columns based on data
							resizeColumns(goResultsTable);

							JScrollPane goScrollPane = new JScrollPane( goResultsTable );
							resultsParentPanel.addTab("GO", goScrollPane );
							
							resultsMasterPanel.setSelectedComponent(resultsParentPanel);
							resultsParentPanel.setSelectedComponent( goScrollPane );
							
							CommandHandler.changeResultStatus(resultName, true);
							CommandHandler.changeResultTabIndex(resultName, "GO");
						}

						debugWindow.append("PathwayResults...\n");

						/*
						 * Process Pathway Results Table
						 */
						if (pathwayResultsRowData.size() > 0) {
							debugWindow.append("processing pathwayResults\n");
							pathwayResultsTable = new JTable(
									pathwayResultsRowData,
									pathwayResultsColumnNames) {
								public boolean isCellEditable(int rowIndex,
										int vColIndex) {
									return false;
								}
							};

							// hide the same columns
							int[] pathwayColumnsToHide = {0, 1, 5, 6, 7, 11,
									12, 13};
							for (int j = pathwayColumnsToHide.length - 1; j >= 0; j--) {
								TableColumn column = pathwayResultsTable
										.getColumnModel().getColumn(
												pathwayColumnsToHide[j]);
								pathwayResultsTable.removeColumn(column);
							}
							debugWindow.append( "hiding columns for pathway results");
							// support row selection
							//pathwayResultsTable.setCellSelectionEnabled(true);
							ListSelectionModel tableRowSelectionModel = pathwayResultsTable
									.getSelectionModel();
							tableRowSelectionModel
									.addListSelectionListener(lsl);
							tableRowSelectionModel
									.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							pathwayResultsTable
									.setSelectionModel(tableRowSelectionModel);
							debugWindow.append( "set selection model for pathway results");

							// render cells to appear like hyperlinks
							debugWindow.append( "1\n");
							ClickableRenderer cr = new ClickableRenderer();
							cr.setToolTipText("Click to load pathway");
							debugWindow.append( "2\n");
							pathwayResultsTable.getColumnModel().getColumn(0)
									.setCellRenderer(cr);
							debugWindow.append( "3\n");
							pathwayResultsTable
									.addMouseMotionListener(new MouseMotionAdapter() {
										public void mouseMoved(MouseEvent e) {
											Point p = new Point(e.getX(), e
													.getY());
											pathwayResultsTable
													.setCursor(pathwayResultsTable
															.columnAtPoint(p) == 0
															? new Cursor(
																	Cursor.HAND_CURSOR)
															: new Cursor(
																	Cursor.DEFAULT_CURSOR));
										}
									});

							debugWindow.append( "4\n");
							// resize columns based on data
							resizeColumns(pathwayResultsTable);
							debugWindow.append( "after resize columns\n");

							JScrollPane pathwayScrollPane = new JScrollPane( pathwayResultsTable );
							resultsParentPanel.addTab("Pathway",
									pathwayScrollPane );
							debugWindow.append( "setting selected component\n");

							resultsMasterPanel.setSelectedComponent(resultsParentPanel);
							resultsParentPanel.setSelectedComponent( pathwayScrollPane );
							debugWindow.append( "end pathway results display");
							
							CommandHandler.changeResultStatus(resultName, true);
							CommandHandler.changeResultTabIndex(resultName, "Pathway");
						}

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

							resultsParentPanel.addTab("Stdout", stdoutPanel);

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

							resultsParentPanel.addTab("Stderr", stderrPanel);
							
							resultsMasterPanel.setSelectedComponent(resultsParentPanel);
							resultsParentPanel.setSelectedComponent( stderrPanel );
							
							CommandHandler.changeResultStatus(resultName, false);
							CommandHandler.changeResultTabIndex(resultName, "Stderr");

							for (int j = 0; j < stderrFileContents.size(); j++) {
								stderrWindow.append(stderrFileContents
										.elementAt(j)
										+ "\n");
							}
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

			} // done() end
		};

		debugWindow.append("executing worker");
		worker.execute();
		debugWindow.append("done executing worker");
	}

	public void resizeColumns(JTable table) {
		for (int i = 0; i < table.getColumnCount(); i++) {
			TableColumn col = table.getColumnModel().getColumn(i);
			int width = 0;

			// cycle through rows to find max
			for (int r = 0; r < table.getRowCount(); r++) {
				Component comp = table.getCellRenderer(r, i)
						.getTableCellRendererComponent(table,
								table.getValueAt(r, i), false, false, r, i);
				// max: header vs. data value width
				width = Math.max(width, comp.getPreferredSize().width);
			}
			// Add margin
			width += 2;
			// Set the width
			col.setPreferredWidth(width);
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