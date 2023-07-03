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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.internal.ui.sourcelookup.BasicContainerContentProvider;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * The browser for creating project source containers.
 *
 * @since 3.0
 */
public class ProjectSourceContainerBrowser extends AbstractSourceContainerBrowser {

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		Object input = ResourcesPlugin.getWorkspace().getRoot();
		IStructuredContentProvider contentProvider=new BasicContainerContentProvider();
		ILabelProvider labelProvider = new WorkbenchLabelProvider();
		Dialog dialog = new ProjectSourceContainerDialog(shell,input, contentProvider, labelProvider,
				SourceLookupUIMessages.projectSelection_chooseLabel);
		if(dialog.open() == Window.OK){
			Object[] elements= ((ListSelectionDialog)dialog).getResult();
			ArrayList<ISourceContainer> res = new ArrayList<>();
			for (Object element : elements) {
				if (!(element instanceof IProject)) {
					continue;
				}
				res.add(new ProjectSourceContainer((IProject) element, ((ProjectSourceContainerDialog)dialog).isAddRequiredProjects()));
			}
			return res.toArray(new ISourceContainer[res.size()]);
		}
		return new ISourceContainer[0];
	}

}
