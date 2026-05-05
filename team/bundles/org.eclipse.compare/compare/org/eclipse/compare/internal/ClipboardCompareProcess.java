/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClipboardCompareProcess implements IObjectActionDelegate {

	private final String clipboard = "Clipboard"; //$NON-NLS-1$
	private IFile currentResource;
	private IWorkbenchPart activePart;
	private IReusableEditor compareEditor;
	IResourceChangeListener listener;
	private String partialSelection;

	public void processComparison(Shell parentShell, IFile file) throws IOException, CoreException {
		currentResource = file;
		String cb = getClipboard().toString();
		String fileName = currentResource.getName();

		IWorkbenchWindow window = CompareUIPlugin.getActiveWorkbenchWindow();
		if (window == null)
			return;

		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return;

		IEditorPart editor = page.getActiveEditor();
		if (activePart instanceof IViewPart) {
			showComparison(fileName, cb, parentShell, false, 0, 0);
			return;
		}

		if (editor instanceof MultiPageEditorPart mpe) {
			Object selectedPage = mpe.getSelectedPage();
			if (selectedPage instanceof IEditorPart e) {
				editor = e;
			}
		}

		if (editor instanceof ITextEditor txtEditor) {
			ISelection selection = txtEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				String selected = textSelection.getText();

				if (selected.isEmpty()) {
					showComparison(fileName, cb, parentShell, false, 0, 0);
				} else {
					showComparison(fileName, cb, parentShell, true, textSelection.getOffset(),
							textSelection.getLength());
				}
				return;
			}
		}
		showComparison(fileName, cb, parentShell, false, 0, 0);
	}

	private void showComparison(String fileName, String clipboardContents, Shell parentShell,
			boolean isPartialSelection, int offset, int length) {
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
			public String getCharset() {
				return "UTF-8"; //$NON-NLS-1$
			}

			@Override
			public InputStream getContents() {
				return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			}
		}
		class EditableFileNode extends ResourceNode implements IEditableContent {

			public EditableFileNode(IFile file) {
				super(file);
			}

			@Override
			public InputStream getContents() throws CoreException {
				IFile file = (IFile) getResource();
				try {
					String content = new String(file.getContents().readAllBytes(), file.getCharset());
					if (!isPartialSelection) {
						return new ByteArrayInputStream(content.getBytes(file.getCharset()));
					}
					if (partialSelection != null) {
						return new ByteArrayInputStream(partialSelection.getBytes(file.getCharset()));
					}
					int start = Math.max(0, Math.min(offset, content.length()));
					int end = Math.max(start, Math.min(offset + length, content.length()));

					String selectedPart = content.substring(start, end);

					return new ByteArrayInputStream(selectedPart.getBytes(file.getCharset()));
				} catch (IOException e) {
					ILog.of(getClass()).error(e.getMessage(), e);
				}

				return new ByteArrayInputStream(file.readAllBytes());
			}

			@Override
			public void setContent(byte[] newContent) {
				try {
					IFile file = (IFile) getResource();
					String charset = file.getCharset();

					if (!isPartialSelection) {
						file.setContents(new ByteArrayInputStream(newContent), IResource.FORCE | IResource.KEEP_HISTORY,
								null);
					} else {
						String original = new String(file.getContents().readAllBytes(), charset);

						String updated = new String(newContent, charset);
						partialSelection = updated;
						int start = Math.max(0, Math.min(offset, original.length()));
						int end = Math.max(start, Math.min(start + length, original.length()));

						String result = original.substring(0, start) + updated + original.substring(end);
						file.setContents(new ByteArrayInputStream(result.getBytes(charset)),
								IResource.FORCE | IResource.KEEP_HISTORY, null);
					}

				} catch (Exception e) {
					ILog.of(getClass()).error(e.getMessage(), e);
				}
			}

			@Override
			public boolean isEditable() {
				return true;
			}
		}

		CompareConfiguration config = new CompareConfiguration();
		config.setLeftLabel(fileName);
		config.setRightLabel(clipboard);
		config.setLeftEditable(true);
		config.setRightEditable(true);

		final CompareEditorInput[] inputHolder = new CompareEditorInput[1];
		CompareEditorInput input = new CompareEditorInput(config) {
			@Override
			protected Object prepareInput(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {

				ITypedElement left = new EditableFileNode(currentResource);
				ITypedElement right = new ClipboardTypedElement(clipboard, clipboardContents);
				return new DiffNode(left, right);
			}
		};
		input.setTitle(NLS.bind(CompareMessages.CompareWithClipboardTitle, fileName));
		inputHolder[0] = input;

		CompareUI.openCompareEditor(input);
		listener = event -> {
			try {
				if (event.getDelta() == null) {
					return;
				}
				event.getDelta().accept(delta -> {
					if (delta.getResource() instanceof IFile file) {
						if (!file.equals(currentResource)) {
							return true;
						}

						if ((delta.getFlags() & IResourceDelta.CONTENT) == 0) {
							return true;
						}
						Display.getDefault().asyncExec(() -> {
							if (compareEditor != null) {

								CompareUI.reuseCompareEditor(inputHolder[0], compareEditor);
							}
						});
					}
					return true;
				});

			} catch (Exception e) {
				ILog.of(getClass()).error(e.getMessage(), e);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
		IWorkbenchPage page = CompareUIPlugin.getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();
		if (editor instanceof IReusableEditor reusable) {
			compareEditor = reusable;

		}
		page.addPartListener(new IPartListener2() {

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				IWorkbenchPart part = partRef.getPart(false);
				if (part == compareEditor) {
					ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
					page.removePartListener(this);
				}
			}

			@Override
			public void partOpened(IWorkbenchPartReference ref) {
			}

			@Override
			public void partActivated(IWorkbenchPartReference ref) {
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference ref) {
			}

			@Override
			public void partHidden(IWorkbenchPartReference ref) {
			}

			@Override
			public void partVisible(IWorkbenchPartReference ref) {
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference ref) {
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference ref) {
			}
		});
	}

	public static Object getClipboard() {
		Clipboard clip = new Clipboard(Display.getDefault());
		try {
			return clip.getContents(TextTransfer.getInstance());
		} finally {
			clip.dispose();
		}
	}

	@Override
	public void run(IAction action) {
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.activePart = targetPart;
	}
}