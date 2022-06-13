/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.nls;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

public class GotoResourceAction extends Action {
	IResource selectedResource;
	IWorkbenchPart part;

	private static class GotoResourceDialog extends ResourceListSelectionDialog {
		private IJavaModel fJavaModel;

		public GotoResourceDialog(Shell parentShell, IContainer container) {
			super(parentShell, container, IResource.FILE | IResource.FOLDER | IResource.PROJECT);
			fJavaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			setTitle("Select the corresponding properties file:"); //$NON-NLS-1$
		}

		@Override
		protected boolean select(IResource resource) {
			IProject project = resource.getProject();
			try {
				if (project.getNature(JavaCore.NATURE_ID) != null)
					return fJavaModel.contains(resource);
			} catch (CoreException e) {
				// do nothing. Consider resource;
			}
			return true;
		}
	}

	public IResource getResource() {
		return selectedResource;
	}

	public GotoResourceAction(IWorkbenchPart part) {
		this.part = part;
		setText("Select the corresponding properties file:"); //$NON-NLS-1$
	}

	@Override
	public void run() {
		GotoResourceDialog dialog = new GotoResourceDialog(part.getSite().getShell(), ResourcesPlugin.getWorkspace().getRoot());
		dialog.open();
		Object[] result = dialog.getResult();
		if (result == null || result.length == 0 || !(result[0] instanceof IResource))
			return;
		selectedResource = (IResource) result[0];
	}
}
