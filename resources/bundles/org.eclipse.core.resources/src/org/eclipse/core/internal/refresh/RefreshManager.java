/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Christoph Läubrich 	- Issue #84 - RefreshManager access ResourcesPlugin.getWorkspace in the init phase
 *     						- Issue #97 - RefreshManager.manageAutoRefresh calls ResourcesPlugin.getWorkspace before the Workspace is fully open
 *     Latha Patil(ETAS GmbH)	- Issue #497- Get rid of deprecated org.eclipse.core.runtime.Preferences in platform code
 *******************************************************************************/
package org.eclipse.core.internal.refresh;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.resources.IManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.resources.refresh.IRefreshResult;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Manages auto-refresh functionality, including maintaining the active
 * set of monitors and controlling the job that performs periodic refreshes
 * on out of sync resources.
 *
 * @since 3.0
 */
public class RefreshManager implements IRefreshResult, IManager, EclipsePreferences.IPreferenceChangeListener {
	public static final String DEBUG_PREFIX = "Auto-refresh: "; //$NON-NLS-1$
	volatile MonitorManager monitors;
	private volatile RefreshJob refreshJob;

	/**
	 * The workspace.
	 */
	private final Workspace workspace;

	public RefreshManager(Workspace workspace) {
		this.workspace = workspace;
	}

	/*
	 * Starts or stops auto-refresh depending on the auto-refresh preference.
	 */
	protected void manageAutoRefresh(boolean enabled, IProgressMonitor progressMonitor) {
		//do nothing if we have already shutdown
		if (refreshJob == null) {
			return;
		}
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 1);
		if (enabled) {
			monitors.start(subMonitor.split(1));
		} else {
			refreshJob.cancel();
			monitors.stop();
		}
	}

	Workspace getWorkspace() {
		return workspace;
	}

	@Override
	public void monitorFailed(IRefreshMonitor monitor, IResource resource) {
		if (monitors != null) {
			monitors.monitorFailed(monitor, resource);
		}
	}

	/**
	 * Checks for changes to the PREF_AUTO_REFRESH property.
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		String property = event.getKey();
		if (ResourcesPlugin.PREF_AUTO_REFRESH.equals(property)) {
			final boolean autoRefresh = Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES,
					ResourcesPlugin.PREF_AUTO_REFRESH, false, null);
			String jobName = autoRefresh ? Messages.refresh_installMonitorsOnWorkspace : Messages.refresh_uninstallMonitorsOnWorkspace;
			MonitorJob.createSystem(jobName, getWorkspace().getRoot(),
					(ICoreRunnable) monitor -> manageAutoRefresh(autoRefresh, monitor)).schedule();
		}
	}

	@Override
	public void refresh(IResource resource) {
		//do nothing if we have already shutdown
		if (refreshJob != null) {
			refreshJob.refresh(resource);
		}
	}

	/**
	 * Shuts down the refresh manager.  This only happens when
	 * the resources plugin is going away.
	 */
	@Override
	public void shutdown(IProgressMonitor monitor) {
		if (refreshJob == null) {
			// do nothing if we have already shutdown
			return;
		}
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).removePreferenceChangeListener(this);
		if (monitors != null) {
			monitors.stop();
			monitors = null;
		}
		if (refreshJob != null) {
			refreshJob.stop();
			refreshJob = null;
		}
	}

	/**
	 * Initializes the refresh manager. This does a minimal amount of work
	 * if auto-refresh is turned off.
	 */
	@Override
	public void startup(IProgressMonitor monitor) {
		refreshJob = new RefreshJob(workspace);
		monitors = new MonitorManager(workspace, this);

		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).addPreferenceChangeListener(this);
		boolean autoRefresh = Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES,
				ResourcesPlugin.PREF_AUTO_REFRESH, false, null);
		if (autoRefresh) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
			manageAutoRefresh(autoRefresh, subMonitor.split(1));
		}
	}
}
