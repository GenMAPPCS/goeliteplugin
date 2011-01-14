package org.genmapp.goelite;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.pathvisio.cytoscape.GpmlPlugin;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;
import org.pathvisio.wikipathways.WikiPathwaysClient;
import org.pathvisio.wikipathways.webservice.WSPathway;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

public class LoadPathwayTask implements Task {

	private GpmlPlugin gp = null;
	private String wp = null;
	private Integer rowIndex = null;
	private CyNetwork n = null;
	private Map<Integer, CyNetwork> networkMap = new HashMap<Integer, CyNetwork>();

	public LoadPathwayTask(GpmlPlugin gp, String wp, Integer ri) {
		super();
		this.gp = gp;
		this.wp = wp;
		this.rowIndex = ri;
	}

	public String getTitle() {
		return "Loading pathway...";
	}

	public void halt() {
		// TODO Auto-generated method stub

	}

	public void run() {
		WSPathway r;
		try {
			// Get the wikipathways client
			r = gp.getWikiPathwaysClient().getStub().getPathway(wp);
			// Load and import the pathway
			Pathway p = WikiPathwaysClient.toPathway(r);
			// Load the pathway in a new network
			n = gp.load(p, true);

			// Track loaded networks to avoid reloading
			networkMap = InputDialog.networkMap;
			networkMap.put(rowIndex, n);
			InputDialog.networkMap = networkMap;
			
			//fire NETWORK_LOADED event
			// Object[2] is a Cytoscape convention
			Object[] new_value = new Object[2];
			new_value[0] = n;
			new_value[1] = n.getIdentifier();
			Cytoscape.firePropertyChange(Cytoscape.NETWORK_LOADED, null, new_value);

		} catch (RemoteException e1) {
			e1.printStackTrace();
		} catch (ConverterException e1) {
			e1.printStackTrace();
		}

	}

	public void setTaskMonitor(TaskMonitor arg0)
			throws IllegalThreadStateException {
		// TODO Auto-generated method stub

	}

}
