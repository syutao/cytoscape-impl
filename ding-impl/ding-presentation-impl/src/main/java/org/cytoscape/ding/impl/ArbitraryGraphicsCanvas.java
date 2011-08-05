/*
  File: ArbitraryGraphicsCanvas.java

  Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.ding.impl;


import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.ding.NodeView;
import org.cytoscape.ding.impl.events.ViewportChangeListener;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


/**
 * This class extends cytoscape.view.CytoscapeCanvas.  Its meant
 * to live within a org.cytoscape.ding.impl.DGraphView class.  It is the canvas
 * used for arbitrary graphics drawing (background & foreground panes).
 */
public class ArbitraryGraphicsCanvas extends DingCanvas implements ViewportChangeListener {
	private final static long serialVersionUID = 1202416510975364L;

	/**
	 * Our reference to the GraphPerspective our view belongs to
	 */
	private CyNetwork m_cyNetwork;

	/**
	 * Our reference to the DGraphView we live within
	 */
	private DGraphView m_dGraphView;

	/**
	 * Our reference to the inner canvas
	 */
	private InnerCanvas m_innerCanvas;

	/*
	 * Map of component(s) to hidden node(s)
	 */        
	private Map<Component, CyNode> m_componentToNodeMap;

	/**
	 * Constructor.
	 *
	 * @param cyNetwork GraphPerspective
	 * @param dGraphView DGraphView
	 * @param innerCanvas InnerCanvas
	 * @param backgroundColor Color
	 * @param isVisible boolean
	 * @param isOpaque boolean
	 */
	public ArbitraryGraphicsCanvas(CyNetwork cyNetwork, DGraphView dGraphView,
	                               InnerCanvas innerCanvas, Color backgroundColor,
	                               boolean isVisible, boolean isOpaque) {
		// init members
		m_cyNetwork = cyNetwork;
		m_dGraphView = dGraphView;
		m_innerCanvas = innerCanvas;
		m_backgroundColor = backgroundColor;
		m_isVisible = isVisible;
		m_isOpaque = isOpaque;
		m_componentToNodeMap = new HashMap<Component, CyNode>();
	}

	/**
	 * Our implementation of add
	 */
        @Override
	public Component add(Component component) {
		// do our stuff
		return super.add(component);
	}
        
	/**
	 * Our implementation of ViewportChangeListener.
	 */
	public void viewportChanged(int viewportWidth, int viewportHeight, double newXCenter,
	                            double newYCenter, double newScaleFactor) {
	}

	public void modifyComponentLocation(int x,int y, int componentNum){
	}

	/**
	 * Our implementation of JComponent setBounds.
	 */
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
                
		// our bounds have changed, create a new image with new size
		if ((width > 1) && (height > 1)) {
			// create the buffered image
			m_img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}
	}

	/**
	 * Our implementation of paint.
	 * Invoked by Swing to draw components.
	 *
	 * @param graphics Graphics
	 */
	public void paint(Graphics graphics) {
		// only paint if we have an image to paint on
		if (m_img != null) {
			// get image graphics
			final Graphics2D image2D = ((BufferedImage) m_img).createGraphics();

			// first clear the image
			clearImage(image2D);

			// now paint children
			if (m_isVisible){
				int num=this.getComponentCount();
				for(int i=0;i<num;i++){
					this.getComponent(i).paint(image2D);
				}
			}
			image2D.dispose();
			// render image
			graphics.drawImage(m_img, 0, 0, null);
		}
                
	}


	@Override
	public Component getComponentAt(int x, int y) {

		int n=getComponentCount();

		for(int i=0;i<n;i++){
				Component c=this.getComponent(i).getComponentAt(x, y);

				if(c!=null)
						return c;
		}
		return null;
	}


	/**
	 * Invoke this method to print the component.
	 *
	 * @param graphics Graphics
	 */
	public void print(Graphics graphics) {
		int num=this.getComponentCount();
		for(int i=0;i<num;i++){
			this.getComponent(i).print(graphics);
		}
	}

	/**
	 * Utility function to clean the background of the image,
	 * using m_backgroundColor
	 *
	 * image2D Graphics2D
	 */
	private void clearImage(Graphics2D image2D) {
		// set color alpha based on opacity setting
		int alpha = (m_isOpaque) ? 255 : 0;
		Color backgroundColor = new Color(m_backgroundColor.getRed(), m_backgroundColor.getGreen(),
		                                  m_backgroundColor.getBlue(), alpha);

		// set the alpha composite on the image, and clear its area
		Composite origComposite = image2D.getComposite();
		image2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		image2D.setPaint(backgroundColor);
		image2D.fillRect(0, 0, m_img.getWidth(null), m_img.getHeight(null));
		image2D.setComposite(origComposite);
	}
}
