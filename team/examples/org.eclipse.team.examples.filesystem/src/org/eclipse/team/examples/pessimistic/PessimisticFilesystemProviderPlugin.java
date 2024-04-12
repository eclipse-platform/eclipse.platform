/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.pessimistic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The plugin for the <code>PessimisticFilesystemProvider</code>.
 */
public class PessimisticFilesystemProviderPlugin extends AbstractUIPlugin {
	/*
	 * Singleton instance.
	 */
	private static PessimisticFilesystemProviderPlugin instance;
	/*
	 * The resource change listener which notifies the provider of
	 * added and deleted files.
	 */
	private ResourceChangeListener fListener;
	/*
	 * The provider listeners
	 */
	private final List<IResourceStateListener> fListeners;

	/**
	 * The plugin identifier
	 */
	public static final String PLUGIN_ID = "org.eclipse.team.examples.pessimistic";
	/**
	 * The nature identifier.
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".pessimisticnature";

	/**
	 * Constructor required by plugin lifecycle.
	 */
	public PessimisticFilesystemProviderPlugin() {
		super();
		instance = this;
		fListeners = new ArrayList<>(1);
		//setDebugging(true);
	}

	/**
	 * Answers the singleton instance of this plugin.
	 */
	public static PessimisticFilesystemProviderPlugin getInstance() {
		return instance;
	}

	/**
	 * Initializes the default preferences for this plugin.
	 */
	protected void initializeDefaultPreferences() {
		IPreferenceStore store = getPreferenceStore();

		store.setDefault(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED,
				IPessimisticFilesystemConstants.OPTION_PROMPT);
		store.setDefault(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED_NOPROMPT,
				IPessimisticFilesystemConstants.OPTION_AUTOMATIC);
		store.setDefault(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED,
				IPessimisticFilesystemConstants.OPTION_DO_NOTHING);
		store.setDefault(
				IPessimisticFilesystemConstants.PREF_ADD_TO_CONTROL,
				IPessimisticFilesystemConstants.OPTION_PROMPT);
		store.setDefault(IPessimisticFilesystemConstants.PREF_FAIL_VALIDATE_EDIT, false);
		store.setDefault(IPessimisticFilesystemConstants.PREF_TOUCH_DURING_VALIDATE_EDIT, true);
	}

	/**
	 * Convenience method for logging errors.
	 */
	public void logError(Throwable exception, String message) {
		String pluginId= getBundle().getSymbolicName();
		Status status= new Status(IStatus.ERROR, pluginId, IStatus.OK, message, exception);
		getLog().log(status);
		if (isDebugging()) {
			System.out.println(message);
			exception.printStackTrace();
		}
	}

	/**
	 * Starts the resource listener.
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		fListener= new ResourceChangeListener();
		fListener.startup();
		initializeDefaultPreferences();
		super.start(context);
	}

	/**
	 * Stops the resource listener.
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		fListener.shutdown();
		fListener= null;
		super.stop(context);
	}

	/**
	 * Notifies the registered <code>IResourceStateListener</code> objects
	 * that the repository state for the resources has changed.
	 *
	 * @param resources	Collection of resources that have changed.
	 */
	public void fireResourcesChanged(IResource[] resources) {
		if (resources == null || resources.length == 0 || fListeners.isEmpty())
			return;
		for (IResourceStateListener listener : fListeners) {
			listener.stateChanged(resources);
		}
	}

	/**
	 * Adds the listener to the list of listeners that are notified when
	 * the repository state of resources change.
	 */
	public void addProviderListener(IResourceStateListener listener) {
		if (fListeners.contains(listener))
			return;
		fListeners.add(listener);
	}


	/**
	 * Removes the listener from the list of listeners that are notified when
	 * the repository state of resources change.
	 */
	public void removeProviderListener(IResourceStateListener listener) {
		fListeners.remove(listener);
	}
}
