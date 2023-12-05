/*******************************************************************************
 * Copyright (c) 2018, 2019 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial version
 *******************************************************************************/

package org.eclipse.debug.tests.ui;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;

/**
 * A Tab whose sole purpose is to say if it was initialized and activated
 * properly
 */
public abstract class SpyTab extends AbstractLaunchConfigurationTab {

	private boolean initialized;
	private boolean activated;

	@Override
	public void createControl(Composite parent) {
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		initialized = true;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		activated = true;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isActivated() {
		return activated;
	}

	// These are necessary because I need several tabs in the launch config and
	// using always the same kind (class) of tab produces incorrect results
	public static class SpyTabA extends SpyTab {
	}

	public static class SpyTabB extends SpyTab {
	}
}
