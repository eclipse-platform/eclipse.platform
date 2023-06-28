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
package org.eclipse.debug.ui;


import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A launch configuration tab is used to edit/view attributes
 * of a specific type of launch configuration. Launch
 * configurations are presented in a dialog, with a tab folder.
 * Each tab manipulates one ore more attributes of a launch
 * configuration.
 * <p>
 * A tab has the following lifecycle methods:
 * <ul>
 * <li><code>setLaunchConfigurationDialog(ILaunchConfigurationDialog)</code> -
 *  this is the first method called on a tab after it is instantiated.</li>
 * <li><code>initializeFrom(ILaunchConfiguration)</code> - called when a
 *  launch configuration is selected to be displayed.</li>
 * <li><code>activated(ILaunchConfigurationWorkingCopy)</code> - called when
 *  a tab is entered.</li>
 * <li><code>deactivated(ILaunchConfigurationWorkingCopy)</code> - called when
 *  a tab is exited.</li>
 * <li><code>performApply(ILaunchConfigurationWorkingCopy)</code> - called when
 *  a tab is to write its values to a launch configuration.</li>
 * <li><code>dispose()</code> - the last method called on a tab, when it is
 *  to perform any required cleanup. Note that a tab can be disposed before its control
 * has been created.</li>
 * </ul>
 * The method <code>setDefaults(ILaunchConfigurationWorkingCopy)</code>
 * can be called before a tab's controls are created.
 * <p>
 * The launch tab framework was originally designed to handle inter tab
 * communication by applying attributes from the active tab to a launch configuration
 * being edited, when a tab is exited, and by initializing a tab when activated.
 * In 3.0, the addition of the methods <code>activated</code> and <code>deactivated</code>
 * allow tabs to determine the appropriate course of action. The default implementation
 * in <code>AbstractLaunchConfigurationTab</code> is to call the old methods
 * (<code>initializeFrom</code> and <code>performApply</code>). Tabs should override
 * the new methods as required.
 * </p>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 * @see org.eclipse.debug.core.ILaunchConfigurationType
 * @see org.eclipse.debug.core.ILaunchConfiguration
 * @since 2.0
 */
public interface ILaunchConfigurationTab {

	/**
	 * Creates the top level control for this launch configuration
	 * tab under the given parent composite.  This method is called once on
	 * tab creation, after <code>setLaunchConfigurationDialog</code>
	 * is called.
	 * <p>
	 * Implementors are responsible for ensuring that
	 * the created control can be accessed via <code>getControl</code>
	 * </p>
	 *
	 * @param parent the parent composite
	 */
	void createControl(Composite parent);

	/**
	 * Returns the top level control for this tab.
	 * <p>
	 * May return <code>null</code> if the control
	 * has not been created yet.
	 * </p>
	 *
	 * @return the top level control or <code>null</code>
	 */
	Control getControl();

	/**
	 * Initializes the given launch configuration with
	 * default values for this tab. This method
	 * is called when a new launch configuration is created
	 * such that the configuration can be initialized with
	 * meaningful values. This method may be called before this
	 * tab's control is created.
	 *
	 * @param configuration launch configuration
	 */
	void setDefaults(ILaunchConfigurationWorkingCopy configuration);

	/**
	 * Initializes this tab's controls with values from the given
	 * launch configuration. This method is called when
	 * a configuration is selected to view or edit, after this
	 * tab's control has been created.
	 *
	 * @param configuration launch configuration
	 */
	void initializeFrom(ILaunchConfiguration configuration);

	/**
	 * Notifies this launch configuration tab that it has
	 * been disposed. Marks the end of this tab's lifecycle,
	 * allowing this tab to perform any cleanup required.
	 */
	void dispose();

	/**
	 * Copies values from this tab into the given
	 * launch configuration.
	 *
	 * @param configuration launch configuration
	 */
	void performApply(ILaunchConfigurationWorkingCopy configuration);

	/**
	 * Returns the current error message for this tab.
	 * May be <code>null</code> to indicate no error message.
	 * <p>
	 * An error message should describe some error state,
	 * as opposed to a message which may simply provide instruction
	 * or information to the user.
	 * </p>
	 *
	 * @return the error message, or <code>null</code> if none
	 */
	String getErrorMessage();

	/**
	 * Returns the current message for this tab.
	 * <p>
	 * A message provides instruction or information to the
	 * user, as opposed to an error message which should
	 * describe some error state.
	 * </p>
	 *
	 * @return the message, or <code>null</code> if none
	 */
	String getMessage();

	/**
	 * Returns whether this tab is in a valid state in the context of the specified launch configuration.
	 * <p>
	 * This information is typically used by the launch configuration
	 * dialog to decide when it is okay to launch.
	 * </p>
	 *
	 * @param launchConfig launch configuration which provides context for validating this tab.
	 *         This value must not be <code>null</code>.
	 *
	 * @return whether this tab is in a valid state
	 */
	boolean isValid(ILaunchConfiguration launchConfig);

	/**
	 * Returns whether this tab is in a state that allows the launch configuration whose values
	 * this tab is showing to be saved.  This differs from <code>isValid()</code> in that <code>canSave()</code>
	 * determines if this tab prevents the current launch configuration from being saved, whereas
	 * <code>isValid()</code> determines if this tab prevents the current launch configuration from
	 * being launched.
	 *
	 * <p>
	 * This information is typically used by the launch configuration
	 * dialog to decide when it is okay to save a launch configuration.
	 * </p>
	 *
	 * @return whether this tab is in a state that allows the current launch configuration to be saved
	 */
	boolean canSave();

	/**
	 * Sets the launch configuration dialog that hosts this tab.
	 * This is the first method called on a launch configuration
	 * tab, and marks the beginning of this tab's lifecycle.
	 *
	 * @param dialog launch configuration dialog
	 */
	void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog);

	/**
	 * Notifies this tab that the specified configuration has been
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

	/**
	 * Returns the name of this tab.
	 *
	 * @return the name of this tab
	 */
	String getName();

	/**
	 * Returns the image for this tab, or <code>null</code> if none
	 *
	 * @return the image for this tab, or <code>null</code> if none
	 */
	Image getImage();

	/**
	 * Notification that this tab has become the active tab in the launch
	 * configuration dialog.
	 *
	 * @param workingCopy the launch configuration being edited
	 * @since 3.0
	 */
	void activated(ILaunchConfigurationWorkingCopy workingCopy);

	/**
	 * Notification that this tab is no longer the active tab in the launch
	 * configuration dialog.
	 *
	 * @param workingCopy the launch configuration being edited
	 * @since 3.0
	 */
	void deactivated(ILaunchConfigurationWorkingCopy workingCopy);

	/**
	 * Checks if it is OK to leave the Tab.
	 *
	 * @return <code>true</code> if OK to leave the tab or <code>false</code>
	 * @since 3.12
	 */
	default boolean OkToLeaveTab() {
		return true;
	}

	/**
	 * Perform the changes after apply.
	 *
	 * @since 3.12
	 */
	default void postApply() {
	}
}

