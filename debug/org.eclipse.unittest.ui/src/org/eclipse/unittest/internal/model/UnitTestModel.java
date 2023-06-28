/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.unittest.internal.model;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.internal.junitXmlReport.TestRunHandler;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

/**
 * Central registry for Unit Test test runs.
 */
public final class UnitTestModel {

	private final ListenerList<ITestRunSessionListener> fTestRunSessionListeners = new ListenerList<>();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList<TestRunSession> fTestRunSessions = new LinkedList<>();

	private static UnitTestModel INSTANCE = null;

	private UnitTestModel() {

	}

	/**
	 * Returns a {@link UnitTestModel} object instance
	 *
	 * @return a {@link UnitTestModel} object instance
	 */
	public static synchronized UnitTestModel getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new UnitTestModel();
		}
		return INSTANCE;
	}

	/**
	 * Starts the model (called by the {@link UnitTestPlugin} on startup).
	 */
	public void start() {
		/*
		 * TODO: restore on restart: - only import headers! - only import last n
		 * sessions; remove all other files in historyDirectory
		 */
//		File historyDirectory= UnitTestPlugin.getHistoryDirectory();
//		File[] swapFiles= historyDirectory.listFiles();
//		if (swapFiles != null) {
//			Arrays.sort(swapFiles, new Comparator() {
//				public int compare(Object o1, Object o2) {
//					String name1= ((File) o1).getName();
//					String name2= ((File) o2).getName();
//					return name1.compareTo(name2);
//				}
//			});
//			for (int i= 0; i < swapFiles.length; i++) {
//				final File file= swapFiles[i];
//				SafeRunner.run(new ISafeRunnable() {
//					public void run() throws Exception {
//						importTestRunSession(file );
//					}
//					public void handleException(Throwable exception) {
//						UnitTestPlugin.log(exception);
//					}
//				});
//			}
//		}

//		addTestRunSessionListener(new LegacyTestRunSessionListener());
	}

	/**
	 * Stops the model (called by the {@link UnitTestPlugin} on shutdown).
	 */
	public void stop() {
//		for (Iterator iter= fTestRunSessions.iterator(); iter.hasNext();) {
//			final TestRunSession session= (TestRunSession) iter.next();
//			SafeRunner.run(new ISafeRunnable() {
//				public void run() throws Exception {
//					session.swapOut();
//				}
//				public void handleException(Throwable exception) {
//					UnitTestPlugin.log(exception);
//				}
//			});
//		}
	}

	/**
	 * Adds an {@link ITestRunSessionListener} object
	 *
	 * @param listener a listener object
	 */
	public void addTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}

	/**
	 * Removes an {@link ITestRunSessionListener} object
	 *
	 * @param listener a listener object
	 */
	public void removeTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}

	/**
	 * Returns a list of {@link TestRunSession} objects
	 *
	 * @return a list of {@link TestRunSession} objects
	 */
	public synchronized List<TestRunSession> getTestRunSessions() {
		return new ArrayList<>(fTestRunSessions);
	}

	/**
	 * Adds a specified {@link TestRunSession} object into the list of processed
	 * test run sessions.
	 *
	 * The list length is limited by the value of
	 * {@link UnitTestPreferencesConstants#MAX_TEST_RUNS} preference.
	 *
	 * @param testRunSession a {@link TestRunSession} object to be added
	 * @see org.eclipse.unittest.internal.UnitTestPreferencesConstants#MAX_TEST_RUNS
	 * @see org.eclipse.unittest.internal.model.UnitTestModel#removeTestRunSession(TestRunSession)
	 */
	void addTestRunSession(TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		ArrayList<TestRunSession> toRemove = new ArrayList<>();

		synchronized (this) {
			Assert.isLegal(!fTestRunSessions.contains(testRunSession));
			fTestRunSessions.addFirst(testRunSession);

			int maxCount = Platform.getPreferencesService().getInt(UnitTestPlugin.PLUGIN_ID,
					UnitTestPreferencesConstants.MAX_TEST_RUNS, 10, null);
			int size = fTestRunSessions.size();
			if (size > maxCount) {
				List<TestRunSession> excess = fTestRunSessions.subList(maxCount, size);
				for (Iterator<TestRunSession> iter = excess.iterator(); iter.hasNext();) {
					TestRunSession oldSession = iter.next();
					if (oldSession.isStopped()) {
						toRemove.add(oldSession);
						iter.remove();
					}
				}
			}
		}

		toRemove.forEach(this::notifyTestRunSessionRemoved);
		notifyTestRunSessionAdded(testRunSession);
	}

	/**
	 * Imports a test run session from an URL
	 *
	 * @param url     an URL to source file
	 * @param monitor a progress monitor object
	 * @return an {@link ITestRunSession} object instance
	 *
	 * @throws InvocationTargetException in case of problems during import operation
	 * @throws InterruptedException      in case of import operation is interrupted
	 */
	public ITestRunSession importTestRunSession(String url, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask(ModelMessages.UnitTestModel_importing_from_url, IProgressMonitor.UNKNOWN);
		final String trimmedUrl = url.trim().replaceAll("\r\n?|\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
		final TestRunHandler handler = new TestRunHandler(monitor);

		final CoreException[] exception = { null };
		final TestRunSession[] session = { null };

		Thread importThread = new Thread("UnitTest URL importer") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					SAXParserFactory parserFactory = SAXParserFactory.newInstance();
//					parserFactory.setValidating(true); // TODO: add DTD and debug flag
					SAXParser parser = parserFactory.newSAXParser();
					parser.parse(trimmedUrl, handler);
					session[0] = handler.getTestRunSession();
				} catch (OperationCanceledException e) {
					// canceled
				} catch (ParserConfigurationException e) {
					storeImportError(e);
				} catch (SAXException e) {
					storeImportError(e);
				} catch (IOException e) {
					storeImportError(e);
				} catch (IllegalArgumentException e) {
					// Bug in parser: can throw IAE even if URL is not null
					storeImportError(e);
				}
			}

			private void storeImportError(Exception e) {
				exception[0] = new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
						UnitTestPlugin.PLUGIN_ID, ModelMessages.UnitTestModel_could_not_import, e));
			}
		};
		importThread.start();

		while (session[0] == null && exception[0] == null && !monitor.isCanceled()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// that's OK
			}
		}
		if (session[0] == null) {
			if (exception[0] != null) {
				throw new InvocationTargetException(exception[0]);
			} else {
				importThread.interrupt(); // have to kill the thread since we don't control URLConnection and XML
											// parsing
				throw new InterruptedException();
			}
		}

		addTestRunSession(session[0]);
		monitor.done();
		return session[0];
	}

	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 *
	 * @param testRunSession the session to remove
	 * @see org.eclipse.unittest.internal.model.UnitTestModel#addTestRunSession(TestRunSession)
	 */
	void removeTestRunSession(TestRunSession testRunSession) {
		boolean existed;
		synchronized (this) {
			existed = fTestRunSessions.remove(testRunSession);
		}
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionRemoved(TestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(ITestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionAdded(testRunSession);
		}
	}

}
