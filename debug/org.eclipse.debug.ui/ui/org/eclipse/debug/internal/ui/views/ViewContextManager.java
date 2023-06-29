/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - Fixed debug context service usage (Bug 258189)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Manages the view context services for each window.
 *
 * @since 3.2
 */
public class ViewContextManager implements IWindowListener {

	/**
	 * Map of services
	 */
	private Map<IWorkbenchWindow, ViewContextService> fWindowToService = new HashMap<>();

	// singleton manager
	private static ViewContextManager fgManager;

	/**
	 * Returns the singleton view context manager.
	 *
	 * @return view manager
	 */
	public static ViewContextManager getDefault() {
		if (fgManager == null) {
			fgManager = new ViewContextManager();
		}
		return fgManager;
	}


	private ViewContextManager() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();
		for (IWorkbenchWindow window : workbenchWindows) {
			windowOpened(window);
		}
		workbench.addWindowListener(this);
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		ViewContextService service = fWindowToService.get(window);
		if (service != null) {
			fWindowToService.remove(window);
			service.dispose();
		}
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		ViewContextService service = fWindowToService.get(window);
		if (service == null) {
			service = new ViewContextService(window);
			fWindowToService.put(window, service);
		}
	}

	/**
	 * Returns the service for the given window, or <code>null</code> if none.
	 *
	 * @param window
	 * @return view context service or <code>null</code>
	 */
	public ViewContextService getService(IWorkbenchWindow window) {
		return fWindowToService.get(window);
	}

}
