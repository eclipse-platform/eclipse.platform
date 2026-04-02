/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.core.ant;

import org.eclipse.osgi.util.NLS;

public class InternalAntMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.core.ant.InternalAntMessages";//$NON-NLS-1$

	public static String InternalAntRunner_Build_file;
	public static String InternalAntRunner_Arguments;
	public static String InternalAntRunner_Unable_to_instantiate_logger;
	public static String InternalAntRunner_Using_file_as_build_log;
	public static String InternalAntRunner_Could_not_write_to_log_file;
	public static String InternalAntRunner_BUILD_SUCCESSFUL_1;
	public static String InternalAntRunner_Unknown_argument;
	public static String InternalAntRunner_Buildfile_does_not_exist;
	public static String InternalAntRunner_not_an_instance_of_apache_ant_BuildListener;
	public static String InternalAntRunner_not_an_instance_of_apache_ant_BuildLogger;
	public static String InternalAntRunner_specify_a_classname_using_the_listener_argument;
	public static String InternalAntRunner_specify_a_classname_using_the_logger_argument;
	public static String InternalAntRunner_specify_a_log_file_using_the_log_argument;
	public static String InternalAntRunner_specify_a_buildfile_using_the_buildfile_argument;
	public static String InternalAntRunner_Only_one_logger_class_may_be_specified;
	public static String InternalAntRunner_specify_a_classname_the_inputhandler_argument;
	public static String InternalAntRunner_Only_one_input_handler_class_may_be_specified;
	public static String InternalAntRunner_specify_a_property_filename_when_using_propertyfile_argument;
	public static String InternalAntRunner_could_not_load_property_file;
	public static String InternalAntRunner_handler_does_not_implement_InputHandler5;
	public static String InternalAntRunner_Unable_to_instantiate_input_handler_class;
	public static String InternalAntRunner_Default_target_does_not_exist;
	public static String InternalAntRunner_anthome_must_be_set_to_use_ant_diagnostics;
	public static String InternalAntRunner_Buildfile_is_not_a_file;
	public static String InternalAntRunner_find_not_supported;
	public static String InternalAntRunner_157;

	public static String InternalAntRunner_unknown_target;

	public static String InternalAntRunner_no_known_target;

	public static String ProgressBuildListener_Build_cancelled;

	public static String InternalAntRunner_SecurityManagerError;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, InternalAntMessages.class);
	}
}