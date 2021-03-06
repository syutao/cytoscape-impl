package org.cytoscape.view.vizmap.gui.internal;

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
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.gui.internal.task.generators.GenerateValuesTaskFactory;
import org.cytoscape.view.vizmap.gui.util.DiscreteMappingGenerator;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * Manager for all Vizmap-local menu items.
 * 
 */
public class VizMapperMenuManager {

	private static final Logger logger = LoggerFactory.getLogger(VizMapperMenuManager.class);

	// Metadata
	private static final String METADATA_MENU_KEY = "menu";
	private static final String METADATA_TITLE_KEY = "title";

	private static final String MAIN_MENU = "main";
	private static final String CONTEXT_MENU = "context";

	// Menu items under the tool button
	private final JPopupMenu mainMenu;

	// Context menu
	private final JPopupMenu rightClickMenu;

	// Context Menu Preset items
	private final JMenu edit;
	private final JMenu generateValues;

	private final TaskManager taskManager;

	private final PropertySheetPanel panel;
	private final VisualMappingManager vmm;

	public VizMapperMenuManager(final TaskManager taskManager, final PropertySheetPanel panel,
			final VisualMappingManager vmm) {

		if (taskManager == null)
			throw new NullPointerException("TaskManager is null.");

		this.taskManager = taskManager;
		this.panel = panel;
		this.vmm = vmm;

		// Will be shown under the button next to Visual Style Name
		mainMenu = new JPopupMenu();

		// Context menu
		rightClickMenu = new JPopupMenu();
		this.edit = new JMenu("Edit");
		rightClickMenu.add(edit);
		this.generateValues = new JMenu("Mapping Value Generators");
		this.rightClickMenu.add(generateValues);

	}

	public JPopupMenu getMainMenu() {
		return mainMenu;
	}

	public JPopupMenu getContextMenu() {
		return rightClickMenu;
	}

	/**
	 * Custom listener for dynamic menu management
	 * 
	 */
	public void onBind(final CyAction action, Map properties) {
		final Object serviceType = properties.get("service.type");
		if (serviceType != null && serviceType.toString().equals("vizmapUI.contextMenu")) {
			Object menuTitle = properties.get("title");
			if (menuTitle == null)
				throw new NullPointerException("Title is missing for a menu item");

			final JMenuItem menuItem = new JMenuItem(menuTitle.toString());
			menuItem.addActionListener(action);
			edit.add(menuItem);
		}
	}

	public void onUnbind(final CyAction service, Map properties) {
		// FIXME: implement this
	}

	/**
	 * Add menu items to proper locations.
	 * 
	 * @param taskFactory
	 * @param properties
	 */
	public void addTaskFactory(final TaskFactory taskFactory, @SuppressWarnings("rawtypes") Map properties) {

		// first filter the service...
		final Object serviceType = properties.get("service.type");
		if (serviceType == null || !(serviceType instanceof String)
				|| !((String) serviceType).equals("vizmapUI.taskFactory"))
			return;

		final Object menuDef = properties.get(METADATA_MENU_KEY);
		if (menuDef == null)
			throw new NullPointerException("Menu metadata is missing.");

		// This is a menu item for Main Command Button.
		final Object title = properties.get(METADATA_TITLE_KEY);
		if (title == null)
			throw new NullPointerException("Title metadata is missing.");

		// Add new menu to the pull-down
		final JMenuItem menuItem = new JMenuItem(title.toString());
		// menuItem.setIcon(iconManager.getIcon(iconId));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				taskManager.execute(taskFactory.createTaskIterator());
			}
		});

		if (menuDef.toString().equals(MAIN_MENU))
			mainMenu.add(menuItem);
		else if (menuDef.toString().equals(CONTEXT_MENU))
			edit.add(menuItem);
	}

	public void removeTaskFactory(final TaskFactory taskFactory, Map properties) {
	}

	public void addMappingGenerator(final DiscreteMappingGenerator<?> generator,
			@SuppressWarnings("rawtypes") Map properties) {
		final Object serviceType = properties.get(METADATA_MENU_KEY);
		if (serviceType == null)
			throw new NullPointerException("Service Type metadata is null.  This value is required.");

		// This is a menu item for Main Command Button.
		final Object title = properties.get(METADATA_TITLE_KEY);
		if (title == null)
			throw new NullPointerException("Title metadata is missing.");

		// Create mapping generator task factory
		final GenerateValuesTaskFactory taskFactory = new GenerateValuesTaskFactory(generator, panel, vmm);

		// Add new menu to the pull-down
		final JMenuItem menuItem = new JMenuItem(title.toString());
		// menuItem.setIcon(iconManager.getIcon(iconId));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				taskManager.execute(taskFactory.createTaskIterator());
			}
		});

		generateValues.add(menuItem);

	}

	public void removeMappingGenerator(final DiscreteMappingGenerator<?> generator,
			@SuppressWarnings("rawtypes") Map properties) {
		// FIXME
	}

}
