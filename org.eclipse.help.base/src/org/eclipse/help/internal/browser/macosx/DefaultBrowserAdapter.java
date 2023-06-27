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
package org.eclipse.help.internal.browser.macosx;

import java.io.*;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.browser.*;

public class DefaultBrowserAdapter implements IBrowser {

	private static DefaultBrowserAdapter fgInstance;

	static DefaultBrowserAdapter getInstance() {
		if (fgInstance == null)
			fgInstance = new DefaultBrowserAdapter();
		return fgInstance;
	}

	@Override
	public void close() {
	}

	@Override
	public void displayURL(String url) {
		/*
		 * Code from Marc-Antoine Parent
		 */
		try {
			Runtime.getRuntime().exec(new String[] { "/usr/bin/osascript", //$NON-NLS-1$
					"-e", //$NON-NLS-1$
					"open location \"" + url + "\"" }); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException ioe) {
			ILog.of(getClass()).error("Launching \"osascript\" has failed.", ioe); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isCloseSupported() {
		return false;
	}

	@Override
	public boolean isSetLocationSupported() {
		return false;
	}

	@Override
	public boolean isSetSizeSupported() {
		return false;
	}

	@Override
	public void setLocation(int x, int y) {
	}

	@Override
	public void setSize(int width, int height) {
	}
}
