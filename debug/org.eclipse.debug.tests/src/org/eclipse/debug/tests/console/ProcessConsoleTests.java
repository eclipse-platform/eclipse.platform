/*******************************************************************************
 * Copyright (c) 2019, 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.launching.LaunchConfigurationTests;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the ProcessConsole.
 */
public class ProcessConsoleTests extends AbstractDebugTest {
	/**
	 * Number of received log messages with severity error while running a
	 * single test method.
	 */
	private final AtomicInteger loggedErrors = new AtomicInteger(0);

	/** Listener to count error messages in {@link ConsolePlugin} log. */
	private final ILogListener errorLogListener = (status, plugin) -> {
			if (status.matches(IStatus.ERROR)) {
				loggedErrors.incrementAndGet();
			}
	};

	/** Temporary test files created by a test. Will be deleted on teardown. */
	private final ArrayList<File> tmpFiles = new ArrayList<>();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		loggedErrors.set(0);
		Platform.addLogListener(errorLogListener);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		Platform.removeLogListener(errorLogListener);
		for (File tmpFile : tmpFiles) {
			tmpFile.delete();
		}
		tmpFiles.clear();

		super.tearDown();

		assertEquals("Test triggered errors.", 0, loggedErrors.get());
	}

	/**
	 * Create a new temporary file for testing. File will be deleted when test
	 * finishes.
	 *
	 * @param filename name of the temporary file
	 * @return the created temporary file
	 * @throws IOException if creating the file failed. Includes file already
	 *             exists.
	 */
	private File createTmpFile(String filename) throws IOException {
		File file = DebugUIPlugin.getDefault().getStateLocation().addTrailingSeparator().append(filename).toFile();
		boolean fileCreated = file.createNewFile();
		assertTrue("Failed to prepare temporary test file.", fileCreated);
		tmpFiles.add(file);
		return file;
	}

	/**
	 * Test if two byte UTF-8 characters get disrupted on there way from process
	 * console to the runtime process.
	 * <p>
	 * This test starts every two byte character on an even byte offset.
	 * </p>
	 */
	@Test
	public void testUTF8InputEven() throws Exception {
		// 5000 characters result in 10000 bytes which should be more than most
		// common buffer sizes.
		processConsoleUTF8Input("", 5000);
	}

	/**
	 * Test if two byte UTF-8 characters get disrupted on there way from process
	 * console to the runtime process.
	 * <p>
	 * This test starts every two byte character on an odd byte offset.
	 * </p>
	 */
	@Test
	public void testUTF8InputOdd() throws Exception {
		// 5000 characters result in 10000 bytes which should be more than most
		// common buffer sizes.
		processConsoleUTF8Input("+", 5000);
	}

	/**
	 * Shared code for the UTF-8 input tests.
	 * <p>
	 * Send some two byte UTF-8 characters through process console user input
	 * stream to mockup process and check if the input got corrupted on its way.
	 * </p>
	 *
	 * @param prefix an arbitrary prefix inserted before the two byte UTF-8
	 *            characters. Used to move the other characters to specific
	 *            offsets e.g. a prefix of one byte will produce an input string
	 *            where every two byte character starts at an odd offset.
	 * @param numTwoByteCharacters number of two byte UTF-8 characters to send
	 *            to process
	 */
	public void processConsoleUTF8Input(String prefix, int numTwoByteCharacters) throws Exception {
		final String input = prefix + String.join("", Collections.nCopies(numTwoByteCharacters, "\u00F8"));
		final MockProcess mockProcess = new MockProcess(input.getBytes(StandardCharsets.UTF_8).length, testTimeout);
		try {
			final ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
			launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, StandardCharsets.UTF_8.toString());
			final IProcess process = DebugPlugin.newProcess(launch, mockProcess, "testUtf8Input");
			@SuppressWarnings("restriction")
			final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), StandardCharsets.UTF_8.toString());
			try {
				console.initialize();
				@SuppressWarnings("resource")
				IOConsoleInputStream consoleIn = console.getInputStream();
				consoleIn.appendData(input);
				mockProcess.waitFor(testTimeout, TimeUnit.MILLISECONDS);
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}

		final String receivedInput = new String(mockProcess.getReceivedInput(), StandardCharsets.UTF_8);
		assertEquals(input, receivedInput);
	}

	/**
	 * Test if InputReadJob can be canceled.
	 * <p>
	 * Actually tests cancellation for all jobs of
	 * <code>ProcessConsole.class</code> family.
	 * </p>
	 */
	@Test
	public void testInputReadJobCancel() throws Exception {
		final MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		try {
			final IProcess process = mockProcess.toRuntimeProcess("testInputReadJobCancel");
			@SuppressWarnings("restriction")
			final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider());
			try {
				console.initialize();
				@SuppressWarnings("restriction")
				final Class<?> jobFamily = org.eclipse.debug.internal.ui.views.console.ProcessConsole.class;
				assertTrue("Input read job not started.", Job.getJobManager().find(jobFamily).length > 0);
				Job.getJobManager().cancel(jobFamily);
				TestUtil.waitForJobs(name.getMethodName(), 0, 1000);
				assertEquals("Input read job not canceled.", 0, Job.getJobManager().find(jobFamily).length);
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}
	}

	/**
	 * Test console finished notification with standard process console.
	 */
	@Test
	public void testProcessTerminationNotification() throws Exception {
		TestUtil.log(IStatus.INFO, name.getMethodName(), "Process terminates after Console is initialized.");
		processTerminationTest(null, false);
		TestUtil.log(IStatus.INFO, name.getMethodName(), "Process terminates before Console is initialized.");
		processTerminationTest(null, true);
	}

	/**
	 * Test console finished notification if process standard input is feed from
	 * file.
	 */
	@Test
	public void testProcessTerminationNotificationWithInputFile() throws Exception {
		File inFile = DebugUIPlugin.getDefault().getStateLocation().addTrailingSeparator().append("testStdin.txt").toFile();
		boolean fileCreated = inFile.createNewFile();
		assertTrue("Failed to prepare input file.", fileCreated);
		try {
			ILaunchConfigurationType launchType = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(LaunchConfigurationTests.ID_TEST_LAUNCH_TYPE);
			ILaunchConfigurationWorkingCopy launchConfiguration = launchType.newInstance(null, "testProcessTerminationNotificationWithInputFromFile");
			launchConfiguration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, inFile.getAbsolutePath());
			TestUtil.log(IStatus.INFO, name.getMethodName(), "Process terminates after Console is initialized.");
			processTerminationTest(launchConfiguration, false);
			TestUtil.log(IStatus.INFO, name.getMethodName(), "Process terminates before Console is initialized.");
			processTerminationTest(launchConfiguration, true);
		} finally {
			inFile.delete();
		}
	}

	/**
	 * The shared code to test console finished notification.
	 *
	 * @param launchConfig <code>null</code> or configured with stdin file.
	 * @param terminateBeforeConsoleInitialization if <code>true</code> the
	 *            tested process is terminated before the ProcessConsole can
	 *            perform its initialization. If <code>false</code> the process
	 *            is guaranteed to run until the ProcessConsole was initialized.
	 */
	public void processTerminationTest(ILaunchConfiguration launchConfig, boolean terminateBeforeConsoleInitialization) throws Exception {
		final AtomicBoolean terminationSignaled = new AtomicBoolean(false);
		final Process mockProcess = new MockProcess(null, null, terminateBeforeConsoleInitialization ? 0 : -1);
		final IProcess process = DebugPlugin.newProcess(new Launch(launchConfig, ILaunchManager.RUN_MODE, null), mockProcess, "testProcessTerminationNotification");
		@SuppressWarnings("restriction")
		final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider());
		console.addPropertyChangeListener(event -> {
				if (event.getSource() == console && IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE.equals(event.getProperty())) {
					terminationSignaled.set(true);
				}
		});
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		try {
			consoleManager.addConsoles(new IConsole[] { console });
			if (mockProcess.isAlive()) {
				mockProcess.destroy();
			}
			TestUtil.waitForJobs(name.getMethodName(), 50, 10000);
			assertTrue("No console complete notification received.", terminationSignaled.get());
		} finally {
			consoleManager.removeConsoles(new IConsole[] { console });
			TestUtil.waitForJobs(name.getMethodName(), 0, 10000);
		}
	}

	/**
	 * Test simple redirect of console output into file.
	 */
	@Test
	public void testRedirectOutputToFile() throws Exception {
		final String testContent = "Hello World!";
		final File outFile = createTmpFile("test.out");
		Map<String, Object> launchConfigAttributes = new HashMap<>();
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
		doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", testContent.getBytes(), Files.readAllBytes(outFile.toPath()));
	}

	/**
	 * Test appending of console output into existing file.
	 */
	@Test
	public void testAppendOutputToFile() throws Exception {
		final String testContent = "Hello World!";
		final File outFile = createTmpFile("test-append.out");
		Map<String, Object> launchConfigAttributes = new HashMap<>();
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		launchConfigAttributes.put(IDebugUIConstants.ATTR_APPEND_TO_FILE, true);
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
		doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", testContent.getBytes(), Files.readAllBytes(outFile.toPath()));

		String appendedContent = "append";
		doConsoleOutputTest(appendedContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", (testContent + appendedContent).getBytes(), Files.readAllBytes(outFile.toPath()));
	}

	/**
	 * Test output redirect with a filename containing regular expression
	 * specific special characters.
	 * <p>
	 * Test a filename with special characters which is still a valid regular
	 * expression and a filename whose name is an invalid regular expression.
	 */
	@Test
	public void testBug333239_regexSpecialCharactersInOutputFilename() throws Exception {
		final String testContent = "1.\n2.\n3.\n";
		File outFile = createTmpFile("test.[out]");
		Map<String, Object> launchConfigAttributes = new HashMap<>();
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
		IOConsole console = doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", testContent.getBytes(), Files.readAllBytes(outFile.toPath()));
		assertEquals("Output in console.", 2, console.getDocument().getNumberOfLines());

		outFile = createTmpFile("exhaustive[128-32].out");
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		console = doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", testContent.getBytes(), Files.readAllBytes(outFile.toPath()));
		assertEquals("Output in console.", 2, console.getDocument().getNumberOfLines());

		outFile = createTmpFile("ug(ly.out");
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		console = doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertArrayEquals("Wrong content redirected to file.", testContent.getBytes(), Files.readAllBytes(outFile.toPath()));
		assertEquals("Output in console.", 2, console.getDocument().getNumberOfLines());
	}

	/**
	 * Shared test code for tests who want to write and verify content to
	 * console. Method will open a console for a mockup process, output the
	 * given content, terminate the process and close the console. If content is
	 * expected to be found in console it will be verified. If output is
	 * redirected to file the file path which should be printed to console is
	 * checked.
	 *
	 * @param testContent content to output in console
	 * @param launchConfigAttributes optional launch configuration attributes to
	 *            specify behavior
	 * @return the console object after it has finished
	 */
	private IOConsole doConsoleOutputTest(byte[] testContent, Map<String, Object> launchConfigAttributes) throws Exception {
		final MockProcess mockProcess = new MockProcess(new ByteArrayInputStream(testContent), null, MockProcess.RUN_FOREVER);
		final IProcess process = mockProcess.toRuntimeProcess("Output Redirect", launchConfigAttributes);
		final String encoding = launchConfigAttributes != null ? (String) launchConfigAttributes.get(DebugPlugin.ATTR_CONSOLE_ENCODING) : null;
		final AtomicBoolean consoleFinished = new AtomicBoolean(false);
		@SuppressWarnings("restriction")
		final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), encoding);
		console.addPropertyChangeListener((PropertyChangeEvent event) -> {
			if (event.getSource() == console && IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE.equals(event.getProperty())) {
				consoleFinished.set(true);
			}
		});
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		try {
			consoleManager.addConsoles(new IConsole[] { console });
			mockProcess.destroy();
			waitWhile(c -> !consoleFinished.get(), testTimeout, c -> "Console did not finished.");

			Object value = launchConfigAttributes != null ? launchConfigAttributes.get(IDebugUIConstants.ATTR_CAPTURE_IN_FILE) : null;
			final File outFile = value != null ? new File((String) value) : null;
			value = launchConfigAttributes != null ? launchConfigAttributes.get(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE) : null;
			final boolean checkOutput = value != null ? (boolean) value : true;
			final IDocument doc = console.getDocument();

			if (outFile != null) {
				@SuppressWarnings("restriction")
				String expectedPathMsg = MessageFormat.format(org.eclipse.debug.internal.ui.views.console.ConsoleMessages.ProcessConsole_1, new Object[] {
						outFile.getAbsolutePath() });
				assertEquals("No or wrong output of redirect file path in console.", expectedPathMsg, doc.get(doc.getLineOffset(0), doc.getLineLength(0)));
				assertEquals("Expected redirect file path to be linked.", 1, console.getHyperlinks().length);
			}
			if (checkOutput) {
				assertEquals("Output not found in console.", new String(testContent), doc.get(doc.getLineOffset(1), doc.getLineLength(1)));
			}
			return console;
		} finally {
			if (!process.isTerminated()) {
				process.terminate();
			}
			consoleManager.removeConsoles(new IConsole[] { console });
			TestUtil.waitForJobs(name.getMethodName(), 0, 1000);
		}
	}

	/**
	 * Simulate the common case of a process which constantly produce output.
	 * This should cover the situation that a process produce output before
	 * ProcessConsole is initialized and more output after console is ready.
	 */
	@Test
	public void testOutput() throws Exception {
		String[] lines = new String[] {
				"'Native' process started.",
				"'Eclipse' process started. Stream proxying started.",
				"Console created.", "Console initialized.",
				"Stopping mock process.", };
		String consoleEncoding = StandardCharsets.UTF_8.name();
		try (PipedOutputStream procOut = new PipedOutputStream(); PrintStream sysout = new PrintStream(procOut, true, consoleEncoding)) {
			@SuppressWarnings("resource")
			final MockProcess mockProcess = new MockProcess(new PipedInputStream(procOut), null, MockProcess.RUN_FOREVER);
			sysout.println(lines[0]);
			try {
				Map<String, Object> launchConfigAttributes = new HashMap<>();
				launchConfigAttributes.put(DebugPlugin.ATTR_CONSOLE_ENCODING, consoleEncoding);
				final IProcess process = mockProcess.toRuntimeProcess("simpleOutput", launchConfigAttributes);
				sysout.println(lines[1]);
				@SuppressWarnings("restriction")
				final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
				sysout.println(lines[2]);
				try {
					console.initialize();
					sysout.println(lines[3]);
					sysout.println(lines[4]);
					mockProcess.destroy();
					sysout.close();
					TestUtil.processUIEvents(200);

					for (int i = 0; i < lines.length; i++) {
						IRegion lineInfo = console.getDocument().getLineInformation(i);
						String line = console.getDocument().get(lineInfo.getOffset(), lineInfo.getLength());
						assertEquals("Wrong content in line " + i, lines[i], line);
					}
				} finally {
					console.destroy();
				}
			} finally {
				mockProcess.destroy();
			}
		}
	}

	/**
	 * Test a process which produces binary output and a launch which redirects
	 * output to file. The process output must not be changed in any way due to
	 * the redirection. See bug 558463.
	 */
	@Test
	public void testBinaryOutputToFile() throws Exception {
		byte[] output = new byte[] { (byte) 0xac };
		String consoleEncoding = StandardCharsets.UTF_8.name();

		final File outFile = createTmpFile("testoutput.bin");
		final MockProcess mockProcess = new MockProcess(new ByteArrayInputStream(output), null, MockProcess.RUN_FOREVER);
		try {
			Map<String, Object> launchConfigAttributes = new HashMap<>();
			launchConfigAttributes.put(DebugPlugin.ATTR_CONSOLE_ENCODING, consoleEncoding);
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
			final IProcess process = mockProcess.toRuntimeProcess("redirectBinaryOutput", launchConfigAttributes);
			@SuppressWarnings("restriction")
			final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
			try {
				console.initialize();
				mockProcess.waitFor(100, TimeUnit.MILLISECONDS);
				mockProcess.destroy();
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}

		byte[] receivedOutput = Files.readAllBytes(outFile.toPath());
		assertArrayEquals(output, receivedOutput);
	}

	/**
	 * Test a process which reads binary input from a file through Eclipse
	 * console. The input must not be changed in any way due to the redirection.
	 * See bug 558463.
	 */
	@Test
	public void testBinaryInputFromFile() throws Exception {
		byte[] input = new byte[] { (byte) 0xac };
		String consoleEncoding = StandardCharsets.UTF_8.name();

		final File inFile = createTmpFile("testinput.bin");
		Files.write(inFile.toPath(), input);
		final MockProcess mockProcess = new MockProcess(input.length, testTimeout);
		try {
			Map<String, Object> launchConfigAttributes = new HashMap<>();
			launchConfigAttributes.put(DebugPlugin.ATTR_CONSOLE_ENCODING, consoleEncoding);
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, inFile.getCanonicalPath());
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
			final IProcess process = mockProcess.toRuntimeProcess("redirectBinaryInput", launchConfigAttributes);
			@SuppressWarnings("restriction")
			final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
			try {
				console.initialize();
				mockProcess.waitFor(testTimeout, TimeUnit.MILLISECONDS);
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}

		byte[] receivedInput = mockProcess.getReceivedInput();
		assertArrayEquals(input, receivedInput);
	}
}
