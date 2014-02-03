/**********************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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