/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.help.internal.context.ContextManager;
import org.eclipse.help.internal.criteria.CriteriaManager;
import org.eclipse.help.internal.extension.ContentExtensionManager;
import org.eclipse.help.internal.index.IndexManager;
import org.eclipse.help.internal.toc.TocManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;

/**
 * Help System Core plug-in
 */
public class HelpPlugin extends Plugin {

	public final static String PLUGIN_ID = "org.eclipse.help"; //$NON-NLS-1$
	// debug options
	public static boolean DEBUG = false;
	public static boolean DEBUG_CONTEXT = false;
	public static boolean DEBUG_SEARCH = false;
	public static boolean DEBUG_TOC = false;
	public static boolean DEBUG_INDEX = false;
	public static boolean DEBUG_CRITERIA = false;

	public final static String HELP_DATA_KEY = "HELP_DATA"; //$NON-NLS-1$
	public final static String BASE_TOCS_KEY = "baseTOCS"; //$NON-NLS-1$
	public final static String IGNORED_TOCS_KEY = "ignoredTOCS"; //$NON-NLS-1$
	public final static String IGNORED_INDEXES_KEY = "ignoredIndexes"; //$NON-NLS-1$
	public final static String FILTER_INFOCENTER_KEY = "filterInfocenter"; //$NON-NLS-1$

	private static HelpPlugin plugin;
	private static Object tocManagerCreateLock = new Object();

	private TocManager tocManager;
	private ContextManager contextManager;
	private ContentExtensionManager contentExtensionManager;
	private IndexManager indexManager;
	private CriteriaManager criteriaManager;
	private IHelpProvider helpProvider;
	private File configurationDirectory;

	/**
	 * @return the singleton instance of the plugin
	 */
	public static HelpPlugin getDefault() {
		return plugin;
	}

	/**
	 * Used to obtain Toc Navigation Manager
	 *
	 * @return instance of TocManager
	 */
	public static TocManager getTocManager() {
		synchronized (tocManagerCreateLock) {
			if (getDefault().tocManager == null) {
				getDefault().tocManager = new TocManager();
			}
		}
		return getDefault().tocManager;
	}

	/**
	 * Used to obtain Context Manager returns an instance of ContextManager
	 */
	public static ContextManager getContextManager() {
		if (getDefault().contextManager == null)
			getDefault().contextManager = new ContextManager();
		return getDefault().contextManager;
	}

	/**
	 * Used to obtain the ContentExtensionManager
	 */
	public static ContentExtensionManager getContentExtensionManager() {
		if (getDefault().contentExtensionManager == null)
			getDefault().contentExtensionManager = new ContentExtensionManager();
		return getDefault().contentExtensionManager;
	}

	public static IndexManager getIndexManager() {
		if (getDefault().indexManager == null)
			getDefault().indexManager = new IndexManager();
		return getDefault().indexManager;
	}

	public static CriteriaManager getCriteriaManager() {
		if (getDefault().criteriaManager == null)
			getDefault().criteriaManager = new CriteriaManager();
		return getDefault().criteriaManager;
	}

	/*
	 * Returns the provider responsible for serving help documents.
	 */
	public IHelpProvider getHelpProvider() {
		return helpProvider;
	}

	/*
	 * Sets the provider responsible for serving help documents. Called
	 * on startup.
	 */
	public void setHelpProvider(IHelpProvider helpProvider) {
		this.helpProvider = helpProvider;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// determine configuration location for this plug-in
		Location location = Platform.getConfigurationLocation();
		if (location != null) {
			URL configURL = location.getURL();
			if (configURL != null && configURL.getProtocol().startsWith("file")) { //$NON-NLS-1$
				configurationDirectory = new File(configURL.getFile(), PLUGIN_ID);
			}
		}
		if (configurationDirectory == null) {
			configurationDirectory = getStateLocation().toFile();
		}
		// Setup debugging options
		DEBUG = isDebugging();
		if (DEBUG) {
			DEBUG_CONTEXT = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/context")); //$NON-NLS-1$ //$NON-NLS-2$
			DEBUG_SEARCH = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/search")); //$NON-NLS-1$ //$NON-NLS-2$
			DEBUG_TOC = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/toc")); //$NON-NLS-1$ //$NON-NLS-2$
			DEBUG_INDEX = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/index")); //$NON-NLS-1$ //$NON-NLS-2$
			DEBUG_CRITERIA = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/criteria")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static File getConfigurationDirectory() {
		return getDefault().configurationDirectory;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/*
	 * An interface by which higher plug-ins can serve help content.
	 */
	public static interface IHelpProvider {
		public InputStream getHelpContent(String href, String locale);
	}
}
