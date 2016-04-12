/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

public class OpenLocalFileAction extends BaseSelectionListenerAction {

	protected OpenLocalFileAction(String text) {
		super(text);
	}

	@Override
	public void run() {
		try {
			IStructuredSelection structSel = getStructuredSelection();

			Object[] objArray = structSel.toArray();

			for (int i = 0; i < objArray.length; i++) {
				IFileState state = (IFileState) objArray[i];
				if (!state.exists()) {
					MessageDialog.openError(TeamUIPlugin.getActivePage().getActivePart().getSite().getShell(), TeamUIMessages.OpenRevisionAction_DeletedRevisionTitle, TeamUIMessages.OpenRevisionAction_DeletedRevisionMessage);
				} else {
					String id = getEditorID(state.getName(), state.getContents());
					IWorkbenchPage page = TeamUIPlugin.getActivePage();
					if (page != null) {
						page.openEditor(new FileRevisionEditorInput(state), id);
					}
				}

			}

		} catch (Exception e) {

		}
	}

	/* private */String getEditorID(String fileName, InputStream contents) {
		IWorkbench workbench = TeamUIPlugin.getPlugin().getWorkbench();
		IEditorRegistry registry = workbench.getEditorRegistry();

		IContentType type = null;
		if (contents != null) {
			try {
				type = Platform.getContentTypeManager().findContentTypeFor(contents, fileName);
			} catch (IOException e) {

			}
		}
		if (type == null) {
			type = Platform.getContentTypeManager().findContentTypeFor(fileName);
		}
		IEditorDescriptor descriptor = registry.getDefaultEditor(fileName, type);
		String id;
		if (descriptor == null) {
			id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
		} else {
			id = descriptor.getId();
		}

		return id;
	}

}
