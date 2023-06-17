/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ui.history.IFileHistoryConstants;
import org.eclipse.team.internal.ui.mapping.StreamMergerDelegate;
import org.eclipse.team.internal.ui.mapping.WorkspaceTeamStateProvider;
import org.eclipse.team.internal.ui.synchronize.SynchronizeManager;
import org.eclipse.team.internal.ui.synchronize.TeamSynchronizingPerspective;
import org.eclipse.team.internal.ui.synchronize.actions.GlobalRefreshAction;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.mapping.ITeamStateProvider;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.SubscriberTeamStateProvider;
import org.eclipse.team.ui.synchronize.TeamStateProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * TeamUIPlugin is the plugin for generic, non-provider specific,
 * team UI functionality in the workbench.
 */
public class TeamUIPlugin extends AbstractUIPlugin {

	private static TeamUIPlugin instance;

	// image paths
	public static final String ICON_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	public static final String ID = "org.eclipse.team.ui"; //$NON-NLS-1$

	// plugin id
	public static final String PLUGIN_ID = "org.eclipse.team.ui"; //$NON-NLS-1$

	public static final String TRIGGER_POINT_ID = "org.eclipse.team.ui.activityTriggerPoint"; //$NON-NLS-1$

	private static List<IPropertyChangeListener> propertyChangeListeners = new ArrayList<>(5);

	private Hashtable<String, ImageDescriptor> imageDescriptors = new Hashtable<>(20);

	private WorkspaceTeamStateProvider provider;

	private Map<String, TeamStateProvider> decoratedStateProviders = new HashMap<>();

	// manages synchronize participants
	private SynchronizeManager synchronizeManager;

	private ServiceRegistration<DebugOptionsListener> debugRegistration;

	/**
	 * ID of the 'Remove from View' action.
	 * Value: <code>"org.eclipse.team.internal.ui.RemoveFromView"</code>
	 */
	public static final String REMOVE_FROM_VIEW_ACTION_ID = "org.eclipse.team.internal.ui.RemoveFromView"; //$NON-NLS-1$


	/**
	 * Creates a new TeamUIPlugin.
	 */
	public TeamUIPlugin() {
		super();
		instance = this;
	}

