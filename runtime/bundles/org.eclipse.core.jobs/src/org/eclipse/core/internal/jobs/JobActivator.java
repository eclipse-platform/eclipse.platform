/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.core.internal.jobs;

import java.util.Hashtable;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.osgi.framework.*;

/**
 * The Jobs plugin class.
 */
public class JobActivator implements BundleActivator {

	/**
	 * Eclipse property. Set to <code>false</code> to avoid registering JobManager
	 * as an OSGi service.
	 */
	private static final String PROP_REGISTER_JOB_SERVICE = "eclipse.service.jobs"; //$NON-NLS-1$

	/**
	 * The bundle associated this plug-in
	 */
	private static BundleContext bundleContext;

	/**
	 * This plugin provides a JobManager service.
	 */
	private ServiceRegistration jobManagerService = null;

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		JobOSGiUtils.getDefault().openServices();

		boolean shouldRegister = !"false".equalsIgnoreCase(context.getProperty(PROP_REGISTER_JOB_SERVICE)); //$NON-NLS-1$
		if (shouldRegister)
			registerServices();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		unregisterServices();
		JobManager.shutdown();
		JobOSGiUtils.getDefault().closeServices();
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}

	private void registerServices() {
		jobManagerService = bundleContext.registerService(IJobManager.class.getName(), JobManager.getInstance(), new Hashtable<>());
	}

	private void unregisterServices() {
		if (jobManagerService != null) {
			jobManagerService.unregister();
			jobManagerService = null;
		}
	}
}
