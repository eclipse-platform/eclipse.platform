package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the AntUI plug-in.
 */
public final class AntUIPlugin extends AbstractUIPlugin {

	/**
	 * 
	 */
	protected IProgressMonitor currentProgressMonitor;

	/**
	 * 
	 */
	protected boolean shouldLogMessages;

	/**
	 * Unique identifier constant (value <code>"org.eclipse.ant.ui"</code>)
	 * for the Ant UI plug-in.
	 */
	public static final String PI_ANTUI= "org.eclipse.ant.ui";
	
	public static final String PROPERTIES_MESSAGES = "org.eclipse.ant.internal.ui.messages";
	
	public static final String IMG_ANT_SCRIPT= "icons/full/eview16/ant_view.gif";
	public static final String IMG_BUILDER= "icons/full/eview16/build_exec.gif";
	
	private static final String SETTINGS_COMMAND_HISTORY = "CommandHistory";
	private static final int MAX_COMMAND_HISTORY = 15;
	
	
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static AntUIPlugin plugin;
	
	private final static int MAX_HISTORY_SIZE= 5;
	/**
	 * The most recent debug launches
	 */
	private IFile[] antHistory = new IFile[MAX_HISTORY_SIZE];



public IProgressMonitor getCurrentProgressMonitor() {
	return currentProgressMonitor;
}

public void setCurrentProgressMonitor(IProgressMonitor monitor) {
	this.currentProgressMonitor = monitor;
}

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

/**
 * Adds the given command to the command history.
 * - if the command doesn't exist: put it at the first place
 * - if it exists and is already at the first place: does nothing
 * - if it exists and is not at the first place: move it to the first place

 */
public void addToCommandHistory(String command) {
	IDialogSettings settings = getDialogSettings();
	String[] oldHistory = settings.getArray(SETTINGS_COMMAND_HISTORY);
	if (oldHistory == null || oldHistory.length == 0) {
		settings.put(SETTINGS_COMMAND_HISTORY, new String[] {command});
		return;
	}
	if (command.equals(oldHistory[0])) {
		return;
	}
	//search for existing command and move to top if found
	int index;
	for (index=1; index<oldHistory.length; index++)
		if (command.equals(oldHistory[index]))
			break;
	if (index < oldHistory.length) {
		//it was found, so replace it
		for (int i=index; i>0; i--)
			oldHistory[i] = oldHistory[i-1];
		oldHistory[0] = command;
		settings.put(SETTINGS_COMMAND_HISTORY, oldHistory);
	}
	//not found -- add it to the history
	String[] newHistory;
	if (oldHistory.length == MAX_COMMAND_HISTORY) {
		//shift other commands down and add new one
		System.arraycopy(oldHistory, 0, oldHistory, 1, oldHistory.length-1);
		oldHistory[0] = command;
		newHistory = oldHistory;
	} else {
		// grow the history array
		newHistory = new String[oldHistory.length+1];
		System.arraycopy(oldHistory, 0, newHistory, 1, oldHistory.length);
		newHistory[0] = command;
	}
	settings.put(SETTINGS_COMMAND_HISTORY, newHistory);
}
/**
 * Adds the given file to the history: 
 * - if the file doesn't exist: put it at the first place
 * - if it exists and is already at the first place: does nothing
 * - if it exists and is not at the first place: move it to the first place
 * 
 * @param the file
 */
public void addToHistory(IFile file) {
	if (file.equals(antHistory[0]))
		return;
		
	int index;
	for (index=1; index<MAX_HISTORY_SIZE; index++)
		if (file.equals(antHistory[index]))
			break;
	if (index == MAX_HISTORY_SIZE)
		index--;
	for (int i=index; i>0; i--)
		antHistory[i] = antHistory[i-1];
	antHistory[0] = file;	
}

/**
 * Returns the history of tool script command that have been run.
 */
public String[] getCommandHistory() {
	return getDialogSettings().getArray(SETTINGS_COMMAND_HISTORY);
}
/**
 * Returns the history of the AntPlugin, i.e. the files that were executed previously
 * 
 * @return an array containing the files
 */
public IFile[] getHistory() {
	return antHistory;	
}

/**
 * Returns the ImageDescriptor for the icon with the given path
 * 
 * @return the ImageDescriptor object
 */
public ImageDescriptor getImageDescriptor(String path) {
	try {
		URL installURL = getDescriptor().getInstallURL();
		URL url = new URL(installURL,path);
		return ImageDescriptor.createFromURL(url);
	} catch (MalformedURLException e) {
		return null;
	}
}

/**
 * @see AbstractUIPlugin#initializeDefaultPreferences
 */
protected void initializeDefaultPreferences(IPreferenceStore prefs) {
	prefs.setDefault(IAntPreferenceConstants.AUTO_SAVE, false);
	prefs.setDefault(IAntPreferenceConstants.INFO_LEVEL, true);
	prefs.setDefault(IAntPreferenceConstants.VERBOSE_LEVEL, false);
	prefs.setDefault(IAntPreferenceConstants.DEBUG_LEVEL, false);

	PreferenceConverter.setDefault(prefs, IAntPreferenceConstants.CONSOLE_ERROR_RGB, new RGB(255, 0, 0)); 		// red - exactly the same as debug Consol
	PreferenceConverter.setDefault(prefs, IAntPreferenceConstants.CONSOLE_WARNING_RGB, new RGB(255, 100, 0)); 	// orange
	PreferenceConverter.setDefault(prefs, IAntPreferenceConstants.CONSOLE_INFO_RGB, new RGB(0, 0, 255)); 		// blue
	PreferenceConverter.setDefault(prefs, IAntPreferenceConstants.CONSOLE_VERBOSE_RGB, new RGB(0, 200, 125));	// green
	PreferenceConverter.setDefault(prefs, IAntPreferenceConstants.CONSOLE_DEBUG_RGB, new RGB(0, 0, 0));			// black
	
}

public boolean getShouldLogMessages() {
	return shouldLogMessages;
}

public void setShouldLogMessages(boolean shouldLogMessages) {
	this.shouldLogMessages = shouldLogMessages;
}

}