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
		UNINITIALIZED, INITIALIZED, CLOSED, DISPOSED,
	}

	private static final Object LOCK = new Object();
	private static IConsole console;
	private static List<IRegion> lines = new ArrayList<>();

	private static State state = State.UNINITIALIZED;

	@Override
	public void dispose() {
		state = State.DISPOSED;
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
		lines.add(line);
	}

	public static int getNumberOfMessages() {
		return lines.size();
	}

	public static String getMessage(int index) {
		if (index < lines.size()) {
			IRegion lineRegion = lines.get(index);
			try {
				return console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
			}
			catch (BadLocationException e) {
				return null;
			}
		}
		return null;
	}

	public static List<String> getAllMessages() {
		List<String> all = new ArrayList<>(lines.size());
		for (IRegion lineRegion : lines) {
			try {
				all.add(console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength()));
			}
			catch (BadLocationException e) {
				continue;
			}
		}
		return all;
	}

	public static IDocument getDocument() {
		return console.getDocument();
	}

	public static void waitForConsole() {
		synchronized (LOCK) {
			if (state == State.CLOSED) {
				return;
			}
			try {
				LOCK.wait(20000);
			}
			catch (InterruptedException ie) {
				// do nothing
			}
		}
	}

	@Override
	public void consoleClosed() {
		synchronized (LOCK) {
			state = State.CLOSED;
			lines.notifyAll();
		}
	}

	public static boolean isClosed() {
		synchronized (LOCK) {
			return state == State.CLOSED;
		}
	}
}
