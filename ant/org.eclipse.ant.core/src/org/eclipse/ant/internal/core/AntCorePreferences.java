package org.eclipse.ant.internal.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class AntCorePreferences {

	protected Map defaultTasks;
	protected Map defaultObjects;
	protected Map defaultTypes;
	protected Map tasks;
	protected List plugins;

public AntCorePreferences(Map defaultTasks, Map defaultObjects, Map defaultTypes) {
	this.defaultTasks = defaultTasks;
	this.defaultObjects = defaultObjects;
	this.defaultTypes = defaultTypes;
	tasks = new HashMap(20);
	plugins = new ArrayList(10);
	if (defaultTasks != null) {
		for (Iterator iterator = defaultTasks.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String taskName = (String) entry.getKey();
			IConfigurationElement element = (IConfigurationElement) entry.getValue();
			String className = element.getAttribute(AntCorePlugin.CLASS);
			tasks.put(taskName, className);
			plugins.add(element.getDeclaringExtension().getDeclaringPluginDescriptor());
		}
	}
	if (defaultObjects != null) {
		for (Iterator iterator = defaultObjects.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			IConfigurationElement element = (IConfigurationElement) entry.getValue();
			plugins.add(element.getDeclaringExtension().getDeclaringPluginDescriptor());
		}
	}
	if (defaultTypes != null) {
		for (Iterator iterator = defaultTypes.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			IConfigurationElement element = (IConfigurationElement) entry.getValue();
			plugins.add(element.getDeclaringExtension().getDeclaringPluginDescriptor());
		}
	}
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

public ClassLoader[] getPluginClassLoaders() {
	List result = new ArrayList(10);
	result.add(Platform.getPlugin("org.eclipse.ant.core").getDescriptor().getPluginClassLoader());
//	// FIXME: should not add ui by default
//	result.add(Platform.getPlugin("org.eclipse.ant.ui").getDescriptor().getPluginClassLoader());
	for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
		IPluginDescriptor descriptor = (IPluginDescriptor) iterator.next();
		result.add(descriptor.getPluginClassLoader());
	}
	return (ClassLoader[]) result.toArray(new ClassLoader[result.size()]);
}

public Map getTasks() {
	return tasks;
}

}