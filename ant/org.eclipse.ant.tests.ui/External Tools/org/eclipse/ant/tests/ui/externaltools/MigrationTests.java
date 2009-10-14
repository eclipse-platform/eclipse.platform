/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.externaltools;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.registry.ExternalToolMigration;

/**
 * Tests migration of Ant and External Tool configurations from old
 * formats to the current format.
 */
public class MigrationTests extends TestCase {
	
	/**
	 * Tests migration of arguments from an Eclipse 2.0 Ant buildfile
	 * configuration to a current launch configuration.
	 * 
	 * @throws CoreException
	 */
	public void test20AntMigration() throws CoreException {
		Map argumentMap= get20AntArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull("Migration failed", config);
		
		assertEquals("Wrong configuration type", IAntLaunchConfigurationConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier());
		assertEquals("ant tool", config.getName());
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, ""));
		String[] targets= AntUtil.getTargetNames(config);
		assertNotNull("No targets found", targets);
		assertEquals("Wrong number of targets", 2, targets.length);
		assertEquals("target1", targets[0]);
		assertEquals("target2", targets[1]);
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, ""));
		assertEquals("arg  ", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""));
		assertEquals("working dir", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""));
	}
	
	/**
	 * Returns a map of arguments for an Ant buildfile using
	 * Eclipse 2.0 arguments.
	 * 
	 * @return a map of 2.0 arguments for an Ant buildfile.
	 */
	private Map get20AntArgumentMap() {
		HashMap arguments= new HashMap();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.0");
		arguments.put(ExternalToolMigration.TAG_TOOL_TYPE, "org.eclipse.ui.externaltools.type.ant");
		arguments.put(ExternalToolMigration.TAG_TOOL_NAME, "ant tool");
		arguments.put(ExternalToolMigration.TAG_TOOL_LOCATION, "location");
		arguments.put(ExternalToolMigration.TAG_TOOL_REFRESH, "refresh scope");
		arguments.put(ExternalToolMigration.TAG_TOOL_ARGUMENTS, "arg ${ant_target:target1} ${ant_target:target2}");
		arguments.put(ExternalToolMigration.TAG_TOOL_SHOW_LOG, "true");
		arguments.put(ExternalToolMigration.TAG_TOOL_BLOCK, "false");
		arguments.put(ExternalToolMigration.TAG_TOOL_BUILD_TYPES, "build kinds");
		arguments.put(ExternalToolMigration.TAG_TOOL_DIRECTORY, "working dir");
		return arguments;
	}

	/**
	 * Tests migration of arguments from an Eclipse 2.0 Ant buildfile
	 * configuration to a current launch configuration.
	 * 
	 * @throws CoreException
	 */
	public void test20ProgramMigration() throws CoreException {
		Map argumentMap= get20ProgramArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertEquals("Wrong configuration type", IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier());
		assertEquals("program tool", config.getName());
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, ""));
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, ""));
		assertEquals("arg ${ant_target:target1} ${ant_target:target2}", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""));
		assertEquals("working dir", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""));
	}
	
	/**
	 * Returns a map of arguments for executing a program
	 * using Eclipse 2.0 arguments.
	 * 
	 * @return a map of 2.0 arguments for a program
	 */
	private Map get20ProgramArgumentMap() {
		HashMap arguments= new HashMap();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.0");
		arguments.put(ExternalToolMigration.TAG_TOOL_TYPE, "org.eclipse.ui.externaltools.type.program");
		arguments.put(ExternalToolMigration.TAG_TOOL_NAME, "program tool");
		arguments.put(ExternalToolMigration.TAG_TOOL_LOCATION, "location");
		arguments.put(ExternalToolMigration.TAG_TOOL_REFRESH, "refresh scope");
		arguments.put(ExternalToolMigration.TAG_TOOL_ARGUMENTS, "arg ${ant_target:target1} ${ant_target:target2}");
		arguments.put(ExternalToolMigration.TAG_TOOL_SHOW_LOG, "true");
		arguments.put(ExternalToolMigration.TAG_TOOL_BLOCK, "false");
		arguments.put(ExternalToolMigration.TAG_TOOL_BUILD_TYPES, "build kinds");
		arguments.put(ExternalToolMigration.TAG_TOOL_DIRECTORY, "working dir");
		return arguments;
	}
	
	/**
	 * Tests migration of arguments from an Eclipse 2.1 Ant buildfile
	 * configuration to a current launch configuration.
	 * 
	 * @throws CoreException
	 */
	public void test21AntMigration() throws CoreException {
		Map argumentMap= get21AntArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull("Migration failed", config);
		
		assertEquals("Wrong config type", IAntLaunchConfigurationConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier());
		assertEquals("ant config", config.getName());
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""));
		assertEquals("working directory", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""));
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_PROMPT_FOR_ARGUMENTS, false));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, ""));
		assertEquals(true, config.getAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, ""));
		assertEquals("arg1 arg2", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""));
		String[] targets= AntUtil.getTargetNames(config);
		assertEquals("Wrong number of targets", 2, targets.length);
		assertEquals("target1", targets[0]);
		assertEquals("target2", targets[1]);
	}
	
	/**
	 * Returns a map of arguments for executing an Ant
	 * buildfile using Eclipse 2.1 arguments.
	 * 
	 * @return a map of 2.1 arguments for an Ant buildfile
	 */
	private Map get21AntArgumentMap() {
		HashMap arguments= new HashMap();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.1");
		arguments.put(ExternalToolMigration.TAG_NAME, "ant config");
		arguments.put(ExternalToolMigration.TAG_TYPE, ExternalToolMigration.TOOL_TYPE_ANT_BUILD);
		arguments.put(ExternalToolMigration.TAG_LOCATION, "location");
		arguments.put(ExternalToolMigration.TAG_WORK_DIR, "working directory");
		arguments.put(ExternalToolMigration.TAG_CAPTURE_OUTPUT, "true");
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true");
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true");
		arguments.put(ExternalToolMigration.TAG_RUN_BKGRND, "true");
		arguments.put(ExternalToolMigration.TAG_PROMPT_ARGS, "true");
		arguments.put(ExternalToolMigration.TAG_REFRESH_SCOPE, "refresh scope");
		arguments.put(ExternalToolMigration.TAG_REFRESH_RECURSIVE, "true");
		arguments.put(ExternalToolMigration.TAG_RUN_BUILD_KINDS, "build kinds");
		arguments.put(ExternalToolMigration.TAG_ARGS, "arg1 arg2");
		arguments.put(ExternalToolMigration.TAG_EXTRA_ATTR, ExternalToolMigration.RUN_TARGETS_ATTRIBUTE + "=target1,target2");
		return arguments;
	}
	
	/**
	 * Tests migration of arguments from an Eclipse 2.1 program
	 * configuration to a current launch configuration.
	 * 
	 * @throws CoreException
	 */
	public void test21ProgramMigration() throws CoreException {
		Map argumentMap= get21ProgramArgumentMap();
		ILaunchConfigurationWorkingCopy config = ExternalToolMigration.configFromArgumentMap(argumentMap);
		assertNotNull("Migration failed", config);
		
		assertEquals("Wrong config type", IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE, config.getType().getIdentifier());
		assertEquals("program config", config.getName());
		assertEquals("location", config.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""));
		assertEquals("working directory", config.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""));
		assertEquals(true, config.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false));
		assertEquals(true, config.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false));
		assertEquals(true, config.getAttribute(IExternalToolConstants.ATTR_PROMPT_FOR_ARGUMENTS, false));
		assertEquals("refresh scope", config.getAttribute(RefreshTab.ATTR_REFRESH_SCOPE, ""));
		assertEquals(true, config.getAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, false));
		assertEquals("build kinds", config.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, ""));
		assertEquals("arg1 arg2", config.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""));
	}
	
	/**
	 * Returns a map of arguments for executing a program
	 * buildfile using Eclipse 2.1 arguments.
	 * 
	 * @return a map of 2.1 arguments for a program
	 */
	private Map get21ProgramArgumentMap() {
		HashMap arguments= new HashMap();
		arguments.put(ExternalToolMigration.TAG_VERSION, "2.1");
		arguments.put(ExternalToolMigration.TAG_NAME, "program config");
		arguments.put(ExternalToolMigration.TAG_TYPE, IExternalToolConstants.TOOL_TYPE_PROGRAM);
		arguments.put(ExternalToolMigration.TAG_LOCATION, "location");
		arguments.put(ExternalToolMigration.TAG_WORK_DIR, "working directory");
		arguments.put(ExternalToolMigration.TAG_CAPTURE_OUTPUT, "true");
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true");
		arguments.put(ExternalToolMigration.TAG_SHOW_CONSOLE, "true");
		arguments.put(ExternalToolMigration.TAG_RUN_BKGRND, "true");
		arguments.put(ExternalToolMigration.TAG_PROMPT_ARGS, "true");
		arguments.put(ExternalToolMigration.TAG_REFRESH_SCOPE, "refresh scope");
		arguments.put(ExternalToolMigration.TAG_REFRESH_RECURSIVE, "true");
		arguments.put(ExternalToolMigration.TAG_RUN_BUILD_KINDS, "build kinds");
		arguments.put(ExternalToolMigration.TAG_ARGS, "arg1 arg2");
		return arguments;
	}
	
}
