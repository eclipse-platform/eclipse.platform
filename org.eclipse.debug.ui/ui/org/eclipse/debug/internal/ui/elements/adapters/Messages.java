/*******************************************************************************
 *  Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.osgi.util.NLS;

/**
 * @since 3.2
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.elements.adapters.Messages"; //$NON-NLS-1$

	private Messages() {
	}

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String VariableColumnPresentation_0;
	public static String VariableColumnPresentation_1;
	public static String VariableColumnPresentation_2;
	public static String VariableColumnPresentation_3;
}
