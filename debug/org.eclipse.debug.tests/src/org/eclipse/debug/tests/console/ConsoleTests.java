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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.tests.ConsoleExtension;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.console.ConsoleManager;
import org.eclipse.ui.internal.console.ConsoleZoomHandler;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ DebugTestExtension.class, ConsoleExtension.class })
public class ConsoleTests {

	@Test
	public void testConsoleOutputStreamEncoding(TestInfo testInfo) throws IOException {
		String testString = "abc\u00e4\u00f6\u00fcdef";
		// abcdef need 1 byte in UTF-8 each
		// Ã¤Ã¶Ã¼ (\u00e4\u00f6\u00fc) need 2 bytes each
		byte[] testStringBuffer = testString.getBytes(StandardCharsets.UTF_8);
		assertThat(testStringBuffer).as("Test string \"" + testString + "\" should consist of 12 UTF-8 bytes").hasSize(12);
		MessageConsole console = new MessageConsole("Test Console",
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
		assertEquals("", document.get(), "Document should be empty");
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write(testStringBuffer, 0, 6);
			// half of Ã¶ (\u00f6) is written so we don't expect this char in
			// output but all previous chars can be decoded
			TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
			assertEquals(testString.substring(0, 4), document.get(), "First 4 chars should be written");
			outStream.write(testStringBuffer, 6, 6);
			// all remaining bytes are written so we expect the whole string
			// including the Ã¶ (\u00f6) which was at buffer boundary
			TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
			assertEquals(testString, document.get(), "whole test string should be written");
		}
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
		// after closing the stream, the document content should still be the
		// same
		assertEquals(testString, document.get(), "closing the stream should not alter the document");
	}

	@Test
	public void testConsoleOutputStreamLastR(TestInfo testInfo) throws IOException {
		String testString = "a\r";
		byte[] testStringBuffer = testString.getBytes(StandardCharsets.UTF_8);
		assertThat(testStringBuffer).as("Test string \"" + testString + "\" should consist of 2 UTF-8 bytes").hasSize(2);
		MessageConsole console = new MessageConsole("Test Console 2",
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
		assertEquals("", document.get(), "Document should be empty");
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write(testStringBuffer);
			// everything but pending \r should be written
			TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
			assertEquals(testString.substring(0, 1), document.get(), "First char should be written");
		}
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
		// after closing the stream, the document content should still be the
		// same
		assertEquals(testString, document.get(), "closing the stream should write the pending \\r");
	}

