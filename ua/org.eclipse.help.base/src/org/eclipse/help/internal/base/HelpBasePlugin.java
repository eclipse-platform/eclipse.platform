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
package org.eclipse.help.internal.base;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;

/**
 * Help Base plug-in.
 */
public class HelpBasePlugin extends Plugin {

	public final static String PLUGIN_ID = "org.eclipse.help.base"; //$NON-NLS-1$
	private static HelpBasePlugin plugin;
	private File configurationDirectory;
	private BundleContext context;

	private IHelpActivitySupport helpActivitySupport = new IHelpActivitySupport() {
		@Override
		public boolean isEnabled(String href) {
			return true;
		}
		@Override
		public boolean isRoleEnabled(String href) {
			return true;
		}
		@Override
		public boolean isEnabledTopic(String href, String locale) {
			return true;
		}
		@Override
		public void enableActivities(String href) {
		}
		@Override
		public boolean isFilteringEnabled() {
			return false;
		}
		@Override
		public void setFilteringEnabled(boolean enabled) {
		}
		@Override
		public boolean isUserCanToggleFiltering() {
			return false;
		}
		@Override
		public String getShowAllMessage() {
			return null;
		}
		@Override
		public String getDocumentMessage(boolean embedded) {
			return null;
		}
		@Override
		public boolean getDocumentMessageUsesLiveHelp(boolean embedded) {
			return false;
		}
	};


	public static HelpBasePlugin getDefault() {
		return plugin;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		BaseHelpSystem.shutdown();
		this.context = null;
		plugin = null;
		super.stop(context);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.context = context;

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
		BaseHelpSystem.startup();
	}

	public static File getConfigurationDirectory() {
		return getDefault().configurationDirectory;
	}

	public static IHelpActivitySupport getActivitySupport() {
		return getDefault().helpActivitySupport;
	}

	public static void setActivitySupport(IHelpActivitySupport activitySupport) {
		getDefault().helpActivitySupport = activitySupport;
	}

	public static BundleContext getBundleContext() {
		return getDefault().context;
	}
}
