package org.cytoscape.ding.impl;

import java.util.Properties;

import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyApplicationManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class ShowGraphicsDetailTaskFactory implements TaskFactory {

    private final CyApplicationManager appManager;
    private final CyProperty<Properties> defaultProps;
    
    ShowGraphicsDetailTaskFactory(final CyApplicationManager appManager, final CyProperty<Properties> defaultProps) {
	this.appManager = appManager;
	this.defaultProps = defaultProps;
    }
    
    @Override
    public TaskIterator getTaskIterator() {
	return new TaskIterator(new ShowGraphicsDetailTask(defaultProps, appManager));
    }

}
