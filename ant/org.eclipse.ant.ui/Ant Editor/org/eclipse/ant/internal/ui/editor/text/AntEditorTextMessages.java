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