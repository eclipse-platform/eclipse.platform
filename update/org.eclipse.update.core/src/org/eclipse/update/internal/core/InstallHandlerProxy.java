/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IInstallHandler;
import org.eclipse.update.core.IInstallHandlerEntry;
import org.eclipse.update.core.IInstallHandlerWithFilter;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.InstallMonitor;
import org.eclipse.update.core.Utilities;
import org.osgi.framework.Bundle;

public class InstallHandlerProxy implements IInstallHandlerWithFilter {

	private IFeature feature = null;
	private int type;
	private IInstallHandler handler = null;
	private IStatus savedStatus = null;
	private boolean DEBUG = false;

	private static final String EXT_PLUGIN = "org.eclipse.update.core"; //$NON-NLS-1$
	private static final String UI_PLUGIN = "org.eclipse.ui"; //$NON-NLS-1$
	private static final String EXT_POINT = "installHandlers"; //$NON-NLS-1$
	private Method nonPluginDataAcceptor = null;

	/**
	 * A class loader that combines a the org.eclipse.update.core plugin class loader with the
	 * org.eclipse.ui class loader (only when UI is active).
	 */
	private static class InstallHandlerClassLoader extends URLClassLoader {
		private Bundle updateCore;
		private Bundle eclipseUI;

		public InstallHandlerClassLoader(URL[] classpath) {
			super(classpath);
			updateCore = Platform.getBundle(EXT_PLUGIN);
			eclipseUI = Platform.getBundle(UI_PLUGIN);
			if (eclipseUI != null && eclipseUI.getState() != Bundle.ACTIVE) 
				eclipseUI = null;
		}

		public Class loadClass(String className) throws ClassNotFoundException {
			// First check update core plugin loader, then the eclipse ui plugin loader
			Class c = null;
			try {
				c = updateCore.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					if(eclipseUI != null)
						c = eclipseUI.loadClass(className);
				} catch (ClassNotFoundException e2) {
				} finally {
				}
			} finally {
			}
			if (c != null)
				return c;
			else
				return super.loadClass(className);
		}

