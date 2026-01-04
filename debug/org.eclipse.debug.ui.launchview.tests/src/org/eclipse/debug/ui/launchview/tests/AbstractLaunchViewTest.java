/*******************************************************************************
 *  Copyright (c) 2017 Andrey Loskutov and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.tests;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.progress.UIJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class AbstractLaunchViewTest {

	private static boolean welcomeClosed;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "setUp");
		assertWelcomeScreenClosed();
	}

	@AfterEach
	public void tearDown(TestInfo testInfo) throws Exception {
		TestUtil.log(IStatus.INFO, testInfo.getDisplayName(), "tearDown");
		TestUtil.cleanUp(testInfo.getDisplayName());
	}

	/**
	 * Ensure the welcome screen is closed because in 4.x the debug perspective
	 * opens a giant fast-view causing issues
	 */
	protected final void assertWelcomeScreenClosed() throws Exception {
		if (!welcomeClosed && PlatformUI.isWorkbenchRunning()) {
			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb == null) {
				return;
			}
			// In UI thread we don't need to run a job
			if (Display.getCurrent() != null) {
				closeIntro(wb);
				return;
			}

			UIJob job = new UIJob("close welcome screen for debug test suite") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					closeIntro(wb);
					return Status.OK_STATUS;
				}

			};
			job.setPriority(Job.INTERACTIVE);
			job.setSystem(true);
			job.schedule();
		}
	}

	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im = wb.getIntroManager();
			IIntroPart intro = im.getIntro();
			if (intro != null) {
				welcomeClosed = im.closeIntro(intro);
			}
		}
	}
}
