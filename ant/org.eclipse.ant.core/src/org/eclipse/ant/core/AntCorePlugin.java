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
	 * Table of Ant objects (IConfigurationElement) added through the objects extension point
	 */
	private Map objectExtensions;

	/**
	 * Table of Ant ypes (IConfigurationElement) added through the types extension point
	 */
	private Map typeExtensions;

	/**
	 * 
	 */
	private AntCorePreferences preferences;

	/**
	 * Unique identifier constant (value <code>"org.eclipse.ant.core"</code>)
	 * for the Ant Core plug-in.
	 */
	public static final String PI_ANTCORE= "org.eclipse.ant.core";

	/**
	 * Simple identifier constant (value <code>"tasks"</code>)
	 * for the Ant tasks extension point.
	 */
	public static final String PT_TASKS = "tasks";

	/**
	 * Simple identifier constant (value <code>"objects"</code>)
	 * for the Ant tasks extension point.
	 */
	public static final String PT_OBJECTS = "objects";

	/**
	 * Simple identifier constant (value <code>"types"</code>)
	 * for the Ant tasks extension point.
	 */
	public static final String PT_TYPES = "types";

	/**
	 * Simple identifier constant (value <code>"class"</code>)
	 * of a tag that appears in Ant extensions.
	 */
	public static final String CLASS = "class";

	/**
	 * Simple identifier constant (value <code>"name"</code>)
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
	taskExtensions = extractExtensions(PT_TASKS);
	objectExtensions = extractExtensions(PT_OBJECTS);
	typeExtensions = extractExtensions(PT_TYPES);
}

protected Map extractExtensions(String point) {
	IExtensionPoint extensionPoint = getDescriptor().getExtensionPoint(point);
	if (extensionPoint == null)
		return null;
	IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
	Map result = new HashMap(extensions.length);
	for (int i = 0; i < extensions.length; i++) {
		String name = extensions[i].getAttribute(NAME);
		result.put(name, extensions[i]);
	}
	return result;
}

public AntCorePreferences getPreferences() {
	if (preferences == null)
		preferences = new AntCorePreferences(taskExtensions, objectExtensions, typeExtensions);
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