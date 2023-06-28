/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.core.externaltools.internal.launchConfigurations;

import org.eclipse.osgi.util.NLS;

public class ExternalToolsProgramMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsProgramMessages";//$NON-NLS-1$


	public static String BackgroundResourceRefresher_0;

	public static String ProgramLaunchDelegate_3;
	public static String ProgramLaunchDelegate_4;
	public static String ProgramLaunchDelegate_5;

	public static String ExternalToolsUtil_Location_not_specified_by__0__1;
	public static String ExternalToolsUtil_invalidLocation__0_;
	public static String ExternalToolsUtil_invalidDirectory__0_;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ExternalToolsProgramMessages.class);
	}
}