	/**
	 * Creates an extension.  If the extension plugin has not
	 * been loaded a busy cursor will be activated during the duration of
	 * the load.
	 *
	 * @param element the config element defining the extension
	 * @param classAttribute the name of the attribute carrying the class
	 * @return the extension object
	 * @throws CoreException
	 */
	public static Object createExtension(final IConfigurationElement element, final String classAttribute) throws CoreException {
		// If plugin has been loaded create extension.
		// Otherwise, show busy cursor then create extension.
		Bundle bundle = Platform.getBundle(element.getNamespaceIdentifier());
		if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE) {
			return element.createExecutableExtension(classAttribute);
		} else {
			final Object [] ret = new Object[1];
			final CoreException [] exc = new CoreException[1];
			BusyIndicator.showWhile(null, () -> {
				try {
					ret[0] = element.createExecutableExtension(classAttribute);
				} catch (CoreException e) {
					exc[0] = e;
				}
			});
			if (exc[0] != null)
				throw exc[0];
			else
				return ret[0];
		}
	}

	/**
	 * Convenience method to get the currently active workbench page. Note that
	 * the active page may not be the one that the usr perceives as active in
	 * some situations so this method of obtaining the activae page should only
	 * be used if no other method is available.
	 *
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) return null;
		return window.getActivePage();
	}

	/**
	 * Return the default instance of the receiver. This represents the runtime plugin.
	 *
	 * @return the singleton plugin instance
	 */
	public static TeamUIPlugin getPlugin() {
		return instance;
	}
	/**
	 * Initializes the preferences for this plugin if necessary.
	 */
	@Override
	protected void initializeDefaultPluginPreferences() {
		IPreferenceStore store = getPreferenceStore();
		store.setDefault(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL, false);
		store.setDefault(IPreferenceIds.SHOW_AUTHOR_IN_COMPARE_EDITOR, false);
		store.setDefault(IPreferenceIds.MAKE_FILE_WRITTABLE_IF_CONTEXT_MISSING, false);
		store.setDefault(IPreferenceIds.REUSE_OPEN_COMPARE_EDITOR, true);
		store.setDefault(IPreferenceIds.RUN_IMPORT_IN_BACKGROUND, false);
		store.setDefault(IPreferenceIds.APPLY_PATCH_IN_SYNCHRONIZE_VIEW, false);
		store.setDefault(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS, true);
		store.setDefault(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT, IPreferenceIds.COMPRESSED_LAYOUT);
		store.setDefault(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE, TeamSynchronizingPerspective.ID);
		store.setDefault(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT, GlobalRefreshAction.NO_DEFAULT_PARTICPANT);
		store.setDefault(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID, GlobalRefreshAction.NO_DEFAULT_PARTICPANT);
		store.setDefault(IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE, MessageDialogWithToggle.PROMPT);
		store.setDefault(IPreferenceIds.SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT, false);
		store.setDefault(IFileHistoryConstants.PREF_GENERIC_HISTORYVIEW_EDITOR_LINKING, true);

		// Convert the old compressed folder preference to the new layout preference
		if (!store.isDefault(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS) && !store.getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			// Set the compress folder preference to the default true) \
			// so will will ignore it in the future
			store.setToDefault(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS);
			// Set the layout to tree (which was used when compress folder was false)
			store.setDefault(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT, IPreferenceIds.TREE_LAYOUT);
		}
	}

	/**
	 * Convenience method for logging statuses to the plugin log
	 *
	 * @param status  the status to log
	 */
	public static void log(IStatus status) {
		getPlugin().getLog().log(status);
	}

	/**
	 * Convenience method for logging a TeamException in such a way that the
	 * stacktrace is logged as well.
	 * @param e
	 */
	public static void log(CoreException e) {
		IStatus status = e.getStatus();
		log (status.getSeverity(), status.getMessage(), e);
	}

	/**
	 * Log the given exception along with the provided message and severity
	 * indicator
	 *
	 * @param severity
	 *            the severity
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param e
	 *            a low-level exception, or <code>null</code> if not applicable
	 */
	public static void log(int severity, String message, Throwable e) {
		log(new Status(severity, ID, 0, message, e));
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// register debug options listener
		Hashtable<String, String> properties = new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER, properties);

		initializeImages(this);
		StreamMergerDelegate.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			// unregister debug options listener
			debugRegistration.unregister();
			debugRegistration = null;

			if (synchronizeManager != null)
				synchronizeManager.dispose();
		} finally {
			super.stop(context);
		}
		if (provider != null) {
			provider.dispose();
		}
		for (TeamStateProvider teamStateProvider : decoratedStateProviders.values()) {
			SubscriberTeamStateProvider sdsp = (SubscriberTeamStateProvider) teamStateProvider;
			sdsp.dispose();
		}
	}

	/**
	 * Register for changes made to Team properties.
	 * @param listener the listener to register
	 */
	public static void addPropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * Deregister as a Team property changes.
	 * @param listener the listener to remove
	 */
	public static void removePropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}

	/**
	 * Broadcast a Team property change.
	 * @param event the property change event object
	 */
	public static void broadcastPropertyChange(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : propertyChangeListeners) {
			listener.propertyChange(event);
		}
	}

	/**
	 * Creates an image and places it in the image registry.
	 *
	 * @param id  the identifier for the image
	 * @param baseURL  the base URL for the image
	 */
	private static void createImageDescriptor(TeamUIPlugin plugin, String id) {
		// Delegate to the plugin instance to avoid concurrent class loading problems
		plugin.privateCreateImageDescriptor(id);
	}
	private void privateCreateImageDescriptor(String id) {
		ImageDescriptor desc = ImageDescriptor.createFromURL(getImageUrl(id));
		imageDescriptors.put(id, desc);
	}
	private void privateCreateImageDescriptor(String id, String imageUrl) {
		ImageDescriptor desc = ImageDescriptor.createFromURL(getImageUrl(imageUrl));
		imageDescriptors.put(id, desc);
	}

	/**
	 * Returns the image descriptor for the given image ID.
	 * Returns null if there is no such image.
	 *
	 * @param id  the identifier for the image to retrieve
	 * @return the image associated with the given ID
	 */
	public static ImageDescriptor getImageDescriptor(String id) {
		// Delegate to the plugin instance to avoid concurrent class loading problems
		return getPlugin().privateGetImageDescriptor(id);
	}
	private ImageDescriptor privateGetImageDescriptor(String id) {
		if(! imageDescriptors.containsKey(id)) {
			createImageDescriptor(getPlugin(), id);
		}
		return imageDescriptors.get(id);
	}

	/**
	 * Convenience method to get an image descriptor for an extension
	 *
	 * @param extension  the extension declaring the image
	 * @param subdirectoryAndFilename  the path to the image
	 * @return the image
	 */
	public static ImageDescriptor getImageDescriptorFromExtension(IExtension extension, String subdirectoryAndFilename) {
		URL iconURL = FileLocator.find(Platform.getBundle(extension.getContributor().getName()), IPath.fromOSString(subdirectoryAndFilename), null);
		if (iconURL != null) {
			return ImageDescriptor.createFromURL(iconURL);
		}
		// try to search as a URL in case it is absolute path
		try {
			iconURL = FileLocator.find(new URL(subdirectoryAndFilename));
			if (iconURL != null) {
				return ImageDescriptor.createFromURL(iconURL);
			}
		} catch (MalformedURLException e) {
			//ignore
		}
		return null;
	}

	public static final String FILE_DIRTY_OVR = "ovr/dirty_ov.png"; //$NON-NLS-1$
	public static final String FILE_CHECKEDIN_OVR = "ovr/version_controlled.png"; //$NON-NLS-1$
	public static final String FILE_CHECKEDOUT_OVR = "ovr/checkedout_ov.png"; //$NON-NLS-1$
	public static final String FILE_CONFLICT_OVR = "ovr/confchg_ov.png"; //$NON-NLS-1$
	public static final String FILE_ERROR_OVR = "ovr/error_co.png"; //$NON-NLS-1$
	public static final String FILE_WARNING_OVR = "ovr/warning_co.png"; //$NON-NLS-1$
	public static final String FILE_HOURGLASS_OVR = "ovr/waiting_ovr.png"; //$NON-NLS-1$
	/*
	 * Initializes the table of images used in this plugin. The plugin is
	 * provided because this method is called before the plugin staic
	 * variable has been set. See the comment on the getPlugin() method
	 * for a description of why this is required.
	 */
	private void initializeImages(TeamUIPlugin plugin) {
		// Overlays

		privateCreateImageDescriptor(ISharedImages.IMG_DIRTY_OVR, FILE_DIRTY_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_CONFLICT_OVR, FILE_CONFLICT_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR, FILE_CHECKEDIN_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_CHECKEDOUT_OVR, FILE_CHECKEDOUT_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_ERROR_OVR, FILE_ERROR_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_WARNING_OVR, FILE_WARNING_OVR);
		privateCreateImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR, FILE_HOURGLASS_OVR);

		// Target Management Icons
		createImageDescriptor(plugin, ITeamUIImages.IMG_SITE_ELEMENT);

		// Sync View Icons
		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_INCOMING);
		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_OUTGOING);
		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_CONFLICTING);
		createImageDescriptor(plugin, ITeamUIImages.IMG_REFRESH);
		createImageDescriptor(plugin, ITeamUIImages.IMG_CHANGE_FILTER);
		createImageDescriptor(plugin, ITeamUIImages.IMG_IGNORE_WHITESPACE);
		createImageDescriptor(plugin, ITeamUIImages.IMG_COLLAPSE_ALL);
		createImageDescriptor(plugin, ITeamUIImages.IMG_COLLAPSE_ALL_ENABLED);

		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_INCOMING_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_OUTGOING_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_DLG_SYNC_CONFLICTING_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_REFRESH_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_IGNORE_WHITESPACE_DISABLED);

		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_CATCHUP);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_RELEASE);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_FREE);

		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_CATCHUP_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_RELEASE_DISABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_FREE_DISABLED);

		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_CATCHUP_ENABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_RELEASE_ENABLED);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_MODE_FREE_ENABLED);

		// Wizard banners
		createImageDescriptor(plugin, ITeamUIImages.IMG_PROJECTSET_IMPORT_BANNER);
		createImageDescriptor(plugin, ITeamUIImages.IMG_PROJECTSET_EXPORT_BANNER);
		createImageDescriptor(plugin, ITeamUIImages.IMG_WIZBAN_SHARE);

		// Live Sync View icons
		createImageDescriptor(plugin, ITeamUIImages.IMG_COMPRESSED_FOLDER);
		createImageDescriptor(plugin, ITeamUIImages.IMG_SYNC_VIEW);
		createImageDescriptor(plugin, ITeamUIImages.IMG_HIERARCHICAL);

		// Local History Page
		createImageDescriptor(plugin, ITeamUIImages.IMG_DATES_CATEGORY);
		createImageDescriptor(plugin, ITeamUIImages.IMG_COMPARE_VIEW);
		createImageDescriptor(plugin, ITeamUIImages.IMG_LOCALREVISION_TABLE);
	}

	private URL getImageUrl(String relative) {
		return FileLocator.find(Platform.getBundle(PLUGIN_ID), IPath.fromOSString(ICON_PATH + relative), null);
	}

	/**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated display. If so, this display
	 * is returned. Otherwise the method returns the display for this workbench.
	 *
	 * @return the standard display to be used
	 */
	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = PlatformUI.getWorkbench().getDisplay();
		}
		return display;
	}

	public Image getImage(String key) {
		Image image = getImageRegistry().get(key);
		if(image == null) {
			ImageDescriptor d = getImageDescriptor(key);
			image = d.createImage();
			getImageRegistry().put(key, image);
		}
		return image;
	}

	public static void run(IRunnableWithProgress runnable) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Utils.handleError(getStandardDisplay().getActiveShell(), e, null, null);
		} catch (InterruptedException e2) {
			// Nothing to be done
		}
	}

	public org.osgi.service.prefs.Preferences getInstancePreferences() {
		return InstanceScope.INSTANCE.getNode(getBundle().getSymbolicName());
	}

	public synchronized TeamStateProvider getDecoratedStateProvider(RepositoryProviderType rpt) {
		TeamStateProvider provider = decoratedStateProviders.get(rpt.getID());
		if (provider != null)
			return provider;
		Subscriber subscriber = rpt.getSubscriber();
		if (subscriber != null) {
			provider = new SubscriberTeamStateProvider(subscriber);
			decoratedStateProviders.put(rpt.getID(), provider);
			return provider;
		}
		return null;
	}

	/**
	 * Return a decorated state provider that delegates to the appropriate team
	 * provider.
	 * @return a decorated state provider that delegates to the appropriate team
	 * provider
	 */
	public synchronized ITeamStateProvider getDecoratedStateProvider() {
		if (provider == null)
			provider = new WorkspaceTeamStateProvider();
		return provider;
	}

	public ISynchronizeManager getSynchronizeManager() {
		if (synchronizeManager == null) {
			synchronizeManager = new SynchronizeManager();
		}
		return synchronizeManager;
	}
}
