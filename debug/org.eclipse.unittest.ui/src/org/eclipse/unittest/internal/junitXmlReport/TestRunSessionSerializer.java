/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.unittest.internal.junitXmlReport;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import org.eclipse.unittest.internal.model.ProgressState;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;

import org.eclipse.core.runtime.Assert;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * A {@link TestRunSession} object serializer
 */
public class TestRunSessionSerializer implements XMLReader {

	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String CDATA = "CDATA"; //$NON-NLS-1$
	private static final Attributes NO_ATTS = new AttributesImpl();

	private final TestRunSession fTestRunSession;
	private ContentHandler fHandler;
	private ErrorHandler fErrorHandler;

	/**
	 * @param testRunSession the test run session to serialize
	 */
	public TestRunSessionSerializer(TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		fTestRunSession = testRunSession;
	}

	@Override
	public void parse(InputSource input) throws IOException, SAXException {
		if (fHandler == null)
			throw new SAXException("ContentHandler missing"); //$NON-NLS-1$

		fHandler.startDocument();
		handleTestRun();
		fHandler.endDocument();
	}

	private void handleTestRun() throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		addCDATA(atts, IXMLTags.ATTR_NAME, fTestRunSession.getTestRunName());

		ILaunchConfiguration launchConfig = fTestRunSession.getLaunch() != null
				? fTestRunSession.getLaunch().getLaunchConfiguration()
				: null;
		if (launchConfig != null) {
			addCDATA(atts, IXMLTags.ATTR_LAUNCH_CONFIG_NAME, launchConfig.getName());
		}

		Integer total = fTestRunSession.getFinalTestCaseCount();
		if (total != null) {
			addCDATA(atts, IXMLTags.ATTR_TESTS, total.intValue());
		}
		addCDATA(atts, IXMLTags.ATTR_STARTED, fTestRunSession.countStartedTestCases());
		addCDATA(atts, IXMLTags.ATTR_FAILURES, fTestRunSession.getCurrentFailureCount());
		addCDATA(atts, IXMLTags.ATTR_ERRORS, fTestRunSession.getCurrentErrorCount());
		addCDATA(atts, IXMLTags.ATTR_IGNORED, fTestRunSession.getCurrentIgnoredCount());
		Instant startTime = fTestRunSession.getStartTime();
		if (startTime != null) {
			addCDATA(atts, IXMLTags.ATTR_START_TIME, startTime.toString());
		}
		Duration duration = fTestRunSession.getDuration();
		if (duration != null) {
			addCDATA(atts, IXMLTags.ATTR_DURATION, duration.toString());
		}
		startElement(IXMLTags.NODE_TESTRUN, atts);

		for (ITestElement topSuite : fTestRunSession.getChildren()) {
			handleTestElement(topSuite);
		}

