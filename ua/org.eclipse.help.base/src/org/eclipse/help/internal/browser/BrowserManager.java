/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.browser.IBrowserFactory;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

/**
 * Creates browser by delegating to appropriate browser adapter
 */
public class BrowserManager {
	public static final String DEFAULT_BROWSER_ID_KEY = "default_browser"; //$NON-NLS-1$

	public static final String BROWSER_ID_CUSTOM = HelpBasePlugin.PLUGIN_ID
			+ ".custombrowser"; //$NON-NLS-1$

	public static final String BROWSER_ID_FIREFOX = HelpBasePlugin.PLUGIN_ID
	+ ".firefox"; //$NON-NLS-1$

	public static final String BROWSER_ID_MAC_SYSTEM = HelpBasePlugin.PLUGIN_ID
			+ ".defaultBrowserMacOSX"; //$NON-NLS-1$

	public static final String BROWSER_ID_EMBEDDED = "org.eclipse.help.ui.embeddedbrowser"; //$NON-NLS-1$

	public static final String BROWSER_ID_SYSTEM = "org.eclipse.help.ui.systembrowser"; //$NON-NLS-1$

	private static BrowserManager instance;

	private boolean initialized = false;

	private BrowserDescriptor currentBrowserDesc;

	private BrowserDescriptor defaultBrowserDesc;

	private BrowserDescriptor[] browsersDescriptors;

	private BrowserDescriptor internalBrowserDesc;

	private Collection<IBrowser> browsers = new ArrayList<>();

	private boolean alwaysUseExternal = false;

	/**
	 * Private Constructor
	 */
	private BrowserManager() {
	}

