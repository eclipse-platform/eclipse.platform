/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui;


import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.appserver.WebappManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.model.SiteLocalModel;
import org.eclipse.update.internal.ui.forms.UpdateAdapterFactory;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.AboutInfo;
import org.eclipse.update.internal.ui.preferences.*;
import org.eclipse.update.internal.ui.security.UpdateManagerAuthenticator;

/**
 * The main plugin class to be used in the desktop.
 */
public class UpdateUI extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.eclipse.update.ui";
	public static final String WEB_APP_ID = "org.eclipse.update";
	//The shared instance.
	private static UpdateUI plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	private UpdateAdapterFactory adapterFactory;
	private UpdateModel model;
	private UpdateManagerAuthenticator authenticator;
	private AboutInfo aboutInfo;
	private String appServerHost;
	private int appServerPort;
	private UpdateLabelProvider labelProvider;

	/**
	 * The constructor.
	 */
	public UpdateUI(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("org.eclipse.update.internal.ui.UpdateUIPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static UpdateUI getDefault() {
		return plugin;
	}

	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}

	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
		if (window != null)
			return window.getActivePage();
		return null;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		return window != null ? window.getShell() : null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public AboutInfo getAboutInfo() {
		return aboutInfo;
	}

	public UpdateLabelProvider getLabelProvider() {
		if (labelProvider == null)
			labelProvider = new UpdateLabelProvider();
		return labelProvider;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getString(String key) {
		ResourceBundle bundle = UpdateUI.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static String getFormattedMessage(String key, String[] args) {
		String text = getString(key);
		return java.text.MessageFormat.format(text, args);
	}

	public static String getFormattedMessage(String key, String arg) {
		String text = getString(key);
		return java.text.MessageFormat.format(text, new String[] { arg });
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	public void startup() throws CoreException {
		super.startup();
		readInfo();
		model = new UpdateModel();
		model.startup();
		IAdapterManager manager = Platform.getAdapterManager();
		adapterFactory = new UpdateAdapterFactory();
		manager.registerAdapters(adapterFactory, UIModelObject.class);
		authenticator = new UpdateManagerAuthenticator();
		Authenticator.setDefault(authenticator);
		int historyPref = getPluginPreferences().getInt(MainPreferencePage.P_HISTORY_SIZE);
		if (historyPref > 0) {
			SiteLocalModel.DEFAULT_HISTORY = historyPref;
		}
		if (AppServerPreferencePage.getUseApplicationServer()) {
			try {
				startWebApp();
			} catch (CoreException e) {
				logException(e);
			}
		}
	}

	public void startWebApp() throws CoreException {

		// configure web install handler
		try {
			WebappManager.start(WEB_APP_ID, PLUGIN_ID, new Path("webapp"));
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return;
		}
		
		appServerHost = WebappManager.getHost();
		appServerPort = WebappManager.getPort();
	}

	public void stopWebApp() throws CoreException {
		try {
			// unconfigure web install handler
			WebappManager.stop(WEB_APP_ID);
		} finally {
			appServerHost = null;
			appServerPort = 0;
		}
	}

	public boolean isWebAppStarted() {
		return appServerHost != null;
	}

	public String getAppServerHost() {
		return appServerHost;
	}

	public int getAppServerPort() {
		return appServerPort;
	}

	public void shutdown() throws CoreException {
		IAdapterManager manager = Platform.getAdapterManager();
		manager.unregisterAdapters(adapterFactory);
		model.shutdown();
		UpdateColors.disposeColors();
		if (labelProvider != null)
			labelProvider.dispose();
		super.shutdown();

	}

	public UpdateModel getUpdateModel() {
		return model;
	}

	public static void logException(Throwable e) {
		logException(e, true);
	}

	public static void logException(Throwable e, boolean showErrorDialog) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}

		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			String message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		log(status, showErrorDialog);
	}

	public static void log(IStatus status, boolean showErrorDialog) {
		if (status.getSeverity() != IStatus.INFO) {
			if (showErrorDialog)
				ErrorDialog.openError(getActiveWorkbenchShell(), null, null, status);
			//ResourcesPlugin.getPlugin().getLog().log(status);
			Platform.getPlugin("org.eclipse.core.runtime").getLog().log(status);
		} else {
			MessageDialog.openInformation(getActiveWorkbenchShell(), null, status.getMessage());
		}
	}

	public static IFeature[] searchSite(String featureId, IConfiguredSite site, boolean onlyConfigured) throws CoreException {
		IFeatureReference[] references = null;

		if (onlyConfigured)
			references = site.getConfiguredFeatures();
		else
			references = site.getSite().getFeatureReferences();
		Vector result = new Vector();

		for (int i = 0; i < references.length; i++) {
			IFeature feature = references[i].getFeature(null);
			String id = feature.getVersionedIdentifier().getIdentifier();
			if (featureId.equals(id)) {
				result.add(feature);
			}
		}
		return (IFeature[]) result.toArray(new IFeature[result.size()]);
	}

	public static IFeature[] getInstalledFeatures(IFeature feature) {
		return getInstalledFeatures(feature, true);
	}

	public static IFeature[] getInstalledFeatures(IFeature feature, boolean onlyConfigured) {
		return getInstalledFeatures(feature.getVersionedIdentifier(), onlyConfigured);
	}
	
	public static IFeature[] getInstalledFeatures(VersionedIdentifier vid, boolean onlyConfigured) {
		Vector features = new Vector();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] isites = config.getConfiguredSites();
			String id = vid.getIdentifier();

			for (int i = 0; i < isites.length; i++) {
				IConfiguredSite isite = isites[i];
				IFeature[] result = UpdateUI.searchSite(id, isite, onlyConfigured);
				for (int j = 0; j < result.length; j++) {
					IFeature installedFeature = result[j];
					features.add(installedFeature);
				}
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
		return (IFeature[]) features.toArray(new IFeature[features.size()]);
	}
	
	public static boolean isPatch(IFeature candidate) {
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) return true;
		}
		return false;
	}

	public static boolean isPatch(IFeature target, IFeature candidate) {
		VersionedIdentifier vid = target.getVersionedIdentifier();
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				VersionedIdentifier ivid = iimport.getVersionedIdentifier();
				if (vid.equals(ivid)) {
					// Bingo.
					return true;
				}
			}
		}
		return false;
	}

	public static IInstallConfiguration getBackupConfigurationFor(IFeature feature) {
		VersionedIdentifier vid = feature.getVersionedIdentifier();
		String key = "@" + vid.getIdentifier() + "_" + vid.getVersion();
		try {
			ILocalSite lsite = SiteManager.getLocalSite();
			IInstallConfiguration[] configs = lsite.getPreservedConfigurations();
			for (int i = 0; i < configs.length; i++) {
				IInstallConfiguration config = configs[i];
				if (config.getLabel().startsWith(key))
					return config;
			}
		} catch (CoreException e) {
		}
		return null;
	}

	/**
	 * Gets the authenticator.
	 * @return Returns a UpdateManagerAuthenticator
	 */
	public UpdateManagerAuthenticator getAuthenticator() {
		return authenticator;
	}

	public static URL getOriginatingURL(String id) {
		IDialogSettings section = getOriginatingURLSection();
		String value = section.get(id);
		if (value != null) {
			try {
				return new URL(value);
			} catch (MalformedURLException e) {
			}
		}
		return null;
	}

	public static void setOriginatingURL(String id, URL url) {
		IDialogSettings section = getOriginatingURLSection();
		section.put(id, url.toString());
	}

	private static IDialogSettings getOriginatingURLSection() {
		IDialogSettings settings = getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection("originatingURLs");
		if (section == null)
			section = settings.addNewSection("originatingURLs");
		return section;
	}

	private void readInfo() {
		// determine the identifier of the "dominant" application 
		IPlatformConfiguration conf = BootLoader.getCurrentPlatformConfiguration();
		String versionedFeatureId = conf.getPrimaryFeatureIdentifier();

		if (versionedFeatureId == null) {
			aboutInfo = new AboutInfo(null, null); // Ok to pass null
		} else {
			int index = versionedFeatureId.lastIndexOf("_"); //$NON-NLS-1$
			if (index == -1)
				aboutInfo = new AboutInfo(versionedFeatureId, null);
			else {
				String mainPluginName = versionedFeatureId.substring(0, index);
				PluginVersionIdentifier mainPluginVersion = null;
				try {
					mainPluginVersion = new PluginVersionIdentifier(versionedFeatureId.substring(index + 1));
				} catch (Exception e) {
					IStatus iniStatus = new Status(IStatus.ERROR, WorkbenchPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, "Unknown plugin version " + versionedFeatureId, e); //$NON-NLS-1$
					log(iniStatus, true); //$NON-NLS-1$
				}
				aboutInfo = new AboutInfo(mainPluginName, mainPluginVersion);
			}
		}
		try {
			aboutInfo.readINIFile();
		} catch (CoreException e) {
			log(e.getStatus(), true); //$NON-NLS-1$
		}
	}
	public static void informRestartNeeded() {
		String title = UpdateUI.getString("RestartTitle");
		String message = UpdateUI.getString("RestartMessage");
		boolean restart = MessageDialog.openQuestion(getActiveWorkbenchShell(), title, message);
		if (restart)
			PlatformUI.getWorkbench().restart();
	}
	/** 
	 * Initializes a preference store with default preference values 
	 * for this plug-in.
	 * <p>
	 * This method is called after the preference store is initially loaded
	 * (default values are never stored in preference stores).
	 * </p>
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses should reimplement this method if the plug-in has any preferences.
	 * </p>
	 * <p>
	 * A subclass may reimplement this method to set default values for the 
	 * preference store using JFace API. This is the older way of initializing 
	 * default values. If this method is reimplemented, do not override
	 * <code>initializeDefaultPluginPreferences()</code>.
	 * </p>
	 *
	 * @param store the preference store to fill
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(MainPreferencePage.P_CHECK_SIGNATURE, true);		
		store.setDefault(MainPreferencePage.P_HISTORY_SIZE, 50);
		store.setDefault(MainPreferencePage.P_BROWSER, MainPreferencePage.EMBEDDED_VALUE);
		store.setDefault(MainPreferencePage.P_UPDATE_VERSIONS, MainPreferencePage.EQUIVALENT_VALUE);
		store.setDefault(AppServerPreferencePage.P_MASTER_SWITCH, false);
		store.setDefault(AppServerPreferencePage.P_ENCODE_URLS, true);
		UpdateColors.setDefaults(store);
	}
}
