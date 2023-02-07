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
package org.eclipse.help.ui.internal.browser;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.ErrorUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

/**
 * Implmentation of IBrowser interface, using org.eclipse.swt.Program
 */
public class SystemBrowserAdapter implements IBrowser {
	String[] cmdarray;

	/**
	 * Adapter constructor.
	 */
	public SystemBrowserAdapter() {
	}

	@Override
	public void close() {
	}

	@Override
	public void displayURL(String url) {
		//		if (Constants.WS_WIN32.equalsIgnoreCase(Platform.getOS())) {
		if (!Program.launch(url)) {
			Platform.getLog(getClass())
					.error(
							"Browser adapter for System Browser failed.  The system has no program registered for file " //$NON-NLS-1$
									+ url
									+ ".  Change the file association or choose a different help web browser in the preferences.", //$NON-NLS-1$
							null);
			ErrorUtil.displayErrorDialog(NLS.bind(Messages.SystemBrowser_noProgramForURL, url));
		}
		//		} else {
		//			Program b = Program.findProgram("html");
		//			if (b == null || !b.execute(url)) {
		//				ErrorUtil.displayErrorDialog(
		//					HelpUIResources.getString(
		//						"SystemBrowser.noProgramForHTML",
		//						url));
		//			}
		//		}
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
