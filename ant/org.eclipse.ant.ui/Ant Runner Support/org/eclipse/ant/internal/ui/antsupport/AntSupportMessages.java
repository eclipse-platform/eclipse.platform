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
package org.eclipse.ant.internal.ui.antsupport;

import org.eclipse.osgi.util.NLS;

public class AntSupportMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.antsupport.AntSupportMessages";//$NON-NLS-1$

	public static String AntInputHandler_Ant_Input_Request_1;
	public static String AntInputHandler_Invalid_input_2;
	public static String AntInputHandler_Unable_to_respond_to__input__request_4;
	public static String AntInputHandler_5;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntSupportMessages.class);
	}
}