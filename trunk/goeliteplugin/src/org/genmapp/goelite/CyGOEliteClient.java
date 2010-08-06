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

package org.genmapp.goelite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JPanel;

import cytoscape.CytoscapeInit;
import cytoscape.data.webservice.CyWebServiceEvent;
import cytoscape.data.webservice.CyWebServiceEventListener;
import cytoscape.data.webservice.CyWebServiceException;
import cytoscape.data.webservice.NetworkImportWebServiceClient;
import cytoscape.data.webservice.WebServiceClientImplWithGUI;
import cytoscape.data.webservice.WebServiceClientManager;
import cytoscape.data.webservice.WebServiceClientManager.ClientType;
import cytoscape.layout.Tunable;
import cytoscape.util.ModuleProperties;
import cytoscape.util.ModulePropertiesImpl;
import cytoscape.visual.VisualStyle;
import edu.sdsc.nbcr.opal.AppService;
import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;
import cytoscape.task.util.TaskManager;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * @author apico
 *
 */
/**
 * @author apico
 *
 */
public class CyGOEliteClient extends WebServiceClientImplWithGUI<AppServicePortType, CyGOEliteClientGUI>
implements NetworkImportWebServiceClient {

	private static final String DISPLAY_NAME = "GO-Elite Web Service Client";
	private static final String CLIENT_ID = "goelite";
	private GOElitePlugin plugin = null;
	private AppServicePortType stub = null;
	static final long serialVersionUID = 0;

	/**
	 * @throws CyWebServiceException
	 */
	public CyGOEliteClient( GOElitePlugin plugin_ ) throws CyWebServiceException 
	{
		super(CLIENT_ID, DISPLAY_NAME, new ClientType[] { ClientType.NETWORK }, null, null, null);
	
		plugin = plugin_;
		
		setDescription();
		setProperty();
		getClientStub();
		//setGUI( new CyGOEliteClientGUI( this ) );  don't use their GUI just yet
		
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
	public void executeService( CyWebServiceEvent e )
			throws CyWebServiceException 
	{
		launchJob( ( JobInputType ) e.getParameter() );
	}

	
	protected void launchJob( JobInputType launchJobInput_ )
	{    
	   LaunchJobTask task = new LaunchJobTask( launchJobInput_ );
	   JTaskConfig config = new JTaskConfig();
	   config.displayCancelButton(false);
	   config.setModal(true);
	   TaskManager.executeTask(task, config);
	}
	
	/* (non-Javadoc)
	 * @see cytoscape.data.webservice.NetworkImportWebServiceClient#getDefaultVisualStyle()
	 */
	public VisualStyle getDefaultVisualStyle() {
		// TODO Auto-generated method stub
		return null;
	}

	class LaunchJobTask implements Task, CyWebServiceEventListener 
	{
		TaskMonitor monitor;
		JobInputType args = null;
		
		public LaunchJobTask( JobInputType args_ ) 
		{
			args = args_;
			WebServiceClientManager.getCyWebServiceEventSupport().addCyWebServiceEventListener(this);
		}
		public String getTitle() {
			return "Launching job...";
		}
		public AppServicePortType getStub()
		{
			//String urlString = CytoscapeInit.getProperties().getProperty(WEBSERVICE_URL);
			if ( stub == null  )
			{
				try
				{
					AppServiceLocator findService = new AppServiceLocator();
					System.out.println( "" + findService );
					findService.setAppServicePortEndpointAddress(
					   "http://webservices.cgl.ucsf.edu/opal/services/GOEliteService" 
					);
					System.out.println( "" + findService );
					    					
					stub = findService.getAppServicePort(); 
				}
				catch ( Exception e )
				{
					System.out.println( "Exception: " + e);
				}
			}
			return( stub );
		}
		public void run() {
			try {
				JobSubOutputType result = getStub().launchJob( args );
				gui.setResults(result);
				if(result == null ) 
				{
					SwingUtilities.invokeLater(new Runnable() 
					{
						public void run() {
							JOptionPane.showMessageDialog(
									gui, "The search didn't return any results",
									"No results", JOptionPane.INFORMATION_MESSAGE
							);
						}
					});
				}
			} catch (final Exception e) {
				System.out.println("Error while searching "+ e);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(
								gui, "Error: " + e.getMessage() + ". See log for details",
								"Error", JOptionPane.ERROR_MESSAGE
						);
					}
				});
			}
			}
			public void setTaskMonitor(TaskMonitor m)
				throws IllegalThreadStateException {
				this.monitor = m;
			}
			public void halt() {}
			//@Override
			public void executeService(CyWebServiceEvent arg0)
					throws CyWebServiceException {
				// TODO Auto-generated method stub
				
			}
	}
}