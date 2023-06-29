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
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *******************************************************************************/

package org.eclipse.core.externaltools.internal;
/**
 * Defines the constants available for client use.
 * <p>
 * This interface is not intended to be extended or implemented by clients.
 * </p>
 */
public interface IExternalToolConstants {

	/**
	 * Constant for the empty {@link String}
	 *
	 * @since org.eclipse.core.externaltools 1.0.100
	 */
	String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Plug-in identifier for external tools UI (value <code>org.eclipse.ui.externaltools</code>).
	 */
	String UI_PLUGIN_ID = "org.eclipse.ui.externaltools"; //$NON-NLS-1$;

	/**
	 * Plug-in identifier for external tools core (value <code>org.eclipse.core.externaltools</code>).
	 */
	String PLUGIN_ID = "org.eclipse.core.externaltools"; //$NON-NLS-1$;

	// ------- Refresh Variables -------
	/**
	 * Variable that expands to the workspace root object (value <code>workspace</code>).
	 */
	String VAR_WORKSPACE = "workspace"; //$NON-NLS-1$
	/**
	 * Variable that expands to the project resource (value <code>project</code>).
	 */
	String VAR_PROJECT = "project"; //$NON-NLS-1$
	/**
	 * Variable that expands to the container resource (value <code>container</code>).
	 */
	String VAR_CONTAINER = "container"; //$NON-NLS-1$
	/**
	 * Variable that expands to a resource (value <code>resource</code>).
	 */
	String VAR_RESOURCE = "resource"; //$NON-NLS-1$
	/**
	 * Variable that expands to the working set object (value <code>working_set</code>).
	 */
	String VAR_WORKING_SET = "working_set"; //$NON-NLS-1$
	// ------- Tool Types -------
	/**
	 * External tool type for programs such as executables, batch files,
	 * shell scripts, etc (value <code>programType</code>).
	 */
	String TOOL_TYPE_PROGRAM = "programType"; //$NON-NLS-1$;

	// ------- Build Types -------
	/**
	 * Build type indicating an incremental project build request for
	 * the external tool running as a builder (value <code>incremental</code>).
	 */
	String BUILD_TYPE_INCREMENTAL = "incremental"; //$NON-NLS-1$

	/**
	 * Build type indicating a full project build request for
	 * the external tool running as a builder (value <code>full</code>).
	 */
	String BUILD_TYPE_FULL = "full"; //$NON-NLS-1$

	/**
	 * Build type indicating an automatic project build request for
	 * the external tool running as a builder (value <code>auto</code>).
	 */
	String BUILD_TYPE_AUTO = "auto"; //$NON-NLS-1$

	/**
	 * Build type indicating a clean project build request for
	 * the external tool running as a builder (value <code>clean</code>).
	 */
	String BUILD_TYPE_CLEAN = "clean"; //$NON-NLS-1$

	/**
	 * Build type indicating no project build request for
	 * the external tool running as a builder (value <code>none</code>).
	 */
	String BUILD_TYPE_NONE = "none"; //$NON-NLS-1$

	// ------- Launch configuration types --------
	/**
	 * Program launch configuration type identifier.
	 */
	String ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ui.externaltools.ProgramLaunchConfigurationType"; //$NON-NLS-1$

	/**
	 * Program builder launch configuration type identifier. Program project
	 * builders are of this type.
	 */
	String ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ui.externaltools.ProgramBuilderLaunchConfigurationType"; //$NON-NLS-1$

	// ------- Launch configuration category --------
	/**
	 * Identifier for external tools launch configuration category. Launch
	 * configuration types for external tools that appear in the external tools
	 * launch configuration dialog should belong to this category.
	 */
	String ID_EXTERNAL_TOOLS_LAUNCH_CATEGORY = "org.eclipse.ui.externaltools"; //$NON-NLS-1$
	/**
	 * Identifier for external tools launch configuration builders category.
	 * Launch configuration types that can be added as project builders should
	 * belong to this category.
	 */
	String ID_EXTERNAL_TOOLS_BUILDER_LAUNCH_CATEGORY = "org.eclipse.ui.externaltools.builder"; //$NON-NLS-1$

	// ------- Common External Tool Launch Configuration Attributes -------

