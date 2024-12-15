/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
package org.eclipse.update.internal.configurator;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ConfigurationActivator implements BundleActivator, IConfigurationConstants {

	public static String PI_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	public static final String LAST_CONFIG_STAMP = "last.config.stamp"; //$NON-NLS-1$
	public static final String NAME_SPACE = "org.eclipse.update"; //$NON-NLS-1$
	public static final String UPDATE_PREFIX = "update@"; //$NON-NLS-1$

	// debug options
	public static String OPTION_DEBUG = PI_CONFIGURATOR + "/debug"; //$NON-NLS-1$
	// debug values
	public static boolean DEBUG = false;

	private static BundleContext context;

	@Override
	public void start(BundleContext ctx) throws Exception {
		context = ctx;
		loadOptions();
		acquireFrameworkLogService();
		Utils.debug("Starting update configurator..."); //$NON-NLS-1$
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		Utils.shutdown();
	}

	private void loadOptions() {
		// all this is only to get the application args
		DebugOptions service = null;
		ServiceReference<DebugOptions> reference = context.getServiceReference(DebugOptions.class);
		if (reference != null)
			service = context.getService(reference);
		if (service == null)
			return;
		try {
			DEBUG = service.getBooleanOption(OPTION_DEBUG, false);
		} finally {
			// we have what we want - release the service
			context.ungetService(reference);
		}
	}

	public static BundleContext getBundleContext() {
		return context;
	}

	private void acquireFrameworkLogService() {
		ServiceReference<FrameworkLog> logServiceReference = context.getServiceReference(FrameworkLog.class);
		if (logServiceReference == null)
			return;
		Utils.log = context.getService(logServiceReference);
	}
}
