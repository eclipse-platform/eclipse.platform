/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
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

			for (Object obj : objArray) {
				IFileState state = (IFileState) obj;
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
		IWorkbench workbench = PlatformUI.getWorkbench();
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
