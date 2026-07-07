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
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how often the compare editor open path re-reads element content and
 * runs {@code prepareInput}, and on which thread. The asserted bounds are the
 * current baseline and tighten as the open path is optimized.
 */
public class CompareOpenEfficiencyTest {

	/**
	 * Upper bound for the {@code getContents()} calls per side during a single
	 * compare editor open: one read for content-type detection, one for the text
	 * heuristic, and one for the document shown in the viewer. Asserted as a bound
	 * because the exact count can vary by platform.
	 */
	private static final int MAX_GET_CONTENTS_PER_SIDE = 3;

	private static final long TIMEOUT_MILLIS = 30_000;

	private boolean originalUnifiedDiff;

	/** A text element that counts every {@link #getContents()} call. */
	private static final class CountingElement
			implements ITypedElement, IEncodedStreamContentAccessor {

		private final String name;
		private final byte[] bytes;
		private final AtomicInteger contentReads = new AtomicInteger();

		CountingElement(String name, String content) {
			this.name = name;
			this.bytes = content.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public InputStream getContents() {
			contentReads.incrementAndGet();
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

		int reads() {
			return contentReads.get();
		}
	}

	/**
	 * A compare input that counts {@code prepareInput} invocations and records
	 * whether they ran on the UI thread.
	 */
	private static final class CountingCompareEditorInput extends CompareEditorInput {

		private final ITypedElement left;
		private final ITypedElement right;
		private final boolean runAsJob;
		private final AtomicInteger prepareInputCount = new AtomicInteger();
		private volatile boolean anyRunOnUiThread;

		CountingCompareEditorInput(boolean runAsJob, ITypedElement left, ITypedElement right) {
			super(new CompareConfiguration());
			this.runAsJob = runAsJob;
			this.left = left;
			this.right = right;
			setTitle("Counting compare"); //$NON-NLS-1$
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) {
			prepareInputCount.incrementAndGet();
			if (Display.getCurrent() != null) {
				anyRunOnUiThread = true;
			}
			return new DiffNode(left, right);
		}

		@Override
		public boolean canRunAsJob() {
			return runAsJob;
		}

		int prepareInputCalls() {
			return prepareInputCount.get();
		}

		boolean ranOnUiThread() {
			return anyRunOnUiThread;
		}
	}

	@BeforeEach
	public void setUp() {
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display"); //$NON-NLS-1$
		originalUnifiedDiff = store().getBoolean(ComparePreferencePage.UNIFIED_DIFF);
	}

	@AfterEach
	public void tearDown() {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, originalUnifiedDiff);
		IWorkbenchPage page = activePage();
		if (page != null) {
			page.closeAllEditors(false);
		}
		processQueuedEvents();
	}

	@Test
	public void testGetContentsCallCountPerSide() throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
		CountingElement leftElement = new CountingElement("left.txt", //$NON-NLS-1$
				"alpha\nbravo\ncharlie\ndelta\n"); //$NON-NLS-1$
		CountingElement rightElement = new CountingElement("right.txt", //$NON-NLS-1$
				"alpha\nBRAVO\ncharlie\nDELTA\n"); //$NON-NLS-1$
		CountingCompareEditorInput input = new CountingCompareEditorInput(true, leftElement, rightElement);

		CompareUI.openCompareEditor(input);
		pumpUntil(() -> {
			IMergeViewerTestAdapter adapter = mergeViewerAdapter(input);
			return adapter != null && adapter.getChangesCount() > 0;
		}, "merge viewer did not report a diff"); //$NON-NLS-1$

		System.out.println("PERF-EFFICIENCY getContents left=" + leftElement.reads() //$NON-NLS-1$
				+ " right=" + rightElement.reads()); //$NON-NLS-1$

		assertReadsWithinBound("left", leftElement.reads()); //$NON-NLS-1$
		assertReadsWithinBound("right", rightElement.reads()); //$NON-NLS-1$
	}

	private static void assertReadsWithinBound(String side, int reads) {
		assertTrue(reads >= 1, "expected at least one getContents() call on the " + side + " side"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(reads <= MAX_GET_CONTENTS_PER_SIDE,
				"getContents() called " + reads + " times on the " + side //$NON-NLS-1$ //$NON-NLS-2$
						+ " side, exceeding the known worst case of " + MAX_GET_CONTENTS_PER_SIDE); //$NON-NLS-1$
	}

	@Test
	public void testPrepareInputRunsOnceUnifiedOff() throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
		CountingCompareEditorInput input = openAndWait(true);
		assertEquals(1, input.prepareInputCalls(),
				"prepareInput must run exactly once per open (unified diff off)"); //$NON-NLS-1$
	}

	@Test
	public void testPrepareInputRunsOnceUnifiedOn() throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, true);
		CountingCompareEditorInput input = openAndWait(true);
		// The unified diff path runs the input once in canShowInUnifiedDiff and the
		// classic fallback reuses the cached result, so the total stays at one.
		assertEquals(1, input.prepareInputCalls(),
				"prepareInput invocation count with unified diff on (documents current behavior)"); //$NON-NLS-1$
	}

	@Test
	public void testPrepareInputOffUiThreadUnifiedOff() throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
		CountingCompareEditorInput input = openAndWait(true);
		assertTrue(input.prepareInputCalls() >= 1, "prepareInput did not run"); //$NON-NLS-1$
		assertEquals(false, input.ranOnUiThread(),
				"prepareInput must run off the UI thread when the input can run as a job"); //$NON-NLS-1$
	}

	// Documents current behavior: the unified diff path prepares the input on the
	// UI thread. This assertion flips once the preparation moves to a background job.
	@Test
	public void testPrepareInputOnUiThreadUnifiedOnDocumentsCurrentBehavior() throws Exception {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, true);
		CountingCompareEditorInput input = openAndWait(true);
		assertTrue(input.prepareInputCalls() >= 1, "prepareInput did not run"); //$NON-NLS-1$
		assertEquals(true, input.ranOnUiThread(),
				"documents current behavior: unified diff runs prepareInput on the UI thread (Phase 1 item 3)"); //$NON-NLS-1$
	}

	private CountingCompareEditorInput openAndWait(boolean runAsJob) throws Exception {
		CountingCompareEditorInput input = new CountingCompareEditorInput(runAsJob,
				new CountingElement("left.txt", "alpha\nbravo\ncharlie\n"), //$NON-NLS-1$ //$NON-NLS-2$
				new CountingElement("right.txt", "alpha\nBRAVO\ncharlie\n")); //$NON-NLS-1$ //$NON-NLS-2$
		CompareUI.openCompareEditor(input);
		pumpUntil(() -> input.getCompareResult() != null && contentPane(input) != null,
				"compare editor did not finish opening"); //$NON-NLS-1$
		return input;
	}

	private static IMergeViewerTestAdapter mergeViewerAdapter(CompareEditorInput input) {
		CompareViewerSwitchingPane pane = contentPane(input);
		if (pane == null) {
			return null;
		}
		Viewer viewer = pane.getViewer();
		if (viewer == null) {
			return null;
		}
		return Adapters.adapt(viewer, IMergeViewerTestAdapter.class);
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
		// A self-rescheduling timer keeps the loop waking so the deadline is
		// enforced even while blocked in Display.sleep().
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
