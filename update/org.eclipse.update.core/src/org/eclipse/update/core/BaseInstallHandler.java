/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import org.eclipse.core.runtime.CoreException;

/**
 * Base implementation of an install handler.
 * This is a convenience implementation of an install handler with
 * null implementation of its methods. It allows subclasses to selectively
 * implement only the methods required for their installation tasks.
 * <p>
 * This class should be subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p> 
 * @see org.eclipse.update.core.IInstallHandler
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class BaseInstallHandler implements IInstallHandler {

	/**
	 * Update action type
	 * 
	 * @see IInstallHandler#HANDLER_ACTION_INSTALL
	 * @see IInstallHandler#HANDLER_ACTION_CONFIGURE
	 * @see IInstallHandler#HANDLER_ACTION_UNCONFIGURE
	 * @see IInstallHandler#HANDLER_ACTION_UNINSTALL
	 * @since 2.0
	 */
	protected int type;

	/**
	 * The target of the action
	 * @since 2.0
	 */
	protected IFeature feature;

	/**
	 * Model entry that defines this handler
	 * 
	 * @since 2.0
	 */
	protected IInstallHandlerEntry entry;

	/** 
	 * Optional progress monitor, can be <code>null</code>
	 * 
	 * @since 2.0
	 */
	protected InstallMonitor monitor;

	/**
	 * Plug-in entries downloaded
	 * 
	 * @see IInstallHandler#HANDLER_ACTION_INSTALL
	 * @since 2.0
	 */
	protected IPluginEntry[] pluginEntries;

	/**
	 * Non-plug-in entries downloaded
	 * 
	 * @see IInstallHandler#HANDLER_ACTION_INSTALL
	 * @since 2.0
	 */
	protected INonPluginEntry[] nonPluginEntries;

	/**
	 * Indicates if handler has been initialized
	 * 
	 * @since 2.0
	 */
	protected boolean initialized = false;

	/**
	 * Initialize the install handler.
	 * 
	 * @see IInstallHandler#initialize(int, IFeature, IInstallHandlerEntry, InstallMonitor)
	 * @since 2.0
	 */
	public void initialize(
		int type,
		IFeature feature,
		IInstallHandlerEntry entry,
		InstallMonitor monitor)
		throws CoreException {

		if (this.initialized)
			return;
		else {
			if (feature == null)
				throw new IllegalArgumentException();
			this.type = type;
			this.feature = feature;
			this.entry = entry;
			this.monitor = monitor;
			this.initialized = true;
		}
	}

	/**
	 * Called at the start of the install action.
	 * 
	 * @see IInstallHandler#installInitiated
	 * @since 2.0
	 */
	public void installInitiated() throws CoreException {
	}

	/**
	 * Called after files corresponding to plug-in entries have been downloaded,
	 * but before they are actually unpacked and installed.
	 * 
	 * @see IInstallHandler#pluginsDownloaded(IPluginEntry[])
	 * @since 2.0
	 */
	public void pluginsDownloaded(IPluginEntry[] plugins) throws CoreException {

		this.pluginEntries = plugins;
	}

	/**
	 * Called after files corresponding to non-plug-in entries have been 
	 * downloaded.
	 * 
	 * @see IInstallHandler#nonPluginDataDownloaded(INonPluginEntry[], IVerificationListener)
	 * @since 2.0
	 */
	public void nonPluginDataDownloaded(
		INonPluginEntry[] nonPluginData,
		IVerificationListener listener)
		throws CoreException {

		this.nonPluginEntries = nonPluginData;
	}

	/**
	 * Called after the feature files and any downloaded plug-ins have
	 * been installed. 
	 * 
	 * @see IInstallHandler#completeInstall(IFeatureContentConsumer)
	 * @since 2.0
	 */
	public void completeInstall(IFeatureContentConsumer consumer)
		throws CoreException {
	}

	/**
	 * Called at the end of the install action.
	 * 
	 * @see IInstallHandler#installCompleted(boolean)
	 * @since 2.0
	 */
	public void installCompleted(boolean success) throws CoreException {
	}

	/**
	 * Called at the start of the configure action.
	 * 
	 * @see IInstallHandler#configureInitiated()
	 * @since 2.0
	 */
	public void configureInitiated() throws CoreException {
	}

	/**
	 * Called after the feature has been configured.
	 * 
	 * @see IInstallHandler#completeConfigure()
	 * @since 2.0
	 */
	public void completeConfigure() throws CoreException {
	}

	/**
	 * Called at the end of the configure action.
	 * 
	 * @see IInstallHandler#configureCompleted(boolean)
	 * @since 2.0
	 */
	public void configureCompleted(boolean success) throws CoreException {
	}

	/**
	 * Called at the start of the unconfigure action.
	 * 
	 * @see IInstallHandler#unconfigureInitiated()
	 * @since 2.0
	 */
	public void unconfigureInitiated() throws CoreException {
	}

	/**
	 * Called after the feature has been unconfigured.
	 * 
	 * @see IInstallHandler#completeUnconfigure()
	 * @since 2.0
	 */
	public void completeUnconfigure() throws CoreException {
	}

	/**
	 * Called at the end of the unconfigure action.
	 * 
	 * @see IInstallHandler#unconfigureCompleted(boolean)
	 * @since 2.0
	 */
	public void unconfigureCompleted(boolean success) throws CoreException {
	}

	/**
	 * Called at the start of the uninstall action.
	 * 
	 * @see IInstallHandler#uninstallInitiated()
	 * @since 2.0
	 */
	public void uninstallInitiated() throws CoreException {
	}

	/**
	 * Called after the feature has been uninstalled.
	 * 
	 * @see IInstallHandler#completeUninstall()
	 * @since 2.0
	 */
	public void completeUninstall() throws CoreException {
	}

	/**
	 * Called at the end of the uninstall action.
	 * 
	 * @see IInstallHandler#uninstallCompleted(boolean)
	 * @since 2.0
	 */
	public void uninstallCompleted(boolean success) throws CoreException {
	}
}
