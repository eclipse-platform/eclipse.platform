package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IImport;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.Utilities;

/**
 * The main plugin class to be used in the desktop.
 */
public class UpdateManagerPlugin extends Plugin {

	// debug options
	public static boolean DEBUG = false;
	public static boolean DEBUG_SHOW_INSTALL = false;
	public static boolean DEBUG_SHOW_PARSING = false;
	public static boolean DEBUG_SHOW_WARNINGS = false;
	public static boolean DEBUG_SHOW_CONFIGURATION = false;
	public static boolean DEBUG_SHOW_TYPE = false;
	public static boolean DEBUG_SHOW_WEB = false;
	public static boolean DEBUG_SHOW_IHANDLER = false;
	public static boolean DEBUG_SHOW_RECONCILER = false;

	//The shared instance.
	private static UpdateManagerPlugin plugin;

	//log
	private static UpdateManagerLogWriter log;
	private static final String LOG_FILE=".install-log";

	// web install
	private static String appServerHost =null;
	private static int appServerPort = 0;

	/**
	 * The constructor.
	 */
	public UpdateManagerPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
	}

	/**
	 * Returns the shared instance.
	 */
	public static UpdateManagerPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Returns the host identifier for the web app server
	 */
	public static String getWebAppServerHost() {
		return appServerHost;
	}

	/**
	 * Returns the port identifier for the web app server
	 */
	public static int getWebAppServerPort() {
		return appServerPort;
	}

	/**
	 * @see Plugin#startup()
	 */
	public void startup() throws CoreException {
		super.startup();

		Policy.localize("org.eclipse.update.internal.core.messages"); //$NON-NLS-1$
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
			File logFile = getUpdateStateLocation();
			if (logFile!=null)
				log = new UpdateManagerLogWriter(logFile);
		} catch (IOException e){
			warn("",e);
		}
	}

	/**
	 * @see Plugin#shutdown()
	 */
	public void shutdown() throws CoreException {
		super.shutdown();
		
		JarContentReference.shutdown(); // make sure we are not leaving jars open
		Utilities.shutdown(); // cleanup temp area
		if (log!=null)
			log.shutdown();
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
		msg.append("^");
		msg.append(Integer.toHexString(Thread.currentThread().hashCode()));
		msg.append(" ");
		msg.append(s);
		System.out.println(msg.toString());
	}
	
	/**
	 * Dumps a String in the log if WARNING is set to true
	 */
	public static void warn(String s) {
		if (DEBUG && DEBUG_SHOW_WARNINGS) {
			if (s!=null){
				s="WARNING: "+s;
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
				s="UPDATE MANAGER INFO: "+s;
			}
			log(s,e);
		}
	}
			
	/**
	 * Logs a status
	 */
	public static void log(IStatus status){
		UpdateManagerPlugin.getPlugin().getLog().log(status);		
	}
	
	/**
	 * Logs an error
	 */
	public static void log(Throwable e){		
		log("",e);
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
	private static File getUpdateStateLocation() throws IOException {
		
		IPlatformConfiguration config = BootLoader.getCurrentPlatformConfiguration();		
		URL configurationLocation = config.getConfigurationLocation();
		if (configurationLocation==null){
			warn("Unable to retrieve location for update manager log file");
			return null;
		}
		URL configLocation = Platform.resolve(configurationLocation);
		File updateStateLocation = null;

		if ("file".equalsIgnoreCase(configLocation.getProtocol())) {
			// ensure path exists. Handle transient configurations
			ArrayList list = new ArrayList();
			File path = new File(configLocation.getFile());
			updateStateLocation = new File(path.getParentFile(),LOG_FILE);
			while (path != null) { // walk up to first dir that exists
				if (!path.exists()) {
					list.add(path);
					path = path.getParentFile();
				} else
					path = null;
			}
			for (int i = list.size() - 1; i >= 0; i--) { // walk down to create missing dirs
				path = (File) list.get(i);
				path.mkdir();
				if (config.isTransient())
					path.deleteOnExit();
			}
		}
		return updateStateLocation;
	}

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

}