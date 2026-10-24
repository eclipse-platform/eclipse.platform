/*******************************************************************************
 * Copyright (c) 2026 Luise Ravnskjaer and others.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the contents of a compare editor input are created only once when
 * the initialization job finishes while the "still initializing" timer is
 * already creating them.
 * <p>
 * The input holds its job until {@code createContents} has started and then
 * runs the event loop from inside {@code createContents}, so that the job's UI
 * callback is dispatched re-entrantly. See
 * https://github.com/eclipse-platform/eclipse.platform/issues/123
 * </p>
 */
public class CompareEditorOpenRaceTest {

	private static final long TIMEOUT_MILLIS = 30_000;

	private boolean originalUnifiedDiff;

	@BeforeEach
	void setUp() {
		assertNotNull(Display.getCurrent(), "tests require a UI thread / Display"); //$NON-NLS-1$
		originalUnifiedDiff = store().getBoolean(ComparePreferencePage.UNIFIED_DIFF);
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, false);
	}

	@AfterEach
	void tearDown() {
		store().setValue(ComparePreferencePage.UNIFIED_DIFF, originalUnifiedDiff);
		IWorkbenchPage page = activePage();
		if (page != null) {
			page.closeAllEditors(false);
		}
		DisplayUtils.processQueuedEvents();
	}

	@Test
	void testContentsCreatedOnceWhenJobFinishesDuringCreation() {
		RaceInput input = new RaceInput();
		CompareUI.openCompareEditor(input);
		DisplayUtils.pumpUntil(() -> input.created > 0 && Job.getJobManager().find(input).length == 0,
				"compare editor did not finish opening", TIMEOUT_MILLIS); //$NON-NLS-1$

		assertFalse(input.raceMissed, "contents were not created while the job was held"); //$NON-NLS-1$
		assertEquals(1, input.created, "the contents of an input must be created once"); //$NON-NLS-1$
	}

	private static final class RaceInput extends CompareEditorInput {

		private final CountDownLatch creating = new CountDownLatch(1);
		private volatile boolean raceMissed;
		private int created;

		RaceInput() {
			super(new CompareConfiguration());
			setTitle("Race compare"); //$NON-NLS-1$
		}

		@Override
		public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
			super.run(monitor); // the compare result is set from here on
			raceMissed = !creating.await(10, TimeUnit.SECONDS);
		}

		@Override
		public Control createContents(Composite parent) {
			if (++created == 1 && !raceMissed) {
				creating.countDown(); // lets the job go on to its syncExec
				DisplayUtils.pumpUntil(() -> Job.getJobManager().find(this).length == 0,
						"the initialization job did not finish", TIMEOUT_MILLIS); //$NON-NLS-1$
			}
			return new Composite(parent, SWT.NONE);
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			return new DiffNode(Differencer.CHANGE);
		}

		@Override
		public boolean canRunAsJob() {
			return true;
		}
	}

	private static IWorkbenchPage activePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static IPreferenceStore store() {
		return CompareUIPlugin.getDefault().getPreferenceStore();
	}
}
