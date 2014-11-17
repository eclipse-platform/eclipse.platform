/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh2;

import java.util.Hashtable;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class CVSSSH2Plugin extends Plugin {

	public static String ID = "org.eclipse.team.cvs.ssh2"; //$NON-NLS-1$
	private static CVSSSH2Plugin plugin;

	private ServiceRegistration debugRegistration;
	private ServiceTracker tracker;
	
	public CVSSSH2Plugin() {
		super();
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		try {
			// unregister debug options listener
			debugRegistration.unregister();
			debugRegistration = null;

			JSchSession.shutdown();
			tracker.close();
		} finally {
			super.stop(context);
		}
	}

	public static CVSSSH2Plugin getDefault() {
		return plugin;
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// register debug options listener
		Hashtable properties = new Hashtable(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, Policy.DEBUG_OPTIONS_LISTENER, properties);

	    tracker = new ServiceTracker(getBundle().getBundleContext(), IJSchService.class.getName(), null);
	    tracker.open();
	}
	
    public IJSchService getJSchService() {
        return (IJSchService)tracker.getService();
    }
}
