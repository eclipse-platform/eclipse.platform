/*******************************************************************************
 *  Copyright (c) 2009, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.internal.tests.contexts.ContextFunctionHigh;
import org.eclipse.e4.core.internal.tests.contexts.ContextFunctionLow;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class CoreTestsActivator implements BundleActivator {

	static private CoreTestsActivator defaultInstance;
	private BundleContext bundleContext;
	private ServiceTracker<DebugOptions, DebugOptions> debugTracker = null;
	private ServiceTracker<IPreferencesService, IPreferencesService> preferencesTracker = null;
	private ServiceTracker<EventAdmin, EventAdmin> eventAdminTracker;

	public CoreTestsActivator() {
		defaultInstance = this;
	}

	public static CoreTestsActivator getDefault() {
		return defaultInstance;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		registerContextFunctions();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (preferencesTracker != null) {
			preferencesTracker.close();
			preferencesTracker = null;
		}
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		if (eventAdminTracker != null) {
			eventAdminTracker.close();
			eventAdminTracker = null;
		}
		bundleContext = null;
	}

	private void registerContextFunctions() {
		{
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(IContextFunction.SERVICE_CONTEXT_KEY,"test.contextfunction.ranking");
			properties.put(Constants.SERVICE_RANKING, 0);
			bundleContext.registerService(IContextFunction.SERVICE_NAME, new ContextFunctionLow(), properties);
		}

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(IContextFunction.SERVICE_CONTEXT_KEY,"test.contextfunction.ranking");
		properties.put(Constants.SERVICE_RANKING, 100);
		bundleContext.registerService(IContextFunction.SERVICE_NAME, new ContextFunctionHigh(), properties);

	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public EventAdmin getEventAdmin() {
		if (eventAdminTracker == null) {
			if (bundleContext == null) {
				return null;
			}
			eventAdminTracker = new ServiceTracker<>(bundleContext, EventAdmin.class.getName(), null);
			eventAdminTracker.open();
		}
		return eventAdminTracker.getService();
	}

}
