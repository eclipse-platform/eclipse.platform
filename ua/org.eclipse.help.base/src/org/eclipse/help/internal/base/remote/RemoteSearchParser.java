/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.help.internal.search.SearchHit;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Converts search results serialized by the SearchServlet on remote help server
 * back into model objects.
 */
public class RemoteSearchParser extends DefaultHandler {

	private SAXParser parser;
	private Stack<SearchHit> stack;
	private List<SearchHit> hits;
	private StringBuilder summary;

	/*
	 * Parses the given serialized search hits and returns generated model
	 * objects (SearchHit objects).
	 */
	public List<SearchHit> parse(InputStream in, IProgressMonitor monitor) throws ParserConfigurationException, SAXException, IOException {
		monitor.beginTask("", 1); //$NON-NLS-1$
		init();
		parser.parse(in, this);
		monitor.worked(1);
		monitor.done();
		return hits;
	}

	/*
	 * Initializes the parser's state for a new search. Must be called
	 * before each parse.
	 */
	private void init() throws ParserConfigurationException, SAXException {
		if (hits == null) {
			hits = new ArrayList<>();
		}
		else if (!hits.isEmpty()) {
			hits.clear();
		}
		if (stack == null) {
			stack = new Stack<>();
		}
		else if (!stack.isEmpty()) {
			stack.clear();
		}
		summary = null;
		if (parser == null) {
			@SuppressWarnings("restriction")
			SAXParser p = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createSAXParserWithErrorOnDOCTYPE();
			parser = p;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("hit")) { //$NON-NLS-1$
			handleHit(attributes);
		}
		else if (qName.equals("summary")) { //$NON-NLS-1$
			handleSummary(attributes);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals("hit")) { //$NON-NLS-1$
			stack.pop();
		}
		else if (qName.equals("summary") && summary != null) { //$NON-NLS-1$
			// collect the summary from the buffer
			SearchHit hit = stack.peek();
			hit.setSummary(summary.toString());
			summary = null;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// are we in <summary></summary> elements?
		if (summary != null) {
			// if so, add to the buffer
			summary.append(ch, start, length);
		}
	}

	private void handleHit(Attributes attr) {
		String href = attr.getValue("href"); //$NON-NLS-1$
		String label = attr.getValue("label"); //$NON-NLS-1$
		boolean isPotentialHit = (String.valueOf(true).equalsIgnoreCase(attr.getValue("isPotentialHit"))); //$NON-NLS-1$
		float score;
		try {
			score = Float.parseFloat(attr.getValue("score")); //$NON-NLS-1$
		}
		catch (NumberFormatException e) {
			// score was probably missing; default to 0
			score = 0;
		}
		SearchHit hit = new SearchHit(href, label, null, score, null, null, null, isPotentialHit);
		hits.add(hit);
		stack.push(hit);
	}

	private void handleSummary(Attributes attr) {
		// prepare the builder to receive text summary
		summary = new StringBuilder();
	}

	/*
	 * Note: throws clause does not declare IOException due to a bug in
	 * sun jdk: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6327149
	 *
	 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}
}
