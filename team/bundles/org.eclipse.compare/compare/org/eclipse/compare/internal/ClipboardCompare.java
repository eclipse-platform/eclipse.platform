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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClipboardCompare extends BaseCompareAction implements IObjectActionDelegate {

	private final String clipboard = "Clipboard"; //$NON-NLS-1$
	private final String compareFailed = "Comparision Failed"; //$NON-NLS-1$

	private IWorkbenchPart activePart;

	@Override
	protected void run(ISelection selection) {
		IFile[] files = Utilities.getFiles(selection);
		Shell parentShell = CompareUIPlugin.getShell();
		for (IFile file : files) {
			try {
				processComparison(file, parentShell);
			} catch (Exception e) {
				MessageDialog.openError(parentShell, compareFailed, e.getMessage());
			}
		}
	}
	@Override
	protected boolean isEnabled(ISelection selection) {
		return Utilities.getFiles(selection).length == 1 && getClipboard() != null;
	}

	/**
	 * Process comparison with selection or entire editor contents with contents in
	 * clipboard
	 *
	 * @param file        Editor file
	 * @param parentShell The shell containing this window's controls
	 * @throws IOException, CoreException
	 */
	private void processComparison(IFile file, Shell parentShell) throws IOException, CoreException {
		String cb = getClipboard().toString();
		String fileName = file.getName();
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();
		if (activePart instanceof IViewPart) {
			String fileContents = new String(file.getContents().readAllBytes(), file.getCharset());
			showComparison(fileContents, fileName, cb, parentShell);
			return;
		}
		final String selectionContents;
		if (editor instanceof FormEditor fromEditor) {
			editor = fromEditor.getActiveEditor();
		}
		if (editor instanceof ITextEditor txtEditor) {
			ISelection selection = txtEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				selectionContents = textSelection.getText();
				if (selectionContents.isEmpty()) {
					String fileContents = new String(file.getContents().readAllBytes(), file.getCharset());
					showComparison(fileContents, fileName, cb, parentShell);
				} else {
					showComparison(selectionContents, fileName, cb, parentShell);
				}
				return;
			}
		}
		if (editor instanceof CompareEditor existingCompare) { // if selection is from compare editor itself
			ISelection selection = existingCompare.getSite().getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				String selectedText = textSelection.getText();
				String fileContents = new String(file.getContents().readAllBytes(), file.getCharset());
				showComparison(fileContents, fileName, selectedText, parentShell);
			}
		}
	}

	/**
	 * Shows comparison result
	 *
	 * @param source            Either selection from current editor or entire
	 *                          editor if no selection
	 * @param fileName          Editor file name
	 * @param clipboardContents Contents in clipboard
	 * @param parentShell       The shell containing this window's controls
	 */
	private void showComparison(String source, String fileName, String clipboardContents, Shell parentShell) {
		class ClipboardTypedElement implements ITypedElement, IEncodedStreamContentAccessor {
			private final String name;
			private final String content;

			public ClipboardTypedElement(String name, String content) {
				this.name = name;
				this.content = content;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Image getImage() {
				return null;
			}

			@Override
			public String getType() {
				return null;
			}

			@Override
			public String getCharset() throws CoreException {
				return "UTF-8"; //$NON-NLS-1$
			}

			@Override
			public InputStream getContents() throws CoreException {
				return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			}

		}
		if (source == null) {
			MessageDialog.openInformation(parentShell, compareFailed, "Failed to process selected file"); //$NON-NLS-1$
			return;
		}
		CompareConfiguration config = new CompareConfiguration();
		config.setLeftLabel(fileName);
		config.setRightLabel(clipboard);
		config.setLeftEditable(true);
		config.setRightEditable(true);
		CompareEditorInput compareInput = new CompareEditorInput(config) {
			@Override
			protected Object prepareInput(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				return new DiffNode(new ClipboardTypedElement(fileName, source),
						new ClipboardTypedElement(clipboard, clipboardContents));

			}
		};
		CompareUI.openCompareEditor(compareInput);
	}

	/**
	 * Returns Clipboard Object or null if there is nothing in clipboard
	 *
	 * @returns Clipboard Object or null
	 */
	private Object getClipboard() {
		Clipboard clip = new Clipboard(Display.getDefault());
		return clip.getContents(TextTransfer.getInstance());
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.activePart = targetPart;
	}

}
