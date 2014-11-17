/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20070119   161112 makandre@ca.ibm.com - Andrew Mak, WSE: can't find business thru a proxy server that needs basic auth
 * 20070201   154100 pmoogk@ca.ibm.com - Peter Moogk, Port internet code from WTP to Eclipse base.
 *******************************************************************************/

package org.eclipse.core.internal.net;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	/**
	 * The identifier of the descriptor of this plugin in plugin.xml.
	 */
	public static final String ID = "org.eclipse.core.net"; //$NON-NLS-1$

	/**
	 * The instance of this plugin.
	 */
	private static Activator instance;

	private static ServiceTracker logTracker;

	private static final String PROP_REGISTER_SERVICE = "org.eclipse.net.core.enableProxyService"; //$NON-NLS-1$

	public static final String PT_AUTHENTICATOR = "authenticator"; //$NON-NLS-1$

	private BundleContext bundleContext;

	private ServiceTracker instanceLocationTracker;
	
	private ServiceRegistration debugRegistration;

	private ServiceRegistration proxyService;
	
	private PreferenceManager preferenceManger;

	/**
	 * Constructor for use by the Eclipse platform only.
	 */
	public Activator() {
		super();
		instance = this;
	}

	/**
	 * Returns the instance of this plugin.
	 * 
	 * @return the singleton instance of this plug-in class
	 */
	static public Activator getInstance() {
		return instance;
	}

	public static void log(int severity, String message, Throwable throwable) {
		getInstance().log(new Status(severity, ID, 0, message, throwable));
	}

	public static void logError(String message, Throwable exc) {
		getInstance().log(new Status(IStatus.ERROR, ID, 0, message, exc));
	}

	public static void logInfo(String message, Throwable exc) {
		getInstance().log(new Status(IStatus.INFO, ID, 0, message, exc));
	}

	/*
	 * Log the given status to the log file. If the log is not available, log
	 * the status to the console.
	 */
	private void log(IStatus status) {
		if (logTracker == null) {
			logTracker = new ServiceTracker(bundleContext, FrameworkLog.class
					.getName(), null);
			logTracker.open();
		}
		FrameworkLog log = (FrameworkLog) logTracker.getService();
		if (log != null) {
			log.log(getLog(status));
		} else {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}

	/**
	 * Copied from PlatformLogWriter in core runtime.
	 */
	private FrameworkLogEntry getLog(IStatus status) {
		Throwable t = status.getException();
		ArrayList childlist = new ArrayList();

		int stackCode = t instanceof CoreException ? 1 : 0;
		// ensure a substatus inside a CoreException is properly logged
		if (stackCode == 1) {
			IStatus coreStatus = ((CoreException) t).getStatus();
			if (coreStatus != null) {
				childlist.add(getLog(coreStatus));
			}
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++) {
				childlist.add(getLog(children[i]));
			}
		}

		FrameworkLogEntry[] children = (FrameworkLogEntry[]) (childlist.size() == 0 ? null
				: childlist.toArray(new FrameworkLogEntry[childlist.size()]));

		return new FrameworkLogEntry(status.getPlugin(), status.getSeverity(),
				status.getCode(), status.getMessage(), stackCode, t, children);
	}

	public PreferenceManager getPreferenceManager() {
		return preferenceManger;
	}

	public boolean instanceLocationAvailable() {
		Location instanceLocation = (Location) instanceLocationTracker
				.getService();
		return (instanceLocation != null && instanceLocation.isSet());
	}

	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;
		this.preferenceManger = PreferenceManager.createConfigurationManager(ID);
		Filter filter = null;
		try {
			filter = context.createFilter(Location.INSTANCE_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this. It should never happen as we have tested the above
			// format.
		}
		instanceLocationTracker = new ServiceTracker(context, filter, null);
		instanceLocationTracker.open();

		// register debug options listener
		Hashtable properties = new Hashtable(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER, properties);

		if (Boolean
				.valueOf(System.getProperty(PROP_REGISTER_SERVICE, "true")).booleanValue()) { //$NON-NLS-1$
			ProxyManager proxyManager = (ProxyManager) ProxyManager
					.getProxyManager();
			proxyManager.initialize();
			proxyService = context.registerService(IProxyService.class
					.getName(), proxyManager, new Hashtable());
		}
	}

	public void stop(BundleContext context) throws Exception {
		if (proxyService != null) {
			proxyService.unregister();
			proxyService = null;
		}

		if (debugRegistration != null) {
			debugRegistration.unregister();
			debugRegistration = null;
		}

		if (instanceLocationTracker != null) {
			instanceLocationTracker.close();
			instanceLocationTracker = null;
		}
	}
}
