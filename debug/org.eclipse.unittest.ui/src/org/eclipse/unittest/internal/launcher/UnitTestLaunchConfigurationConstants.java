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
 *******************************************************************************/
package org.eclipse.unittest.internal.launcher;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Attribute keys used by the UnitTest LaunchConfiguration. Note that these
 * constants are not API and might change in the future.
 */
public class UnitTestLaunchConfigurationConstants {

	/**
	 * An identifier of a property to be set on a {@link ILaunchConfiguration} to
	 * identify an {@link ITestViewSupport} implementation.
	 */
	public static final String ATTR_UNIT_TEST_VIEW_SUPPORT = UnitTestPlugin.PLUGIN_ID + ".TEST_VIEW_SUPPPORT"; //$NON-NLS-1$

	private UnitTestLaunchConfigurationConstants() {
		// No instance allowed
	}

}
