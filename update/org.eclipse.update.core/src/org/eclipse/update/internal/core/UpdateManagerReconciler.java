package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.boot.*;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

/**
 * Update manager reconciler application.
 * This application is invoked by the platform startup support
 * whenever changes are detected in the installation that require
 * the update state to be reconciled. Typically, this will be as
 * a result of installation actions that were done directly 
 * in the file system bypassing the Update Manager.
 * When triggered, this application performs the following:
 * <ul>
 * <li>call update manager reconciler
 * <li>if plugin path changed as a result of this call, trigger
 * restart ("exit code 23")
 * <li>if plugin path has not changed as a result of this call,
 * start this original application
 * </ul>
 */

public class UpdateManagerReconciler implements IPlatformRunnable {
	
	// NOTE: originalApplication is set to a dummy string to prevent "silent"
	// failures in case of internal errors
	private String originalApplication = "org.eclipse.update.UNKNOWN"; //$NON-NLS-1$
	private boolean initialize = false;
	private boolean firstUse = false;
	private boolean optimistic = false;
	private boolean DEBUG = false;
	
	private static final String RECONCILER_APP = "org.eclipse.update.core.reconciler";
	private static final String APPLICATION = "-application"; //$NON-NLS-1$	
	private static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
	private static final String FIRSTUSE = "-firstuse"; //$NON-NLS-1$
	private static final String NEWUPDATES = "-newUpdates"; //$NON-NLS-1$
	private static final String CHANGES_MARKER = ".newupdates"; //$NON-NLS-1$

