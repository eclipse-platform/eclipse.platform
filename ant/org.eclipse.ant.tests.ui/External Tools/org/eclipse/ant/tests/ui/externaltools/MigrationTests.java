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
package org.eclipse.ant.tests.ui.externaltools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.ant.tests.ui.externaltools.ExternalToolTestUtil.get20AntArgumentMap;
import static org.eclipse.ant.tests.ui.externaltools.ExternalToolTestUtil.get20ProgramArgumentMap;
import static org.eclipse.ant.tests.ui.externaltools.ExternalToolTestUtil.get21AntArgumentMap;
import static org.eclipse.ant.tests.ui.externaltools.ExternalToolTestUtil.get21ProgramArgumentMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.ant.tests.ui.testplugin.AntUITest;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.registry.ExternalToolMigration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.junit.jupiter.api.Test;

/**
 * Tests migration of Ant and External Tool configurations from old formats to the current format.
 */
@SuppressWarnings("restriction")
@AntUITest
public class MigrationTests {

	/**
	 * Tests migration of arguments from an Eclipse 2.0 Ant buildfile configuration to a current launch configuration.
	 */
	@Test
	public void test20AntMigration() throws CoreException {
		Map<String, String> argumentMap = get20AntArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull(config, "Migration failed"); //$NON-NLS-1$

		assertEquals(IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier(), "Wrong configuration type"); //$NON-NLS-1$
		assertEquals("ant tool", config.getName()); //$NON-NLS-1$
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		String[] targets = AntLaunchingUtil.getTargetNames(config);
		assertNotNull(targets, "No targets found"); //$NON-NLS-1$
		assertThat(targets).containsExactly("target1", "target2"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("arg  ", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("working dir", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, "")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests migration of arguments from an Eclipse 2.0 Ant buildfile configuration to a current launch configuration.
	 */
	@Test
	public void test20ProgramMigration() throws CoreException {
		Map<String, String> argumentMap = get20ProgramArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertEquals(IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE,
				config.getType().getIdentifier(), "Wrong configuration type"); //$NON-NLS-1$
		assertEquals("program tool", config.getName()); //$NON-NLS-1$
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("arg ${ant_target:target1} ${ant_target:target2}", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("working dir", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, "")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests migration of arguments from an Eclipse 2.1 Ant buildfile configuration to a current launch configuration.
	 */
	@Test
	public void test21AntMigration() throws CoreException {
		Map<String, String> argumentMap = get21AntArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull(config, "Migration failed"); //$NON-NLS-1$

		assertEquals(IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier(),
				"Wrong config type"); //$NON-NLS-1$
		assertEquals("ant config", config.getName()); //$NON-NLS-1$
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("working directory", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_PROMPT_FOR_ARGUMENTS, false));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("arg1 arg2", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		String[] targets = AntLaunchingUtil.getTargetNames(config);
		assertThat(targets).containsExactly("target1", "target2"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests migration of arguments from an Eclipse 2.1 program configuration to a current launch configuration.
	 */
	@Test
	public void test21ProgramMigration() throws CoreException {
		Map<String, String> argumentMap = get21ProgramArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull(config, "Migration failed"); //$NON-NLS-1$

		assertEquals(IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE,
				config.getType().getIdentifier(), "Wrong config type"); //$NON-NLS-1$
		assertEquals("program config", config.getName()); //$NON-NLS-1$
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("working directory", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_PROMPT_FOR_ARGUMENTS, false));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(true, config.getAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("arg1 arg2", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
