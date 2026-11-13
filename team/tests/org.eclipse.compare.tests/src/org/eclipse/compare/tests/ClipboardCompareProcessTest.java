/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.ClipboardCompareProcess;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ClipboardCompareProcessTest {

	private static final long TIMEOUT_MILLIS = 30_000;

	private IProject project;

	@BeforeEach
	public void setUp() throws CoreException {
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display"); //$NON-NLS-1$
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("ClipboardCompareProcessTest"); //$NON-NLS-1$
		if (!project.exists()) {
			project.create(null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}
	}
	
	@AfterEach
	public void tearDown() {
		IWorkbenchPage page = activePage();
		if (page != null) {
			page.closeAllEditors(false);
		}
		processQueuedEvents();
	}

	@Test
	public void testProcessComparisonComparesWholeFileWhenNoSelection() throws Exception {
		IFile file = createFile("whole.txt", "line1\nline2\n"); //$NON-NLS-1$ //$NON-NLS-2$
		setClipboardText("clipboard-content"); //$NON-NLS-1$

		new ClipboardCompareProcess().processComparison(shell(), file);
		pumpUntil(() -> activePage().getActiveEditor() instanceof CompareEditor, "compare editor did not open"); //$NON-NLS-1$

		DiffNode result = compareResult();
		assertEquals("line1\nline2\n", readContents(result.getLeft())); //$NON-NLS-1$
		assertEquals("clipboard-content", readContents(result.getRight())); //$NON-NLS-1$
	}

	@Test
	public void testEditingLeftSideWritesWholeFileBack() throws Exception {
		IFile file = createFile("editWhole.txt", "old content\n"); //$NON-NLS-1$ //$NON-NLS-2$
		setClipboardText("clipboard"); //$NON-NLS-1$

		new ClipboardCompareProcess().processComparison(shell(), file);
		pumpUntil(() -> activePage().getActiveEditor() instanceof CompareEditor, "compare editor did not open"); //$NON-NLS-1$

		ITypedElement left = compareResult().getLeft();
		IEditableContent editable = assertInstanceOf(IEditableContent.class, left,
				"the file side of the clipboard compare must be editable"); //$NON-NLS-1$
		assertTrue(editable.isEditable());

		editable.setContent("new content\n".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$

		assertEquals("new content\n", fileContents(file)); //$NON-NLS-1$
	}

	@Test
	public void testProcessComparisonComparesSelectionOnly() throws Exception {
		IFile file = createFile("selection.txt", "alpha\nbravo\ncharlie\n"); //$NON-NLS-1$ //$NON-NLS-2$
		setClipboardText("clip"); //$NON-NLS-1$
		selectText(file, "alpha\n".length(), "bravo".length()); //$NON-NLS-1$ //$NON-NLS-2$

		new ClipboardCompareProcess().processComparison(shell(), file);
		pumpUntil(() -> activePage().getActiveEditor() instanceof CompareEditor, "compare editor did not open"); //$NON-NLS-1$

		DiffNode result = compareResult();
		assertEquals("bravo", readContents(result.getLeft()), //$NON-NLS-1$
				"only the selected text must be shown on the file side"); //$NON-NLS-1$
		assertEquals("clip", readContents(result.getRight())); //$NON-NLS-1$
	}

	@Test
	public void testEditingLeftSideWithSelectionWritesBackOnlySelectedRange() throws Exception {
		IFile file = createFile("editSelection.txt", "alpha\nbravo\ncharlie\n"); //$NON-NLS-1$ //$NON-NLS-2$
		setClipboardText("clip"); //$NON-NLS-1$
		selectText(file, "alpha\n".length(), "bravo".length()); //$NON-NLS-1$ //$NON-NLS-2$

		new ClipboardCompareProcess().processComparison(shell(), file);
		pumpUntil(() -> activePage().getActiveEditor() instanceof CompareEditor, "compare editor did not open"); //$NON-NLS-1$

		ITypedElement left = compareResult().getLeft();
		IEditableContent editable = assertInstanceOf(IEditableContent.class, left);

		editable.setContent("BRAVO".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$

		assertEquals("alpha\nBRAVO\ncharlie\n", fileContents(file), //$NON-NLS-1$
				"only the selected range must be replaced, the rest of the file must be untouched"); //$NON-NLS-1$
	}

	private void selectText(IFile file, int offset, int length) throws Exception {
		IEditorPart editor = IDE.openEditor(activePage(), file);
		ITextEditor textEditor = assertInstanceOf(ITextEditor.class, editor);
		textEditor.getSelectionProvider().setSelection(new TextSelection(offset, length));
	}

	private IFile createFile(String name, String content) throws CoreException {
		IFile file = project.getFile(name);
		ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		if (file.exists()) {
			file.setContents(source, true, false, null);
		} else {
			file.create(source, true, null);
		}
		return file;
	}

	private static void setClipboardText(String text) {
		Clipboard clip = new Clipboard(Display.getDefault());
		try {
			clip.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clip.dispose();
		}
	}

	private static DiffNode compareResult() {
		IEditorPart editorPart = activePage().getActiveEditor();
		CompareEditorInput input = assertInstanceOf(CompareEditorInput.class, editorPart.getEditorInput());
		return assertInstanceOf(DiffNode.class, input.getCompareResult());
	}

	private static String readContents(ITypedElement element) throws Exception {
		IStreamContentAccessor accessor = assertInstanceOf(IStreamContentAccessor.class, element);
		try (InputStream in = accessor.getContents()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String fileContents(IFile file) throws Exception {
		try (InputStream in = file.getContents()) {
			return new String(in.readAllBytes(), file.getCharset());
		}
	}

	private static void pumpUntil(BooleanSupplier condition, String failMessage) {
		Display display = Display.getCurrent();
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		Runnable[] wake = new Runnable[1];
		wake[0] = () -> display.timerExec(50, wake[0]);
		display.timerExec(50, wake[0]);
		try {
			while (!condition.getAsBoolean()) {
				if (System.currentTimeMillis() > deadline) {
					fail(failMessage + " within " + TIMEOUT_MILLIS + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} finally {
			display.timerExec(-1, wake[0]);
		}
	}

	private static void processQueuedEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// drain the event queue
		}
	}

	private static IWorkbenchPage activePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static Shell shell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}
}