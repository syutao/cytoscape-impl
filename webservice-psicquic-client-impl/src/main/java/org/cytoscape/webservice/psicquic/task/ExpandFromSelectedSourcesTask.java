package org.cytoscape.webservice.psicquic.task;

/*
 * #%L
 * Cytoscape PSIQUIC Web Service Impl (webservice-psicquic-client-impl)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.mapper.MergedNetworkBuilder;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;

import uk.ac.ebi.enfin.mi.cluster.InteractionCluster;

public class ExpandFromSelectedSourcesTask extends AbstractTask {
	
	@Tunable(description="Select Data Source")
	public ListMultipleSelection<String> services;
	
	private final PSICQUICRestClient client;
	private final MergedNetworkBuilder builder;

	private final String query;

	private final CyLayoutAlgorithmManager layouts;

	private final CyNetworkView netView;
	private final View<CyNode> nodeView;

	private final CyEventHelper eh;
	private final VisualMappingManager vmm;

	private volatile boolean canceled = false;
	
	private final Map<String, String> sourceMap;

	public ExpandFromSelectedSourcesTask(final String query, final PSICQUICRestClient client, final Map<String, String> sourceMap,
			final CyNetworkView parentNetworkView, final View<CyNode> nodeView, final CyEventHelper eh,
			final VisualMappingManager vmm, final CyLayoutAlgorithmManager layouts, final MergedNetworkBuilder builder) {
		this.client = client;
		this.query = query;
		this.netView = parentNetworkView;
		this.nodeView = nodeView;
		this.eh = eh;
		this.vmm = vmm;
		this.layouts = layouts;
		this.sourceMap = sourceMap;
		this.builder = builder;
		
		final List<String> sourceNames = new ArrayList<String>(sourceMap.keySet());
		services = new ListMultipleSelection<String>(sourceNames);
		services.setSelectedValues(sourceNames);		
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (query == null)
			throw new NullPointerException("Query is null");
		
		taskMonitor.setProgress(0.01d);
		List<String> selected = services.getSelectedValues();
		Collection<String> targetServices = new HashSet<String>();
		for(String targetURL: selected) {
			targetServices.add(sourceMap.get(targetURL));
		}
		// Switch task type based on the user option.
		InteractionCluster ic = client.importNeighbours(query, targetServices, SearchMode.INTERACTOR, taskMonitor);

		if (canceled) {
			ic = null;
			return;
		}

		taskMonitor.setProgress(0.8d);
		expand(ic);
		
		final CyLayoutAlgorithm layout = layouts.getLayout("force-directed");
		
		Set<View<CyNode>> entries = new HashSet<View<CyNode>>();
		for (View<CyNode> item : netView.getNodeViews()) {
			CyRow row = netView.getModel().getRow(item.getModel());
			if (row.get(CyNetwork.SELECTED, Boolean.class)) {
				entries.add(item);
			}
		}
		
		TaskIterator itr = layout.createTaskIterator(netView, layout.getDefaultLayoutContext(), entries, "");
		this.insertTasksAfterCurrentTask(itr);
		taskMonitor.setProgress(1.0d);
		
	}
	
	private void expand(final InteractionCluster iC) {
		eh.flushPayloadEvents();
		builder.addToNetwork(iC, netView, nodeView);
		eh.flushPayloadEvents();

		// Apply visual style
		final VisualStyle vs = vmm.getVisualStyle(netView);
		vs.apply(netView);
		netView.updateView();
	}

	@Override
	public void cancel() {
		this.canceled = true;
		client.cancel();
	}

}
