/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ShowCVSPerspectiveAction extends Action {
	public void run() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
		if (activeWindow == null) {
			return;
		}
		IWorkbenchPage activePage = activeWindow.getActivePage();
		if (activePage == null) {
			return;
		}
		IPerspectiveDescriptor cvsPerspective = workbench.getPerspectiveRegistry().findPerspectiveWithId("org.eclipse.team.cvs.ui.cvsPerspective"); //$NON-NLS-1$
		if(cvsPerspective!=null) {
			activePage.setPerspective(cvsPerspective);
		}
	}
}
