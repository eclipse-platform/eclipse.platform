package org.eclipse.ant.internal.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.*;

public class AntCorePreferences {

	protected Map defaultTasks;
	protected Map defaultObjects;
	protected Map defaultTypes;
	protected Map customTasks;
	protected List customURLs;

	protected static final String PREFERENCES_FILE_NAME = ".preferences";

public AntCorePreferences(Map defaultTasks, Map defaultObjects, Map defaultTypes) {
	this.defaultTasks = defaultTasks;
	this.defaultObjects = defaultObjects;
	this.defaultTypes = defaultTypes;
	restoreCustomObjects();
}

protected void restoreCustomObjects() {
	customURLs = new ArrayList(10);
	Properties urls = new Properties();
	try {
		IPath location = AntCorePlugin.getPlugin().getStateLocation().append(".urls");
		urls.load(new FileInputStream(location.toOSString()));
		for (Iterator iterator = urls.values().iterator(); iterator.hasNext();) {
			String url = (String) iterator.next();
			try {
				customURLs.add(new URL(url));
			} catch (MalformedURLException e) {
				e.printStackTrace(); // FIXME
			}
		}
	} catch (IOException e) {
		e.printStackTrace(); // FIXME
	}
}

/**
 * Param source is a Collection of IConfigurationElement.
 */
protected void addPluginClassLoaders(Collection source, List destination) {
	for (Iterator iterator = source.iterator(); iterator.hasNext();) {
		IConfigurationElement element = (IConfigurationElement) iterator.next();
		IPluginDescriptor descriptor = element.getDeclaringExtension().getDeclaringPluginDescriptor();
		ClassLoader loader = descriptor.getPluginClassLoader();
		if (!destination.contains(loader))
			destination.add(loader);
	}
}

/**
 * Param source is a Collection of IConfigurationElement.
 */
protected void addURLs(Collection source, List destination) {
	for (Iterator iterator = source.iterator(); iterator.hasNext();) {
		IConfigurationElement element = (IConfigurationElement) iterator.next();
		String library = element.getAttribute(AntCorePlugin.LIBRARY);
		if (library == null)
			continue; // FIXME: can it be null?
		IPluginDescriptor descriptor = element.getDeclaringExtension().getDeclaringPluginDescriptor();
		try {
			URL url = Platform.asLocalURL(new URL(descriptor.getInstallURL(), library));
			if (!destination.contains(url))
				destination.add(url);
		} catch (Exception e) {
			e.printStackTrace(); // FIXME
		}
	}
}

public URL[] getURLs() {
	List result = new ArrayList(10);
	if (customURLs != null);
		result.addAll(customURLs);
	// look for places that can provide more URLs
	Map[] places = new Map[] { defaultTasks, defaultObjects, defaultTypes };
	for (int i = 0; i < places.length; i++) {
		if (places[i] != null)
			addURLs(places[i].values(), result);
	}
	return (URL[]) result.toArray(new URL[result.size()]);

//	URL[] urls = null;
//	try {
//		urls = new URL[] {
//			new URL("file:c:/eclipse/workspaces/newant/org.apache.xerces/xerces.jar"),
//			new URL("file:c:/eclipse/workspaces/newant/org.apache.ant/ant.jar"),
//			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.ant.core.ant/bin/"),
//			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.ant.ui.ant/bin/"),
//			new URL("file:c:/eclipse/workspaces/newant/org.eclipse.core.resources.ant/bin/"),
//			new URL("file:c:/ibm-jdk/lib/tools.jar")
//		};
//	} catch (MalformedURLException e) {
//		e.printStackTrace();
//	}
//	return urls;
}

public ClassLoader[] getPluginClassLoaders() {
	List result = new ArrayList(10);
	// ant.core should always be present
	result.add(Platform.getPlugin(AntCorePlugin.PI_ANTCORE).getDescriptor().getPluginClassLoader());
	// look for places that can provide more class loaders
	Map[] places = new Map[] { defaultTasks, defaultObjects, defaultTypes };
	for (int i = 0; i < places.length; i++) {
		if (places[i] != null)
			addPluginClassLoaders(places[i].values(), result);
	}
	return (ClassLoader[]) result.toArray(new ClassLoader[result.size()]);
}


/**
 * Returns default + custom tasks.
 */
public Map getTasks() {
	Map result = new HashMap(10);
	if (defaultTasks != null)
		result.putAll(defaultTasks);
	if (customTasks != null)
		result.putAll(customTasks);
	return result;
}

public Map getTypes() {
	if (defaultTypes != null)
		return defaultTypes;
	return new HashMap(0);
}

}