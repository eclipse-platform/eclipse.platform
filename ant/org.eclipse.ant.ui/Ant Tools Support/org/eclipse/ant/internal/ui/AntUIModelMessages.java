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
package org.eclipse.ant.internal.ui;

import org.eclipse.osgi.util.NLS;

public class AntUIModelMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.AntUIModelMessages";//$NON-NLS-1$

	public static String ImageDescriptorRegistry_Allocating_image_for_wrong_display_1;

	public static String AntUtil_6;
	public static String AntUtil_0;
	public static String AntUtil_1;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntUIModelMessages.class);
	}
}