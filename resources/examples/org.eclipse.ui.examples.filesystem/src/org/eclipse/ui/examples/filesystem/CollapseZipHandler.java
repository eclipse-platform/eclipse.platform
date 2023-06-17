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
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class CollapseZipHandler extends AbstractHandler {

	private void collapseZip(IFolder folder, Shell shell) {
		try {
			URI zipURI = new URI(folder.getLocationURI().getQuery());
			//check if the zip file is physically stored below the folder in the workspace
			IFileStore parentStore = EFS.getStore(folder.getParent().getLocationURI());
			URI childURI = parentStore.getChild(folder.getName()).toURI();
			if (URIUtil.equals(zipURI, childURI)) {
				//the zip file is in the workspace so just delete the link 
				// and refresh the parent to create the resource
				folder.delete(IResource.NONE, null);
				folder.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
			} else {
				//otherwise the zip file must be a linked resource
				IFile file = folder.getParent().getFile(IPath.fromOSString(folder.getName()));
				file.createLink(zipURI, IResource.REPLACE, null);
			}
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", "Error opening zip file");
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
		
		if (!(element instanceof IFolder)) {
			return null;
		}
		
		collapseZip((IFolder) element, shell);
		return null;
	}

}
