/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.testplugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Simple console line tracker extension point that collects the lines appended to the console.
 */
public class ConsoleLineTracker implements IConsoleLineTrackerExtension {

	private enum State {
		UNINITIALIZED, INITIALIZED, CLOSED,
	}

	private static final long WAIT_TIMEOUT_MS = 20000;
	private static final Object LOCK = new Object();
	private static IConsole console;
	private static List<IRegion> lines = new ArrayList<>();

	private static State state = State.UNINITIALIZED;

	@Override
	public void dispose() {
		// Called by the console line notifier right after consoleClosed(): keep the CLOSED state, tests rely on it
	}

	@Override
	public void init(IConsole c) {
		synchronized (LOCK) {
			ConsoleLineTracker.console = c;
			lines = new ArrayList<>();
			state = State.INITIALIZED;
		}
	}

	@Override
	public void lineAppended(IRegion line) {
		synchronized (LOCK) {
			lines.add(line);
		}
	}

	public static int getNumberOfMessages() {
		synchronized (LOCK) {
			return lines.size();
		}
	}

	public static String getMessage(int index) {
		IRegion lineRegion;
		IConsole c;
		synchronized (LOCK) {
			if (index >= lines.size()) {
				return null;
			}
			lineRegion = lines.get(index);
			c = console;
		}
		try {
			return c.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
		}
		catch (BadLocationException e) {
			return null;
		}
	}

	public static List<String> getAllMessages() {
		List<IRegion> regions;
		IConsole c;
		synchronized (LOCK) {
			regions = new ArrayList<>(lines);
			c = console;
		}
		List<String> all = new ArrayList<>(regions.size());
		for (IRegion lineRegion : regions) {
			try {
				all.add(c.getDocument().get(lineRegion.getOffset(), lineRegion.getLength()));
			}
			catch (BadLocationException e) {
				continue;
			}
		}
		return all;
	}

	public static IDocument getDocument() {
		synchronized (LOCK) {
			return console.getDocument();
		}
	}

	/**
	 * Waits (at most 20 seconds) until the last initialized console is closed.
	 */
	public static void waitForConsole() {
		long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
		synchronized (LOCK) {
			while (state != State.CLOSED) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					return;
				}
				try {
					LOCK.wait(remaining);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	@Override
	public void consoleClosed() {
		synchronized (LOCK) {
			state = State.CLOSED;
			LOCK.notifyAll();
		}
	}

	public static boolean isClosed() {
		synchronized (LOCK) {
			return state == State.CLOSED;
		}
	}
}
