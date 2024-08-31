/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) - [352077] error dialogs when just probing browser
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.help.ui.internal.browser.embedded;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.browser.IBrowserFactory;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.ui.internal.HelpUIEventLoop;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class EmbeddedBrowserFactory implements IBrowserFactory {
	private boolean tested = false;

	private boolean available = false;
	private String browserType;

	/**
	 * Constructor.
	 */
	public EmbeddedBrowserFactory() {
		super();
	}

	@Override
	public boolean isAvailable() {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_STANDALONE) {
			try {
				if (HelpUIEventLoop.isRunning()) {
					Display.getDefault().syncExec(this::test);
				}
			} catch (Exception e) {
				// just in case
			}
		} else {
			test();
		}
		tested = true;
		return available;
	}

	/**
	 * Must run on UI thread
	 */
	private boolean test() {
		if (!Constants.OS_WIN32.equalsIgnoreCase(Platform.getOS())
				&& !Constants.OS_FREEBSD.equalsIgnoreCase(Platform.getOS())
				&& !Constants.OS_LINUX.equalsIgnoreCase(Platform.getOS())) {
			return false;
		}
		if (!tested) {
			tested = true;
			Shell sh = new Shell();
			try {
				Browser browser = new Browser(sh, SWT.NONE);
				browserType = browser.getBrowserType();
				available = true;
			} catch (SWTError se) {
				if (se.code == SWT.ERROR_NO_HANDLES) {
					// Browser not implemented
					available = false;
				} else {
					Status errorStatus = new Status(IStatus.WARNING, HelpUIPlugin.PLUGIN_ID, IStatus.OK,
							"An error occurred during creation of embedded help browser.", new Exception(se)); //$NON-NLS-1$
					ILog.of(getClass()).log(errorStatus);
				}
			} catch (Exception e) {
				// Browser not implemented
			}
			if (sh != null && !sh.isDisposed())
				sh.dispose();
		}
		return available;
	}

	@Override
	public IBrowser createBrowser() {
		return new EmbeddedBrowserAdapter(browserType);
	}
}
