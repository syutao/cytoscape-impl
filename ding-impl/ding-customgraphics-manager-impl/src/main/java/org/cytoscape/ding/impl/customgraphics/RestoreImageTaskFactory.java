package org.cytoscape.ding.impl.customgraphics;

import java.io.File;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class RestoreImageTaskFactory implements TaskFactory {
	
	private final File imageLocation;
	private final CustomGraphicsManagerImpl manager;
	
	RestoreImageTaskFactory(final File imageLocation, final CustomGraphicsManagerImpl manager) {
		this.manager = manager;
		this.imageLocation = imageLocation;
	}

	@Override
	public TaskIterator getTaskIterator() {
		
		return new TaskIterator(new RestoreImageTask(imageLocation, manager));
	}

}
