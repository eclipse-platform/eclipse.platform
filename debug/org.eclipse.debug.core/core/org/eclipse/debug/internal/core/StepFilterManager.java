/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Anton Kosyakov (Itemis AG) - Bug 438621 - [step filtering] Provide an extension point to enhance methods step filtering.
 *******************************************************************************/
package org.eclipse.debug.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.commands.IStepFiltersHandler;
import org.eclipse.debug.core.model.IStepFilter;
import org.eclipse.debug.internal.core.commands.DebugCommandRequest;

/**
 * As targets are launched, this manager sets its step filter
 * support settings according to the "use step filter" setting.
 *
 * @since 3.0
 */
public class StepFilterManager implements ILaunchListener {

	public static final String PREF_USE_STEP_FILTERS = DebugPlugin.getUniqueIdentifier() + ".USE_STEP_FILTERS"; //$NON-NLS-1$

	/**
	 * The step filter manager is instantiated by the debug UI plug-in,
	 * and should be accessed from the <code>DebugUIPlugin</code> class.
	 */
	protected StepFilterManager() {
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	/**
	 * This method is called by the debug UI plug-in at shutdown.
	 */
	public void shutdown() {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	@Override
	public void launchAdded(ILaunch launch) {
		launchChanged(launch);
	}

	@Override
	public void launchChanged(ILaunch launch) {
		IStepFiltersHandler command = launch.getAdapter(IStepFiltersHandler.class);
		if (command != null) {
			command.execute(new DebugCommandRequest(new Object[]{launch}));
		}
	}

	/**
	 * Returns whether the 'use step filters' preference is on.
	 *
	 * @return whether to use step filters
	 */
	public boolean isUseStepFilters() {
		return Platform.getPreferencesService().getBoolean(DebugPlugin.getUniqueIdentifier(), PREF_USE_STEP_FILTERS, false, null);
	}

	/**
	 * Sets whether to use step filters.
	 *
	 * @param useFilters whether to use step filters
	 */
	public void setUseStepFilters(boolean useFilters) {
		Preferences.setBoolean(DebugPlugin.getUniqueIdentifier(), PREF_USE_STEP_FILTERS, useFilters, null);
		ILaunch[] launchs = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (ILaunch launch : launchs) {
			launchChanged(launch);
		}
	}

	@Override
	public void launchRemoved(ILaunch launch) {}

	/**
	 * Returns any step filters that have been contributed for the given model
	 * identifier.
	 *
	 * @param modelIdentifier the model identifier
	 * @return step filters that have been contributed for the given model
	 *         identifier, possibly an empty collection
	 * @since 3.10
	 * @see org.eclipse.debug.core.model.IStepFilter
	 */
	public IStepFilter[] getStepFilters(String modelIdentifier) {
		initialize();
		List<IStepFilter> select = new ArrayList<>();
		for (StepFilter extension : stepFilters) {
			Collections.addAll(select, extension.getStepFilters(modelIdentifier));
		}
		return select.toArray(new IStepFilter[select.size()]);
	}

	private List<StepFilter> stepFilters = null;

	private synchronized void initialize() {
		if (stepFilters == null) {
			IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_STEP_FILTERS);
			IConfigurationElement[] extensions = point.getConfigurationElements();
			stepFilters = new ArrayList<>();
			for (IConfigurationElement extension : extensions) {
				try {
					stepFilters.add(new StepFilter(extension));
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
			}
		}
	}
}
