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
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Custom install handler.
 * Custom install handlers can optionally be associated with a feature.
 * The actual install handler implementation can be physically delivered
 * as part of the feature package, or can already be installed on the client
 * machine and registered via the <code>org.eclipse.update.core.installHandlers</code>
 * extension point. The install handler methods are called at predetermined
 * point during update actions.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly subclass the provided implementation of this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.BaseInstallHandler
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IInstallHandler {

	/**
	 * Indicates the handler is being initialized for feature install.
	 * @since 2.0
	 */
	public static final int HANDLER_ACTION_INSTALL = 1;

	/**
	 * Indicates the handler is being initialized for feature configure.
	 * @since 2.0
	 */
	public static final int HANDLER_ACTION_CONFIGURE = 2;

	/**
	 * Indicates the handler is being initialized for feature unconfigure.
	 * @since 2.0
	 */
	public static final int HANDLER_ACTION_UNCONFIGURE = 3;

	/**
	 * Indicates the handler is being initialized for feature uninstall.
	 * @since 2.0
	 */
	public static final int HANDLER_ACTION_UNINSTALL = 4;

	/**
	 * Initialize the install handler.
	 * Install handlers are always constructed using the default constructor.
	 * The are initialized immediately following construction.
	 * 
	 * @param type update action type
	 * @param feature the target of the action
	 * @param entry model entry that defines this handler
	 * @param monitor optional progress monitor, can be <code>null</code>
	 * @exception CoreException
	 * @since 2.0
	 */
	public void initialize(
		int type,
		IFeature feature,
		IInstallHandlerEntry entry,
		InstallMonitor monitor)
		throws CoreException;

	/**
	 * Called at the start of the install action. At this point, no install
	 * processing has taken place.
	 * 
	 * @see #HANDLER_ACTION_INSTALL
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void installInitiated() throws CoreException;

	/**
	 * Called after files corresponding to plug-in entries have been downloaded,
	 * but before they are actully unpacked and installed.
	 * 
	 * @see #HANDLER_ACTION_INSTALL
	 * @param plugins downloaded plug-in entries. Note this may be a subset
	 * of the plug-ins actually references by the feature.
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void pluginsDownloaded(IPluginEntry[] plugins) throws CoreException;

	/**
	 * Called after files corresponding to non-plug-in entries have been 
	 * downloaded. The custom install handler can perform any custom
	 * verification of the non-plug-in entries (these are not interpreted
	 * in any way by the platform (beyond downloading)).
	 * 
	 * @see #HANDLER_ACTION_INSTALL
	 * @param nonPluginData downloaded non-plug-in entries.
	 * @param listener verification listener, may be <code>null</code>.
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void nonPluginDataDownloaded(
		INonPluginEntry[] nonPluginData,
		IVerificationListener listener)
		throws CoreException;

	/**
	 * Called after the feature files and any downloaded plug-ins have
	 * been installed. Typically this is the point where the custom
	 * install handler can install any non-plug-in entries (these are not 
	 * interpreted in any way by the platform (beyond downloading)).
	 * 
	 * @see #HANDLER_ACTION_INSTALL
	 * @param consumer content consumer for the feature. The install handler
	 * can choose to use this consumer to install the non-plug-in data,
	 * or can handle the data in any other way. If using the consumer,
	 * the install handler should only call 
	 * @see IFeatureContentConsumer#store(ContentReference, IProgressMonitor)
	 * and @see IFeatureContentConsumer#open(INonPluginEntry)
	 * methods of the consumer. 
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void completeInstall(IFeatureContentConsumer consumer)
		throws CoreException;

	/**
	 * Called at the end of the install action.
	 * 
	 * @see #HANDLER_ACTION_INSTALL
	 * @param success indicates action success. 
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void installCompleted(boolean success) throws CoreException;

	/**
	 * Called at the start of the configure action
	 * 
	 * @see #HANDLER_ACTION_CONFIGURE
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void configureInitiated() throws CoreException;

	/**
	 * Called after the feature has been configured. The install handler
	 * should perform any completion tasks. No arguments are passed
	 * to the method. If needed, the install handler can use arguments
	 * passed on the initialization call.
	 * 
	 * @see #HANDLER_ACTION_CONFIGURE
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void completeConfigure() throws CoreException;

	/**
	 * Called at the end of the configure action.
	 * 
	 * @see #HANDLER_ACTION_CONFIGURE
	 * @param success indicates action success. 
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void configureCompleted(boolean success) throws CoreException;

	/**
	 * Called at the start of the unconfigure action
	 * 
	 * @see #HANDLER_ACTION_UNCONFIGURE
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void unconfigureInitiated() throws CoreException;

	/**
	 * Called after the feature has been unconfigured. The install handler
	 * should perform any completion tasks. No arguments are passed
	 * to the method. If needed, the install handler can use arguments
	 * passed on the initialization call.
	 * 
	 * @see #HANDLER_ACTION_UNCONFIGURE
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void completeUnconfigure() throws CoreException;

	/**
	 * Called at the end of the unconfigure action.
	 * 
	 * @see #HANDLER_ACTION_UNCONFIGURE
	 * @param success indicates action success. 
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void unconfigureCompleted(boolean success) throws CoreException;

	/**
	 * Called at the start of the uninstall action
	 * 
	 * @see #HANDLER_ACTION_UNINSTALL
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void uninstallInitiated() throws CoreException;

	/**
	 * Called after the feature has been uninstalled. The install handler
	 * should perform any completion tasks. No arguments are passed
	 * to the method. If needed, the install handler can use arguments
	 * passed on the initialization call. Note, that at this point
	 * the feature files and any unreferenced plug-ins have been
	 * removed.
	 * 
	 * @see #HANDLER_ACTION_UNINSTALL
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void completeUninstall() throws CoreException;

	/**
	 * Called at the end of the uninstall action.
	 * 
	 * @see #HANDLER_ACTION_UNINSTALL
	 * @param success indicates action success. 
	 * @exception CoreException terminates the action
	 * @since 2.0
	 */
	public void uninstallCompleted(boolean success) throws CoreException;
}
