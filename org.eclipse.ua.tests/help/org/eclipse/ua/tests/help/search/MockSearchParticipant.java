/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.search;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.search.IHelpSearchIndex;
import org.eclipse.help.search.ISearchDocument;
import org.eclipse.help.search.SearchParticipant;

public class MockSearchParticipant extends SearchParticipant {

	private static final String DOC_1 = "/org.eclipse.ua.tests/participant1.xml";
	private static final String DOC_2 = "/org.eclipse.ua.tests/participant2.xml";

	@Override
	public Set<String> getAllDocuments(String locale) {
		HashSet<String> set = new HashSet<>();
		set.add(DOC_1);
		set.add(DOC_2);
		return set;
	}

	@Override
	public IStatus addDocument(IHelpSearchIndex index, String pluginId,
			String name, URL url, String id, ISearchDocument doc) {
		boolean isDoc1 = url.getPath().equals(DOC_1) ;
		String title = isDoc1 ? "Title1" : "Title2";
		String summary = isDoc1? "Summary1" : "Summary2";
		String contents = isDoc1? "jkijkijkk frgeded" : "olhoykk lgktihku";
		doc.setTitle(title);
		doc.setSummary(summary);
		doc.addContents(contents);
		return Status.OK_STATUS;
	}

}
