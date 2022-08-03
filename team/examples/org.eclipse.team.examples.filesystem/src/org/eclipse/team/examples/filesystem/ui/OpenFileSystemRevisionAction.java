/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.IProgressService;

public class OpenFileSystemRevisionAction extends BaseSelectionListenerAction {

	private IStructuredSelection selection;
	HistoryPage page;

	public OpenFileSystemRevisionAction(String text) {
		super(text);
	}

	@Override
	public void run() {
		IStructuredSelection structSel = selection;

		Object[] objArray = structSel.toArray();

		for (Object tempRevision : objArray) {
			final IFileRevision revision = (IFileRevision) tempRevision;
			if (revision == null || !revision.exists()) {
				MessageDialog.openError(page.getSite().getShell(), "Deleted Revision", "Can't open a deleted revision");
			} else {
				IRunnableWithProgress runnable = monitor -> {
					IStorage file;
					try {
						file = revision.getStorage(monitor);
						String id = getEditorID(file.getName(), file.getContents());

						if (file instanceof IFile) {
							//if this is the current workspace file, open it
							IDE.openEditor(page.getSite().getPage(), (IFile) file);
						} else {
							FileSystemRevisionEditorInput fileRevEditorInput = new FileSystemRevisionEditorInput(revision);
							if (!editorAlreadyOpenOnContents(fileRevEditorInput))
								page.getSite().getPage().openEditor(fileRevEditorInput, id);
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}

				};

				IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
				try {
					progressService.run(false, false, runnable);
				} catch (InvocationTargetException e) {
					// ignore
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	/* private */String getEditorID(String fileName, InputStream contents) {
		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		IContentType type = null;
		if (contents != null) {
			try {
				type = Platform.getContentTypeManager().findContentTypeFor(contents, fileName);
			} catch (IOException e) {
				// ignore
			}
		}
		if (type == null) {
			type = Platform.getContentTypeManager().findContentTypeFor(fileName);
		}
		IEditorDescriptor descriptor = registry.getDefaultEditor(fileName, type);
		String id;
		if (descriptor == null || descriptor.isOpenExternal()) {
			id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
		} else {
			id = descriptor.getId();
		}

		return id;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;
		return shouldShow();
	}

	public void setPage(HistoryPage page) {
		this.page = page;
	}

	private boolean shouldShow() {
		IStructuredSelection structSel = selection;
		Object[] objArray = structSel.toArray();

		if (objArray.length == 0)
			return false;

		for (Object obj : objArray) {
			IFileRevision revision = (IFileRevision) obj;
			//check to see if any of the selected revisions are deleted revisions
			if (revision != null && !revision.exists())
				return false;
		}

		return true;
	}

	boolean editorAlreadyOpenOnContents(FileSystemRevisionEditorInput input) {
		IEditorReference[] editorRefs = page.getSite().getPage().getEditorReferences();
		for (IEditorReference editorRef : editorRefs) {
			IEditorPart part = editorRef.getEditor(false);
			if (part != null && part.getEditorInput() instanceof FileSystemRevisionEditorInput) {
				IFileRevision inputRevision = input.getAdapter(IFileRevision.class);
				IFileRevision editorRevision = part.getEditorInput().getAdapter(IFileRevision.class);

				if (inputRevision.equals(editorRevision)) {
					//make the editor that already contains the revision current
					page.getSite().getPage().activate(part);
					return true;
				}
			}
		}
		return false;
	}

}
