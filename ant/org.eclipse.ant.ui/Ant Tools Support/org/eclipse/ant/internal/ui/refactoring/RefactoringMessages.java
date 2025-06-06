/**********************************************************************
.
.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.ui.refactoring;

import org.eclipse.osgi.util.NLS;

public class RefactoringMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.refactoring.RefactoringMessages";//$NON-NLS-1$

	public static String LaunchConfigurationParticipant_0;
	public static String LaunchConfigurationBuildfileChange_0;
	public static String LaunchConfigurationBuildfileChange_1;
	public static String LaunchConfigurationBuildfileChange_2;
	public static String LaunchConfigurationBuildfileChange_4;
	public static String LaunchConfigurationBuildfileChange_5;
	public static String LaunchConfigurationBuildfileChange_6;
	public static String LaunchConfigurationBuildfileChange_7;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, RefactoringMessages.class);
	}
}