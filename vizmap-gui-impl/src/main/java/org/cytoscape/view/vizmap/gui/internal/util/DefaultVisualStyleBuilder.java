package org.cytoscape.view.vizmap.gui.internal.util;

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

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_LABEL;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_LABEL_COLOR;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_PAINT;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NETWORK_BACKGROUND_PAINT;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_FILL_COLOR;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_HEIGHT;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_LABEL;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_LABEL_COLOR;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_WIDTH;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Builder object for default Visual Style.
 * 
 * <p>
 * If default visual style does not exists, create one by this builder.
 * <p>
 * Rendering-engine specific style can be created from this style by adding 
 * extra properties, such as Shape.
 *
 */
public class DefaultVisualStyleBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultVisualStyleBuilder.class);
	
	// Name of default visual style.
	private static final String DEFAULT_VS_NAME = "default";
	
	// TODO: replace these!!
	private static final String NAME = "name";
	private static final String INTERACTION = "interaction";
	
	// Preset parameters for default style
	private static final Color DEFAULT_NODE_COLOR = new Color(0x79CDCD);
	private static final Color DEFAULT_NODE_LABEL_COLOR = new Color(0x1C1C1C);

	private static final Color DEFAULT_EDGE_COLOR = new Color(0x80, 0x80, 0x80);
	private static final Color DEFAULT_EDGE_LABEL_COLOR = new Color(50, 50, 255);
	
	private static final Color DEFAULT_BACKGROUND_COLOR = Color.white;
	
	private static final double DEFAULT_NODE_WIDTH = 55.0d;
	private static final double DEFAULT_NODE_HEIGHT = 30.0d;
		
	
	// This should be injected
	private final VisualStyleFactory vsFactory;
	
	// Each lexicon has its own defaults.
	private final Map<VisualLexicon, VisualStyle> styleMap;
	private final VisualMappingFunctionFactory passthroughMappingFactory; 
	
	
	public DefaultVisualStyleBuilder(final VisualStyleFactory vsFactory, 
	                                 final VisualMappingFunctionFactory passthroughMappingFactory) {
		this.vsFactory = vsFactory;
		this.passthroughMappingFactory = passthroughMappingFactory;
		this.styleMap = new HashMap<VisualLexicon, VisualStyle>();
	}
	
	public VisualStyle getDefaultStyle(final VisualLexicon lexicon) {
		
		VisualStyle defStyle = styleMap.get(lexicon);
		if(defStyle == null) {
			defStyle = buildDefaultStyle(lexicon);
			styleMap.put(lexicon, defStyle);
		}
		
		logger.debug("Default Style Created: " + defStyle.getTitle());
		
		return defStyle;
	}
	
	
	private VisualStyle buildDefaultStyle(final VisualLexicon lexicon) {

		// Create new style 
		final VisualStyle newStyle = vsFactory.createVisualStyle(DEFAULT_VS_NAME);
		
		// Set node appearance
		newStyle.setDefaultValue(NODE_FILL_COLOR, DEFAULT_NODE_COLOR );
		newStyle.setDefaultValue(NODE_LABEL_COLOR, DEFAULT_NODE_LABEL_COLOR );
		newStyle.setDefaultValue(NODE_WIDTH, DEFAULT_NODE_WIDTH );
		newStyle.setDefaultValue(NODE_HEIGHT, DEFAULT_NODE_HEIGHT );
		
		// Set edge appearance
		newStyle.setDefaultValue(EDGE_PAINT, DEFAULT_EDGE_COLOR );
		newStyle.setDefaultValue(EDGE_LABEL_COLOR, DEFAULT_EDGE_LABEL_COLOR );
		
		// Set network appearance
		newStyle.setDefaultValue(NETWORK_BACKGROUND_PAINT, DEFAULT_BACKGROUND_COLOR );
		
		// Create label mappings
		final PassthroughMapping<String, String> labelMapping = (PassthroughMapping) passthroughMappingFactory
				.createVisualMappingFunction(NAME, String.class, NODE_LABEL);
		final PassthroughMapping<String, String> edgeLabelMapping = (PassthroughMapping) passthroughMappingFactory
				.createVisualMappingFunction(INTERACTION, String.class, EDGE_LABEL);

		newStyle.addVisualMappingFunction(labelMapping);
		newStyle.addVisualMappingFunction(edgeLabelMapping);
		
		return newStyle;
	}

}
