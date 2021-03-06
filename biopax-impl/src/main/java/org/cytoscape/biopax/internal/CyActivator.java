package org.cytoscape.biopax.internal;

/*
 * #%L
 * Cytoscape BioPAX Impl (biopax-impl)
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

//import org.biopax.paxtools.io.SimpleIOHandler;
//import org.biopax.paxtools.model.Model;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

import org.cytoscape.biopax.internal.BioPaxFilter;
import org.cytoscape.biopax.internal.action.BioPaxViewTracker;
import org.cytoscape.biopax.internal.BioPaxReaderTaskFactory;
import org.cytoscape.biopax.internal.util.BioPaxVisualStyleUtil;
import org.cytoscape.biopax.internal.view.BioPaxContainer;
import org.cytoscape.biopax.internal.view.BioPaxDetailsPanel;
import org.cytoscape.biopax.internal.view.BioPaxCytoPanelComponent;
import org.cytoscape.biopax.internal.action.LaunchExternalBrowser;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.io.read.InputStreamTaskFactory;


import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;

import java.util.Properties;



public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {

		CySwingApplication cySwingApplicationRef = getService(bc,CySwingApplication.class);
		OpenBrowser openBrowserRef = getService(bc,OpenBrowser.class);
		CyApplicationManager cyApplicationManagerRef = getService(bc,CyApplicationManager.class);
		CyNetworkViewManager cyNetworkViewManagerRef = getService(bc,CyNetworkViewManager.class);
		CyNetworkNaming cyNetworkNamingRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryRef = getService(bc,CyNetworkFactory.class);
		CyNetworkViewFactory cyNetworkViewFactoryRef = getService(bc,CyNetworkViewFactory.class);
		StreamUtil streamUtilRef = getService(bc,StreamUtil.class);
		VisualMappingManager visualMappingManagerRef = getService(bc,VisualMappingManager.class);
		VisualStyleFactory visualStyleFactoryRef = getService(bc,VisualStyleFactory.class);
		VisualMappingFunctionFactory discreteMappingFunctionFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughMappingFunctionFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=passthrough)");
		CyLayoutAlgorithmManager cyLayoutsRef = getService(bc,CyLayoutAlgorithmManager.class);	
		TaskManager taskManagerRef = getService(bc, DialogTaskManager.class);
		CyProperty<Properties> cytoscapePropertiesServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyGroupFactory cyGroupFactory = getService(bc, CyGroupFactory.class);
		
		BioPaxFilter bioPaxFilter = new BioPaxFilter(streamUtilRef);
		LaunchExternalBrowser launchExternalBrowser = new LaunchExternalBrowser(openBrowserRef);	
		BioPaxDetailsPanel bioPaxDetailsPanel = new BioPaxDetailsPanel(launchExternalBrowser);
		BioPaxContainer bioPaxContainer = new BioPaxContainer(launchExternalBrowser,cyApplicationManagerRef,cyNetworkViewManagerRef,bioPaxDetailsPanel,cySwingApplicationRef);
		BioPaxVisualStyleUtil bioPaxVisualStyleUtil = new BioPaxVisualStyleUtil(visualStyleFactoryRef,visualMappingManagerRef,discreteMappingFunctionFactoryRef,passthroughMappingFunctionFactoryRef);
		BioPaxViewTracker bioPaxViewTracker = new BioPaxViewTracker(bioPaxDetailsPanel,bioPaxContainer, cyApplicationManagerRef, 
				visualMappingManagerRef, bioPaxVisualStyleUtil, cyLayoutsRef, taskManagerRef, cytoscapePropertiesServiceRef, cySwingApplicationRef);
		InputStreamTaskFactory inputStreamTaskFactory = new BioPaxReaderTaskFactory(bioPaxFilter,cyNetworkFactoryRef,cyNetworkViewFactoryRef,cyNetworkNamingRef, cyGroupFactory);
		CytoPanelComponent cytoPanelComponent = new BioPaxCytoPanelComponent(bioPaxContainer);
		
		// register/export osgi services
		registerService(bc,inputStreamTaskFactory,InputStreamTaskFactory.class, new Properties());
		registerService(bc,cytoPanelComponent,CytoPanelComponent.class, new Properties());
		registerService(bc,(RowsSetListener)bioPaxViewTracker,RowsSetListener.class, new Properties());
		registerService(bc,(NetworkViewAddedListener)bioPaxViewTracker,NetworkViewAddedListener.class, new Properties());
		registerService(bc,(NetworkViewAboutToBeDestroyedListener)bioPaxViewTracker,NetworkViewAboutToBeDestroyedListener.class, new Properties());
		registerService(bc,(SetCurrentNetworkViewListener)bioPaxViewTracker,SetCurrentNetworkViewListener.class, new Properties());
		
		
		//quick sanity test
//		try {
//			Model model = (new SimpleIOHandler())
//				.convertFromOWL(this.getClass().getResourceAsStream("biopax3-short-metabolic-pathway.owl"));
//			System.out.println("Started biopax-impl.");
//		} catch (Throwable t) {
//			System.out.println("Failed test biopax-impl.");
//			t.printStackTrace();
//		}
		
		// TODO set paxtools logging level to ERROR
		
	}
}