	/**
	 * Boolean attribute indicating if external tool output should be captured.
	 * Default value is <code>false</code>.
	 * @deprecated since 3.1 Replaced by <code>org.eclipse.debug.core.DebugPlugin.ATTR_CAPTURE_OUTPUT</code>
	 */
	@Deprecated String ATTR_CAPTURE_OUTPUT = UI_PLUGIN_ID + ".ATTR_CAPTURE_OUTPUT"; //$NON-NLS-1$
	/**
	 * String attribute identifying the location of an external. Default value
	 * is <code>null</code>. Encoding is tool specific.
	 */
	String ATTR_LOCATION = UI_PLUGIN_ID + ".ATTR_LOCATION"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating if the user should be prompted for
	 * arguments before running a tool. Default value is <code>false</code>.
	 * THIS ATTRIBUTE IS NOT USED.
	 */
	String ATTR_PROMPT_FOR_ARGUMENTS = UI_PLUGIN_ID + ".ATTR_PROMPT_FOR_ARGUMENTS"; //$NON-NLS-1$

	/**
	 * String attribute identifying the scope of resources that should trigger an
	 * external tool to run. Default value is <code>null</code>
	 * indicating that the builder will be triggered for all changes.
	 */
	String ATTR_BUILDER_SCOPE = UI_PLUGIN_ID + ".ATTR_BUILD_SCOPE"; //$NON-NLS-1$

	/**
	 * String attribute containing an array of build kinds for which an
	 * external tool builder should be run.
	 */
	String ATTR_RUN_BUILD_KINDS = UI_PLUGIN_ID + ".ATTR_RUN_BUILD_KINDS"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating if the console should be shown on external
	 * tool output. Default value is <code>false</code>.
	 */
	String ATTR_SHOW_CONSOLE = UI_PLUGIN_ID + ".ATTR_SHOW_CONSOLE"; //$NON-NLS-1$

	/**
	 * String attribute containing the arguments that should be passed to the
	 * tool. Default value is <code>null</code>, and encoding is tool specific.
	 */
	String ATTR_TOOL_ARGUMENTS = UI_PLUGIN_ID + ".ATTR_TOOL_ARGUMENTS"; //$NON-NLS-1$

	/**
	 * String attribute identifying the working directory of an external tool.
	 * Default value is <code>null</code>, which indicates a default working
	 * directory, which is tool specific.
	 */
	String ATTR_WORKING_DIRECTORY = UI_PLUGIN_ID + ".ATTR_WORKING_DIRECTORY"; //$NON-NLS-1$

	/**
	 * String attribute identifying whether an external tool builder configuration
	 * is enabled. The default value is <code>true</code>, which indicates
	 * that the configuration will be executed as appropriate by the builder.
	 */
	String ATTR_BUILDER_ENABLED = UI_PLUGIN_ID + ".ATTR_BUILDER_ENABLED"; //$NON-NLS-1$

	/**
	 * Boolean attribute identifying whether an external tool launcher should execute
	 * synchronously (value <code>false</code>) or asynchronously (value <code>true</code>).
	 * Default value is
	 */
	String ATTR_LAUNCH_IN_BACKGROUND = "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND"; //$NON-NLS-1$

	/**
	 * Status code indicating an unexpected internal error.
	 */
	int ERR_INTERNAL_ERROR = 150;

	/**
	 * String attribute identifying a non-external tool builder launch configuration that is disabled
	 * The value is the name of the disabled builder.
	 */
	String ATTR_DISABLED_BUILDER = UI_PLUGIN_ID + ".ATTR_DISABLED_BUILDER";		 //$NON-NLS-1$

	/**
	 * boolean attribute identifying that an external tool builder has been configured for triggering
	 * using the <code>ICommand.setBuilding(int)</code> mechanism
	 * @since 3.1
	 */
	String ATTR_TRIGGERS_CONFIGURED = UI_PLUGIN_ID + ".ATTR_TRIGGERS_CONFIGURED";		 //$NON-NLS-1$

	/**
	 * String attribute identifying the build scope for a launch configuration.
	 * <code>null</code> indicates the default workspace build.
	 */
	String ATTR_BUILD_SCOPE = UI_PLUGIN_ID + ".ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE"; //$NON-NLS-1$

	/**
	 * Attribute identifier specifying whether referenced projects should be
	 * considered when computing the projects to build. Default value is
	 * <code>true</code>.
	 */
	String ATTR_INCLUDE_REFERENCED_PROJECTS = UI_PLUGIN_ID + ".ATTR_INCLUDE_REFERENCED_PROJECTS"; //$NON-NLS-1$
}
