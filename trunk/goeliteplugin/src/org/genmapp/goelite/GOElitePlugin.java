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

import giny.model.Node;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;

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
 */
public class GOElitePlugin extends CytoscapePlugin {

	public static final String NET_ATTR_SETS = "org.genmapp.criteriasets_1.0";
	public static final String NET_ATTR_SET_PREFIX = "org.genmapp.criteriaset.";
	static boolean bResultsMasterPanelAlreadyAdded = false; // used for results
	// panel in
	// Cytoscape window
	CloseableTabbedPane resultsMasterPanel = null;

	public static String[] getCriteriaSets(JTextArea debugWindow) {

		String setsString = CytoscapeInit.getProperties().getProperty(
				NET_ATTR_SETS);

		String[] a = {""};

		debugWindow.append("getCriteriaSets!" + setsString);

		if (null != setsString && setsString.length() > 2) {
			debugWindow.append("Criteria Sets found");
			setsString = setsString.substring(1, setsString.length() - 1);
			String[] temp = setsString.split("\\]\\[");
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
	public static String[] getCriteria(String criteriaSet, JTextArea debugWindow) {
		ArrayList<String> criteriaNames = new ArrayList<String>();
		String paramString = CytoscapeInit.getProperties().getProperty(
				NET_ATTR_SET_PREFIX + criteriaSet);
		paramString = paramString.substring(1, paramString.length() - 1);
		CyLogger.getLogger().debug( "getCriteria " + paramString );

		// Example: [Node Color][ "18v14 log-fold" > 1.2::Label 1::#ffffff]
		//   We want to extract "Label 1"
		String[] temp = paramString.split("\\]\\[");
		debugWindow.append("criteria for " + criteriaSet + " found "
				+ temp.length);

		// split first on "comma", then on ":"
		boolean isFirst = true;
		for (String criterion : temp) {
			// skip the first entry, it's not actually a criterion
			if (isFirst) {
				isFirst = false;
				continue;
			}

			String[] tokens = criterion.split(":");
			debugWindow.append("tokens[2]: " + tokens[2]);
			criteriaNames.add(tokens[2]);
		}
		debugWindow.append("returning ");

		return ((String[]) criteriaNames.toArray(new String[criteriaNames
				.size()]));
	}

	public static void generateInputFileFromNodeSet( 
			String pathToFile, String systemCode, 
			Set< Node > nodeSet, 
			boolean bWriteMode, String keyAttribute, boolean bUseCytoscapeID, JTextArea debugWindow)
			throws java.io.IOException 
	{
		FileWriter fw = null;
		PrintWriter out = null;

		if (bWriteMode) {
			debugWindow.append("opening filewriter: keyAttribute " + keyAttribute + "\n");
			fw = new FileWriter(pathToFile, false);
			debugWindow.append("filewriter opened\n");
			out = new PrintWriter(fw);
			debugWindow.append("2\n");

			out.write( keyAttribute );
			out.write("\tsystemCode");
			debugWindow.append("3\n");
			out.println();
			debugWindow.append("4\n");
		}
		
		boolean bFirstValue = true;
		long numTotal = 0;
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		for (Node node : nodeSet) {
			boolean value = false;
			{
				numTotal++;
				if (bWriteMode) {
					if (!bFirstValue) 
					{
						out.println();
					} // opal server barfs on empty lines in numerator file

					String key = "";
					if ( bUseCytoscapeID )
					{
						key = node.getIdentifier();
					}
					else
					{
						key = nodeAttributes.getStringAttribute( node.getIdentifier(), keyAttribute );
					}
							
					debugWindow.append( ">" + key + "\t" + systemCode + "\n" );
					out.write( key + "\t" + systemCode);
				}
				bFirstValue = false;
			}
		}
		if (bWriteMode) {
			out.close();
			fw.close();
		}
		debugWindow.append("done writing input file\n");
	}
	
	
	// returns a single node that has the given attribute
	public static Node getSampleNodeWithAttribute( String attrib, boolean bUseCytoscapeID )
	{
		// for every dataset node,
		// get all nodes that pass the test
		// collect node list assembled from all CyDatasets
		Map<String, Object> args = new HashMap<String, Object>();
		CyCommandResult re = null;
		try {
			 re = CyCommandManager.execute("workspaces", CommandHandler.GET_ALL_DATASET_NODES,
					 args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int[] dsetNodes = (int[]) re.getResult();
		CyLogger.getLogger().debug( "nodeSet: " + dsetNodes.length );

		int[] nodeList = dsetNodes; //Cytoscape.getRootGraph().getNodeIndicesArray();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		
		for( int i : nodeList  )
		{
			Node node = Cytoscape.getRootGraph().getNode(i);
						
			if (nodeAttributes.hasAttribute(node.getIdentifier(),
					attrib ) || bUseCytoscapeID ) 
			{
				return( node );
			}
		}
		
		return( null );
	}
	public static Object [] getNodeSetWithCriteria( String criteriaSet, 
			 String criteria, boolean bAcceptTrueValuesOnly )
	{
		// for every dataset node,
		// get all nodes that pass the test
		// collect node list assembled from all CyDatasets
		Map<String, Object> args = new HashMap<String, Object>();
		CyCommandResult re = null;
		try {
			 re = CyCommandManager.execute("workspaces", CommandHandler.GET_ALL_DATASET_NODES,
					 args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int[] dsetNodes = (int[]) re.getResult();
		CyLogger.getLogger().debug( "nodeSet: " + dsetNodes.length );

		String nodeAttributeCriteriaLabel = criteriaSet + "_"
				+ criteria;
		int[] nodeList = dsetNodes; //Cytoscape.getRootGraph().getNodeIndicesArray();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		boolean bFirstValue = true;
		long numHits = 0;
		long numTotal = 0;
		
		List< Node > finalNodeList = new java.util.LinkedList< Node >();
		for( int i : nodeList  )
		{
			boolean value = false;
			Node node = Cytoscape.getRootGraph().getNode(i);
						
			if (nodeAttributes.hasAttribute(node.getIdentifier(),
					nodeAttributeCriteriaLabel)) {
				numTotal++;
				Object o = nodeAttributes.getAttribute( node
						.getIdentifier(), nodeAttributeCriteriaLabel );
				
				boolean x = o.toString().equals( "true" );  
								
				if (!bAcceptTrueValuesOnly
						|| x ) 
				{
					if (node.getIdentifier().length() > 0) 
					{
						numHits++;
						finalNodeList.add( node );
					}
				}
			}
		}
		Object [] rs = new Object[ 3 ];
		rs[ 0 ] = numHits;
		rs[ 1 ] = numTotal;
		rs[ 2 ] = finalNodeList;
		
		return( rs );
	 }
	// produces a probeset/denominator file that can be sent to the webservice
	// for GO-Elite analysis
	// bWriteMode = if false, then does not write the file to disk; useful for
	// counting number of hits it would write
	public static long[] generateInputFileFromNetworkCriteria(
			String pathToFile, String systemCode, String criteriaSetName,
			String criteriaLabel, boolean bAcceptTrueValuesOnly,
			boolean bWriteMode, String keyAttribute, boolean bUseCytoscapeID, JTextArea debugWindow)
			throws java.io.IOException {
		FileWriter fw = null;
		PrintWriter out = null;

		CyLogger.getLogger().debug( "generateInputFile: systemCode = " + systemCode );
		
		Object[] result = getNodeSetWithCriteria( criteriaSetName, criteriaLabel, bAcceptTrueValuesOnly );
		Long numHits = ( Long ) result[ 0 ];
		Long numTotal = ( Long ) result[ 1 ];
		List< Node > finalNodeList = ( List< Node > ) result[ 2 ];
		generateInputFileFromNodeSet( pathToFile, systemCode, new HashSet< Node >( finalNodeList ), bWriteMode, keyAttribute, bUseCytoscapeID, debugWindow );
		
		long[] nums = { numHits, numTotal };
		debugWindow.append( "numHits " + numHits + " numTotal " + numTotal );
		return( nums );
	}
	
				

	public GOElitePlugin() {
		JMenuItem item = new JMenuItem("Run GO-Elite");
		JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
				.getMenu("Plugins");
		item.addActionListener(new GOElitePluginCommandListener(this));
		pluginMenu.add(item);

		new CommandHandler();
	}
};

	
// Handles the top-level menu selection event from Cytoscape
class GOElitePluginCommandListener implements ActionListener {
	GOElitePlugin plugin = null;

	public GOElitePluginCommandListener(GOElitePlugin plugin_) {
		plugin = plugin_;
	}


	public void actionPerformed(ActionEvent evt_) {
		try {
			// pop up dialog
			InputDialog d = new InputDialog();
			
			d.pack();
			d.setVisible( true );


		} catch (Exception e) {
			Utilities.showError("Could not open main GOElite dialog", e);
			System.out.println("Exception: " + e);
		}

	}
}
