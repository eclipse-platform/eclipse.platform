/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.connection.ConnectionThreadManagerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The main plugin class to be used in the desktop.
 */
public class UpdateCore extends Plugin {

	// debug options
	public static boolean DEBUG;
	public static boolean DEBUG_SHOW_INSTALL;
	public static boolean DEBUG_SHOW_PARSING;
	public static boolean DEBUG_SHOW_WARNINGS;
	public static boolean DEBUG_SHOW_CONFIGURATION;
	public static boolean DEBUG_SHOW_TYPE;
	public static boolean DEBUG_SHOW_WEB;
	public static boolean DEBUG_SHOW_IHANDLER;
	public static boolean DEBUG_SHOW_RECONCILER;
		
	private static final String PREFIX = "org.eclipse.update.core"; //$NON-NLS-1$
	public static final String P_HISTORY_SIZE = PREFIX + ".historySize"; //$NON-NLS-1$
	public static final String P_CHECK_SIGNATURE = PREFIX + ".checkSignature"; //$NON-NLS-1$
	public static final String P_AUTOMATICALLY_CHOOSE_MIRROR =  PREFIX + ".automaticallyChooseMirror"; //$NON-NLS-1$
	public static final String P_UPDATE_VERSIONS = PREFIX + ".updateVersions"; //$NON-NLS-1$
	public static final String EQUIVALENT_VALUE = "equivalent"; //$NON-NLS-1$
	public static final String COMPATIBLE_VALUE = "compatible"; //$NON-NLS-1$
	
	public static int DEFAULT_HISTORY = 100;//Integer.MAX_VALUE;
	
	//The shared instance.
	private static UpdateCore plugin;

	//log
	private static UpdateManagerLogWriter log;
	private static final String LOG_FILE="install.log"; //$NON-NLS-1$

	// bundle data
	private BundleContext context;
	private ServiceTracker pkgAdminTracker;
	private ServiceTracker verifierFactoryTracker;
	private ServiceTracker proxyTracker;
	
	// Session
	private UpdateSession updateSession = null;
	/**
	 * HTTP response code indicating success.
	 */
	public static final int HTTP_OK = 200;
	
	/**
	 * The constructor.
	 */
	public UpdateCore() {
		plugin = this;
	}
	

	/**
	 * Returns the shared instance.
	 */
	public static UpdateCore getPlugin() {
		return plugin;
	}
	

	private boolean getBooleanDebugOption(String flag, boolean dflt) {
		String result = Platform.getDebugOption(flag);
		if (result == null)
			return dflt;
		else
			return result.trim().equalsIgnoreCase("true"); //$NON-NLS-1$
	}

	/**
	 * dumps a String in the trace
	 */
	public static void debug(String s) {
		StringBuffer msg = new StringBuffer();
		msg.append(getPlugin().toString());
		msg.append("^"); //$NON-NLS-1$
		msg.append(Integer.toHexString(Thread.currentThread().hashCode()));
		msg.append(" "); //$NON-NLS-1$
		msg.append(s);
		System.out.println(msg.toString());
	}
	
	/**
	 * Dumps a String in the log if WARNING is set to true
	 */
	public static void warn(String s) {
		if (DEBUG && DEBUG_SHOW_WARNINGS) {
			if (s!=null){
				s="WARNING: "+s; //$NON-NLS-1$
			}
			log(s, null); 
		}
	}

	/**
	 * Dumps an exception in the log if WARNING is set to true
	 * 
	 * @param s log string
	 * @param e exception to be logged
	 * @since 2.0
	 */
	public static void warn(String s, Throwable e) {
		if (DEBUG && DEBUG_SHOW_WARNINGS){
			if (s!=null){
				s="UPDATE MANAGER INFO: "+s; //$NON-NLS-1$
			}
			log(s,e);
		}
	}
			
	/**
	 * Logs a status
	 */
	public static void log(IStatus status){
		UpdateCore.getPlugin().getLog().log(status);		
	}
	
	/**
	 * Logs an error
	 */
	public static void log(Throwable e){		
		log("",e); //$NON-NLS-1$
	}	
	
	/**
	 * Logs a string and an  error
	 */
	public static void log(String msg, Throwable e){
		IStatus status = null;
		if (e instanceof CoreException) 
			status = ((CoreException)e).getStatus();
		else 
			status = Utilities.newCoreException(msg,e).getStatus();		
		if (status!=null)
			log(status);
	}		
	/*
	 * Method log.
	 * @param newConfiguration
	 */
	public static void log(IInstallConfiguration newConfiguration) {
		if (log!=null)
			log.log(newConfiguration);
	}

