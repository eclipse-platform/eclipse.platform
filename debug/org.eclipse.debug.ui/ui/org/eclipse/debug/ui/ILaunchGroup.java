/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A launch group identifies a group of launch configurations by a launch mode
 * and category. The launch configuration dialog can be opened on a launch
 * group, and a launch history is maintained for each group. A launch group is
 * defined in plug-in XML via the <code>launchGroups</code> extension point.
 * <p>
 * Following is an example of a launch group contribution:
 * </p>
 *
 * <pre>
 * 	&lt;extension point="org.eclipse.debug.ui.launchGroups"&gt;
 * 		&lt;launchGroup
 * 			  id="com.example.ExampleLaunchGroupId"
 * 			  mode="run"
 * 			  label="Run"
 * 			  image="icons\run.png"
 * 		&lt;/launchGroup&gt;
 * 	&lt;/extension&gt;
 * </pre>
 * <p>
 * The debug platform defines constants for the identifiers of the launch groups
 * provided by the debug platform:
 * </p>
 * <ul>
 * <li>IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP</li>
 * <li>IDebugUIConstants.ID_RUN_LAUNCH_GROUP</li>
 * <li>IDebugUIConstants.ID_PROFILE_LAUNCH_GROUP</li>
 * </ul>
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILaunchGroup {

	/**
	 * Returns the image for this launch group, or <code>null</code>
	 * if none.
	 *
	 * @return the image for this launch group, or <code>null</code> if none
	 */
	ImageDescriptor getImageDescriptor();

	/**
	 * Returns the banner image for this launch group, or <code>null</code> if
	 * none
	 *
	 * @return the banner image for this launch group, or <code>null</code> if
	 * none
	 */
	ImageDescriptor getBannerImageDescriptor();

	/**
	 * Returns the label for this launch group
	 *
	 * @return the label for this launch group
	 */
	String getLabel();

	/**
	 * Returns the id for this launch group
	 *
	 * @return the id for this launch group
	 */
	String getIdentifier();

	/**
	 * Returns the category for this launch group, possibly <code>null</code>
	 *
	 * @return the category for this launch group, possibly <code>null</code>
	 */
	String getCategory();

	/**
	 * Returns the mode for this launch group
	 *
	 * @return the mode for this launch group
	 */
	String getMode();

	/**
	 * Returns whether this launch group is public
	 *
	 * @return boolean
	 */
	boolean isPublic();

}

