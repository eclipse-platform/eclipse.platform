/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.core.externaltools.internal.registry;


import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;


/**
 * Responsible reading an old external tool format and creating
 * and migrating it to create a new external tool.
 */
public final class ExternalToolMigration {

	private static final ILog LOG = Platform.getLog(ExternalToolMigration.class);

	/**
	 * Structure to represent a variable definition within a
	 * source string.
	 */
	public static final class VariableDefinition {
		/**
		 * Index in the source text where the variable started
		 * or <code>-1</code> if no valid variable start tag
		 * identifier found.
		 */
		public int start = -1;

		/**
		 * Index in the source text of the character following
		 * the end of the variable or <code>-1</code> if no
		 * valid variable end tag found.
		 */
		public int end = -1;

		/**
		 * The variable's name found in the source text, or
		 * <code>null</code> if no valid variable found.
		 */
		public String name = null;

		/**
		 * The variable's argument found in the source text, or
		 * <code>null</code> if no valid variable found or if
		 * the variable did not specify an argument
		 */
		public String argument = null;

		/**
		 * Create an initialized variable definition.
		 */
		private VariableDefinition() {
			super();
		}
	}

	/**
	 * Variable tag indentifiers
	 */
	private static final String VAR_TAG_START = "${"; //$NON-NLS-1$
	private static final String VAR_TAG_END = "}"; //$NON-NLS-1$
	private static final String VAR_TAG_SEP = ":"; //$NON-NLS-1$

	/**
	 * External tool type for Ant build files (value <code>antBuildType</code>).
	 */
	public static final String TOOL_TYPE_ANT_BUILD = "antBuildType"; //$NON-NLS-1$;
	/**
	 * Ant builder launch configuration type identifier. Ant project builders
	 * are of this type.
	 */
	public static final String ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ant.AntBuilderLaunchConfigurationType"; //$NON-NLS-1$

	public static final String RUN_TARGETS_ATTRIBUTE = TOOL_TYPE_ANT_BUILD + ".runTargets"; //$NON-NLS-1$;

	/**
	* String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * NOTE: This value is copied here from org.eclipse.ant.ui.internal.IAntLaunchConfigurationConstants.
	 * 		Ant no longer resides in External Tools and this plug-in. This value is kept here only
	 * 		for migration.
	 */
	public static final String ATTR_ANT_TARGETS = IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_ANT_TARGETS"; //$NON-NLS-1$

	/*
	 * 2.0 External Tool Tags
	 */
	public static final String TAG_TOOL_TYPE = "!{tool_type}"; //$NON-NLS-1$
	public static final String TAG_TOOL_NAME = "!{tool_name}"; //$NON-NLS-1$
	public static final String TAG_TOOL_LOCATION = "!{tool_loc}"; //$NON-NLS-1$
	public static final String TAG_TOOL_ARGUMENTS = "!{tool_args}"; //$NON-NLS-1$
	public static final String TAG_TOOL_DIRECTORY = "!{tool_dir}"; //$NON-NLS-1$
	public static final String TAG_TOOL_REFRESH = "!{tool_refresh}"; //$NON-NLS-1$
	public static final String TAG_TOOL_SHOW_LOG = "!{tool_show_log}"; //$NON-NLS-1$
	public static final String TAG_TOOL_BUILD_TYPES = "!{tool_build_types}"; //$NON-NLS-1$
	public static final String TAG_TOOL_BLOCK = "!{tool_block}"; //$NON-NLS-1$

	// Known kind of tools
	private static final String TOOL_TYPE_ANT = "org.eclipse.ui.externaltools.type.ant"; //$NON-NLS-1$
	private static final String TOOL_TYPE_PROGRAM = "org.eclipse.ui.externaltools.type.program"; //$NON-NLS-1$

	/*
	 * 2.1 External Tool Keys
	 */
	public static final String TAG_TYPE = "type"; //$NON-NLS-1$
	public static final String TAG_NAME = "name"; //$NON-NLS-1$
	public static final String TAG_LOCATION = "location"; //$NON-NLS-1$
	public static final String TAG_WORK_DIR = "workDirectory"; //$NON-NLS-1$
	public static final String TAG_CAPTURE_OUTPUT = "captureOutput"; //$NON-NLS-1$
	public static final String TAG_SHOW_CONSOLE = "showConsole"; //$NON-NLS-1$
	public static final String TAG_RUN_BKGRND = "runInBackground"; //$NON-NLS-1$
	public static final String TAG_PROMPT_ARGS = "promptForArguments"; //$NON-NLS-1$
	public static final String TAG_ARGS = "arguments"; //$NON-NLS-1$
	public static final String TAG_REFRESH_SCOPE = "refreshScope"; //$NON-NLS-1$
	public static final String TAG_REFRESH_RECURSIVE = "refreshRecursive"; //$NON-NLS-1$
	public static final String TAG_RUN_BUILD_KINDS = "runForBuildKinds"; //$NON-NLS-1$
	public static final String TAG_EXTRA_ATTR = "extraAttribute"; //$NON-NLS-1$
	public static final String TAG_VERSION = "version"; //$NON-NLS-1$

