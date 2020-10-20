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

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ComparePlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.compare.core"; //$NON-NLS-1$

	// The shared instance
	private static ComparePlugin plugin;

	private boolean cappingDisabled;

	/**
	 * The constructor
	 */
	public ComparePlugin() {
		// nothing to do
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ComparePlugin getDefault() {
		return plugin;
	}

	public void setCappingDisabled(boolean disable) {
		this.cappingDisabled = disable;
	}


	public boolean isCappingDisabled() {
		return this.cappingDisabled;
	}

}
