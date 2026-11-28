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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the initialization job of a replaced input leaves a reused compare
 * editor alone.
 */
public class CompareEditorReuseTest {

	private static final long TIMEOUT_MILLIS = 30_000;

	private boolean originalUnifiedDiff;
	private final HeldInput first = new HeldInput();
	private final HeldInput second = new HeldInput();

	@BeforeEach
	void setUp() {
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display"); //$NON-NLS-1$
		originalUnifiedDiff = store().getBoolean(ComparePreferencePage.UNIFIED_DIFF);
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
	}

	@AfterEach
	void tearDown() {
		first.release.countDown();
		second.release.countDown();
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, originalUnifiedDiff);
		activePage().closeAllEditors(false);
	}

	@Test
	void testCanceledJobOfReplacedInputDoesNotCloseEditor() {
		CompareUI.openCompareEditor(first);
		// A job that has not started yet is discarded by the cancel and never calls back
		pumpUntil(() -> first.started, "the job of the first input did not start"); //$NON-NLS-1$
		IEditorPart editor = activePage().findEditor(first);
		assertNotNull(editor, "no compare editor for the first input"); //$NON-NLS-1$

		CompareUI.reuseCompareEditor(second, (IReusableEditor) editor);
		pumpUntil(() -> second.started, "the job of the second input did not start"); //$NON-NLS-1$
		first.release.countDown();
		pumpUntil(() -> Job.getJobManager().find(first).length == 0, "the job of the first input did not finish"); //$NON-NLS-1$

		assertSame(editor, activePage().findEditor(second),
				"the canceled job of the replaced input must not close the editor"); //$NON-NLS-1$

		second.release.countDown();
		pumpUntil(() -> second.created > 0, "the contents of the second input were not created"); //$NON-NLS-1$
		assertEquals(0, first.created, "contents created for the replaced input"); //$NON-NLS-1$
		assertEquals(1, second.created, "the contents of the second input must be created once"); //$NON-NLS-1$
	}

	/** An input whose job waits in {@code prepareInput} until it is released. */
	private static final class HeldInput extends CompareEditorInput {

		private final CountDownLatch release = new CountDownLatch(1);
		private volatile boolean started;
		private int created;

		HeldInput() {
			super(new CompareConfiguration());
			setTitle("Held compare"); //$NON-NLS-1$
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {
			started = true;
			release.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			return new DiffNode(Differencer.CHANGE);
		}

		@Override
		public Control createContents(Composite parent) {
			created++;
			return new Composite(parent, SWT.NONE);
		}

		@Override
		public boolean canRunAsJob() {
			return true;
		}
	}

	private static void pumpUntil(BooleanSupplier condition, String failMessage) {
		Display display = Display.getCurrent();
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		// Keeps the loop waking so that the deadline holds while blocked in sleep()
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

	private static IWorkbenchPage activePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static IPreferenceStore store() {
		return CompareUIPlugin.getDefault().getPreferenceStore();
	}
}