	/**
	 * Initialize
	 */
	private void init() {
		initialized = true;
		// Find all available browsers
		browsersDescriptors = createBrowserDescriptors();
		// 1. set default browser from preferences
		IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		String defBrowserID = defaultPreferences.get(DEFAULT_BROWSER_ID_KEY, null);
		if (defBrowserID != null && (!"".equals(defBrowserID))) { //$NON-NLS-1$
			setDefaultBrowserID(defBrowserID);
		}
		// 2. set default browser to embedded
		//		if (defaultBrowserDesc == null) {
		//			setDefaultBrowserID(BROWSER_ID_EMBEDDED);
		//		}
		// 3. set default browser to help implementation of system specific
		// browser
		String os = Platform.getOS();
		if (defaultBrowserDesc == null) {
			if (Constants.WS_WIN32.equalsIgnoreCase(os)) {
				setDefaultBrowserID(BROWSER_ID_SYSTEM);
			} else if (Constants.OS_LINUX.equalsIgnoreCase(os)) {
				setDefaultBrowserID(BROWSER_ID_FIREFOX);
			} else if (Constants.OS_MACOSX.equalsIgnoreCase(os)) {
				setDefaultBrowserID(BROWSER_ID_MAC_SYSTEM);
			}
		}
		// 4. set browser to one of externally contributed
		if (defaultBrowserDesc == null) {
			for (int i = 0; i < browsersDescriptors.length; i++) {
				if (BROWSER_ID_CUSTOM.equals(browsersDescriptors[i].getID())) {
					defaultBrowserDesc = browsersDescriptors[i];
				}
			}
		}
		// 5. let user specify program
		if (defaultBrowserDesc == null) {
			setDefaultBrowserID(BROWSER_ID_CUSTOM);
		}
		// 6. use null browser
		if (defaultBrowserDesc == null) {
			// If no browsers at all, use the Null Browser Adapter
			defaultBrowserDesc = new BrowserDescriptor("", "Null Browser", //$NON-NLS-1$ //$NON-NLS-2$
					new IBrowserFactory() {
						@Override
						public boolean isAvailable() {
							return true;
						}

						@Override
						public IBrowser createBrowser() {
							return new IBrowser() {
								@Override
								public void close() {
								}

								@Override
								public void displayURL(String url) {
											ILog.of(getClass())
													.error(
													"There is no browser adapter configured to display " //$NON-NLS-1$
															+ url
															+ ".  Ensure that you have a required browser and adapter installed, and that the browser program is available on the system path.", //$NON-NLS-1$
													null);
									String msg = NLS.bind(HelpBaseResources.no_browsers, url);
									BaseHelpSystem.getDefaultErrorUtil()
											.displayError(msg);
								}

								@Override
								public boolean isCloseSupported() {
									return false;
								}

								@Override
								public boolean isSetLocationSupported() {
									return false;
								}

								@Override
								public boolean isSetSizeSupported() {
									return false;
								}

								@Override
								public void setLocation(int width, int height) {
								}

								@Override
								public void setSize(int x, int y) {
								}
							};
						}
					});
		}
		// initialize current browser
		String curBrowserID =
			Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, DEFAULT_BROWSER_ID_KEY, null, null);
		if (curBrowserID != null && (!"".equals(curBrowserID))) { //$NON-NLS-1$
			setCurrentBrowserID(curBrowserID);
			// may fail if such browser does not exist
		}
		if (currentBrowserDesc == null) {
			setCurrentBrowserID(getDefaultBrowserID());
		}
		setAlwaysUseExternal(
				Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_ALWAYS_EXTERNAL_BROWSER, false, null));
	}

	/**
	 * Obtains singleton instance.
	 */
	public static BrowserManager getInstance() {
		if (instance == null)
			instance = new BrowserManager();
		return instance;
	}

	/**
	 * Creates all adapters, and returns available ones.
	 */
	private BrowserDescriptor[] createBrowserDescriptors() {
		if (this.browsersDescriptors != null)
			return this.browsersDescriptors;
		Collection<BrowserDescriptor> bDescriptors = new ArrayList<>();
		IConfigurationElement configElements[] = Platform
				.getExtensionRegistry().getConfigurationElementsFor(
						HelpBasePlugin.PLUGIN_ID, "browser"); //$NON-NLS-1$
		for (int i = 0; i < configElements.length; i++) {
			if (!configElements[i].getName().equals("browser")) //$NON-NLS-1$
				continue;
			String id = configElements[i].getAttribute("id"); //$NON-NLS-1$
			if (id == null)
				continue;
			String label = configElements[i].getAttribute("name"); //$NON-NLS-1$
			if (label == null)
				continue;
			try {
				Object adapter = configElements[i]
						.createExecutableExtension("factoryclass"); //$NON-NLS-1$
				if (!(adapter instanceof IBrowserFactory))
					continue;
				if (((IBrowserFactory) adapter).isAvailable()) {
					BrowserDescriptor descriptor = new BrowserDescriptor(id,
							label, (IBrowserFactory) adapter);
					if (descriptor.isExternal()) {
						bDescriptors.add(descriptor);
					} else {
						internalBrowserDesc = descriptor;
					}
				}
			} catch (CoreException ce) {
			}
		}
		this.browsersDescriptors = bDescriptors
				.toArray(new BrowserDescriptor[bDescriptors.size()]);
		return this.browsersDescriptors;
	}

	/**
	 * Obtains browsers descriptors.
	 */
	public BrowserDescriptor[] getBrowserDescriptors() {
		if (!initialized) {
			init();
		}
		return this.browsersDescriptors;
	}

	/**
	 * Gets the currentBrowserID.
	 *
	 * @return Returns a String or null if not set
	 */
	public String getCurrentBrowserID() {
		if (!initialized) {
			init();
		}
		if (currentBrowserDesc == null)
			return null;
		return currentBrowserDesc.getID();
	}

	/**
	 * Gets the currentBrowserID.
	 *
	 * @return Returns a String or null if not set
	 */
	public String getCurrentInternalBrowserID() {
		if (!initialized) {
			init();
		}
		if (isEmbeddedBrowserPresent() && !alwaysUseExternal) {
			return internalBrowserDesc.getID();
		}
		return getCurrentBrowserID();
	}

	/**
	 * Gets the currentBrowserID.
	 *
	 * @return Returns a String or null if not set
	 */
	public String getDefaultBrowserID() {
		if (!initialized) {
			init();
		}
		if (defaultBrowserDesc == null)
			return null;
		return defaultBrowserDesc.getID();
	}

	/**
	 * Sets the currentBrowserID. If browser of given ID does not exists, the
	 * method does nothing
	 *
	 * @param currentAdapterID
	 *            The ID of the adapter to to set as current
	 */
	public void setCurrentBrowserID(String currentAdapterID) {
		if (!initialized) {
			init();
		}
		for (int i = 0; i < browsersDescriptors.length; i++) {
			if (browsersDescriptors[i].getID().equals(currentAdapterID)) {
				currentBrowserDesc = browsersDescriptors[i];
				return;
			}
		}
	}

	/**
	 * Sets the defaultBrowserID. If browser of given ID does not exists, the
	 * method does nothing
	 *
	 * @param defaultAdapterID
	 *            The ID of the adapter to to set as default
	 */
	private void setDefaultBrowserID(String defaultAdapterID) {
		if (!initialized) {
			init();
		}
		for (int i = 0; i < browsersDescriptors.length; i++) {
			if (browsersDescriptors[i].getID().equals(defaultAdapterID)) {
				defaultBrowserDesc = browsersDescriptors[i];
				return;
			}
		}
	}

	/**
	 * Creates web browser If preferences specify to always use external, the
	 * parameter will not be honored
	 */
	public IBrowser createBrowser(boolean external) {
		if (!initialized) {
			init();
		}
		//external = external || alwaysUseExternal;
		//return createBrowserAdapter(forceExternal);
		if (external) {
			return new CurrentBrowser(createBrowserAdapter(true),
					getCurrentBrowserID(), true);
		}
		return new CurrentBrowser(createBrowserAdapter(alwaysUseExternal),
				getCurrentInternalBrowserID(), false);
	}

	/**
	 * Creates web browser
	 */
	public IBrowser createBrowser() {
		return createBrowser(true);
	}

	/**
	 * Creates web browser for external == false, if no internal browsers are
	 * present it will create external one
	 */
	private IBrowser createBrowserAdapter(boolean external) {
		if (!initialized) {
			init();
		}
		IBrowser browser = null;
		if (!external && isEmbeddedBrowserPresent()) {
			browser = internalBrowserDesc.getFactory().createBrowser();
		} else {
			browser = currentBrowserDesc.getFactory().createBrowser();
		}
		browsers.add(browser);
		return browser;
	}

	/**
	 * Closes all browsers created
	 */
	public void closeAll() {
		if (!initialized) {
			// nothing to do, do not initialize
			return;
		}
		for (IBrowser browser : browsers) {
			browser.close();
		}
	}

	public boolean isEmbeddedBrowserPresent() {
		if (!initialized) {
			init();
		}
		return internalBrowserDesc != null;
	}

	public void setAlwaysUseExternal(boolean alwaysExternal) {
		if (!initialized) {
			init();
		}
		alwaysUseExternal = alwaysExternal || !isEmbeddedBrowserPresent();
	}

	public boolean isAlwaysUseExternal() {
		if (!isEmbeddedBrowserPresent()) {
			return true;
		}
		return alwaysUseExternal;
	}
}
