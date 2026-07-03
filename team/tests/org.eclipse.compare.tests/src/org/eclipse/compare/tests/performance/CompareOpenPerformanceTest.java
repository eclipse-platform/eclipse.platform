/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests.performance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.tests.ReflectionUtils;
import org.eclipse.compare.unifieddiff.UnifiedDiff;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * Timing measurements for the compare editor open path. These are not correctness
 * tests: each scenario is repeated, the first repetitions are discarded as warmup
 * and the median is printed as a stable greppable {@code PERF ...} line plus a CSV
 * under the bundle {@code target/} directory. Only a generous sanity bound is
 * asserted, so unstable shared-CI timings do not produce spurious failures.
 * <p>
 * This class is intentionally not part of {@code AllCompareTests}; it is wired into
 * {@link PerformanceTestSuite} instead.
 */
public class CompareOpenPerformanceTest {

	private static final int REPETITIONS = 20;
	private static final int WARMUP = 5;
	private static final long TIMEOUT_MILLIS = 60_000;
	private static final long SANITY_BOUND_MILLIS = 10_000;

	private static final String UNIFIED_DIFF_ANNOTATION_PREFIX = "org.eclipse.compare.unifieddiff"; //$NON-NLS-1$

	private enum Scenario {
		CLASSIC_5K_SPARSE("compare-open", 5000, 100), //$NON-NLS-1$
		CLASSIC_5K_DENSE("compare-open", 5000, 1250), //$NON-NLS-1$
		CLASSIC_50K_SPARSE("compare-open", 50000, 1000), //$NON-NLS-1$
		UNIFIED_5K_SPARSE("compare-open-unified", 5000, 100), //$NON-NLS-1$
		DOC_MERGER_5K_DENSE("document-merger", 5000, 1250); //$NON-NLS-1$

		final String label;
		final int lines;
		final int changes;
		final List<long[]> samples = new ArrayList<>();

		Scenario(String label, int lines, int changes) {
			this.label = label;
			this.lines = lines;
			this.changes = changes;
		}
	}

	private boolean originalUnifiedDiff;
	private static IProject unifiedProject;

	@BeforeEach
	public void setUp() {
		originalUnifiedDiff = store().getBoolean(ComparePreferencePage.UNIFIED_DIFF);
	}

