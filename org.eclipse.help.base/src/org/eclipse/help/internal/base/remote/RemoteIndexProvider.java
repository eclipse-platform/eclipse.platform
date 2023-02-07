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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.IIndexContribution;
import org.eclipse.help.internal.base.util.ProxyUtil;

/*
 * Provides the TOC data that is located on the remote infocenter, if the system
 * is configured for remote help. If not, returns no contributions.
 */
public class RemoteIndexProvider extends AbstractIndexProvider {

	private static final String PATH_INDEX = "/index"; //$NON-NLS-1$
	private static final String PARAM_LANG = "lang"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTP = "http"; //$NON-NLS-1$

	/*
	 * Constructs a new remote index provider, which listens for remote
	 * help preference changes.
	 */
	public RemoteIndexProvider() {
		RemoteHelp.addPreferenceChangeListener(event -> contentChanged());
	}

	@Override
	public IIndexContribution[] getIndexContributions(String locale) {
		if (RemoteHelp.isEnabled()) {
			List<IIndexContribution> contributions = new ArrayList<>();
			PreferenceFileHandler handler = new PreferenceFileHandler();
			String isEnabled[] = handler.isEnabled();
			String [] protocol = handler.getProtocolEntries();
			String [] host = handler.getHostEntries();
			String [] port = handler.getPortEntries();
			String [] path = handler.getPathEntries();
			for (int ic = 0; ic < handler.getTotalRemoteInfocenters(); ic++) {
				if (isEnabled[ic].equalsIgnoreCase("true")) { //$NON-NLS-1$
					@SuppressWarnings("resource")
					InputStream in = null;
					try {
						URL url;

						if(protocol[ic].equals(PROTOCOL_HTTP))
						{
							url = RemoteHelp.getURL(ic, PATH_INDEX + '?' + PARAM_LANG + '=' + locale);
							in = ProxyUtil.getStream(url);
						}
						else
						{
							url = HttpsUtility.getHttpsURL(protocol[ic], host[ic], port[ic], path[ic]+PATH_INDEX + '?' + PARAM_LANG + '=' + locale);
							in = HttpsUtility.getHttpsStream(url);
						}

						RemoteIndexParser parser = new RemoteIndexParser();
						IIndexContribution[] result = parser.parse(in);
						Collections.addAll(contributions, result);
					} catch (IOException e) {
						String msg = "I/O error while trying to contact the remote help server"; //$NON-NLS-1$
						Platform.getLog(getClass()).error(msg, e);
					} catch (Throwable t) {
						String msg = "Internal error while reading index contents from remote server"; //$NON-NLS-1$
						Platform.getLog(getClass()).error(msg, t);
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
			return contributions.toArray(new IIndexContribution[contributions.size()]);
		}
		return new IIndexContribution[0];
	}
}
