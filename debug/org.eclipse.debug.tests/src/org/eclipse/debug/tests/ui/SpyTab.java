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

	private int initializedCount;
	private int activatedCount;
	private int deactivatedCount;

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [initializedCount=" + initializedCount + ", activatedCount=" + activatedCount + ", deactivatedCount=" + deactivatedCount + "]";
	}

	@Override
	public void createControl(Composite parent) {
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		++initializedCount;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		++activatedCount;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	public boolean isInitialized() {
		return initializedCount > 0;
	}

	public boolean isInitializedExactlyOnce() {
		return initializedCount == 1;
	}

	public boolean isActivated() {
		return activatedCount > 0;
	}

	public boolean isActivatedExactlyOnce() {
		return activatedCount == 1;
	}

	public boolean isDeactivated() {
		return deactivatedCount > 0;
	}

	public boolean isDeactivatedExactlyOnce() {
		return deactivatedCount == 1;
	}

	// These are necessary because I need several tabs in the launch config and
	// using always the same kind (class) of tab produces incorrect results
	public static class SpyTabA extends SpyTab {
	}

	public static class SpyTabB extends SpyTab {
	}
}
