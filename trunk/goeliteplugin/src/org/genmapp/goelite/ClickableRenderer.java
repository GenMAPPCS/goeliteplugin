package org.genmapp.goelite;

import java.awt.Color;
import java.awt.Component;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;

/**
 * Renderer for making table cell contents appear clickable, i.e., like a
 * hyperlink.
 * 
 */
class ClickableRenderer extends DefaultTableCellRenderer {

	private Map<Integer, CyNetwork> networkMap = new HashMap<Integer, CyNetwork>();
	private Integer row = null;
	
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		this.row = row;
		
		Component c = super.getTableCellRendererComponent(table, value,
				isSelected, false, row, column);
		// Font f = new Font("", Font.BOLD, 12);
		Map<TextAttribute, Integer> map = new HashMap<TextAttribute, Integer>();
		map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		
		Color linkColor = Color.blue;
		if(InputDialog.networkMap.containsKey(this.row)){
			linkColor = new Color(85,  26, 139);
		}

		// if GPML plugin is loaded, then create pathway links
		GpmlPlugin gp = GpmlPlugin.getInstance();
		if (null != gp) {
			c.setFont(c.getFont().deriveFont(map));
			c.setForeground(linkColor);
			c.setBackground(Color.white);
		}

		return this;
	}
}