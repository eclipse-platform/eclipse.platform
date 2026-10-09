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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.unifieddiff.UnifiedDiff;
import org.eclipse.compare.unifieddiff.UnifiedDiff.ToolbarAction;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.AcceptAllRunnable;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.inlined.LineHeaderAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
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
	private static final String ADDITION_RULER_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.addition.ruler";
	private static final String DELETION_RULER_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.deletion.ruler";
	private static final String DETAILED_ADDITION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedAddition";
	private static final String DETAILED_DELETION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedDeletion";
	// StyledText#getData(...) keys used by UnifiedDiffManager to remember the
	// per-diff toolbar and the annotation it was opened for.
	private static final String TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY = "TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY";
	private static final String TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY = "TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY";
	private static final String CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY = "CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY";

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
	// org.eclipse.team.ui preference deciding whether validateEdit silently makes a
	// read-only file writable when called without a UI context (as UnifiedDiffManager
	// does). Its default is already false (see TeamUIPlugin), so pinning it to false
	// is only defensive against a leaked instance-scope value from another test.
	private static final String TEAM_UI_NODE = "org.eclipse.team.ui";
	private static final String MAKE_WRITABLE_WITHOUT_CONTEXT_KEY = "org.eclipse.team.ui.validate_edit_with_no_context";
	private String previousMakeWritable;
	private final List<IStatus> loggedErrors = synchronizedList(new ArrayList<>());
	private final ILogListener logListener = (status, _) -> {
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

		previousMakeWritable = InstanceScope.INSTANCE.getNode(TEAM_UI_NODE)
				.get(MAKE_WRITABLE_WITHOUT_CONTEXT_KEY, null);
		InstanceScope.INSTANCE.getNode(TEAM_UI_NODE).putBoolean(MAKE_WRITABLE_WITHOUT_CONTEXT_KEY, false);

		Platform.addLogListener(logListener);
	}

	@AfterEach
	public void tearDown() {
		Platform.removeLogListener(logListener);
		// Clear the read-only flag a test may have set so the workspace reset can
		// delete the project afterwards.
		if (file != null && file.exists() && file.isReadOnly()) {
			setReadOnly(false);
		}
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (editor != null) {
			page.closeEditor(editor, false);
		}
		if (previousMakeWritable == null) {
			InstanceScope.INSTANCE.getNode(TEAM_UI_NODE).remove(MAKE_WRITABLE_WITHOUT_CONTEXT_KEY);
		} else {
			InstanceScope.INSTANCE.getNode(TEAM_UI_NODE).put(MAKE_WRITABLE_WITHOUT_CONTEXT_KEY, previousMakeWritable);
		}
		printLoggedErrorStackTraces();
		assertThat(loggedErrors).as("errors logged during open + paint").isEmpty();
	}

	private void printLoggedErrorStackTraces() {
		synchronized (loggedErrors) {
			for (IStatus status : loggedErrors) {
				System.err.println(status);
				Throwable exception = status.getException();
				if (exception != null) {
					exception.printStackTrace(System.err);
				}
			}
		}
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
		int additionRulerCount = countAnnotations(model, ADDITION_RULER_ANNO_TYPE);
		assertTrue(deletionCount >= 1,
				"expected at least one deletion annotation, got " + deletionCount);
		assertTrue(detailedDeletionCount >= 1,
				"expected at least one detailed-deletion annotation, got " + detailedDeletionCount);
		assertTrue(additionRulerCount >= 1,
				"expected at least one addition overview-ruler annotation, got " + additionRulerCount);
		assertTrue(annotations(model, ADDITION_RULER_ANNO_TYPE).get(0).getText().startsWith("Replacement"));

		forcePaintCycle(viewer.getTextWidget());
	}

	@Test
	public void testOverviewRulerAnnotationsHaveCompletePreferences() {
		assertCompleteRulerAnnotationPreference(ADDITION_RULER_ANNO_TYPE, "inlineDiffRulerIndication",
				"inlineDiffRulerIndicationInOverviewRuler");
		assertCompleteRulerAnnotationPreference(DELETION_RULER_ANNO_TYPE, "deletionInlineDiffRulerIndication",
				"deletionInlineDiffRulerIndicationInOverviewRuler");
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
		int additionRulerCount = countAnnotations(model, ADDITION_RULER_ANNO_TYPE);
		assertTrue(additionCount >= 1,
				"expected at least one addition annotation, got " + additionCount);
		assertTrue(detailedAdditionCount >= 1,
				"expected at least one detailed-addition annotation, got " + detailedAdditionCount);
		assertTrue(additionRulerCount >= 1,
				"expected at least one addition overview-ruler annotation, got " + additionRulerCount);
		assertEquals(0, countAnnotations(model, DELETION_RULER_ANNO_TYPE),
				"revert mode must use only addition overview-ruler annotations");

		forcePaintCycle(viewer.getTextWidget());
	}

	@Test
	public void testRevertModePaintsTrailingEmptyCrLfLine() throws BadLocationException {
		String current = "package jp1;\r\n\r\n// Footer mining manual comparison fixture \r\n\r\n"; //$NON-NLS-1$
		String source = "package jp1;\r\n\r\n// Footer mining manual comparison fixture"; //$NON-NLS-1$
		assertEquals(63, current.length());
		assertEquals(58, source.length());
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		boolean oldWhitespace = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS);
		boolean oldCr = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN);
		boolean oldLf = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED);
		try {
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, true);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN, true);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED, true);
			setEditorContent(current);

			IStatus status = UnifiedDiff.create(editor, source, UnifiedDiffMode.REVERT_MODE).ignoreWhiteSpace(false).open();
			assertTrue(status.isOK(), "open() should return OK status: " + status); //$NON-NLS-1$

			IAnnotationModel model = annotationModel();
			List<Annotation> additions = annotations(model, ADDITION_ANNO_TYPE);
			assertThat(additions).as("the added trailing lines must have one main annotation").hasSize(1); //$NON-NLS-1$
			Position position = model.getPosition(additions.get(0));
			assertThat(position.offset + position.length)
					.as("the addition annotation must include the final CRLF") //$NON-NLS-1$
					.isEqualTo(current.length());

			StyledText tw = viewer().getTextWidget();
			forcePaintCycle(tw);
			int offset = document().getLineOffset(3);
			RGB background = PreferenceConverter.getColor(store, "detailedInlineDiffColor");
			assertDelimiterBackgrounds(tw, offset, "\r\n", background, background);
		} finally {
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, oldWhitespace);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN, oldCr);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED, oldLf);
		}
	}

	@Test
	public void testDelimiterCharactersUseTheirOwnDetailedRange() throws BadLocationException {
		for (UnifiedDiffMode mode : List.of(UnifiedDiffMode.REVERT_MODE, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE)) {
			for (int[] range : new int[][] { { 0, 2 }, { 0, 1 }, { 1, 1 }, { 0, 0 } }) {
				assertDelimiterDetailedRange(mode, "\r\n", range[0], range[1]);
			}
			for (String delimiter : List.of("\r", "\n")) {
				assertDelimiterDetailedRange(mode, delimiter, 0, 1);
				assertDelimiterDetailedRange(mode, delimiter, 0, 0);
			}
		}
	}

	private void assertDelimiterDetailedRange(UnifiedDiffMode mode, String delimiter, int start, int length)
			throws BadLocationException {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		boolean oldWhitespace = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS);
		boolean oldCr = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN);
		boolean oldLf = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED);
		boolean addition = mode == UnifiedDiffMode.REVERT_MODE;
		String colorKey = addition ? "detailedInlineDiffColor" : "detailedDeletionInlineDiffColor";
		String oldColor = store.getString(colorKey);
		boolean defaultColor = store.isDefault(colorKey);
		RGB background = new RGB(73, 113, 159);
		try {
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, true);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN, true);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED, true);
			PreferenceConverter.setValue(store, colorKey, background);
			setEditorContent(("package jp1;\r\n\r\n// Footer mining manual comparison fixture \r\n\r\n")
					.replace("\r\n", delimiter));
			assertTrue(UnifiedDiff.create(editor,
					("package jp1;\r\n\r\n// Other mining changed comparison words").replace("\r\n", delimiter), mode)
					.ignoreWhiteSpace(false).open().isOK());
			IAnnotationModel model = annotationModel();
			List<Annotation> details = annotations(model,
					addition ? DETAILED_ADDITION_ANNO_TYPE : DETAILED_DELETION_ANNO_TYPE);
			assertThat(details).as("exercise lookup among multiple unordered token annotations").hasSizeGreaterThan(1);
			int offset = document().getLineOffset(3);
			Annotation delimiterDetail = details.stream()
					.filter(annotation -> model.getPosition(annotation).overlapsWith(offset, delimiter.length()))
					.findFirst().orElseThrow();
			// Restrict the token annotation to one delimiter character (or neither).
			((IAnnotationModelExtension) model).modifyAnnotationPosition(delimiterDetail,
					new Position(offset + start, length));
			StyledText tw = viewer().getTextWidget();
			forcePaintCycle(tw);
			int lfIndex = delimiter.indexOf('\n');
			assertDelimiterBackgrounds(tw, offset, delimiter, start == 0 && length > 0 ? background : null,
					start <= lfIndex && lfIndex < start + length ? background : null);
		} finally {
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, oldWhitespace);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN, oldCr);
			store.setValue(AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED, oldLf);
			if (defaultColor) {
				store.setToDefault(colorKey);
			} else {
				store.setValue(colorKey, oldColor);
			}
		}
	}

	private static void assertDelimiterBackgrounds(StyledText tw, int offset, String delimiter, RGB crBackground,
			RGB lfBackground) {
		Point location = tw.getLocationAtOffset(offset);
		Rectangle bounds = tw.getClientArea();
		Image image = new Image(tw.getDisplay(), bounds.width, bounds.height);
		GC gc = new GC(image);
		try {
			gc.setFont(tw.getFont());
			gc.setForeground(tw.getForeground());
			gc.setBackground(tw.getBackground());
			gc.fillRectangle(bounds);
			// Render the unified-diff paint listener into an image. Native widget capture
			// depends on visibility and can convert colors through the display profile.
			List<PaintListener> listeners = tw.getTypedListeners(SWT.Paint, PaintListener.class)
					.filter(listener -> listener.getClass().getEnclosingClass() == UnifiedDiffManager.class
							&& !listener.getClass().isAnonymousClass()).toList();
			assertThat(listeners).as("the unified-diff paint listener must be installed").hasSize(1);
			firePaint(tw, gc, listeners);
			int crWidth = delimiter.startsWith("\r") ? gc.stringExtent("¤").x : 0;
			int width = crWidth + (delimiter.endsWith("\n") ? gc.stringExtent("¶").x : 0);
			int height = gc.stringExtent("¤¶").y;
			// Preserve native pixels: downscaling a Retina image blends the narrow
			// delimiter background with the glyph and neighboring cells.
			int zoom = tw.getMonitor().getZoom();
			ImageData data = image.getImageData(zoom);
			location.x = location.x * zoom / 100;
			location.y = location.y * zoom / 100;
			crWidth = crWidth * zoom / 100;
			width = width * zoom / 100;
			height = height * zoom / 100;
			RGB mainBackground = data.palette.getRGB(data.getPixel(location.x + width + 1, location.y + height / 2));
			if (crWidth > 0) {
				assertMarkerOnBackground(data, location.x, location.x + crWidth, location.y, height,
						crBackground != null ? crBackground : mainBackground,
						crBackground != null ? mainBackground : lfBackground);
			}
			if (delimiter.endsWith("\n")) {
				assertMarkerOnBackground(data, location.x + crWidth, location.x + width, location.y, height,
						lfBackground != null ? lfBackground : mainBackground,
						lfBackground != null ? mainBackground : crBackground);
			}
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private static void assertMarkerOnBackground(ImageData image, int from, int to, int top, int height, RGB background,
			RGB otherBackground) {
		int backgroundPixels = 0;
		int glyphPixels = 0;
		int otherBackgroundPixels = 0;
		for (int x = from; x < to; x++) {
			// Exclude the band's border, which must not count as a glyph pixel.
			for (int y = top + 1; y < top + height - 1; y++) {
				RGB pixel = image.palette.getRGB(image.getPixel(x, y));
				if (pixel.equals(otherBackground)) {
					otherBackgroundPixels++;
				}
				if (background.equals(pixel)) {
					backgroundPixels++;
				} else {
					glyphPixels++;
				}
			}
		}
		assertThat(backgroundPixels).as("delimiter cell must have the annotation background")
				.isGreaterThan(0);
		assertThat(otherBackgroundPixels).as("delimiter cell must not retain the other annotation background").isZero();
		assertThat(glyphPixels).as("delimiter glyph must be visible").isGreaterThan(0);
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
		assertEquals(1, countAnnotations(annotationModel(), ADDITION_RULER_ANNO_TYPE),
				"one addition overview-ruler annotation for the single diff");
		Position position = annotationModel().getPosition(additions.get(0));
		assertEquals("line TWO modified\n", document().get(position.offset, position.length),
				"the annotation must cover the text that was inserted into the document");
	}

	@Test
	public void testRulerAnnotationTextDescribesReplacement() {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REPLACE_MODE).open().isOK());

		assertEquals("Replacement at line 2",
				annotations(annotationModel(), ADDITION_RULER_ANNO_TYPE).get(0).getText());
	}

	@Test
	public void testReplaceModeRulerAnnotationTextDescribesInsertedTextAsDeletion() {
		setEditorContent("one\n");

		assertTrue(UnifiedDiff.create(editor, "one\ntwo\n", UnifiedDiffMode.REPLACE_MODE).open().isOK());

		assertEquals("Deletion at line 2",
				annotations(annotationModel(), DELETION_RULER_ANNO_TYPE).get(0).getText());
	}

	@Test
	public void testRevertModeRulerAnnotationTextDescribesRemovedCurrentTextAsAddition() {
		setEditorContent("one\ntwo\n");

		assertTrue(UnifiedDiff.create(editor, "one\n", UnifiedDiffMode.REVERT_MODE).open().isOK());

		assertEquals("Addition at line 2",
				annotations(annotationModel(), ADDITION_RULER_ANNO_TYPE).get(0).getText());
	}

	/**
	 * The token comparator, not the mode, decides what counts as a difference. A
	 * case-insensitive comparator reports no difference between "line one" and
	 * "LINE ONE", so REPLACE_MODE must leave the document untouched rather than
	 * rewrite it to a form the comparator already considers identical.
	 */
	@Test
	public void testReplaceModeKeepsDocumentWhenTokenComparatorTreatsChangeAsEqual() {
		setEditorContent("line one\n");

		assertTrue(UnifiedDiff.create(editor, "LINE ONE\n", UnifiedDiffMode.REPLACE_MODE).ignoreWhiteSpace(false)
				.tokenComparatorFactory(CaseInsensitiveTokenComparator::new).open().isOK());

		assertTrue(UnifiedDiffManager.get(viewer()).isEmpty(),
				"a change the token comparator treats as equal must not be applied, even in REPLACE_MODE");
		assertEquals("line one\n", document().get(),
				"REPLACE_MODE must leave the document untouched when the comparator sees no real difference");
	}

	/**
	 * The counterpart to {@link #testReplaceModeKeepsDocumentWhenTokenComparatorTreatsChangeAsEqual()}:
	 * with the default comparator a case change is a real difference, so REPLACE_MODE
	 * applies it and the document becomes the compared source.
	 */
	@Test
	public void testReplaceModeAppliesCaseChangeWithDefaultComparator() {
		setEditorContent("line one\n");

		assertTrue(UnifiedDiff.create(editor, "LINE ONE\n", UnifiedDiffMode.REPLACE_MODE).ignoreWhiteSpace(false).open()
				.isOK());

		assertEquals("LINE ONE\n", document().get(),
				"with the default comparator a case change is a real difference and must be applied");
	}

	// ------------------------------------------------------- compare filters

	/**
	 * A compare filter that strips a known suffix from every line before
	 * comparison. Lines that differ only in that suffix must not produce a diff.
	 */
	@Test
	public void testCompareFilterSuppressesDiffForFilteredRegion() {
		// left: "line one[IGNORED]", right: "line one[DIFFERENT]"
		// Without a filter these two lines differ; with a filter that strips
		// everything from the 9th character onwards they are equal.
		String left = "line one[IGNORED]\n";
		String right = "line one[DIFFERENT]\n";
		setEditorContent(left);

		ICompareFilter suffixFilter = new SuffixStrippingFilter(8); // strips from offset 8 onward
		assertTrue(UnifiedDiff.create(editor, right, UnifiedDiffMode.OVERLAY_MODE)
				.ignoreWhiteSpace(false)
				.compareFilters(new ICompareFilter[] { suffixFilter })
				.open().isOK());

		assertTrue(UnifiedDiffManager.get(viewer()).isEmpty(),
				"a difference covered entirely by the compare filter must not produce a diff");
	}

	/**
	 * A compare filter that returns {@code null} regions (no-op) must not prevent
	 * real differences from being reported.
	 */
	@Test
	public void testNoOpCompareFilterStillReportsDiff() {
		setEditorContent("line one\nline two\n");

		ICompareFilter noOpFilter = new SuffixStrippingFilter(-1); // strips nothing
		assertTrue(UnifiedDiff.create(editor, "line one\nline TWO\n", UnifiedDiffMode.OVERLAY_MODE)
				.ignoreWhiteSpace(false)
				.compareFilters(new ICompareFilter[] { noOpFilter })
				.open().isOK());

		assertEquals(1, UnifiedDiffManager.get(viewer()).size(),
				"a no-op compare filter must not suppress a real difference");
	}

	@Test
	public void testCompareFilterReceivesContributorIdentities() {
		setEditorContent("left\n");

		ContributorRecordingFilter filter = new ContributorRecordingFilter();
		assertTrue(UnifiedDiff.create(editor, "right\n", UnifiedDiffMode.OVERLAY_MODE)
				.ignoreWhiteSpace(false)
				.compareFilters(new ICompareFilter[] { filter }, MergeViewerContentProvider.RIGHT_CONTRIBUTOR,
						MergeViewerContentProvider.LEFT_CONTRIBUTOR)
				.open().isOK());

		assertTrue(filter.comparisons.contains("R:L"), "the editor must retain its logical contributor identity");
		assertTrue(filter.comparisons.contains("L:R"), "the source must retain its logical contributor identity");
	}

	// ------------------------------------------- read-only / validateEdit guard

	// Only an actual document modification may run validateEdit and prompt the user
	// to make a read-only file writable. REPLACE_MODE modifies the document while it
	// opens, so it validates up front. The overlay/revert modes only add annotations
	// when they open and modify later, from their accept/revert/undo actions, so
	// opening them must not run validateEdit, but accepting/reverting must. In the
	// headless test environment validateEdit on a read-only file returns an error
	// (no UI prompt), so a modification that wrongly skipped the check would still
	// change the document, and an open() that wrongly ran it would fail with CANCEL.

	@Test
	public void testReplaceModeOnReadOnlyFileIsCancelledAndKeepsDocument() {
		setEditorContent(LEFT);
		setReadOnly(true);

		IStatus status = UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REPLACE_MODE).open();

		assertEquals(IStatus.CANCEL, status.getSeverity(),
				"REPLACE_MODE must fail the validateEdit check on a read-only file");
		assertEquals(LEFT, document().get(),
				"a cancelled validateEdit must leave the document untouched");
	}

	@Test
	public void testOverlayModeOnReadOnlyFileOpensWithoutValidateEdit() {
		setEditorContent(LEFT);
		setReadOnly(true);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK(),
				"opening OVERLAY_MODE does not modify the document and must not run validateEdit");
		assertEquals(LEFT, document().get(), "opening an overlay must not touch the document");
	}

	@Test
	public void testOverlayReadOnlyModeOnReadOnlyFileOpensWithoutValidateEdit() {
		setEditorContent(LEFT);
		setReadOnly(true);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK(),
				"opening OVERLAY_READ_ONLY_MODE does not modify the document and must not run validateEdit");
		assertEquals(LEFT, document().get(), "opening a read-only overlay must not touch the document");
	}

	@Test
	public void testRevertModeOnReadOnlyFileOpensWithoutValidateEdit() {
		setEditorContent(LEFT);
		setReadOnly(true);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REVERT_MODE).open().isOK(),
				"opening REVERT_MODE does not modify the document and must not run validateEdit");
		assertEquals(LEFT, document().get(), "opening a revert diff must not touch the document");
	}

	@Test
	public void testAcceptAllOnReadOnlyOverlayDoesNotModifyTheDocument() {
		setEditorContent(LEFT);
		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		assertFalse(UnifiedDiffManager.get(viewer()).isEmpty(), "the overlay must have a diff that could be applied");
		setReadOnly(true);

		new AcceptAllRunnable(viewer(), annotationModel()).run();

		// A leaked modification would be deferred until a repaint, so pump paints
		// before asserting that the document stayed unchanged.
		assertFalse(waitForDocument(RIGHT, 2_000), "accepting a diff on a read-only file must be blocked by validateEdit");
		assertEquals(LEFT, document().get(), "a blocked accept must leave the document untouched");
	}

	@Test
	public void testRevertAllOnReadOnlyRevertDiffDoesNotModifyTheDocument() {
		setEditorContent(LEFT);
		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REVERT_MODE).open().isOK());
		assertFalse(UnifiedDiffManager.get(viewer()).isEmpty(), "the revert diff must have a change that could be reverted");
		setReadOnly(true);

		// Revert mode reuses AcceptAllRunnable for its "revert all" action.
		new AcceptAllRunnable(viewer(), annotationModel()).run();

		// A leaked modification would be deferred until a repaint, so pump paints
		// before asserting that the document stayed unchanged.
		assertFalse(waitForDocument(RIGHT, 2_000), "reverting a diff on a read-only file must be blocked by validateEdit");
		assertEquals(LEFT, document().get(), "a blocked revert must leave the document untouched");
	}

	/**
	 * The positive control for the two tests above: on a writable file the same
	 * accept-all must pass validateEdit and actually apply the diff. This both
	 * guards the normal path against the lazy guard and proves the waitForDocument
	 * harness the negative tests rely on can observe a real modification.
	 */
	@Test
	public void testAcceptAllOnWritableOverlayModifiesTheDocument() {
		setEditorContent(LEFT);
		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		assertFalse(UnifiedDiffManager.get(viewer()).isEmpty(), "the overlay must have a diff that could be applied");

		new AcceptAllRunnable(viewer(), annotationModel()).run();

		assertTrue(waitForDocument(RIGHT, 2_000), "accept all must apply the diff on a writable file");
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

	@Test
	public void testTokenComparatorCanIgnoreALineChange() {
		setEditorContent("line one\n");

		assertTrue(UnifiedDiff.create(editor, "LINE ONE\n", UnifiedDiffMode.OVERLAY_MODE)
				.ignoreWhiteSpace(false).tokenComparatorFactory(CaseInsensitiveTokenComparator::new).open().isOK());

		assertTrue(UnifiedDiffManager.get(viewer()).isEmpty(),
				"a change ignored by the token comparator must not create a parent diff");
	}

	/**
	 * A diff at the very end of the document is shown as a code mining. Only a line
	 * header mining reserves its height in the text widget, so an emptied file must
	 * not fall back to a footer mining: the removed content would be unreachable
	 * because the viewer would have nothing to scroll over.
	 */
	@Test
	public void testRemovingTheWholeContentStaysScrollable() {
		setEditorContent("");
		String removed = "a removed line\n".repeat(200);

		assertTrue(UnifiedDiff.create(editor, removed, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());

		LineHeaderAnnotation header = waitForLineHeaderAnnotation();
		assertNotNull(header, "the removed content must be shown as a line header mining, not as a document footer");
		int reserved = header.getHeight();
		assertTrue(reserved >= 200 * viewer().getTextWidget().getLineHeight(),
				"the 200 removed lines must reserve scrollable space, but only " + reserved + " pixels were reserved");
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
				+ countAnnotations(annotationModel(), DETAILED_DELETION_ANNO_TYPE)
				+ countAnnotations(annotationModel(), ADDITION_RULER_ANNO_TYPE)
				+ countAnnotations(annotationModel(), DELETION_RULER_ANNO_TYPE);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());

		assertEquals(diffsAfterFirstOpen, UnifiedDiffManager.get(viewer()).size(), "diffs must not accumulate");
		assertEquals(annotationsAfterFirstOpen, countAnnotations(annotationModel(), DELETION_ANNO_TYPE)
				+ countAnnotations(annotationModel(), DETAILED_DELETION_ANNO_TYPE)
				+ countAnnotations(annotationModel(), ADDITION_RULER_ANNO_TYPE)
				+ countAnnotations(annotationModel(), DELETION_RULER_ANNO_TYPE),
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

	// ------------------------------------------------------ mouse move listener

	/** Hovering over a diff must open its toolbar. */
	@Test
	public void testMouseMoveOpensTheToolbarForTheHoveredDiff() throws BadLocationException {
		setEditorContent(LEFT);
		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());

		IAnnotationModel model = annotationModel();
		List<Annotation> deletions = annotations(model, DELETION_ANNO_TYPE);
		assertEquals(1, deletions.size(), "the sample only contains a single changed line");
		Position pos = model.getPosition(deletions.get(0));
		int expectedLine = document().getLineOfOffset(pos.offset);

		StyledText tw = viewer().getTextWidget();
		Composite toolbarBefore = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY);

		fireMouseMove(tw, tw.getLinePixel(widgetLineOfModelOffset(viewer(), pos.offset)) + 2);
		processEvents();

		Composite toolbar = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY);
		assertNotNull(toolbar, "hovering over a diff must show its toolbar");
		assertNotSame(toolbarBefore, toolbar, "hovering over a diff must create a new toolbar composite");
		Annotation selected = (Annotation) toolbar.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
		assertNotNull(selected, "the toolbar must remember which diff it was opened for");
		Position selectedPos = model.getPosition(selected);
		assertEquals(expectedLine, document().getLineOfOffset(selectedPos.offset),
				"the diff under the mouse cursor must be the one whose toolbar is shown");
	}

	@Test
	public void testNavigationCommandsUseTheUnifiedDiffHandlers() throws Exception {
		setEditorContent("""
				line one
				line two
				line three
				line four
				""");
		assertTrue(UnifiedDiff.create(editor, """
				line ONE changed
				line two
				line THREE changed
				line four
				""", UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());

		IAnnotationModel model = annotationModel();
		List<Annotation> deletions = annotations(model, DELETION_ANNO_TYPE);
		deletions.sort((left, right) -> Integer.compare(model.getPosition(left).offset, model.getPosition(right).offset));
		assertEquals(2, deletions.size(), "the sample must produce two unified diffs");

		Position first = model.getPosition(deletions.get(0));
		Position second = model.getPosition(deletions.get(1));
		waitForSelectedOffset(first.offset);

		executeEditorCommand(ActionFactory.NEXT.getCommandId());
		assertEquals(second.offset, viewer().getSelectedRange().x,
				"The compare Next command must navigate to the next unified diff");

		executeEditorCommand(ActionFactory.PREVIOUS.getCommandId());
		assertEquals(first.offset, viewer().getSelectedRange().x,
				"The compare Previous command must navigate to the previous unified diff");
	}

	@Test
	public void testHideAllDiffsCommandDisposesTheUnifiedDiff() throws Exception {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());
		assertNotNull(UnifiedDiffManager.get(viewer()), "the unified diff must be registered before hiding it");

		executeEditorCommand("org.eclipse.compare.hideAllDiffs"); //$NON-NLS-1$

		assertNull(UnifiedDiffManager.get(viewer()), "Hide All Diffs must dispose the unified diff state");
		assertEquals(0, countAnnotations(annotationModel(), DELETION_ANNO_TYPE));
		assertEquals(0, countAnnotations(annotationModel(), DETAILED_DELETION_ANNO_TYPE));
	}

	@Test
	public void testAcceptAllDiffsCommandAppliesAllChanges() throws Exception {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		assertNotNull(UnifiedDiffManager.get(viewer()), "the unified diff must be registered before accepting");

		executeEditorCommand("org.eclipse.compare.acceptAllDiffs"); //$NON-NLS-1$

		assertNull(UnifiedDiffManager.get(viewer()), "Accept All Diffs must dispose the unified diff state");
		waitForDocumentContent(RIGHT);
	}

	@Test
	public void testAcceptCurrentDiffCommandAppliesCurrentChange() throws Exception {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		IAnnotationModel model = annotationModel();
		List<Annotation> deletions = annotations(model, DELETION_ANNO_TYPE);
		assertEquals(1, deletions.size(), "the sample must produce exactly one unified diff");
		Position pos = model.getPosition(deletions.get(0));

		StyledText tw = viewer().getTextWidget();
		fireMouseMove(tw, tw.getLinePixel(widgetLineOfModelOffset(viewer(), pos.offset)) + 2);
		processEvents();

		executeEditorCommand("org.eclipse.compare.acceptCurrentDiff"); //$NON-NLS-1$

		waitForDocumentContent(RIGHT);
	}

	/**
	 * The keyboard shortcut must work without the per-diff toolbar being open: a
	 * user navigating with Next/Previous has no hover toolbar, so the current diff
	 * is resolved from the caret instead.
	 */
	@Test
	public void testAcceptCurrentDiffCommandAppliesDiffAtCaretWithoutToolbar() throws Exception {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE).open().isOK());
		IAnnotationModel model = annotationModel();
		List<Annotation> deletions = annotations(model, DELETION_ANNO_TYPE);
		assertEquals(1, deletions.size(), "the sample must produce exactly one unified diff");
		Position pos = model.getPosition(deletions.get(0));

		// Place the caret inside the diff without ever hovering it, so no per-diff
		// toolbar exists.
		viewer().setSelectedRange(pos.offset, 0);
		processEvents();
		assertNull(viewer().getTextWidget().getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY),
				"no per-diff toolbar must be open for this test to prove the caret fallback");

		executeEditorCommand("org.eclipse.compare.acceptCurrentDiff"); //$NON-NLS-1$

		waitForDocumentContent(RIGHT);
	}

	/**
	 * Hide Current Diff removes a single diff. Like Accept Current Diff it must
	 * resolve the diff from the caret when no hover toolbar is open, so the
	 * keyboard shortcut works for keyboard-only navigation.
	 */
	@Test
	public void testHideCurrentDiffCommandHidesDiffAtCaretWithoutToolbar() throws Exception {
		setEditorContent("""
				line one
				line two
				line three
				line four
				""");
		assertTrue(UnifiedDiff.create(editor, """
				line ONE changed
				line two
				line THREE changed
				line four
				""", UnifiedDiffMode.OVERLAY_READ_ONLY_MODE).open().isOK());

		IAnnotationModel model = annotationModel();
		List<Annotation> deletions = annotations(model, DELETION_ANNO_TYPE);
		assertEquals(2, deletions.size(), "the sample must produce two unified diffs");
		deletions.sort((left, right) -> Integer.compare(model.getPosition(left).offset, model.getPosition(right).offset));
		Position first = model.getPosition(deletions.get(0));

		// Caret on the first diff, no hover toolbar.
		viewer().setSelectedRange(first.offset, 0);
		processEvents();
		assertNull(viewer().getTextWidget().getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY),
				"no per-diff toolbar must be open for this test to prove the caret fallback");

		executeEditorCommand("org.eclipse.compare.hideCurrentDiff"); //$NON-NLS-1$

		assertEquals(1, UnifiedDiffManager.get(viewer()).size(),
				"Hide Current Diff must remove only the diff at the caret, leaving the other one");
		assertNotNull(UnifiedDiffManager.get(viewer()), "hiding one of two diffs must not dispose the unified diff");
	}

	@Test
	public void testToolbarActionPresentationCanOverrideTextAndRemoveImage() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(ToolbarAction.ACCEPT_ALL, "Apply all changes")
				.toolbarActionImage(ToolbarAction.ACCEPT_ALL, null)
				.toolbarActionText(ToolbarAction.ACCEPT, "Apply change")
				.toolbarActionImage(ToolbarAction.ACCEPT, null).open().isOK());

		StyledText tw = viewer().getTextWidget();
		Composite allDiffsToolbar = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY);
		assertNotNull(allDiffsToolbar, "the all-diffs toolbar must be shown");
		assertTextOnlyToolbarItem(toolbar(allDiffsToolbar), "Apply all changes");

		Position pos = annotationModel().getPosition(annotations(annotationModel(), DELETION_ANNO_TYPE).get(0));
		fireMouseMove(tw, tw.getLinePixel(widgetLineOfModelOffset(viewer(), pos.offset)) + 2);
		processEvents();

		Composite oneDiffToolbar = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY);
		assertNotNull(oneDiffToolbar, "the per-diff toolbar must be shown");
		assertTextOnlyToolbarItem(toolbar(oneDiffToolbar), "Apply change");
	}

	@Test
	public void testToolbarActionPresentationInRevertMode() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REVERT_MODE)
				.toolbarActionText(ToolbarAction.REVERT_ALL, "Revert everything")
				.toolbarActionImage(ToolbarAction.REVERT_ALL, null)
				.toolbarActionText(ToolbarAction.REVERT, "Revert this")
				.toolbarActionImage(ToolbarAction.REVERT, null).open().isOK());

		StyledText tw = viewer().getTextWidget();
		assertTextOnlyToolbarItem(toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY)),
				"Revert everything");

		Position pos = annotationModel().getPosition(annotations(annotationModel(), ADDITION_ANNO_TYPE).get(0));
		fireMouseMove(tw, tw.getLinePixel(widgetLineOfModelOffset(viewer(), pos.offset)) + 2);
		processEvents();

		assertTextOnlyToolbarItem(toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY)), "Revert this");
	}

	@Test
	public void testToolbarActionPresentationInKeepUndoMode() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.REPLACE_MODE)
				.toolbarActionText(ToolbarAction.KEEP_ALL, "Keep everything")
				.toolbarActionText(ToolbarAction.KEEP, "Keep this").open().isOK());

		StyledText tw = viewer().getTextWidget();
		assertTextOnlyToolbarItem(toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY)),
				"Keep everything");

		Position pos = annotationModel().getPosition(annotations(annotationModel(), ADDITION_ANNO_TYPE).get(0));
		fireMouseMove(tw, tw.getLinePixel(widgetLineOfModelOffset(viewer(), pos.offset)) + 2);
		processEvents();

		assertTextOnlyToolbarItem(toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY)), "Keep this");
	}

	/**
	 * Overriding text without overriding the image must leave the default image in
	 * place (the two overrides are independent).
	 */
	@Test
	public void testToolbarActionTextOnlyOverrideKeepsDefaultImage() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(ToolbarAction.ACCEPT_ALL, "My custom label").open().isOK());

		StyledText tw = viewer().getTextWidget();
		ToolBar toolbar = toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY));
		// an action with an image shows its text only as the tooltip
		ToolItem item = findToolbarItem(toolbar, ToolItem::getToolTipText, "My custom label");
		assertNotNull(item, "toolbar item with overridden tooltip must exist");
		assertNotNull(item.getImage(), "a text-only override must not remove the default image");
	}

	/**
	 * Opening a second diff on the same editor must replace the first toolbar
	 * presentations, not accumulate them.
	 */
	@Test
	public void testReopeningReplacesToolbarPresentations() throws BadLocationException {
		setEditorContent(LEFT);

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(ToolbarAction.ACCEPT_ALL, "First label")
				.toolbarActionImage(ToolbarAction.ACCEPT_ALL, null).open().isOK());

		assertTrue(UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(ToolbarAction.ACCEPT_ALL, "Second label")
				.toolbarActionImage(ToolbarAction.ACCEPT_ALL, null).open().isOK());

		StyledText tw = viewer().getTextWidget();
		ToolBar toolbar = toolbar((Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY));
		assertNull(findToolbarItem(toolbar, ToolItem::getText, "First label"),
				"first label must no longer appear after re-open");
		assertNotNull(findToolbarItem(toolbar, ToolItem::getText, "Second label"),
				"second label must be shown after re-open");
	}

	@Test
	public void testToolbarActionTextRejectsNullAction() {
		assertThrows(NullPointerException.class, () -> UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(null, "some text"));
	}

	@Test
	public void testToolbarActionTextRejectsNullText() {
		assertThrows(NullPointerException.class, () -> UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionText(ToolbarAction.ACCEPT_ALL, null));
	}

	@Test
	public void testToolbarActionImageRejectsNullAction() {
		assertThrows(NullPointerException.class, () -> UnifiedDiff.create(editor, RIGHT, UnifiedDiffMode.OVERLAY_MODE)
				.toolbarActionImage(null, null));
	}

	// ------------------------------------------------------------------ helpers

	private void assertReplaceModeYields(String left, String right) {
		setEditorContent(left);

		IStatus status = UnifiedDiff.create(editor, right, UnifiedDiffMode.REPLACE_MODE).open();

		assertTrue(status.isOK(), "open() should return OK status: " + status);
		assertEquals(right, document().get(), "REPLACE_MODE must transform the document into the compared source");
	}

	private static final class CaseInsensitiveTokenComparator implements ITokenComparator {
		private final String text;

		CaseInsensitiveTokenComparator(String text) {
			this.text = text;
		}

		@Override
		public int getRangeCount() {
			return 1;
		}

		@Override
		public int getTokenStart(int index) {
			return index == 0 ? 0 : text.length();
		}

		@Override
		public int getTokenLength(int index) {
			return index == 0 ? text.length() : 0;
		}

		@Override
		public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
			return other instanceof CaseInsensitiveTokenComparator comparator && text.equalsIgnoreCase(comparator.text);
		}

		@Override
		public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
			return false;
		}
	}

	/**
	 * A compare filter that strips every character from {@code stripFromOffset}
	 * onwards (like a suffix). Pass {@code -1} to strip nothing (no-op).
	 */
	private static final class SuffixStrippingFilter implements ICompareFilter {
		private final int stripFromOffset;

		SuffixStrippingFilter(int stripFromOffset) {
			this.stripFromOffset = stripFromOffset;
		}

		@Override
		public void setInput(Object input, Object ancestor, Object left, Object right) {
			// nothing to store
		}

		@Override
		public IRegion[] getFilteredRegions(HashMap lineComparison) {
			if (stripFromOffset < 0) {
				return null;
			}
			String line = (String) lineComparison.get(THIS_LINE);
			if (line == null || stripFromOffset >= line.length()) {
				return null;
			}
			return new IRegion[] { new Region(stripFromOffset, line.length() - stripFromOffset) };
		}

		@Override
		public boolean isEnabledInitially() {
			return true;
		}

		@Override
		public boolean canCacheFilteredRegions() {
			return true;
		}
	}

	private static final class ContributorRecordingFilter implements ICompareFilter {
		private final List<String> comparisons = new ArrayList<>();

		@Override
		public void setInput(Object input, Object ancestor, Object left, Object right) {
			// nothing to store
		}

		@Override
		public IRegion[] getFilteredRegions(HashMap lineComparison) {
			comparisons.add(lineComparison.get(THIS_CONTRIBUTOR) + ":" + lineComparison.get(OTHER_CONTRIBUTOR));
			return null;
		}

		@Override
		public boolean isEnabledInitially() {
			return true;
		}

		@Override
		public boolean canCacheFilteredRegions() {
			return false;
		}
	}

	private ITextViewer viewer() {
		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		assertNotNull(viewer, "editor must adapt to ITextViewer");
		return viewer;
	}

	/**
	 * Dispatches a synthetic {@link SWT#MouseMove} event with the given
	 * widget-relative y coordinate to every {@link MouseMoveListener} attached to
	 * the widget, in particular {@code UnifiedDiffManager}'s own listener.
	 */
	private static void fireMouseMove(StyledText tw, int y) {
		Event event = new Event();
		event.widget = tw;
		event.x = 5;
		event.y = y;
		MouseEvent mouseEvent = new MouseEvent(event);
		tw.getTypedListeners(SWT.MouseMove, MouseMoveListener.class).forEach(listener -> listener.mouseMove(mouseEvent));
	}

	private static ToolBar toolbar(Composite composite) {
		assertNotNull(composite, "the toolbar composite must be shown");
		for (var child : composite.getChildren()) {
			if (child instanceof ToolBar toolbar) {
				return toolbar;
			}
		}
		throw new AssertionError("toolbar control not found");
	}

	private static ToolItem findToolbarItem(ToolBar toolbar, Function<ToolItem, String> property, String expected) {
		for (ToolItem item : toolbar.getItems()) {
			if (expected.equals(property.apply(item))) {
				return item;
			}
		}
		return null;
	}

	/** Asserts that an action without an image is shown with the given text. */
	private static void assertTextOnlyToolbarItem(ToolBar toolbar, String text) {
		ToolItem item = findToolbarItem(toolbar, ToolItem::getText, text);
		assertNotNull(item, "toolbar item not found: " + text);
		assertNull(item.getImage(), "an action without an image must display its text");
	}

	/** The widget line the given model (document) offset is displayed on. */
	private static int widgetLineOfModelOffset(ITextViewer viewer, int modelOffset) {
		int widgetOffset = modelOffset;
		if (viewer instanceof ProjectionViewer pv) {
			widgetOffset = pv.modelOffset2WidgetOffset(modelOffset);
		}
		return viewer.getTextWidget().getLineAtOffset(widgetOffset);
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

	/**
	 * Flags or unflags the underlying file as read-only. A document modification
	 * runs validateEdit on such a file; opening a non modifying diff must not.
	 */
	private void setReadOnly(boolean readOnly) {
		try {
			ResourceAttributes attributes = file.getResourceAttributes();
			assertNotNull(attributes, "file must expose resource attributes");
			attributes.setReadOnly(readOnly);
			file.setResourceAttributes(attributes);
		} catch (CoreException e) {
			throw new AssertionError("could not change the read-only state of " + file, e);
		}
	}

	/**
	 * Pumps the event loop until the document reaches the expected content or
	 * {@code timeoutMillis} elapses. The accept/revert/undo actions defer the actual
	 * replace to a {@code timerExec(100, ...)} scheduled from a PaintListener (see
	 * runAfterRepaintFinished). A single synthetic paint schedules that timer; the
	 * loop must then only pump and sleep, because firing another paint would reset
	 * the 100 ms timer before it can mature.
	 */
	private boolean waitForDocument(String expected, long timeoutMillis) {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		firePaint(viewer().getTextWidget());
		while (System.currentTimeMillis() < deadline) {
			processEvents();
			if (expected.equals(document().get())) {
				return true;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return expected.equals(document().get());
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

	private static void assertCompleteRulerAnnotationPreference(String annotationType, String textPreferenceKey,
			String overviewRulerPreferenceKey) {
		AnnotationPreference preference = new MarkerAnnotationPreferences().getAnnotationPreferences().stream()
				.filter(candidate -> annotationType.equals(candidate.getAnnotationType())).findFirst().orElse(null);
		assertNotNull(preference, "ruler annotation must be registered as a complete preference");
		assertEquals(textPreferenceKey, preference.getTextPreferenceKey());
		assertEquals(overviewRulerPreferenceKey, preference.getOverviewRulerPreferenceKey());
		assertTrue(preference.getOverviewRulerPreferenceValue());
	}

	/**
	 * Returns the line header annotation of the code minings, or <code>null</code>
	 * if none appears within the timeout. The minings are computed asynchronously,
	 * so the annotation only reaches the model after the request completed. Its
	 * height is asserted instead of the vertical indent of the widget line, because
	 * the indent is applied while painting, and a build machine gives no guarantee
	 * that the editor is ever painted.
	 */
	private LineHeaderAnnotation waitForLineHeaderAnnotation() {
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			processEvents();
			Iterator<Annotation> it = ((ISourceViewer) viewer()).getAnnotationModel().getAnnotationIterator();
			while (it.hasNext()) {
				if (it.next() instanceof LineHeaderAnnotation header) {
					return header;
				}
			}
		}
		return null;
	}

	private static void forcePaintCycle(StyledText tw) {
		if (tw == null || tw.isDisposed()) {
			return;
		}
		tw.redraw();
		tw.update();
		processEvents();
	}

	/**
	 * Dispatches a synthetic {@link SWT#Paint} event to every {@link PaintListener}
	 * attached to the widget, mirroring {@link #fireMouseMove(StyledText, int)}. An
	 * off-screen widget on a build machine never reliably receives a real paint, so
	 * this drives UnifiedDiffManager.runAfterRepaintFinished, which only schedules
	 * its deferred document replace from a PaintListener.
	 */
	private static void firePaint(StyledText tw) {
		if (tw == null || tw.isDisposed()) {
			return;
		}
		GC gc = new GC(tw);
		try {
			firePaint(tw, gc, tw.getTypedListeners(SWT.Paint, PaintListener.class).toList());
		} finally {
			gc.dispose();
		}
	}

	private static void firePaint(StyledText tw, GC gc, List<PaintListener> listeners) {
		Event event = new Event();
		event.widget = tw;
		event.gc = gc;
		event.x = 0;
		event.y = 0;
		event.width = Math.max(1, tw.getClientArea().width);
		event.height = Math.max(1, tw.getClientArea().height);
		PaintEvent paintEvent = new PaintEvent(event);
		listeners.forEach(listener -> listener.paintControl(paintEvent));
	}

	private void executeEditorCommand(String commandId) throws Exception {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editor);
		IHandlerService handlerService = editor.getSite().getService(IHandlerService.class);
		assertNotNull(handlerService, "editor site must provide the handler service");
		handlerService.executeCommand(commandId, null);
		processEvents();
	}

	private void waitForSelectedOffset(int expectedOffset) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			forcePaintCycle(viewer().getTextWidget());
			if (viewer().getSelectedRange().x == expectedOffset) {
				return;
			}
			Thread.sleep(20);
		}
		assertEquals(expectedOffset, viewer().getSelectedRange().x,
				"the unified diff must eventually select the first diff");
	}

	/**
	 * Accepting a diff replaces the text only after the next paint (the manager
	 * defers the document change via a paint listener so the line header minings
	 * can be removed first). A build machine gives no guarantee the editor is ever
	 * painted on its own, so a synthetic paint is dispatched and the document polled.
	 */
	private void waitForDocumentContent(String expected) {
		assertTrue(waitForDocument(expected, 10_000),
				"the accepted change must eventually reach the document");
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
