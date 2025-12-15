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
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClipboardCompare extends BaseCompareAction implements IObjectActionDelegate {

	private final String clipboard = "Clipboard"; //$NON-NLS-1$
	private final String compareFailed = "Comparision Failed"; //$NON-NLS-1$

	private IFile currentResouce;

	private IWorkbenchPart activePart;

	private int offSet;
	private int len;

	private boolean partialSelection;

	@Override
	protected void run(ISelection selection) {
		offSet = -1;
		len = -1;
		partialSelection = false;
		IFile[] files = Utilities.getFiles(selection);
		Shell parentShell = CompareUIPlugin.getShell();
		for (IFile file : files) {
			currentResouce = file;
			try {
				processComparison(parentShell);
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
	 * @param parentShell The shell containing this window's controls
	 * @throws IOException, CoreException
	 */
	private void processComparison(Shell parentShell) throws IOException, CoreException {
		String cb = getClipboard().toString();
		String fileName = currentResouce.getName();
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();
		if (activePart instanceof IViewPart) {
			String fileContents = new String(currentResouce.getContents().readAllBytes(), currentResouce.getCharset());
			showComparison(fileContents, fileName, cb, parentShell);
			return;
		}
		final String selectionContents;
		if (editor instanceof MultiPageEditorPart mpe) {
			Object page2 = mpe.getSelectedPage();
			if (page2 instanceof IEditorPart e) {
				editor = e;
			}
		}
		if (editor instanceof ITextEditor txtEditor) {
			ISelection selection = txtEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				selectionContents = textSelection.getText();
				if (selectionContents.isEmpty()) {
					String fileContents = new String(currentResouce.getContents().readAllBytes(),
							currentResouce.getCharset());
					showComparison(fileContents, fileName, cb, parentShell);
				} else {
					offSet = textSelection.getOffset();
					len = textSelection.getLength();
					partialSelection = true;
					showComparison(selectionContents, fileName, cb, parentShell);
				}
				return;
			}
		}
		if (editor instanceof CompareEditor existingCompare) { // if selection is from compare editor itself
			ISelection selection = existingCompare.getSite().getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				String selectedText = textSelection.getText();
				String fileContents = new String(currentResouce.getContents().readAllBytes(),
						currentResouce.getCharset());
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
		class EditableFileNode extends ResourceNode implements IEditableContent {

			private final int selectionOffset;
			private final int selectionLength;

			public EditableFileNode(IFile file, int selectionOffset, int selectionLength) {
				super(file);
				this.selectionOffset = selectionOffset;
				this.selectionLength = selectionLength;
			}

			@Override
			public InputStream getContents() throws CoreException {
				IFile file = (IFile) getResource();
				if (!partialSelection) {
					return new ByteArrayInputStream(file.readAllBytes());
				}
				try {
					String content = new String(file.getContents().readAllBytes(), file.getCharset());
					int start = Math.max(0, Math.min(selectionOffset, content.length()));
					int end = Math.max(start, Math.min(selectionOffset + selectionLength, content.length()));
					String selectedPart = content.substring(start, end);
					return new ByteArrayInputStream(selectedPart.getBytes(file.getCharset()));
				} catch (IOException e) {
					MessageDialog.openError(CompareUIPlugin.getShell(), compareFailed, e.getMessage());
				}
				return new ByteArrayInputStream(file.readAllBytes());
			}

			@Override
			public void setContent(byte[] newContent) {
				try {
					if (selectionLength <= 1) {
						((IFile) getResource()).setContents(new ByteArrayInputStream(newContent),
								IResource.FORCE | IResource.KEEP_HISTORY, null);
					} else {
						IFile file = (IFile) getResource();
						String charset = file.getCharset();
						String original = new String(file.getContents().readAllBytes(), charset);
						String updatedSelection = new String(newContent, charset);
						int offset = Math.max(0, Math.min(selectionOffset, original.length()));
						int end = Math.max(offset, Math.min(offset + selectionLength, original.length()));
						String newFileContent = original.substring(0, offset) + updatedSelection
								+ original.substring(end);
						ByteArrayInputStream updatedStream = new ByteArrayInputStream(newFileContent.getBytes(charset));
						file.setContents(updatedStream, IResource.FORCE | IResource.KEEP_HISTORY, null);
					}

				} catch (Exception e) {
					MessageDialog.openError(CompareUIPlugin.getShell(), compareFailed, e.getMessage());
				}
			}

			@Override
			public boolean isEditable() {
				return true;
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
				ITypedElement left;
				if (offSet >= 0 && len >= 0) {
					left = new EditableFileNode(currentResouce, offSet, len);
				} else {
					left = new EditableFileNode(currentResouce, 0, Integer.MAX_VALUE);
				}
				ITypedElement right = new ClipboardTypedElement(clipboard, clipboardContents);
				return new DiffNode(left, right);

			}
		};
		compareInput.setTitle(currentResouce.getName());
		CompareUI.openCompareEditor(compareInput);

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

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.activePart = targetPart;
	}

}