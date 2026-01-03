/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.tests;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.progress.UIJob;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DebugTestExtension implements BeforeEachCallback, AfterEachCallback {

	private boolean welcomeClosed = false;

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		TestUtil.log(IStatus.INFO, context.getDisplayName(), "setUp");
		assertWelcomeScreenClosed();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		TestUtil.log(IStatus.INFO, context.getDisplayName(), "tearDown");
		TestUtil.cleanUp(context.getDisplayName());
	}

	/**
	 * Ensure the welcome screen is closed because in 4.x the debug perspective
	 * opens a giant fast-view causing issues
	 */
	public void assertWelcomeScreenClosed() throws Exception {
		if (!welcomeClosed && PlatformUI.isWorkbenchRunning()) {
			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb != null) {
				UIJob job = new UIJob("close welcome screen for Ant test suite") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
						if (window != null) {
							IIntroManager im = wb.getIntroManager();
							IIntroPart intro = im.getIntro();
							if (intro != null) {
								welcomeClosed = im.closeIntro(intro);
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.INTERACTIVE);
				job.setSystem(true);
				job.schedule();
			}
		}
	}

}
