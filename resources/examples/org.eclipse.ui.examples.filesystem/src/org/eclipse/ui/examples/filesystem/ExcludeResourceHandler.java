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
 *     Patrick Ziegler - Migration from a JFace Action to a Command Handler,
 *                       in order to be used with the 'org.eclipse.ui.menus'
 *                       extension point.
 *******************************************************************************/
package org.eclipse.ui.examples.filesystem;

import java.net.URI;
import org.eclipse.core.commands.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExcludeResourceHandler extends AbstractHandler {

	private void exclude(IResource resource, Shell shell) {
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
			MessageDialog.openError(shell, "Error", "Error excluding resource");
			e.printStackTrace();
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		
		Object element = ((IStructuredSelection) selection).getFirstElement();
		
		if (!(element instanceof IResource)) {
			return null;
		}
		
		IResource resource = (IResource) element;
		if (resource.isLinked()) {
			return null;
		}
		
		exclude(resource, shell);
		return null;
	}

}
