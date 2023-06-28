/*******************************************************************************
 * Copyright (c) 2017, 2021 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *     IBM Corporation - Bug fixes
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.launchview.LaunchConfigurationViewPlugin;
import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

public class DebugCoreLaunchObject implements ILaunchObject, Comparable<ILaunchObject> {

	private final ILaunchConfiguration config;

	public DebugCoreLaunchObject(ILaunchConfiguration config) {
		this.config = config;
	}

	@Override
	public String getId() {
		return config.getName();
	}

	@Override
	public StyledString getLabel() {
		return new StyledString(config.getName());
	}

	@Override
	public ILaunchConfigurationType getType() {
		try {
			return config.getType();
		} catch (CoreException e) {
			ILog.of(this.getClass()).error(NLS.bind(LaunchViewMessages.DebugCoreLaunchObject_CannotGetType, config.getName()), e);
		}
		return null;
	}

	@Override
	public void launch(ILaunchMode mode) {
		LaunchConfigurationViewPlugin.getExecutor().launchProcess(config, mode.getIdentifier(), true, false, null);
	}

	@Override
	public boolean canTerminate() {
		return !findTerminateableLaunches(config.getName()).isEmpty();
	}

	@Override
	public void terminate() {
		// DON'T use Eclipse' mechanism - it's a little broken if shutdown of
		// the processes takes longer than a few seconds.
		// Instead we start a job that tries to terminate processes. If the job
		// itself is stopped, we give up like Eclipse does.
		Collection<ILaunch> launches = findTerminateableLaunches(config.getName());
		for (ILaunch launch: launches) {
			Job terminateJob = new Job(NLS.bind(LaunchViewMessages.DebugCoreLaunchObject_Terminate, config.getName())) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (!launch.isTerminated()) {
						try {
							launch.terminate();
						} catch (DebugException e) {
							// could not terminate - but we cannot do anything
							// anyway... :(
							return new Status(IStatus.WARNING, LaunchViewBundleInfo.PLUGIN_ID, NLS.bind(LaunchViewMessages.DebugCoreLaunchObject_CannotTerminate, config.getName()));
						}
					}
					return Status.OK_STATUS;
				}
			};

			terminateJob.setUser(true);
			terminateJob.schedule();
		}
	}

	@Override
	public void relaunch() {
		ILaunch launch = findLaunch(getId());
		String launchMode = launch.getLaunchMode();
		try {
			launch.terminate();
			LaunchConfigurationViewPlugin.getExecutor().launchProcess(config, launchMode, true, false, null);
		} catch (Exception e) {
			throw new RuntimeException(NLS.bind(LaunchViewMessages.DebugCoreLaunchObject_CannotRelaunch, config.getName()), e);
		}
	}

	private static ILaunch findLaunch(String name) {
		for (ILaunch l : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
			if (l.getLaunchConfiguration() == null || l.isTerminated()) {
				continue;
			}
			if (l.getLaunchConfiguration().getName().equals(name)) {
				return l;
			}
		}
		return null;
	}

	private static Collection<ILaunch> findTerminateableLaunches(String name) {
		Collection<ILaunch> result = new ArrayList<>();
		for (ILaunch l : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
			if (l.getLaunchConfiguration() == null || l.isTerminated()) {
				continue;
			}
			if (l.getLaunchConfiguration().getName().equals(name) && l.canTerminate()) {
				result.add(l);
			}
		}
		return result;
	}

	@Override
	public void edit() {
		// This prefers "debug" mode as the Eclipse infrastructure
		// requires a group to be given. This covers most launch configurations
		// as most of them support debug, whereas e.g. "Remote Java Application"
		// does not support "run". Ant launch configurations in turn do not
		// support debug...
		ILaunchGroup group = DebugUITools.getLaunchGroup(config, "debug"); //$NON-NLS-1$
		if (group == null) {
			group = DebugUITools.getLaunchGroup(config, "run"); //$NON-NLS-1$
		}
		if (group != null) { // Id Debug & run both not supported and only
								// profile is supported
			DebugUITools.openLaunchConfigurationDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), config, group.getIdentifier(), null);
		}
	}

	@Override
	public boolean isFavorite() {
		try {
			return !config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, Collections.emptyList()).isEmpty();
		} catch (CoreException e) {
			return false; // oups
		}
	}

	@Override
	public int compareTo(ILaunchObject o) {
		if (getId() == null) {
			ILog.of(this.getClass()).warn(NLS.bind(LaunchViewMessages.LaunchObject_ErrorNoId, this), null);
			if (o.getId() == null) {
				return 0;
			}
			return 1;
		}

		return getId().compareTo(o.getId());
	}

}
