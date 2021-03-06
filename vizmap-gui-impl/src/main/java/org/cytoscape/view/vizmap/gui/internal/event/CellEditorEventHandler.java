package org.cytoscape.view.vizmap.gui.internal.event;

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

import java.beans.PropertyChangeEvent;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.event.TableModelListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.Visualizable;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.gui.event.VizMapEventHandler;
import org.cytoscape.view.vizmap.gui.internal.AttributeSet;
import org.cytoscape.view.vizmap.gui.internal.AttributeSetManager;
import org.cytoscape.view.vizmap.gui.internal.VizMapPropertySheetBuilder;
import org.cytoscape.view.vizmap.gui.internal.VizMapperProperty;
import org.cytoscape.view.vizmap.gui.internal.editor.propertyeditor.AttributeComboBoxPropertyEditor;
import org.cytoscape.view.vizmap.gui.internal.util.VizMapperUtil;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;

// TODO: Should be refactored for readability!!
/**
 *
 */
public final class CellEditorEventHandler implements VizMapEventHandler {

	private static final Logger logger = LoggerFactory.getLogger(CellEditorEventHandler.class);

	private final CyNetworkTableManager tableMgr;

	protected final VizMapPropertySheetBuilder vizMapPropertySheetBuilder;
	protected final PropertySheetPanel propertySheetPanel;
	protected final CyApplicationManager applicationManager;
	private final VisualMappingManager vmm;

	private final AttributeSetManager attrManager;

	private final VizMapperUtil util;

	/**
	 * Creates a new CellEditorEventHandler object.
	 */
	public CellEditorEventHandler(final PropertySheetPanel propertySheetPanel, final CyNetworkTableManager tableMgr,
			final CyApplicationManager applicationManager, final VizMapPropertySheetBuilder vizMapPropertySheetBuilder,
			final AttributeSetManager attrManager, final VizMapperUtil util, final VisualMappingManager vmm) {

		this.propertySheetPanel = propertySheetPanel;
		this.tableMgr = tableMgr;
		this.applicationManager = applicationManager;
		this.vizMapPropertySheetBuilder = vizMapPropertySheetBuilder;
		this.attrManager = attrManager;
		this.util = util;
		this.vmm = vmm;
	}

	/**
	 * Execute commands based on PropertyEditor's local event.
	 * 
	 * In this handler, we should handle the following:
	 * <ul>
	 * <li>Mapping Type change
	 * <li>Attribute Name Change
	 * </ul>
	 * 
	 * Other old global events (ex. Cytoscape.NETWORK_LOADED) is replaced by new
	 * events.
	 * 
	 * @param e
	 *            PCE to be processed in this handler.
	 */
	@Override
	public void processEvent(final PropertyChangeEvent e) {
		final Object newVal = e.getNewValue();
		final Object oldVal = e.getOldValue();

		// Check update is necessary or not.
		if (newVal == null && oldVal == null)
			return;

		// Same value. No change required.
		if (newVal != null && newVal.equals(oldVal))
			return;

		// find selected cell
		final PropertySheetTable table = propertySheetPanel.getTable();
		final int selected = table.getSelectedRow();

		// If nothing selected, ignore.
		if (selected < 0)
			return;

		// Extract selected Property object in the table.
		final Item selectedItem = (Item) propertySheetPanel.getTable().getValueAt(selected, 0);
		final VizMapperProperty<?, ?, ?> prop = (VizMapperProperty<?, ?, ?>) selectedItem.getProperty();

		if (prop == null)
			return;
		
		VisualProperty<?> type = null;

		if (prop.getCellType() == CellType.VISUAL_PROPERTY_TYPE) {
			// Case 1: Attribute type changed.
			if (newVal != null && e.getSource() instanceof AttributeComboBoxPropertyEditor) {
				final AttributeComboBoxPropertyEditor editor = (AttributeComboBoxPropertyEditor) e.getSource();
				processTableColumnChange(newVal.toString(), prop, editor);
			}
		} else if (prop.getCellType() == CellType.MAPPING_TYPE) {
			// Case 2. Switch mapping type
			// Parent is always root.
			final VizMapperProperty<?, ?, ?> parent = (VizMapperProperty<?, ?, ?>) prop.getParentProperty();
			type = (VisualProperty<?>) parent.getKey();
			Object controllingAttrName = parent.getValue();

			if (type == null || controllingAttrName == null)
				return;

			logger.debug("New Type = " + type.getDisplayName());
			logger.debug("New Attr Name = " + controllingAttrName);

			switchMappingType(prop, type, (VisualMappingFunctionFactory) e.getNewValue(),
					controllingAttrName.toString());
		} else if (prop.getParentProperty() != null) {
			// Case 3: Discrete Cell editor event. Create new map entry and
			// register it.
			logger.debug("Cell edit event: name = " + prop.getName());
			logger.debug("Cell edit event: old val = " + prop.getValue());
			logger.debug("Cell edit event: new val = " + newVal);
			logger.debug("Cell edit event: associated mapping = " + prop.getInternalValue());

			final VisualMappingFunction<?, ?> mapping = (VisualMappingFunction<?, ?>) prop.getInternalValue();

			if (mapping == null)
				return;

			if (mapping instanceof DiscreteMapping) {
				DiscreteMapping<Object, Object> discMap = (DiscreteMapping<Object, Object>) mapping;
				discMap.putMapValue(prop.getKey(), newVal);
			}

//			vmm.getCurrentVisualStyle().apply(applicationManager.getCurrentNetworkView());
			//applicationManager.getCurrentNetworkView().updateView();
		}
	}

