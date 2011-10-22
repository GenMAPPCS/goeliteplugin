package org.genmapp.goelite;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.logger.CyLogger;
import cytoscape.task.Task;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

public class PathwayListSelectionListener implements ListSelectionListener 
{
	/*
	 * Listener for row selection in Pathway results table. Useful for
	 * responding to clicks on pathway results.
	 */
	
		// the lsl has its own network map to track what pathway networks have been loaded by the user already---this prevents double-loading, but also
		//    allows multiple results panels to have this functionality.  The networkMap tracks rowIndex of the resultsTable its associated with --> 
		//    already-loaded pathways network object
		Map<Integer, CyNetwork> networkMap = null;
		JTable resultsTable = null;
		public PathwayListSelectionListener( JTable resultsTable_ )
		{
			networkMap = new HashMap<Integer, CyNetwork>();
			resultsTable = resultsTable_;

		}
		public void valueChanged(ListSelectionEvent e) {
			CyLogger.getLogger().debug( "valueChanged:" );
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();

			int rowIndex = lsm.getLeadSelectionIndex();
			boolean isAdjusting = e.getValueIsAdjusting();

			boolean loaded = false;
			CyNetwork n = networkMap.get(rowIndex);
			CyLogger.getLogger().debug( "rowIndex:" + rowIndex + "cached cynetwork: " + ( n != null ? n.getTitle() : "null" ) );
			if (n != null) {
				// verify that network still exists and has not been destroyed
				if (Cytoscape.getNetworkSet().contains(n)) {
					CyLogger.getLogger().debug( "n found" );
					// check to see if network has view; if not then destroy and reload!
					if (!Cytoscape.getNetworkViewMap().containsValue(Cytoscape.getNetworkView(n.getIdentifier()))){
						CyLogger.getLogger().debug( "no view found, destroy and reload" );
						Cytoscape.destroyNetwork(n);
						networkMap.remove(n);
					} else {
						CyLogger.getLogger().debug( "do nothing: loaded == true" );

						loaded = true;
						Cytoscape.getDesktop().setFocus(n.getIdentifier());
						// then clear selection to allow refocus
						resultsTable.clearSelection();
					}
				} else {
					CyLogger.getLogger().debug( "cached network does not exist in cytoscape -- was destroyed already" );

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
					Task task = new LoadPathwayTask(gp, wp, rowIndex, resultsTable, networkMap );
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

