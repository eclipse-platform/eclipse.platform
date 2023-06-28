/**********************************************************************
 * Copyright (c) 2004, 2008 QNX Software Systems and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * IBM Corporation - Bug 90318
 * Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 ***********************************************************************/
package org.eclipse.debug.internal.ui.views.modules;

import org.eclipse.osgi.util.NLS;

public class ModulesViewMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.views.modules.ModulesViewMessages";//$NON-NLS-1$

	public static String ModulesView_0;

	public static String ModulesView_1;

	public static String ModulesView_2;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ModulesViewMessages.class);
	}
}
