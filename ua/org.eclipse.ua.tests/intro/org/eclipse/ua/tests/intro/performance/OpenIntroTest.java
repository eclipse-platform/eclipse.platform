/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.ua.tests.intro.performance;

import org.eclipse.swt.widgets.Display;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCaseJunit5;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.FrameworkUtil;

@Disabled("Disabled due to inability to backport test to 3.2. Internal test hooks were added in 3.2.2 code base but do not exist in 3.2 so the test will not be accurate.")
public class OpenIntroTest extends PerformanceTestCaseJunit5 {

	@BeforeEach
	@Override
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);
		closeIntro();
		// test extensions filter by this system property
		System.setProperty("org.eclipse.ua.tests.property.isTesting", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		ExtensionPointManager.getInst().setExtensionFilter(FrameworkUtil.getBundle(getClass()).getSymbolicName());
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		closeIntro();
		// test extensions filter by this system property
		System.setProperty("org.eclipse.ua.tests.property.isTesting", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		ExtensionPointManager.getInst().setExtensionFilter(null);
	}

	@Test
	public void testOpenIntro() throws Exception {
		tagAsSummary("Open welcome", Dimension.ELAPSED_PROCESS);

		// warm-up
		for (int i=0;i<3;++i) {
			openIntro();
			closeIntro();
		}

		// run the tests
		for (int i=0;i<50;++i) {
			startMeasuring();
			openIntro();
			stopMeasuring();
			closeIntro();
		}

		commitMeasurements();
		assertPerformance();
	}

	public static void closeIntro() throws Exception {
		IIntroManager manager = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart part = manager.getIntro();
		if (part != null) {
			manager.closeIntro(part);
		}
		ExtensionPointManager.getInst().clear();
		flush();
	}

	private static void openIntro() throws Exception {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IIntroManager manager = workbench.getIntroManager();
		CustomizableIntroPart introPart = (CustomizableIntroPart)manager.showIntro(workbench.getActiveWorkbenchWindow(), false);

		Display display = Display.getDefault();
		while (!introPart.internal_isFinishedLoading()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		flush();
	}

	private static void flush() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
		}
	}
}
