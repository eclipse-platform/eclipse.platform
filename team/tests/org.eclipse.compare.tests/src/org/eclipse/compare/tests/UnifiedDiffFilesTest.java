/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.eclipse.compare.tests.UnifiedDiffOpenTest.activePage;
import static org.eclipse.compare.tests.UnifiedDiffOpenTest.hasUnifiedDiffAnnotation;
import static org.eclipse.compare.tests.UnifiedDiffOpenTest.processQueuedEvents;
import static org.eclipse.compare.tests.UnifiedDiffOpenTest.pumpUntil;
import static org.eclipse.compare.tests.UnifiedDiffOpenTest.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.UnifiedDiffFiles;
import org.eclipse.compare.internal.UnifiedDiffFiles.ChangedFile;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.tests.UnifiedDiffOpenTest.InMemoryElement;
import org.eclipse.compare.tests.UnifiedDiffOpenTest.RevisionElement;
import org.eclipse.compare.tests.UnifiedDiffOpenTest.WorkspaceFileElement;
import org.eclipse.compare.unifieddiff.internal.HideAllDiffsRunnable;
import org.eclipse.compare.unifieddiff.internal.NextRunnable;
import org.eclipse.compare.unifieddiff.internal.PreviousRunnable;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the unified diff of a comparison of folders, which shows the changed
 * files one at a time in a single editor.
 */
public class UnifiedDiffFilesTest {

	private static final String OLD = "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n"; //$NON-NLS-1$
	private static final String NEW = "one\nTWO\nthree\nfour\nfive\nsix\nseven\neight\nNINE\nten\n"; //$NON-NLS-1$
	/** The zero-based lines of the two changes between {@link #OLD} and {@link #NEW}. */
	private static final int FIRST_CHANGED_LINE = 1;
	private static final int LAST_CHANGED_LINE = 8;

	private boolean originalUnifiedDiff;
	private String originalEndAction;
	private IProject project;

	/** A folder of the comparison tree. */
	private static final class FolderElement implements ITypedElement {

		private final String name;

		FolderElement(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getType() {
			return FOLDER_TYPE;
		}

		@Override
		public Image getImage() {
			return null;
		}
	}

	/**
	 * A revision that, like the ones of a version control system, only has a
	 * document key once its contents were fetched.
	 */
	private static final class FetchedRevisionElement implements ITypedElement, IEncodedStreamContentAccessor, IAdaptable {

		private final RevisionElement revision;
		private volatile boolean fetched;

		FetchedRevisionElement(String name, String content) {
			this.revision = new RevisionElement(name, content);
		}

		@Override
		public InputStream getContents() {
			fetched = true;
			return revision.getContents();
		}

		@Override
		public String getCharset() {
			return revision.getCharset();
		}

		@Override
		public String getName() {
			return revision.getName();
		}

		@Override
		public String getType() {
			return revision.getType();
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == ISharedDocumentAdapter.class) {
				ISharedDocumentAdapter shared = revision.getAdapter(ISharedDocumentAdapter.class);
				return adapter.cast(new SharedDocumentAdapter() {
					@Override
					public IEditorInput getDocumentKey(Object element) {
						return fetched && element == FetchedRevisionElement.this ? shared.getDocumentKey(revision) : null;
					}

					@Override
					public void flushDocument(IDocumentProvider provider, IEditorInput documentKey, IDocument document,
							boolean overwrite) {
						// a revision is read-only
					}
				});
			}
			return null;
		}
	}

	/** Compares two folders, handing out a prepared tree of changed files. */
	private static final class FolderCompareInput extends CompareEditorInput {

		private final DiffNode root;

		FolderCompareInput(DiffNode root) {
			super(new CompareConfiguration());
			this.root = root;
			setTitle("Unified diff of folders"); //$NON-NLS-1$
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) {
			return root;
		}

		@Override
		public boolean canRunAsJob() {
			return true;
		}
	}

