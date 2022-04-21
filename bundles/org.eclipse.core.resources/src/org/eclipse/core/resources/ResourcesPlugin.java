/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add PT_FILTER_PROVIDERS
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] add PT_VARIABLE_PROVIDERS
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Tom Hochstein (Freescale) - Bug 409996 - 'Restore Defaults' does not work properly on Project Properties > Resource tab
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Christoph Läubrich 	- Issue #52 - Make ResourcesPlugin more dynamic and better handling early start-up
 *     						- Issue #68 - Use DS for CheckMissingNaturesListener
 *******************************************************************************/
package org.eclipse.core.resources;

import java.nio.charset.Charset;
import java.util.Hashtable;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The plug-in runtime class for the Resources plug-in. This is the starting
 * point for all workspace and resource manipulation. A typical sequence of
 * events would be for a dependent plug-in to track the
 * <code>org.eclipse.core.resources.IWorkspace</code> service. Doing so would
 * cause this plug-in to be activated and the workspace (if any) to be loaded
 * from disk and initialized.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ResourcesPlugin extends Plugin {

	/**
	 * Unique identifier constant (value <code>"org.eclipse.core.resources"</code>)
	 * for the standard Resources plug-in.
	 */
	public static final String PI_RESOURCES = "org.eclipse.core.resources"; //$NON-NLS-1$

	/*====================================================================
	 * Constants defining the ids of the standard workspace extension points:
	 *====================================================================*/

	/**
	 * Simple identifier constant (value <code>"builders"</code>)
	 * for the builders extension point.
	 */
	public static final String PT_BUILDERS = "builders"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"natures"</code>)
	 * for the natures extension point.
	 */
	public static final String PT_NATURES = "natures"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"markers"</code>)
	 * for the markers extension point.
	 */
	public static final String PT_MARKERS = "markers"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"fileModificationValidator"</code>)
	 * for the file modification validator extension point.
	 */
	public static final String PT_FILE_MODIFICATION_VALIDATOR = "fileModificationValidator"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"moveDeleteHook"</code>)
	 * for the move/delete hook extension point.
	 *
	 * @since 2.0
	 */
	public static final String PT_MOVE_DELETE_HOOK = "moveDeleteHook"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"teamHook"</code>)
	 * for the team hook extension point.
	 *
	 * @since 2.1
	 */
	public static final String PT_TEAM_HOOK = "teamHook"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"refreshProviders"</code>)
	 * for the auto-refresh refresh providers extension point.
	 *
	 * @since 3.0
	 */
	public static final String PT_REFRESH_PROVIDERS = "refreshProviders"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"modelProviders"</code>)
	 * for the model providers extension point.
	 *
	 * @since 3.2
	 */
	public static final String PT_MODEL_PROVIDERS = "modelProviders"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"variableProviders"</code>)
	 * for the variable providers extension point.
	 *
	 * @since 3.6
	 */
	public static final String PT_VARIABLE_PROVIDERS = "variableResolvers"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"filterMatchers"</code>)
	 * for the filter matchers extension point.
	 *
	 * @since 3.6
	 */
	public static final String PT_FILTER_MATCHERS = "filterMatchers"; //$NON-NLS-1$

	/**
	 * Constant identifying the job family identifier for the background autobuild job.
	 *
	 * @see IJobManager#join(Object, IProgressMonitor)
	 * @since 3.0
	 */
	public static final Object FAMILY_AUTO_BUILD = new Object();

	/**
	 * Constant identifying the job family identifier for the background auto-refresh job.
	 *
	 * @see IJobManager#join(Object, IProgressMonitor)
	 * @since 3.1
	 */
	public static final Object FAMILY_AUTO_REFRESH = new Object();

	/**
	 * Constant identifying the job family identifier for a background build job. All clients
	 * that schedule background jobs for performing builds should include this job
	 * family in their implementation of <code>belongsTo</code>.
	 *
	 * @see IJobManager#join(Object, IProgressMonitor)
	 * @see Job#belongsTo(Object)
	 * @since 3.0
	 */
	public static final Object FAMILY_MANUAL_BUILD = new Object();

	/**
	 * Constant identifying the job family identifier for a background refresh job. All clients
	 * that schedule background jobs for performing refreshing should include this job
	 * family in their implementation of <code>belongsTo</code>.
	 *
	 * @see IJobManager#join(Object, IProgressMonitor)
	 * @see Job#belongsTo(Object)
	 * @since 3.4
	 */
	public static final Object FAMILY_MANUAL_REFRESH = new Object();

	/**
	 * Name of a preference indicating the encoding to use when reading text
	 * files in the workspace.  The value is a string, and may
	 * be the default empty string, indicating that the file system encoding should
	 * be used instead.  The file system encoding can be retrieved using
	 * <code>System.getProperty("file.encoding")</code>.
	 * There is also a convenience method <code>getEncoding</code> which returns
	 * the value of this preference, or the file system encoding if this
	 * preference is not set.
	 * <p>
	 * Note that there is no guarantee that the value is a supported encoding.
	 * Callers should be prepared to handle <code>UnsupportedEncodingException</code>
	 * where this encoding is used.
	 * </p>
	 *
	 * @see #getEncoding()
	 * @see java.io.UnsupportedEncodingException
	 */
	public static final String PREF_ENCODING = "encoding"; //$NON-NLS-1$

	/**
	 * Common prefix for workspace preference names.
	 * @since 2.1
	 */
	private static final String PREF_DESCRIPTION_PREFIX = "description."; //$NON-NLS-1$

	/**
	 * @deprecated Do not use.
	 * @since 3.0
	 */
	@Deprecated
	public static final String PREF_MAX_NOTIFICATION_DELAY = "maxnotifydelay"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether the workspace performs auto-
	 * builds.
	 *
	 * @see IWorkspaceDescription#isAutoBuilding()
	 * @see IWorkspaceDescription#setAutoBuilding(boolean)
	 * @since 2.1
	 */
	public static final String PREF_AUTO_BUILDING = PREF_DESCRIPTION_PREFIX + "autobuilding"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the order projects in the workspace
	 * are built.
	 *
	 * @see IWorkspaceDescription#getBuildOrder()
	 * @see IWorkspaceDescription#setBuildOrder(String[])
	 * @since 2.1
	 */
	public static final String PREF_BUILD_ORDER = PREF_DESCRIPTION_PREFIX + "buildorder"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether to use the workspace's
	 * default order for building projects.
	 * @since 2.1
	 */
	public static final String PREF_DEFAULT_BUILD_ORDER = PREF_DESCRIPTION_PREFIX + "defaultbuildorder"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the maximum number of times that the
	 * workspace should rebuild when builders affect projects that have already
	 * been built.
	 *
	 * @see IWorkspaceDescription#getMaxBuildIterations()
	 * @see IWorkspaceDescription#setMaxBuildIterations(int)
	 * @since 2.1
	 */
	public static final String PREF_MAX_BUILD_ITERATIONS = PREF_DESCRIPTION_PREFIX + "maxbuilditerations"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether to apply the specified history size policy.
	 *
	 * @see IWorkspaceDescription#isApplyFileStatePolicy()
	 * @see IWorkspaceDescription#setApplyFileStatePolicy(boolean)
	 * @since 3.6
	 */
	public static final String PREF_APPLY_FILE_STATE_POLICY = PREF_DESCRIPTION_PREFIX + "applyfilestatepolicy"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the maximum number of milliseconds a
	 * file state should be kept in the local history
	 *
	 * @see IWorkspaceDescription#getFileStateLongevity()
	 * @see IWorkspaceDescription#setFileStateLongevity(long)
	 * @since 2.1
	 */
	public static final String PREF_FILE_STATE_LONGEVITY = PREF_DESCRIPTION_PREFIX + "filestatelongevity"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the maximum permitted size of a file
	 * to be stored in the local history
	 *
	 * @see IWorkspaceDescription#getMaxFileStateSize()
	 * @see IWorkspaceDescription#setMaxFileStateSize(long)
	 * @since 2.1
	 */
	public static final String PREF_MAX_FILE_STATE_SIZE = PREF_DESCRIPTION_PREFIX + "maxfilestatesize"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether derived files should be stored
	 * in the local history.
	 *
	 * @see IWorkspaceDescription#isKeepDerivedState()
	 * @see IWorkspaceDescription#setKeepDerivedState(boolean)
	 * @since 3.15
	 */
	public static final String PREF_KEEP_DERIVED_STATE = PREF_DESCRIPTION_PREFIX + "keepDerivedState"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the maximum number of states per
	 * file that can be stored in the local history.
	 *
	 * @see IWorkspaceDescription#getMaxFileStates()
	 * @see IWorkspaceDescription#setMaxFileStates(int)
	 * @since 2.1
	 */
	public static final String PREF_MAX_FILE_STATES = PREF_DESCRIPTION_PREFIX + "maxfilestates"; //$NON-NLS-1$
	/**
	 * Name of a preference for configuring the amount of time in milliseconds
	 * between automatic workspace snapshots
	 *
	 * @see IWorkspaceDescription#getSnapshotInterval()
	 * @see IWorkspaceDescription#setSnapshotInterval(long)
	 * @since 2.1
	 */
	public static final String PREF_SNAPSHOT_INTERVAL = PREF_DESCRIPTION_PREFIX + "snapshotinterval"; //$NON-NLS-1$

	/**
	 * Name of a preference for turning off support for linked resources.  When
	 * this preference is set to "true", attempting to create linked resources will fail.
	 * @since 2.1
	 */
	public static final String PREF_DISABLE_LINKING = PREF_DESCRIPTION_PREFIX + "disableLinking";//$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether the workspace performs auto-
	 * refresh.  Auto-refresh installs a file-system listener, or performs
	 * periodic file-system polling to actively discover changes in the resource
	 * hierarchy.
	 * @since 3.0
	 */
	public static final String PREF_AUTO_REFRESH = "refresh.enabled"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether out-of-sync resources are automatically
	 * asynchronously refreshed, when discovered to be out-of-sync by the workspace.
	 * <p>
	 * This preference suppresses out-of-sync CoreException for some read methods, including:
	 * {@link IFile#getContents()} &amp; {@link IFile#getContentDescription()}.
	 * </p>
	 * <p>
	 * In the future the workspace may enable other lightweight auto-refresh mechanisms when this
	 * preference is true. (The existing {@link ResourcesPlugin#PREF_AUTO_REFRESH} will continue
	 * to enable filesystem hooks and the existing polling based monitor.)
	 * </p>
	 * See the discussion: https://bugs.eclipse.org/303517
	 * @since 3.7
	 */
	public static final String PREF_LIGHTWEIGHT_AUTO_REFRESH = "refresh.lightweight.enabled"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring whether encodings for derived
	 * resources within the project should be stored in a separate derived
	 * preference file.
	 *
	 * @since 3.7
	 */
	public static final String PREF_SEPARATE_DERIVED_ENCODINGS = "separateDerivedEncodings"; //$NON-NLS-1$

	/**
	 * Default setting for {@value #PREF_SEPARATE_DERIVED_ENCODINGS}.
	 *
	 * @since 3.9
	 */
	public static final boolean DEFAULT_PREF_SEPARATE_DERIVED_ENCODINGS = false;

	/**
	 * Name of a preference for configuring the marker severity in case project
	 * description references an unknown nature.
	 *
	 * @since 3.13
	 */
	public static final String PREF_MISSING_NATURE_MARKER_SEVERITY = "missingNatureMarkerSeverity"; //$NON-NLS-1$

	/**
	 * Name of the preference to set max number of concurrent jobs running the workspace build.
	 * @since 3.13
	 */
	public static final String PREF_MAX_CONCURRENT_BUILDS = "maxConcurrentBuilds"; //$NON-NLS-1$

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static volatile ResourcesPlugin plugin;

	private ServiceRegistration<DebugOptionsListener> debugRegistration;

	private ServiceTracker<Location, Workspace> instanceLocationTracker;

	private WorkspaceInitCustomizer workspaceInitCustomizer;

	/**
	 * Returns the encoding to use when reading text files in the workspace. This is
	 * the value of the <code>PREF_ENCODING</code> preference, or the file system
	 * encoding (<code>System.getProperty("file.encoding")</code>) if the preference
	 * is not set, the workspace is not ready yet or any other condition where it
	 * could not be determined.
	 * <p>
	 * Note that this method does not check whether the result is a supported
	 * encoding. Callers should be prepared to handle
	 * <code>UnsupportedEncodingException</code> where this encoding is used.
	 *
	 * <p>
	 * <b>Hint:</b> Using this method might return different results depending on
	 * the system state. Code that don't want to be affected from this ambiguities
	 * should do the following:
	 * </p>
	 * <ol>
	 * <li>using any of your favorite techniques (Declarative Services,
	 * ServiceTracker, Blueprint, ...) to track the workspace</li>
	 * <li>Calling workspace.getRoot().getDefaultCharset(false)</li>
	 * <li>If <code>null</code> is returned take the appropriate action, e.g fall
	 * back to <code>System.getProperty("file.encoding")</code> or even better
	 * {@link Charset#defaultCharset()}</li>
	 * </ol>
	 *
	 * @return the encoding to use when reading text files in the workspace
	 * @see java.io.UnsupportedEncodingException
	 */
	public static String getEncoding() {
		ResourcesPlugin resourcesPlugin = plugin;
		if (resourcesPlugin == null) {
			return getSystemEncoding();
		}
		Workspace workspace = resourcesPlugin.workspaceInitCustomizer.workspace;
		if (workspace == null) {
			return getSystemEncoding();
		}
		try {
			String enc = workspace.getRoot().getDefaultCharset(false);
			if (enc == null) {
				return getSystemEncoding();
			}
			return enc;
		} catch (CoreException e) {
			// should never happen here... even if we fall back to system encoding...
			return getSystemEncoding();
		}
	}

	private static String getSystemEncoding() {
		return System.getProperty("file.encoding"); //$NON-NLS-1$
	}

	/**
	 * Returns the Resources plug-in.
	 *
	 * @return the single instance of this plug-in runtime class
	 */
	public static ResourcesPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Returns the workspace. The workspace is not accessible after the resources
	 * plug-in has shutdown.
	 *
	 * <b>Hint:</b> Accessing the Workspace in a static way is prone to start-up
	 * order problem, please consider using any of your favorite techniques
	 * (Declarative Services, ServiceTracker, Blueprint, ...) instead. Please see
	 * the documentation of {@link IWorkspace} for more information.
	 *
	 * @return the workspace that was created by the single instance of this plug-in
	 *         class.
	 */
	public static IWorkspace getWorkspace() {
		ResourcesPlugin resourcesPlugin = plugin;
		if (resourcesPlugin == null) {
			// this happens when the resource plugin is shut down already... or never
			// started!
			throw new IllegalStateException(Messages.resources_workspaceClosedStatic);
		}
		Workspace workspace = resourcesPlugin.workspaceInitCustomizer.workspace;
		if (workspace == null) {
			throw new IllegalStateException(Messages.resources_workspaceClosedStatic);
		}
		return workspace;
	}

	/**
	 * This implementation of the corresponding {@link BundleActivator} method
	 * closes the workspace without saving.
	 * @see BundleActivator#stop(BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		// unregister debug options listener
		debugRegistration.unregister();
		instanceLocationTracker.close();

		// save the preferences for this plug-in
		getPlugin().savePluginPreferences();
		plugin = null;
	}

	/**
	 * This implementation of the corresponding {@link BundleActivator} method
	 * opens the workspace.
	 * @see BundleActivator#start(BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		workspaceInitCustomizer = new WorkspaceInitCustomizer(context);
		// register debug options listener
		Hashtable<String, String> properties = new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, PI_RESOURCES);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.RESOURCES_DEBUG_OPTIONS_LISTENER,
				properties);
		instanceLocationTracker = new ServiceTracker<>(context,
				context.createFilter(String.format("(&%s(%s=*))", Location.INSTANCE_FILTER, //$NON-NLS-1$
						Location.SERVICE_PROPERTY_URL)),
				workspaceInitCustomizer);
		plugin = this; // must before open the tracker, as this can cause the registration of the
						// workspace and this might trigger code that calls the static method then.
		instanceLocationTracker.open();
	}

	private final class WorkspaceInitCustomizer implements ServiceTrackerCustomizer<Location, Workspace> {
		private final BundleContext context;
		private volatile Workspace workspace;
		private ServiceRegistration<IWorkspace> workspaceRegistration;

		private WorkspaceInitCustomizer(BundleContext context) {
			this.context = context;
		}

		@Override
		public Workspace addingService(ServiceReference<Location> reference) {
			if (workspace != null) {
				return null; // there can only be one workspace right now...
			}
			Location location = context.getService(reference);
			if (location == null) {
				return null; // we can't use that service...
			}
			// the workspace is accessible from now on, this is because some plugins require
			// access to it in the early startup phase
			workspace = new Workspace();
			IStatus result;
			try {
				result = workspace.open(null);
				if (!result.isOK())
					getLog().log(result);
				workspaceRegistration = context.registerService(IWorkspace.class, workspace, null);
				return workspace;
			} catch (CoreException e) {
				getLog().log(e.getStatus());
			} catch (IllegalStateException e) {
				getLog().log(Status.error("Internal error open workspace", e)); //$NON-NLS-1$
			}
			return null;
		}

		@Override
		public void modifiedService(ServiceReference<Location> reference, Workspace service) {
			// nothing to care about here as we already tracking the service with the
			// properties of interest
		}

		@Override
		public void removedService(ServiceReference<Location> reference, Workspace service) {
			if (service == workspace) {
				try {
					workspaceRegistration.unregister();
				} catch (RuntimeException e) {
					getLog().log(Status.warning("Unregistering workspaces throws an exception", e)); //$NON-NLS-1$
				}
				try {
					service.close(null);
				} catch (CoreException e) {
					getLog().log(e.getStatus());
				} finally {
					workspace = null;
					context.ungetService(reference);
				}
			}
		}
	}

}
