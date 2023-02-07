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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.help.search.SearchParticipantXML;
import org.xml.sax.Attributes;

public class MockSearchParticipantXML extends SearchParticipantXML {

	public static final String DOC_1 = "/org.eclipse.ua.tests/data/help/search/participant_xml_1.xml";
	public static final String DOC_2 = "/org.eclipse.ua.tests/data/help/search/participant_xml_2.xml";

	@Override
	public Set<String> getAllDocuments(String locale) {
		HashSet<String> set = new HashSet<>();
		set.add(DOC_1 + "?id=xml1" + "?id=xml2");
		set.add(DOC_2);
		return set;
	}

	@Override
	public Set<String> getContributingPlugins() {
		Set<String> result = new HashSet<>();
		result.add("org.eclipse.ua.tests");
		return result;
	}

	@Override
	protected void handleStartElement(String name, Attributes attributes,
			IParsedXMLContent data) {
		if ("participant".equalsIgnoreCase(name)) {
			data.setTitle(attributes.getValue("title"));
		}
		String summary = attributes.getValue("summary");
		if (summary != null) {
			data.addToSummary(summary);
		}
	}

	@Override
	protected void handleEndElement(String name, IParsedXMLContent data) {
	}

	@Override
	protected void handleText(String text, IParsedXMLContent data) {
		String top = getTopElement();
		if ("text".equalsIgnoreCase(top)) {
			data.addText(text);
		}
	}

	@Override
	public boolean open(String id) {
		System.out.println("Open " + id);
		return true;
	}

}
