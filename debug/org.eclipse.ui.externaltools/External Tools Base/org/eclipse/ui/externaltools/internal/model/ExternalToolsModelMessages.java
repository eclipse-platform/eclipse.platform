/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.model;

import org.eclipse.osgi.util.NLS;

public class ExternalToolsModelMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ui.externaltools.internal.model.ExternalToolsModelMessages";//$NON-NLS-1$

	public static String ImageDescriptorRegistry_Allocating_image_for_wrong_display_1;
	public static String BuilderUtils_5;
	public static String BuilderUtils_6;
	public static String BuilderUtils_7;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ExternalToolsModelMessages.class);
	}
}