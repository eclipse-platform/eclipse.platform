package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IInstallHandler;
import org.eclipse.update.core.IInstallHandlerEntry;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.InstallMonitor;
import org.eclipse.update.core.Utilities;

public class InstallHandlerProxy implements IInstallHandler {

	private IFeature feature = null;
	private int type;
	private IInstallHandler handler = null;
	private IStatus savedStatus = null;
	private boolean DEBUG = false;

	private static final String EXT_PLUGIN = "org.eclipse.update.core";
	private static final String EXT_POINT = "installHandlers";

	private InstallHandlerProxy() {
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

		DEBUG = UpdateManagerPlugin.DEBUG_SHOW_IHANDLER;
		// validate arguments
		if (feature == null)
			throw new IllegalArgumentException();
		this.feature = feature;
		this.type = type;

		//  check if we have a handler entry specified in the feature.xml
		if (entry == null) {
			if (DEBUG)
				debug("not specified");
			return; // no handler entry
		}

		String library = entry.getLibrary();
		String handlerName = entry.getHandlerName();
		if (handlerName == null || handlerName.trim().equals("")) {
			if (DEBUG)
				debug("not specified");
			return; // no handler class spacified in entry
		}
		if (DEBUG) {
			debug("handler=" + handlerName);
			debug("path=   " + library);
		}

		// get handler instance
		try {
			if (library == null || library.trim().equals(""))
				this.handler = getGlobalHandler(handlerName);
			else
				this.handler = getLocalHandler(library, handlerName);
			if (this.handler == null)
				return;
			handler.initialize(type, feature, entry, monitor);
		} catch (ClassNotFoundException e) {
			handleExceptionInInit(
				Policy.bind("InstallHandler.notFound", feature.getLabel()),
				e);
			//$NON-NLS-1$
		} catch (ClassCastException e) {
			handleExceptionInInit(
				Policy.bind("InstallHandler.invalidHandler", feature.getLabel()),
				e);
			//$NON-NLS-1$
		} catch (CoreException e) {
			handleExceptionInInit(null, e);
		} catch (Exception e) {
			handleExceptionInInit(
				Policy.bind("InstallHandler.unableToCreateHandler", feature.getLabel()),
				e);
			//$NON-NLS-1$
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
					debug("calling installInitiated()");
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
					debug("calling pluginsDownloaded()");
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
					debug("calling completeInstall()");
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
					debug("calling nonPluginDataDownloaded()");
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
					debug("calling installCompleted()");
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
					debug("calling configureInitiated()");
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
					debug("calling completeConfigure()");
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
					debug("calling configureCompleted()");
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
					debug("calling unconfigureInitiated()");
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
					debug("calling completeUnconfigure()");
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
					debug("calling unconfigureCompleted()");
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
					debug("calling uninstallInitiated()");
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
					debug("calling completeUninstall()");
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
					debug("calling uninstallCompleted()");
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
				UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status =
				new Status(IStatus.ERROR, id, 0, "InstallHandler.deactivated", ce);
			//$NON-NLS-1$
			UpdateManagerPlugin.getPlugin().getLog().log(status);
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
					Policy.bind("InstallHandler.callException", feature.getLabel()),
					e);
		//$NON-NLS-1$
		
		if (isUndoAction()) {
			// for "undo" operations, deactivate handler and log error
			String id =
				UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status =
				new Status(IStatus.ERROR, id, 0, "InstallHandler.deactivated", ce);
			//$NON-NLS-1$
			UpdateManagerPlugin.getPlugin().getLog().log(status);
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
	private IInstallHandler getLocalHandler(String lib, String name)
		throws Exception {

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
				Policy.bind("InstallHandler.unableToCreateHandler", this.feature.getLabel()),
				null);
		//$NON-NLS-1$

		// determine loader class path
		URL cp = new URL(base, lib);
		if (this.type == IInstallHandler.HANDLER_ACTION_UNINSTALL) {
			// check if we are doing uninstall
			// ... need to make temp copy of library (being removed)
			File tempLib = File.createTempFile("tmp", ".jar");
			tempLib.deleteOnExit();
			FileOutputStream fos = null;
			InputStream is = null;
			try {
				fos = new FileOutputStream(tempLib);
				is = UpdateManagerPlugin.getPlugin().get(cp).getInputStream();
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
			cp = tempLib.toURL();
		}

		// create class loader, load and instantiate handler
		URLClassLoader loader =
			new URLClassLoader(new URL[] { cp }, this.getClass().getClassLoader());
		Class clazz = loader.loadClass(name);
		IInstallHandler handler = (IInstallHandler) clazz.newInstance();
		return handler;
	}

	/*
	 * get instance of global handler registered via extension point
	 */
	private IInstallHandler getGlobalHandler(String name) throws Exception {

		IPluginRegistry reg = Platform.getPluginRegistry();
		IConfigurationElement[] handlerExtension =
			reg.getConfigurationElementsFor(EXT_PLUGIN, EXT_POINT, name);
		if (handlerExtension == null || handlerExtension.length <= 0)
			throw Utilities.newCoreException(
				Policy.bind("InstallHandler.unableToCreateHandler", this.feature.getLabel()),
				null);
		//$NON-NLS-1$	

		return (IInstallHandler) handlerExtension[0].createExecutableExtension("class");
	}
	
	private void debug(String s) {
		String pfx = (feature==null) ? "" : feature.getVersionedIdentifier().toString();
		System.out.println("InstallHandler["+pfx+"]: " + s);
	}
}