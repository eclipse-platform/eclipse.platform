/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Starts a source lookup service in each workbench window.
 *
 * @since 3.2
 */
public class SourceLookupManager implements IWindowListener {

	private static SourceLookupManager fgDefault;

	/**
	 * Services per window
	 */
	private Map<IWorkbenchWindow, SourceLookupService> fServices = new HashMap<>();

	private SourceLookupManager() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();
		for (IWorkbenchWindow window : workbenchWindows) {
			windowOpened(window);
		}
		workbench.addWindowListener(this);
	}

	/**
	 * Returns the default source lookup manager.
	 *
	 * @return
	 */
	public static SourceLookupManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new SourceLookupManager();
		}
		return fgDefault;
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		SourceLookupService service = fServices.get(window);
		if (service != null) {
			fServices.remove(window);
			service.dispose();
		}
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		SourceLookupService service = fServices.get(window);
		if (service == null) {
			service = new SourceLookupService(window);
			fServices.put(window, service);
		}
	}

	public void displaySource(Object context, IWorkbenchPage page, boolean forceSourceLookup) {
		IWorkbenchWindow window = page.getWorkbenchWindow();
		SourceLookupService service = fServices.get(window);
		if (service != null) {
			service.displaySource(context, page, forceSourceLookup);
		}
	}
}
