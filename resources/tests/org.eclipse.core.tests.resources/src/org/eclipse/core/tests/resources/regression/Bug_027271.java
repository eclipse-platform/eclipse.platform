/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests how changes in the underlying preference store may affect the path
 * variable manager.
 */
// Explicitly tests deprecated API
@SuppressWarnings("deprecation")
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_027271 {

	static final String VARIABLE_PREFIX = "pathvariable."; //$NON-NLS-1$

	@BeforeEach
	public void setUp() {
		clearPathVariablesProperties();
	}

	@AfterEach
	public void tearDown() {
		clearPathVariablesProperties();
	}

	private void clearPathVariablesProperties() {
		Preferences preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
		// ensure we have no preferences related to path variables
		String[] propertyNames = preferences.propertyNames();
		for (String propertyName : propertyNames) {
			if (propertyName.startsWith(VARIABLE_PREFIX)) {
				preferences.setToDefault(propertyName);
			}
		}
	}

	@Test
	public void testBug() {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IPathVariableManager pvm = getWorkspace().getPathVariableManager();
		Preferences prefs = ResourcesPlugin.getPlugin().getPluginPreferences();

		assertThat(pvm.getPathVariableNames()).isEmpty();
		prefs.setValue(VARIABLE_PREFIX + "VALID_VAR", IPath.fromOSString("c:/temp").toPortableString());
		assertThat(pvm.getPathVariableNames()).containsExactly("VALID_VAR");

		//sets invalid value (relative path)
		IPath relativePath = IPath.fromOSString("temp");
		prefs.setValue(VARIABLE_PREFIX + "INVALID_VAR", relativePath.toPortableString());
		assertThat(pvm.getPathVariableNames()).containsExactly("VALID_VAR");

		//sets invalid value (invalid path)
		IPath invalidPath = IPath.fromOSString("c:\\a\\:\\b");
		prefs.setValue(VARIABLE_PREFIX + "ANOTHER_INVALID_VAR", invalidPath.toPortableString());
		assertFalse(IPath.EMPTY.isValidPath(invalidPath.toPortableString()));
		assertThat(pvm.getPathVariableNames()).containsExactly("VALID_VAR");
	}

}
