/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.base;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.ILiveHelpAction;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.util.IErrorUtil;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.help.internal.search.LocalSearchManager;
import org.eclipse.help.internal.search.SearchManager;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.help.internal.workingset.WorkingSetManager;
import org.osgi.framework.Bundle;

/**
 * Base Help System.
 */
public final class BaseHelpSystem {

	private static final BaseHelpSystem instance = new BaseHelpSystem();

	public static final String BOOKMARKS = "bookmarks"; //$NON-NLS-1$
	public static final String WORKING_SETS = "workingSets"; //$NON-NLS-1$
	public static final String WORKING_SET = "workingSet"; //$NON-NLS-1$

	public static final int MODE_WORKBENCH = 0;
	public static final int MODE_INFOCENTER = 1;
	public static final int MODE_STANDALONE = 2;

	private int mode = MODE_WORKBENCH;

	private SearchManager searchManager;
	private WorkingSetManager workingSetManager;
	private BookmarkManager bookmarkManager;

	private boolean webappStarted = false;
	private boolean webappRunning = false;
	private IErrorUtil defaultErrorMessenger;
	private IBrowser browser;
	private IBrowser internalBrowser;
	private HelpDisplay helpDisplay = null;
	private String liveHelpToken = null;

	private BaseHelpSystem() {
		super();
	}

	public static BaseHelpSystem getInstance() {
		return instance;
	}

	/*
	 * Returns the singleton search manager, which is the main interface to the
	 * help system's search capability.
	 */
	public static SearchManager getSearchManager() {
		synchronized (BaseHelpSystem.class) {
			if (getInstance().searchManager == null) {
				getInstance().searchManager = new SearchManager();
			}
		}
		return getInstance().searchManager;
	}

	/*
	 * Returns the local search manager which deals only with the local content
	 * and is called by the global search manager.
	 */
	public static LocalSearchManager getLocalSearchManager() {
		return getSearchManager().getLocalSearchManager();
	}

	public static synchronized WorkingSetManager getWorkingSetManager() {
		if (getInstance().workingSetManager == null) {
			getInstance().workingSetManager = new WorkingSetManager();
		}
		return getInstance().workingSetManager;
	}

	public static synchronized BookmarkManager getBookmarkManager() {
		if (getInstance().bookmarkManager == null) {
			getInstance().bookmarkManager = new BookmarkManager();
		}
		return getInstance().bookmarkManager;
	}

	/*
	 * Allows Help UI to plug-in a soft adapter that delegates all the work to
	 * the workbench browser support.
	 */
	public synchronized void setBrowserInstance(IBrowser browser) {
		this.browser = browser;
	}

	public static synchronized IBrowser getHelpBrowser(boolean forceExternal) {
		if (!forceExternal && !BrowserManager.getInstance().isAlwaysUseExternal()) {
			if (getInstance().internalBrowser == null) {
				getInstance().internalBrowser = BrowserManager.getInstance().createBrowser(false);
			}
			return getInstance().internalBrowser;
		}
		if (getInstance().browser == null) {
			getInstance().browser = BrowserManager.getInstance().createBrowser(true);
		}
		return getInstance().browser;
	}

	public static synchronized HelpDisplay getHelpDisplay() {
		if (getInstance().helpDisplay == null)
			getInstance().helpDisplay = new HelpDisplay();
		return getInstance().helpDisplay;
	}

	/*
	 * Shuts down the BaseHelpSystem.
	 */
	public static void shutdown() throws CoreException {
		if (getInstance().bookmarkManager != null) {
			getInstance().bookmarkManager.close();
			getInstance().bookmarkManager = null;
		}
		if (getInstance().searchManager != null) {
			getInstance().searchManager.close();
			getInstance().searchManager = null;
		}
		if (getInstance().webappStarted) {
			// stop the web app
			WebappManager.stop("help"); //$NON-NLS-1$
		}
	}

	/**
	 * Called by Platform after loading the plugin
	 */
	public static void startup() {
		try {
			setDefaultErrorUtil(new IErrorUtil() {
				@Override
				public void displayError(String msg) {
					System.out.println(msg);
				}
				@Override
				public void displayError(String msg, Thread uiThread) {
					System.out.println(msg);
				}
			});
		}
		catch (Exception e) {
			Platform.getLog(BaseHelpSystem.class).error("Error launching help.", e); //$NON-NLS-1$
		}

		/*
		 * Assigns the provider responsible for providing help
		 * document content.
		 */
		HelpPlugin.getDefault().setHelpProvider(new HelpProvider());
	}

	public static boolean ensureWebappRunning() {
		if (!getInstance().webappStarted) {
			getInstance().webappStarted = true;
			try {
				// start the help web app
				WebappManager.start("help"); //$NON-NLS-1$
			} catch (Exception e) {
				Platform.getLog(BaseHelpSystem.class).error(HelpBaseResources.HelpWebappNotStarted, e);
				return false;
			}
			getInstance().webappRunning = true;
		}
		return getInstance().webappRunning;
	}

