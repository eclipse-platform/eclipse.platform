/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.variables;

import org.eclipse.osgi.util.NLS;

public class VariableMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ui.externaltools.internal.variables.VariableMessages";//$NON-NLS-1$

	public static String BuildProjectResolver_3;
	public static String SystemPathResolver_0;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, VariableMessages.class);
	}
}