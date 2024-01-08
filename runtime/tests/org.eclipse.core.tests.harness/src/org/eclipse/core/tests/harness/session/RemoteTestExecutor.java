/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session;

import static org.eclipse.core.tests.harness.TestHarnessPlugin.PI_HARNESS;
import static org.eclipse.core.tests.harness.TestHarnessPlugin.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.session.RemoteAssertionFailedError;
import org.eclipse.core.tests.session.RemoteTestException;
import org.eclipse.core.tests.session.Setup;
import org.eclipse.core.tests.session.TestDescriptor;

class RemoteTestExecutor {
	private static final String REMOTE_EXECUTION_INDICATION_SYSTEM_PROPERY = "org.eclipse.core.tests.session.isRemoteExecution"; //$NON-NLS-1$

	/**
	 * {@return whether the current test execution is done remotely}
	 */
	static boolean isRemoteExecution() {
		return System.getProperty(REMOTE_EXECUTION_INDICATION_SYSTEM_PROPERY, "").equals(Boolean.toString(true));
	}

	private final String pluginId;
	private final String applicationId;
	private final Setup setup;

	RemoteTestExecutor(Setup setup, String applicationId, String pluginId) {
		this.setup = setup;
		this.applicationId = applicationId;
		this.pluginId = pluginId;
	}

	void executeRemotely(String testClass, String testMethod, boolean shouldFail) throws Throwable {
		Setup localSetup = createSingleTestSetup(testClass, testMethod);
		localSetup.setSystemProperty(REMOTE_EXECUTION_INDICATION_SYSTEM_PROPERY, Boolean.toString(true));

		TestDescriptor descriptor = new TestDescriptor(testClass, testMethod);
		ResultCollector collector = new ResultCollector(getTestId(testClass, testMethod));
		localSetup.setEclipseArgument("port", Integer.toString(collector.getPort()));
		new Thread(collector, "Test result collector").start();
		IStatus status = launch(localSetup);
		collector.shutdown();
		if (!shouldFail) {
			if (!status.isOK()) {
				log(status);
				throw new CoreException(status);
			}
			if (!collector.didTestFinish()) {
				throw new Exception("session test did not run: " + descriptor);
			}
			if (!collector.wasTestSuccessful()) {
				throw collector.getError();
			}
		} else {
			if (status.isOK() && collector.wasTestSuccessful()) {
				throw new AssertionFailedException("test should fail but did not: " + descriptor);
			}
		}
	}

	private static String getTestId(String testClass, String testMethod) {
		return testMethod + "(" + testClass + ")";
	}

	private Setup createSingleTestSetup(String testClassName, String testMethodName) {
		Setup localSetup = (Setup) setup.clone();
		localSetup.setEclipseArgument(Setup.APPLICATION, applicationId);
		localSetup.setEclipseArgument("loaderpluginname", "org.eclipse.core.tests.harness");
		localSetup.setEclipseArgument("testloaderclass", "org.eclipse.jdt.internal.junit5.runner.JUnit5TestLoader");
		localSetup.setEclipseArgument("testpluginname", pluginId);
		localSetup.setEclipseArgument("test", testClassName + ":" + testMethodName);
		return localSetup;
	}

	/**
	 * Runs the setup. Returns a status object indicating the outcome of the
	 * operation.
	 *
	 * @return a status object indicating the outcome
	 */
	private static IStatus launch(Setup setup) {
		Assert.isNotNull(setup.getEclipseArgument(Setup.APPLICATION), "test application is not defined");
		Assert.isNotNull(setup.getEclipseArgument("testpluginname"), "test plug-in id not defined");
		Assert.isTrue(setup.getEclipseArgument("classname") != null ^ setup.getEclipseArgument("test") != null,
				"either a test suite or a test case must be provided");
		// to prevent changes in the protocol from breaking us,
		// force the version we know we can work with
		setup.setEclipseArgument("version", "4");
		IStatus outcome = Status.OK_STATUS;
		try {
			int returnCode = setup.run();
			if (returnCode == 23) {
				// asked to restart; for now just do this once.
				// Note that 23 is our magic return code indicating that a restart is required.
				// This can happen for tests that update framework extensions which requires a
				// restart.
				returnCode = setup.run();
			}
			if (returnCode != 0) {
				outcome = new Status(IStatus.WARNING, Platform.PI_RUNTIME, returnCode,
						"Process returned non-zero code: " + returnCode + "\n\tCommand: " + setup, null);
			}
		} catch (Exception e) {
			outcome = new Status(IStatus.ERROR, Platform.PI_RUNTIME, -1, "Error running process\n\tCommand: " + setup,
					e);
		}
		return outcome;
	}

