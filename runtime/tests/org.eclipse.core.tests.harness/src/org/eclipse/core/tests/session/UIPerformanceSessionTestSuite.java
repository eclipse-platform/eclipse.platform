/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.session;

import junit.framework.TestCase;
import org.eclipse.core.tests.session.SetupManager.SetupException;

/**
 * TODO It should live in the UI tests instead.
 */
public class UIPerformanceSessionTestSuite extends PerformanceSessionTestSuite {

	public UIPerformanceSessionTestSuite(String pluginId, int timesToRun) {
		super(pluginId, timesToRun);
		setApplicationId(SessionTestSuite.UI_TEST_APPLICATION);
	}

	public UIPerformanceSessionTestSuite(String pluginId, int timesToRun, Class<?> theClass) {
		super(pluginId, timesToRun, theClass);
		setApplicationId(SessionTestSuite.UI_TEST_APPLICATION);
	}

	public UIPerformanceSessionTestSuite(String pluginId, int timesToRun, Class<? extends TestCase> theClass,
			String name) {
		super(pluginId, timesToRun, theClass, name);
		setApplicationId(SessionTestSuite.UI_TEST_APPLICATION);
	}

	public UIPerformanceSessionTestSuite(String pluginId, int timesToRun, String name) {
		super(pluginId, timesToRun, name);
		setApplicationId(SessionTestSuite.UI_TEST_APPLICATION);
	}

	/**
	 * Ensures setup uses this suite's instance location.
	 * @throws SetupException
	 */
	@Override
	protected Setup newSetup() throws SetupException {
		Setup base = super.newSetup();
		base.setSystemProperty("org.eclipse.ui.testsWaitForEarlyStartup", "false");
		return base;
	}
}
