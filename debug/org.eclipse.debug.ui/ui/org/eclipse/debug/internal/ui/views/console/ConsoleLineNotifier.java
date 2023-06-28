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
package org.eclipse.debug.internal.ui.views.console;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Tracks text appended to the console and notifies listeners in terms of whole
 * lines.
 */
public class ConsoleLineNotifier implements IPatternMatchListener, IPropertyChangeListener {
	/**
	 * Console listeners
	 */
	private List<IConsoleLineTracker> fListeners = new ArrayList<>(2);

	/**
	 * The console this notifier is tracking
	 */
	private ProcessConsole fConsole = null;

	@Override
	public void connect(TextConsole console) {
		if (console instanceof ProcessConsole) {
			fConsole = (ProcessConsole)console;

			for (IConsoleLineTracker lineTracker : DebugUIPlugin.getDefault().getProcessConsoleManager().getLineTrackers(fConsole.getProcess())) {
				lineTracker.init(fConsole);
				addConsoleListener(lineTracker);
			}

			fConsole.addPropertyChangeListener(this);
		}
	}

	@Override
	public synchronized void disconnect() {
		try {
			IDocument document = fConsole.getDocument();
			if (document != null) {
				int lastLine = document.getNumberOfLines() - 1;
				if (document.getLineDelimiter(lastLine) == null) {
					IRegion lineInformation = document.getLineInformation(lastLine);
					lineAppended(lineInformation);
				}
			}
		} catch (BadLocationException e) {
		}
	}

	/**
	 * Notification the console's streams have been closed
	 */
	public synchronized void consoleClosed() {
		int size = fListeners.size();
		for (int i = 0; i < size; i++) {
			IConsoleLineTracker tracker = fListeners.get(i);
			if (tracker instanceof IConsoleLineTrackerExtension) {
				((IConsoleLineTrackerExtension) tracker).consoleClosed();
			}
			tracker.dispose();
		}

		fConsole = null;
		fListeners = null;
	}

	/**
	 * Adds the given listener to the list of listeners notified when a line of
	 * text is appended to the console.
	 *
	 * @param listener the listener to add
	 */
	public void addConsoleListener(IConsoleLineTracker listener) {
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		try  {
			IDocument document = fConsole.getDocument();
			int lineOfOffset = document.getLineOfOffset(event.getOffset());
			String delimiter = document.getLineDelimiter(lineOfOffset);
			int strip = delimiter==null ? 0 : delimiter.length();
			Region region = new Region(event.getOffset(), event.getLength()-strip);
			lineAppended(region);
		} catch (BadLocationException e) {}
	}

	public void lineAppended(IRegion region) {
		int size = fListeners.size();
		for (int i=0; i<size; i++) {
			IConsoleLineTracker tracker = fListeners.get(i);
			tracker.lineAppended(region);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE)) {
			fConsole.removePropertyChangeListener(this);
			consoleClosed();
		}
	}

	@Override
	public String getPattern() {
		return ".*\\r(\\n?)|.*\\n"; //$NON-NLS-1$
	}

	@Override
	public int getCompilerFlags() {
		return 0;
	}

	@Override
	public String getLineQualifier() {
		return "\\n|\\r"; //$NON-NLS-1$
	}

}
