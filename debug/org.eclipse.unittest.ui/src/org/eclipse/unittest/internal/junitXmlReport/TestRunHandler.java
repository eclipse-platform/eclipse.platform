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

import java.time.Duration;
import java.time.Instant;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.osgi.util.NLS;
import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.ModelMessages;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;

public class TestRunHandler extends DefaultHandler {

	/*
	 * TODO: validate (currently assumes correct XML)
	 */

	private int fId;

	private TestRunSession fTestRunSession;
	private TestSuiteElement fTestSuite;
	private TestCaseElement fTestCase;
	private Stack<Boolean> fNotRun = new Stack<>();

	private StringBuilder fFailureBuffer;
	private boolean fInExpected;
	private boolean fInActual;
	private StringBuilder fExpectedBuffer;
	private StringBuilder fActualBuffer;

	private Locator fLocator;

	private Result fStatus;

	private final IProgressMonitor fMonitor;
	private int fLastReportedLine;

	/**
	 * Constructs a default {@link TestRunHandler} object instance
	 */
	public TestRunHandler() {
		fMonitor = new NullProgressMonitor();
	}

	/**
	 * Constructs a {@link TestRunHandler} object instance
	 *
	 * @param monitor a progress monitor
	 */
	public TestRunHandler(IProgressMonitor monitor) {
		fMonitor = monitor != null ? monitor : new NullProgressMonitor();
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	@Override
	public void startDocument() throws SAXException {
		// Nothing to do
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (fMonitor.isCanceled())
			throw new OperationCanceledException();

		if (fLocator != null) {
			int line = fLocator.getLineNumber();
			if (line - 20 >= fLastReportedLine) {
				line -= line % 20;
				fLastReportedLine = line;
				fMonitor.subTask(NLS.bind(ModelMessages.TestRunHandler_lines_read, Integer.valueOf(line)));
			}
		}

		if (Thread.interrupted())
			throw new OperationCanceledException();

		switch (qName) {
		case IXMLTags.NODE_TESTRUN:
			if (fTestRunSession == null) {
				String name = attributes.getValue(IXMLTags.ATTR_NAME);
				String launchConfigName = attributes.getValue(IXMLTags.ATTR_LAUNCH_CONFIG_NAME);
				ILaunchConfiguration launchConfiguration = null;
				if (launchConfigName != null) {
					try {
						for (ILaunchConfiguration config : DebugPlugin.getDefault().getLaunchManager()
								.getLaunchConfigurations()) {
							if (config.getName().equals(launchConfigName)) {
								launchConfiguration = config;
							}
						}
					} catch (CoreException e) {
						UnitTestPlugin.log(e);
					}
				}
				fTestRunSession = new TestRunSession(name, Instant.parse(attributes.getValue(IXMLTags.ATTR_START_TIME)),
						launchConfiguration);
				readDuration(fTestRunSession, attributes);
				// TODO: read counts?
			} else {
				fTestRunSession.reset();
			}
			fTestSuite = fTestRunSession;
			break;
		case IXMLTags.NODE_TESTSUITES:
			break;
		case IXMLTags.NODE_TESTSUITE: {
			String name = attributes.getValue(IXMLTags.ATTR_NAME);
			String pack = attributes.getValue(IXMLTags.ATTR_PACKAGE);
			String suiteName = pack == null ? name : pack + "." + name; //$NON-NLS-1$
			String displayName = attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
			String data = attributes.getValue(IXMLTags.ATTR_DATA);
			if (data != null && data.isBlank()) {
				data = null;
			}
			fTestSuite = (TestSuiteElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), suiteName, true,
					null, false, displayName, data);
			readDuration(fTestSuite, attributes);
			fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
			break;
		}
		// not interested
		case IXMLTags.NODE_PROPERTIES:
		case IXMLTags.NODE_PROPERTY:
			break;
		case IXMLTags.NODE_TESTCASE: {
			String name = attributes.getValue(IXMLTags.ATTR_NAME);
			String classname = attributes.getValue(IXMLTags.ATTR_CLASSNAME);
			String testName = name + '(' + classname + ')';
			boolean isDynamicTest = Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_DYNAMIC_TEST)).booleanValue();
			String displayName = attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
			String data = attributes.getValue(IXMLTags.ATTR_DATA);
			if (data != null && data.isBlank()) {
				data = null;
			}
			fTestCase = (TestCaseElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), testName, false, 1,
					isDynamicTest, displayName, data);
			fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
			fTestCase.setIgnored(Boolean.parseBoolean(attributes.getValue(IXMLTags.ATTR_IGNORED)));
			readDuration(fTestCase, attributes);
			break;
		}
		case IXMLTags.NODE_ERROR:
			// TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus = Result.ERROR;
			fFailureBuffer = new StringBuilder();
			break;
		case IXMLTags.NODE_FAILURE:
			// TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus = Result.FAILURE;
			fFailureBuffer = new StringBuilder();
			break;
		case IXMLTags.NODE_EXPECTED:
			fInExpected = true;
			fExpectedBuffer = new StringBuilder();
			break;
		case IXMLTags.NODE_ACTUAL:
			fInActual = true;
			fActualBuffer = new StringBuilder();
			break;
		// not interested
		case IXMLTags.NODE_SYSTEM_OUT:
		case IXMLTags.NODE_SYSTEM_ERR:
			break;
		case IXMLTags.NODE_SKIPPED:
			// before Ant 1.9.0: not an Ant JUnit tag, see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
			// later: child of <suite> or <test>, see
			// https://issues.apache.org/bugzilla/show_bug.cgi?id=43969
			fStatus = Result.OK;
			fFailureBuffer = new StringBuilder();
			String message = attributes.getValue(IXMLTags.ATTR_MESSAGE);
			if (message != null) {
				fFailureBuffer.append(message).append('\n');
			}
			break;
		default:
			throw new SAXParseException("unknown node '" + qName + "'", fLocator); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private void readDuration(ITestElement testElement, Attributes attributes) {
		if (testElement instanceof TestElement) {
			TestElement element = (TestElement) testElement;
			String timeString = attributes.getValue(IXMLTags.ATTR_DURATION);
			if (timeString != null) {
				try {
					double seconds = Double.parseDouble(timeString);
					long millis = (long) (seconds * 1000);
					element.setDuration(Duration.ofMillis(millis));
				} catch (NumberFormatException e) {
					// Ignore
				}
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (fInExpected) {
			fExpectedBuffer.append(ch, start, length);

		} else if (fInActual) {
			fActualBuffer.append(ch, start, length);

		} else if (fFailureBuffer != null) {
			fFailureBuffer.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (qName) {
		// OK
		case IXMLTags.NODE_TESTRUN:
			break;
		// OK
		case IXMLTags.NODE_TESTSUITES:
			break;
		case IXMLTags.NODE_TESTSUITE:
			handleTestElementEnd(fTestSuite);
			fTestSuite = fTestSuite.getParent();
			// TODO: end suite: compare counters?
			break;
		// OK
		case IXMLTags.NODE_PROPERTIES:
		case IXMLTags.NODE_PROPERTY:
			break;
		case IXMLTags.NODE_TESTCASE:
			handleTestElementEnd(fTestCase);
			fTestCase = null;
			break;
		case IXMLTags.NODE_FAILURE:
		case IXMLTags.NODE_ERROR: {
			ITestElement testElement = fTestCase;
			if (testElement == null)
				testElement = fTestSuite;
			handleFailure(testElement);
			break;
		}
		case IXMLTags.NODE_EXPECTED:
			fInExpected = false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}
			break;
		case IXMLTags.NODE_ACTUAL:
			fInActual = false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}
			break;
		// OK
		case IXMLTags.NODE_SYSTEM_OUT:
		case IXMLTags.NODE_SYSTEM_ERR:
			break;
		case IXMLTags.NODE_SKIPPED: {
			TestElement testElement = fTestCase;
			if (testElement == null)
				testElement = fTestSuite;
			if (fFailureBuffer != null && fFailureBuffer.length() > 0) {
				handleFailure(testElement);
				testElement.setAssumptionFailed(true);
			} else if (fTestCase != null) {
				fTestCase.setIgnored(true);
			} else { // not expected
				testElement.setAssumptionFailed(true);
			}
			break;
		}
		default:
			handleUnknownNode(qName);
			break;
		}
	}

	private void handleTestElementEnd(ITestElement testElement) {
		boolean completed = !fNotRun.pop().booleanValue();
		fTestRunSession.registerTestEnded((TestElement) testElement, completed);
	}

	private void handleFailure(ITestElement testElement) {
		if (fFailureBuffer != null) {
			fTestRunSession.registerTestFailureStatus((TestElement) testElement, fStatus,
					new FailureTrace(fFailureBuffer.toString(), toString(fExpectedBuffer), toString(fActualBuffer)));
			fFailureBuffer = null;
			fExpectedBuffer = null;
			fActualBuffer = null;
			fStatus = null;
		}
	}

	private String toString(StringBuilder buffer) {
		return buffer != null ? buffer.toString() : null;
	}

	private void handleUnknownNode(String qName) throws SAXException {
		// TODO: just log if debug option is enabled?
		String msg = "unknown node '" + qName + "'"; //$NON-NLS-1$//$NON-NLS-2$
		if (fLocator != null) {
			msg += " at line " + fLocator.getLineNumber() + ", column " + fLocator.getColumnNumber(); //$NON-NLS-1$//$NON-NLS-2$
		}
		throw new SAXException(msg);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		throw e;
	}

	private String getNextId() {
		return Integer.toString(fId++);
	}

	/**
	 * @return the parsed test run session, or <code>null</code>
	 */
	public TestRunSession getTestRunSession() {
		return fTestRunSession;
	}
}
