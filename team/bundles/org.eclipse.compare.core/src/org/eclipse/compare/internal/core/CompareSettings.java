/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core;

/**
 * The activator class controls the plug-in life cycle
 */
public class CompareSettings {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.compare.core"; //$NON-NLS-1$

	// The shared instance
	private static CompareSettings compareSettings;

	private boolean cappingDisabled;

	/**
	 * The constructor
	 */
	private CompareSettings() {
		// nothing to do
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CompareSettings getDefault() {
		if (compareSettings == null) {
			compareSettings = new CompareSettings();
		}
		return compareSettings;
	}

	public void setCappingDisabled(boolean disable) {
		this.cappingDisabled = disable;
	}


	public boolean isCappingDisabled() {
		return this.cappingDisabled;
	}

}
