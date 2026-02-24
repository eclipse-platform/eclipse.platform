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

import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.debug.tests.TestUtil.waitWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.debug.internal.ui.views.console.ConsoleMessages;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.launching.LaunchConfigurationTests;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.internal.console.ConsoleManager;
import org.eclipse.ui.internal.console.ConsoleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the ProcessConsole.
 */
@ExtendWith(DebugTestExtension.class)
public class ProcessConsoleTests {
	/**
	 * Log messages with severity error received while running a single test
	 * method.
	 */
	private final List<IStatus> loggedErrors = Collections.synchronizedList(new ArrayList<>());

	/** Listener to count error messages in {@link ConsolePlugin} log. */
	private final ILogListener errorLogListener = (status, plugin) -> {
			if (status.matches(IStatus.ERROR)) {
				loggedErrors.add(status);
			}
	};

	/** Temporary test files created by a test. Will be deleted on teardown. */
	private final ArrayList<File> tmpFiles = new ArrayList<>();

	private TestInfo testInfo;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		this.testInfo = testInfo;
		loggedErrors.clear();
		Platform.addLogListener(errorLogListener);
	}

	@AfterEach
	public void tearDown() throws Exception {
		Platform.removeLogListener(errorLogListener);
		waitForConsoleRelatedJobs();
		for (File tmpFile : tmpFiles) {
			tmpFile.delete();
		}
		tmpFiles.clear();

		assertThat(errorsToStrings()).as("logged errors").isEmpty();
	}

	private void waitForConsoleRelatedJobs() {
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 100, 10000);
		TestUtil.waitForJobs(testInfo.getDisplayName(), ProcessConsole.class, 0, 10000);
	}

	private Stream<String> errorsToStrings() {
		return loggedErrors.stream().map(status -> status.toString() + throwableToString(status.getException()));
	}

	private static String throwableToString(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		return System.lineSeparator() + "Stack trace: " + Stream.of(throwable.getStackTrace()).map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
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
		assertTrue(fileCreated, "Failed to prepare temporary test file.");
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
		final MockProcess mockProcess = new MockProcess(input.getBytes(StandardCharsets.UTF_8).length, TestUtil.DEFAULT_TIMEOUT);
		try {
			final ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
			launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, StandardCharsets.UTF_8.toString());
			final IProcess process = DebugPlugin.newProcess(launch, mockProcess, "testUtf8Input");
			final org.eclipse.debug.internal.ui.views.console.ProcessConsole console = new org.eclipse.debug.internal.ui.views.console.ProcessConsole(process, new ConsoleColorProvider(), StandardCharsets.UTF_8.toString());
			try {
				console.initialize();
				@SuppressWarnings("resource")
				IOConsoleInputStream consoleIn = console.getInputStream();
				consoleIn.appendData(input);
				mockProcess.waitFor(TestUtil.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
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
			final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider());
			try {
				console.initialize();
				final Class<?> jobFamily = ProcessConsole.class;
				assertThat(Job.getJobManager().find(jobFamily)).as("check input read job started").hasSizeGreaterThan(0);
				Job.getJobManager().cancel(jobFamily);
				TestUtil.waitForJobs(testInfo.getDisplayName(), ProcessConsole.class, 0, 1000);
				assertThat(Job.getJobManager().find(jobFamily)).as("check input read job is canceled").isEmpty();
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
		TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "Process terminates after Console is initialized.");
		processTerminationTest(null, false);
		TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "Process terminates before Console is initialized.");
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
		assertTrue(fileCreated, "Failed to prepare input file.");
		try {
			ILaunchConfigurationType launchType = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(LaunchConfigurationTests.ID_TEST_LAUNCH_TYPE);
			ILaunchConfigurationWorkingCopy launchConfiguration = launchType.newInstance(null, testInfo.getDisplayName());
			launchConfiguration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, inFile.getAbsolutePath());
			TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "Process terminates after Console is initialized.");
			processTerminationTest(launchConfiguration, false);
			TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "Process terminates before Console is initialized.");
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
		final IProcess process = DebugPlugin.newProcess(new Launch(launchConfig, ILaunchManager.RUN_MODE, null), mockProcess, testInfo.getDisplayName());
		final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider());
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
			waitWhile(() -> !terminationSignaled.get(), () -> "No console complete notification received.");
		} finally {
			consoleManager.removeConsoles(new IConsole[] { console });
			waitForConsoleRelatedJobs();
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
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly(testContent.getBytes());
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
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly(testContent.getBytes());

		String appendedContent = "append";
		doConsoleOutputTest(appendedContent.getBytes(), launchConfigAttributes);
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly((testContent + appendedContent).getBytes());
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
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly(testContent.getBytes());
		assertEquals(2, console.getDocument().getNumberOfLines(), "Output in console.");

		outFile = createTmpFile("exhaustive[128-32].out");
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		console = doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly(testContent.getBytes());
		assertEquals(2, console.getDocument().getNumberOfLines(), "Output in console.");

		outFile = createTmpFile("ug(ly.out");
		launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, outFile.getCanonicalPath());
		console = doConsoleOutputTest(testContent.getBytes(), launchConfigAttributes);
		assertThat(readAllBytes(outFile.toPath())).as("content redirected to file").containsExactly(testContent.getBytes());
		assertEquals(2, console.getDocument().getNumberOfLines(), "Output in console.");
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
		final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider(), encoding);
		console.addPropertyChangeListener((PropertyChangeEvent event) -> {
			if (event.getSource() == console && IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE.equals(event.getProperty())) {
				consoleFinished.set(true);
			}
		});
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		try {
			consoleManager.addConsoles(new IConsole[] { console });
			mockProcess.destroy();
			waitWhile(() -> !consoleFinished.get(), () -> "Console did not finished.");

			Object value = launchConfigAttributes != null ? launchConfigAttributes.get(IDebugUIConstants.ATTR_CAPTURE_IN_FILE) : null;
			final File outFile = value != null ? new File((String) value) : null;
			value = launchConfigAttributes != null ? launchConfigAttributes.get(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE) : null;
			final boolean checkOutput = value != null ? (boolean) value : true;
			final IDocument doc = console.getDocument();

			if (outFile != null) {
				String expectedPathMsg = MessageFormat.format(ConsoleMessages.ProcessConsole_1, outFile.getAbsolutePath());
				assertEquals(expectedPathMsg, doc.get(doc.getLineOffset(0), doc.getLineLength(0)), "No or wrong output of redirect file path in console.");
				assertThat(console.getHyperlinks()).as("check redirect file path is linked").hasSize(1);
			}
			if (checkOutput) {
				assertEquals(new String(testContent), doc.get(doc.getLineOffset(1), doc.getLineLength(1)), "Output not found in console.");
			}
			return console;
		} finally {
			if (!process.isTerminated()) {
				process.terminate();
			}
			consoleManager.removeConsoles(new IConsole[] { console });
			waitForConsoleRelatedJobs();
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
				final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
				sysout.println(lines[2]);
				try {
					console.initialize();
					sysout.println(lines[3]);
					sysout.println(lines[4]);
					mockProcess.destroy();
					sysout.close();

					BooleanSupplier waitForLastLineWritten = () -> {
						try {
							TestUtil.processUIEvents(50);
						} catch (Exception e) {
							// try again
						}
						return console.getDocument().getNumberOfLines() < lines.length;
					};
					Supplier<String> errorMessageProvider = () -> {
						String expected = String.join(System.lineSeparator(), lines);
						String actual = console.getDocument().get();
						return "Not all lines have been written, expected: " + expected + ", was: " + actual;
					};
					waitWhile(waitForLastLineWritten, errorMessageProvider);

					for (int i = 0; i < lines.length; i++) {
						IRegion lineInfo = console.getDocument().getLineInformation(i);
						String line = console.getDocument().get(lineInfo.getOffset(), lineInfo.getLength());
						assertEquals(lines[i], line, "Wrong content in line " + i);
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
			final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
			try {
				console.initialize();

				BooleanSupplier waitForFileWritten = () -> {
					try {
						TestUtil.processUIEvents(20);
						return readAllBytes(outFile.toPath()).length < output.length;
					} catch (Exception e) {
						// try again
					}
					return false;
				};
				Supplier<String> errorMessageProvider = () -> {
					byte[] actualOutput = new byte[0];
					try {
						actualOutput = readAllBytes(outFile.toPath());
					} catch (IOException e) {
						// Proceed as if output was empty
					}
					return "File has not been written, expected: " + Arrays.toString(output) + ", was: " + Arrays.toString(actualOutput);
				};
				waitWhile(waitForFileWritten, errorMessageProvider);
				mockProcess.destroy();
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}

		byte[] receivedOutput = Files.readAllBytes(outFile.toPath());
		assertThat(receivedOutput).as("received output").isEqualTo(output);
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
		final MockProcess mockProcess = new MockProcess(input.length, TestUtil.DEFAULT_TIMEOUT);
		try {
			Map<String, Object> launchConfigAttributes = new HashMap<>();
			launchConfigAttributes.put(DebugPlugin.ATTR_CONSOLE_ENCODING, consoleEncoding);
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, inFile.getCanonicalPath());
			launchConfigAttributes.put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
			final IProcess process = mockProcess.toRuntimeProcess("redirectBinaryInput", launchConfigAttributes);
			final ProcessConsole console = new ProcessConsole(process, new ConsoleColorProvider(), consoleEncoding);
			try {
				console.initialize();
				mockProcess.waitFor(TestUtil.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
			} finally {
				console.destroy();
			}
		} finally {
			mockProcess.destroy();
		}

		byte[] receivedInput = mockProcess.getReceivedInput();
		assertThat(receivedInput).as("received input").isEqualTo(input);
	}

	/**
	 * Test that console name updates (elapsed time) only happen for visible
	 * consoles. Hidden consoles should not update their name. When a hidden
	 * console is brought to front, it should start updating. When the console
	 * view is hidden/minimized, no console should update its name. When the
	 * view is shown again, the visible console should resume updates.
	 */
	@Test
	public void testConsoleNameUpdateForVisibleAndHiddenConsoles() throws Exception {
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		// Make sure we only see exact one console view during the test,
		// otherwise it may interfere with visibility and name update checks.
		IPerspectiveDescriptor currentPerspective = activePage.getPerspective();
		activePage.closeAllPerspectives(false, false);
		TestUtil.processUIEvents();
		activePage.setPerspective(currentPerspective);
		TestUtil.processUIEvents();

		// Create two silent mock processes (no output, run forever)
		final MockProcess mockProcess1 = new MockProcess(MockProcess.RUN_FOREVER);
		final MockProcess mockProcess2 = new MockProcess(MockProcess.RUN_FOREVER);
		try {
			final IProcess process1 = mockProcess1.toRuntimeProcess("Process1");
			final IProcess process2 = mockProcess2.toRuntimeProcess("Process2");
			process1.setAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			process2.setAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			final ProcessConsole console1 = new ProcessConsole(process1, new ConsoleColorProvider());
			final ProcessConsole console2 = new ProcessConsole(process2, new ConsoleColorProvider());
			ConsoleView consoleView = (ConsoleView) activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			try {
				// Open console view and add both consoles
				activePage.activate(consoleView);
				TestUtil.processUIEvents();

				consoleManager.addConsoles(new IConsole[] { console1, console2 });

				// Display console2 (visible) - console1 is hidden
				consoleView.display(console2);
				TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 10000);

				// Record initial names
				String console2NameBefore = console2.getName();
				String console1NameBefore = console1.getName();

				// Wait >1 second for the elapsed time update to trigger
				TestUtil.processUIEvents(1500);

				// Visible console (console2) should have updated name
				String console2NameAfter = console2.getName();
				assertNotEquals(console2NameBefore, console2NameAfter, "Visible console name should have been updated (elapsed time changed)");

				// Hidden console (console1) should NOT have updated name
				String console1NameAfter = console1.getName();
				assertEquals(console1NameBefore, console1NameAfter, "Hidden console name should not be updated");

				// Bring hidden console1 to front (visible) - console2 becomes
				// hidden
				consoleView.display(console1);
				TestUtil.processUIEvents(200);

				// Record names after switch
				String console1NameAfterSwitch = console1.getName();
				String console2NameAfterSwitch = console2.getName();

				// Wait >1 second for the elapsed time update
				TestUtil.processUIEvents(2000);

				// Now console1 (visible) should update
				String console1NameAfterWait = console1.getName();
				assertNotEquals(console1NameAfterSwitch, console1NameAfterWait, "Console brought to front should start updating its name");

				// console2 (now hidden) should stop updating
				String console2NameAfterWait = console2.getName();
				assertEquals(console2NameAfterSwitch, console2NameAfterWait, "Console moved to background should stop updating its name");

				// Minimize the console view - neither console should update
				activePage.setPartState(activePage.getReference(consoleView), IWorkbenchPage.STATE_MINIMIZED);
				TestUtil.processUIEvents(200);

				// Record names after minimizing
				String console1NameBeforeHide = console1.getName();
				String console2NameBeforeHide = console2.getName();

				// Wait >1 second
				TestUtil.processUIEvents(2000);

				// Neither console should update when view is minimized
				assertEquals(console1NameBeforeHide, console1.getName(), "Console name should not update when console view is minimized");
				assertEquals(console2NameBeforeHide, console2.getName(), "Console name should not update when console view is minimized");

				// Restore the console view - console1 should resume
				// updating, console2 should still not update because it's
				// hidden in the view
				activePage.setPartState(activePage.getReference(consoleView), IWorkbenchPage.STATE_RESTORED);
				activePage.activate(consoleView);
				TestUtil.processUIEvents(200);

				String console1NameAfterReshow = console1.getName();
				String console2NameAfterReshow = console2.getName();

				// Wait >1 second for update
				TestUtil.processUIEvents(2000);

				// Visible console should resume updating
				assertNotEquals(console1NameAfterReshow, console1.getName(), "Visible console should resume name updates after view is shown again");
				assertEquals(console2NameAfterReshow, console2.getName(), "Hidden console name should not update after view is restored");

				console1NameAfterReshow = console1.getName();
				console2NameAfterReshow = console2.getName();

				activePage.hideView(consoleView);

				// Wait >1 second for update
				TestUtil.processUIEvents(2000);
				assertEquals(console1NameAfterReshow, console1.getName(), "Console name should not update when console view is minimized");
				assertEquals(console2NameAfterReshow, console2.getName(), "Console name should not update when console view is minimized");

				mockProcess1.destroy();
				mockProcess2.destroy();
			} finally {
				activePage.hideView(consoleView);
				consoleManager.removeConsoles(List.of(console1, console2).toArray(new IConsole[0]));
				waitForConsoleRelatedJobs();
				console1.destroy();
				console2.destroy();
			}
		} finally {
			mockProcess1.destroy();
			mockProcess2.destroy();
		}
	}
}
