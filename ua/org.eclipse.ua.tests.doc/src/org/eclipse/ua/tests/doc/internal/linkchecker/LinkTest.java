/*******************************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.doc.internal.linkchecker;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchHitCollector;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.QueryTooComplexException;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.junit.Test;

public class LinkTest {

	@Test
	public void testAllLinks() {
		IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
		ISearchQuery query = new SearchQuery("*", false, Collections.emptyList(), Platform.getNL());
		final Set<URI> indexedPagesURIs = new HashSet<>();
		ISearchHitCollector collector = new ISearchHitCollector() {
			@Override
			public void addQTCException(QueryTooComplexException exception) throws QueryTooComplexException {
				throw exception;
			}

			@Override
			public void addHits(List<SearchHit> hits, String wordsSearched) {
				hits.stream().map(SearchHit::getHref).map(href -> {
					try {
						return helpSystem.resolve(href, false).toURI();
					} catch (URISyntaxException e) {
						e.printStackTrace();
						return null;
					}
				}).sorted().peek(System.err::println).forEach(indexedPagesURIs::add);
			}
		};
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		Set<String> linkFailures = Collections.synchronizedSet(new TreeSet<>());
		Set<Exception> ex = Collections.synchronizedSet(new LinkedHashSet<>());
		Set<URI> allKnownPageURIs = new TreeSet<>(indexedPagesURIs);
		indexedPagesURIs.parallelStream().forEach(t -> {
			String path = t.getPath();
			if (path.lastIndexOf('/') > 0) {
				path = path.substring(path.lastIndexOf('/'));
			}
			boolean notFile = false;
			if (!path.contains(".")) {
				notFile = true;
				System.out.println("Not a file?: " + t);
			}
			try (InputStream stream = t.toURL().openStream()) {
				linkFailures.addAll(checkLinks(stream, t, allKnownPageURIs));
			} catch (IOException e) {
				if (!notFile) {
					ex.add(e);
				}
			}
		});
		assertEquals(Collections.emptySet().toString(), ex.toString());
		assertEquals(Collections.emptySet().toString(), linkFailures.toString());
	}

	private Set<String> checkLinks(InputStream stream, URI currentDoc, Set<URI> knownPagesURIs) throws IOException {
		Set<String> res = new HashSet<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				int index = 0;
				while ((index = inputLine.indexOf("<a href=\"", index)) > 0) {
					int closeIndex = inputLine.indexOf('"', index + "<a href=\"".length());
					URI href = URI
							.create(inputLine.substring(index + "<a href=\"".length(), closeIndex).replace(" ", "%20"));
					index = closeIndex;
					if (href.isAbsolute()) {
						continue;
					}
					URI linkURI = URI.create(currentDoc.toString() + "/../" + href).normalize();
					if (knownPagesURIs.contains(linkURI)) { // page already indexed or successfully visited
						// we already know this help page exists as it is indexed
						continue;
					} else { // page isn't indexed: can be generated navigation page
						// check whether it's existing anyway
						HttpURLConnection connection = (HttpURLConnection) linkURI.toURL().openConnection();
						connection.setRequestMethod("HEAD");
						connection.connect();
						if (connection.getResponseCode() != 200) {
							res.add("Link from " + currentDoc + " to " + href + " is broken: target URI " + linkURI
									+ " doens't exist.");
						} else {
							knownPagesURIs.add(linkURI);
						}
					}
				}
			}
		}
		return res;
	}

}
