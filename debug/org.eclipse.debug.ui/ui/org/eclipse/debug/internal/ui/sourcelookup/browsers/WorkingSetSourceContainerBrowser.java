/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import java.util.ArrayList;

import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.debug.ui.sourcelookup.WorkingSetSourceContainer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

/**
 * The browser for creating working set source containers.
 *
 * @since 3.0
 */
public class WorkingSetSourceContainerBrowser extends AbstractSourceContainerBrowser {

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		ArrayList<ISourceContainer> containers = new ArrayList<>();
		IWorkingSetSelectionDialog dialog = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(shell, true);

		if (dialog.open() == Window.OK) {
			IWorkingSet[] selections = dialog.getSelection();
			if (selections != null) {
				for (IWorkingSet selection : selections) {
					containers.add(new WorkingSetSourceContainer(selection));
				}
			}
		}

		return containers.toArray(new ISourceContainer[containers.size()]);
	}

}