	/*
	 * Get update log location relative to platform configuration
	 */
	private static File getInstallLogFile() throws IOException {
		
		IPlatformConfiguration config = ConfiguratorUtils.getCurrentPlatformConfiguration();		
		URL configurationLocation = config.getConfigurationLocation();
		if (configurationLocation==null){
			warn("Unable to retrieve location for update manager log file"); //$NON-NLS-1$
			return null;
		}
//		URL configLocation = Platform.resolve(configurationLocation);
		File updateStateLocation = null;

		if ("file".equalsIgnoreCase(configurationLocation.getProtocol())) { //$NON-NLS-1$
			File path = new File(configurationLocation.getFile());
			updateStateLocation = new File(path.getParentFile(), LOG_FILE);
		}
		return updateStateLocation;
	}

	/**
	 * Sends the GET request to the server and returns the server's
	 * response.
	 *
	 * @param url the URL to open on the server
	 * @return the server's response
	 * @throws IOException if an I/O error occurs. Reasons include:
	 * <ul>
	 * <li>The client is closed.
	 * <li>The client could not connect to the server
	 * <li>An I/O error occurs while communicating with the server
	 * <ul>
	 */

	
	/*
	 * Returns true if the feature is a patch
	 */
	public static boolean isPatch(IFeature candidate) {
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch())
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;

        DEBUG = getBooleanDebugOption("org.eclipse.update.core/debug", false); //$NON-NLS-1$

		if (DEBUG) {
			DEBUG_SHOW_WARNINGS = getBooleanDebugOption("org.eclipse.update.core/debug/warning", false); //$NON-NLS-1$
			DEBUG_SHOW_PARSING = getBooleanDebugOption("org.eclipse.update.core/debug/parsing", false); //$NON-NLS-1$
			DEBUG_SHOW_INSTALL = getBooleanDebugOption("org.eclipse.update.core/debug/install", false); //$NON-NLS-1$
			DEBUG_SHOW_CONFIGURATION = getBooleanDebugOption("org.eclipse.update.core/debug/configuration", false); //$NON-NLS-1$
			DEBUG_SHOW_TYPE = getBooleanDebugOption("org.eclipse.update.core/debug/type", false); //$NON-NLS-1$
			DEBUG_SHOW_WEB = getBooleanDebugOption("org.eclipse.update.core/debug/web", false); //$NON-NLS-1$
			DEBUG_SHOW_IHANDLER = getBooleanDebugOption("org.eclipse.update.core/debug/installhandler", false); //$NON-NLS-1$
			DEBUG_SHOW_RECONCILER = getBooleanDebugOption("org.eclipse.update.core/debug/reconciler", false); //$NON-NLS-1$
		}
		
		//
		try {
			File logFile = getInstallLogFile();
			if (logFile!=null)
				log = new UpdateManagerLogWriter(logFile);
		} catch (IOException e){
			warn("",e); //$NON-NLS-1$
		}
	}
	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		
		JarContentReference.shutdown(); // make sure we are not leaving jars open
		Utilities.shutdown(); // cleanup temp area
		if (log!=null)
			log.shutdown();
		
		ConnectionThreadManagerFactory.getConnectionManager().shutdown();

		
		this.context = null;
		if (pkgAdminTracker != null) {
			pkgAdminTracker.close();
			pkgAdminTracker = null;
		}
		if (verifierFactoryTracker != null) {
			verifierFactoryTracker.close();
			verifierFactoryTracker = null;
		}
		if (proxyTracker != null) {
			proxyTracker.close();
			proxyTracker = null;
		}
	}
	
	public BundleContext getBundleContext() {
		return context;
	}
	
	PackageAdmin getPackageAdmin() {
		if (pkgAdminTracker == null) {
			pkgAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
			pkgAdminTracker.open();
		}
		return (PackageAdmin)pkgAdminTracker.getService();
	}
	
	public IProxyService getProxyService() {
		if (proxyTracker == null) {
		    proxyTracker=new ServiceTracker(getBundle().getBundleContext(),
		            IProxyService.class.getName(), null);
		    proxyTracker.open();
		}
		return (IProxyService)proxyTracker.getService();
	}


	public SignedContentFactory getSignedContentFactory() {
		if (verifierFactoryTracker == null) {
			verifierFactoryTracker = new ServiceTracker(context, SignedContentFactory.class.getName(), null);
			verifierFactoryTracker.open();
		}
		return (SignedContentFactory)verifierFactoryTracker.getService();
	}
	
	public UpdateSession getUpdateSession() {
		synchronized(UpdateSession.class) {
			if (updateSession == null) {
				updateSession = new UpdateSession();
			}
		}		
		return updateSession;
	}
}
