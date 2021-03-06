package org.cytoscape.app.internal.task;

import java.io.File;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.net.DownloadStatus;
import org.cytoscape.app.internal.net.WebApp;
import org.cytoscape.app.internal.net.WebQuerier;
import org.cytoscape.app.internal.util.DebugHelper;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallAppFromAppStoreTask extends AbstractTask {
	
	private static final Logger logger = LoggerFactory.getLogger(InstallAppFromAppStoreTask.class);
	
	private final WebApp webApp;
	private final WebQuerier webQuerier;
	private final AppManager appManager;
	
	public InstallAppFromAppStoreTask(final WebApp webApp, final WebQuerier webQuerier, final AppManager appManager){
		this.webApp = webApp;
		this.webQuerier = webQuerier;
		this.appManager = appManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		DownloadStatus status = new DownloadStatus(taskMonitor);
		taskMonitor.setTitle("Installing App from App Store");
		taskMonitor.setStatusMessage("Installing app: " + webApp.getFullName());
		
		// Download app
		File appFile = webQuerier.downloadApp(webApp, null, new File(appManager.getDownloadedAppsPath()), status);
		if(appFile != null) {
			insertTasksAfterCurrentTask(new InstallAppFromFileTask(appFile, appManager));
		}
		else {
			// Log error: no download links were found for app
			logger.warn("Unable to find download URL for about-to-be-installed " + webApp.getFullName());
			DebugHelper.print("Unable to find download url for: " + webApp.getFullName());
		}
	}

}
