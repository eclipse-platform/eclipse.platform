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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * UI performance test for the Compare Editor: opens the editor with two large
 * files and measures time until the compare is ready. The editor is opened
 * without pre-computing the diff (no input.run() before openEditor), so the
 * diff runs in the background and saveable registration follows the normal
 * flow, avoiding "unknown saveable" / "already registered" issues.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class CompareEditorPerformanceUITest {

	private static final String COMPARE_EDITOR_ID = CompareUIPlugin.PLUGIN_ID + ".CompareEditor"; //$NON-NLS-1$

	private static final String S = System.lineSeparator();

	/** Number of lines per file; keep moderate so the test stays usable in CI. */
	private static final int LINES = 20_000;

	/** Approximate characters per line in the "long lines" performance test. */
	private static final int CHARS_PER_LINE_LONG = 500;

	/** Worst-case: many lines, very long lines, 95% different, anchors at different positions. */
	private static final int WORST_CASE_LINES = 40_000;
	private static final int WORST_CASE_CHARS_PER_LINE = 1000;
	/** Only every 20th line is identical (95% different). */
	private static final int WORST_CASE_ANCHOR_EVERY = 20;

	private IProject project;
	private IFile fileLeft;
	private IFile fileRight;

	@BeforeEach
	public void setUp() throws Exception {
		project = getWorkspace().getRoot().getProject("ComparePerfProject");
		createInWorkspace(project);
		fileLeft = project.getFile("left.txt");
		fileRight = project.getFile("right.txt");
		createInWorkspace(fileLeft);
		createInWorkspace(fileRight);
		fileLeft.setContents(createInputStream(createLargeContent(0, LINES)), true, true, new NullProgressMonitor());
		fileRight.setContents(createInputStream(createLargeContent(1, LINES)), true, true, new NullProgressMonitor());
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (project != null && project.exists()) {
			project.delete(true, new NullProgressMonitor());
		}
	}

	@Test
	public void testCompareEditorOpenPerformance() throws Exception {
		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 120_000);
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);
		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		System.out.println("CompareEditor UI performance (" + LINES + " lines): openEditor=" + openMs + " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	@Test
	public void testCompareEditorOpenPerformance90PercentDifferent() throws Exception {
		fileLeft.setContents(createInputStream(createLargeContent90PercentDifferent(0, LINES)), true, true, new NullProgressMonitor());
		fileRight.setContents(createInputStream(createLargeContent90PercentDifferent(1, LINES)), true, true, new NullProgressMonitor());

		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 120_000);
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);
		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		System.out.println("CompareEditor UI performance (" + LINES + " lines, 90% different): openEditor=" + openMs + " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	@Test
	public void testCompareEditorOpenPerformanceLongLines() throws Exception {
		fileLeft.setContents(createInputStream(createLargeContentWithLongLines(0, LINES, CHARS_PER_LINE_LONG)), true, true, new NullProgressMonitor());
		fileRight.setContents(createInputStream(createLargeContentWithLongLines(1, LINES, CHARS_PER_LINE_LONG)), true, true, new NullProgressMonitor());

		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 120_000);
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);
		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		System.out.println("CompareEditor UI performance (" + LINES + " lines, " + CHARS_PER_LINE_LONG + " chars/line): openEditor=" + openMs + " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	@Test
	public void testCompareEditorOpenPerformanceLongLines90PercentDifferent() throws Exception {
		fileLeft.setContents(createInputStream(createLargeContent90PercentDifferentWithLongLines(0, LINES, CHARS_PER_LINE_LONG)), true, true, new NullProgressMonitor());
		fileRight.setContents(createInputStream(createLargeContent90PercentDifferentWithLongLines(1, LINES, CHARS_PER_LINE_LONG)), true, true, new NullProgressMonitor());

		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 120_000);
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);
		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		System.out.println("CompareEditor UI performance (" + LINES + " lines, " + CHARS_PER_LINE_LONG + " chars/line, 90% different): openEditor=" + openMs + " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	@Test
	public void testCompareEditorOriginalCase() throws Exception {
		int anchorEvery = WORST_CASE_ANCHOR_EVERY;

		fileLeft.setContents(new FileInputStream(new File("C:\\Users\\mehmet.karaman\\Desktop\\install_rh9.txt")), true,
				true, new NullProgressMonitor());
//		fileRight.setContents(new FileInputStream(new File("C:\\Users\\mehmet.karaman\\Desktop\\install_rh9.txt")),
//				true, true, new NullProgressMonitor());

		fileRight.setContents(new FileInputStream(new File("C:\\Users\\mehmet.karaman\\Desktop\\install_rh7.txt")),
				true, true, new NullProgressMonitor());

		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 300_000); // 5 min timeout for worst case
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);

		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		int pctDiff = 100 - (100 / anchorEvery);
		System.out.println("CompareEditor UI performance WORST CASE (" + pctDiff + "% different): openEditor=" + openMs //$NON-NLS-1$ //$NON-NLS-2$
				+ " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	@Test
	public void testCompareEditorOpenPerformanceWorstCase() throws Exception {
		int lines = WORST_CASE_LINES;
		int charsPerLine = WORST_CASE_CHARS_PER_LINE;
		int anchorEvery = WORST_CASE_ANCHOR_EVERY;
		fileLeft.setContents(createInputStream(createLargeContentPercentDifferentWithLongLines(0, lines, charsPerLine, anchorEvery)), true, true, new NullProgressMonitor());
		fileRight.setContents(createInputStream(createLargeContentPercentDifferentWithLongLines(1, lines, charsPerLine, anchorEvery)), true, true, new NullProgressMonitor());

		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(null,
				SaveablesCompareEditorInput.createFileElement(fileLeft),
				SaveablesCompareEditorInput.createFileElement(fileRight),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());

		long startTotal = System.nanoTime();
		EditorPart part = (EditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR_ID, true);
		long openNanos = System.nanoTime() - startTotal;

		waitForCompareResult(input, 300_000); // 5 min timeout for worst case
		long timeToReadyNanos = System.nanoTime() - startTotal;
		CompareEditor editor = (CompareEditor) part;
		waitForContentReady(input);
		long totalNanos = System.nanoTime() - startTotal;

		assertThat(input.getCompareResult()).isNotNull();

		long openMs = openNanos / 1_000_000L;
		long timeToReadyMs = timeToReadyNanos / 1_000_000L;
		long totalMs = totalNanos / 1_000_000L;
		int pctDiff = 100 - (100 / anchorEvery);
		System.out.println("CompareEditor UI performance WORST CASE (" + lines + " lines, " + charsPerLine + " chars/line, " + pctDiff + "% different): openEditor=" + openMs + " ms, timeToCompareReady=" + timeToReadyMs + " ms, total=" + totalMs + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
	}

	/**
	 * Waits until the compare input has a result (background job finished) or timeout.
	 * Processes the display so the background job can run.
	 */
	private void waitForCompareResult(SaveablesCompareEditorInput input, int timeoutMs) throws InterruptedException {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (input.getCompareResult() == null && System.currentTimeMillis() < deadline) {
			if (!display.readAndDispatch()) {
				Thread.sleep(20);
			}
		}
	}

	private void waitForContentReady(SaveablesCompareEditorInput input) {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		for (int i = 0; i < 100; i++) {
			display.syncExec(() -> {
				try {
					CompareViewerSwitchingPane pane = (CompareViewerSwitchingPane) ReflectionUtils.getField(input, "fContentInputPane", true);
					if (pane != null) {
						Viewer viewer = pane.getViewer();
						if (viewer != null && viewer.getInput() != null) {
							// Content viewer is ready
						}
					}
				} catch (Exception e) {
					// ignore
				}
			});
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	/**
	 * Same 90% different pattern as {@link #createLargeContent90PercentDifferent(int, int)}
	 * (identical lines at different row positions), but each line is padded to
	 * approximately {@code charsPerLine} characters.
	 */
	private String createLargeContent90PercentDifferentWithLongLines(int variant, int lines, int charsPerLine) {
		return createLargeContentPercentDifferentWithLongLines(variant, lines, charsPerLine, 10);
	}

	/**
	 * Large content with only 1/{@code anchorEvery} lines identical (e.g. anchorEvery=20 → 95% different).
	 * Identical lines are at different row positions in left vs right (alternating above/below).
	 * Each line is padded to ~{@code charsPerLine} characters. Used for worst-case performance tests.
	 */
	private String createLargeContentPercentDifferentWithLongLines(int variant, int lines, int charsPerLine, int anchorEvery) {
		StringBuilder sb = new StringBuilder();
		String pad = charsPerLine > 30 ? "x".repeat(charsPerLine - 30) : ""; //$NON-NLS-1$ //$NON-NLS-2$
		int blocks = (lines + anchorEvery - 1) / anchorEvery;
		for (int b = 0; b < blocks; b++) {
			int leftAnchorPos = b % anchorEvery;
			int rightAnchorPos = (b + anchorEvery / 2) % anchorEvery;
			String anchorContent = "line " + (b * anchorEvery) + " " + pad; //$NON-NLS-1$ //$NON-NLS-2$
			if (anchorContent.length() > charsPerLine) {
				anchorContent = anchorContent.substring(0, charsPerLine);
			}
			for (int j = 0; j < anchorEvery && b * anchorEvery + j < lines; j++) {
				int idx = b * anchorEvery + j;
				if (variant == 0) {
					if (j == leftAnchorPos) {
						sb.append(anchorContent).append(S);
					} else {
						String line = "left_" + idx + " " + pad; //$NON-NLS-1$ //$NON-NLS-2$
						sb.append(line.length() > charsPerLine ? line.substring(0, charsPerLine) : line).append(S);
					}
				} else {
					if (j == rightAnchorPos) {
						sb.append(anchorContent).append(S);
					} else {
						String line = "right_" + idx + " " + pad; //$NON-NLS-1$ //$NON-NLS-2$
						sb.append(line.length() > charsPerLine ? line.substring(0, charsPerLine) : line).append(S);
					}
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Builds large text where left and right are 90% different: 10% identical lines,
	 * but each identical line appears at a different row in left vs right (sometimes
	 * above in left, sometimes above in right). Block size 10; anchor position
	 * alternates (variant 0 = left, variant 1 = right).
	 */
	private String createLargeContent90PercentDifferent(int variant, int lines) {
		StringBuilder sb = new StringBuilder();
		int blocks = (lines + 9) / 10;
		for (int b = 0; b < blocks; b++) {
			int leftAnchorPos = b % 10;
			int rightAnchorPos = (b + 5) % 10;
			String anchorContent = "line " + (b * 10) + S; //$NON-NLS-1$
			for (int j = 0; j < 10 && b * 10 + j < lines; j++) {
				if (variant == 0) {
					sb.append(j == leftAnchorPos ? anchorContent : "left_" + (b * 10 + j) + S); //$NON-NLS-1$
				} else {
					sb.append(j == rightAnchorPos ? anchorContent : "right_" + (b * 10 + j) + S); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Builds large text with the same small-diff pattern as {@link #createLargeContent(int, int)},
	 * but each line is padded to approximately {@code charsPerLine} characters. Used to measure
	 * the impact of line length on compare performance (extraction, string comparison, memory).
	 */
	private String createLargeContentWithLongLines(int variant, int lines, int charsPerLine) {
		StringBuilder sb = new StringBuilder();
		String pad = charsPerLine > 30 ? "x".repeat(charsPerLine - 30) : ""; //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < lines; i++) {
			String line = "line " + i + " " + pad; //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(line.length() > charsPerLine ? line.substring(0, charsPerLine) : line).append(S);
			int mod = i % 10;
			if (variant == 1) {
				if (mod == 3) {
					sb.append("right_only_line ").append(pad.length() > 80 ? pad.substring(0, 80) : pad).append(S); //$NON-NLS-1$
				}
				if (mod == 7) {
					sb.append("changed_in_right ").append(pad.length() > 80 ? pad.substring(0, 80) : pad).append(S); //$NON-NLS-1$
				}
			} else {
				if (mod == 2) {
					sb.append("left_only_line ").append(pad.length() > 80 ? pad.substring(0, 80) : pad).append(S); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Builds large text with small differences between left (variant 0) and right (variant 1).
	 */
	private String createLargeContent(int variant, int lines) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines; i++) {
			sb.append("line ").append(i).append(S);
			int mod = i % 10;
			if (variant == 1) {
				if (mod == 3) {
					sb.append("right_only_line").append(S);
				}
				if (mod == 7) {
					sb.append("changed_in_right").append(S);
				}
			} else {
				if (mod == 2) {
					sb.append("left_only_line").append(S);
				}
			}
		}
		return sb.toString();
	}
}
