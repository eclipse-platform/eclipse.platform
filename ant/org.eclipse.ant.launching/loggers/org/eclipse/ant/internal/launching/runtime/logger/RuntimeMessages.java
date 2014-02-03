/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.runtime.logger;

import org.eclipse.osgi.util.NLS;

public class RuntimeMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.launching.runtime.logger.RuntimeMessages";//$NON-NLS-1$

	public static String NullBuildLogger_1;
	public static String AntProcessBuildLogger_Total_time;
	public static String AntProcessBuildLogger__minutes_2;
	public static String AntProcessBuildLogger__minute_3;
	public static String AntProcessBuildLogger__seconds_4;
	public static String AntProcessBuildLogger__second_5;
	public static String AntProcessBuildLogger__milliseconds_6;
	
	public static String AntProcessDebugBuildLogger_1;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, RuntimeMessages.class);
	}
}