/*******************************************************************************
 * Copyright (c) 2017, 2020 Andreas Loth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andreas Loth - initial API and implementation
 *******************************************************************************/

package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.commands.Command;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.junit.Test;


public class ConsoleTests extends AbstractDebugTest {

	@Test
	public void testConsoleOutputStreamEncoding() throws IOException {
		String testString = "abc\u00e4\u00f6\u00fcdef"; //$NON-NLS-1$
		// abcdef need 1 byte in UTF-8 each
		// Ã¤Ã¶Ã¼ (\u00e4\u00f6\u00fc) need 2 bytes each
		byte[] testStringBuffer = testString.getBytes(StandardCharsets.UTF_8);
		assertEquals("Test string \"" + testString + "\" should consist of 12 UTF-8 bytes", 12, testStringBuffer.length); //$NON-NLS-1$ //$NON-NLS-2$
		MessageConsole console = new MessageConsole("Test Console", //$NON-NLS-1$
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
		assertEquals("Document should be empty", "", document.get()); //$NON-NLS-1$ //$NON-NLS-2$
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write(testStringBuffer, 0, 6);
			// half of Ã¶ (\u00f6) is written so we don't expect this char in
			// output but all previous chars can be decoded
			TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
			assertEquals("First 4 chars should be written", testString.substring(0, 4), document.get()); //$NON-NLS-1$
			outStream.write(testStringBuffer, 6, 6);
			// all remaining bytes are written so we expect the whole string
			// including the Ã¶ (\u00f6) which was at buffer boundary
			TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
			assertEquals("whole test string should be written", testString, document.get()); //$NON-NLS-1$
		}
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
		// after closing the stream, the document content should still be the
		// same
		assertEquals("closing the stream should not alter the document", testString, document.get()); //$NON-NLS-1$
	}

	@Test
	public void testConsoleOutputStreamLastR() throws IOException {
		String testString = "a\r"; //$NON-NLS-1$
		byte[] testStringBuffer = testString.getBytes(StandardCharsets.UTF_8);
		assertEquals("Test string \"" + testString + "\" should consist of 2 UTF-8 bytes", 2, testStringBuffer.length); //$NON-NLS-1$ //$NON-NLS-2$
		MessageConsole console = new MessageConsole("Test Console 2", //$NON-NLS-1$
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
		assertEquals("Document should be empty", "", document.get()); //$NON-NLS-1$ //$NON-NLS-2$
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write(testStringBuffer);
			// everything but pending \r should be written
			TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
			assertEquals("First char should be written", testString.substring(0, 1), document.get()); //$NON-NLS-1$
		}
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
		// after closing the stream, the document content should still be the
		// same
		assertEquals("closing the stream should write the pending \\r", testString, document.get()); //$NON-NLS-1$
	}

	@Test
	public void testConsoleOutputStreamDocumentClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 3", //$NON-NLS-1$
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("write1"); //$NON-NLS-1$
			document.getDocumentPartitioner().disconnect();
			try {
				outStream.write("write2"); //$NON-NLS-1$
				fail("IOException with message \"Document is closed\" expected"); //$NON-NLS-1$
			} catch (IOException ioe) {
				assertEquals("Document is closed", ioe.getMessage()); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testConsoleOutputStreamClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 4", //$NON-NLS-1$
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("test1".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$
			outStream.close();
			try {
				outStream.write("test2".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$
				fail("IOException with message \"Output Stream is closed\" expected"); //$NON-NLS-1$
			} catch (IOException ioe) {
				assertEquals("Output Stream is closed", ioe.getMessage()); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testConsoleOutputStreamDocumentStreamClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 5", //$NON-NLS-1$
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("write1"); //$NON-NLS-1$
			document.getDocumentPartitioner().disconnect();
			try {
				outStream.write("write2"); //$NON-NLS-1$
				fail("IOException with message \"Document is closed\" expected"); //$NON-NLS-1$
			} catch (IOException ioe) {
				assertEquals("Document is closed", ioe.getMessage()); //$NON-NLS-1$
			}
			try {
				outStream.write("write3"); //$NON-NLS-1$
				fail("IOException with message \"Output Stream is closed\" expected"); //$NON-NLS-1$
			} catch (IOException ioe) {
				assertEquals("Output Stream is closed", ioe.getMessage()); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSetNullEncoding() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 6", null); //$NON-NLS-1$
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.setEncoding(null);
		}
	}

	/**
	 * Validate that we can use commands findReplace, findNext and findPrevious
	 * after opening a console in the Console View.
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=268608">bug
	 *      268608</a>
	 */
	@Test
	public void testFindCommandsAreEnabledOnConsoleOpen() throws Exception {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart consoleView = activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW);

		IOConsole console = new IOConsole("Test Console 7", IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, true); //$NON-NLS-1$
		console.getDocument().set("some text"); //$NON-NLS-1$

		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = { console };

		try {
			consoleManager.addConsoles(consoles);
			consoleManager.showConsoleView(console);
			TestUtil.waitForJobs(name.getMethodName(), 100, 3000);

			ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			Command commandFindReplace = commandService.getCommand(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
			assertTrue("expected FindReplace command to be enabled after opening console", commandFindReplace.isEnabled());
			Command commandFindNext = commandService.getCommand(IWorkbenchActionDefinitionIds.FIND_NEXT);
			assertTrue("expected FindNext command to be enabled after opening console", commandFindNext.isEnabled());
			Command commandFindPrevious = commandService.getCommand(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
			assertTrue("expected FindPrevious command to be enabled after opening console", commandFindPrevious.isEnabled());
		} finally {
			consoleManager.removeConsoles(consoles);
			activePage.hideView(consoleView);
		}
	}

	/**
	 * Tests for IOConsoleInputStream#available().
	 *
	 * @throws Exception if test fails
	 */
	@Test
	public void testIOConsoleAvailable() throws Exception {
		IOConsole console = new IOConsole("", null);
		try (InputStream consoleInput = console.getInputStream()) {
			consoleInput.available();
			consoleInput.available();
		}

		console = new IOConsole("", null);
		try (InputStream consoleInput = console.getInputStream()) {
			consoleInput.available();
			new Thread(() -> {
				try {
					Thread.sleep(100);
					consoleInput.close();
				} catch (Exception e) {
				}
			}).start();
			assertEquals("read() did not signal EOF.", -1, consoleInput.read());
		}

		console = new IOConsole("", null);
		try (InputStream consoleInput = console.getInputStream()) {
			consoleInput.close();
			consoleInput.available();
			consoleInput.available();
		}
	}
}
