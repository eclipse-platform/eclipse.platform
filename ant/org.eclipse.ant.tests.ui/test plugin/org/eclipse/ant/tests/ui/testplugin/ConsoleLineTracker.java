package org.eclipse.ant.tests.ui.testplugin;

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Simple console line tracker extension point that collects the lines
 * appended to the console. 
 */
public class ConsoleLineTracker implements IConsoleLineTrackerExtension {
	
	private static IConsole console;
	private static List lines= new ArrayList(); 
	
	private static boolean consoleClosed= false;

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
		console = null;
		lines= new ArrayList();
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public synchronized void init(IConsole c) {
		ConsoleLineTracker.console= c;
		lines= new ArrayList();
		consoleClosed= false;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		lines.add(line);
	}
	
	public static int getNumberOfMessages() {
		return lines.size();
	}
	
	public static String getMessage(int index) {
		if (index < lines.size()){
			IRegion lineRegion= (IRegion)lines.get(index);
			try {
				return console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
			} catch (BadLocationException e) {
				return null;
			}
		}
		return null;
	}
	
	public static List getAllMessages() {
		List all= new ArrayList(lines.size());
		for (int i = 0; i < lines.size(); i++) {
			IRegion lineRegion= (IRegion)lines.get(i);
			try {
				all.add(console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength()));
			} catch (BadLocationException e) {
				continue;
			}
		}
		return all;
	}
	
	public static IDocument getDocument() {
		return console.getDocument();
	}
	
	public static void waitForConsole() {
		if (consoleClosed) {
			return;
		}
		try {
			synchronized (lines) {
				lines.wait(20000);
			}
		} catch (InterruptedException ie) {
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.console.IConsoleLineTrackerExtension#consoleClosed()
	 */
	public void consoleClosed() {
		consoleClosed= true;
		
		synchronized (lines) {
			lines.notifyAll();
		}
	}
}