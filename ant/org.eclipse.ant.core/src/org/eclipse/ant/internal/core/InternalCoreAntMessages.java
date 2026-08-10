/**********************************************************************
.
. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.core;

import org.eclipse.osgi.util.NLS;

public class InternalCoreAntMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.core.InternalCoreAntMessages";//$NON-NLS-1$

	public static String AntCorePreferences_Malformed_URL__1;
	public static String AntCorePreferences_Library_not_specified_for___0__4;
	public static String AntCorePreferences_No_library_for_task;
	public static String AntCorePreferences_No_library_for_type;
	public static String AntCorePreferences_8;
	public static String AntCorePreferences_6;
	public static String AntCorePreferences_0;
	public static String AntCorePreferences_1;

	public static String AntRunner_Could_not_find_one_or_more_classes__Please_check_the_Ant_classpath__1;
	public static String AntRunner_Could_not_find_one_or_more_classes__Please_check_the_Ant_classpath__2;
	public static String AntRunner_Build_Failed__3;
	public static String AntRunner_Already_in_progess;


	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, InternalCoreAntMessages.class);
	}
}