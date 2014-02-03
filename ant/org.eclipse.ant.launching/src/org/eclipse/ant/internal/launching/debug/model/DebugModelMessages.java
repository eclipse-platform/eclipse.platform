/**********************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.osgi.util.NLS;

public class DebugModelMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.launching.debug.model.DebugModelMessages";//$NON-NLS-1$

	public static String AntDebugTarget_0;

	public static String AntLineBreakpoint_0;
	public static String AntThread_0;
	public static String AntThread_1;
	public static String AntThread_2;
    public static String AntThread_3;
    public static String AntThread_4;
    
    public static String AntProperties_1;
    
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, DebugModelMessages.class);
	}
}