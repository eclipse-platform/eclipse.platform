package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;import org.eclipse.core.runtime.*;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the AntUI plug-in.
 */
public final class AntUIPlugin extends AbstractUIPlugin {

	/**
	 * Unique identifier constant (value <code>"org.eclipse.ant.ui"</code>)
	 * for the Ant UI plug-in.
	 */
	public static final String PI_ANTUI= "org.eclipse.ant.ui";
	
	public static final String PROPERTIES_MESSAGES = "org.eclipse.ant.internal.ui.messages";
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static AntUIPlugin plugin;

public AntUIPlugin(IPluginDescriptor desc) {
	super(desc);
	plugin= this;
}
public static AntUIPlugin getPlugin() {
	return plugin;
}
public static ResourceBundle getResourceBundle() {
	try {
		return ResourceBundle.getBundle(PROPERTIES_MESSAGES);
	} catch (MissingResourceException e) {
		ErrorDialog.openError(
			plugin.getWorkbench().getActiveWorkbenchWindow().getShell(),
			Policy.bind("exception.missingResourceBundle"),
			Policy.bind("exception.missingResourceBundle.message"),
			new Status(IStatus.ERROR,PI_ANTUI,0,Policy.bind("exception.missingResourceBundle.message"),e));
		return null;
	}
}

}
