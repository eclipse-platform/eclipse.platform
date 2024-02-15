/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.configurator;

import java.io.IOException;
import java.net.URL;

import org.eclipse.update.internal.configurator.ConfigurationActivator;
import org.eclipse.update.internal.configurator.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Helper class to get platform configuration data without having to
 * use BootLoader methods from the compatibility layer.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
@Deprecated(forRemoval = true, since = "2024-03")
public class ConfiguratorUtils {
	/**
	 * Returns the current platform configuration. This method replaces the one we used
	 * to call on BootLoader.
	 *
	 * @return platform configuration used in current instance of platform
	 * @since 3.0
	 */
	public static IPlatformConfiguration getCurrentPlatformConfiguration() {
		// acquire factory service first
		BundleContext context = ConfigurationActivator.getBundleContext();
		ServiceReference<IPlatformConfigurationFactory> configFactorySR = context.getServiceReference(IPlatformConfigurationFactory.class);
		if (configFactorySR == null)
			throw new IllegalStateException();
		IPlatformConfigurationFactory configFactory = context.getService(configFactorySR);
		if (configFactory == null)
			throw new IllegalStateException();
		// get the configuration using the factory
		IPlatformConfiguration currentConfig = configFactory.getCurrentPlatformConfiguration();
		context.ungetService(configFactorySR);
		return currentConfig;
	}

	/**
	 * Returns a platform configuration object, optionally initialized with previously saved
	 * configuration information. We will use this method instead of the old one in BootLoader.
	 *
	 * @param url location of previously save configuration information. If <code>null</code>
	 * is specified, an empty configuration object is returned
	 * @return platform configuration used in current instance of platform
	 */
	public static IPlatformConfiguration getPlatformConfiguration(URL url) throws IOException {
		// acquire factory service first
		BundleContext context = ConfigurationActivator.getBundleContext();
		ServiceReference<IPlatformConfigurationFactory> configFactorySR = context.getServiceReference(IPlatformConfigurationFactory.class);
		if (configFactorySR == null)
			throw new IllegalStateException();
		IPlatformConfigurationFactory configFactory = context.getService(configFactorySR);
		if (configFactory == null)
			throw new IllegalStateException();
		// get the configuration using the factory
		IPlatformConfiguration config = configFactory.getPlatformConfiguration(url);
		context.ungetService(configFactorySR);
		return config;
	}

	/**
	 * Returns a platform configuration object, optionally initialized with previously saved
	 * configuration information. We will use this method instead of the old one in BootLoader.
	 *
	 * @param url location of previously save configuration information. If <code>null</code>
	 * is specified, an empty configuration object is returned
	 * @param loc location of the platform installation.  Used to resolve entries in the save location
	 * @return platform configuration used in current instance of platform
	 */
	public static IPlatformConfiguration getPlatformConfiguration(URL url, URL loc) throws IOException {
		// acquire factory service first
		BundleContext context = ConfigurationActivator.getBundleContext();
		ServiceReference<IPlatformConfigurationFactory> configFactorySR = context.getServiceReference(IPlatformConfigurationFactory.class);
		if (configFactorySR == null)
			throw new IllegalStateException();
		IPlatformConfigurationFactory configFactory = context.getService(configFactorySR);
		if (configFactory == null)
			throw new IllegalStateException();
		// get the configuration using the factory
		IPlatformConfiguration config = configFactory.getPlatformConfiguration(url, loc);
		context.ungetService(configFactorySR);
		return config;
	}

	/**
	 * @return the URL of this eclispe installation
	 */
	public static URL getInstallURL() {
		return Utils.getInstallURL();
	}
}