		public URL getResource(String resName) {
			// First check update core plugin loader, then the eclipse ui plugin loader
			URL u = updateCore.getResource(resName);
			if(u == null && eclipseUI != null)
				u = eclipseUI.getResource(resName);
				
			if (u != null)
				return u;
			else
				return super.getResource(resName);
		}
	}
	
	public InstallHandlerProxy(
		int type,
		IFeature feature,
		IInstallHandlerEntry entry,
		InstallMonitor monitor)
		throws CoreException {

		initialize(type, feature, entry, monitor);
	}

	/*
	 * @see IInstallHandler#initialize
	 */
	public void initialize(
		int type,
		IFeature feature,
		IInstallHandlerEntry entry,
		InstallMonitor monitor)
		throws CoreException {

		DEBUG = UpdateCore.DEBUG_SHOW_IHANDLER;
		// validate arguments
		if (feature == null)
			throw new IllegalArgumentException();
		this.feature = feature;
		this.type = type;

		//  check if we have a handler entry specified in the feature.xml
		if (entry == null) {
			if (DEBUG)
				debug("not specified"); //$NON-NLS-1$
			return; // no handler entry
		}

		String library = entry.getLibrary();
		String handlerName = entry.getHandlerName();
		if (handlerName == null || handlerName.trim().equals("")) { //$NON-NLS-1$
			if (DEBUG)
				debug("not specified"); //$NON-NLS-1$
			return; // no handler class spacified in entry
		}
		if (DEBUG) {
			debug("handler=" + handlerName); //$NON-NLS-1$
			debug("path=   " + library); //$NON-NLS-1$
		}

		// get handler instance
		try {
			if (library == null || library.trim().equals("")) //$NON-NLS-1$
				this.handler = getGlobalHandler(handlerName);
			else
				this.handler = getLocalHandler(library, handlerName);
			if (this.handler == null)
				return;
			handler.initialize(type, feature, entry, monitor);
		} catch (ClassNotFoundException e) {
			handleExceptionInInit(
				NLS.bind(Messages.InstallHandler_notFound, (new String[] { feature.getLabel() })),
				e);

		} catch (ClassCastException e) {
			handleExceptionInInit(
				NLS.bind(Messages.InstallHandler_invalidHandler, (new String[] { feature.getLabel() })),
				e);
		} catch (CoreException e) {
			handleExceptionInInit(null, e);
		} catch (Exception e) {
			handleExceptionInInit(
				NLS.bind(Messages.InstallHandler_unableToCreateHandler, (new String[] { feature.getLabel() })),
				e);
		}

	}

	/*
	 * @see IInstallHandler#installInitiated
	 */
	public void installInitiated() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling installInitiated()"); //$NON-NLS-1$
				handler.installInitiated();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#allPluginsDownloaded
	 */
	public void pluginsDownloaded(IPluginEntry[] plugins) throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling pluginsDownloaded()"); //$NON-NLS-1$
				handler.pluginsDownloaded(plugins);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#allPluginsInstalled
	 */
	public void completeInstall(IFeatureContentConsumer consumer)
		throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling completeInstall()"); //$NON-NLS-1$
				handler.completeInstall(consumer);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#allDataDownloaded
	 */
	public void nonPluginDataDownloaded(
		INonPluginEntry[] nonPluginData,
		IVerificationListener listener)
		throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling nonPluginDataDownloaded()"); //$NON-NLS-1$
				handler.nonPluginDataDownloaded(nonPluginData, listener);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#installCompleted
	 */
	public void installCompleted(boolean success) throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling installCompleted()"); //$NON-NLS-1$
				handler.installCompleted(success);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#configureInitiated
	 */
	public void configureInitiated() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling configureInitiated()"); //$NON-NLS-1$
				handler.configureInitiated();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#completeConfigure
	 */
	public void completeConfigure() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling completeConfigure()"); //$NON-NLS-1$
				handler.completeConfigure();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#configureCompleted
	 */
	public void configureCompleted(boolean success) throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling configureCompleted()"); //$NON-NLS-1$
				handler.configureCompleted(success);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#unconfigureInitiated
	 */
	public void unconfigureInitiated() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling unconfigureInitiated()"); //$NON-NLS-1$
				handler.unconfigureInitiated();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#completeUnconfigure
	 */
	public void completeUnconfigure() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling completeUnconfigure()"); //$NON-NLS-1$
				handler.completeUnconfigure();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#unconfigureCompleted
	 */
	public void unconfigureCompleted(boolean success) throws CoreException {
		if (handler == null) {
			if (savedStatus == null)
				return;
			else
				throw new CoreException(savedStatus); // delayed exception
		} else {
			try {
				if (DEBUG)
					debug("calling unconfigureCompleted()"); //$NON-NLS-1$
				handler.unconfigureCompleted(success);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
			if (savedStatus != null)
				throw new CoreException(savedStatus); // delayed exception
		}
	}

	/*
	 * @see IInstallHandler#uninstallInitiated
	 */
	public void uninstallInitiated() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling uninstallInitiated()"); //$NON-NLS-1$
				handler.uninstallInitiated();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#completeUninstall
	 */
	public void completeUninstall() throws CoreException {
		if (handler == null)
			return;
		else {
			try {
				if (DEBUG)
					debug("calling completeUninstall()"); //$NON-NLS-1$
				handler.completeUninstall();
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
		}
	}

	/*
	 * @see IInstallHandler#uninstallCompleted
	 */
	public void uninstallCompleted(boolean success) throws CoreException {
		if (handler == null) {
			if (savedStatus == null)
				return;
			else
				throw new CoreException(savedStatus); // delayed exception
		} else {
			try {
				if (DEBUG)
					debug("calling uninstallCompleted()"); //$NON-NLS-1$
				handler.uninstallCompleted(success);
			} catch (Throwable e) {
				handleExceptionInCall(e, feature);
			}
			if (savedStatus != null)
				throw new CoreException(savedStatus); // delayed exception
		}
	}

	/*
	 * common exception handling for initialization
	 */
	private void handleExceptionInInit(String s, Exception e)
		throws CoreException {

		CoreException ce;
		if (e instanceof CoreException)
			ce = (CoreException) e;
		else
			ce = Utilities.newCoreException(s, e);

		if (isUndoAction()) {
			// for "undo" operations, deactivate handler and log error
			String id =
				UpdateCore.getPlugin().getBundle().getSymbolicName();
			IStatus status =
				new Status(IStatus.ERROR, id, 0, "InstallHandler.deactivated", ce);	//$NON-NLS-1$
			UpdateCore.getPlugin().getLog().log(status);
			handler = null; // disable subsequent handler calls
			savedStatus = status;
		} else
			// for "do" operations, hurl ...
			throw ce;
	}

	/*
	 * common exception handling for calls to install handler
	 */
	private void handleExceptionInCall(Throwable e, IFeature feature)
		throws CoreException {

		CoreException ce;
		if (e instanceof CoreException)
			ce = (CoreException) e;
		else
			ce =
				Utilities.newCoreException(
					NLS.bind(Messages.InstallHandler_callException, (new String[] { feature.getLabel() })),
					e);
		
		if (isUndoAction()) {
			// for "undo" operations, deactivate handler and log error
			String id =
				UpdateCore.getPlugin().getBundle().getSymbolicName();
			IStatus status =
				new Status(IStatus.ERROR, id, 0, "InstallHandler.deactivated", ce);	//$NON-NLS-1$
			UpdateCore.getPlugin().getLog().log(status);
			handler = null; // disable subsequent handler calls
			savedStatus = status;
		} else
			// for "do" operations, hurl ...
			throw ce;
	}

	/*
	 * Indicates whether we are doing (install, configure) or 
	 * undoing (uninstall, unconfigure)
	 */
	private boolean isUndoAction() {
		if (this.type == IInstallHandler.HANDLER_ACTION_INSTALL
			|| this.type == IInstallHandler.HANDLER_ACTION_CONFIGURE)
			return false; // causes exception to be thrown and action aborted
		else
			return true; // causes exception to be logged and action continues
	}

	/*
	 * get an instance of handler downloaded as part of the feature
	 */
	private IInstallHandler getLocalHandler(String libs, String name)
		throws IOException, CoreException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		// Get baseline URL for handler (relative to feature.xml). For
		// features being installed from a server (eg. http protocol)
		// the URL will most likely be to a local file copy containing the
		// unpacked feature jar.
		ContentReference baseRef =
			feature.getFeatureContentProvider().getFeatureManifestReference(null);
		URL base = null;
		if (baseRef != null)
			base = baseRef.asURL();
		if (base == null)
			throw Utilities.newCoreException(
				NLS.bind(Messages.InstallHandler_unableToCreateHandler, (new String[] { this.feature.getLabel() })),
				null);


		// determine loader class path
		StringTokenizer libraries = new StringTokenizer(libs, ","); //$NON-NLS-1$
		URL[] cp = new URL[libraries.countTokens()];
		for( int token = 0; token < cp.length; token++) {
			cp[token] = new URL(base, libraries.nextToken());
		}
		if (this.type == IInstallHandler.HANDLER_ACTION_UNINSTALL) {
			// check if we are doing uninstall
			// ... need to make temp copy of library (being removed)
			URL[] jars = new URL[cp.length];
			for( int jar = 0; jar < cp.length; jar++) {
				File tempLib = File.createTempFile("tmp" + jar, ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
				tempLib.deleteOnExit();
				FileOutputStream fos = null;
				InputStream is = null;
				try {
					fos = new FileOutputStream(tempLib);
					is = new FileInputStream(cp[jar].getPath());
					Utilities.copy(is, fos, null);
				} finally {
					if (fos != null)
						try {
							fos.close();
						} catch (Exception e) {
						}
					if (is != null)
						try {
							is.close();
						} catch (Exception e) {
						}
				}
				jars[jar] = tempLib.toURL();
			}
			cp = jars;
		}

		// create class loader, load and instantiate handler
		ClassLoader loader = new InstallHandlerClassLoader(cp);
		Class clazz = loader.loadClass(name);
		IInstallHandler handler = (IInstallHandler) clazz.newInstance();
		return handler;
	}

	/*
	 * get instance of global handler registered via extension point
	 */
	private IInstallHandler getGlobalHandler(String name) throws Exception {

		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] handlerExtension =
			reg.getConfigurationElementsFor(EXT_PLUGIN, EXT_POINT, name);
		if (handlerExtension == null || handlerExtension.length <= 0)
			throw Utilities.newCoreException(
				NLS.bind(Messages.InstallHandler_unableToCreateHandler, (new String[] { this.feature.getLabel() })),
				null);

		return (IInstallHandler) handlerExtension[0].createExecutableExtension("class"); //$NON-NLS-1$
	}
	
	private void debug(String s) {
		String pfx = (feature==null) ? "" : feature.getVersionedIdentifier().toString(); //$NON-NLS-1$
		System.out.println("InstallHandler["+pfx+"]: " + s); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean acceptNonPluginData(INonPluginEntry data) {
		Boolean result = new Boolean(true);
		if (handler != null){
			if (DEBUG)
				debug("calling acceptNonPluginData()"); //$NON-NLS-1$
			if(handler instanceof IInstallHandlerWithFilter)
				return ((IInstallHandlerWithFilter)handler).acceptNonPluginData(data);
			else{ //support upgrade from legacy versions
				if(getNonPluginDataAcceptor() != null){
					try{
						Object[] param = {data};
						result = (Boolean)getNonPluginDataAcceptor().invoke(handler,param);
					}catch(Exception e){
						//todo
					}
				}
			}
		}
		return result.booleanValue();
	}
	private Method getNonPluginDataAcceptor(){
		if(nonPluginDataAcceptor == null){
			try{
				Class[] types = {INonPluginEntry.class};
				nonPluginDataAcceptor = handler.getClass().getMethod("acceptNonPluginData",types); //$NON-NLS-1$
			}catch(NoSuchMethodException nsme){
			}
		}
		return nonPluginDataAcceptor;
	}
}
