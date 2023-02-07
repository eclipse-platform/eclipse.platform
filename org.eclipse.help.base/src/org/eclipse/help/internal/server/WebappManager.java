/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.server.HelpServer;

public class WebappManager {

	private static HelpServer server;
	private static final String SERVER_EXTENSION_ID = "org.eclipse.help.base.server"; //$NON-NLS-1$
	private static final String SERVER_CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$

	private static HelpServer getHelpServer() {
		if (server == null) {
			createWebappServer();
		}
		if (server == null) {
			server = new JettyHelpServer();
		}
		return server;
	}

	public static void start(String webappName) throws Exception {
		getHelpServer().start(webappName);
	}

	public static void stop(String webappName) throws CoreException {
		getHelpServer().stop(webappName);
	}

	public static int getPort() {
		return getHelpServer().getPort();
	}

	public static String getHost() {
		return getHelpServer().getHost();
	}

	private static void createWebappServer() {
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint(SERVER_EXTENSION_ID );
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			if (extensions.length != 0) {
				// We need to pick up the non-default configuration
				IConfigurationElement[] elements = extensions[0]
						.getConfigurationElements();
				if (elements.length == 0)
					return;
				IConfigurationElement serverElement  = elements[0];
				// Instantiate the app server
				try {
					server = (HelpServer) (serverElement
							.createExecutableExtension(SERVER_CLASS_ATTRIBUTE));
				} catch (CoreException e) {
					Platform.getLog(WebappManager.class).log(e.getStatus());
				}
			}
		}
	}

}