	private static final String EXTRA_ATTR_SEPARATOR = "="; //$NON-NLS-1$

	private static final String VERSION_21 = "2.1"; //$NON-NLS-1$;

	private static final String TRUE = "true"; //$NON-NLS-1$
	private static final String FALSE = "false"; //$NON-NLS-1$

	/**
	 * Allows no instances.
	 */
	private ExternalToolMigration() {
		super();
	}

	/**
	 * Returns a  launch configuration working copy from the argument map or
	 * <code>null</code> if the given map cannot be interpreted as a 2.0 or 2.1
	 * branch external tool. The returned working copy will be unsaved and its
	 * location will be set to the metadata area.
	 */
	public static ILaunchConfigurationWorkingCopy configFromArgumentMap(Map<String, String> args) {
		String version = args.get(TAG_VERSION);
		if (VERSION_21.equals(version)) {
			return configFrom21ArgumentMap(args);
		}
		return configFrom20ArgumentMap(args);
	}

	public static ILaunchConfigurationWorkingCopy configFrom21ArgumentMap(Map<String, String> commandArgs) {
		String name = commandArgs.get(TAG_NAME);
		String type = commandArgs.get(TAG_TYPE);

		ILaunchConfigurationWorkingCopy config = newConfig(type, name);
		if (config == null) {
			return null;
		}

		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, commandArgs.get(TAG_LOCATION));
		config.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, commandArgs.get(TAG_WORK_DIR));
		config.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, TRUE.equals(commandArgs.get(TAG_CAPTURE_OUTPUT)));
		config.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, TRUE.equals(commandArgs.get(TAG_SHOW_CONSOLE)));
		config.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, TRUE.equals(commandArgs.get(TAG_RUN_BKGRND)));
		config.setAttribute(IExternalToolConstants.ATTR_PROMPT_FOR_ARGUMENTS, TRUE.equals(commandArgs.get(TAG_PROMPT_ARGS)));
		config.setAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, commandArgs.get(TAG_REFRESH_SCOPE));
		config.setAttribute(RefreshUtil.ATTR_REFRESH_RECURSIVE, TRUE.equals(commandArgs.get(TAG_REFRESH_RECURSIVE)));

		config.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, commandArgs.get(TAG_RUN_BUILD_KINDS));

		String args = commandArgs.get(TAG_ARGS);
		if (args != null) {
			config.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, args);
		}

		String extraAttributes = commandArgs.get(TAG_EXTRA_ATTR);
		if (extraAttributes != null) {
			StringTokenizer tokenizer = new StringTokenizer(extraAttributes, EXTRA_ATTR_SEPARATOR);
			while (tokenizer.hasMoreTokens()) {
				String key = tokenizer.nextToken();
				if (!tokenizer.hasMoreTokens()) {
					break;
				}
				String value = tokenizer.nextToken();
				if (key.equals(RUN_TARGETS_ATTRIBUTE)) {
					// 2.1 implementation only defined 1 "extra attribute"
					config.setAttribute(ATTR_ANT_TARGETS, value);
				}
			}
		}
		return config;
	}

	/**
	 * Creates an external tool from the map.
	 */
	public static ILaunchConfigurationWorkingCopy configFrom20ArgumentMap(Map<String, String> args) {
		// Update the type...
		String type = args.get(TAG_TOOL_TYPE);
		if (TOOL_TYPE_ANT.equals(type)) {
			type = TOOL_TYPE_ANT_BUILD;
		} else if (TOOL_TYPE_PROGRAM.equals(type)){
			type = IExternalToolConstants.TOOL_TYPE_PROGRAM;
		} else {
			return null;
		}

		String name = args.get(TAG_TOOL_NAME);

		ILaunchConfigurationWorkingCopy config = newConfig(type, name);
		if (config == null) {
			return null;
		}

		// Update the location...
		String location = args.get(TAG_TOOL_LOCATION);
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, location);

		// Update the refresh scope...
		String refresh = args.get(TAG_TOOL_REFRESH);
		if (refresh != null) {
			VariableDefinition varDef = extractVariableDefinition(refresh, 0);
			if ("none".equals(varDef.name)) { //$NON-NLS-1$
				refresh = null;
			}
			config.setAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, refresh);
		}

		// Update the arguments
		String arguments = args.get(TAG_TOOL_ARGUMENTS);
		if (type.equals(TOOL_TYPE_ANT_BUILD)) {
			String targetNames = null;
			if (arguments != null) {
				int start = 0;
				ArrayList<String> targets = new ArrayList<>();
				StringBuilder buffer = new StringBuilder();
				VariableDefinition varDef = extractVariableDefinition(arguments, start);
				while (varDef.end != -1) {
					if ("ant_target".equals(varDef.name) && varDef.argument != null) { //$NON-NLS-1$
						targets.add(varDef.argument);
						buffer.append(arguments.substring(start, varDef.start));
					} else {
						buffer.append(arguments.substring(start, varDef.end));
					}
					start = varDef.end;
					varDef = extractVariableDefinition(arguments, start);
				}
				buffer.append(arguments.substring(start, arguments.length()));
				arguments = buffer.toString();

				buffer.setLength(0);
				for (String target : targets) {
					if (target != null && target.length() > 0) {
						buffer.append(target);
						buffer.append(","); //$NON-NLS-1$
					}
				}
				targetNames = buffer.toString();
			}
			if (targetNames != null && targetNames.length() > 0) {
				config.setAttribute(ATTR_ANT_TARGETS, targetNames);
			}
		}
		config.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);

		// Collect the rest of the information
		config.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, TRUE.equals(args.get(TAG_TOOL_SHOW_LOG)));
		config.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, TRUE.equals(args.get(TAG_TOOL_SHOW_LOG)));
		config.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, FALSE.equals(args.get(TAG_TOOL_BLOCK)));
		String buildKinds= args.get(TAG_TOOL_BUILD_TYPES);
		if (buildKinds != null) {
			buildKinds= buildKinds.replace(';', ','); // Replace the old separator with the new
		}
		config.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, buildKinds);
		config.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, args.get(TAG_TOOL_DIRECTORY));
		return config;
	}

	/**
	 * Returns a new working copy with the given external tool name and external
	 * tool type or <code>null</code> if no config could be created.
	 */
	private static ILaunchConfigurationWorkingCopy newConfig(String type, String name) {
		if (type == null || name == null) {
			return null;
		}
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType;
		if (TOOL_TYPE_ANT_BUILD.equals(type)) {
			configType = manager.getLaunchConfigurationType(ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE);
		} else if (IExternalToolConstants.TOOL_TYPE_PROGRAM.equals(type)) {
			configType = manager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE);
		} else {
			return null;
		}
		try {
			if (configType != null) {
				return configType.newInstance(null, name);
			}
		} catch (CoreException e) {
			LOG.log(Status.error("Error logged from Ant UI: ", e)); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Returns the tool name extracted from the given command argument map.
	 * Extraction is attempted using 2.0 and 2.1 external tool formats.
	 */
	public static String getNameFromCommandArgs(Map<String, String> commandArgs) {
		String name= commandArgs.get(TAG_NAME);
		if (name == null) {
			name= commandArgs.get(TAG_TOOL_NAME);
		}
		return name;
	}

	/**
	 * Migrate the old RUN_IN_BACKGROUND launch config attribute to the new
	 * LAUNCH_IN_BACKGROUND attribute provided by the debug ui plugin.
	 *
	 * @param config the config to migrate
	 * @return the migrated config
	 */
	public static ILaunchConfiguration migrateRunInBackground(ILaunchConfiguration config) {
		String noValueFlag= "NoValue"; //$NON-NLS-1$
		String attr= null;
		try {
			attr = config.getAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, noValueFlag);
		} catch (CoreException e) {
			// Exception will occur if the attribute is already set because the attribute is actually a boolean.
			// No migration necessary.
			return config;
		}
		if (noValueFlag.equals(attr)) {
			//the old constant
			String ATTR_RUN_IN_BACKGROUND= IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_RUN_IN_BACKGROUND"; //$NON-NLS-1$
			boolean runInBackground= false;
			try {
				runInBackground = config.getAttribute(ATTR_RUN_IN_BACKGROUND, runInBackground);
			} catch (CoreException e) {
				LOG.log(Status.error(ExternalToolsMigrationMessages.ExternalToolMigration_37, e));
			}
			try {
				ILaunchConfigurationWorkingCopy workingCopy= config.getWorkingCopy();
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, runInBackground);
				return workingCopy.doSave();
			} catch (CoreException e) {
				LOG.log(Status.error(ExternalToolsMigrationMessages.ExternalToolMigration_38, e));
			}
		}
		return config;
	}

	/**
	 * Extracts a variable name and argument from the given string.
	 *
	 * @param text the source text to parse for a variable tag
	 * @param start the index in the string to start the search
	 * @return the variable definition
	 */
	public static VariableDefinition extractVariableDefinition(String text, int start) {
		VariableDefinition varDef = new VariableDefinition();
		int strt = start;
		varDef.start = text.indexOf(VAR_TAG_START, strt);
		if (varDef.start < 0){
			return varDef;
		}
		strt = varDef.start + VAR_TAG_START.length();

		int end = text.indexOf(VAR_TAG_END, strt);
		if (end < 0) {
			return varDef;
		}
		varDef.end = end + VAR_TAG_END.length();
		if (end == strt) {
			return varDef;
		}

		int mid = text.indexOf(VAR_TAG_SEP, strt);
		if (mid < 0 || mid > end) {
			varDef.name = text.substring(strt, end);
		} else {
			if (mid > strt) {
				varDef.name = text.substring(strt, mid);
			}
			mid = mid + VAR_TAG_SEP.length();
			if (mid < end) {
				varDef.argument = text.substring(mid, end);
			}
		}

		return varDef;
	}
}
