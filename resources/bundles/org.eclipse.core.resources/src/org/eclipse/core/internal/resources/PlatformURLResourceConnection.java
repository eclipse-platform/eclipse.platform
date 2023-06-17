/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import org.eclipse.core.internal.boot.PlatformURLConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;

/**
 * Platform URL support {@literal platform:/resource/<path>/<resource>} maps to
 * resource in current workspace
 */
public class PlatformURLResourceConnection extends PlatformURLConnection {

	// resource/ protocol
	public static final String RESOURCE = "resource"; //$NON-NLS-1$
	public static final String RESOURCE_URL_STRING = PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + '/' + RESOURCE + '/';
	private static URL rootURL;

	public PlatformURLResourceConnection(URL url) {
		super(url);
	}

	@Override
	protected boolean allowCaching() {
		return false; // don't cache, workspace is local
	}

	@Override
	protected URL resolve() throws IOException {
		String filePath = url.getFile().trim();
		filePath = URLDecoder.decode(filePath, "UTF-8"); //$NON-NLS-1$
		IPath spec = IPath.fromOSString(filePath).makeRelative();
		if (!spec.segment(0).equals(RESOURCE))
			throw new IOException(NLS.bind(Messages.url_badVariant, url));
		int count = spec.segmentCount();
		// if there is only one segment then we are talking about the workspace root.
		if (count == 1)
			return rootURL;
		// if there are two segments then the second is a project name.
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(spec.segment(1));
		if (!project.exists()) {
			String message = NLS.bind(Messages.url_couldNotResolve_projectDoesNotExist, project.getName(), url.toExternalForm());
			throw new IOException(message);
		}

		IResource resource = null;
		if (count == 2) {
			resource = project;
		} else {
			spec = spec.removeFirstSegments(2);
			resource = project.getFile(spec);
		}

		IPath result = resource.getLocation();

		if (result == null) {
			URI uri = resource.getLocationURI();
			if (uri != null) {
				try {
					URL url2 = uri.toURL();
					if (url2.getProtocol().equals("file")) //$NON-NLS-1$
						return url2;
				} catch (MalformedURLException e) {
					String message = NLS.bind(Messages.url_couldNotResolve_URLProtocolHandlerCanNotResolveURL, uri.toString(), url.toExternalForm());
					throw new IOException(message);
				}
			}
			String message = NLS.bind(Messages.url_couldNotResolve_resourceLocationCanNotBeDetermined, resource.getFullPath(), url.toExternalForm());
			throw new IOException(message);
		}
		return new URL("file", "", result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * This method is called during resource plugin startup() initialization.
	 *
	 * @param root URL to the root of the current workspace.
	 */
	public static void startup(IPath root) {
		// register connection type for platform:/resource/ handling
		if (rootURL != null)
			return;
		try {
			rootURL = root.toFile().toURL();
		} catch (MalformedURLException e) {
			// should never happen but if it does, the resource URL cannot be supported.
			return;
		}
		PlatformURLHandler.register(RESOURCE, PlatformURLResourceConnection.class);
	}
}
