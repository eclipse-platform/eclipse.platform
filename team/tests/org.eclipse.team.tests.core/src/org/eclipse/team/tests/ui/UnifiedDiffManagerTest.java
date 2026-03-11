/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.team.tests.ui;

import static java.util.Collections.synchronizedList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.unifieddiff.UnifiedDiff;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("restriction")
@ExtendWith(WorkspaceResetExtension.class)
public class UnifiedDiffManagerTest {

	// Annotation type constants from
	// org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager
	private static final String ADDITION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.addition";
	private static final String DELETION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.deletion";
	private static final String DETAILED_ADDITION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedAddition";
	private static final String DETAILED_DELETION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedDeletion";

	private static final String LEFT = """
			line one
			line two
			line three
			""";
	private static final String RIGHT = """
			line one
			line TWO modified
			line three
			""";

	private IFile file;
	private ITextEditor editor;
	private final List<IStatus> loggedErrors = synchronizedList(new ArrayList<>());
	private final ILogListener logListener = (status, plugin) -> {
		if (status.getSeverity() == IStatus.ERROR) {
			loggedErrors.add(status);
		}
	};

	@BeforeEach
	public void setUp() throws Exception {
		// Tests force a paint cycle, which requires a real Display. Fail loudly if
		// the test is launched in a headless environment instead of silently passing.
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display");

		IProject project = getWorkspace().getRoot().getProject("UnifiedDiffProject");
		createInWorkspace(project);
		file = project.getFile("Sample.txt");
		createInWorkspace(file);
		file.setContents(createInputStream(LEFT), true, true, null);

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		// Opens the default text editor (.txt has no presentation reconciler), which
		// exercises the empty-StyleRange fallback path in
		// UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining.draw.
		editor = (ITextEditor) IDE.openEditor(page, file);
		processEvents();

		Platform.addLogListener(logListener);
	}

	@AfterEach
	public void tearDown() {
		Platform.removeLogListener(logListener);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (editor != null) {
			page.closeEditor(editor, false);
		}
		assertThat(loggedErrors).as("errors logged during open + paint").isEmpty();
	}

	@Test
	public void testOverlayReadOnlyModeProducesAnnotationsAndPaints() {
		IStatus status = UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open();
		assertTrue(status.isOK(), "open() should return OK status: " + status);

		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		assertNotNull(viewer, "editor must adapt to ITextViewer");

		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		assertNotNull(model, "annotation model must not be null");

		// In OVERLAY_READ_ONLY_MODE the document is not modified; the diff is shown as
		// a deletion annotation on the original line plus detailed-deletion annotations
		// on the changed tokens.
		int deletionCount = countAnnotations(model, DELETION_ANNO_TYPE);
		int detailedDeletionCount = countAnnotations(model, DETAILED_DELETION_ANNO_TYPE);
		assertTrue(deletionCount >= 1,
				"expected at least one deletion annotation, got " + deletionCount);
		assertTrue(detailedDeletionCount >= 1,
				"expected at least one detailed-deletion annotation, got " + detailedDeletionCount);

		forcePaintCycle(viewer.getTextWidget());
	}

	@Test
	public void testRevertModeProducesAnnotationsAndPaints() {
		IStatus status = UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REVERT_MODE).open();
		assertTrue(status.isOK(), "open() should return OK status: " + status);

		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		assertNotNull(viewer, "editor must adapt to ITextViewer");

		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		assertNotNull(model, "annotation model must not be null");

		// REVERT_MODE uses ADDITION_ANNO_TYPE for the main annotation and
		// DETAILED_ADDITION_ANNO_TYPE for the fine-grained ones (see
		// UnifiedDiffAnnotation / DetailedDiffAnnotation constructors).
		int additionCount = countAnnotations(model, ADDITION_ANNO_TYPE);
		int detailedAdditionCount = countAnnotations(model, DETAILED_ADDITION_ANNO_TYPE);
		assertTrue(additionCount >= 1,
				"expected at least one addition annotation, got " + additionCount);
		assertTrue(detailedAdditionCount >= 1,
				"expected at least one detailed-addition annotation, got " + detailedAdditionCount);

		forcePaintCycle(viewer.getTextWidget());
	}

	private static int countAnnotations(IAnnotationModel model, String type) {
		int count = 0;
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation anno = it.next();
			if (type.equals(anno.getType())) {
				count++;
			}
		}
		return count;
	}

	private static void forcePaintCycle(StyledText tw) {
		if (tw == null || tw.isDisposed()) {
			return;
		}
		tw.redraw();
		tw.update();
		processEvents();
	}

	private static void processEvents() {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		while (display.readAndDispatch()) {
			// drain the event queue
		}
	}
}