	@Test
	public void testConsoleOutputStreamDocumentClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 3",
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("write1");
			document.getDocumentPartitioner().disconnect();
			try {
				outStream.write("write2");
				fail("IOException with message \"Document is closed\" expected");
			} catch (IOException ioe) {
				assertEquals("Document is closed", ioe.getMessage());
			}
		}
	}

	@Test
	public void testConsoleOutputStreamClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 4",
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("test1".getBytes(StandardCharsets.UTF_8));
			outStream.close();
			try {
				outStream.write("test2".getBytes(StandardCharsets.UTF_8));
				fail("IOException with message \"Output Stream is closed\" expected");
			} catch (IOException ioe) {
				assertEquals("Output Stream is closed", ioe.getMessage());
			}
		}
	}

	@Test
	public void testConsoleOutputStreamDocumentStreamClosed() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 5",
				IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_8.name(), true);
		IDocument document = console.getDocument();
		try (IOConsoleOutputStream outStream = console.newOutputStream()) {
			outStream.write("write1");
			document.getDocumentPartitioner().disconnect();
			try {
				outStream.write("write2");
				fail("IOException with message \"Document is closed\" expected");
			} catch (IOException ioe) {
				assertEquals("Document is closed", ioe.getMessage());
			}
			try {
				outStream.write("write3");
				fail("IOException with message \"Output Stream is closed\" expected");
			} catch (IOException ioe) {
				assertEquals("Output Stream is closed", ioe.getMessage());
			}
		}
	}

	@Test
	public void testSetNullEncoding() throws IOException {
		MessageConsole console = new MessageConsole("Test Console 6", null);
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
	public void testFindCommandsAreEnabledOnConsoleOpen(TestInfo testInfo) throws Exception {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart consoleView = activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW);

		IOConsole console = new IOConsole("Test Console 7", IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, true);
		console.getDocument().set("some text");

		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = { console };

		try {
			consoleManager.addConsoles(consoles);
			consoleManager.showConsoleView(console);
			TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 100, 3000);

			ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			Command commandFindReplace = commandService.getCommand(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
			assertTrue(commandFindReplace.isEnabled(), "expected FindReplace command to be enabled after opening console");
			Command commandFindNext = commandService.getCommand(IWorkbenchActionDefinitionIds.FIND_NEXT);
			assertTrue(commandFindNext.isEnabled(), "expected FindNext command to be enabled after opening console");
			Command commandFindPrevious = commandService.getCommand(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
			assertTrue(commandFindPrevious.isEnabled(), "expected FindPrevious command to be enabled after opening console");
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
			assertEquals(-1, consoleInput.read(), "read() did not signal EOF.");
		}

		console = new IOConsole("", null);
		try (InputStream consoleInput = console.getInputStream()) {
			consoleInput.close();
			consoleInput.available();
			consoleInput.available();
		}
	}

	/**
	 * Zooming in must only change the font size of the console currently
	 * shown in the active console view; a different console (of a different
	 * type, with its own natural font) that was already open must not be
	 * affected.
	 */
	@Test
	public void testZoomOnlyAffectsActiveConsoleType(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole activeConsole = createConsole("Zoom Active Console", uniqueConsoleType("active"));
		MessageConsole otherConsole = createConsole("Zoom Other Console", uniqueConsoleType("other"));
		try {
			showConsoles(testInfo, activeConsole, otherConsole);

			int activeInitialHeight = getFontHeight(activeConsole);
			int otherInitialHeight = getFontHeight(otherConsole);

			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();

			assertEquals(activeInitialHeight + 1, getFontHeight(activeConsole),
					"zooming should increase the font size of the console shown in the active console view");
			assertEquals(otherInitialHeight, getFontHeight(otherConsole),
					"zooming must not affect a console of a different type that was already open");
		} finally {
			removeConsoles(activeConsole, otherConsole);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * A font change coming from somewhere else (e.g. a preference page) must
	 * be able to override the current zoom instead of being silently
	 * reverted.
	 * <p>
	 * The very first external font change on a freshly zoomed console is
	 * reasserted once (to cope with consoles, such as {@code ProcessConsole},
	 * that set their own font asynchronously right after being zoomed), so
	 * this test performs such a change first and confirms it is reverted,
	 * before confirming that a second, later change is genuinely honored.
	 * </p>
	 */
	@Test
	public void testPreferenceFontChangeOverridesZoom(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Preference Console", uniqueConsoleType("pref"));
		Font reassertedFont = null;
		Font preferenceFont = null;
		try {
			showConsoles(testInfo, console);

			int naturalHeight = getFontHeight(console);

			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();
			int zoomedHeight = getFontHeight(console);
			assertEquals(naturalHeight + 1, zoomedHeight, "zoom should have increased the font size");

			// simulate the console re-asserting its own natural font once, right
			// after being zoomed (mirrors e.g. ProcessConsole's asynchronous font
			// initialization) - this first external change is expected to be
			// reverted back to the current zoom level
			reassertedFont = withHeight(console.getFont(), naturalHeight);
			console.setFont(reassertedFont);
			TestUtil.processUIEvents();
			assertEquals(zoomedHeight, getFontHeight(console),
					"the console's own re-assertion of its natural font should have been reverted back to the current zoom level");

			// simulate an actual, deliberate font change from a preference page:
			// this must be accepted as the new base font, overriding the zoom
			int preferenceHeight = naturalHeight + 5;
			preferenceFont = withHeight(console.getFont(), preferenceHeight);
			console.setFont(preferenceFont);
			TestUtil.processUIEvents();
			assertEquals(preferenceHeight, getFontHeight(console),
					"a later, deliberate font change (e.g. from a preference page) should override the current zoom");
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
			dispose(reassertedFont, preferenceFont);
		}
	}

	/**
	 * The current zoom level (base font height and delta) must be persisted
	 * to the console plug-in's preferences as soon as a zoom step is applied.
	 */
	@Test
	public void testZoomLevelIsPersisted(TestInfo testInfo) throws Exception {
		String type = uniqueConsoleType("persist");

		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Persistence Console", type);
		try {
			showConsoles(testInfo, console);

			int naturalHeight = getFontHeight(console);

			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();

			String expectedEntry = type + "=" + naturalHeight + "|" + 1;
			assertThat(getPersistedZoomState())
					.as("persisted zoom state should contain an entry for the zoomed console type")
					.contains(expectedEntry);
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * A console added later, of a type that has already been zoomed, must
	 * immediately start at the current zoom level for that type - even
	 * though it was never itself shown in the active console view.
	 */
	@Test
	public void testNewConsoleOfAlreadyZoomedTypeInheritsZoom(TestInfo testInfo) throws Exception {
		String type = uniqueConsoleType("inherit");

		IConsoleView consoleView = showConsoleView();
		MessageConsole firstConsole = createConsole("Zoom Inherit Console 1", type);
		MessageConsole secondConsole = createConsole("Zoom Inherit Console 2", type);
		try {
			showConsoles(testInfo, firstConsole);

			int naturalHeight = getFontHeight(firstConsole);
			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();
			int zoomedHeight = getFontHeight(firstConsole);
			assertEquals(naturalHeight + 1, zoomedHeight, "zoom should have increased the font size");

			// a second console of the SAME type, added afterwards and never shown
			// in the console view, should still start at the current zoom level
			addConsoles(testInfo, secondConsole);

			assertEquals(zoomedHeight, getFontHeight(secondConsole),
					"a newly added console of an already zoomed type should immediately start at the current zoom level");
		} finally {
			removeConsoles(firstConsole, secondConsole);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * Zooming out must decrease the font size, mirroring zoom in.
	 */
	@Test
	public void testZoomOutDecreasesFontSize(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Out Console", uniqueConsoleType("zoomOut"));
		try {
			showConsoles(testInfo, console);

			int naturalHeight = getFontHeight(console);

			// zoom in twice, then out once: should settle one step above natural
			ConsoleZoomHandler.applyZoom(consoleView, 1);
			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();
			assertEquals(naturalHeight + 2, getFontHeight(console), "two zoom-in steps should increase the font size by 2");

			ConsoleZoomHandler.applyZoom(consoleView, -1);
			TestUtil.processUIEvents();
			assertEquals(naturalHeight + 1, getFontHeight(console), "a zoom-out step should decrease the font size");

			// zoom back down to (and past) the natural size
			ConsoleZoomHandler.applyZoom(consoleView, -1);
			ConsoleZoomHandler.applyZoom(consoleView, -1);
			TestUtil.processUIEvents();
			assertEquals(naturalHeight - 1, getFontHeight(console),
					"zooming out below the natural size should be possible");
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * The font height must never go below the minimum, nor above the maximum
	 * supported font size, regardless of how large a zoom delta is applied.
	 */
	@Test
	public void testZoomClampsAtMinimumAndMaximumFontSize(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Clamp Console", uniqueConsoleType("clamp"));
		try {
			showConsoles(testInfo, console);

			ConsoleZoomHandler.applyZoom(consoleView, 1000);
			TestUtil.processUIEvents();
			assertEquals(ConsoleZoomHandler.MAX_FONT_SIZE, getFontHeight(console),
					"zooming in by a huge delta should clamp at the maximum font size");

			ConsoleZoomHandler.applyZoom(consoleView, -10000);
			TestUtil.processUIEvents();
			assertEquals(ConsoleZoomHandler.MIN_FONT_SIZE, getFontHeight(console),
					"zooming out by a huge delta should clamp at the minimum font size");
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * When a console is removed, its custom zoom font must be disposed so it
	 * is not leaked (see also the {@code SWT Resource was not properly
	 * disposed} regression this guards against).
	 */
	@Test
	public void testRemovingConsoleDisposesZoomFont(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Dispose Console", uniqueConsoleType("disposeOnRemove"));
		try {
			showConsoles(testInfo, console);

			ConsoleZoomHandler.applyZoom(consoleView, 1);
			TestUtil.processUIEvents();

			Font zoomFont = console.getFont();
			assertFalse(zoomFont.isDisposed(), "the zoom font must not be disposed while its console is still open");

			removeConsoles(console);
			TestUtil.processUIEvents(200);
			long startTime = System.currentTimeMillis();
			while (getConsoleManager().getConsoles().length > 0 && System.currentTimeMillis() - startTime < 60_000) {
				TestUtil.processUIEvents(200);
			}
			assertEquals(0, getConsoleManager().getConsoles().length, "Should have no consoles after removal, but some are still present: " + Arrays.toString(getConsoleManager().getConsoles()));
			assertTrue(zoomFont.isDisposed(), "the zoom font must be disposed once its console is removed");
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * Executing the actual registered zoom-in command (as a keybinding would)
	 * while the console view is the active part must zoom the console it is
	 * currently showing.
	 */
	@Test
	public void testZoomCommandExecutesWhileConsoleViewActive(TestInfo testInfo) throws Exception {
		IConsoleView consoleView = showConsoleView();
		MessageConsole console = createConsole("Zoom Command Console", uniqueConsoleType("command"));
		try {
			showConsoles(testInfo, console);
			consoleView.getSite().getPage().activate(consoleView);
			TestUtil.processUIEvents();

			int naturalHeight = getFontHeight(console);

			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			handlerService.executeCommand("org.eclipse.ui.console.command.fontZoomIn", null);
			TestUtil.processUIEvents();

			assertEquals(naturalHeight + 1, getFontHeight(console),
					"executing the zoom-in command should increase the font size of the active console");

			handlerService.executeCommand("org.eclipse.ui.console.command.fontZoomOut", null);
			TestUtil.processUIEvents();

			assertEquals(naturalHeight, getFontHeight(console),
					"executing the zoom-out command should decrease the font size of the active console");
		} finally {
			removeConsoles(console);
			hideConsoleView(consoleView);
		}
	}

	/**
	 * Opens (or reveals) the Console view.
	 */
	private static IConsoleView showConsoleView() throws PartInitException {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return (IConsoleView) activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW);
	}

	private static void hideConsoleView(IConsoleView consoleView) {
		consoleView.getSite().getPage().hideView(consoleView);
	}

	/**
	 * Creates - but does not yet register - a console of the given type.
	 */
	private static MessageConsole createConsole(String name, String type) {
		return new MessageConsole(name, type, null, StandardCharsets.UTF_8.name(), true);
	}

	/**
	 * Registers the given consoles, shows the first one in the Console view and
	 * waits until their fonts are initialized.
	 */
	private static void showConsoles(TestInfo testInfo, IConsole... consoles) {
		getConsoleManager().addConsoles(consoles);
		getConsoleManager().showConsoleView(consoles[0]);
		waitForConsoles(testInfo);
	}

	/**
	 * Registers the given consoles without showing them, and waits until their
	 * fonts are initialized.
	 */
	private static void addConsoles(TestInfo testInfo, IConsole... consoles) {
		getConsoleManager().addConsoles(consoles);
		waitForConsoles(testInfo);
	}

	/**
	 * Unregisters the given consoles. Consoles which are not registered (anymore)
	 * are silently ignored, so this is safe to call from a test's cleanup.
	 */
	private static void removeConsoles(IConsole... consoles) {
		getConsoleManager().removeConsoles(consoles);
	}

	/**
	 * Waits until all pending console jobs and UI events have been processed.
	 */
	private static void waitForConsoles(TestInfo testInfo) {
		TestUtil.waitForJobs(testInfo.getDisplayName(), ConsoleManager.CONSOLE_JOB_FAMILY, 100, 3000);
		TestUtil.processUIEvents();
	}

	private static IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

	private static void dispose(Font... fonts) {
		for (Font font : fonts) {
			if (font != null && !font.isDisposed()) {
				font.dispose();
			}
		}
	}

	private static String uniqueConsoleType(String suffix) {
		return "org.eclipse.debug.tests.console.zoomTest." + suffix + "." + System.nanoTime();
	}

	private static int getFontHeight(TextConsole console) {
		Font font = console.getFont();
		FontData[] fontData = font.getFontData();
		return fontData[0].getHeight();
	}

	private static Font withHeight(Font font, int height) {
		FontData fontData = font.getFontData()[0];
		return new Font(font.getDevice(), fontData.getName(), height, fontData.getStyle());
	}

	/**
	 * Reads the raw, persisted per-console-type zoom state string.
	 */
	private static String getPersistedZoomState() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(ConsolePlugin.getUniqueIdentifier());
		return preferences.get(ConsoleZoomHandler.PREF_ZOOM_FONT_HEIGHTS, "");
	}
}