	/**
	 * @see IPlatformRunnable#run(Object)
	 */
	public Object run(Object args) throws Exception {
		
		// get debug setting and process command line arguments
		DEBUG = UpdateManagerPlugin.DEBUG_SHOW_RECONCILER;
		processCommandLine((String[])args);		
		
		// obtain current platform configuration and save stamps
		IPlatformConfiguration cfg = BootLoader.getCurrentPlatformConfiguration();
	   // URL[] originalPluginPath = cfg.getPluginPath();
		
		// perform reconciliation
		long start = 0;
		if (DEBUG) {
			start = new Date().getTime();
			debug("begin"); //$NON-NLS-1$
		}
		
		if (initialize || firstUse)
			optimistic = true;	// initialize and first-time startup ... optimistic
								// (changes are auto-configured)
		else
			optimistic = false;	// all other ... pessimistic (changes are
									// reflected in state but not configured)
		
		boolean changes = reconcile(cfg);
		
		if (DEBUG) {
			long delta = (new Date().getTime()) - start;
			debug("end [" + delta + "ms]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// if we are completing post-install initialization (-initialize)
		// just exit
		if (initialize) {
			Platform.endSplash();
			return EXIT_OK;
		}
		
		// if the reconciler app was explicitly invoked, mark changes and
		// just exit
		if (RECONCILER_APP.equals(originalApplication)) {
			Platform.endSplash();
			if (changes) 
				markChanges(cfg);
			return EXIT_OK; // just exit if original app was reconciler
		}		
				
		// see if plugins have changed
//		if (pluginPathChanged(cfg, originalPluginPath)) {
			// plugins lineup changed ... need to restart
			Platform.endSplash();
			if (changes)
				markChanges(cfg);
			if (DEBUG)
				debug("restarting ...");  //$NON-NLS-1$
			return EXIT_RESTART;
//		} else {
//			// plugins lineup did not change ... no need to restart, 
//			// just continue starting up the original app. Original
//			// app will take down splash
//			IPlatformRunnable originalRunnable = getRunnable(originalApplication);
//			if (originalRunnable == null)
//				throw new IllegalArgumentException(Policy.bind("Reconciler.appNotFound",originalApplication));  //$NON-NLS-1$
//			if (DEBUG)
//				debug("invoking " + originalApplication + " ...");  //$NON-NLS-1$  //$NON-NLS-2$
//			// indicate we have new updates (-newUpdates)	
//			if (changes)
//				appArgs = markChanges(appArgs);
//			return originalRunnable.run(appArgs);
//		}
	}
	
	private boolean reconcile(IPlatformConfiguration cfg) {
			
		boolean changes = true;
		
		// do the reconciliation
		try {
			if (DEBUG)
				debug("mode: " + (optimistic ? "optimistic" : "pessimistic")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			changes = InternalSiteManager.reconcile(optimistic);
			if (DEBUG) 
				debug(changes ? "changes detected" :"no changes detected"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException e){
			UpdateManagerPlugin.warn(null,e);
		} catch (Exception e) {
			UpdateManagerPlugin.warn(null,e);
		}
		cfg.refresh(); // recompute stamps and plugin path
		
		return changes;
	}
	
	private boolean pluginPathChanged(IPlatformConfiguration cfg, URL[] originalPluginPath) {
		URL[] currentPluginPath = cfg.getPluginPath();
		HashMap originalMap = new HashMap();
		HashMap currentMap = new HashMap();
		
		// populate maps
		for (int i=0; i< originalPluginPath.length; i++) {
			originalMap.put(originalPluginPath[i].toExternalForm(), null);
		}		
		for (int i=0; i< currentPluginPath.length; i++) {
			currentMap.put(currentPluginPath[i].toExternalForm(), null);
		}
		
		// check for deletions
		for (int i=0; i<originalPluginPath.length; i++) {
			String key = originalPluginPath[i].toExternalForm();
			if (!currentMap.containsKey(key))
				return true;
		}
		
		// check for additions
		for (int i=0; i<currentPluginPath.length; i++) {
			String key = currentPluginPath[i].toExternalForm();
			if (!originalMap.containsKey(key))
				return true;
		}
		
		return false;
	}
	
	private void markChanges(IPlatformConfiguration cfg) {
		// indicate we have changes in restart scenario ... converted to -newUpdates on restart
		FileOutputStream fos = null;
		try {
			URL markerLocation = new URL(cfg.getConfigurationLocation(),CHANGES_MARKER);
			fos = new FileOutputStream(new File(markerLocation.getFile()));
			fos.write(0);
			fos.close();
		} catch(IOException e) {
			if (fos != null) try { fos.close(); } catch(IOException e2) {}
		}		
	}
	
	private String[] markChanges(String[] args) {
		// indicate we have changes in continue-startup scenario ... adds -newUpdate
		String[] newArgs = new String[args.length+1];
		newArgs[0] = NEWUPDATES;
		System.arraycopy(args,0,newArgs,1,args.length);
		return newArgs;
	}
	
	private String[] processCommandLine(String[] args) {
		int[] configArgs = new int[100];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;			
					
			// check for args without parameters (i.e., a flag arg)
			
			// look for the initialization flag
			if (args[i].equalsIgnoreCase(INITIALIZE)) {
				initialize = true;
				found = true;
			}
			
			// look for first use flag
			if (args[i].equalsIgnoreCase(FIRSTUSE)) {
				firstUse = true;
				found = true;
			}
				
			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}
			
			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) { //$NON-NLS-1$
				continue;
			}
			String arg = args[++i];
			
			// look for the application argument
			if (args[i - 1].equalsIgnoreCase(APPLICATION)) {
				found = true;
				originalApplication = arg;
			}

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0)
			return args;
		String[] passThruArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int j = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex])
				configArgIndex++;
			else
				passThruArgs[j++] = args[i];
		}
		return passThruArgs;
	}
	
	private IPlatformRunnable getRunnable(String application) {
		// NOTE: we need to get the runnable for the original application.
		// We can either lookup the application extension point (duplicate
		// the InternalPlatform logic here), or make the internal call
		return InternalPlatform.loaderGetRunnable(application);
	}
	
	private void debug(String s) {
		UpdateManagerPlugin.debug(this.getClass().getName()+": "+s); //$NON-NLS-1$
	}
}
