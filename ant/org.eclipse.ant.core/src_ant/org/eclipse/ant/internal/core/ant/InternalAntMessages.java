/**********************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.core.ant;

import org.eclipse.osgi.util.NLS;

public class InternalAntMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.core.ant.InternalAntMessages";//$NON-NLS-1$

	public static String InternalAntRunner_2;

	public static String InternalAntRunner_3;

	public static String InternalAntRunner_Build_file___0__1;
	public static String InternalAntRunner_Arguments___0__2;
	public static String InternalAntRunner_Unable_to_instantiate_logger___0__6;
	public static String InternalAntRunner_Could_not_load_the_version_information___0__9;
	public static String InternalAntRunner_Could_not_load_the_version_information__10;
	public static String InternalAntRunner_Using__0__file_as_build_log__1;
	public static String InternalAntRunner_Could_not_write_to_the_specified_log_file___0___Make_sure_the_path_exists_and_you_have_write_permissions__2;
	public static String InternalAntRunner_BUILD_SUCCESSFUL_1;
	public static String InternalAntRunner_Unknown_argument___0__2;
	public static String InternalAntRunner_Buildfile___0__does_not_exist___1;
	public static String InternalAntRunner__0__which_was_specified_to_be_a_build_listener_is_not_an_instance_of_org_apache_tools_ant_BuildListener__1;
	public static String InternalAntRunner__0__which_was_specified_to_perform_logging_is_not_an_instance_of_org_apache_tools_ant_BuildLogger__2;
	public static String InternalAntRunner_You_must_specify_a_classname_when_using_the__listener_argument_1;
	public static String InternalAntRunner_You_must_specify_a_classname_when_using_the__logger_argument_2;
	public static String InternalAntRunner_You_must_specify_a_log_file_when_using_the__log_argument_3;
	public static String InternalAntRunner_You_must_specify_a_buildfile_when_using_the__buildfile_argument_4;
	public static String InternalAntRunner_Class__0__not_found_for_task__1__1;
	public static String InternalAntRunner_Class__0__not_found_for_type__1__2;
	public static String InternalAntRunner_Only_one_logger_class_may_be_specified_1;
	public static String InternalAntRunner_You_must_specify_a_classname_when_using_the__inputhandler_argument_1;
	public static String InternalAntRunner_Only_one_input_handler_class_may_be_specified__2;
	public static String InternalAntRunner_You_must_specify_a_property_filename_when_using_the__propertyfile_argument_3;
	public static String InternalAntRunner_4;
	public static String InternalAntRunner_The_specified_input_handler_class__0__does_not_implement_the_org_apache_tools_ant_input_InputHandler_interface_5;
	public static String InternalAntRunner_Unable_to_instantiate_specified_input_handler_class__0_____1__6;
	public static String InternalAntRunner_Specifying_an_InputHandler_is_an_Ant_1_5___feature__Please_update_your_Ant_classpath_to_include_an_Ant_version_greater_than_this__2;
	public static String InternalAntRunner_The_diagnositics_options_is_an_Ant_1_5___feature__Please_update_your_Ant_classpath_to_include_an_Ant_version_greater_than_this__4;
	public static String InternalAntRunner_Specifying_property_files_is_a_Ant_1_5___feature__Please_update_your_Ant_classpath__6;
	public static String InternalAntRunner_Default_target__0__1__2__does_not_exist_in_this_project_1;
	public static String InternalAntRunner_ANT_HOME_must_be_set_to_use_Ant_diagnostics_2;
	public static String InternalAntRunner_Buildfile___0__is_not_a_file_1;
	public static String InternalAntRunner__find_not_supported;

	public static String InternalAntRunner_0;
	public static String InternalAntRunner_Error_setting_Ant_task;
	public static String InternalAntRunner_Missing_Class;
	public static String InternalAntRunner_157;
	
	public static String ProgressBuildListener_Build_cancelled__5;

	public static String InternalProject_0;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, InternalAntMessages.class);
	}
}