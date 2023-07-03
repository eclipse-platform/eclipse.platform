/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest.internal;

import org.osgi.framework.BundleContext;

import org.eclipse.unittest.internal.model.UnitTestLaunchListener;
import org.eclipse.unittest.internal.model.UnitTestModel;
import org.eclipse.unittest.internal.ui.history.History;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchListener;

/**
 * The plug-in runtime class for the Unit Test plug-in.
 */
public class UnitTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static UnitTestPlugin fgPlugin = null;

	private final ILaunchListener fLaunchListener = new UnitTestLaunchListener();

	public static final String PLUGIN_ID = "org.eclipse.unittest.ui"; //$NON-NLS-1$

	/**
	 * Constructs a {@link UnitTestPlugin} object
	 */
	public UnitTestPlugin() {
		fgPlugin = this;
	}

	/**
	 * Returns the {@link UnitTestPlugin} instance
	 *
	 * @return a {@link UnitTestPlugin} instance
	 */
	public static UnitTestPlugin getDefault() {
		return fgPlugin;
	}

	/**
	 * Logs the given exception.
	 *
	 * @param e the {@link Throwable} to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * Logs the given status.
	 *
	 * @param status the status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		UnitTestModel.getInstance().start();
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(fLaunchListener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID).flush();
			UnitTestModel.getInstance().stop();
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(fLaunchListener);
			History.INSTANCE.clear();
		} finally {
			super.stop(context);
		}
	}

}
