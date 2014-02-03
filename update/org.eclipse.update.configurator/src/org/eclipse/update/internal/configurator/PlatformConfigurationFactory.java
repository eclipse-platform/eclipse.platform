/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.IOException;
import java.net.URL;

import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfigurationFactory;

public class PlatformConfigurationFactory implements IPlatformConfigurationFactory {
	public IPlatformConfiguration getCurrentPlatformConfiguration() {
		return PlatformConfiguration.getCurrent();
	}
	
	public IPlatformConfiguration getPlatformConfiguration(URL url)	throws IOException {
		try {
			return new PlatformConfiguration(url);
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException)e;
			throw new IOException(e.getMessage());
		}
	}
	
	public IPlatformConfiguration getPlatformConfiguration(URL url, URL loc) throws IOException {
		try {
			return new PlatformConfiguration(url, loc);
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException)e;
			throw new IOException(e.getMessage());
		}
	}
	
}