	@BeforeEach
	public void setUp() throws CoreException {
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display"); //$NON-NLS-1$
		originalUnifiedDiff = store().getBoolean(ComparePreferencePage.UNIFIED_DIFF);
		originalEndAction = store().getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, true);
		store().setValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, ICompareUIConstants.PREF_VALUE_NEXT);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("UnifiedDiffFilesTest"); //$NON-NLS-1$
		if (!project.exists()) {
			project.create(null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}
	}

	@AfterEach
	public void tearDown() throws CoreException {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, originalUnifiedDiff);
		store().setValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, originalEndAction);
		if (activePage() != null) {
			activePage().closeAllEditors(false);
		}
		processQueuedEvents();
		if (project != null && project.exists()) {
			project.delete(true, null);
		}
	}

	@Test
	public void testFolderComparisonOpensTheFirstChangedFile() throws Exception {
		FolderCompareInput input = openThreeFiles();

		ITextEditor editor = assertShowing("a.txt"); //$NON-NLS-1$
		UnifiedDiffFiles files = filesOf(editor);
		assertEquals(List.of("a.txt", "c.txt", "dir/b.txt"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				files.getFiles().stream().map(ChangedFile::path).toList(),
				"the changed files must be listed by path"); //$NON-NLS-1$
		assertEquals(0, files.getCurrent());
		assertEquals(1, activePage().getEditorReferences().length, "only one editor may open"); //$NON-NLS-1$
		assertTrue(CompareUIPlugin.canShowAsUnifiedDiff(input), "a folder comparison must qualify"); //$NON-NLS-1$
	}

	@Test
	public void testNextAtTheLastDiffMovesToTheNextFile() throws Exception {
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$

		runNext(first, true);

		ITextEditor second = assertShowing("c.txt"); //$NON-NLS-1$
		assertEquals(1, filesOf(second).getCurrent());
		pumpUntil(() -> activePage().getEditorReferences().length == 1,
				"the editor of the previous file must close"); //$NON-NLS-1$
		assertLineOfCaret(second, FIRST_CHANGED_LINE, "the first diff of the next file must be revealed"); //$NON-NLS-1$
	}

	@Test
	public void testPreviousAtTheFirstDiffRevealsTheLastDiffOfThePreviousFile() throws Exception {
		openThreeFiles();
		runNext(assertShowing("a.txt"), true); //$NON-NLS-1$
		ITextEditor second = assertShowing("c.txt"); //$NON-NLS-1$

		runNext(second, false);

		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$
		assertLineOfCaret(first, LAST_CHANGED_LINE, "going back must reveal the last diff of the file"); //$NON-NLS-1$
	}

	@Test
	public void testLoopPreferenceWrapsWithinTheFile() throws Exception {
		store().setValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, ICompareUIConstants.PREF_VALUE_LOOP);
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$

		runNext(first, true);
		processQueuedEvents();

		assertSame(first, activePage().getActiveEditor(), "looping must stay in the file"); //$NON-NLS-1$
		assertEquals(0, filesOf(first).getCurrent());
		assertLineOfCaret(first, FIRST_CHANGED_LINE, "looping must wrap to the first diff"); //$NON-NLS-1$
	}

	@Test
	public void testDoNothingPreferenceStaysAtTheLastDiff() throws Exception {
		store().setValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, ICompareUIConstants.PREF_VALUE_DO_NOTHING);
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$
		ITextViewer viewer = first.getAdapter(ITextViewer.class);

		assertTrue(UnifiedDiffManager.getFileNavigator(viewer).endReached(true),
				"doing nothing must still keep the navigation from wrapping"); //$NON-NLS-1$
		processQueuedEvents();

		assertSame(first, activePage().getActiveEditor(), "doing nothing must stay in the file"); //$NON-NLS-1$
	}

	@Test
	public void testTheLastFileWrapsWithinItself() throws Exception {
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$
		filesOf(first).showFile(2);
		ITextEditor last = assertShowing("b.txt"); //$NON-NLS-1$

		assertFalse(UnifiedDiffManager.getFileNavigator(last.getAdapter(ITextViewer.class)).endReached(true),
				"with no file left the navigation must wrap within the last one"); //$NON-NLS-1$
	}

	@Test
	public void testFinishingAllDiffsMovesToTheNextFile() throws Exception {
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$

		new HideAllDiffsRunnable(first).run();

		assertShowing("c.txt"); //$NON-NLS-1$
	}

	@Test
	public void testFileSelectorJumpsToTheChosenFile() throws Exception {
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$

		filesOf(first).showFile(2);

		ITextEditor chosen = assertShowing("b.txt"); //$NON-NLS-1$
		assertEquals(2, filesOf(chosen).getCurrent());
		pumpUntil(() -> activePage().getEditorReferences().length == 1,
				"the editor of the previous file must close"); //$NON-NLS-1$
	}

	/** An editor the user had open is not the unified diff's to close; it only loses the diff. */
	@Test
	public void testAnEditorOpenBeforeKeepsOpenWithoutTheDiff() throws Exception {
		IFile a = createFile("a.txt", NEW); //$NON-NLS-1$
		IEditorPart preOpened = IDE.openEditor(activePage(), a);
		openThreeFiles();
		ITextEditor first = assertShowing("a.txt"); //$NON-NLS-1$
		assertSame(preOpened, first, "the unified diff must reuse the open editor"); //$NON-NLS-1$

		runNext(first, true);
		assertShowing("c.txt"); //$NON-NLS-1$

		assertSame(preOpened, activePage().findEditor(new FileEditorInput(a)), "the editor must stay open"); //$NON-NLS-1$
		IAnnotationModel model = UnifiedDiffManager.annotationModelOf(first);
		pumpUntil(() -> !hasUnifiedDiffAnnotation(model), "the diff must be taken off the file left behind"); //$NON-NLS-1$
	}

	@Test
	public void testBinaryFilesAreLeftOut() throws Exception {
		DiffNode root = new DiffNode(Differencer.CHANGE);
		addChangedFile(root, "a.txt"); //$NON-NLS-1$
		IFile image = project.getFile("image.bin"); //$NON-NLS-1$
		image.create(new ByteArrayInputStream(new byte[] { 'P', 'N', 'G', 0, 1, 2 }), true, null);
		new DiffNode(root, Differencer.CHANGE, null, new WorkspaceFileElement(image),
				new InMemoryElement("image.bin", "PNG")); //$NON-NLS-1$ //$NON-NLS-2$
		open(root);

		ITextEditor editor = assertShowing("a.txt"); //$NON-NLS-1$
		assertEquals(List.of("a.txt"), filesOf(editor).getFiles().stream().map(ChangedFile::path).toList(), //$NON-NLS-1$
				"a binary file must not be shown as text"); //$NON-NLS-1$
	}

	@Test
	public void testFolderComparisonWithoutQualifyingFilesOpensTheCompareEditor() throws Exception {
		DiffNode root = new DiffNode(Differencer.CHANGE);
		new DiffNode(root, Differencer.CHANGE, null, new InMemoryElement("a.txt", NEW), //$NON-NLS-1$
				new InMemoryElement("a.txt", OLD)); //$NON-NLS-1$
		FolderCompareInput input = open(root);

		assertInstanceOf(CompareEditor.class, activePage().getActiveEditor(),
				"a folder comparison without a workspace file must fall back to the compare editor"); //$NON-NLS-1$
		assertFalse(CompareUIPlugin.canShowAsUnifiedDiff(input));
	}

	/** Comparing two commits shows each changed file read-only on its revision. */
	@Test
	public void testCommitComparisonShowsTheRevisionsOfTheChangedFiles() {
		DiffNode root = new DiffNode(Differencer.CHANGE);
		for (String name : List.of("a.txt", "b.txt")) { //$NON-NLS-1$ //$NON-NLS-2$
			new DiffNode(root, Differencer.CHANGE, null, new FetchedRevisionElement(name, NEW),
					new FetchedRevisionElement(name, OLD));
		}
		open(root);

		pumpUntil(() -> activePage().getActiveEditor() instanceof ITextEditor editor
				&& editor.getEditorInput() instanceof IStorageEditorInput
				&& "a.txt".equals(editor.getEditorInput().getName()) //$NON-NLS-1$
				&& UnifiedDiffManager.getFileNavigator(editor.getAdapter(ITextViewer.class)) != null,
				"the unified diff of the first revision did not show"); //$NON-NLS-1$
		ITextEditor editor = (ITextEditor) activePage().getActiveEditor();
		assertEquals(List.of("a.txt", "b.txt"), //$NON-NLS-1$ //$NON-NLS-2$
				filesOf(editor).getFiles().stream().map(ChangedFile::path).toList());
	}

	/** Opens a comparison of a.txt, c.txt and dir/b.txt, deliberately out of order. */
	private FolderCompareInput openThreeFiles() throws CoreException {
		DiffNode root = new DiffNode(Differencer.CHANGE);
		addChangedFile(root, "c.txt"); //$NON-NLS-1$
		DiffNode dir = new DiffNode(root, Differencer.CHANGE, null, new FolderElement("dir"), //$NON-NLS-1$
				new FolderElement("dir")); //$NON-NLS-1$
		IFolder folder = project.getFolder("dir"); //$NON-NLS-1$
		if (!folder.exists()) {
			folder.create(true, true, null);
		}
		addChangedFile(dir, "dir/b.txt"); //$NON-NLS-1$
		addChangedFile(root, "a.txt"); //$NON-NLS-1$
		return open(root);
	}

	private void addChangedFile(IDiffContainer parent, String path) throws CoreException {
		IFile file = createFile(path, NEW);
		new DiffNode(parent, Differencer.CHANGE, null, new WorkspaceFileElement(file),
				new InMemoryElement(file.getName(), OLD));
	}

	private static FolderCompareInput open(DiffNode root) {
		FolderCompareInput input = new FolderCompareInput(root);
		CompareUI.openCompareEditor(input);
		pumpUntil(() -> activePage().getActiveEditor() != null, "no editor opened"); //$NON-NLS-1$
		return input;
	}

	/** Runs Next (or Previous) with the caret past the last (or before the first) diff. */
	private static void runNext(ITextEditor editor, boolean next) {
		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		IAnnotationModel model = UnifiedDiffManager.annotationModelOf(editor);
		viewer.setSelectedRange(next ? viewer.getDocument().getLength() : 0, 0);
		if (next) {
			new NextRunnable(viewer, model, new ToolBarManager()).run();
		} else {
			new PreviousRunnable(viewer, model, new ToolBarManager()).run();
		}
	}

	/** Waits for the active editor to show the unified diff of the given file. */
	private static ITextEditor assertShowing(String fileName) {
		pumpUntil(() -> activePage().getActiveEditor() instanceof ITextEditor editor
				&& editor.getEditorInput() instanceof IFileEditorInput fileInput
				&& fileInput.getFile().getName().equals(fileName)
				&& UnifiedDiffManager.getFileNavigator(editor.getAdapter(ITextViewer.class)) != null
				&& hasUnifiedDiffAnnotation(UnifiedDiffManager.annotationModelOf(editor)),
				"the unified diff of " + fileName + " did not show"); //$NON-NLS-1$ //$NON-NLS-2$
		return (ITextEditor) activePage().getActiveEditor();
	}

	private static UnifiedDiffFiles filesOf(ITextEditor editor) {
		return assertInstanceOf(UnifiedDiffFiles.class,
				UnifiedDiffManager.getFileNavigator(editor.getAdapter(ITextViewer.class)));
	}

	private static void assertLineOfCaret(ITextEditor editor, int line, String message) {
		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		pumpUntil(() -> lineOfCaret(viewer) == line, message);
	}

	private static int lineOfCaret(ITextViewer viewer) {
		try {
			return viewer.getDocument().getLineOfOffset(viewer.getSelectedRange().x);
		} catch (BadLocationException e) {
			return -1;
		}
	}

	private IFile createFile(String path, String content) throws CoreException {
		IFile file = project.getFile(path);
		ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		if (file.exists()) {
			file.setContents(source, true, false, null);
		} else {
			file.create(source, true, null);
		}
		return file;
	}
}