	public static URL resolve(String href, boolean documentOnly) {
		String url = null;
		if (href == null || href.contains("://") //$NON-NLS-1$
					|| isFileProtocol(href))
			url = href;
		else {
			BaseHelpSystem.ensureWebappRunning();
			String base = getBase(documentOnly);
			if (href.startsWith("/")) //$NON-NLS-1$
				url = base + href;
			else
				url = base + "/" + href; //$NON-NLS-1$
		}
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public static URL resolve(String href, String servlet) {
		String url = null;
		if (href == null || href.contains("://") //$NON-NLS-1$
			|| isFileProtocol(href)) {
			url = href;
		}
		else {
			BaseHelpSystem.ensureWebappRunning();
			String base = getBase(servlet);
			if (href.startsWith("/")) { //$NON-NLS-1$
				url = base + href;
			}
			else {
				url = base + "/" + href; //$NON-NLS-1$
			}
		}
		try {
			return new URL(url);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	private static boolean isFileProtocol(String href) {
		// Test for file: or /file:
		int index = href.indexOf("file:"); //$NON-NLS-1$
		return ( index == 0 ||  (index == 1 && href.charAt(0) == '/' ));
	}

	public static String unresolve(URL url) {
		return unresolve(url.toString());
	}

	public static String unresolve(String href) {
		String[] baseVariants = { getBase("/help/topic"), //$NON-NLS-1$
				getBase("/help/nftopic"),  //$NON-NLS-1$
				getBase("/help/ntopic"),  //$NON-NLS-1$
				getBase("/help/rtopic") }; //$NON-NLS-1$
		for (String baseVariant : baseVariants) {
			if (href.startsWith(baseVariant)) {
				return href.substring(baseVariant.length());
			}
		}
		return href;
	}

	private static String getBase(boolean documentOnly) {
		String servlet = documentOnly ? "/help/nftopic" : "/help/topic";//$NON-NLS-1$ //$NON-NLS-2$
		return getBase(servlet);
	}

	private static String getBase(String servlet) {
		return "http://" //$NON-NLS-1$
				+ WebappManager.getHost() + ":" //$NON-NLS-1$
				+ WebappManager.getPort() + servlet;
	}

	/*
	 * Returns the mode of operation.
	 */
	public static int getMode() {
		return getInstance().mode;
	}

	/*
	 * Sets the mode of operation.
	 */
	public static void setMode(int mode) {
		getInstance().mode = mode;
		HelpSystem.setShared(mode == MODE_INFOCENTER);
	}

	/*
	 * Sets the error messenger
	 */
	public static void setDefaultErrorUtil(IErrorUtil em) {
		getInstance().defaultErrorMessenger = em;
	}

	/*
	 * Returns the default error messenger. When no UI is present, all errors
	 * are sent to System.out.
	 */
	public static IErrorUtil getDefaultErrorUtil() {
		return getInstance().defaultErrorMessenger;
	}

	/**
	 * Obtains name of the Eclipse product
	 *
	 * @return String
	 */
	public static String getProductName() {
		IProduct product = Platform.getProduct();
		if (product == null) {
			return ""; //$NON-NLS-1$
		}
		String name = product.getName();
		return name == null ? "" : name; //$NON-NLS-1$
	}

	public static void runLiveHelp(String pluginID, String className, String arg) {
		Bundle bundle = Platform.getBundle(pluginID);
		if (bundle == null) {
			return;
		}

		try {
			Class<?> c = bundle.loadClass(className);
			Object o = c.getDeclaredConstructor().newInstance();
			//Runnable runnable = null;
			if (o instanceof ILiveHelpAction) {
				ILiveHelpAction helpExt = (ILiveHelpAction) o;
				if (arg != null)
					helpExt.setInitializationString(arg);
				Thread runnableLiveHelp = new Thread(helpExt);
				runnableLiveHelp.setDaemon(true);
				runnableLiveHelp.start();
			}
		} catch (ThreadDeath td) {
			throw td;
		} catch (Exception e) {
		}
	}

	/**
	 * Called when index.jsp is opened, check to see if we index.jsp is running outside out server in which
	 * case set the mode to infocenter
	 */
	public static void checkMode() {
		if (!getInstance().webappStarted) {
			setMode(MODE_INFOCENTER);
		}
	}

	/**
	 * Check supplied token against stored token. Clears the stored token if
	 * successful.
	 *
	 * @param helpSessionToken
	 * @return true if match successful
	 */
	public boolean matchOnceLiveHelpToken(String helpSessionToken) {
		/*
		 * @FIXME - should we use a constant time comparison, and store/compare a
		 * cryptographic hash?
		 */
		if (liveHelpToken != null && liveHelpToken.equals(helpSessionToken)) {
			// Enforce one-time use.
			liveHelpToken = null;
			return true;
		} else {
			return false;
		}
	}

	public void setLiveHelpToken(String helpSessionToken) {
		liveHelpToken = helpSessionToken;
	}

}
