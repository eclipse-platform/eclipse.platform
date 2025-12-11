/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClipboardReplace extends BaseCompareAction {

	@Override
	protected void run(ISelection selection) {
		IFile[] files = Utilities.getFiles(selection);
		for (IFile file : files) {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorPart editor = page.getActiveEditor();
				if (editor instanceof FormEditor fromEditor) {
					editor = fromEditor.getActiveEditor();
				}
				IEditorInput input = editor.getEditorInput();
				if (input instanceof IFileEditorInput ed) {
					IFile file2 = ed.getFile();
					String fileName2 = file2.getName();
					if (!file.getName().equals(fileName2)) {
						ByteArrayInputStream source = new ByteArrayInputStream(
								getClipboard().toString().getBytes(StandardCharsets.UTF_8));
						file.setContents(source, IResource.FORCE, null);
						return;
					}
				}
				if (editor instanceof ITextEditor txtEditor) {
					ISelection selection2 = txtEditor.getSelectionProvider().getSelection();
					if (selection2 instanceof ITextSelection textSelection) {
						int offset = textSelection.getOffset();
						int len = textSelection.getLength();
						if (len > 0) {
							IDocument doc = ((ITextEditor) editor).getDocumentProvider()
									.getDocument(editor.getEditorInput());
							doc.replace(offset, len, getClipboard().toString());
							return;
						}
						ByteArrayInputStream source = new ByteArrayInputStream(
								getClipboard().toString().getBytes(StandardCharsets.UTF_8));
						file.setContents(source, IResource.FORCE, null);
					}
				}

			} catch (Exception e) {
				Shell parentShell = CompareUIPlugin.getShell();
				MessageDialog.openError(parentShell, "Replace Failed", e.getMessage()); //$NON-NLS-1$
			}
		}
	}
	@Override
	protected boolean isEnabled(ISelection selection) {
		return Utilities.getFiles(selection).length == 1 && getClipboard() != null;
	}

	/**
	 * Returns Clipboard Object or null if there is nothing in clipboard
	 *
	 * @returns Clipboard Object or null
	 */
	private Object getClipboard() {
		Clipboard clip = new Clipboard(Display.getDefault());
		try {
			return clip.getContents(TextTransfer.getInstance());
		} finally {
			clip.dispose();
		}
	}

}
