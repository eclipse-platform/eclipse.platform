/*******************************************************************************
 * Copyright (c) 2016, 2017 Andrey Loskutov and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests console manager
 */
public class ConsoleManagerTests extends AbstractDebugTest {

	private ExecutorService executorService;
	private IConsoleManager manager;
	private int count;
	private CountDownLatch latch;
	private ConsoleMock[] consoles;
	ConsoleMock firstConsole;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		assertNotNull("Must run in UI thread, but was in: " + Thread.currentThread().getName(), //$NON-NLS-1$
				Display.getCurrent());
		count = 20;
		latch = new CountDownLatch(count);
		executorService = Executors.newFixedThreadPool(count);
		manager = ConsolePlugin.getDefault().getConsoleManager();
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		TestUtil.processUIEvents(100);
		consoles = new ConsoleMock[count];
		for (int i = 0; i < count; i++) {
			final ConsoleMock console = new ConsoleMock(i + 1);
			consoles[i] = console;
		}
		// register consoles (this does *not* show anything)
		manager.addConsoles(consoles);

		IViewPart consoleView = activePage.showView("org.eclipse.ui.console.ConsoleView"); //$NON-NLS-1$
		activePage.activate(consoleView);
		TestUtil.processUIEvents(100);

		// The test is unstable ("show" event on the the first console seem to
		// be not always sent), so make sure console view has shown at least
		// one console for real before starting the main test
		firstConsole = new ConsoleMock(0);
		manager.addConsoles(new ConsoleMock[] { firstConsole });
		manager.showConsoleView(firstConsole);
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
		TestUtil.processUIEvents(100);
		ConsoleMock.allShownConsoles.set(0);
	}

	@Override @After
	public void tearDown() throws Exception {
		executorService.shutdownNow();
		manager.removeConsoles(consoles);
		manager.removeConsoles(new ConsoleMock[] { firstConsole });
		TestUtil.processUIEvents(100);
		super.tearDown();
	}

	/**
	 * The test triggers {@link #count} simultaneous calls to the
	 * {@link IConsoleManager#showConsoleView(IConsole)} and checks if all of
	 * this calls were properly proceeded by the manager.
	 * <p>
	 * See bug 489546.
	 *
	 * @throws Exception
	 */
	@Test
	public void testShowAllConsoles() throws Exception {
		// Create a number of threads which will start and wait for the last one
		// created to call ConsoleManager.show.
		for (ConsoleMock console : consoles) {
			showConsole(console);
		}
		System.out.println("All tasks scheduled, processing UI events now..."); //$NON-NLS-1$
		TestUtil.processUIEvents(1000);

		// Console manager starts a job with delay, let wait for him a bit
		System.out.println("Waiting on jobs now..."); //$NON-NLS-1$
		TestUtil.waitForJobs(name.getMethodName(), 200, 5000);

		// Give UI a chance to proceed pending console manager jobs
		System.out.println("Done with jobs, processing UI events again..."); //$NON-NLS-1$
		TestUtil.processUIEvents(3000);

		executorService.shutdown();

		System.out.println("Waiting on execution service to finish..."); //$NON-NLS-1$
		boolean OK = waitForExecutorService();
		if (!OK) {
			System.out.println("Timed out..."); //$NON-NLS-1$
			TestUtil.processUIEvents(10000);

			// timeout?
			assertTrue("Timeout occurred while waiting on console to be shown", //$NON-NLS-1$
					waitForExecutorService());
		} else {
			System.out.println("Done waiting on execution service to finish"); //$NON-NLS-1$
		}
		int shown = ConsoleMock.allShownConsoles.intValue();
		assertEquals("Only " + shown + " consoles were shown from " + count, count, shown); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean waitForExecutorService() throws Exception {
		for (int i = 0; i < 60; i++) {
			if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
				return true;
			}
			TestUtil.processUIEvents(100);
		}
		return false;
	}

	private void showConsole(final ConsoleMock console) {
		executorService.execute(() -> {
			// last one arriving here triggers execution for all at same
			// time
			latch.countDown();
			try {
				latch.await(1, TimeUnit.MINUTES);
				System.out.println("Requesting to show: " + console); //$NON-NLS-1$
				manager.showConsoleView(console);
				TestUtil.waitForJobs(name.getMethodName(), 200, 5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.interrupted();
			}
		});
	}

	/**
	 * Dummy console page showing mock number and counting the numbers its
	 * control was shown in the console view.
	 */
	static final class ConsoleMock implements IConsole {
		MessagePage page;
		final AtomicInteger showCalled;
		final int number;
		final static AtomicInteger allShownConsoles = new AtomicInteger();

		public ConsoleMock(int number) {
			this.number = number;
			showCalled = new AtomicInteger();
		}

		@Override
		public void removePropertyChangeListener(IPropertyChangeListener listener) {
		}

		@Override
		public String getType() {
			return null;
		}

		@Override
		public String getName() {
			return toString();
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public void addPropertyChangeListener(IPropertyChangeListener listener) {
		}

		/**
		 * Just a page showing the mock console name
		 */
		@Override
		public IPageBookViewPage createPage(IConsoleView view) {
			page = new MessagePage() {
				@Override
				public void createControl(Composite parent) {
					super.createControl(parent);
					// This listener is get called if the page is really shown
					// in the console view
					getControl().addListener(SWT.Show, event -> {
						int count = showCalled.incrementAndGet();
						if (count == 1) {
							count = allShownConsoles.incrementAndGet();
							System.out.println("Shown: " + ConsoleMock.this + ", overall: " + count); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				}

			};
			page.setMessage(toString());
			return page;
		}

		@Override
		public String toString() {
			return "mock #" + number; //$NON-NLS-1$
		}
	}

}
