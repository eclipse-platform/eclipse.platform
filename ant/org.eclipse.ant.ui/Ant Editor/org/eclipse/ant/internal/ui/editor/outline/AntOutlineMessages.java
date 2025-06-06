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
 * John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 53547
 **********************************************************************/
package org.eclipse.ant.internal.ui.editor.outline;

import org.eclipse.osgi.util.NLS;

public class AntOutlineMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.editor.outline.AntOutlineMessages";//$NON-NLS-1$

	public static String AntEditorContentOutlinePage_Open_With_1;

	public static String FilterInternalTargetsAction_0;
	public static String FilterImportedElementsAction_0;
	public static String FilterPropertiesAction_0;

	public static String FilterTopLevelAction_0;

	public static String ToggleSortAntOutlineAction_0;

	public static String ToggleLinkWithEditorAction_0;
	public static String ToggleLinkWithEditorAction_1;
	public static String ToggleLinkWithEditorAction_2;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntOutlineMessages.class);
	}
}