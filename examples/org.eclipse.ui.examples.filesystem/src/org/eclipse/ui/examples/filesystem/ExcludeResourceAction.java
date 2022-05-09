/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.ui.examples.filesystem;

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

public class ExcludeResourceAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public ExcludeResourceAction() {
		super();
	}

	private void exclude(IResource resource) {
		try {
			URI nullURI = new URI(EFS.SCHEME_NULL, null, "/", null, null);
			if (resource.getType() == IResource.FILE) {
				IFile link = (IFile) resource;
				link.createLink(nullURI, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
			} else {
				IFolder link = (IFolder) resource;
				link.createLink(nullURI, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Error excluding resource");
			e.printStackTrace();
		}
	}

	private Shell getShell() {
		return targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection))
			return;
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (!(element instanceof IResource))
			return;
		IResource resource = (IResource) element;
		if (resource.isLinked())
			return;
		exclude(resource);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

}
