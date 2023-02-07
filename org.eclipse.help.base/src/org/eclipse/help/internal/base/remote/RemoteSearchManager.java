/***************************************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 **************************************************************************************************/
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.search.ISearchHitCollector;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.QueryTooComplexException;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.util.URLCoder;

/*
 * Manages indexing and searching for all remote help content.
 */
public class RemoteSearchManager {

	private static final String PATH_SEARCH = "/search"; //$NON-NLS-1$
	private static final String PARAM_PHRASE = "phrase"; //$NON-NLS-1$
	private static final String PARAM_LANG = "lang"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTP = "http"; //$NON-NLS-1$

	/*
	 * Performs a search for remote content.
	 */
	public void search(ISearchQuery searchQuery, ISearchHitCollector collector, IProgressMonitor pm)
			throws QueryTooComplexException {
		SubMonitor subMonitor = SubMonitor.convert(pm, 100);

		PreferenceFileHandler prefHandler = new PreferenceFileHandler();
		String host[] = prefHandler.getHostEntries();
		String port[] = prefHandler.getPortEntries();
		String path[] = prefHandler.getPathEntries();
		String [] protocols = prefHandler.getProtocolEntries();
		String isEnabled[] = prefHandler.isEnabled();

		try {
			// InfoCenters ignore remote content
			if (RemoteHelp.isEnabled()) {

				int numICs = host.length; // Total number of hosts
				for (int i = 0; i < numICs; i++) {

					if (isEnabled[i].equals("true")) { //$NON-NLS-1$
						@SuppressWarnings("resource")
						InputStream in = null;
						try {
							URL url;

							if(protocols[i].equals(PROTOCOL_HTTP))
							{
								url = new URL("http", host[i], Integer.parseInt(port[i]), path[i] + PATH_SEARCH + '?' + PARAM_PHRASE + '=' + URLCoder.encode(searchQuery.getSearchWord()) + '&' + PARAM_LANG + '=' + searchQuery.getLocale()); //$NON-NLS-1$
								in = ProxyUtil.getStream(url);
							}
							else
							{
								url = HttpsUtility.getHttpsURL(protocols[i], host[i], port[i], path[i]+ PATH_SEARCH + '?' + PARAM_PHRASE + '=' + URLCoder.encode(searchQuery.getSearchWord()) + '&' + PARAM_LANG + '=' + searchQuery.getLocale());
								in = HttpsUtility.getHttpsStream(url);
							}

							RemoteSearchParser parser = new RemoteSearchParser();
							// parse the XML-serialized search results
							List<SearchHit> hits = parser.parse(in, subMonitor.split(100));
							collector.addHits(hits, null);
						} catch (IOException e) {
							String msg = "I/O error while trying to contact the remote help server"; //$NON-NLS-1$
							Platform.getLog(getClass()).error(msg, e);
						} catch (Throwable t) {
							String msg = "Internal error while reading search results from remote server"; //$NON-NLS-1$
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
			}

		} finally {
			pm.done();
		}
	}
}
