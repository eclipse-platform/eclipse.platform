package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.Task;

/**
 * The plug-in runtime class for the Ant Core plug-in.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */

public final class AntPlugin extends Plugin {
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static AntPlugin plugin;

	/**
	 * Table of Ant tasks added through the antTasks extension point
	 */
	private Map taskExtensions;

	/**
	 * Table of Ant objects added through the antObjects extension point
	 */
	private Map objectExtensions;

	/**
	 * Table of Ant types added through the antTypes extension point
	 */
	private Map typeExtensions;
	
	/**
	 * The notification manager for registering build listeners.
	 */
	private AntNotificationManager notificationManager = new AntNotificationManager();

	/**
	 * Unique identifier constant (value <code>"org.eclipse.ant.core"</code>)
	 * for the Ant plug-in.
	 */
	public static final String PI_ANT = "org.eclipse.ant.core";

	/**
	 * Simple identifier constant (value <code>"antTasks"</code>)
	 * for the Ant tasks extension point.
	 */
	public static final String PT_ANTTASKS = "antTasks";

	/**
	 * Simple identifier constant (value <code>"antTypes"</code>)
	 * for the Ant types extension point.
	 */
	public static final String PT_ANTTYPES = "antTypes";

	/**
	 * Simple identifier constant (value <code>"antObjects"</code>)
	 * for the Ant objects extension point.
	 */
	public static final String PT_ANTOBJECTS = "antObjects";

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
 * Constructs an instance of this plug-in runtime class.
 * <p>
 * Instances of plug-in runtime classes are automatically created 
 * by the platform in the course of plug-in activation.
 * <b>No client or plug-in should ever explicitly instantiate
 * a plug-in runtime class</b>.
 * </p>
 * 
 * @param pluginDescriptor the plug-in descriptor for the
 *   Resources plug-in
 */
public AntPlugin(IPluginDescriptor pluginDescriptor) {
	super(pluginDescriptor);
	plugin = this;
}
/**
 * Gets the notificationManager.
 * @return Returns the AntNotificationManager
 */
public AntNotificationManager getNotificationManager() {
	return notificationManager;
}
/**
 * Returns the internal collection of object extensions.
 * 
 * @return the internal collection of object extensions
 */	
public Map getObjectExtensions() {
	return objectExtensions;
}

/**
 * Returns this plug-in.
 *
 * @return the single instance of this plug-in runtime class
 */
public static AntPlugin getPlugin() {
	return plugin;
}

/**
 * Returns the internal collection of task extensions.
 * 
 * @return the internal collection of task extensions
 */	
public Map getTaskExtensions() {
	return taskExtensions;
}

/**
 * Returns the internal collection of type extensions.
 * 
 * @return the internal collection of type extensions
 */	
public Map getTypeExtensions() {
	return typeExtensions;
}

/**
 * This implementation of the corresponding <code>Plugin</code> method
 * 
 * @exception CoreException if this method fails to shut down this plug-in
 */
public void shutdown() throws CoreException {
}

/**
 * This implementation of the corresponding <code>Plugin</code> method
 * 
 * @exception CoreException if this plug-in did not start up properly
 */
public void startup() throws CoreException {
	IExtensionPoint extensionPoint = getDescriptor().getExtensionPoint(PT_ANTTASKS);
	if (extensionPoint != null) {
		IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
		taskExtensions = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			String name = extensions[i].getAttribute(NAME);
			taskExtensions.put(name, extensions[i]);
		}
	}
	extensionPoint = getDescriptor().getExtensionPoint(PT_ANTTYPES);
	if (extensionPoint != null) {
		IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
		typeExtensions = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			String name = extensions[i].getAttribute(NAME);
			typeExtensions.put(name, extensions[i]);
		}
	}
	extensionPoint = getDescriptor().getExtensionPoint(PT_ANTOBJECTS);
	if (extensionPoint != null) {
		IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
		objectExtensions = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			String name = extensions[i].getAttribute(NAME);
			objectExtensions.put(name, extensions[i]);
		}
	}
}
}
