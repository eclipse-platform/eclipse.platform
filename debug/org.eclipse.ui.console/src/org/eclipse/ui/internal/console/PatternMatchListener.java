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
package org.eclipse.ui.internal.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class PatternMatchListener implements IPatternMatchListener {

	private PatternMatchListenerExtension fExtension;
	private IPatternMatchListenerDelegate fDelegate;

	public PatternMatchListener(PatternMatchListenerExtension extension) throws CoreException {
		fExtension = extension;
		fDelegate = fExtension.createDelegate();
	}

	@Override
	public String getPattern() {
		return fExtension.getPattern();
	}

	@Override
	public int getCompilerFlags() {
		return fExtension.getCompilerFlags();
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		fDelegate.matchFound(event);
	}

	@Override
	public void connect(TextConsole console) {
		fDelegate.connect(console);
	}

	@Override
	public void disconnect() {
		fDelegate.disconnect();
	}

	@Override
	public String getLineQualifier() {
		return fExtension.getQuickPattern();
	}

}
