/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.update.internal.configurator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfigurationFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@SuppressWarnings("deprecation")
@Component(service = IPlatformConfigurationFactory.class)
public class PlatformConfigurationFactory implements IPlatformConfigurationFactory {
	private Location configLocation;

	@Override
	public IPlatformConfiguration getCurrentPlatformConfiguration() {
		try {
			PlatformConfiguration.startup(configLocation);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null)
				message = ""; //$NON-NLS-1$
			Utils.log(Utils.newStatus(message, e));
		}
		return PlatformConfiguration.getCurrent();
	}

	@Override
	public IPlatformConfiguration getPlatformConfiguration(URL url)	throws IOException {
		try {
			return new PlatformConfiguration(url);
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException)e;
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public IPlatformConfiguration getPlatformConfiguration(URL url, URL loc) throws IOException {
		try {
			return new PlatformConfiguration(url, loc);
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException)e;
			throw new IOException(e.getMessage());
		}
	}

	@Activate
	public void startup() {
		configLocation = Utils.getConfigurationLocation();
		// create the name space directory for update (configuration/org.eclipse.update)
		if (!configLocation.isReadOnly()) {
			try {
				URL privateURL = new URL(configLocation.getURL(), ConfigurationActivator.NAME_SPACE);
				File f = new File(privateURL.getFile());
				if (!f.exists())
					f.mkdirs();
			} catch (MalformedURLException e1) {
				// ignore
			}
		}
	}

	@Deactivate
	public void shutdown() {
		try {
			PlatformConfiguration.shutdown();
		} catch (IOException e) {
		}
	}

}
