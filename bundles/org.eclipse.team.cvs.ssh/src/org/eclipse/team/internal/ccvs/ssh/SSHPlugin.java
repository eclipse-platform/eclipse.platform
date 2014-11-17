/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh;

import java.util.Hashtable;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class SSHPlugin extends Plugin {
	
	public static String ID = "org.eclipse.team.cvs.ssh"; //$NON-NLS-1$

	private static SSHPlugin instance;

	private ServiceRegistration debugRegistration;

	/**
	 * Log the given exception along with the provided message and severity indicator
	 */
	public static void log(int severity, String message, Throwable e) {
		getPlugin().getLog().log(new Status(severity, ID, 0, message, e));
	}
	
	/**
	 * Constructor for SSHPlugin
	 */
	public SSHPlugin() {
		super();	
		instance = this;
	}
	
	/**
	 * Method getPlugin.
	 */
	public static SSHPlugin getPlugin() {
		return instance;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);

		// register debug options listener
		Hashtable properties = new Hashtable(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER, properties);
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		// unregister debug options listener
		debugRegistration.unregister();
		debugRegistration = null;
	}
}
