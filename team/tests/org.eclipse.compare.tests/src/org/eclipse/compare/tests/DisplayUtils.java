/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.BooleanSupplier;

import org.eclipse.swt.widgets.Display;

/**
 * Helpers for tests that need to drive the SWT event loop.
 */
public class DisplayUtils {

	/**
	 * Runs the event loop of the current display until the given condition holds,
	 * and fails the test if it does not hold within the timeout.
	 * <p>
	 * Must be called on the UI thread.
	 * </p>
	 *
	 * @param condition     checked before each dispatch; the method returns as soon
	 *                      as it yields {@code true}
	 * @param failMessage   start of the failure message if the timeout expires
	 * @param timeoutMillis time in milliseconds after which the test fails
	 */
	public static void pumpUntil(BooleanSupplier condition, String failMessage, long timeoutMillis) {
		Display display = Display.getCurrent();
		long deadline = System.currentTimeMillis() + timeoutMillis;
		// A self-rescheduling timer keeps the loop waking so the deadline is
		// enforced even while blocked in Display.sleep().
		Runnable[] wake = new Runnable[1];
		wake[0] = () -> display.timerExec(50, wake[0]);
		display.timerExec(50, wake[0]);
		try {
			while (!condition.getAsBoolean()) {
				if (System.currentTimeMillis() > deadline) {
					fail(failMessage + " within " + timeoutMillis + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} finally {
			display.timerExec(-1, wake[0]);
		}
	}

	/**
	 * Dispatches all events currently queued on the current display and returns
	 * once the queue is empty. Unlike {@link #pumpUntil}, it does not wait for
	 * anything that has not been queued yet.
	 * <p>
	 * Must be called on the UI thread.
	 * </p>
	 */
	public static void processQueuedEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// drain the event queue
		}
	}
}
