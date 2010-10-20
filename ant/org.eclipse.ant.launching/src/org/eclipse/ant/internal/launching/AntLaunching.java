/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.osgi.framework.BundleContext;

/**
 * Controls the plug-in life cycle
 */
public class AntLaunching extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.ant.launching"; //$NON-NLS-1$
	
	private static final String EMPTY_STRING= ""; //$NON-NLS-1$
	
	/**
	 * Boolean attribute indicating if an input handler should be supplied for
	 * the build Default value is <code>true</code>.
	 */
	public static final String SET_INPUTHANDLER = "org.eclipse.ant.uiSET_INPUTHANDLER"; //$NON-NLS-1$

	/**
	 * int preference identifier constant which specifies the length of time to
	 * wait to connect with the socket that communicates with the separate JRE
	 * to capture the output
	 */
	public static final String ANT_COMMUNICATION_TIMEOUT = "timeout"; //$NON-NLS-1$

	/**
	 * Size of left-hand column for right-justified task name. Used for Ant
	 * Build logging.
	 * 
	 * @see org.eclipse.ant.internal.ui.antsupport.logger.AntProcessBuildLogger
	 * @see org.eclipse.ant.internal.launching.launchConfigurations.RemoteAntBuildListener
	 */
	public static final int LEFT_COLUMN_SIZE = 15;
	
	/**
	 * String attribute indicating the custom runtime classpath to use for an
	 * Ant build. Default value is <code>null</code> which indicates that the
	 * global classpath is to be used. Format is a comma separated listing of
	 * URLs.
	 * 
	 * @deprecated no longer supported: use
	 *             {@link IJavaLaunchConfigurationConstants#ATTR_CLASSPATH_PROVIDER}
	 * @see IJavaLaunchConfigurationConstants#ATTR_DEFAULT_CLASSPATH
	 */
	public static final String ATTR_ANT_CUSTOM_CLASSPATH = IExternalToolConstants.UI_PLUGIN_ID
			+ ".ATTR_ANT_CUSTOM_CLASSPATH"; //$NON-NLS-1$
	/**
	 * String attribute indicating the custom Ant home to use for an Ant build.
	 * Default value is <code>null</code> which indicates that no Ant home is to
	 * be set
	 * 
	 * @deprecated no longer supported: use
	 *             {@link IJavaLaunchConfigurationConstants#ATTR_CLASSPATH_PROVIDER}
	 * @see IJavaLaunchConfigurationConstants#ATTR_DEFAULT_CLASSPATH
	 */
	public static final String ATTR_ANT_HOME = IExternalToolConstants.UI_PLUGIN_ID
			+ ".ATTR_ANT_HOME"; //$NON-NLS-1$
	
	
	/**
	 * Status code indicating an unexpected internal error.
	 * @since 2.1
	 */
	public static final int INTERNAL_ERROR = 120;	

	// The shared instance
	private static AntLaunching plugin;
	
	/**
	 * The constructor
	 */
	public AntLaunching() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AntLaunching getDefault() {
		return plugin;
	}
	
	public static String getUniqueIdentifier() {
		return PLUGIN_ID;
	}
	
	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 */
	public static void log(Throwable t) {
		IStatus status= new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, "Error logged from Ant UI: ", t); //$NON-NLS-1$
		log(status);
	}
	
	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status 
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	/**
	 * Writes the message to the plug-in's log
	 * 
	 * @param message the text to write to the log
	 */
	public static void log(String message, Throwable exception) {
		IStatus status = newErrorStatus(message, exception);
		log(status);
	}
	
	/**
	 * Returns a new <code>IStatus</code> for this plug-in
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		if (message == null) {
			message= EMPTY_STRING; 
		}		
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception);
	}
	
	public static AntLaunching getPlugin() {
		return plugin;
	}

}
