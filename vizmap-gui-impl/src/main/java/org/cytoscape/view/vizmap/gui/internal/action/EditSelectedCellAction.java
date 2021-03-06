package org.cytoscape.view.vizmap.gui.internal.action;

/*
 * #%L
 * Cytoscape VizMap GUI Impl (vizmap-gui-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.gui.editor.EditorManager;
import org.cytoscape.view.vizmap.gui.internal.VizMapperProperty;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;

/**
 *
 */
public class EditSelectedCellAction extends AbstractVizMapperAction {

	private static final long serialVersionUID = 7640977428847967990L;

	private static final Logger logger = LoggerFactory.getLogger(EditSelectedCellAction.class);

	private final EditorManager editorManager;
	private final VisualMappingManager vmm;

	public EditSelectedCellAction(final EditorManager editorManager, final CyApplicationManager appManager,
			final PropertySheetPanel propertySheetPanel, final VisualMappingManager vmm) {
		super("Edit all selected cells", appManager, propertySheetPanel);
		this.editorManager = editorManager;
		this.vmm = vmm;
	}

	/**
	 * Edit all selected cells at once. This is for Discrete Mapping only.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		final PropertySheetTable table = propertySheetPanel.getTable();
		final int[] selected = table.getSelectedRows();

		Item item = null;

		// If nothing selected, return.
		if ((selected == null) || (selected.length == 0))
			return;

		// Test with the first selected item
		item = (Item) propertySheetPanel.getTable().getValueAt(selected[0], 0);
		VizMapperProperty prop = (VizMapperProperty) item.getProperty();

		if ((prop == null) || (prop.getParentProperty() == null))
			return;

		Object internalVal = prop.getInternalValue();
		if (internalVal == null || internalVal instanceof DiscreteMapping == false)
			return;
		final DiscreteMapping dm = (DiscreteMapping) internalVal;
		final VisualProperty<Object> vp = dm.getVisualProperty();

		final VisualStyle currentStyle = vmm.getCurrentVisualStyle();
		Object newValue = null;

		try {
			newValue = editorManager.showVisualPropertyValueEditor(vizMapperMainPanel, vp, vp.getDefault());
		} catch (Exception e1) {
			logger.error("Could not edit value.", e1);
			return;
		}

		if (newValue == null)
			return;

		final Class<?> keyClass = dm.getMappingColumnType();
		final Map<Object, Object> changes = new HashMap<Object, Object>();
		for (int i = 0; i < selected.length; i++) {
			final Item currentItem = ((Item) propertySheetPanel.getTable().getValueAt(selected[i], 0));
			// First, update property sheet
			currentItem.getProperty().setValue(newValue);
			// Then update .
			Object key = currentItem.getProperty().getDisplayName();

			// If not String, need to parse actual value
			try {
				if (keyClass == Integer.class)
					key = Integer.valueOf((String) key);
				else if (keyClass == Double.class)
					key = Double.valueOf((String) key);
				else if (keyClass == Boolean.class)
					key = Boolean.valueOf((String) key);
				else if (keyClass == Float.class)
					key = Float.valueOf((String) key);
			} catch (Exception ex) {
				logger.warn("Could not parse discrete mapping key value.  Ignored: " + key, e);
				continue;
			}

			changes.put(key, newValue);
		}
		
		dm.putAll(changes);

		table.repaint();
	}
}