		endElement(IXMLTags.NODE_TESTRUN);
	}

	private void handleTestElement(ITestElement testElement) throws SAXException {
		if (testElement instanceof TestSuiteElement) {
			TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;

			AttributesImpl atts = new AttributesImpl();
			// Need to store the full #getTestName instead of only the #getSuiteTypeName for
			// test factory methods
			addCDATA(atts, IXMLTags.ATTR_NAME, testSuiteElement.getTestName());
			if (testSuiteElement.getDuration() != null) {
				addCDATA(atts, IXMLTags.ATTR_DURATION,
						Double.toString(testSuiteElement.getDuration().toMillis() / 1000.));
			}
			if (testSuiteElement.getProgressState() != ProgressState.COMPLETED
					|| testSuiteElement.getTestResult(false) != Result.UNDEFINED)
				addCDATA(atts, IXMLTags.ATTR_INCOMPLETE, Boolean.TRUE.toString());
			if (testSuiteElement.getDisplayName() != null) {
				addCDATA(atts, IXMLTags.ATTR_DISPLAY_NAME, testSuiteElement.getDisplayName());
			}
			if (testSuiteElement.getData() != null) {
				addCDATA(atts, IXMLTags.ATTR_DATA, testSuiteElement.getData());
			}
			startElement(IXMLTags.NODE_TESTSUITE, atts);
			addFailure(testSuiteElement);

			for (ITestElement child : testSuiteElement.getChildren()) {
				handleTestElement(child);
			}
			endElement(IXMLTags.NODE_TESTSUITE);

		} else if (testElement instanceof TestCaseElement) {
			TestCaseElement testCaseElement = (TestCaseElement) testElement;

			AttributesImpl atts = new AttributesImpl();
			if (testCaseElement.getDuration() != null) {
				addCDATA(atts, IXMLTags.ATTR_DURATION, testCaseElement.getDuration().toString());
			}
			if (testCaseElement.getProgressState() != ProgressState.COMPLETED)
				addCDATA(atts, IXMLTags.ATTR_INCOMPLETE, Boolean.TRUE.toString());
			if (testCaseElement.isIgnored())
				addCDATA(atts, IXMLTags.ATTR_IGNORED, Boolean.TRUE.toString());
			if (testCaseElement.isDynamicTest()) {
				addCDATA(atts, IXMLTags.ATTR_DYNAMIC_TEST, Boolean.TRUE.toString());
			}
			if (testCaseElement.getDisplayName() != null) {
				addCDATA(atts, IXMLTags.ATTR_DISPLAY_NAME, testCaseElement.getDisplayName());
			}
			if (testCaseElement.getData() != null) {
				addCDATA(atts, IXMLTags.ATTR_DATA, testCaseElement.getData());
			}
			startElement(IXMLTags.NODE_TESTCASE, atts);
			addFailure(testCaseElement);

			endElement(IXMLTags.NODE_TESTCASE);

		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

	}

	private void addFailure(TestElement testElement) throws SAXException {
		FailureTrace failureTrace = testElement.getFailureTrace();

		if (testElement.isAssumptionFailure()) {
			startElement(IXMLTags.NODE_SKIPPED, NO_ATTS);
			if (failureTrace != null) {
				addCharacters(failureTrace.getTrace());
			}
			endElement(IXMLTags.NODE_SKIPPED);

		} else if (failureTrace != null) {
			AttributesImpl failureAtts = new AttributesImpl();
//				addCDATA(failureAtts, IXMLTags.ATTR_MESSAGE, xx);
//				addCDATA(failureAtts, IXMLTags.ATTR_TYPE, xx);
			String failureKind = testElement.getTestResult(false) == Result.ERROR ? IXMLTags.NODE_ERROR
					: IXMLTags.NODE_FAILURE;
			startElement(failureKind, failureAtts);
			String expected = failureTrace.getExpected();
			String actual = failureTrace.getActual();
			if (expected != null) {
				startElement(IXMLTags.NODE_EXPECTED, NO_ATTS);
				addCharacters(expected);
				endElement(IXMLTags.NODE_EXPECTED);
			}
			if (actual != null) {
				startElement(IXMLTags.NODE_ACTUAL, NO_ATTS);
				addCharacters(actual);
				endElement(IXMLTags.NODE_ACTUAL);
			}
			String trace = failureTrace.getTrace();
			addCharacters(trace);
			endElement(failureKind);
		}
	}

	private void startElement(String name, Attributes atts) throws SAXException {
		fHandler.startElement(EMPTY, name, name, atts);
	}

	private void endElement(String name) throws SAXException {
		fHandler.endElement(EMPTY, name, name);
	}

	private static void addCDATA(AttributesImpl atts, String name, int value) {
		addCDATA(atts, name, Integer.toString(value));
	}

	private static void addCDATA(AttributesImpl atts, String name, String value) {
		atts.addAttribute(EMPTY, EMPTY, name, CDATA, value);
	}

	private void addCharacters(String string) throws SAXException {
		string = escapeNonUnicodeChars(string);
		fHandler.characters(string.toCharArray(), 0, string.length());
	}

	/**
	 * Replaces all non-Unicode characters in the given string.
	 *
	 * @param string a string
	 * @return string with Java-escapes
	 */
	private static String escapeNonUnicodeChars(String string) {
		StringBuilder buf = null;
		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			if (!(ch == 9 || ch == 10 || ch == 13 || ch >= 32)) {
				if (buf == null) {
					buf = new StringBuilder(string.substring(0, i));
				}
				buf.append("\\u"); //$NON-NLS-1$
				String hex = Integer.toHexString(ch);
				for (int j = hex.length(); j < 4; j++)
					buf.append('0');
				buf.append(hex);
			} else if (buf != null) {
				buf.append(ch);
			}
		}
		if (buf != null) {
			return buf.toString();
		}
		return string;
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		this.fHandler = handler;
	}

	@Override
	public ContentHandler getContentHandler() {
		return fHandler;
	}

	@Override
	public void setErrorHandler(ErrorHandler handler) {
		fErrorHandler = handler;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return fErrorHandler;
	}

	// ignored:

	@Override
	public void parse(String systemId) throws IOException, SAXException {
		// Nothing to do
	}

	@Override
	public void setDTDHandler(DTDHandler handler) {
		// Nothing to do
	}

	@Override
	public DTDHandler getDTDHandler() {
		return null;
	}

	@Override
	public void setEntityResolver(EntityResolver resolver) {
		// Nothing to do
	}

	@Override
	public EntityResolver getEntityResolver() {
		return null;
	}

	@Override
	public void setProperty(java.lang.String name, java.lang.Object value) {
		// Nothing to do
	}

	@Override
	public Object getProperty(java.lang.String name) {
		return null;
	}

	@Override
	public void setFeature(java.lang.String name, boolean value) {
		// Nothing to do
	}

	@Override
	public boolean getFeature(java.lang.String name) {
		return false;
	}
}