	@AfterEach
	public void tearDown() {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, originalUnifiedDiff);
		closeAllEditors();
	}

	@RepeatedTest(REPETITIONS)
	public void compareOpen5kSparse(RepetitionInfo info) {
		measureCompareOpen(Scenario.CLASSIC_5K_SPARSE, info);
	}

	@RepeatedTest(REPETITIONS)
	public void compareOpen5kDense(RepetitionInfo info) {
		measureCompareOpen(Scenario.CLASSIC_5K_DENSE, info);
	}

	@RepeatedTest(REPETITIONS)
	public void compareOpen50kSparse(RepetitionInfo info) {
		measureCompareOpen(Scenario.CLASSIC_50K_SPARSE, info);
	}

	@RepeatedTest(REPETITIONS)
	public void compareOpenUnified5kSparse(RepetitionInfo info) throws Exception {
		measureUnifiedOpen(Scenario.UNIFIED_5K_SPARSE, info);
	}

	@RepeatedTest(REPETITIONS)
	public void documentMergerDoDiff5kDense(RepetitionInfo info) throws Exception {
		measureDocumentMerger(Scenario.DOC_MERGER_5K_DENSE, info);
	}

	private void measureCompareOpen(Scenario scenario, RepetitionInfo info) {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
		String[] content = generate(scenario.lines, scenario.changes);
		TextCompareEditorInput input = new TextCompareEditorInput(content[0], content[1]);

		long start = System.nanoTime();
		long[] textReadyAt = { -1 };
		CompareUI.openCompareEditor(input);
		pumpUntil(() -> {
			CompareViewerSwitchingPane pane = contentPane(input);
			Viewer viewer = pane == null ? null : pane.getViewer();
			if (viewer != null && textReadyAt[0] < 0) {
				textReadyAt[0] = System.nanoTime();
			}
			IMergeViewerTestAdapter adapter = viewer == null ? null
					: Adapters.adapt(viewer, IMergeViewerTestAdapter.class);
			return adapter != null && adapter.getChangesCount() > 0;
		}, "compare editor did not report a diff"); //$NON-NLS-1$
		long diffReadyAt = System.nanoTime();

		record(scenario, info, millis(start, textReadyAt[0]), millis(start, diffReadyAt));
		closeAllEditors();
	}

	private void measureUnifiedOpen(Scenario scenario, RepetitionInfo info) throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, true);
		String[] content = generate(scenario.lines, scenario.changes);
		IFile file = unifiedFile(content[0]);

		IWorkbenchPage page = activePage();
		long start = System.nanoTime();
		ITextEditor editor = (ITextEditor) IDE.openEditor(page, file);
		pumpUntil(() -> editor.getAdapter(ITextViewer.class) != null,
				"text editor did not open"); //$NON-NLS-1$
		long textReadyAt = System.nanoTime();

		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		assertNotNull(model, "annotation model must not be null"); //$NON-NLS-1$
		UnifiedDiff.create(editor, content[1], UnifiedDiffMode.REVERT_MODE).open();
		pumpUntil(() -> hasUnifiedDiffAnnotation(model), "unified diff annotations did not appear"); //$NON-NLS-1$
		long diffReadyAt = System.nanoTime();

		record(scenario, info, millis(start, textReadyAt), millis(start, diffReadyAt));
		page.closeAllEditors(false);
		processQueuedEvents();
	}

	private void measureDocumentMerger(Scenario scenario, RepetitionInfo info) throws Exception {
		String[] content = generate(scenario.lines, scenario.changes);
		IDocument left = new Document(content[0]);
		IDocument right = new Document(content[1]);
		DocumentMerger merger = new DocumentMerger(headlessInput(left, right));

		long start = System.nanoTime();
		merger.doDiff();
		long doneAt = System.nanoTime();

		if (info.getCurrentRepetition() > WARMUP) {
			scenario.samples.add(new long[] { millis(start, doneAt), millis(start, doneAt) });
		}
		assertTrue(millis(start, doneAt) < SANITY_BOUND_MILLIS,
				"DocumentMerger.doDiff() exceeded the sanity bound"); //$NON-NLS-1$
		assertTrue(merger.changesCount() > 0, "expected at least one change"); //$NON-NLS-1$
	}

	private void record(Scenario scenario, RepetitionInfo info, long timeToText, long timeToDiff) {
		if (info.getCurrentRepetition() > WARMUP) {
			scenario.samples.add(new long[] { timeToText, timeToDiff });
		}
		assertTrue(timeToDiff < SANITY_BOUND_MILLIS,
				scenario.name() + " time to diff exceeded the sanity bound: " + timeToDiff + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@AfterAll
	public static void reportResults() throws IOException {
		StringBuilder csv = new StringBuilder("label,lines,changes,timeToTextMedianMs,timeToDiffMedianMs,n\n"); //$NON-NLS-1$
		for (Scenario scenario : Scenario.values()) {
			if (scenario.samples.isEmpty()) {
				continue;
			}
			long textMedian = median(scenario.samples, 0);
			long diffMedian = median(scenario.samples, 1);
			int n = scenario.samples.size();
			if (scenario.label.equals("document-merger")) { //$NON-NLS-1$
				System.out.println("PERF document-merger lines=" + scenario.lines + " changes=" + scenario.changes //$NON-NLS-1$ //$NON-NLS-2$
						+ " doDiff=" + diffMedian + "ms (median, n=" + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else {
				System.out.println("PERF " + scenario.label + " lines=" + scenario.lines + " changes=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ scenario.changes + " timeToText=" + textMedian + "ms timeToDiff=" + diffMedian //$NON-NLS-1$ //$NON-NLS-2$
						+ "ms (median, n=" + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			csv.append(scenario.label).append(',').append(scenario.lines).append(',').append(scenario.changes)
					.append(',').append(textMedian).append(',').append(diffMedian).append(',').append(n)
					.append('\n');
		}
		writeCsv(csv.toString());
		if (unifiedProject != null && unifiedProject.exists()) {
			try {
				unifiedProject.delete(true, null);
			} catch (CoreException e) {
				// ignore cleanup failure
			}
		}
	}

	private static void writeCsv(String content) throws IOException {
		File targetDir = new File("target"); //$NON-NLS-1$
		if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
			targetDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		}
		File csvFile = new File(targetDir, "compare-open-performance.csv"); //$NON-NLS-1$
		Files.writeString(csvFile.toPath(), content);
		System.out.println("PERF csv written to " + csvFile.getAbsolutePath()); //$NON-NLS-1$
	}

	// Synthetic content generator modeled on RangeDifferencerTest.createDocument.
	// The right side modifies roughly "changes" evenly spaced lines of the left side.
	private static String[] generate(int lines, int changes) {
		int interval = changes > 0 ? Math.max(1, lines / changes) : lines + 1;
		StringBuilder left = new StringBuilder(lines * 10);
		StringBuilder right = new StringBuilder(lines * 12);
		for (int i = 0; i < lines; i++) {
			String base = "line " + i; //$NON-NLS-1$
			left.append(base).append('\n');
			if (i > 0 && i % interval == 0) {
				right.append(base).append(" CHANGED").append('\n'); //$NON-NLS-1$
			} else {
				right.append(base).append('\n');
			}
		}
		return new String[] { left.toString(), right.toString() };
	}

	private IFile unifiedFile(String content) throws CoreException {
		if (unifiedProject == null) {
			unifiedProject = ResourcesPlugin.getWorkspace().getRoot().getProject("CompareOpenPerformance"); //$NON-NLS-1$
		}
		if (!unifiedProject.exists()) {
			unifiedProject.create(null);
		}
		if (!unifiedProject.isOpen()) {
			unifiedProject.open(null);
		}
		IFile file = unifiedProject.getFile("unified.txt"); //$NON-NLS-1$
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		if (file.exists()) {
			file.setContents(new ByteArrayInputStream(bytes), true, false, null);
		} else {
			file.create(new ByteArrayInputStream(bytes), true, null);
		}
		return file;
	}

	private static boolean hasUnifiedDiffAnnotation(IAnnotationModel model) {
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation annotation = it.next();
			String type = annotation.getType();
			if (type != null && type.startsWith(UNIFIED_DIFF_ANNOTATION_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	private static IDocumentMergerInput headlessInput(IDocument left, IDocument right) {
		return new IDocumentMergerInput() {
			@Override
			public IDocument getDocument(char contributor) {
				return contributor == 'L' ? left : contributor == 'R' ? right : null;
			}

			@Override
			public Position getRegion(char contributor) {
				return null;
			}

			@Override
			public boolean isIgnoreAncestor() {
				return true;
			}

			@Override
			public boolean isThreeWay() {
				return false;
			}

			@Override
			public CompareConfiguration getCompareConfiguration() {
				return new CompareConfiguration();
			}

			@Override
			public ITokenComparator createTokenComparator(String line) {
				return new TokenComparator(line);
			}

			@Override
			public Optional<IIgnoreWhitespaceContributor> createIgnoreWhitespaceContributor(IDocument document) {
				return Optional.empty();
			}

			@Override
			public boolean isHunkOnLeft() {
				return false;
			}

			@Override
			public int getHunkStart() {
				return 0;
			}

			@Override
			public boolean isPatchHunk() {
				return false;
			}

			@Override
			public boolean isShowPseudoConflicts() {
				return false;
			}

			@Override
			public boolean isPatchHunkOk() {
				return false;
			}
		};
	}

	private static long median(List<long[]> samples, int index) {
		List<Long> values = new ArrayList<>(samples.size());
		for (long[] sample : samples) {
			values.add(sample[index]);
		}
		Collections.sort(values);
		int n = values.size();
		if (n == 0) {
			return -1;
		}
		if (n % 2 == 1) {
			return values.get(n / 2);
		}
		return (values.get(n / 2 - 1) + values.get(n / 2)) / 2;
	}

	private static long millis(long startNanos, long endNanos) {
		if (endNanos < 0) {
			return -1;
		}
		return (endNanos - startNanos) / 1_000_000;
	}

	private static class TextElement implements ITypedElement, IEncodedStreamContentAccessor {
		private final String name;
		private final byte[] bytes;

		TextElement(String name, String content) {
			this.name = name;
			this.bytes = content.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public String getCharset() {
			return "UTF-8"; //$NON-NLS-1$
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getType() {
			return TEXT_TYPE;
		}

		@Override
		public Image getImage() {
			return null;
		}
	}

	private static class TextCompareEditorInput extends CompareEditorInput {
		private final ITypedElement left;
		private final ITypedElement right;

		TextCompareEditorInput(String leftContent, String rightContent) {
			super(new CompareConfiguration());
			this.left = new TextElement("left.txt", leftContent); //$NON-NLS-1$
			this.right = new TextElement("right.txt", rightContent); //$NON-NLS-1$
			setTitle("Performance compare"); //$NON-NLS-1$
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) {
			return new DiffNode(left, right);
		}

		@Override
		public boolean canRunAsJob() {
			return true;
		}
	}

	private static CompareViewerSwitchingPane contentPane(CompareEditorInput input) {
		try {
			return (CompareViewerSwitchingPane) ReflectionUtils.getField(input, "fContentInputPane", true); //$NON-NLS-1$
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
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

	private void closeAllEditors() {
		IWorkbenchPage page = activePage();
		if (page != null) {
			page.closeAllEditors(false);
		}
		processQueuedEvents();
	}

	private void processQueuedEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// drain the event queue
		}
	}

	private static IWorkbenchPage activePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static IPreferenceStore store() {
		return CompareUIPlugin.getDefault().getPreferenceStore();
	}
}
