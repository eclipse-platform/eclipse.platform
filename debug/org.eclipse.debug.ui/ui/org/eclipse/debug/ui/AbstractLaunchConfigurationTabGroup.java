/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Common function for launch configuration tab groups.
 * Generally, a launch configuration tab group will subclass
 * this class, and define a method to create and set the tabs
 * in that group.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see ILaunchConfigurationTabGroup
 * @since 2.0
 */
public abstract class AbstractLaunchConfigurationTabGroup implements ILaunchConfigurationTabGroup {

	/**
	 * The tabs in this tab group, or <code>null</code> if not yet instantiated.
	 */
	protected ILaunchConfigurationTab[] fTabs = null;

	/**
	 * @see ILaunchConfigurationTabGroup#getTabs()
	 */
	@Override
	public ILaunchConfigurationTab[] getTabs() {
		return fTabs;
	}

	/**
	 * Sets the tabs in this group
	 *
	 * @param tabs the tabs in this group
	 */
	protected void setTabs(ILaunchConfigurationTab... tabs) {
		fTabs = tabs;
	}

	/**
	 * By default, dispose all the tabs in this group.
	 *
	 * @see ILaunchConfigurationTabGroup#dispose()
	 */
	@Override
	public void dispose() {
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs != null) {
			for (ILaunchConfigurationTab tab : tabs) {
				tab.dispose();
			}
		}
	}

	/**
	 * By default, delegate to all of the tabs in this group.
	 *
	 * @see ILaunchConfigurationTabGroup#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		ILaunchConfigurationTab[] tabs = getTabs();
		for (ILaunchConfigurationTab tab : tabs) {
			tab.setDefaults(configuration);
		}
	}

	/**
	 * By default, delegate to all of the tabs in this group.
	 *
	 * @see ILaunchConfigurationTabGroup#initializeFrom(ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		ILaunchConfigurationTab[] tabs = getTabs();
		for (ILaunchConfigurationTab tab : tabs) {
			tab.initializeFrom(configuration);
		}
	}

	/**
	 * By default, delegate to all of the tabs in this group.
	 *
	 * @see ILaunchConfigurationTabGroup#performApply(ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		ILaunchConfigurationTab[] tabs = getTabs();
		for (ILaunchConfigurationTab tab : tabs) {
			tab.performApply(configuration);
		}
	}

	/**
	 * By default, delegate to all of the tabs in this group.
	 *
	 * @see ILaunchConfigurationTabGroup#launched(ILaunch)
	 * @deprecated As of R3.0, this method is no longer called by the launch
	 *  framework. Since tabs do not exist when launching is performed elsewhere
	 *  than the launch dialog, this method cannot be relied upon for launching
	 *  functionality.
	 */
	@Deprecated
	@Override
	public void launched(ILaunch launch) {
		ILaunchConfigurationTab[] tabs = getTabs();
		for (ILaunchConfigurationTab tab : tabs) {
			tab.launched(launch);
		}
	}

}
