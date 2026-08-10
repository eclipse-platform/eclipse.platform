/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime.bundlegroups;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.core.internal.runtime.bundlegroups.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String BundleGroupProvider;
	public static String FeatureParser_IdOrVersionInvalid;
	public static String IniFileReader_MissingDesc;
	public static String IniFileReader_OpenINIError;
	public static String IniFileReader_ReadIniError;
	public static String IniFileReader_ReadPropError;
	public static String IniFileReader_ReadMapError;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