	/**
	 * Partly taken from
	 * {@code org.eclipse.core.tests.session.SessionTestRunner#ResultCollector}.
	 */
	private static class ResultCollector implements Runnable {
		private final String testId;
		private final ServerSocket serverSocket;

		private volatile boolean shouldRun = true;
		private volatile boolean executionFinished;

		private static enum STATE {
			SUCCESS, FAILURE, ERROR;
		}

		private STATE state = STATE.SUCCESS;
		private StringBuilder stackBuilder;

		private String stackTrace;
		private boolean testFinished;
		private Throwable error;

		ResultCollector(String testId) throws IOException {
			this.serverSocket = new ServerSocket(0);
			this.testId = testId;
		}

		int getPort() {
			return serverSocket.getLocalPort();
		}

		boolean didTestFinish() {
			return testFinished;
		}

		boolean wasTestSuccessful() {
			return testFinished && error == null;
		}

		Throwable getError() {
			return error;
		}

		@Override
		public void run() {
			// someone asked us to stop before we could do anything
			if (!shouldRun) {
				return;
			}
			try (Socket s = serverSocket.accept();
					BufferedReader messageReader = new BufferedReader(
							new InputStreamReader(s.getInputStream(), "UTF-8"))) {
				// main loop
				while (true) {
					synchronized (this) {
						processAvailableMessages(messageReader);
						if (!shouldRun) {
							return;
						}
						this.wait(150);
					}
				}
			} catch (InterruptedException e) {
				// not expected
			} catch (IOException e) {
				if (!shouldRun) {
					// we have been finished without ever getting any connections
					// no need to throw exception
					return;
				}
				log(new Status(IStatus.WARNING, PI_HARNESS, IStatus.ERROR, "Error", e));
			} finally {
				executionFinished = true;
				synchronized (this) {
					notifyAll();
				}
			}
		}

		/*
		 * Politely asks the collector thread to stop and wait until it is finished.
		 */
		void shutdown() {
			// ask the collector to stop
			synchronized (this) {
				if (executionFinished) {
					return;
				}
				shouldRun = false;
				try {
					serverSocket.close();
				} catch (IOException e) {
					log(new Status(IStatus.ERROR, PI_HARNESS, IStatus.ERROR, "Error", e));
				}
				notifyAll();
			}
			// wait until the collector is done
			synchronized (this) {
				while (!executionFinished) {
					try {
						wait(100);
					} catch (InterruptedException e) {
						// we don't care
					}
				}
			}
		}

		private void processAvailableMessages(BufferedReader messageReader) throws IOException {
			while (messageReader.ready()) {
				String message = messageReader.readLine();
				processMessage(message);
			}
		}

		private void processMessage(String message) {
			if (message.startsWith("%TESTS")) {
				String receivedTestId = parseTestId(message);
				if (!testId.equals(receivedTestId)) {
					throw new IllegalStateException("unknown test id: " + receivedTestId + message);
				}
				return;
			} else if (message.startsWith("%TESTE")) {
				switch (state) {
				case FAILURE:
					error = new RemoteAssertionFailedError("", stackTrace);
					break;
				case ERROR:
					error = new RemoteTestException("", stackTrace);
				}
				testFinished = true;
			} else if (message.startsWith("%ERROR")) {
				state = STATE.ERROR;
			} else if (message.startsWith("%FAILED")) {
				state = STATE.FAILURE;
			} else if (message.startsWith("%TRACES")) {
				// just create the string buffer that will hold all the frames of the stack
				// trace
				stackBuilder = new StringBuilder();
			} else if (message.startsWith("%TRACEE")) {
				// stack trace fully read - fill the slot in the result object and reset the
				// string buffer
				stackTrace = stackBuilder.toString();
				stackBuilder = null;
			} else if (message.startsWith("%")) {
				// ignore any other messages
			} else if (stackBuilder != null) {
				// build the stack trace line by line
				stackBuilder.append(message);
				stackBuilder.append(System.lineSeparator());
			}
		}

		private String parseTestId(String message) {
			if (message.isEmpty() || message.charAt(0) != '%') {
				return null;
			}
			int firstComma = message.indexOf(',');
			if (firstComma == -1) {
				return null;
			}
			int secondComma = message.indexOf(',', firstComma + 1);
			if (secondComma == -1) {
				secondComma = message.length();
			}
			return message.substring(firstComma + 1, secondComma);
		}

	}

}
