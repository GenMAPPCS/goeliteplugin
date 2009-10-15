package org.genmapp.goelite;

import javax.swing.*;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;

public class GOElitePlugin extends CytoscapePlugin {

    public GOElitePlugin() {
       
	JMenuItem item = new JMenuItem("Run GO-Elite");
	JMenu pluginMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar().getMenu("Plugins");
	pluginMenu.add(item);
    }
}

