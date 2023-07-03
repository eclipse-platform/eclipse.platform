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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.internal;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.internal.base.HelpApplication;
import org.eclipse.swt.widgets.Display;

public class HelpUIEventLoop {
	/**
	 * Indicates whether run had a chance to execute and display got created
	 */
	private static boolean started = false;
	/**
	 * Indicates whether it is still running
	 */
	private static boolean running = false;
	private static Display display;

	/**
	 * Called by base in stand-alone help since it cannot run event loop
	 */
	public static void run() {
		try {
			if (display == null)
				display = Display.getCurrent();
			if (display == null)
				display = new Display();
		} finally {
			started = true;
		}
		try {
			running = true;
			while (HelpApplication.isRunning()) {
				try {
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				} catch (Throwable t) {
					ILog.of(HelpUIEventLoop.class).error(t.getMessage(), t);
				}
			}
			display.dispose();
			display = null;
		} finally {
			running = false;
		}
	}

	public static void wakeup() {
		Display d = display;
		if (d != null)
			try {
				d.wake();
			} catch (Exception e) {
			}
	}

	/**
	 * Blocks until the loop is started (Display created)
	 */
	public static void waitFor() {
		while (!started && HelpApplication.isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
			}
		}
	}

	/**
	 * @return Returns if loop is running.
	 */
	public static boolean isRunning() {
		return running;
	}
}
