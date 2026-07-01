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
package org.eclipse.team.tests.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.FoldedRegionCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.inlined.AbstractInlinedAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the code minings of the unified diff describe the diffs and the
 * collapsed regions shown at the time they are computed, whatever the code
 * mining framework still has attached to the viewer from an earlier request.
 */
@SuppressWarnings("restriction")
public class UnifiedDiffCodeMiningProviderTest {

	private static final UnifiedDiffMode MODE = UnifiedDiffMode.OVERLAY_READ_ONLY_MODE;
	private static final int CONTEXT_LINES = 3;
	private static final int TIMEOUT_SECONDS = 10;

	private Display display;
	private Shell shell;
	private ProjectionViewer viewer;
	private IDocument document;
	private IAnnotationModel model;
	private UnifiedDiffCodeMiningProvider provider;
	private final List<CompletableFuture<List<? extends ICodeMining>>> pendingRequests = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		display = Display.getDefault();
		assertNotNull(Display.getCurrent(), "the test drives the viewer from the UI thread");
		shell = new Shell(display);
		document = numberedLines(60);
		model = new AnnotationModel();
		viewer = new ProjectionViewer(shell, null, null, false, SWT.V_SCROLL);
		viewer.setDocument(document, model);
		viewer.enableProjection();
		DocumentUndoManagerRegistry.connect(document);
		provider = new UnifiedDiffCodeMiningProvider();
	}

	@AfterEach
	public void tearDown() {
		DocumentUndoManagerRegistry.disconnect(document);
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
		waitForPendingWork();
	}

	/**
	 * Opening a diff on a viewer that already shows one clears the shown diffs in
	 * place and puts the new ones while the minings of the old ones are still
	 * attached. The framework's requests are asynchronous and each one cancels the
	 * one before it, so the provider can be asked at any point in between and must
	 * answer from the current diffs and folds, never from what it finds attached.
	 */
	@Test
	public void testMiningsFollowTheDiffsAndFoldsThroughAReopen() throws Exception {
		installCodeMinings(provider);
		assertTrue(open(withChangedLines(10, 45)).isOK());
		List<UnifiedDiff> first = UnifiedDiffManager.get(viewer);
		assertEquals(2, first.size(), "one diff per changed line");
		waitForAttachedMinings(first, collapsedRegions().size());
		viewer.doOperation(ProjectionViewer.EXPAND_ALL);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("everything is expanded again").isEmpty();

		assertMinings(provide(), first, collapsedRegions());

		UnifiedDiffManager.foldUnchangedRegions(viewer, document, first, MODE, CONTEXT_LINES);
		waitForPendingWork();
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed").hasSize(3);
		assertMinings(provide(), first, collapsedRegions());

		// what a reopen does before the framework caught up: the old list is cleared
		// in place and a new one with the same diffs is put
		List<UnifiedDiff> second = diffsOnLines(10, 45);
		first.clear();
		UnifiedDiffManager.put(viewer, second);
		assertMinings(provide(), second, collapsedRegions());

		// a reopen showing fewer diffs than there are minings attached
		List<UnifiedDiff> third = diffsOnLines(45);
		second.clear();
		UnifiedDiffManager.put(viewer, third);
		assertMinings(provide(), third, collapsedRegions());

		// the framework caught up, then lost the mining of one diff again
		viewer.updateCodeMinings();
		waitForAttachedMinings(third, collapsedRegions().size());
		List<UnifiedDiff> fourth = diffsOnLines(10, 45);
		third.clear();
		UnifiedDiffManager.put(viewer, fourth);
		viewer.updateCodeMinings();
		waitForAttachedMinings(fourth, collapsedRegions().size());
		model.removeAnnotation(attachedMiningAnnotationOf(fourth.get(0)));
		assertMinings(provide(), fourth, collapsedRegions());
	}

	/**
	 * The case seen in the IDE: an editor with another code mining provider that
	 * answers asynchronously, such as the Java editor. Each request the reopen
	 * issues cancels the one before it, so the request that would have dropped the
	 * old minings never renders, and the framework keeps believing they are
	 * attached. The rebuilt minings must still all reach the editor.
	 */
	@Test
	public void testReopeningNextToAnAsynchronousProviderKeepsEveryMining() throws Exception {
		installCodeMinings(provider, new PendingProvider());
		String changed = withChangedLines(10, 45);

		assertTrue(open(changed).isOK());
		answerPendingRequests();
		List<UnifiedDiff> diffs = UnifiedDiffManager.get(viewer);
		assertEquals(2, diffs.size(), "one diff per changed line");
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed").hasSize(3);
		waitForAttachedMinings(diffs, collapsedRegions().size());

		assertTrue(open(changed).isOK());
		answerPendingRequests();
		diffs = UnifiedDiffManager.get(viewer);
		assertEquals(2, diffs.size(), "one diff per changed line");
		assertThat(collapsedRegions()).as("the unchanged regions are collapsed again").hasSize(3);
		waitForAttachedMinings(diffs, collapsedRegions().size());
	}

	// ------------------------------------------------------------------ helpers

	/** A provider that answers only when the test lets it, like a slow editor. */
	private final class PendingProvider implements ICodeMiningProvider {

		@Override
		public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer textViewer,
				IProgressMonitor monitor) {
			CompletableFuture<List<? extends ICodeMining>> request = new CompletableFuture<>();
			pendingRequests.add(request);
			return request;
		}

		@Override
		public void dispose() {
			// nothing to release
		}
	}

	private void installCodeMinings(ICodeMiningProvider... providers) {
		viewer.setCodeMiningProviders(providers);
		AnnotationPainter painter = new AnnotationPainter(viewer, null);
		viewer.setCodeMiningAnnotationPainter(painter);
		viewer.addPainter(painter);
	}

	private IStatus open(String source) {
		return UnifiedDiffManager.open(viewer, document, model, null, source, MODE, null, null, null, true,
				CONTEXT_LINES);
	}

	/**
	 * Lets the other provider answer every request made so far, once the fold
	 * listener had its turn and cancelled the requests it supersedes.
	 */
	private void answerPendingRequests() {
		waitForPendingWork();
		List<CompletableFuture<List<? extends ICodeMining>>> requests = new ArrayList<>(pendingRequests);
		pendingRequests.clear();
		for (CompletableFuture<List<? extends ICodeMining>> request : requests) {
			request.complete(List.of());
		}
		waitForPendingWork();
	}

	private List<ICodeMining> provide() throws Exception {
		return new ArrayList<>(
				provider.provideCodeMinings(viewer, new NullProgressMonitor()).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}

	/**
	 * One overlay mining per diff, showing that very diff, and one expander per
	 * collapsed region, anchored to its first line.
	 */
	private static void assertMinings(List<ICodeMining> minings, List<UnifiedDiff> diffs, List<Position> collapsed) {
		List<UnifiedDiff> shown = new ArrayList<>();
		List<Integer> expanders = new ArrayList<>();
		for (ICodeMining mining : minings) {
			if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay) {
				shown.add(overlay.getUnifiedDiff());
			} else if (mining instanceof FoldedRegionCodeMining expander) {
				expanders.add(Integer.valueOf(expander.getPosition().getOffset()));
			} else {
				fail("unexpected mining " + mining);
			}
		}
		assertThat(shown).as("one overlay mining per shown diff").containsExactlyInAnyOrderElementsOf(diffs);
		assertThat(expanders).as("one expander per collapsed region")
				.containsExactlyInAnyOrderElementsOf(collapsed.stream().map(p -> Integer.valueOf(p.getOffset())).toList());
	}

	/** Waits until the framework attached exactly the minings of the diffs and regions. */
	private void waitForAttachedMinings(List<UnifiedDiff> diffs, int collapsedRegions) {
		waitUntil(() -> {
			List<UnifiedDiff> shown = new ArrayList<>();
			int expanders = 0;
			for (ICodeMining mining : attachedMinings()) {
				if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay) {
					shown.add(overlay.getUnifiedDiff());
				} else if (mining instanceof FoldedRegionCodeMining) {
					expanders++;
				}
			}
			return shown.size() == diffs.size() && shown.containsAll(diffs) && diffs.containsAll(shown)
					&& expanders == collapsedRegions;
		}, () -> "the minings of " + diffs.size() + " diffs and " + collapsedRegions
				+ " collapsed regions are attached, but the model holds " + attachedMinings());
	}

	private List<ICodeMining> attachedMinings() {
		List<ICodeMining> result = new ArrayList<>();
		for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext();) {
			if (it.next() instanceof AbstractInlinedAnnotation inlined) {
				result.addAll(inlined.getMinings());
			}
		}
		return result;
	}

	private Annotation attachedMiningAnnotationOf(UnifiedDiff diff) {
		for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = it.next();
			if (annotation instanceof AbstractInlinedAnnotation inlined) {
				for (ICodeMining mining : inlined.getMinings()) {
					if (mining instanceof UnifiedDiffLineHeaderCodeMining overlay && overlay.getUnifiedDiff() == diff) {
						return annotation;
					}
				}
			}
		}
		return fail("no mining is attached for the diff on line " + diff.leftStart);
	}

	private List<Position> collapsedRegions() {
		List<Position> result = new ArrayList<>();
		ProjectionAnnotationModel projectionModel = viewer.getProjectionAnnotationModel();
		for (Iterator<Annotation> it = projectionModel.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = it.next();
			if (annotation instanceof ProjectionAnnotation fold && fold.isCollapsed()) {
				result.add(projectionModel.getPosition(annotation));
			}
		}
		return result;
	}

	private void waitUntil(BooleanSupplier condition, Supplier<String> what) {
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) {
				fail("timed out waiting until " + what.get());
			}
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					fail("interrupted");
				}
			}
		}
	}

	private void waitForPendingWork() {
		while (display.readAndDispatch()) {
			// keep going until the queue is empty
		}
	}

	/** The document with the given lines changed, as the other side of the diff. */
	private String withChangedLines(int... lines) {
		String content = document.get();
		for (int line : lines) {
			content = content.replace("line " + line + "\n", "line " + line + " changed\n");
		}
		return content;
	}

	/** The diffs the manager would record for the given changed lines. */
	private List<UnifiedDiff> diffsOnLines(int... lines) throws BadLocationException {
		List<UnifiedDiff> diffs = new ArrayList<>();
		for (int line : lines) {
			int offset = document.getLineOffset(line);
			int length = document.getLineLength(line);
			diffs.add(new UnifiedDiff(document, offset, offset + length, document.get(offset, length), document,
					offset, offset + length + 8, "line " + line + " changed\n", diffs, MODE));
		}
		return diffs;
	}

	private static IDocument numberedLines(int count) {
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < count; i++) {
			content.append("line ").append(i).append('\n');
		}
		return new Document(content.toString());
	}
}
