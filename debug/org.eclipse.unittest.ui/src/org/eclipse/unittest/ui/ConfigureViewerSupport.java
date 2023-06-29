/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.ui;

import java.util.function.Function;

import org.eclipse.unittest.internal.launcher.UnitTestLaunchConfigurationConstants;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Configures a Launch configuration Working Copy with an identifier of Test
 * View Support extension
 */
public final class ConfigureViewerSupport
		implements Function<ILaunchConfigurationWorkingCopy, ILaunchConfigurationWorkingCopy> {
	private final String identifier;

	public ConfigureViewerSupport(String testViewSupportExtensionId) {
		this.identifier = testViewSupportExtensionId;
	}

	@Override
	public ILaunchConfigurationWorkingCopy apply(ILaunchConfigurationWorkingCopy configuration) {
		if (configuration != null && identifier != null) {
			configuration.setAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT, identifier);
		}
		return configuration;
	}
}
