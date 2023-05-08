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
 *  IBM - Initial API and implementation
 *  Anton Kosyakov (Itemis AG) - Bug 438621 - [step filtering] Provide an extension point to enhance methods step filtering.
 *  Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;

import org.eclipse.osgi.util.NLS;

public class DebugCoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.core.DebugCoreMessages";//$NON-NLS-1$

	public static String Breakpoint_no_associated_marker;
	public static String BreakpointManager_Missing_breakpoint_definition;
	public static String BreakpointManager_Missing_model_identifier;
	public static String DebugEvent_illegal_detail;
	public static String DebugEvent_illegal_kind;
	public static String DebugPlugin_0;
	public static String DebugPlugin_1;
	public static String DebugPlugin_2;
	public static String DebugPlugin_3;
	public static String DebugPlugin_4;
	public static String DebugPlugin_5;
	public static String DebugPlugin_6;
	public static String DebugPlugin_7;
	public static String DebugPlugin_8;
	public static String DebugPlugin_Eclipse_runtime_does_not_support_working_directory_2;
	public static String EnvironmentVariableResolver_0;
	public static String GroupLaunchConfigurationDelegate_waiting;
	public static String GroupLaunchConfigurationDelegate_Delay;
	public static String GroupLaunchConfigurationDelegate_Delaying;
	public static String GroupLaunchConfigurationDelegate_Launching;
	public static String GroupLaunchConfigurationDelegate_mode_debug;
	public static String GroupLaunchConfigurationDelegate_mode_inherit;
	public static String GroupLaunchConfigurationDelegate_mode_profile;
	public static String GroupLaunchConfigurationDelegate_mode_run;
	public static String GroupLaunchConfigurationDelegate_None;
	public static String GroupLaunchConfigurationDelegate_Wait_until_terminated;
	public static String GroupLaunchConfigurationDelegate_Waiting_for_termination;
	public static String GroupLaunchElement_inherit_launch_mode_label;
	public static String GroupLaunchElement_outputRegexp;
	public static String LaunchConfiguration_0;
	public static String LaunchConfiguration_11;
	public static String LaunchConfiguration_13;
	public static String LaunchConfiguration_14;
	public static String LaunchConfiguration_16;
	public static String LaunchConfiguration_17;
	public static String LaunchConfiguration_18;
	public static String LaunchConfiguration_19;
	public static String LaunchConfiguration_2;
	public static String LaunchConfiguration_3;
	public static String LaunchConfiguration_4;
	public static String LaunchConfiguration_5;
	public static String LaunchConfiguration_6;
	public static String LaunchConfiguration_7;
	public static String LaunchConfiguration_8;
	public static String LaunchConfiguration_9;
	public static String LaunchConfigurationInfo_35;
	public static String LaunchConfigurationType_2;
	public static String LaunchConfigurationType_7;
	public static String LaunchConfigurationWorkingCopy_0;
	public static String LaunchConfigurationWorkingCopy_1;
	public static String LaunchConfigurationWorkingCopy_2;
	public static String LaunchConfigurationWorkingCopy_3;
	public static String LaunchConfigurationWorkingCopy_6;
	public static String LaunchConfigurationWorkingCopy_7;
	public static String LaunchConfigurationWorkingCopy_8;
	public static String LaunchDelegate_0;
	public static String LaunchDelegate_1;
	public static String LaunchDelegate_2;
	public static String LaunchDelegate_3;
	public static String LaunchManager_26;
	public static String LaunchManager_27;
	public static String LaunchManager_28;
	public static String LaunchManager_29;
	public static String LaunchManager_31;
	public static String LaunchMode_1;
	public static String LogicalStructureProvider_0;
	public static String LogicalStructureProvider_1;
	public static String LogicalStructureType_1;
	public static String SystemPropertyResolver_0;
	public static String Launch_terminate_failed;
	public static String LaunchConfiguration_Failed_to_delete_launch_configuration__1;
	public static String LaunchConfigurationDelegate_6;
	public static String LaunchConfigurationDelegate_7;
	public static String LaunchConfigurationDelegate_scoped_incremental_build;
	public static String LaunchConfigurationInfo_Attribute__0__is_not_of_type_boolean__3;
	public static String LaunchConfigurationInfo_Attribute__0__is_not_of_type_int__2;
	public static String LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_lang_String__1;
	public static String LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_util_List__1;
	public static String LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_util_Map__1;
	public static String LaunchConfigurationInfo_Invalid_launch_configuration_XML__10;
	public static String LaunchConfigurationInfo_missing_type;
	public static String LaunchConfigurationInfo_36;
	public static String LaunchConfigurationWorkingCopy__0__occurred_generating_launch_configuration_XML__1;
	public static String LaunchConfigurationWorkingCopy_4;
	public static String LaunchConfigurationWorkingCopy_Specified_container_for_launch_configuration_does_not_exist_2;
	public static String LaunchConfigurationWorkingCopy_5;
	public static String LaunchManager__0__occurred_while_reading_launch_configuration_file__1___1;
	public static String LaunchManager_Invalid_launch_configuration_index__18;
	public static String LaunchManager_does_not_exist;
	public static String LaunchManager_does_not_exist_no_store_found;
	public static String LaunchManager_invalid_config_name;
	public static String LaunchManager_invalid_config_name_char;
	public static String LaunchManager_Source_locator_does_not_exist___0__13;
	public static String LogicalStructureType_0;
	public static String ProcessMonitorJob_0;
	public static String RuntimeProcess_terminate_failed;
	public static String RuntimeProcess_Exit_value_not_available_until_process_terminates__1;
	public static String WatchExpression_0;
	public static String NullStreamsProxy_0;
	public static String RefreshingResources;
	public static String RefreshingResourcesError;
	public static String RefreshUtil_0;
	public static String RefreshUtil_1;
	public static String StepFilter_0;
	public static String StepFilter_1;
	public static String ErrorAccessingProblemMarker;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, DebugCoreMessages.class);
	}
	public static String LaunchMode_0;
}
