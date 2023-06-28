/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.quickaccess;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.ui.quickaccess.IQuickAccessComputer;
import org.eclipse.ui.quickaccess.QuickAccessElement;

public abstract class AbstractLaunchQuickAccessComputer implements IQuickAccessComputer {

	protected final ILaunchManager manager;
	protected final ILaunchMode launchMode;

	private boolean launchConfigurationsChanged = false;

	public AbstractLaunchQuickAccessComputer(ILaunchMode launchMode) {
		super();
		manager = DebugPlugin.getDefault().getLaunchManager();
		this.launchMode = launchMode;
		manager.addLaunchConfigurationListener(new ILaunchConfigurationListener() {
			@Override
			public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
				launchConfigurationsChanged = true;
			}

			@Override
			public void launchConfigurationChanged(ILaunchConfiguration configuration) {
				launchConfigurationsChanged = true;
			}

			@Override
			public void launchConfigurationAdded(ILaunchConfiguration configuration) {
				launchConfigurationsChanged = true;
			}
		});
	}

	@Override
	public QuickAccessElement[] computeElements() {
		try {
			return Arrays.stream(manager.getLaunchConfigurations()).filter(config -> {
				try {
					return config.getType().supportsMode(launchMode.getIdentifier());
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
					return false;
				}
			}).map(config -> new LaunchQuickAccessElement(config, launchMode)).toArray(QuickAccessElement[]::new);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
			return new QuickAccessElement[0];
		}
	}

	@Override
	public void resetState() {
	}

	@Override
	public boolean needsRefresh() {
		return launchConfigurationsChanged;
	}
}