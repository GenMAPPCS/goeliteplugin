package org.genmapp.goelite;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import cytoscape.Cytoscape;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.plugin.CytoscapePlugin;

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
    public GOElitePlugin() 
    { 
	  JMenuItem item = new JMenuItem("Run GO-Elite");
	  JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar().getMenu("Plugins");
	  item.addActionListener(new GoElitePluginCommandListener());

	  pluginMenu.add(item);
	  
	  WebServiceClientManager.registerClient(GOEliteClient.getClient());
    }

	class GoElitePluginCommandListener implements ActionListener {
		
		public GoElitePluginCommandListener() {
		}

		public void actionPerformed(ActionEvent e) 
		{
		  // Create the dialog
			
		  String vSpecies[] = { new String( "Mm" ), new String( "Hs" ) };
		  String vGeneSystems[] = { new String( "Ensembl" ), new String( "EntrezGene" ) };
		  String vPruningAlgorithms[] = { new String( "z-score" ), new String( "gene number" ), 
				new String( "combination" ) };

	      LayoutProperties layoutProperties = new LayoutProperties( "Go-elite" );

		    layoutProperties.add( new Tunable("group", "group",
		    		Tunable.GROUP, new Integer( 10 ) ) );
		    layoutProperties.add(new Tunable("num_cpus", "Number of CPUs",
		            Tunable.INTEGER, new Integer( 1 ) ) );
	
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
		    layoutProperties.add( new Tunable( "num_permutations", "Number of permutations for over-representation analysis",
		    		Tunable.INTEGER, new Integer( 2000 ) ) );
		    layoutProperties.add( new Tunable( "go_pruning_algorithm", "GO pruning algorithm",
		    		Tunable.LIST, new Integer( 0 ),
		    		(Object) vPruningAlgorithms, (Object) null, 0 ) );
		    layoutProperties.add(new Tunable( "z-score_thresh", "Z-score threshold",
		            Tunable.DOUBLE, new Double( 1.96 ) ) );
		    layoutProperties.add(new Tunable("p-value_thresh", "P-value threshold",
		            Tunable.DOUBLE, new Double( 0.05 ) ) );
		    layoutProperties.add(new Tunable("min_num_genes_changed", "Minimum number of genes changed",
		            Tunable.INTEGER, new Integer( 3 ) ) );
	
		    JDialog dialog = new JDialog();
		    JPanel panel = layoutProperties.getTunablePanel();
		    dialog.add( panel );
		    dialog.setSize( 300, 500 );

		    dialog.setVisible( true );
		}
	}
}

