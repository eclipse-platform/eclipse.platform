/*******************************************************************************
 * Copyright (c) 2026 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.internal.console.ConsoleManager;
import org.eclipse.ui.internal.console.ConsoleView;
import org.eclipse.ui.internal.console.ConsoleViewConsoleFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests console manager's show/hide behavior when multiple consoles are shown
 * in the console view.
 */
@ExtendWith(DebugTestExtension.class)
public class ConsoleShowHideTests {

	private IConsoleManager manager;
	private int count;
	private ConsoleMock[] consoles;
	private ConsoleView consoleView;
	private ConsoleView consoleView2;
	private IWorkbenchPage activePage;

	@BeforeEach
	public void setUp() throws Exception {
		assertNotNull(Display.getCurrent(), "Must run in UI thread, but was in: " + Thread.currentThread().getName());
		count = 3;
		manager = ConsolePlugin.getDefault().getConsoleManager();
		activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		TestUtil.processUIEvents(100);
		consoles = new ConsoleMock[count];
		for (int i = 0; i < count; i++) {
			consoles[i] = new ConsoleMock(i + 1);
		}
		consoleView = (ConsoleView) activePage.showView("org.eclipse.ui.console.ConsoleView");
		activePage.activate(consoleView);
		TestUtil.processUIEvents(100);
	}

	@AfterEach
	public void tearDown() throws Exception {
		manager.removeConsoles(consoles);
		activePage.hideView(consoleView);
		if (consoleView2 != null) {
			activePage.hideView(consoleView2);
		}
		TestUtil.processUIEvents(100);
	}

	/**
	 * The test triggers {@link #count} sequential calls to the
	 * {@link IConsoleManager#showConsoleView(IConsole)} and checks if
	 * {@link IConsole#pageShown()} and {@link IConsole#pageHidden()} were
	 * properly called for each console.
	 */
	@Test
	public void testShowHideConsoles(TestInfo testInfo) throws Exception {
		// First time adding & showing consoles should trigger pageShown for
		// each console
		for (ConsoleMock console : consoles) {
			addConsole(console, testInfo.getDisplayName());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
		}

		// Page hidden should be called for all but the last console
		for (int i = 0; i < consoles.length; i++) {
			ConsoleMock console = consoles[i];
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			if (i == consoles.length - 1) {
				// last console should not be hidden
				assertEquals(0, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
			} else {
				assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
			}
		}

		clearConsoleCounts();

		// Second time showing consoles should trigger pageShown for each
		// console
		for (ConsoleMock console : consoles) {
			showConsole(console, testInfo.getDisplayName());
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
		}

		// Page hidden should be called for all consoles
		for (ConsoleMock console : consoles) {
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
		}

		clearConsoleCounts();

		// Last console should be shown now.
		// Close consoles one by one in reverse order.
		for (int i = consoles.length - 1; i >= 0; i--) {
			ConsoleMock console = consoles[i];
			removeConsole(console, testInfo.getDisplayName());
			if (i == consoles.length - 1) {
				// last console should not be shown again
				assertEquals(0, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			} else {
				// Other consoles should be shown again when the previous one is
				// closed
				assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			}
			assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
		}
	}

	/**
	 * The test triggers {@link #count} sequential calls to the
	 * {@link IConsoleManager#showConsoleView(IConsole)} and checks if
	 * {@link IConsole#pageShown()} and {@link IConsole#pageHidden()} were
	 * properly called for each console, if there are two console views showing
	 * the same consoles.
	 */
	@Test
	public void testShowHideConsolesWithTwoViews(TestInfo testInfo) throws Exception {
		assertSame(consoleView, activePage.getActivePart());

		ConsoleViewConsoleFactoryForTest factory = new ConsoleViewConsoleFactoryForTest();
		factory.setConsoleView(consoleView);
		factory.openConsole();
		TestUtil.processUIEvents(100);

		// Second console view should be opened and active
		consoleView2 = (ConsoleView) activePage.getActivePart();
		assertNotSame(consoleView, consoleView2);

		// First time adding & showing consoles should trigger pageShown for
		// each console (on creation)
		for (ConsoleMock console : consoles) {
			addConsole(console, testInfo.getDisplayName());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
		}

		// Page hidden should be called for all but the last console
		for (int i = 0; i < consoles.length; i++) {
			ConsoleMock console = consoles[i];
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			if (i == consoles.length - 1) {
				// last console should not be hidden
				assertEquals(0, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
			} else {
				assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
			}
		}

		// Activate first console view again
		activePage.activate(consoleView);
		TestUtil.processUIEvents(100);
		assertSame(consoleView, activePage.getActivePart());

		clearConsoleCounts();

		// Second time showing consoles should trigger pageShown for each
		// console
		for (ConsoleMock console : consoles) {
			showConsole(console, testInfo.getDisplayName());
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
		}

		// Page hidden should be called for all consoles (last one should be
		// hidden now, as it was shown on top)
		for (ConsoleMock console : consoles) {
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
		}

		// Activate second console view again
		activePage.activate(consoleView2);
		TestUtil.processUIEvents(100);
		assertSame(consoleView2, activePage.getActivePart());

		clearConsoleCounts();

		// Second time showing consoles should trigger pageShown for each
		// console (last one should be shown on top again)
		for (ConsoleMock console : consoles) {
			showConsole(console, testInfo.getDisplayName());
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
		}

		// Page hidden should be called for all consoles (last one should be
		// hidden now, as it was shown on top)
		for (ConsoleMock console : consoles) {
			assertEquals(1, console.showCalled.get());
			assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
		}

		clearConsoleCounts();

		// Last console should be shown now.
		// Close consoles one by one in reverse order.
		for (int i = consoles.length - 1; i >= 0; i--) {
			ConsoleMock console = consoles[i];
			removeConsole(console, testInfo.getDisplayName());
			if (i == consoles.length - 1) {
				// last console should not be shown again
				assertEquals(0, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			} else {
				// Other consoles should be shown again when the previous one is
				// closed
				assertEquals(1, console.pageShownCalled.get(), console + " was shown unexpected number of times: " + console.pageShownCalled.get());
			}
			assertEquals(1, console.pageHiddenCalled.get(), console + " was hidden unexpected number of times: " + console.pageHiddenCalled.get());
		}
	}

	private void clearConsoleCounts() {
		for (ConsoleMock console : consoles) {
			console.showCalled.set(0);
			console.pageShownCalled.set(0);
			console.pageHiddenCalled.set(0);
		}
	}

	private void addConsole(final ConsoleMock console, String testName) {
		System.out.println("Requesting to add: " + console); //$NON-NLS-1$
		manager.addConsoles(new IConsole[] { console });
		TestUtil.waitForJobs(testName, ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
	}

	private void removeConsole(final ConsoleMock console, String testName) {
		System.out.println("Requesting to remove: " + console); //$NON-NLS-1$
		manager.removeConsoles(new IConsole[] { console });
		TestUtil.waitForJobs(testName, ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
	}

	private void showConsole(final ConsoleMock console, String testName) {
		System.out.println("Requesting to show: " + console); //$NON-NLS-1$
		manager.showConsoleView(console);
		TestUtil.waitForJobs(testName, ConsoleManager.CONSOLE_JOB_FAMILY, 200, 5000);
	}

	class ConsoleViewConsoleFactoryForTest extends ConsoleViewConsoleFactory {

		@Override
		protected boolean handleAutoPin() {
			// Override super to avoid dialogs about pinning consoles in the
			// view, which would require user interaction and thus fail the
			// test.
			// No pinning required
			return false;
		}
	}

}
