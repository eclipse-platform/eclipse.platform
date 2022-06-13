/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.services.about;

import org.eclipse.osgi.util.NLS;

public class AboutMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.e4.core.internal.services.about.AboutMessages"; //$NON-NLS-1$

	public static String bundleInfoFormat;

	public static String bundleStateInstalled;
	public static String bundleStateResolved;
	public static String bundleStateStarting;
	public static String bundleStateStopping;
	public static String bundleStateUninstalled;
	public static String bundleStateActive;
	public static String bundleStateUnknown;

	public static String errorReadingPreferences;

	public static String featuresInfoFormat;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, AboutMessages.class);
	}

	private AboutMessages() {
	}
}
