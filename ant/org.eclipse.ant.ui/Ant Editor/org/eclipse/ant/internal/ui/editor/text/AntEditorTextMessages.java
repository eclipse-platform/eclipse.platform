/**********************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.osgi.util.NLS;

public class AntEditorTextMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.editor.text.AntEditorTextMessages";//$NON-NLS-1$

	public static String XMLTextHover_4;
	public static String XMLTextHover_5;
	public static String XMLTextHover_6;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntEditorTextMessages.class);
	}
}