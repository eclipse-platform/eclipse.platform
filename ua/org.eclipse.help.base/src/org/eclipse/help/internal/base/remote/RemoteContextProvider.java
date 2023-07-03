/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.AbstractContextProvider;
import org.eclipse.help.IContext;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.context.Context;
import org.eclipse.help.internal.dynamic.DocumentReader;

/*
 * Provides the context-sensitive help data that is located on the remote infocenter for a
 * particular id, if the system is configured for remote help. If not, returns null.
 */
public class RemoteContextProvider extends AbstractContextProvider {

	private static final String PATH_CONTEXT = "/context"; //$NON-NLS-1$
	private static final String PARAM_ID = "id"; //$NON-NLS-1$
	private static final String PARAM_LANG = "lang"; //$NON-NLS-1$
	private static final String PROTOCOL = "http"; //$NON-NLS-1$

	private DocumentReader reader;

	@Override
	public IContext getContext(String id, String locale) {

		PreferenceFileHandler prefHandler = new PreferenceFileHandler();
		String host[] = prefHandler.getHostEntries();
		String port[] = prefHandler.getPortEntries();
		String path[] = prefHandler.getPathEntries();
		String [] protocols = prefHandler.getProtocolEntries();
		String isEnabled[] = prefHandler.isEnabled();

		// InfoCenters ignore remote content
		if (RemoteHelp.isEnabled()) {

			int numICs = host.length; // Total number of hosts
			//Loop through remote InfoCenters and return first CSH match found
			URL url=null;
			for (int i = 0; i < numICs; i++) {

				if (isEnabled[i].equals("true")) { //$NON-NLS-1$
					@SuppressWarnings("resource")
					InputStream in = null;
					try {

						HttpURLConnection connection;
						if(protocols[i].equals(PROTOCOL))
						{
							url = new URL(PROTOCOL, host[i], Integer.parseInt(port[i]), path[i]+ PATH_CONTEXT + '?' + PARAM_ID + '=' + id + '&' + PARAM_LANG + '=' + locale);
							connection = (HttpURLConnection)ProxyUtil.getConnection(url);
							if (connection.getResponseCode() == 200) {
								in = connection.getInputStream();
							}
						}
						else
						{
							url = HttpsUtility.getHttpsURL(protocols[i], host[i], port[i], path[i]+ PATH_CONTEXT + '?' + PARAM_ID + '=' + id + '&' + PARAM_LANG + '=' + locale);
							in = HttpsUtility.getHttpsStream(url);
						}

						if (reader == null) {
							reader = new DocumentReader();
						}
						return (Context) reader.read(in);

					} catch (IOException e) {
						String msg = "I/O error while trying to contact the remote help server"; //$NON-NLS-1$
						ILog.of(getClass()).error(msg, e);
					} catch (Throwable t) {
						String msg = "Internal error while reading search results from remote server"; //$NON-NLS-1$
						ILog.of(getClass()).error(msg, t);
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException e) {
								// nothing more we can do
							}
						}
					}
				}
			}
		}


		return null;
	}

	@Override
	public String[] getPlugins() {
		// this is a global provider
		return null;
	}
}