	private <K, V> void switchControllingAttr(final VisualMappingFunctionFactory factory,
			final AttributeComboBoxPropertyEditor editor, VizMapperProperty<K, V, ?> prop, final String ctrAttrName) {
		final VisualStyle currentStyle = vmm.getCurrentVisualStyle();

		final VisualProperty<V> vp = (VisualProperty<V>) prop.getKey();
		VisualMappingFunction<K, V> mapping = (VisualMappingFunction<K, V>) currentStyle.getVisualMappingFunction(vp);

		/*
		 * Ignore if not compatible.
		 */
		@SuppressWarnings("unchecked")
		Class<? extends CyIdentifiable> type = (Class<? extends CyIdentifiable>) editor.getTargetObjectType();
		final CyTable attrForTest = tableMgr.getTable(applicationManager.getCurrentNetwork(), type, CyNetwork.DEFAULT_ATTRS);

		final CyColumn column = attrForTest.getColumn(ctrAttrName);
		if (column == null)
			return;

		final Class<K> dataType = (Class<K>) column.getType();

		if (mapping == null) {
			// Need to create new one
			logger.debug("Mapping is still null: " + ctrAttrName);

			if (factory == null)
				return;

			mapping = factory.createVisualMappingFunction(ctrAttrName, dataType, vp);
			currentStyle.addVisualMappingFunction(mapping);
		}

		// If same, do nothing.
		if (ctrAttrName.equals(mapping.getMappingColumnName())) {
			logger.debug("Same controlling attr.  Do nothing for: " + ctrAttrName);
			return;
		}

		
		/////// Create new Mapping ///////
		
		VisualMappingFunction<K, V> newMapping = null;
		if (mapping instanceof PassthroughMapping) {
			// Create new Passthrough mapping and register to current style.
			newMapping = factory.createVisualMappingFunction(ctrAttrName, dataType, vp);
			
			logger.debug("Changed to new Map from " + mapping.getMappingColumnName() + " to "
					+ newMapping.getMappingColumnName());
		} else if (mapping instanceof ContinuousMapping) {
			if ((dataType == Double.class) || (dataType == Integer.class) || (dataType == Long.class)
					|| (dataType == Float.class) || (dataType == Byte.class)) {
				newMapping = factory.createVisualMappingFunction(ctrAttrName, dataType, vp);
			} else {
				JOptionPane.showMessageDialog(null,
						"Continuous Mapper can be used with Numbers only.\nPlease select a numerical column type.",
						"Incompatible Mapping Type.", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
		} else if (mapping instanceof DiscreteMapping) {
			newMapping = factory.createVisualMappingFunction(ctrAttrName, dataType, vp);
			logger.debug("Changed to new Map from " + mapping.getMappingColumnName() + " to "
					+ newMapping.getMappingColumnName());
		}
		
		
		// Register the new mapping
		if(newMapping != null) {
			currentStyle.addVisualMappingFunction(newMapping);
		} else {
			throw new NullPointerException("Mapping function is null.");
		}

		// Remove old property
		propertySheetPanel.removeProperty(prop);
		
		// Create new Property.
		final VisualProperty<Visualizable> category = util.getCategory((Class<? extends CyIdentifiable>) vp
				.getTargetDataType());
		VizMapperProperty<VisualProperty<V>, String, ?> newRootProp = vizMapPropertySheetBuilder.getPropertyBuilder()
				.buildProperty(newMapping, category.getDisplayName(), propertySheetPanel, factory);

		vizMapPropertySheetBuilder.removeProperty(prop, currentStyle);
		final List<Property> propList = vizMapPropertySheetBuilder.getPropertyList(currentStyle);
		propList.add(newRootProp);

		prop = null;

		vizMapPropertySheetBuilder.expandLastSelectedItem(vp.getIdString());
		vizMapPropertySheetBuilder.updateTableView();

		// Finally, update graph view and focus.
//		currentStyle.apply(applicationManager.getCurrentNetworkView());
//		applicationManager.getCurrentNetworkView().updateView();
		return;

	}

	private void switchMappingType(final VizMapperProperty<?, ?, ?> prop, final VisualProperty<?> vp,
			final VisualMappingFunctionFactory factory, final String controllingAttrName) {
		// This is the currently selected Visual Style.
		final VisualStyle style = vmm.getCurrentVisualStyle();
		final VisualProperty<Visualizable> startVP = util.getCategory((Class<? extends CyIdentifiable>) vp
				.getTargetDataType());
		final VisualMappingFunction<?, ?> currentMapping = style.getVisualMappingFunction(vp);

		logger.debug("Current Mapping for " + vp.getDisplayName() + " is: " + currentMapping);

		final VisualMappingFunction<?, ?> newMapping;
		logger.debug("!! New factory Category: " + factory.getMappingFunctionType());
		logger.debug("!! Current Mapping type: " + currentMapping);

		if (currentMapping == null || currentMapping.getClass() != factory.getMappingFunctionType()) {
			final CyNetwork currentNet = applicationManager.getCurrentNetwork();
			
			if (currentNet == null)
				return;
				
			// Mapping does not exist. Need to create new one.
			final AttributeSet attrSet = attrManager.getAttributeSet(currentNet, vp.getTargetDataType());
			final Class<?> attributeDataType = attrSet.getAttrMap().get(controllingAttrName);

			if (factory.getMappingFunctionType() == ContinuousMapping.class) {
				if (attributeDataType == null) {
					JOptionPane.showMessageDialog(null, "The current table does not have the selected column (\""
							+ controllingAttrName + "\").\nPlease select another column.", "Invalid Column.",
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				if (!Number.class.isAssignableFrom(attributeDataType)) {
					JOptionPane.showMessageDialog(null,
							"Selected column data type is not Number.\nPlease select a numerical column type.",
							"Incompatible Column Type.", JOptionPane.WARNING_MESSAGE);
					return;
				}
			}

			newMapping = factory.createVisualMappingFunction(controllingAttrName, attributeDataType, vp);
			style.addVisualMappingFunction(newMapping);
		} else {
			newMapping = currentMapping;
		}

		// Disable listeners to avoid unnecessary updates
		final PropertySheetTableModel model = (PropertySheetTableModel) this.propertySheetPanel.getTable().getModel();
		final TableModelListener[] modelListeners = model.getTableModelListeners();
		
		for (final TableModelListener tm : modelListeners)
			model.removeTableModelListener(tm);

		logger.debug("New VisualMappingFunction Created: Mapping Type = "
				+ style.getVisualMappingFunction(vp).toString());
		logger.debug("New VisualMappingFunction Created: Controlling attr = "
				+ style.getVisualMappingFunction(vp).getMappingColumnName());

		// First, remove current property
		Property parent = prop.getParentProperty();
		propertySheetPanel.removeProperty(parent);

		final VizMapperProperty<?, ?, VisualMappingFunctionFactory> newRootProp;

		newRootProp = vizMapPropertySheetBuilder.getPropertyBuilder().buildProperty(newMapping,
				startVP.getDisplayName(), propertySheetPanel, factory);

		vizMapPropertySheetBuilder.expandLastSelectedItem(vp.getDisplayName());
		vizMapPropertySheetBuilder.removeProperty(parent, style);

		final List<Property> propList = vizMapPropertySheetBuilder.getPropertyList(style);
		propList.add(newRootProp);

		parent = null;
//		final VisualStyle currentStyle = vmm.getCurrentVisualStyle();
//		currentStyle.apply(applicationManager.getCurrentNetworkView());
		
		//applicationManager.getCurrentNetworkView().updateView();

		vizMapPropertySheetBuilder.updateTableView();

		// Restore listeners
		for (final TableModelListener tm : modelListeners)
			model.addTableModelListener(tm);
	}

	private void processTableColumnChange(final String newColumnName, final VizMapperProperty<?, ?, ?> prop,
			final AttributeComboBoxPropertyEditor editor) {

		VisualMappingFunctionFactory factory = (VisualMappingFunctionFactory) prop.getInternalValue();
		if (factory == null) {
			Property[] children = prop.getSubProperties();
			for (int i = 0; i < children.length; i++) {
				final VizMapperProperty<?, ?, ?> child = (VizMapperProperty<?, ?, ?>) children[i];
				if (child.getCellType().equals(CellType.MAPPING_TYPE)
						&& child.getValue() instanceof VisualMappingFunctionFactory) {
					factory = (VisualMappingFunctionFactory) child.getValue();
					break;
				}
			}
			if (factory == null)
				return;
		}
		switchControllingAttr(factory, editor, prop, newColumnName);
	}
}
