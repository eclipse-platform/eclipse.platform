package org.eclipse.ant.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.core.AntCorePreferences;
import org.eclipse.core.runtime.*;

public class AntCorePlugin extends Plugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static AntCorePlugin plugin;

	/**
	 * Table of Ant tasks (IConfigurationElement) added through the tasks extension point
	 */
	private Map taskExtensions;

	/**
	 * 
	 */
	private AntCorePreferences preferences;

	/**
	 * Simple identifier constant (value <code>"tasks"</code>)
	 * for the Ant tasks extension point.
	 */
	public static final String PT_TASKS = "tasks";

	/**
	 * Simple identifier constant (value <code>"class"</code>)
	 * of a tag that appears in Ant extensions.
	 */
	public static final String CLASS = "class";

	/**
	 * Simple identifier constant (value <code>"class"</code>)
	 * of a tag that appears in Ant extensions.
	 */
	public static final String NAME = "name";

	/**
	 * Simple identifier constant (value <code>"library"</code>)
	 * of a tag that appears in Ant extensions.
	 */
	public static final String LIBRARY = "library";

public AntCorePlugin(IPluginDescriptor descriptor) {
	super(descriptor);
	plugin = this;
}

public void startup() throws CoreException {
	IExtensionPoint extensionPoint = getDescriptor().getExtensionPoint(PT_TASKS);
	if (extensionPoint != null) {
		IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
		taskExtensions = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			String name = extensions[i].getAttribute(NAME);
			taskExtensions.put(name, extensions[i]);
		}
	}
}

public AntCorePreferences getPreferences() {
	if (preferences == null) {
		preferences = new AntCorePreferences(taskExtensions);
	}
	return preferences;
}

/**
 * Returns this plug-in.
 *
 * @return the single instance of this plug-in runtime class
 */
public static AntCorePlugin getPlugin() {
	return plugin;
}
}