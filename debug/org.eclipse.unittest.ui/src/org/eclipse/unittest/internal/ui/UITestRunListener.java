/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.launcher.TestRunListener;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * This test run listener is the entry point that makes sure the
 * org.eclipse.unittest plug-in gets loaded when a UnitTest launch configuration
 * is launched.
 */
public class UITestRunListener extends TestRunListener {
	@Override
	public void sessionLaunched(ITestRunSession session) {
		getDisplay().asyncExec(this::showTestRunnerViewPartInActivePage);
	}

	/**
	 * Creates a Test Runner View Part if it's not yet created and makes it visible
	 * in active page
	 *
	 * @return a {@link TestRunnerViewPart} instance
	 */
	private TestRunnerViewPart showTestRunnerViewPartInActivePage() {
		try {
			// Have to force the creation of view part contents
			// otherwise the UI will not be updated
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null)
				return null;
			TestRunnerViewPart view = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
			if (view == null) {
				// create and show the result view if it isn't created yet.
				return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME, null, IWorkbenchPage.VIEW_VISIBLE);
			} else {
				page.activate(view);
				return view;
			}
		} catch (PartInitException pie) {
			UnitTestPlugin.log(pie);
			return null;
		}
	}

	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

}
