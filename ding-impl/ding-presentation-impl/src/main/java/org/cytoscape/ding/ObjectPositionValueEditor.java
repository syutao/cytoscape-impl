/*
 File: PopupLabelPositionChooser.java

 Copyright (c) 2006, The Cytoscape Consortium (www.cytoscape.org)

 The Cytoscape Consortium is:
 - Institute for Systems Biology
 - University of California San Diego
 - Memorial Sloan-Kettering Cancer Center
 - Institut Pasteur
 - Agilent Technologies

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
package org.cytoscape.ding;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.cytoscape.ding.impl.ObjectPositionImpl;
import org.cytoscape.view.vizmap.gui.editor.ValueEditor;

/**
 * Swing implementation of Object Position editor.
 * 
 */
public class ObjectPositionValueEditor extends JDialog implements ValueEditor<ObjectPosition> {

	private static final long serialVersionUID = 7146654020668346430L;

	// Current position
	private String label;
	
	private ObjectPosition oldValue;
	
	private boolean canceled = false;
	
	private ObjectPlacerGraphic graphic;
	private ObjectPlacerControl control;
	
	public ObjectPositionValueEditor() {
		super();
		this.label = "Object";
		
		this.setModal(true);
		init();
	}

	private void init() {
		
		setTitle("Select Position");

		final JPanel placer = new JPanel();
		
		placer.setLayout(new BoxLayout(placer, BoxLayout.Y_AXIS));
		placer.setOpaque(true); // content panes must be opaque

		// Set up and connect the gui components.
		graphic = new ObjectPlacerGraphic(null, true, label);
		control = new ObjectPlacerControl();

		control.addPropertyChangeListener(graphic);
		//control.addPropertyChangeListener(this);

		graphic.addPropertyChangeListener(control);
		//graphic.addPropertyChangeListener(this);

		placer.add(graphic);
		placer.add(control);

		final JPanel buttonPanel = new JPanel();
		final JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				dispose();
			}
		});
		ok.addActionListener(control);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canceled = true;
				dispose();
			}
		});

		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		placer.add(buttonPanel);
		add(placer);

		pack();
	}


	@Override
	public <S extends ObjectPosition> ObjectPosition showEditor(Component parent, S initialValue) {

		ObjectPosition pos;

		if (initialValue == null) {
			oldValue = null;
			pos = new ObjectPositionImpl();
		} else {
			oldValue = initialValue;
			pos = new ObjectPositionImpl(initialValue);
		}

		control.setPosition(pos);
		graphic.setPosition(pos);

		if (parent != null)
			this.setLocationRelativeTo(parent);
		else
			this.setLocationByPlatform(true);

		this.setVisible(true);

		if (canceled) {
			canceled = false;
			return oldValue;
		} else {
			return control.getPosition();
		}
	}


	@Override
	public Class<ObjectPosition> getValueType() {
		return ObjectPosition.class;
	}
}
