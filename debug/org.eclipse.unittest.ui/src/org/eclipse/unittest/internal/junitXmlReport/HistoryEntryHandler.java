/*******************************************************************************
 * Copyright (c) 2020 Red Ha, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.unittest.internal.junitXmlReport;

import java.time.Instant;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Minimal reader to populate history entry data.
 */
public class HistoryEntryHandler extends DefaultHandler {

	private int failuresAndErrors;
	private Instant startTime;
	private String name;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (Thread.interrupted())
			throw new OperationCanceledException();

		if (IXMLTags.NODE_TESTRUN.equals(qName)) {
			String attribute = attributes.getValue(IXMLTags.ATTR_ERRORS);
			if (attribute != null) {
				failuresAndErrors = getFailuresAndErrors() + Integer.parseInt(attribute);
			}
			attribute = attributes.getValue(IXMLTags.ATTR_FAILURES);
			if (attribute != null) {
				failuresAndErrors = getFailuresAndErrors() + Integer.parseInt(attribute);
			}
			name = attributes.getValue(IXMLTags.ATTR_NAME);
			attribute = attributes.getValue(IXMLTags.ATTR_START_TIME);
			if (attribute != null) {
				startTime = Instant.parse(attribute);
			}
		}
	}

	/**
	 * Returns the number of failures and errors for the history entry
	 *
	 * @return a number of failures and errors
	 */
	public int getFailuresAndErrors() {
		return failuresAndErrors;
	}

	/**
	 * Returns an {@link Instant} object instance indicating a unit test start time.
	 *
	 * @return an {@link Instant} object instance
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Returns a name of history entry
	 *
	 * @return a name of history entry
	 */
	public String getName() {
		return name;
	}
}