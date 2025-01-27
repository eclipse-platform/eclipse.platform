/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.core.tests.pki;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
/**
 * The activator class controls the plug-in life cycle
 */

public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.core.tests.pki";

	// The shared instance
	private static Activator plugin;

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		//super.start(context);
		Activator.context = context;
		new MainTest();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}
	public static Activator getDefault() {
		return plugin;
	}

}
