package org.eclipse.ant.internal.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.ant.core.AntCorePlugin;

public class AntCorePreferences {

	protected Map tasks;

public AntCorePreferences(Map tasks) {
	this.tasks = tasks;
}

public URL[] getURLs() {
	URL[] urls = null;
	try {
		urls = new URL[] {
			new URL("file:c:/eclipse/workspaces/newant/org.apache.xerces/xerces.jar"),
			new URL("file:c:/eclipse/workspaces/newant/org.apache.ant/ant.jar"),
			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.ant.core.ant/bin/"),
			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.ant.ui.ant/bin/"),
			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.core.resources.ant/bin/"),
			new URL("file:c:/ibm-jdk/lib/tools.jar")
		};
	} catch (MalformedURLException e) {
		e.printStackTrace();
	}
	return urls;
}

public Map getTasks() {
	return tasks;
}

}