/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor;

import org.eclipse.ant.internal.ui.editor.utils.ProjectHelper;

public class DecayCodeCompletionDataStructuresThread extends Thread {

	private static final int fgDelay = 6000 * 5; // 5 minutes

	private static DecayCodeCompletionDataStructuresThread fgInstance;

	public static DecayCodeCompletionDataStructuresThread getDefault() {
		if (fgInstance == null) {
			fgInstance = new DecayCodeCompletionDataStructuresThread();
		}
		return fgInstance;
	}

	/**
	 * Creates a new background thread. The thread runs with minimal priority.
	 */
	private DecayCodeCompletionDataStructuresThread() {
		super("Decay Ant Data Structures"); //$NON-NLS-1$
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		fgInstance = this;
		setContextClassLoader(null); // don't hold on to any class loaders
	}

	/**
	 * The background activity that is triggered when the last <code>AntModel</code> is disposed. Waits for the required delay and then nulls out the
	 * memory expensive Ant code completion data structures and reset the ProjectHelper. If an <code>AntModel</code> is created during the wait, the
	 * thread is interrupted and no nulling out occurs.
	 */
	@Override
	public void run() {
		synchronized (this) {
			try {
				wait(fgDelay);
				AntEditorCompletionProcessor.resetCodeCompletionDataStructures();
				ProjectHelper.reset();
			}
			catch (InterruptedException x) {
				// do nothing
			}
		}
	}

	/**
	 * Cancels the background thread.
	 */
	public static void cancel() {
		if (fgInstance != null) {
			synchronized (fgInstance) {
				fgInstance.interrupt();
				fgInstance = null;
			}
		}
	}
}
