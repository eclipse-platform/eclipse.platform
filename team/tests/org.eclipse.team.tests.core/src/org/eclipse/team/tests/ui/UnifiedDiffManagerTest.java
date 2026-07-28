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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.unifieddiff.UnifiedDiff;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
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

	// ------------------------------------------------------------ REPLACE_MODE

	// REPLACE_MODE rewrites the editor document diff by diff and shifts every
	// following diff by the length delta of the applied ones. The resulting
	// document must be character-identical to the compared source, otherwise the
	// user silently ends up with a mixture of both versions.

	@Test
	public void testReplaceModeAppliesAnInsertionAtTheEndOfTheDocument() {
		assertReplaceModeYields("""
				line one
				line two
				""", """
				line one
				line two
				line three
				""");
	}

	@Test
	public void testReplaceModeAppliesADeletion() {
		assertReplaceModeYields("""
				line one
				line two
				line three
				""", """
				line one
				line three
				""");
	}

	/**
	 * Several diffs of differing lengths in one document: each applied diff shifts
	 * the offsets of all following ones, so a wrong delta only shows up from the
	 * second diff onwards.
	 */
	@Test
	public void testReplaceModeAppliesSeveralDiffsOfDifferentLength() {
		String left = """
				one
				two
				three
				four
				five
				six
				""";
		String right = """
				one
				a much longer second line
				three
				4
				five
				six and a bit more
				""";
		assertReplaceModeYields(left, right);
		assertTrue(UnifiedDiffManager.get(viewer()).size() >= 3,
				"the three changed regions must be reported as separate diffs");
	}

	@Test
	public void testReplaceModeWithWindowsLineDelimiters() {
		assertReplaceModeYields("line one\r\nline two\r\nline three\r\n",
				"line one\r\nline TWO modified\r\nline three\r\n");
	}

	@Test
	public void testReplaceModeAnnotatesTheAppliedText() throws BadLocationException {
		setEditorContent(LEFT);

		IStatus status = UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REPLACE_MODE).open();
		assertTrue(status.isOK(), "open() should return OK status: " + status);

		List<Annotation> additions = annotations(annotationModel(), ADDITION_ANNO_TYPE);
		assertEquals(1, additions.size(), "one addition annotation for the single diff");
		Position position = annotationModel().getPosition(additions.get(0));
		assertEquals("line TWO modified\n", document().get(position.offset, position.length),
				"the annotation must cover the text that was inserted into the document");
	}

	// ---------------------------------------------------- non modifying modes

	@Test
	public void testOverlayModeLeavesTheDocumentUnchanged() {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());

		assertEquals(LEFT, document().get(), "OVERLAY_MODE must not touch the editor document");
	}

	@Test
	public void testRevertModeLeavesTheDocumentUnchanged() {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REVERT_MODE).open().isOK());

		assertEquals(LEFT, document().get(), "REVERT_MODE must not touch the editor document");
	}

	/**
	 * The detailed (token level) annotations are positioned relative to the
	 * enclosing diff, so their offsets must stay inside it.
	 */
	@Test
	public void testDetailedAnnotationsStayInsideTheirDiff() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());

		IAnnotationModel model = annotationModel();
		Position parent = model.getPosition(annotations(model, DELETION_ANNO_TYPE).get(0));
		List<Annotation> detailed = annotations(model, DETAILED_DELETION_ANNO_TYPE);
		assertTrue(detailed.size() >= 1, "expected at least one detailed annotation");
		for (Annotation annotation : detailed) {
			Position position = model.getPosition(annotation);
			assertTrue(position.offset >= parent.offset && position.offset + position.length <= parent.offset
					+ parent.length, "detailed annotation " + position.offset + "+" + position.length
							+ " is outside its diff " + parent.offset + "+" + parent.length);
			String text = document().get(position.offset, position.length);
			assertTrue(!text.isBlank(), "a detailed annotation must not cover whitespace only, was '" + text + "'");
		}
	}

	// ------------------------------------------------------------- edge cases

	@Test
	public void testIdenticalContentProducesNoDiffs() {
		setEditorContent(LEFT);

		IStatus status = UnifiedDiff.create(editor, LEFT, UnifiedDiffMode.OVERLAY_MODE).open();

		assertTrue(status.isOK(), "open() should return OK status: " + status);
		assertTrue(UnifiedDiffManager.get(viewer()).isEmpty(), "identical content must not produce diffs");
		assertEquals(0, countAnnotations(annotationModel(), DELETION_ANNO_TYPE));
		assertEquals(LEFT, document().get());
	}

	@Test
	public void testWhitespaceOnlyChangeIsIgnoredByDefault() {
		setEditorContent("line one\nline    two\n");

		assertTrue(UnifiedDiff.create(editor, "line one\nline two\n", UnifiedDiffMode.OVERLAY_MODE).open().isOK());

		assertTrue(UnifiedDiffManager.get(viewer()).isEmpty(),
				"whitespace only changes are ignored unless requested otherwise");
	}

	@Test
	public void testWhitespaceOnlyChangeIsReportedWhenNotIgnored() {
		setEditorContent("line one\nline    two\n");

		assertTrue(UnifiedDiff.create(editor, "line one\nline two\n", UnifiedDiffMode.OVERLAY_MODE)
				.ignoreWhiteSpace(false).open().isOK());

		assertEquals(1, UnifiedDiffManager.get(viewer()).size(),
				"with ignoreWhiteSpace(false) the changed indentation is a diff");
	}

	/**
	 * Opening a second diff on the same editor must replace the first one instead
	 * of stacking annotations and diffs on top of each other.
	 */
	@Test
	public void testReopeningReplacesThePreviousDiffs() {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		int diffsAfterFirstOpen = UnifiedDiffManager.get(viewer()).size();
		int annotationsAfterFirstOpen = countAnnotations(annotationModel(), DELETION_ANNO_TYPE)
				+ countAnnotations(annotationModel(), DETAILED_DELETION_ANNO_TYPE);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());

		assertEquals(diffsAfterFirstOpen, UnifiedDiffManager.get(viewer()).size(), "diffs must not accumulate");
		assertEquals(annotationsAfterFirstOpen, countAnnotations(annotationModel(), DELETION_ANNO_TYPE)
				+ countAnnotations(annotationModel(), DETAILED_DELETION_ANNO_TYPE),
				"annotations of the previous diff must be removed");
	}

	/**
	 * The manager keeps its state in a static map keyed by the text viewer. If it
	 * is not cleaned up on dispose, every opened editor leaks its diffs and its
	 * documents for the rest of the session.
	 */
	@Test
	public void testClosingTheEditorReleasesTheManagerState() {
		setEditorContent(LEFT);
		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		ITextViewer viewer = viewer();
		assertNotNull(UnifiedDiffManager.get(viewer));

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
		editor = null;
		processEvents();

		assertNull(UnifiedDiffManager.get(viewer), "the manager must forget the viewer when it is disposed");
	}

	// ------------------------------------------------------------------ helpers

	private void assertReplaceModeYields(String left, String right) {
		setEditorContent(left);

		IStatus status = UnifiedDiff.create(editor, right, UnifiedDiffMode.REPLACE_MODE).open();

		assertTrue(status.isOK(), "open() should return OK status: " + status);
		assertEquals(right, document().get(), "REPLACE_MODE must transform the document into the compared source");
	}

	private ITextViewer viewer() {
		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		assertNotNull(viewer, "editor must adapt to ITextViewer");
		return viewer;
	}

	private IDocument document() {
		return editor.getDocumentProvider().getDocument(editor.getEditorInput());
	}

	private IAnnotationModel annotationModel() {
		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		assertNotNull(model, "annotation model must not be null");
		return model;
	}

	private void setEditorContent(String content) {
		document().set(content);
		processEvents();
	}

	private static List<Annotation> annotations(IAnnotationModel model, String type) {
		List<Annotation> result = new ArrayList<>();
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation anno = it.next();
			if (type.equals(anno.getType())) {
				result.add(anno);
			}
		}
		return result;
	}

	private static int countAnnotations(IAnnotationModel model, String type) {
		return annotations(model, type).size();
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
