/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.console;
 
import org.eclipse.ant.internal.ui.launchConfigurations.TaskLinkManager;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jface.text.IRegion;

/**
 * Processes task hyperlinks as lines are appended to the console
 */
public class TaskLineTracker implements IConsoleLineTrackerExtension {
	
	private IConsole fConsole;

	/**
	 * Constructor for TaskLineTracker.
	 */
	public TaskLineTracker() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public void init(IConsole console) {
		fConsole = console;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		TaskLinkManager.processNewLine(fConsole, line);
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
		fConsole = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.console.IConsoleLineTrackerExtension#consoleClosed()
	 */
	public void consoleClosed() {
		TaskLinkManager.dispose(fConsole.getProcess());
		AbstractJavacPatternMatcher.consoleClosed(fConsole.getProcess());
	}
}
