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
 * A launch configuration tab group is used to edit/view attributes of a
 * specific type of launch configuration. Launch configurations are presented in
 * a dialog, with a tab folder. Each tab manipulates one or more attributes of a
 * launch configuration. The tab group controls which tabs are displayed for a
 * specific type of launch configuration, and provides a mechanism for
 * overriding configuration initialization performed by tabs.
 * <p>
 * A tab group has the following lifecycle methods:
 * </p>
 * <ul>
 * <li><code>createTabs(ILaunchConfigurationDialog, String)</code> - this is the
 * first method called on a tab group after it is instantiated.</li>
 * <li><code>initializeFrom(ILaunchConfiguration)</code> - called when a launch
 * configuration is selected to be displayed.</li>
 * <li><code>performApply(ILaunchConfigurationWorkingCopy)</code> - called when
 * a tab group's values are to be written to a launch configuration.</li>
 * <li><code>dispose()</code> - the last method called on a tab group, when it
 * is to perform any required cleanup. Note that a tab can be disposed before
 * its control has been created.</li>
 * </ul>
 * <p>
 * The method <code>setDefaults(ILaunchConfigurationWorkingCopy)</code> can be
 * called before a tab's controls are created.
 * </p>
 * <p>
 * The launch tab framework was originally designed to handle inter tab
 * communication by applying attributes from the active tab to a launch
 * configuration being edited, when a tab is exited, and by initializing a tab
 * when activated. In 3.0, the addition of the methods <code>activated</code>
 * and <code>deactivated</code> allow tabs to determine the appropriate course
 * of action. The default implementation in
 * <code>AbstractLaunchConfigurationTab</code> is to call the old methods
 * (<code>initializeFrom</code> and <code>performApply</code>). Tabs should
 * override the new methods as required.
 * </p>
 * <p>
 * A launch configuration group extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a launch configuration group extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.launchConfigurationTabGroups"&gt;
 *   &lt;launchConfigurationTabGroup
 *      id="com.example.ExampleTabGroup"
 *      type="com.example.ExampleLaunchConfigurationTypeIdentifier"
 *      class="com.example.ExampleLaunchConfigurationTabGroupClass"&gt;
 *   &lt;/launchConfigurationTabGroup&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this launch
 * configuration tab group.</li>
 * <li><code>type</code> specifies launch configuration type that this tab group
 * is applicable to (corresponds to the id of a launch configuration type
 * extension).</li>
 * <li><code>class</code> specifies a fully qualified name of a Java class that
 * implements <code>ILaunchConfigurationTabGroup</code>.</li>
 * </ul>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 *
 * @see org.eclipse.debug.core.ILaunchConfigurationType
 * @see org.eclipse.debug.core.ILaunchConfiguration
 * @see org.eclipse.debug.ui.ILaunchConfigurationTab
 * @since 2.0
 */
public interface ILaunchConfigurationTabGroup {

	/**
	 * Creates the tabs contained in this tab group for the specified
	 * launch mode. The tabs control's are not created. This is the
	 * fist method called in the lifecycle of a tab group.
	 *
	 * @param dialog the launch configuration dialog this tab group
	 *  is contained in
	 * @param mode the mode the launch configuration dialog was
	 *  opened in
	 */
	void createTabs(ILaunchConfigurationDialog dialog, String mode);

	/**
	 * Returns the tabs contained in this tab group.
	 *
	 * @return the tabs contained in this tab group
	 */
	ILaunchConfigurationTab[] getTabs();

	/**
	 * Notifies this launch configuration tab group that it has
	 * been disposed, and disposes this group's tabs. Marks the end
	 * of this tab group's lifecycle, allowing this tab group to
	 * perform any cleanup required.
	 */
	void dispose();

	/**
	 * Initializes the given launch configuration with
	 * default values for this tab group. This method
	 * is called when a new launch configuration is created
	 * such that the configuration can be initialized with
	 * meaningful values. This method may be called before
	 * tab controls are created.
	 *
	 * @param configuration launch configuration
	 */
	void setDefaults(ILaunchConfigurationWorkingCopy configuration);

	/**
	 * Initializes this group's tab controls with values from the given
	 * launch configuration. This method is called when
	 * a configuration is selected to view or edit.
	 *
	 * @param configuration launch configuration
	 */
	void initializeFrom(ILaunchConfiguration configuration);

	/**
	 * Copies values from this group's tabs into the given
	 * launch configuration.
	 *
	 * @param configuration launch configuration
	 */
	void performApply(ILaunchConfigurationWorkingCopy configuration);

	/**
	 * Notifies this tab that a configuration has been
	 * launched, resulting in the given launch. This method can be
	 * called when a tab's control does not exist, to support single-click
	 * launching.
	 *
	 * @param launch the result of launching the current
	 *  launch configuration
	 * @deprecated As of R3.0, this method is no longer called by the launch
	 *  framework. Since tabs do not exist when launching is performed elsewhere
	 *  than the launch dialog, this method cannot be relied upon for launching
	 *  functionality.
	 */
	@Deprecated void launched(ILaunch launch);
}

