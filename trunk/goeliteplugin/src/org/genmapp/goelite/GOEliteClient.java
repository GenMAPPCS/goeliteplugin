package org.genmapp.goelite;

import javax.swing.JPanel;

import cytoscape.data.webservice.CyWebServiceEvent;
import cytoscape.data.webservice.CyWebServiceException;
import cytoscape.data.webservice.NetworkImportWebServiceClient;
import cytoscape.data.webservice.WebServiceClientImplWithGUI;
import cytoscape.data.webservice.WebServiceClientManager.ClientType;
import cytoscape.layout.Tunable;
import cytoscape.util.ModulePropertiesImpl;
import cytoscape.visual.VisualStyle;
import edu.sdsc.nbcr.opal.AppService;

/**
 * @author apico
 *
 */
/**
 * @author apico
 *
 */
public class GOEliteClient extends WebServiceClientImplWithGUI<AppService, JPanel>
implements NetworkImportWebServiceClient {

	private static final String DISPLAY_NAME = "GO-Elite Web Service Client";
	private static final String CLIENT_ID = "goelite";
	private static GOEliteClient client;
	
	static {
		try {
			client = new GOEliteClient();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * @throws CyWebServiceException
	 */
	public GOEliteClient()  throws CyWebServiceException {
		super(CLIENT_ID, DISPLAY_NAME,
			      new ClientType[] { ClientType.NETWORK }, null, null, null);
		
		setDescription();
		setProperty();
		
	}

	/**
	 * 
	 */
	private void setDescription() {
		description = "http://webservices.rbvi.ucsf.edu/opal/CreateSubmissionForm.do?serviceURL=http%3A%2F%2Flocalhost%3A8080%2Fopal%2Fservices%2FGOEliteService";
	}
	
	/**
	 * Set props for this client.
	 */
	private void setProperty() {
		
		  String vSpecies[] = { new String( "Mm" ), new String( "Hs" ) };
		  String vGeneSystems[] = { new String( "Ensembl" ), new String( "EntrezGene" ) };
		  String vPruningAlgorithms[] = { new String( "z-score" ), new String( "gene number" ), 
				new String( "combination" ) };

		props = new ModulePropertiesImpl(clientID, "wsc");

	    props.add(new Tunable("num_cpus", "Number of CPUs",
	            Tunable.INTEGER, new Integer( 1 ) ) );

	    props.add(new Tunable("species", "Species to analyze",
	            Tunable.LIST, new Integer(0),
	            (Object) vSpecies, (Object) null, 0));
	    props.add( new Tunable( "input_denom_file", "Input denominator file",
	    		Tunable.STRING, new String( "" ) ) );
	    props.add( new Tunable( "input_gene_list_file", "Input gene list file",
	    		Tunable.STRING, new String( "" ) ) );
	    props.add(new Tunable("gene_system", "Primary gene system",
	            Tunable.LIST, new Integer(0),
	            (Object) vGeneSystems, (Object) null, 0));
	    props.add( new Tunable( "num_permutations", "Number of permutations for over-representation analysis",
	    		Tunable.INTEGER, new Integer( 2000 ) ) );
	    props.add( new Tunable( "go_pruning_algorithm", "GO pruning algorithm",
	    		Tunable.LIST, new Integer( 0 ),
	    		(Object) vPruningAlgorithms, (Object) null, 0 ) );
	    props.add(new Tunable( "z-score_thresh", "Z-score threshold",
	            Tunable.DOUBLE, new Double( 1.96 ) ) );
	    props.add(new Tunable("p-value_thresh", "P-value threshold",
	            Tunable.DOUBLE, new Double( 0.05 ) ) );
	    props.add(new Tunable("min_num_genes_changed", "Minimum number of genes changed",
	            Tunable.INTEGER, new Integer( 3 ) ) );
	}
	/* (non-Javadoc)
	 * @see cytoscape.data.webservice.WebServiceClientImpl#executeService(cytoscape.data.webservice.CyWebServiceEvent)
	 */
	@Override
	public void executeService(CyWebServiceEvent arg0)
			throws CyWebServiceException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see cytoscape.data.webservice.NetworkImportWebServiceClient#getDefaultVisualStyle()
	 */
	public VisualStyle getDefaultVisualStyle() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Return instance of this client.
	 * @return
	 */
	public static GOEliteClient getClient() {
		return client;
	}

}
